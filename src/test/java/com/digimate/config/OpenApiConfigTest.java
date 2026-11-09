package com.digimate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenApiConfigTest {

    private OpenAPI documentWithAnswers() {
        Schema<?> requestSchema = new Schema<>().$ref("#/components/schemas/HelpRequestResponse");
        ApiResponses responses = new ApiResponses()
                .addApiResponse("200", new ApiResponse().description("ok")
                        .content(new Content().addMediaType("*/*", new MediaType().schema(requestSchema))))
                .addApiResponse("400", new ApiResponse().description("bad")
                        .content(new Content().addMediaType("*/*", new MediaType().schema(requestSchema))))
                .addApiResponse("401", new ApiResponse().description("not logged in")
                        .content(new Content().addMediaType("*/*", new MediaType().schema(requestSchema))))
                .addApiResponse("409", new ApiResponse().description("conflict")
                        .content(new Content().addMediaType("*/*", new MediaType().schema(requestSchema))));
        return new OpenAPI().paths(new Paths().addPathItem("/api/requests",
                new PathItem().get(new Operation().responses(responses))));
    }

    @Test
    void shouldShowTheErrorShapeForErrorAnswers() {
        OpenAPI openApi = documentWithAnswers();

        new OpenApiConfig().errorResponseCustomizer().customise(openApi);

        ApiResponses responses = openApi.getPaths().get("/api/requests").getGet().getResponses();
        assertEquals("#/components/schemas/ErrorResponse",
                responses.get("400").getContent().get("application/json").getSchema().get$ref());
        assertEquals("#/components/schemas/ErrorResponse",
                responses.get("409").getContent().get("application/json").getSchema().get$ref());
        assertNotNull(openApi.getComponents().getSchemas().get("ErrorResponse"));
    }

    @Test
    void shouldKeepTheSuccessAnswerAndShowNoBodyFor401() {
        OpenAPI openApi = documentWithAnswers();

        new OpenApiConfig().errorResponseCustomizer().customise(openApi);

        ApiResponses responses = openApi.getPaths().get("/api/requests").getGet().getResponses();
        assertEquals("#/components/schemas/HelpRequestResponse",
                responses.get("200").getContent().get("*/*").getSchema().get$ref());
        assertNull(responses.get("401").getContent());
    }
}