package com.rumi.gateway.infrastructure;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI apiGatewayOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Rumi API Gateway")
                .description("Single entry point of the Rumi platform. It only documents its own health endpoint; "
                        + "the API of each bounded context is documented by the service that owns it.")
                .version("0.1.0"));
    }
}
