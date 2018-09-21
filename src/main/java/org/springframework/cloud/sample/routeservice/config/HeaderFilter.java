package org.springframework.cloud.sample.routeservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.sample.routeservice.filter.LoggingGatewayFilterFactory;
import org.springframework.web.server.WebFilter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import static org.springframework.cloud.gateway.handler.predicate.CloudFoundryRouteServiceRoutePredicateFactory.X_CF_FORWARDED_URL;


/**
 * @author Rob Winch
 */
public class HeaderFilter implements WebFilter {
    private final Logger log = LoggerFactory.getLogger(HeaderFilter.class);

    private static final String X_CF_INSTANCE_URL = "";

    public static final String ORIGINAL = "ORIGINAL_EXCHANGE";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Hooks.onOperatorDebug();

        ServerHttpRequest request = exchange.getRequest();
        String url = request.getHeaders().getFirst(X_CF_FORWARDED_URL);
        log.info("Url: {}", url);
        if (url == null) {
            return chain.filter(exchange);
        }
        String path = UriComponentsBuilder.fromHttpUrl(url).build().getPath();
        log.info("Path: {}", path);
        ServerHttpRequest modified = request.mutate()
                .header(X_CF_INSTANCE_URL, request.getURI().toASCIIString())
                .path(path)
                .build();
        ServerWebExchange modifiedExchange = exchange.mutate().request(modified).build();
        modifiedExchange.getAttributes().put(ORIGINAL, exchange);
        return chain.filter(modifiedExchange);
    }
}
