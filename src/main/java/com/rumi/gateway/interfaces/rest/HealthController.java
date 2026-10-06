package com.rumi.gateway.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health", description = "Liveness of the gateway")
public class HealthController {

    static final String SERVICE_NAME = "rumi-api-gateway";

    @Operation(
            summary = "Check that the gateway is running",
            description = "Answered by the gateway itself, without routing to any service. "
                    + "It is not a functional endpoint of the platform."
    )
    @ApiResponse(
            responseCode = "200",
            description = "The gateway is running",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = HealthResponse.class),
                    examples = @ExampleObject(
                            name = "Gateway up",
                            value = """
                                    {
                                      "status": "UP",
                                      "service": "rumi-api-gateway"
                                    }"""
                    )
            )
    )
    @GetMapping("/api/v1/gateway/health")
    public HealthResponse health() {
        return new HealthResponse("UP", SERVICE_NAME);
    }
}
