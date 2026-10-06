package com.rumi.gateway.interfaces.rest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "HealthResponse", description = "Liveness information of the gateway")
public record HealthResponse(
        @Schema(description = "Liveness status", example = "UP")
        String status,
        @Schema(description = "Name of the service", example = "rumi-api-gateway")
        String service
) {
}
