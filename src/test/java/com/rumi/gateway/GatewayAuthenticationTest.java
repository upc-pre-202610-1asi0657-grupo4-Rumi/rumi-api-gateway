package com.rumi.gateway;

import com.rumi.gateway.support.JwtTestTokens;
import com.rumi.gateway.support.StubServiceBackend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static com.rumi.gateway.support.StubServiceBackend.ECHO_PREFIX;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayAuthenticationTest {

    private static final String PROTECTED_PATH = "/api/v1/alerts";

    @DynamicPropertySource
    static void routeToStubBackend(DynamicPropertyRegistry registry) {
        StubServiceBackend.registerServiceUrls(registry);
    }

    @Autowired
    private WebTestClient client;

    @Value("${rumi.security.jwt.secret}")
    private String secret;

    private JwtTestTokens tokens;

    @BeforeEach
    void createTokens() {
        tokens = new JwtTestTokens(secret);
    }

    @Test
    void rejectsARequestWithoutTokenWithUnauthorizedProblemDetail() {
        client.get().uri(PROTECTED_PATH).exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.title").isEqualTo("Unauthorized")
                .jsonPath("$.detail").isEqualTo("A valid bearer token is required to access this resource.")
                .jsonPath("$.instance").isEqualTo(PROTECTED_PATH);
    }

    @Test
    void rejectsAMalformedToken() {
        expectInvalidToken("not-a-jwt");
    }

    @Test
    void rejectsATokenSignedWithAnotherSecret() {
        expectInvalidToken(tokens.signedWithAnotherSecret());
    }

    @Test
    void rejectsAnExpiredToken() {
        expectInvalidToken(tokens.expired());
    }

    @Test
    void rejectsATokenWithAnUnknownRole() {
        expectInvalidToken(tokens.token("SUPERUSER", List.of(JwtTestTokens.BUILDING_ID)));
    }

    @Test
    void rejectsATokenWithoutBuildingIds() {
        expectInvalidToken(tokens.withoutBuildingIds());
    }

    @Test
    void rejectsATokenWithBuildingIdsThatAreNotUuids() {
        expectInvalidToken(tokens.token("ADMINISTRATOR", List.of("building-1")));
    }

    @Test
    void acceptsAnAdministratorWithoutBuildingsYet() {
        client.get().uri("/api/v1/buildings")
                .headers(headers -> headers.setBearerAuth(tokens.token("ADMINISTRATOR", List.of())))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(ECHO_PREFIX + "X-Building-Id", "");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/buildings/3f2c8a10-6b7d-4e9f-a1b2-c3d4e5f60718/risk-indexes",
            "/api/v1/readings",
            "/api/v1/digital-twin/zones",
            "/api/v1/seismic-events",
            "/api/v1/risk-indexes/1",
            "/api/v1/reports",
            "/api/v1/subscriptions",
            "/api/v1/subscriptions/health",
            "/api/v1/payments",
            "/api/v1/thresholds"
    })
    void forbidsAResidentOnAdministratorPaths(String path) {
        client.get().uri(path)
                .headers(headers -> headers.setBearerAuth(tokens.resident()))
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.title").isEqualTo("Forbidden")
                .jsonPath("$.detail").isEqualTo("Your role is not allowed to access this resource.")
                .jsonPath("$.instance").isEqualTo(path);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/buildings/3f2c8a10-6b7d-4e9f-a1b2-c3d4e5f60718/risk-indexes",
            "/api/v1/readings",
            "/api/v1/digital-twin/zones",
            "/api/v1/seismic-events",
            "/api/v1/risk-indexes/1",
            "/api/v1/reports",
            "/api/v1/subscriptions",
            "/api/v1/payments",
            "/api/v1/thresholds"
    })
    void letsAnAdministratorThroughAdministratorPaths(String path) {
        expectRouted(HttpMethod.GET, path, tokens.administrator());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/users/1",
            "/api/v1/device-tokens",
            "/api/v1/evacuation-plans",
            "/api/v1/evacuation-plans/health",
            "/api/v1/checklists",
            "/api/v1/inspections",
            "/api/v1/inspections/health",
            "/api/v1/damage-reports",
            "/api/v1/alerts",
            "/api/v1/alerts/health"
    })
    void letsAnyValidTokenThroughSharedPaths(String path) {
        expectRouted(HttpMethod.GET, path, tokens.resident());
        expectRouted(HttpMethod.GET, path, tokens.administrator());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/buildings", "/api/v1/buildings/3f2c8a10/sensors", "/api/v1/sensors/1", "/api/v1/invitations"})
    void letsBothRolesReadBuildingPaths(String path) {
        expectRouted(HttpMethod.GET, path, tokens.resident());
        expectRouted(HttpMethod.GET, path, tokens.administrator());
    }

    @ParameterizedTest
    @CsvSource({
            "POST, /api/v1/buildings",
            "PUT, /api/v1/buildings/1",
            "PATCH, /api/v1/sensors/1",
            "DELETE, /api/v1/invitations/1"
    })
    void letsOnlyAnAdministratorChangeBuildingPaths(HttpMethod method, String path) {
        client.method(method).uri(path)
                .headers(headers -> headers.setBearerAuth(tokens.resident()))
                .exchange()
                .expectStatus().isForbidden();
        expectRouted(method, path, tokens.administrator());
    }

    @Test
    void requiresATokenOnPathsWithoutAnExplicitRule() {
        client.get().uri("/api/v1/unknown").exchange().expectStatus().isUnauthorized();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/authentication/sign-in", "/api/v1/authentication/health"})
    void routesAuthenticationPathsWithoutToken(String path) {
        client.get().uri(path).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("routed");
    }

    @Test
    void answersTheGatewayHealthWithoutToken() {
        client.get().uri("/api/v1/gateway/health").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void servesTheApiDocumentationWithoutToken() {
        client.get().uri("/v3/api-docs").exchange().expectStatus().isOk();
        client.get().uri("/swagger-ui.html").exchange().expectStatus().is3xxRedirection();
    }

    @Test
    void stripsUserHeadersSentByTheClientOnPublicRoutes() {
        client.get().uri("/api/v1/authentication/sign-in")
                .header("X-User-Id", "00000000-0000-0000-0000-000000000000")
                .header("x-role", "ADMINISTRATOR")
                .header("X-Building-Id", JwtTestTokens.OTHER_BUILDING_ID)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(ECHO_PREFIX + "X-User-Id")
                .expectHeader().doesNotExist(ECHO_PREFIX + "X-Role")
                .expectHeader().doesNotExist(ECHO_PREFIX + "X-Building-Id");
    }

    @Test
    void replacesUserHeadersSentByTheClientWithTheOnesFromTheToken() {
        client.get().uri("/api/v1/buildings")
                .headers(headers -> headers.setBearerAuth(tokens.resident()))
                .header("X-User-Id", "00000000-0000-0000-0000-000000000000")
                .header("X-Role", "ADMINISTRATOR")
                .header("X-Building-Id", JwtTestTokens.OTHER_BUILDING_ID)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(ECHO_PREFIX + "X-User-Id", JwtTestTokens.USER_ID)
                .expectHeader().valueEquals(ECHO_PREFIX + "X-Role", "RESIDENT")
                .expectHeader().valueEquals(ECHO_PREFIX + "X-Building-Id", JwtTestTokens.BUILDING_ID);
    }

    @Test
    void forwardsTheAdministratorBuildingsAsACommaSeparatedList() {
        client.get().uri("/api/v1/readings")
                .headers(headers -> headers.setBearerAuth(tokens.administrator()))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(ECHO_PREFIX + "X-User-Id", JwtTestTokens.USER_ID)
                .expectHeader().valueEquals(ECHO_PREFIX + "X-Role", "ADMINISTRATOR")
                .expectHeader().valueEquals(ECHO_PREFIX + "X-Building-Id",
                        JwtTestTokens.BUILDING_ID + "," + JwtTestTokens.OTHER_BUILDING_ID);
    }

    private void expectInvalidToken(String token) {
        client.get().uri(PROTECTED_PATH)
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"")
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.detail").isEqualTo("A valid bearer token is required to access this resource.");
    }

    private void expectRouted(HttpMethod method, String path, String token) {
        client.method(method).uri(path)
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("routed");
    }
}
