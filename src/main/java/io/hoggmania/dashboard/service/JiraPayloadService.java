package io.hoggmania.dashboard.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import io.hoggmania.dashboard.exception.ValidationException;
import io.hoggmania.dashboard.model.Capabilities;
import io.hoggmania.dashboard.model.ComponentInitiative;
import io.hoggmania.dashboard.model.ComponentItem;
import io.hoggmania.dashboard.model.Domain;
import io.hoggmania.dashboard.model.ESA;
import io.hoggmania.dashboard.model.Governance;
import io.hoggmania.dashboard.util.StringUtils;
import io.hoggmania.dashboard.util.UrlUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Builds ESA payloads from Jira issues based on the hierarchy rules outlined by ESA.
 * 
 * <p>The service expects a specific Jira issue structure with labels:
 * <ul>
 *   <li>Root issue: labeled with "ESA" and "ESA-Root:{name}"</li>
 *   <li>Governance issue: linked to root, labeled with "ESA-Governance"</li>
 *   <li>Capabilities issue: linked to root, labeled with "ESA-Capabilities"</li>
 *   <li>Domain issues: linked to capabilities, issue type "Epic"</li>
 *   <li>Feature issues: linked to domains or governance, issue type "Feature"</li>
 *   <li>Initiative issues: linked to features, issue types "Theme", "Initiative", "Epic", or "Feature"</li>
 * </ul>
 * 
 * <p>Components are configured using labels on Feature issues:
 * <ul>
 *   <li>ESA-Capability:{value}</li>
 *   <li>ESA-Maturity:{value}</li>
 *   <li>ESA-Status:{value}</li>
 *   <li>ESA-RAG:{value}</li>
 *   <li>ESA-Tool:{value}</li>
 *   <li>ESA-Risk:{value}</li>
 *   <li>ESA-Double (for double border)</li>
 *   <li>ESA-Icon:{icon-name}</li>
 * </ul>
 */
@ApplicationScoped
public class JiraPayloadService {

    private static final Pattern CAPABILITY_PATTERN = Pattern.compile("ESA-Capability:(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MATURITY_PATTERN = Pattern.compile("ESA-Maturity:(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STATUS_PATTERN = Pattern.compile("ESA-Status:(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RAG_PATTERN = Pattern.compile("ESA-RAG:(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOOL_PATTERN = Pattern.compile("ESA-Tool:(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RISK_PATTERN = Pattern.compile("ESA-Risk:(.+)", Pattern.CASE_INSENSITIVE);

    private static final Set<String> INITIATIVE_ISSUE_TYPES = Set.of(
            "Theme", "Initiative", "Epic", "Feature");

    @Inject
    JiraClient jiraClient;

    /**
     * Builds an ESA model from a Jira root issue URL.
     * Recursively fetches linked issues to build the complete hierarchy.
     * 
     * @param baseUrl the Jira instance base URL (e.g., https://jira.example.com)
     * @param issueUrl the root issue URL or key (e.g., PROJ-123 or https://jira.example.com/browse/PROJ-123)
     * @param personalToken the personal access token for authentication
     * @return the complete ESA model
     * @throws ValidationException if the issue structure is invalid or required labels are missing
     */
    public ESA buildFromUrl(String baseUrl, String issueUrl, String personalToken) {
        if (StringUtils.isBlank(baseUrl)) {
            throw new ValidationException("Jira location is required.");
        }
        if (StringUtils.isBlank(issueUrl)) {
            throw new ValidationException("Jira issue URL is required.");
        }
        String key = UrlUtils.extractIssueKey(issueUrl);
        JsonNode root = jiraClient.fetchIssue(baseUrl, key, personalToken);
        validateRoot(root, key);

        ESA esa = new ESA();
        esa.title = root.path("fields").path("summary").asText("ESA Dashboard");
        esa.icon = "shield";

        Map<String, JsonNode> linkedIssues = loadLinkedIssues(root, baseUrl, personalToken);
        JsonNode governanceNode = findByLabel(linkedIssues, "ESA-Governance");
        JsonNode capabilitiesNode = findByLabel(linkedIssues, "ESA-Capabilities");
        if (governanceNode == null) {
            throw new ValidationException("Root issue " + key + " must have a linked issue labelled ESA-Governance");
        }
        if (capabilitiesNode == null) {
            throw new ValidationException("Root issue " + key + " must have a linked issue labelled ESA-Capabilities");
        }

        esa.governance = buildGovernance(governanceNode, baseUrl, personalToken);
        esa.capabilities = buildCapabilities(capabilitiesNode, baseUrl, personalToken);
        return esa;
    }

    private Governance buildGovernance(JsonNode governanceIssue, String baseUrl, String token) {
        Governance governance = new Governance();
        governance.title = governanceIssue.path("fields").path("summary").asText("Governance");
        governance.components = toComponentList(resolveLinkedFeatures(governanceIssue, baseUrl, token), baseUrl, token);
        return governance;
    }

    private Capabilities buildCapabilities(JsonNode capabilitiesIssue, String baseUrl, String token) {
        Capabilities capabilities = new Capabilities();
        capabilities.title = capabilitiesIssue.path("fields").path("summary").asText("Capabilities");
        capabilities.icon = "chart";
        List<JsonNode> domains = resolveLinkedIssues(capabilitiesIssue, "Epic", baseUrl, token);
        List<Domain> domainList = new ArrayList<>();
        for (JsonNode domainIssue : domains) {
            Domain domain = new Domain();
            domain.domain = domainIssue.path("fields").path("summary").asText("Domain");
            domain.icon = inferIconFromLabels(domainIssue);
            domain.components = toComponentList(resolveLinkedFeatures(domainIssue, baseUrl, token), baseUrl, token);
            domainList.add(domain);
        }
        capabilities.domains = domainList;
        return capabilities;
    }

    private List<ComponentItem> toComponentList(List<JsonNode> featureIssues, String baseUrl, String token) {
        List<ComponentItem> components = new ArrayList<>();
        for (JsonNode issue : featureIssues) {
            components.add(toComponent(issue, baseUrl, token));
        }
        return components;
    }

    private ComponentItem toComponent(JsonNode issue, String baseUrl, String token) {
        ComponentItem component = new ComponentItem();
        component.name = issue.path("fields").path("summary").asText("Component");
        component.capability = extractLabel(issue, CAPABILITY_PATTERN).orElse(component.name);
        component.maturity = parseMaturity(extractLabel(issue, MATURITY_PATTERN).orElse("DEFINED"));
        component.status = parseStatus(extractLabel(issue, STATUS_PATTERN).orElse("MEDIUM"));
        component.icon = inferIconFromLabels(issue);
        component.rag = extractLabel(issue, RAG_PATTERN).orElse("green").toLowerCase(Locale.ENGLISH);
        component.iRag = component.rag;
        component.doubleBorder = hasLabel(issue, "ESA-Double");
        List<JsonNode> initiativeIssues = resolveLinkedInitiatives(issue, baseUrl, token);
        component.initiativeDetails = buildInitiatives(initiativeIssues, baseUrl);
        component.initiatives = component.initiativeDetails != null ? component.initiativeDetails.size() : 0;
        return component;
    }

    private List<ComponentInitiative> buildInitiatives(List<JsonNode> initiativeIssues, String baseUrl) {
        if (initiativeIssues.isEmpty()) {
            return List.of();
        }
        List<ComponentInitiative> initiatives = new ArrayList<>();
        for (JsonNode issue : initiativeIssues) {
            ComponentInitiative initiative = new ComponentInitiative();
            initiative.key = issue.path("key").asText();
            initiative.link = UrlUtils.buildBrowseUrl(baseUrl, initiative.key);
            initiative.summary = issue.path("fields").path("summary").asText();
            initiative.rag = extractLabel(issue, RAG_PATTERN).orElse("green");
            initiative.toolId = extractLabel(issue, TOOL_PATTERN).orElse("In-Demand");
            initiative.riskAppetite = extractLabel(issue, RISK_PATTERN).orElse("");
            initiative.businessBenefit = optionalText(issue.path("fields").path("description"));
            initiative.dueDate = issue.path("fields").path("duedate").asText("");
            initiatives.add(initiative);
        }
        return initiatives;
    }

    private String optionalText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.has("content")) {
            // Jira rich text
            return node.path("content").toString();
        }
        return node.asText("");
    }

    private List<JsonNode> resolveLinkedFeatures(JsonNode parentIssue, String baseUrl, String token) {
        return resolveLinkedIssues(parentIssue, "Feature", baseUrl, token);
    }

    private List<JsonNode> resolveLinkedIssues(JsonNode parentIssue, String expectedIssueType, String baseUrl, String token) {
        List<JsonNode> linked = new ArrayList<>();
        for (JsonNode link : parentIssue.path("fields").path("issuelinks")) {
            JsonNode raw = link.has("outwardIssue") ? link.path("outwardIssue") : link.path("inwardIssue");
            if (raw.isMissingNode()) {
                continue;
            }
            JsonNode issue = jiraClient.fetchIssue(baseUrl, raw.path("key").asText(), token);
            String typeName = issue.path("fields").path("issuetype").path("name").asText();
            if (expectedIssueType == null || typeName.equalsIgnoreCase(expectedIssueType)) {
                linked.add(issue);
            }
        }
        return linked;
    }

    private List<JsonNode> resolveLinkedInitiatives(JsonNode issue, String baseUrl, String token) {
        List<JsonNode> linked = new ArrayList<>();
        for (JsonNode link : issue.path("fields").path("issuelinks")) {
            JsonNode raw = link.has("outwardIssue") ? link.path("outwardIssue") : link.path("inwardIssue");
            if (raw.isMissingNode()) {
                continue;
            }
            JsonNode child = jiraClient.fetchIssue(baseUrl, raw.path("key").asText(), token);
            String typeName = child.path("fields").path("issuetype").path("name").asText();
            if (INITIATIVE_ISSUE_TYPES.contains(typeName)) {
                linked.add(child);
            }
        }
        return linked;
    }

    private Map<String, JsonNode> loadLinkedIssues(JsonNode issue, String baseUrl, String token) {
        Map<String, JsonNode> map = new HashMap<>();
        for (JsonNode link : issue.path("fields").path("issuelinks")) {
            JsonNode raw = link.has("outwardIssue") ? link.path("outwardIssue") : link.path("inwardIssue");
            if (raw.isMissingNode()) continue;
            JsonNode full = jiraClient.fetchIssue(baseUrl, raw.path("key").asText(), token);
            for (String label : collectLabels(full)) {
                map.put(label, full);
            }
        }
        return map;
    }

    private JsonNode findByLabel(Map<String, JsonNode> issues, String label) {
        for (Map.Entry<String, JsonNode> entry : issues.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(label)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void validateRoot(JsonNode root, String key) {
        Set<String> labels = collectLabels(root);
        if (!labels.contains("ESA")) {
            throw new ValidationException("Root issue " + key + " is missing required label 'ESA'");
        }
        boolean hasRootLabel = labels.stream().anyMatch(l -> l.toUpperCase(Locale.ENGLISH).startsWith("ESA-ROOT:"));
        if (!hasRootLabel) {
            throw new ValidationException("Root issue " + key + " is missing label 'ESA-Root:{name}'");
        }
    }

    private Set<String> collectLabels(JsonNode issue) {
        Set<String> labels = new HashSet<>();
        for (JsonNode label : issue.path("fields").path("labels")) {
            labels.add(label.asText());
        }
        return labels;
    }

    private ComponentItem.Status parseStatus(String value) {
        try {
            return ComponentItem.Status.valueOf(value.trim().toUpperCase(Locale.ENGLISH));
        } catch (Exception e) {
            return ComponentItem.Status.MEDIUM;
        }
    }

    private ComponentItem.Maturity parseMaturity(String value) {
        try {
            return ComponentItem.Maturity.valueOf(value.trim().toUpperCase(Locale.ENGLISH));
        } catch (Exception e) {
            return ComponentItem.Maturity.DEFINED;
        }
    }

    private String inferIconFromLabels(JsonNode issue) {
        Set<String> labels = collectLabels(issue);
        if (labels.contains("ESA-Icon:bug")) return "bug";
        if (labels.contains("ESA-Icon:user")) return "user";
        if (labels.contains("ESA-Icon:group")) return "group";
        return "search";
    }

    private Optional<String> extractLabel(JsonNode issue, Pattern pattern) {
        for (String label : collectLabels(issue)) {
            Matcher m = pattern.matcher(label);
            if (m.matches()) {
                return Optional.of(m.group(1).trim());
            }
        }
        return Optional.empty();
    }

    private boolean hasLabel(JsonNode issue, String label) {
        return collectLabels(issue).stream().anyMatch(l -> l.equalsIgnoreCase(label));
    }
}
