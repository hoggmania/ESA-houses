package io.hoggmania.dashboard.service;

import java.net.URI;
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Builds ESA payloads from Jira issues based on the hierarchy rules outlined by ESA.
 */
@ApplicationScoped
public class JiraPayloadService {

    private static final Pattern ROOT_LABEL_PATTERN = Pattern.compile("ESA-Root:(.+)", Pattern.CASE_INSENSITIVE);
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

    public ESA buildFromUrl(String urlOrKey) {
        return buildFromUrl(urlOrKey, null);
    }

    public ESA buildFromUrl(String urlOrKey, String personalToken) {
        Optional<String> tokenOverride = Optional.ofNullable(personalToken).filter(s -> !s.isBlank());
        String key = extractIssueKey(urlOrKey);
        JsonNode root = jiraClient.fetchIssue(key, tokenOverride);
        validateRoot(root, key);

        ESA esa = new ESA();
        esa.title = root.path("fields").path("summary").asText("ESA Dashboard");
        esa.icon = "shield";

        Map<String, JsonNode> linkedIssues = loadLinkedIssues(root, tokenOverride);
        JsonNode governanceNode = findByLabel(linkedIssues, "ESA-Governance");
        JsonNode capabilitiesNode = findByLabel(linkedIssues, "ESA-Capabilities");
        if (governanceNode == null) {
            throw new ValidationException("Root issue " + key + " must have a linked issue labelled ESA-Governance");
        }
        if (capabilitiesNode == null) {
            throw new ValidationException("Root issue " + key + " must have a linked issue labelled ESA-Capabilities");
        }

        esa.governance = buildGovernance(governanceNode, tokenOverride);
        esa.capabilities = buildCapabilities(capabilitiesNode, tokenOverride);
        return esa;
    }

    private Governance buildGovernance(JsonNode governanceIssue, Optional<String> tokenOverride) {
        Governance governance = new Governance();
        governance.title = governanceIssue.path("fields").path("summary").asText("Governance");
        governance.components = toComponentList(resolveLinkedFeatures(governanceIssue, tokenOverride), tokenOverride);
        return governance;
    }

    private Capabilities buildCapabilities(JsonNode capabilitiesIssue, Optional<String> tokenOverride) {
        Capabilities capabilities = new Capabilities();
        capabilities.title = capabilitiesIssue.path("fields").path("summary").asText("Capabilities");
        capabilities.icon = "chart";
        List<JsonNode> domains = resolveLinkedIssues(capabilitiesIssue, "Epic", tokenOverride);
        List<Domain> domainList = new ArrayList<>();
        for (JsonNode domainIssue : domains) {
            Domain domain = new Domain();
            domain.domain = domainIssue.path("fields").path("summary").asText("Domain");
            domain.icon = inferIconFromLabels(domainIssue);
            domain.components = toComponentList(resolveLinkedFeatures(domainIssue, tokenOverride), tokenOverride);
            domainList.add(domain);
        }
        capabilities.domains = domainList;
        return capabilities;
    }

    private List<ComponentItem> toComponentList(List<JsonNode> featureIssues, Optional<String> tokenOverride) {
        List<ComponentItem> components = new ArrayList<>();
        for (JsonNode issue : featureIssues) {
            components.add(toComponent(issue, tokenOverride));
        }
        return components;
    }

    private ComponentItem toComponent(JsonNode issue, Optional<String> tokenOverride) {
        ComponentItem component = new ComponentItem();
        component.name = issue.path("fields").path("summary").asText("Component");
        component.capability = extractLabel(issue, CAPABILITY_PATTERN).orElse(component.name);
        component.maturity = parseMaturity(extractLabel(issue, MATURITY_PATTERN).orElse("DEFINED"));
        component.status = parseStatus(extractLabel(issue, STATUS_PATTERN).orElse("MEDIUM"));
        component.icon = inferIconFromLabels(issue);
        component.rag = extractLabel(issue, RAG_PATTERN).orElse("green").toLowerCase(Locale.ENGLISH);
        component.iRag = component.rag;
        component.doubleBorder = hasLabel(issue, "ESA-Double");
        List<JsonNode> initiativeIssues = resolveLinkedInitiatives(issue, tokenOverride);
        component.initiativeDetails = buildInitiatives(initiativeIssues);
        component.initiatives = component.initiativeDetails != null ? component.initiativeDetails.size() : 0;
        return component;
    }

    private List<ComponentInitiative> buildInitiatives(List<JsonNode> initiativeIssues) {
        if (initiativeIssues.isEmpty()) {
            return List.of();
        }
        List<ComponentInitiative> initiatives = new ArrayList<>();
        for (JsonNode issue : initiativeIssues) {
            ComponentInitiative initiative = new ComponentInitiative();
            initiative.key = issue.path("key").asText();
            initiative.link = URI.create(issue.path("self").asText()).resolve(issue.path("key").asText()).toString();
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

    private List<JsonNode> resolveLinkedFeatures(JsonNode parentIssue, Optional<String> tokenOverride) {
        return resolveLinkedIssues(parentIssue, "Feature", tokenOverride);
    }

    private List<JsonNode> resolveLinkedIssues(JsonNode parentIssue, String expectedIssueType, Optional<String> tokenOverride) {
        List<JsonNode> linked = new ArrayList<>();
        for (JsonNode link : parentIssue.path("fields").path("issuelinks")) {
            JsonNode raw = link.has("outwardIssue") ? link.path("outwardIssue") : link.path("inwardIssue");
            if (raw.isMissingNode()) {
                continue;
            }
            JsonNode issue = jiraClient.fetchIssue(raw.path("key").asText(), tokenOverride);
            String typeName = issue.path("fields").path("issuetype").path("name").asText();
            if (expectedIssueType == null || typeName.equalsIgnoreCase(expectedIssueType)) {
                linked.add(issue);
            }
        }
        return linked;
    }

    private List<JsonNode> resolveLinkedInitiatives(JsonNode issue, Optional<String> tokenOverride) {
        List<JsonNode> linked = new ArrayList<>();
        for (JsonNode link : issue.path("fields").path("issuelinks")) {
            JsonNode raw = link.has("outwardIssue") ? link.path("outwardIssue") : link.path("inwardIssue");
            if (raw.isMissingNode()) {
                continue;
            }
            JsonNode child = jiraClient.fetchIssue(raw.path("key").asText(), tokenOverride);
            String typeName = child.path("fields").path("issuetype").path("name").asText();
            if (INITIATIVE_ISSUE_TYPES.contains(typeName)) {
                linked.add(child);
            }
        }
        return linked;
    }

    private Map<String, JsonNode> loadLinkedIssues(JsonNode issue, Optional<String> tokenOverride) {
        Map<String, JsonNode> map = new HashMap<>();
        for (JsonNode link : issue.path("fields").path("issuelinks")) {
            JsonNode raw = link.has("outwardIssue") ? link.path("outwardIssue") : link.path("inwardIssue");
            if (raw.isMissingNode()) continue;
            JsonNode full = jiraClient.fetchIssue(raw.path("key").asText(), tokenOverride);
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

    private String extractIssueKey(String urlOrKey) {
        try {
            URI uri = URI.create(urlOrKey);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return urlOrKey;
            }
            String[] segments = path.split("/");
            return segments[segments.length - 1];
        } catch (IllegalArgumentException ex) {
            return urlOrKey;
        }
    }
}
