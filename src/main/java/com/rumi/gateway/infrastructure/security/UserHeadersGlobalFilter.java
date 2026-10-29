package com.rumi.gateway.infrastructure.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * Forwards the authenticated user to the services. The user headers sent by the client are removed on
 * every route, public ones included, so that nobody can impersonate another user.
 */
@Component
public class UserHeadersGlobalFilter implements GlobalFilter, Ordered {

    public static final String USER_ID = "X-User-Id";
    public static final String ROLE = "X-Role";
    public static final String BUILDING_ID = "X-Building-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(principal -> ((JwtAuthenticationToken) principal).getToken())
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(jwt -> chain.filter(withUserHeaders(exchange, jwt)));
    }

    private static ServerWebExchange withUserHeaders(ServerWebExchange exchange, Optional<Jwt> jwt) {
        return exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.remove(USER_ID);
                    headers.remove(ROLE);
                    headers.remove(BUILDING_ID);
                    jwt.ifPresent(token -> addUserHeaders(headers, token));
                }))
                .build();
    }

    private static void addUserHeaders(HttpHeaders headers, Jwt token) {
        List<String> buildingIds = token.getClaimAsStringList(JwtClaimNames.BUILDING_IDS);
        headers.set(USER_ID, token.getSubject());
        headers.set(ROLE, token.getClaimAsString(JwtClaimNames.ROLE));
        headers.set(BUILDING_ID, String.join(",", buildingIds));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
