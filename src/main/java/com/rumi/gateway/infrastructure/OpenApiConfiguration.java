package com.rumi.gateway.infrastructure;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class OpenApiConfiguration {

    public static final String BEARER_JWT = "bearerJwt";

    private static final String PROBLEM_JSON = org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Bean
    public OpenAPI apiGatewayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Rumi API Gateway")
                        .description("Single entry point of the Rumi platform. It only documents its own health endpoint; "
                                + "the API of each bounded context is documented by the service that owns it. "
                                + "Every routed path except /api/v1/authentication/** requires a bearer JWT issued by "
                                + "rumi-iam-service; the gateway answers 401 (Unauthorized) for a missing, malformed, "
                                + "badly signed or expired token and 403 (Forbidden) when the role is not allowed.")
                        .version("0.1.0"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT))
                .components(new Components()
                        .addSecuritySchemes(BEARER_JWT, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("HS256 token issued by rumi-iam-service. Claims: sub (user id), "
                                        + "role (ADMINISTRATOR or RESIDENT) and buildingIds (list of building ids)."))
                        .addSchemas("ProblemDetail", problemDetailSchema())
                        .addResponses("Unauthorized", problemResponse(
                                "Missing, malformed, badly signed or expired token", 401, "Unauthorized",
                                "A valid bearer token is required to access this resource."))
                        .addResponses("Forbidden", problemResponse(
                                "Valid token, but its role is not allowed on this path and method", 403, "Forbidden",
                                "Your role is not allowed to access this resource.")));
    }

    private static Schema<?> problemDetailSchema() {
        Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put("type", new StringSchema().example("about:blank"));
        properties.put("title", new StringSchema().example("Unauthorized"));
        properties.put("status", new IntegerSchema().example(401));
        properties.put("detail", new StringSchema().example("A valid bearer token is required to access this resource."));
        properties.put("instance", new StringSchema().example("/api/v1/alerts"));
        return new ObjectSchema()
                .description("Error answer of the gateway (RFC 9457)")
                .properties(properties);
    }

    private static ApiResponse problemResponse(String description, int status, String title, String detail) {
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "about:blank");
        problem.put("title", title);
        problem.put("status", status);
        problem.put("detail", detail);
        problem.put("instance", "/api/v1/alerts");
        Example example = new Example().value(problem);
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(PROBLEM_JSON, new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                        .addExamples(title, example)));
    }
}
