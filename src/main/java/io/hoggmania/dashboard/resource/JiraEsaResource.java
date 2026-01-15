package io.hoggmania.dashboard.resource;

import io.hoggmania.dashboard.exception.ValidationException;
import io.hoggmania.dashboard.model.AttributePair;
import io.hoggmania.dashboard.model.ESA;
import io.hoggmania.dashboard.model.JiraEsaRequest;
import io.hoggmania.dashboard.service.JiraPayloadService;
import io.hoggmania.dashboard.util.StringUtils;
import io.hoggmania.dashboard.util.UrlUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/jira")
@Tag(name = "Jira", description = "Generate ESA JSON from Jira issues")
public class JiraEsaResource {

    private static final String SAMPLE_REQUEST = "{\n" +
            "  \"jiraUrl\": \"https://jira.example.com/browse/ESA-123\",\n" +
            "  \"jiraBase\": \"https://jira.example.com\",\n" +
            "  \"jiraToken\": \"<personal-access-token>\",\n" +
            "  \"headers\": [\n" +
            "    {\"name\": \"X-Atlassian-Token\", \"value\": \"no-check\"}\n" +
            "  ],\n" +
            "  \"attributes\": [\n" +
            "    {\"name\": \"owner\", \"value\": \"AppSec\"},\n" +
            "    {\"name\": \"portfolio\", \"value\": \"Security\"}\n" +
            "  ]\n" +
            "}";

    @Inject
    JiraPayloadService jiraPayloadService;

    @POST
    @Path("/esa")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Generate ESA JSON from Jira", description = "Builds the ESA JSON from a Jira root issue and optional attribute pairs.")
    @APIResponse(
            responseCode = "200",
            description = "ESA JSON payload",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ESA.class)))
    public Response generate(
            @RequestBody(required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = JiraEsaRequest.class),
                            examples = @ExampleObject(name = "request", value = SAMPLE_REQUEST)))
            JiraEsaRequest request) {
        if (request == null) {
            throw new ValidationException("Request body cannot be null.");
        }

        String jiraUrl = request.jiraUrl;
        String jiraBase = StringUtils.isBlank(request.jiraBase)
                ? UrlUtils.inferBaseUrl(jiraUrl)
                : request.jiraBase;

        Map<String, String> headers = normalizeHeaders(request.headers);
        ESA esa = jiraPayloadService.buildFromUrl(jiraBase, jiraUrl, request.jiraToken, headers);
        Map<String, String> attributes = normalizeAttributes(request.attributes);
        if (!attributes.isEmpty()) {
            esa.attributes = attributes;
        }
        return Response.ok(esa).build();
    }

    private Map<String, String> normalizeAttributes(List<AttributePair> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Map.of();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        for (AttributePair pair : attributes) {
            if (pair == null) {
                continue;
            }
            String name = pair.name != null ? pair.name.trim() : "";
            if (StringUtils.isBlank(name)) {
                throw new ValidationException("Attribute name cannot be blank.");
            }
            String value = pair.value == null ? "" : pair.value;
            normalized.put(name, value);
        }
        return normalized;
    }

    private Map<String, String> normalizeHeaders(List<AttributePair> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        for (AttributePair pair : headers) {
            if (pair == null) {
                continue;
            }
            String name = pair.name != null ? pair.name.trim() : "";
            if (StringUtils.isBlank(name)) {
                throw new ValidationException("Header name cannot be blank.");
            }
            String value = pair.value == null ? "" : pair.value;
            normalized.put(name, value);
        }
        return normalized;
    }
}
