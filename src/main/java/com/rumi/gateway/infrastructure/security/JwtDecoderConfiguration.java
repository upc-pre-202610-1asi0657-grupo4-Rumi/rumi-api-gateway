package com.rumi.gateway.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Validates the HS256 tokens issued by rumi-iam-service locally: the gateway never calls IAM.
 */
@Configuration
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class JwtDecoderConfiguration {

    @Bean
    public ReactiveJwtDecoder jwtDecoder(JwtSecurityProperties properties) {
        SecretKey key = new SecretKeySpec(properties.secretBytes(), "HmacSHA256");
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), new RumiClaimsValidator()));
        return decoder;
    }
}
