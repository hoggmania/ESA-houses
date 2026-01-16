package io.hoggmania.dashboard.resource;

import io.hoggmania.dashboard.model.ESA;
import io.hoggmania.dashboard.service.SvgService;
import io.hoggmania.dashboard.service.InitiativesPageService;
import io.hoggmania.dashboard.service.JiraDiscoveryService;
import io.hoggmania.dashboard.service.JiraPayloadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hoggmania.dashboard.exception.ValidationException;
import io.hoggmania.dashboard.model.JiraRootIssue;
import io.hoggmania.dashboard.util.StringUtils;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Inject
    JiraDiscoveryService jiraDiscoveryService;

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
                .data("jiraHeaders", "")
                .data("rootIssues", null)
                .data("selectedRootKey", "")
                .render();
        return Response.ok(html).type(MediaType.TEXT_HTML).build();
    }

    @POST
    @Path("/jira/discover")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response jiraDiscover(@FormParam("jiraBase") String jiraBase,
                                 @FormParam("jiraUrl") String jiraUrl,
                                 @FormParam("jiraToken") String jiraToken,
                                 @FormParam("jiraHeaders") String jiraHeaders,
                                 @FormParam("jiraRootKey") String jiraRootKey) {
        try {
            Map<String, String> headers = parseHeaders(jiraHeaders);
            List<JiraRootIssue> roots = jiraDiscoveryService.findEsaRootIssues(jiraBase, jiraToken, headers);
            String message = roots.isEmpty() ? "No ESA-Root issues found." : null;
            String html = jiraTemplate
                    .data("message", message)
                    .data("payload", null)
                    .data("jiraUrl", jiraUrl == null ? "" : jiraUrl)
                    .data("jiraBase", jiraBase == null ? "" : jiraBase)
                    .data("jiraHeaders", jiraHeaders == null ? "" : jiraHeaders)
                    .data("rootIssues", roots)
                    .data("selectedRootKey", jiraRootKey == null ? "" : jiraRootKey)
                    .render();
            return Response.ok(html).type(MediaType.TEXT_HTML).build();
        } catch (Exception e) {
            String html = jiraTemplate
                    .data("message", e.getMessage())
                    .data("payload", null)
                    .data("jiraUrl", jiraUrl == null ? "" : jiraUrl)
                    .data("jiraBase", jiraBase == null ? "" : jiraBase)
                    .data("jiraHeaders", jiraHeaders == null ? "" : jiraHeaders)
                    .data("rootIssues", null)
                    .data("selectedRootKey", jiraRootKey == null ? "" : jiraRootKey)
                    .render();
            return Response.status(Response.Status.BAD_REQUEST).entity(html).type(MediaType.TEXT_HTML).build();
        }
    }

    @POST
    @Path("/jira/generate")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response jiraGenerate(@FormParam("jiraBase") String jiraBase,
                                 @FormParam("jiraUrl") String jiraUrl,
                                 @FormParam("jiraToken") String jiraToken,
                                 @FormParam("jiraHeaders") String jiraHeaders,
                                 @FormParam("jiraRootKey") String jiraRootKey) {
        try {
            Map<String, String> headers = parseHeaders(jiraHeaders);
            List<JiraRootIssue> roots = jiraDiscoveryService.findEsaRootIssues(jiraBase, jiraToken, headers);
            String resolvedJiraUrl = StringUtils.isBlank(jiraRootKey) ? jiraUrl : jiraRootKey;
            ESA esa = jiraPayloadService.buildFromUrl(jiraBase, resolvedJiraUrl, jiraToken, headers);
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(esa);
            String html = jiraTemplate
                    .data("message", null)
                    .data("payload", json)
                    .data("jiraUrl", jiraUrl)
                    .data("jiraBase", jiraBase)
                    .data("jiraHeaders", jiraHeaders)
                    .data("rootIssues", roots)
                    .data("selectedRootKey", jiraRootKey == null ? "" : jiraRootKey)
                    .render();
            return Response.ok(html).type(MediaType.TEXT_HTML).build();
        } catch (Exception e) {
            String html = jiraTemplate
                    .data("message", e.getMessage())
                    .data("payload", null)
                    .data("jiraUrl", jiraUrl == null ? "" : jiraUrl)
                    .data("jiraBase", jiraBase == null ? "" : jiraBase)
                    .data("jiraHeaders", jiraHeaders == null ? "" : jiraHeaders)
                    .data("rootIssues", jiraBase == null || jiraToken == null ? null
                            : jiraDiscoveryService.findEsaRootIssues(jiraBase, jiraToken, parseHeaders(jiraHeaders)))
                    .data("selectedRootKey", jiraRootKey == null ? "" : jiraRootKey)
                    .render();
            return Response.status(Response.Status.BAD_REQUEST).entity(html).type(MediaType.TEXT_HTML).build();
        }
    }

    @POST
    @Path("/render-initiatives")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response renderInitiatives(@FormParam("payload") String payload) {
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

    private Map<String, String> parseHeaders(String raw) {
        if (StringUtils.isBlank(raw)) {
            return Map.of();
        }
        Map<String, String> headers = new LinkedHashMap<>();
        String[] lines = raw.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int separator = trimmed.indexOf(':');
            if (separator < 0) {
                separator = trimmed.indexOf('=');
            }
            if (separator < 0) {
                throw new ValidationException("Invalid header line (expected name:value): " + trimmed);
            }
            String name = trimmed.substring(0, separator).trim();
            String value = trimmed.substring(separator + 1).trim();
            if (StringUtils.isBlank(name)) {
                throw new ValidationException("Header name cannot be blank.");
            }
            headers.put(name, value);
        }
        return headers;
    }
}
