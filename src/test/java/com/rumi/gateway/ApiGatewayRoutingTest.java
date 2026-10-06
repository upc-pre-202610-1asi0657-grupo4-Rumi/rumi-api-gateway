package com.rumi.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApiGatewayRoutingTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void theBuildingRiskIndexRouteIsEvaluatedBeforeTheGenericBuildingRoute() {
        List<String> routeIds = routeLocator.getRoutes().map(Route::getId).collectList().block();

        assertThat(routeIds).hasSize(9);
        assertThat(routeIds.indexOf("seismic-service-building-risk-indexes"))
                .isLessThan(routeIds.indexOf("building-service"));
    }

    @ParameterizedTest
    @CsvSource({
            "/api/v1/buildings/3f2c8a10/risk-indexes, 8083",
            "/api/v1/buildings/3f2c8a10/risk-indexes/latest, 8083",
            "/api/v1/buildings, 8081",
            "/api/v1/buildings/3f2c8a10, 8081",
            "/api/v1/buildings/3f2c8a10/sensors, 8081",
            "/api/v1/sensors/1, 8081",
            "/api/v1/invitations, 8081",
            "/api/v1/readings, 8082",
            "/api/v1/digital-twin/zones, 8082",
            "/api/v1/seismic-events, 8083",
            "/api/v1/risk-indexes/1, 8083",
            "/api/v1/reports, 8083",
            "/api/v1/authentication/sign-in, 8084",
            "/api/v1/users/1, 8084",
            "/api/v1/device-tokens, 8084",
            "/api/v1/alerts, 8085",
            "/api/v1/thresholds, 8085",
            "/api/v1/evacuation-plans, 8086",
            "/api/v1/checklists, 8086",
            "/api/v1/subscriptions, 8087",
            "/api/v1/payments, 8087",
            "/api/v1/inspections, 8088",
            "/api/v1/damage-reports, 8088"
    })
    void routesEachPathPrefixToTheServiceThatOwnsIt(String path, int expectedPort) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path));

        Route route = routeLocator.getRoutes()
                .filterWhen(candidate -> Flux.from(candidate.getPredicate().apply(exchange)))
                .blockFirst();

        assertThat(route).as("route for %s", path).isNotNull();
        assertThat(route.getUri().getPort()).isEqualTo(expectedPort);
    }
}
