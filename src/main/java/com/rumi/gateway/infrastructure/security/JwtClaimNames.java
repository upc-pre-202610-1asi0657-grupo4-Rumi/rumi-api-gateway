package com.rumi.gateway.infrastructure.security;

/**
 * Custom claims of the tokens issued by rumi-iam-service. {@code sub} holds the user id.
 */
public final class JwtClaimNames {

    public static final String ROLE = "role";
    public static final String BUILDING_IDS = "buildingIds";

    private JwtClaimNames() {
    }
}
