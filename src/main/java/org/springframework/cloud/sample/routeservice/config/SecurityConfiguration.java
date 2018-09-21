/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sample.routeservice.config;

import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

import static org.springframework.cloud.gateway.handler.predicate.CloudFoundryRouteServiceRoutePredicateFactory.X_CF_FORWARDED_URL;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

	private ServerWebExchangeMatcher matcher = exchange -> {

		String forwardedUrl = exchange.getRequest().getHeaders().getFirst(X_CF_FORWARDED_URL);
		if (forwardedUrl != null) {
			return ServerWebExchangeMatcher.MatchResult.match();
		}
		return ServerWebExchangeMatcher.MatchResult.notMatch();
	};


	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
		http
				.securityMatcher(matcher)// process only request with forwardUrl
				.addFilterAt(new HeaderFilter(), SecurityWebFiltersOrder.FIRST)
				.addFilterAt(new UndoHeaderFilter(), SecurityWebFiltersOrder.LAST)
				.csrf().disable()
				.authorizeExchange()
//				.pathMatchers("/v2/**").hasRole("ADMIN")
//				.matchers(EndpointRequest.to("info", "health")).permitAll()
//				.matchers(EndpointRequest.toAnyEndpoint()).hasRole("ADMIN")
//				.pathMatchers("/images/**").permitAll()
				.anyExchange().authenticated()
				.and().logout().logoutSuccessHandler(new RoleBasedServerLogoutSuccessHandler())
				.and().formLogin().authenticationSuccessHandler(new RoleBasedAuthenticationSuccessHandler())
				.and().httpBasic();
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public MapReactiveUserDetailsService userDetailsService() {
		UserDetails admin = User.builder().username("admin").password(passwordEncoder().encode("supersecret")).roles("ADMIN").build();
		UserDetails trial = User.builder().username("trial").password(passwordEncoder().encode("pw")).roles("TRIAL").build();
		UserDetails basic = User.builder().username("basic").password(passwordEncoder().encode("pw")).roles("BASIC").build();
		UserDetails premium = User.builder().username("premium").password(passwordEncoder().encode("pw")).roles("PREMIUM").build();
		return new MapReactiveUserDetailsService(admin, trial, premium, basic);
	}

}
