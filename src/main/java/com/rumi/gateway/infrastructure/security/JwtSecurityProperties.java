package com.rumi.gateway.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

/**
 * HS256 secret shared with rumi-iam-service, read from {@code JWT_SECRET}.
 * The gateway refuses to start without a secret of at least 32 bytes.
 */
@ConfigurationProperties(prefix = "rumi.security.jwt")
public record JwtSecurityProperties(String secret) {

    static final int MINIMUM_SECRET_BYTES = 32;

    public JwtSecurityProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not set: the gateway cannot validate tokens without it");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MINIMUM_SECRET_BYTES + " bytes long");
        }
    }

    public byte[] secretBytes() {
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
