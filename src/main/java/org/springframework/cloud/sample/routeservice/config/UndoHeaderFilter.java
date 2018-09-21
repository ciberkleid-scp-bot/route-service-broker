package org.springframework.cloud.sample.routeservice.config;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * @author Rob Winch
 */
public class UndoHeaderFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerWebExchange original = exchange.getAttribute(HeaderFilter.ORIGINAL);
        if (original != null) {
            exchange = original.mutate().principal(exchange.getPrincipal()).build();
        }
        return chain.filter(exchange);
    }
}
