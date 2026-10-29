package com.rumi.gateway.infrastructure.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Answers 403 when the token is valid but its role is not allowed on the requested path and method.
 */
@Component
class ProblemDetailAccessDeniedHandler implements ServerAccessDeniedHandler {

    static final String DETAIL = "Your role is not allowed to access this resource.";

    private final ProblemDetailResponseWriter writer;

    ProblemDetailAccessDeniedHandler(ProblemDetailResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        return writer.write(exchange, HttpStatus.FORBIDDEN, DETAIL);
    }
}
