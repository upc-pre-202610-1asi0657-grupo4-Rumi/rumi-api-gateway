package com.rumi.gateway.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.util.List;

/**
 * Stands in for every Rumi service. It answers 200 to any request and echoes the user headers it
 * received as {@code Echo-<header>} response headers, so a test can see what the gateway forwarded.
 */
public final class StubServiceBackend {

    public static final String ECHO_PREFIX = "Echo-";

    private static final List<String> ECHOED_HEADERS = List.of("X-User-Id", "X-Role", "X-Building-Id");
    private static final List<String> SERVICE_URL_VARIABLES = List.of(
            "BUILDING_SERVICE_URL", "MONITORING_SERVICE_URL", "SEISMIC_SERVICE_URL", "IAM_SERVICE_URL",
            "NOTIFICATION_SERVICE_URL", "SAFETY_SERVICE_URL", "BILLING_SERVICE_URL", "MAINTENANCE_SERVICE_URL");

    private static DisposableServer server;

    private StubServiceBackend() {
    }

    /**
     * Points every route of the gateway to the stub, starting it on first use. The server lives until
     * the JVM of the test run stops.
     */
    public static synchronized void registerServiceUrls(DynamicPropertyRegistry registry) {
        if (server == null) {
            server = HttpServer.create()
                    .port(0)
                    .handle((request, response) -> {
                        ECHOED_HEADERS.forEach(name -> {
                            String value = request.requestHeaders().get(name);
                            if (value != null) {
                                response.header(ECHO_PREFIX + name, value);
                            }
                        });
                        return response.sendString(Mono.just("routed"));
                    })
                    .bindNow();
        }
        String url = "http://localhost:" + server.port();
        SERVICE_URL_VARIABLES.forEach(variable -> registry.add(variable, () -> url));
    }
}
