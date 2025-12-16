package io.hoggmania.dashboard.resource;

import io.hoggmania.dashboard.model.ESA;
import io.hoggmania.dashboard.service.SvgService;
import io.hoggmania.dashboard.service.InitiativesPageService;
import io.hoggmania.dashboard.service.JiraPayloadService;
import io.hoggmania.dashboard.model.ESA;
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
    InitiativesPageService initiativesPageService;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @Location("ui-form.html.qute")
    Template formTemplate;

    @Inject
    @Location("ui-result.html.qute")
    Template resultTemplate;

    @Inject
    @Location("jira-form.html.qute")
    Template jiraTemplate;

    @Inject
    JiraPayloadService jiraPayloadService;

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
        return Response.ok(DashboardResource.SAMPLE_PAYLOAD).build();
    }

    @POST
    @Path("/render")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response render(@FormParam("payload") String payload) {
        try {
            ESA esa = objectMapper.readValue(payload, ESA.class);
            String svg = svgService.renderSvg(esa);
            String initiatives = initiativesPageService.renderInitiativesFragment(esa);
            TemplateInstance ti = resultTemplate
                .data("svg", svg)
                .data("initiatives", initiatives)
                .data("payload", payload)
                .data("error", null);
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
            String initiatives = initiativesPageService.renderInitiativesFragment(esa);
            TemplateInstance ti = resultTemplate
                .data("svg", svg)
                .data("initiatives", initiatives)
                .data("payload", payload)
                .data("error", null);
            String html = ti.render();
            return Response.ok(html).type(MediaType.TEXT_HTML).build();
        } catch (Exception e) {
            String error = e.getMessage();
            TemplateInstance ti = formTemplate.data("message", error).data("payload", payload == null ? "" : payload);
            String html = ti.render();
            return Response.status(Response.Status.BAD_REQUEST).entity(html).type(MediaType.TEXT_HTML).build();
        }
    }

    @GET
    @Path("/jira")
    @Produces(MediaType.TEXT_HTML)
    public Response jiraForm() {
        String html = jiraTemplate
                .data("message", null)
                .data("payload", null)
                .data("jiraUrl", "")
                .data("jiraBase", "")
                .render();
        return Response.ok(html).type(MediaType.TEXT_HTML).build();
    }

    @POST
    @Path("/jira/generate")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response jiraGenerate(@FormParam("jiraBase") String jiraBase,
                                 @FormParam("jiraUrl") String jiraUrl,
                                 @FormParam("jiraToken") String jiraToken) {
        try {
            ESA esa = jiraPayloadService.buildFromUrl(jiraBase, jiraUrl, jiraToken);
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(esa);
            String html = jiraTemplate
                    .data("message", null)
                    .data("payload", json)
                    .data("jiraUrl", jiraUrl)
                    .data("jiraBase", jiraBase)
                    .render();
            return Response.ok(html).type(MediaType.TEXT_HTML).build();
        } catch (Exception e) {
            String html = jiraTemplate
                    .data("message", e.getMessage())
                    .data("payload", null)
                    .data("jiraUrl", jiraUrl == null ? "" : jiraUrl)
                    .data("jiraBase", jiraBase == null ? "" : jiraBase)
                    .render();
            return Response.status(Response.Status.BAD_REQUEST).entity(html).type(MediaType.TEXT_HTML).build();
        }
    }

    @POST
    @Path("/render-initiatives")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_HTML)
    public Response renderInitiatives(String payload) {
        try {
            ESA esa = objectMapper.readValue(payload, ESA.class);
            String html = initiativesPageService.renderInitiativesPage(esa, payload);
            return Response.ok(html).type(MediaType.TEXT_HTML).build();
        } catch (Exception e) {
            String error = e.getMessage();
            TemplateInstance ti = formTemplate.data("message", error).data("payload", payload == null ? "" : payload);
            String html = ti.render();
            return Response.status(Response.Status.BAD_REQUEST).entity(html).type(MediaType.TEXT_HTML).build();
        }
    }

    @POST
    @Path("/svg")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces("image/svg+xml")
    public Response downloadSvg(String payload) {
        try {
            ESA esa = objectMapper.readValue(payload, ESA.class);
            String svg = svgService.renderSvg(esa);
            return Response.ok(svg)
                    .header("Content-Disposition", "attachment; filename=dashboard.svg")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(("Failed to render SVG: " + e.getMessage()).getBytes())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
    }
}
