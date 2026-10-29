package com.rumi.gateway.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

/**
 * Access rules of the gateway. Matchers are evaluated in order and the first match wins.
 * The finer rule "only my own building" belongs to each service, not to the gateway.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    static final String[] PUBLIC_PATHS = {
            "/api/v1/authentication/**",
            "/api/v1/gateway/health",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
    };

    static final String[] ADMINISTRATOR_PATHS = {
            // Must stay before the building paths: /api/v1/buildings/** would capture it otherwise.
            "/api/v1/buildings/*/risk-indexes/**",
            "/api/v1/readings/**",
            "/api/v1/digital-twin/**",
            "/api/v1/seismic-events/**",
            "/api/v1/risk-indexes/**",
            "/api/v1/reports/**",
            "/api/v1/subscriptions/**",
            "/api/v1/payments/**",
            "/api/v1/thresholds/**"
    };

    static final String[] BUILDING_PATHS = {
            "/api/v1/buildings/**",
            "/api/v1/sensors/**",
            "/api/v1/invitations/**"
    };

    private static final String ADMINISTRATOR = Role.ADMINISTRATOR.name();
    private static final String RESIDENT = Role.RESIDENT.name();

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
            ProblemDetailAccessDeniedHandler accessDeniedHandler
    ) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(PUBLIC_PATHS).permitAll()
                        .pathMatchers(ADMINISTRATOR_PATHS).hasRole(ADMINISTRATOR)
                        .pathMatchers(HttpMethod.GET, BUILDING_PATHS).hasAnyRole(ADMINISTRATOR, RESIDENT)
                        .pathMatchers(HttpMethod.POST, BUILDING_PATHS).hasRole(ADMINISTRATOR)
                        .pathMatchers(HttpMethod.PUT, BUILDING_PATHS).hasRole(ADMINISTRATOR)
                        .pathMatchers(HttpMethod.PATCH, BUILDING_PATHS).hasRole(ADMINISTRATOR)
                        .pathMatchers(HttpMethod.DELETE, BUILDING_PATHS).hasRole(ADMINISTRATOR)
                        // users, device-tokens, evacuation-plans, checklists, inspections, damage-reports,
                        // alerts and any other path: a valid token of any role.
                        .anyExchange().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(roleAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    /**
     * Maps the {@code role} claim to the authority {@code ROLE_<role>}.
     */
    private static ReactiveJwtAuthenticationConverterAdapter roleAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(JwtClaimNames.ROLE);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
