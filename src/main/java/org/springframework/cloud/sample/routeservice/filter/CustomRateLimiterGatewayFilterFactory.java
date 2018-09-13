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
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern.PathMatchInfo;

import java.util.Map;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE;

@Component
public class CustomRateLimiterGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final Logger log = LoggerFactory.getLogger(CustomRateLimiterGatewayFilterFactory.class);

    RateLimiters rateLimiters;

    public CustomRateLimiterGatewayFilterFactory(RateLimiters rateLimiters) {
        this.rateLimiters = rateLimiters;
    }

    @SuppressWarnings("unchecked")
    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {

            String serviceId = getServiceInstanceId(exchange);
            RedisRateLimiter limiter = rateLimiters.getLimiter(serviceId);

            KeyResolver resolver = new SessionIdKeyResolver();

            log.info("Hello from the new filter");

            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

            return resolver.resolve(exchange).flatMap(key ->
                    // TODO: if key is empty?
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
        };
    }

    private String getServiceInstanceId(ServerWebExchange exchange) {
        PathMatchInfo uriVariablesAttr = exchange.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        Map<String, String> uriVariables = uriVariablesAttr.getUriVariables();
        return uriVariables.get("instanceId");
    }


}
