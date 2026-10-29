package com.rumi.gateway.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Builds HS256 tokens shaped like the ones issued by rumi-iam-service.
 */
public final class JwtTestTokens {

    public static final String USER_ID = "7d3c5b1e-2f4a-4c8e-9b6d-1a2b3c4d5e6f";
    public static final String BUILDING_ID = "3f2c8a10-6b7d-4e9f-a1b2-c3d4e5f60718";
    public static final String OTHER_BUILDING_ID = "9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";
    public static final String OTHER_SECRET = "another-secret-that-the-gateway-does-not-know";

    private final String secret;

    public JwtTestTokens(String secret) {
        this.secret = secret;
    }

    public String administrator() {
        return token("ADMINISTRATOR", List.of(BUILDING_ID, OTHER_BUILDING_ID));
    }

    public String resident() {
        return token("RESIDENT", List.of(BUILDING_ID));
    }

    public String token(String role, List<String> buildingIds) {
        return sign(secret, claims -> claims.claim("role", role).claim("buildingIds", buildingIds));
    }

    public String expired() {
        Instant issuedAt = Instant.now().minus(Duration.ofMinutes(30));
        return sign(secret, claims -> claims
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plus(Duration.ofMinutes(15))))
                .claim("role", "ADMINISTRATOR")
                .claim("buildingIds", List.of(BUILDING_ID)));
    }

    public String signedWithAnotherSecret() {
        return sign(OTHER_SECRET, claims -> claims.claim("role", "ADMINISTRATOR").claim("buildingIds", List.of(BUILDING_ID)));
    }

    public String withoutBuildingIds() {
        return sign(secret, claims -> claims.claim("role", "ADMINISTRATOR"));
    }

    /**
     * Signs a token with a UUID subject and a 15-minute expiration, then applies {@code customizer}.
     */
    public String sign(String key, UnaryOperator<JWTClaimsSet.Builder> customizer) {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(USER_ID)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofMinutes(15))));
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), customizer.apply(claims).build());
            jwt.sign(new MACSigner(key.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Could not sign the test token", exception);
        }
    }
}
