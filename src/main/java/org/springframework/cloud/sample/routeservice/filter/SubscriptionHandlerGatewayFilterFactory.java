package org.springframework.cloud.sample.routeservice.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.sample.routeservice.servicebroker.RateLimiters;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern.PathMatchInfo;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import static org.springframework.cloud.gateway.handler.predicate.CloudFoundryRouteServiceRoutePredicateFactory.X_CF_FORWARDED_URL;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE;

@Component
public class SubscriptionHandlerGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {
    private final Logger log = LoggerFactory.getLogger(SubscriptionHandlerGatewayFilterFactory.class);

    private RateLimiters rateLimiters;

    private KeyResolver resolver;

    public SubscriptionHandlerGatewayFilterFactory(RateLimiters rateLimiters, KeyResolver resolver) {
        this.rateLimiters = rateLimiters;
        this.resolver = resolver;
    }

    @SuppressWarnings("unchecked")
    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {

            String serviceId = getServiceInstanceId(exchange);

            return getUserRole(exchange).single().flatMap(role -> {
                RedisRateLimiter limiter = rateLimiters.getLimiter(serviceId.concat(role.getAuthority().substring(4)));

                String forwardedUrl = exchange.getRequest().getHeaders().getFirst(X_CF_FORWARDED_URL);
                Optional<URI> forwardedUri = Optional.ofNullable(forwardedUrl).map(url -> {
                    try {
                        return new URL(url).toURI();
                    } catch (MalformedURLException | URISyntaxException e) {
                        log.info("Request url is invalid : url={}, error={}", forwardedUrl,
                                e.getMessage());
                        return null;
                    }
                });

                if (forwardedUri.get().getPath().equals("/logout")) {
                    // Set cookie with user role as type
                    log.info("Clearing cookie 'type' field");
                    ResponseCookie cookie = ResponseCookie.from("type", "").path("/").build();
                    exchange.getResponse().addCookie(cookie);
                } else {
                    // Set cookie with user role as type
                    log.info("Setting cookie with type={}", role.getAuthority().substring(5).toLowerCase());
                    ResponseCookie cookie = ResponseCookie.from("type", role.getAuthority().substring(5).toLowerCase()).path("/").build();
                    exchange.getResponse().addCookie(cookie);
                }

                Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

                return resolver.resolve(exchange).flatMap(key ->
                        limiter.isAllowed(route.getId(), key).flatMap(response -> {
                            for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
                                exchange.getResponse().getHeaders().add(header.getKey(), header.getValue());
                            }

                            if (response.isAllowed()) {
                                return chain.filter(exchange);
                            }

                            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                            return exchange.getResponse().setComplete();
                        }));
            });
        };
    }

    private String getServiceInstanceId(ServerWebExchange exchange) {
        PathMatchInfo uriVariablesAttr = exchange.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        Map<String, String> uriVariables = uriVariablesAttr.getUriVariables();
        return uriVariables.get("instanceId");
    }

    private Flux<GrantedAuthority> getUserRole(ServerWebExchange exchange) {
        return exchange.getPrincipal().cast(Authentication.class).flatMapIterable(a -> a.getAuthorities());
    }


}
