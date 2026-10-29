package com.rumi.gateway.acceptance;

import com.rumi.gateway.support.StubServiceBackend;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Starts the gateway on a random port with every route pointing to the stub backend.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @DynamicPropertySource
    static void routeToStubBackend(DynamicPropertyRegistry registry) {
        StubServiceBackend.registerServiceUrls(registry);
    }
}
