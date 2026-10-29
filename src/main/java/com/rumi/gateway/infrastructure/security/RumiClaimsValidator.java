package com.rumi.gateway.infrastructure.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Checks the claims the gateway relies on: an expiration, a UUID subject, a known role and a list of
 * building UUIDs. The list may be empty: a new administrator has no buildings yet.
 * Signature and the expiration instant itself are checked by the decoder and the default validators.
 */
class RumiClaimsValidator implements OAuth2TokenValidator<Jwt> {

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getExpiresAt() == null) {
            return invalid("The token has no expiration");
        }
        if (!isUuid(jwt.getSubject())) {
            return invalid("The subject is not a user id");
        }
        if (!isKnownRole(jwt.getClaim(JwtClaimNames.ROLE))) {
            return invalid("The role is not recognized");
        }
        if (!isListOfUuids(jwt.getClaim(JwtClaimNames.BUILDING_IDS))) {
            return invalid("The building ids are not a list of ids");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static boolean isKnownRole(Object role) {
        return role instanceof String value
                && Arrays.stream(Role.values()).anyMatch(known -> known.name().equals(value));
    }

    private static boolean isListOfUuids(Object claim) {
        return claim instanceof List<?> values && values.stream().allMatch(RumiClaimsValidator::isUuid);
    }

    private static boolean isUuid(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }
        try {
            return UUID.fromString(text).toString().equalsIgnoreCase(text);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static OAuth2TokenValidatorResult invalid(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, description, null));
    }
}
