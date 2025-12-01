package io.hoggmania.dashboard.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hoggmania.dashboard.model.ESA;
import io.hoggmania.dashboard.service.SvgService;
import io.hoggmania.dashboard.exception.ValidationException;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Base64;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Render dashboard SVG or PNG from a JSON model")
public class DashboardResource {

    @Inject
    SvgService svgService;

    @Inject
    ObjectMapper mapper;

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
                    examples = {
                        @ExampleObject(name = "sample",
                            description = "Sample hierarchical payload with valid Status and Maturity enum values",
                            value = "{\n    \"title\": \"Application Security\",\n    \"icon\": \"shield\",\n    \"governance\": {\n        \"title\": \"Application Security Governance\",\n        \"components\": [\n            {\"capability\":\"SAST\",\"name\":\"Static Code Scanning\",\"maturity\":\"MANAGED\",\"status\":\"HIGH\",\"icon\":\"search\",\"initiatives\":3,\"doubleBorder\":true,\"rag\":\"green\"},\n            {\"capability\":\"RASP\",\"name\":\"RASP Agent\",\"maturity\":\"DEFINED\",\"status\":\"MEDIUM\",\"icon\":\"user\",\"initiatives\":0,\"doubleBorder\":false,\"rag\":\"amber\"}\n        ]\n    },\n    \"capabilities\": {\n      \"title\": \"Application Security Capabilities\",\n      \"icon\": \"chart\",\n      \"domains\": [\n        {\n                \"domain\":\"Application Security Testing\",\n                \"icon\": \"group\",\n                \"components\": [\n                    {\"capability\":\"SAST\",\"name\":\"Static Code Scanning\",\"maturity\":\"MANAGED\",\"status\":\"HIGH\",\"icon\":\"search\",\"initiatives\":5,\"doubleBorder\":true,\"rag\":\"green\"},\n                    {\"capability\":\"DAST\",\"name\":\"Dynamic Scanning\",\"maturity\":\"DEFINED\",\"status\":\"EFFECTIVE\",\"icon\":\"bug\",\"initiatives\":2,\"doubleBorder\":false,\"rag\":\"green\"}\n                ]\n            },\n            {\n                \"domain\":\"Runtime Protection\",\n                \"icon\": \"user\",\n                \"components\": [\n                    {\"capability\":\"RASP\",\"name\":\"RASP Agent\",\"maturity\":\"REPEATABLE\",\"status\":\"LOW\",\"icon\":\"user\",\"initiatives\":2,\"doubleBorder\":false,\"rag\":\"amber\"},\n                    {\"capability\":\"WAF\",\"name\":\"Web Application Firewall\",\"maturity\":\"INITIAL\",\"status\":\"MEDIUM\",\"icon\":\"firewall\",\"initiatives\":1,\"doubleBorder\":false,\"rag\":\"red\"}\n                ]\n            }\n        ]\n    } \n}"
                        )
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
                    examples = {
                        @ExampleObject(name = "sample",
                            description = "Sample hierarchical payload with valid Status and Maturity enum values",
                            value = "{\n    \"title\": \"Application Security\",\n    \"icon\": \"shield\",\n    \"governance\": {\n        \"title\": \"Application Security Governance\",\n        \"components\": [\n            {\"capability\":\"SAST\",\"name\":\"Static Code Scanning\",\"maturity\":\"MANAGED\",\"status\":\"HIGH\",\"icon\":\"search\",\"initiatives\":3,\"doubleBorder\":true,\"rag\":\"green\"},\n            {\"capability\":\"RASP\",\"name\":\"RASP Agent\",\"maturity\":\"DEFINED\",\"status\":\"MEDIUM\",\"icon\":\"user\",\"initiatives\":0,\"doubleBorder\":false,\"rag\":\"amber\"}\n        ]\n    },\n    \"capabilities\": {\n      \"title\": \"Application Security Capabilities\",\n      \"icon\": \"chart\",\n      \"domains\": [\n        {\n                \"domain\":\"Application Security Testing\",\n                \"icon\": \"group\",\n                \"components\": [\n                    {\"capability\":\"SAST\",\"name\":\"Static Code Scanning\",\"maturity\":\"MANAGED\",\"status\":\"HIGH\",\"icon\":\"search\",\"initiatives\":5,\"doubleBorder\":true,\"rag\":\"green\"},\n                    {\"capability\":\"DAST\",\"name\":\"Dynamic Scanning\",\"maturity\":\"DEFINED\",\"status\":\"EFFECTIVE\",\"icon\":\"bug\",\"initiatives\":2,\"doubleBorder\":false,\"rag\":\"green\"}\n                ]\n            },\n            {\n                \"domain\":\"Runtime Protection\",\n                \"icon\": \"user\",\n                \"components\": [\n                    {\"capability\":\"RASP\",\"name\":\"RASP Agent\",\"maturity\":\"REPEATABLE\",\"status\":\"LOW\",\"icon\":\"user\",\"initiatives\":2,\"doubleBorder\":false,\"rag\":\"amber\"},\n                    {\"capability\":\"WAF\",\"name\":\"Web Application Firewall\",\"maturity\":\"INITIAL\",\"status\":\"MEDIUM\",\"icon\":\"firewall\",\"initiatives\":1,\"doubleBorder\":false,\"rag\":\"red\"}\n                ]\n            }\n        ]\n    } \n}"
                        )
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
                    examples = {
                        @ExampleObject(name = "sample",
                            description = "Sample hierarchical payload with valid Status and Maturity enum values",
                            value = "{\n    \"title\": \"Application Security\",\n    \"icon\": \"shield\",\n    \"governance\": {\n        \"title\": \"Application Security Governance\",\n        \"components\": [\n            {\"capability\":\"SAST\",\"name\":\"Static Code Scanning\",\"maturity\":\"MANAGED\",\"status\":\"HIGH\",\"icon\":\"search\",\"initiatives\":3,\"doubleBorder\":true,\"rag\":\"green\"},\n            {\"capability\":\"RASP\",\"name\":\"RASP Agent\",\"maturity\":\"DEFINED\",\"status\":\"MEDIUM\",\"icon\":\"user\",\"initiatives\":0,\"doubleBorder\":false,\"rag\":\"amber\"}\n        ]\n    },\n    \"capabilities\": {\n      \"title\": \"Application Security Capabilities\",\n      \"icon\": \"chart\",\n      \"domains\": [\n        {\n                \"domain\":\"Application Security Testing\",\n                \"icon\": \"group\",\n                \"components\": [\n                    {\"capability\":\"SAST\",\"name\":\"Static Code Scanning\",\"maturity\":\"MANAGED\",\"status\":\"HIGH\",\"icon\":\"search\",\"initiatives\":5,\"doubleBorder\":true,\"rag\":\"green\"},\n                    {\"capability\":\"DAST\",\"name\":\"Dynamic Scanning\",\"maturity\":\"DEFINED\",\"status\":\"EFFECTIVE\",\"icon\":\"bug\",\"initiatives\":2,\"doubleBorder\":false,\"rag\":\"green\"}\n                ]\n            },\n            {\n                \"domain\":\"Runtime Protection\",\n                \"icon\": \"user\",\n                \"components\": [\n                    {\"capability\":\"RASP\",\"name\":\"RASP Agent\",\"maturity\":\"REPEATABLE\",\"status\":\"LOW\",\"icon\":\"user\",\"initiatives\":2,\"doubleBorder\":false,\"rag\":\"amber\"},\n                    {\"capability\":\"WAF\",\"name\":\"Web Application Firewall\",\"maturity\":\"INITIAL\",\"status\":\"MEDIUM\",\"icon\":\"firewall\",\"initiatives\":1,\"doubleBorder\":false,\"rag\":\"red\"}\n                ]\n            }\n        ]\n    } \n}"
                        )
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
        
        String html = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "  <meta charset='UTF-8'>\n" +
            "  <title>Dashboard Preview</title>\n" +
            "  <style>\n" +
            "    body { font-family: sans-serif; margin: 20px; background: #f5f5f5; }\n" +
            "    h1 { color: #333; }\n" +
            "    .preview { background: white; padding: 20px; margin: 20px 0; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n" +
            "    .preview h2 { margin-top: 0; color: #1E3A8A; }\n" +
            "    img { max-width: 100%; border: 1px solid #ddd; }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "  <h1>Dashboard Preview</h1>\n" +
            "  <div class='preview'>\n" +
            "    <h2>SVG Output</h2>\n" +
            "    " + svg + "\n" +
            "  </div>\n" +
            "  <div class='preview'>\n" +
            "    <h2>PNG Output (converted from SVG)</h2>\n" +
            "    <img src='data:image/png;base64," + pngBase64 + "' alt='Dashboard PNG'/>\n" +
            "  </div>\n" +
            "</body>\n" +
            "</html>";
        
        return Response.ok(html).build();
    }
}
