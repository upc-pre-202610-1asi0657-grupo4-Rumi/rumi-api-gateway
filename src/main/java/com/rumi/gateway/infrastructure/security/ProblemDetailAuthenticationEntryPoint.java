package com.rumi.gateway.infrastructure.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Answers 401 for a missing, malformed, badly signed or expired token. The detail is the same in every
 * case so that the answer does not reveal why the token was rejected.
 */
@Component
class ProblemDetailAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    static final String DETAIL = "A valid bearer token is required to access this resource.";

    private final ProblemDetailResponseWriter writer;

    ProblemDetailAuthenticationEntryPoint(ProblemDetailResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
        String challenge = exception instanceof InvalidBearerTokenException ? "Bearer error=\"invalid_token\"" : "Bearer";
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, challenge);
        return writer.write(exchange, HttpStatus.UNAUTHORIZED, DETAIL);
    }
}
