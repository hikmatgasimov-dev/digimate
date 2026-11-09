package com.digimate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.digimate.exception.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
@Configuration
public class OpenApiConfig {


    @Bean
    public OpenAPI digimateOpenAPI() {
        String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("DigiMate API")
                        .version("1.0.0")
                        .description("API documentation for the DigiMate application. "
                                + "Log in with POST /api/auth/login, then click Authorize and paste the token."))
                .components(new Components()
                        .addSecuritySchemes(
                                securitySchemeName,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ))
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(securitySchemeName)
                );
    }

    /**
     * Swagger shows the success type as the example for every answer. Our errors have their own shape
     * (timestamp, status, message, path), so every 4xx and 5xx answer points to ErrorResponse.
     * A 401 has no body (the security check answers before our code), so it shows none.
     */
    @Bean
    public OpenApiCustomizer errorResponseCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            ModelConverters.getInstance().read(ErrorResponse.class)
                    .forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));

            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                if (operation.getResponses() == null) {
                    return;
                }
                operation.getResponses().forEach((code, response) -> {
                    if ("401".equals(code)) {
                        response.setContent(null);
                    } else if (code.startsWith("4") || code.startsWith("5")) {
                        Schema<?> reference = new Schema<>().$ref("#/components/schemas/ErrorResponse");
                        response.setContent(new Content()
                                .addMediaType("application/json", new MediaType().schema(reference)));
                    }
                });
            }));
        };
    }
}