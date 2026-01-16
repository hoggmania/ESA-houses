package io.hoggmania.dashboard.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hoggmania.dashboard.model.ESA;
import io.hoggmania.dashboard.service.SvgService;
import io.hoggmania.dashboard.service.InitiativesPageService;
import io.hoggmania.dashboard.exception.ValidationException;
import io.quarkus.logging.Log;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Base64;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Render dashboard SVG or PNG from a JSON model")
public class DashboardResource {

    public static final String SAMPLE_PAYLOAD = "{\n" +
        "  \"title\": \"Application Security\",\n" +
        "  \"icon\": \"shield\",\n" +
        "  \"governance\": {\n" +
        "    \"title\": \"Application Security Governance\",\n" +
        "    \"components\": [\n" +
        "      {\n" +
        "        \"name\": \"Static Code Scanning\",\n" +
        "        \"maturity\": \"MANAGED\",\n" +
        "        \"status\": \"HIGH\",\n" +
        "        \"icon\": \"search\",\n" +
        "        \"initiatives\": 2,\n" +
        "        \"iRag\": \"green\",\n" +
        "        \"rag\": \"green\",\n" +
        "        \"initiative\": [\n" +
        "          {\n" +
        "            \"key\": \"AS-001\",\n" +
        "            \"link\": \"https://example.com/initiatives/AS-001\",\n" +
        "            \"summary\": \"Expand SAST coverage\",\n" +
        "            \"businessBenefit\": \"Detect critical issues earlier\",\n" +
        "            \"riskAppetite\": \"Low\",\n" +
        "            \"toolId\": \"In-Demand\",\n" +
        "            \"dueDate\": \"2024-12-15\",\n" +
        "            \"rag\": \"green\"\n" +
        "          },\n" +
        "          {\n" +
        "            \"key\": \"AS-001b\",\n" +
        "            \"link\": \"https://example.com/initiatives/AS-001b\",\n" +
        "            \"summary\": \"Introduce SAST guardrails\",\n" +
        "            \"businessBenefit\": \"Improve self-service onboarding\",\n" +
        "            \"riskAppetite\": \"Low\",\n" +
        "            \"toolId\": \"In-Demand\",\n" +
        "            \"dueDate\": \"2025-04-30\",\n" +
        "            \"rag\": \"green\"\n" +
        "          }\n" +
        "        ]\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"RASP Agent\",\n" +
        "        \"maturity\": \"DEFINED\",\n" +
        "        \"status\": \"MEDIUM\",\n" +
        "        \"icon\": \"user\",\n" +
        "        \"initiatives\": 1,\n" +
        "        \"iRag\": \"amber\",\n" +
        "        \"rag\": \"amber\",\n" +
        "        \"initiative\": [\n" +
        "          {\n" +
        "            \"key\": \"AS-002\",\n" +
        "            \"link\": \"https://example.com/initiatives/AS-002\",\n" +
        "            \"summary\": \"Pilot runtime agents\",\n" +
        "            \"businessBenefit\": \"Reduce exploitation risk\",\n" +
        "            \"riskAppetite\": \"Medium\",\n" +
        "            \"toolId\": \"In-Demand\",\n" +
        "            \"dueDate\": \"2025-02-01\",\n" +
        "            \"rag\": \"amber\"\n" +
        "          }\n" +
        "        ]\n" +
        "      }\n" +
        "    ]\n" +
        "  },\n" +
        "  \"capabilities\": {\n" +
        "    \"title\": \"Application Security Capabilities\",\n" +
        "    \"domains\": [\n" +
        "      {\n" +
        "        \"domain\": \"Application Security Testing\",\n" +
        "        \"components\": [\n" +
        "          {\n" +
        "            \"name\": \"Static Code Scanning\",\n" +
        "            \"maturity\": \"MANAGED\",\n" +
        "            \"status\": \"HIGH\",\n" +
        "            \"icon\": \"search\",\n" +
        "            \"initiatives\": 3,\n" +
        "            \"iRag\": \"green\",\n" +
        "            \"rag\": \"green\",\n" +
        "            \"initiative\": [\n" +
        "              {\n" +
        "                \"key\": \"AS-003\",\n" +
        "                \"link\": \"https://example.com/initiatives/AS-003\",\n" +
        "                \"summary\": \"Automate onboarding\",\n" +
        "                \"businessBenefit\": \"Increase coverage quickly\",\n" +
        "                \"riskAppetite\": \"Low\",\n" +
        "                \"toolId\": \"In-Demand\",\n" +
        "                \"dueDate\": \"2025-01-20\",\n" +
        "                \"rag\": \"green\"\n" +
        "              }\n" +
        "            ]\n" +
        "          },\n" +
        "          {\n" +
        "            \"name\": \"RASP Agent\",\n" +
        "            \"maturity\": \"DEFINED\",\n" +
        "            \"status\": \"MEDIUM\",\n" +
        "            \"icon\": \"user\",\n" +
        "            \"initiatives\": 2,\n" +
        "            \"iRag\": \"amber\",\n" +
        "            \"rag\": \"amber\",\n" +
        "            \"initiative\": [\n" +
        "              {\n" +
        "                \"key\": \"AS-004\",\n" +
        "                \"link\": \"https://example.com/initiatives/AS-004\",\n" +
        "                \"summary\": \"Expand telemetry\",\n" +
        "                \"businessBenefit\": \"Improve runtime visibility\",\n" +
        "                \"riskAppetite\": \"Medium\",\n" +
        "                \"toolId\": \"In-Demand\",\n" +
        "                \"dueDate\": \"2024-11-30\",\n" +
        "                \"rag\": \"amber\"\n" +
        "              },\n" +
        "              {\n" +
        "                \"key\": \"AS-004b\",\n" +
        "                \"link\": \"https://example.com/initiatives/AS-004b\",\n" +
        "                \"summary\": \"Codify alert playbooks\",\n" +
        "                \"businessBenefit\": \"Accelerate runtime response\",\n" +
        "                \"riskAppetite\": \"Medium\",\n" +
        "                \"toolId\": \"In-Demand\",\n" +
        "                \"dueDate\": \"2025-03-15\",\n" +
        "                \"rag\": \"amber\"\n" +
        "              }\n" +
        "            ]\n" +
        "          }\n" +
        "        ]\n" +
        "      }\n" +
        "    ]\n" +
        "  }\n" +
        "}";

    @Inject
    SvgService svgService;

    @Inject
    InitiativesPageService initiativesPageService;

    @Inject
    ObjectMapper mapper;

    @Inject
    @Location("preview.html.qute")
    Template previewTemplate;

    @POST
    @Path("/svg")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("image/svg+xml")
    @Operation(summary = "Render SVG", description = "Renders an SVG dashboard using the provided JSON model")
    @APIResponse(responseCode = "200", description = "SVG image", content = @Content(mediaType = "image/svg+xml"))
    public Response svg(
            @RequestBody(required = true,
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ESA.class),
                    examples = {
                        @ExampleObject(name = "sample",
                            description = "Sample hierarchical payload with nested initiatives",
                            value = SAMPLE_PAYLOAD)
                    }
                )
            ) JsonNode model) {
        // Validate input
        if (model == null || model.isNull()) {
            throw new ValidationException("Request body cannot be null or empty");
        }
        
        // Parse full JSON into ESA root DTO first
        ESA esa = mapper.convertValue(model, ESA.class);
        String svg = svgService.renderSvg(esa);
        return Response.ok(svg).build();
    }

    @POST
    @Path("/png")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("image/png")
    @Operation(summary = "Render PNG", description = "Renders a PNG by first generating an SVG then converting via Batik")
    @APIResponse(responseCode = "200", description = "PNG image", content = @Content(mediaType = "image/png"))
    public Response png(
            @RequestBody(required = true,
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ESA.class),
                    examples = {
                        @ExampleObject(name = "sample",
                            description = "Sample hierarchical payload with nested initiatives",
                            value = SAMPLE_PAYLOAD)
                    }
                )
            ) JsonNode model) throws Exception {
        // Validate input
        if (model == null || model.isNull()) {
            throw new ValidationException("Request body cannot be null or empty");
        }
        
        // Parse full JSON into ESA root DTO first
        ESA esa = mapper.convertValue(model, ESA.class);
        String svg = svgService.renderSvg(esa);
        byte[] png = svgService.renderPngFromSvg(svg, 150f);
        return Response.ok(png).build();
    }

    @POST
    @Path("/preview")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_HTML)
    @Operation(summary = "Preview Dashboard", description = "Renders an HTML page with inline SVG and PNG previews")
    @APIResponse(responseCode = "200", description = "HTML preview page", content = @Content(mediaType = MediaType.TEXT_HTML))
    public Response preview(
            @RequestBody(required = true,
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ESA.class),
                    examples = {
                        @ExampleObject(name = "sample",
                            description = "Sample hierarchical payload with nested initiatives",
                            value = SAMPLE_PAYLOAD)
                    }
                )
            ) JsonNode model) throws Exception {
        // Validate input
        if (model == null || model.isNull()) {
            throw new ValidationException("Request body cannot be null or empty");
        }

        // Parse full JSON into ESA root DTO first
        ESA esa = mapper.convertValue(model, ESA.class);
        String svg = svgService.renderSvg(esa);
        byte[] png = svgService.renderPngFromSvg(svg, 150f);
        String pngBase64 = Base64.getEncoder().encodeToString(png);
        
        String html = previewTemplate
            .data("svg", svg)
            .data("pngBase64", pngBase64)
            .render();
        
        return Response.ok(html).build();
    }

    @POST
    @Path("/initiatives")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_HTML)
    @Operation(summary = "Render initiatives page", description = "Renders an HTML page listing every embedded initiative.")
    @APIResponse(responseCode = "200", description = "HTML initiatives table", content = @Content(mediaType = MediaType.TEXT_HTML))
    public Response initiatives(
            @RequestBody(required = true,
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ESA.class),
                    examples = {
                        @ExampleObject(name = "sample",
                            description = "Payload containing initiative arrays under each component",
                            value = SAMPLE_PAYLOAD)
                    }
                )
            ) JsonNode model) {
        if (model == null || model.isNull()) {
            throw new ValidationException("Request body cannot be null or empty");
        }
        ESA esa = mapper.convertValue(model, ESA.class);
        String payloadRaw = null;
        try {
            payloadRaw = mapper.writeValueAsString(model);
        } catch (Exception e) {
            Log.warn("Failed to serialize payload for display", e);
        }
        String html = initiativesPageService.renderInitiativesPage(esa, payloadRaw);
        return Response.ok(html).build();
    }
}
