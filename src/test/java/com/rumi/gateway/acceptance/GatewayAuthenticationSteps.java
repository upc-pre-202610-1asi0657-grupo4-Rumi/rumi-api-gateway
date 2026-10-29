package com.rumi.gateway.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumi.gateway.support.JwtTestTokens;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static com.rumi.gateway.support.StubServiceBackend.ECHO_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

public class GatewayAuthenticationSteps {

    @Autowired
    private WebTestClient client;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${rumi.security.jwt.secret}")
    private String secret;

    private final HttpHeaders requestHeaders = new HttpHeaders();
    private EntityExchangeResult<String> result;

    @Given("the client has no token")
    public void theClientHasNoToken() {
        requestHeaders.remove(HttpHeaders.AUTHORIZATION);
    }

    @Given("the client holds a(n) {string} token")
    public void theClientHoldsAToken(String kind) {
        JwtTestTokens tokens = new JwtTestTokens(secret);
        String token = switch (kind) {
            case "administrator" -> tokens.administrator();
            case "administrator without buildings" -> tokens.token("ADMINISTRATOR", List.of());
            case "resident" -> tokens.resident();
            case "malformed" -> "not-a-jwt";
            case "signed with another secret" -> tokens.signedWithAnotherSecret();
            case "expired" -> tokens.expired();
            default -> throw new IllegalArgumentException("Unknown token kind: " + kind);
        };
        requestHeaders.setBearerAuth(token);
    }

    @And("the client sends the header {string} with {string}")
    public void theClientSendsTheHeader(String name, String value) {
        requestHeaders.set(name, value);
    }

    @When("the client sends a {word} request to {string}")
    public void theClientSendsARequest(String method, String path) {
        result = client.method(HttpMethod.valueOf(method)).uri(path)
                .headers(headers -> headers.addAll(requestHeaders))
                .exchange()
                .expectBody(String.class)
                .returnResult();
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertThat(result.getStatus().value()).isEqualTo(status);
    }

    @And("the response is a problem detail with the message {string}")
    public void theResponseIsAProblemDetail(String message) throws Exception {
        assertThat(result.getResponseHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        JsonNode problem = objectMapper.readTree(result.getResponseBody());
        assertThat(problem.path("status").asInt()).isEqualTo(result.getStatus().value());
        assertThat(problem.path("detail").asText()).isEqualTo(message);
    }

    @Then("the request reaches the service")
    public void theRequestReachesTheService() {
        assertThat(result.getStatus().value()).isEqualTo(200);
        assertThat(result.getResponseBody()).isEqualTo("routed");
    }

    @And("the service receives the header {string} with {string}")
    public void theServiceReceivesTheHeader(String name, String value) {
        assertThat(result.getResponseHeaders().getFirst(ECHO_PREFIX + name)).isEqualTo(value);
    }

    @And("the service does not receive the header {string}")
    public void theServiceDoesNotReceiveTheHeader(String name) {
        assertThat(result.getResponseHeaders().containsKey(ECHO_PREFIX + name)).isFalse();
    }
}
