package io.hoggmania.dashboard.resource;

import io.hoggmania.dashboard.model.ESA;
import io.hoggmania.dashboard.service.SvgService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/ui")
@RequestScoped
public class UIResource {

    @Inject
    SvgService svgService;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @Location("ui-form.html.qute")
    Template formTemplate;

    @Inject
    @Location("ui-result.html.qute")
    Template resultTemplate;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response form() {
        TemplateInstance ti = formTemplate.data("message", null).data("payload", "");
        String html = ti.render();
        return Response.ok(html).type(MediaType.TEXT_HTML).build();
    }

    @GET
    @Path("/sample")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sample() {
        String sample = "{\n" +
                "  \"title\": \"Application Security\",\n" +
                "  \"icon\": \"shield\",\n" +
                "  \"governance\": {\n" +
                "    \"title\": \"Application Security Governance\",\n" +
                "    \"components\": [\n" +
                "      {\"capability\":\"SAST\",\"name\":\"Static Code Scanning\",\"maturity\":\"MANAGED\",\"status\":\"HIGH\",\"icon\":\"search\",\"initiatives\":3,\"doubleBorder\":true,\"rag\":\"green\"},\n" +
                "      {\"capability\":\"RASP\",\"name\":\"RASP Agent\",\"maturity\":\"DEFINED\",\"status\":\"MEDIUM\",\"icon\":\"user\",\"initiatives\":0,\"doubleBorder\":false,\"rag\":\"amber\"}\n" +
                "    ]\n" +
                "  },\n" +
                "  \"capabilities\": {\n" +
                "    \"title\": \"Application Security Capabilities\",\n" +
                "    \"icon\": \"chart\",\n" +
                "    \"domains\": [\n" +
                "      {\n" +
                "        \"domain\":\"Application Security Testing\",\n" +
                "        \"icon\": \"group\",\n" +
                "        \"components\": [\n" +
                "          {\"capability\":\"SAST\",\"name\":\"Static Code Scanning\",\"maturity\":\"MANAGED\",\"status\":\"HIGH\",\"icon\":\"search\",\"initiatives\":5,\"doubleBorder\":true,\"rag\":\"green\"},\n" +
                "          {\"capability\":\"RASP\",\"name\":\"RASP Agent\",\"maturity\":\"DEFINED\",\"status\":\"MEDIUM\",\"icon\":\"user\",\"initiatives\":0,\"doubleBorder\":false,\"rag\":\"amber\"}\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"domain\":\"RASP Security Testing\",\n" +
                "        \"icon\": \"user\",\n" +
                "        \"components\": [\n" +
                "          {\"capability\":\"SAST\",\"name\":\"Static Code Scanning\",\"maturity\":\"REPEATABLE\",\"status\":\"LOW\",\"icon\":\"search\",\"initiatives\":2,\"doubleBorder\":false,\"rag\":\"amber\"},\n" +
                "          {\"capability\":\"RASP\",\"name\":\"RASP Agent\",\"maturity\":\"INITIAL\",\"status\":\"MEDIUM\",\"icon\":\"user\",\"initiatives\":1,\"doubleBorder\":false,\"rag\":\"red\"}\n" +
                "        ]\n" +
                "      },\n" +
                "      { \"domain\":\"SPACE\", \"components\": [] },\n" +
                "      {\n" +
                "        \"domain\":\"DAST Security Testing\",\n" +
                "        \"icon\": \"bug\",\n" +
                "        \"components\": [\n" +
                "          {\"capability\":\"SAST\",\"name\":\"Static Code Scanning\",\"maturity\":\"OPTIMISED\",\"status\":\"EFFECTIVE\",\"icon\":\"search\",\"initiatives\":4,\"doubleBorder\":false,\"rag\":\"green\"},\n" +
                "          {\"capability\":\"RASP\",\"name\":\"RASP Agent\",\"maturity\":\"DEFINED\",\"status\":\"MEDIUM\",\"icon\":\"user\",\"initiatives\":0,\"doubleBorder\":false,\"rag\":\"amber\"}\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}\n";
        return Response.ok(sample).build();
    }

    @POST
    @Path("/render")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response render(@FormParam("payload") String payload) {
        try {
            ESA esa = objectMapper.readValue(payload, ESA.class);
            String svg = svgService.renderSvg(esa);
            TemplateInstance ti = resultTemplate.data("svg", svg).data("payload", payload).data("error", null);
            String html = ti.render();
            return Response.ok(html).type(MediaType.TEXT_HTML).build();
        } catch (Exception e) {
            String error = e.getMessage();
            TemplateInstance ti = formTemplate.data("message", error).data("payload", payload == null ? "" : payload);
            String html = ti.render();
            return Response.status(Response.Status.BAD_REQUEST).entity(html).type(MediaType.TEXT_HTML).build();
        }
    }

    @POST
    @Path("/render-text")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_HTML)
    public Response renderText(String payload) {
        try {
            ESA esa = objectMapper.readValue(payload, ESA.class);
            String svg = svgService.renderSvg(esa);
            TemplateInstance ti = resultTemplate.data("svg", svg).data("payload", payload).data("error", null);
            String html = ti.render();
            return Response.ok(html).type(MediaType.TEXT_HTML).build();
        } catch (Exception e) {
            String error = e.getMessage();
            TemplateInstance ti = formTemplate.data("message", error).data("payload", payload == null ? "" : payload);
            String html = ti.render();
            return Response.status(Response.Status.BAD_REQUEST).entity(html).type(MediaType.TEXT_HTML).build();
        }
    }

    @POST
    @Path("/png")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("image/png")
    public Response downloadPng(@FormParam("payload") String payload) {
        try {
            ESA esa = objectMapper.readValue(payload, ESA.class);
            String svg = svgService.renderSvg(esa);
            byte[] png = svgService.renderPngFromSvg(svg, 96f);
            return Response.ok(png)
                    .header("Content-Disposition", "attachment; filename=dashboard.png")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(("Failed to render PNG: " + e.getMessage()).getBytes())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
    }
}
