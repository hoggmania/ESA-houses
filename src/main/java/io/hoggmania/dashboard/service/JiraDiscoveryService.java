package io.hoggmania.dashboard.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import io.hoggmania.dashboard.model.JiraRootIssue;
import io.hoggmania.dashboard.util.StringUtils;
import io.hoggmania.dashboard.util.UrlUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class JiraDiscoveryService {

    private static final String ESA_ROOT_LABEL_PREFIX = "ESA-Root:";

    @Inject
    JiraClient jiraClient;

    public List<JiraRootIssue> findEsaRootIssues(String baseUrl, String token, Map<String, String> extraHeaders) {
        String jql = "labels = ESA AND labels ~ \"ESA-Root\"";
        JsonNode response = jiraClient.searchIssues(baseUrl, jql, token, extraHeaders);
        List<JiraRootIssue> roots = new ArrayList<>();
        if (response == null) {
            return roots;
        }
        for (JsonNode issue : response.path("issues")) {
            JiraRootIssue root = toRootIssue(issue, baseUrl);
            if (root != null) {
                roots.add(root);
            }
        }
        return roots;
    }

    private JiraRootIssue toRootIssue(JsonNode issue, String baseUrl) {
        if (issue == null || issue.isMissingNode()) {
            return null;
        }
        String key = issue.path("key").asText(null);
        if (StringUtils.isBlank(key)) {
            return null;
        }
        JiraRootIssue root = new JiraRootIssue();
        root.key = key;
        root.summary = issue.path("fields").path("summary").asText("");
        root.rootName = extractRootName(issue.path("fields").path("labels"));
        root.url = UrlUtils.buildBrowseUrl(baseUrl, key);
        return root;
    }

    private String extractRootName(JsonNode labelsNode) {
        if (labelsNode == null || labelsNode.isMissingNode()) {
            return "";
        }
        for (JsonNode labelNode : labelsNode) {
            String label = labelNode.asText("");
            if (label.toLowerCase(Locale.ENGLISH).startsWith(ESA_ROOT_LABEL_PREFIX.toLowerCase(Locale.ENGLISH))) {
                return label.substring(ESA_ROOT_LABEL_PREFIX.length()).trim();
            }
        }
        return "";
    }
}
