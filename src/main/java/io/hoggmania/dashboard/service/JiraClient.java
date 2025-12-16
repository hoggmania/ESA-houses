package io.hoggmania.dashboard.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import io.hoggmania.dashboard.exception.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Minimal Jira REST client that supports fetching issues either from a live Jira instance
 * or from mock JSON files on disk (useful for local development without credentials).
 */
@ApplicationScoped
public class JiraClient {

    private final Optional<String> baseUrl;
    private final Optional<String> email;
    private final Optional<String> token;
    private final Optional<String> mockDir;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Inject
    ObjectMapper mapper;

    @Inject
    public JiraClient(
            @ConfigProperty(name = "jira.api.base-url") Optional<String> baseUrl,
            @ConfigProperty(name = "jira.api.email") Optional<String> email,
            @ConfigProperty(name = "jira.api.token") Optional<String> token,
            @ConfigProperty(name = "jira.mock.dir") Optional<String> mockDir) {
        this.baseUrl = baseUrl.filter(s -> !s.isBlank());
        this.email = email.filter(s -> !s.isBlank());
        this.token = token.filter(s -> !s.isBlank());
        this.mockDir = mockDir.filter(s -> !s.isBlank());
    }

    public JsonNode fetchIssue(String issueKey) {
        return fetchIssue(issueKey, Optional.empty());
    }

    public JsonNode fetchIssue(String issueKey, Optional<String> overrideToken) {
        if (mockDir.isPresent()) {
            return loadFromMock(issueKey);
        }
        Optional<String> sanitizedToken = overrideToken.filter(token -> token != null && !token.isBlank());
        String base = baseUrl.orElseThrow(() ->
                new ValidationException("jira.api.base-url is not configured and no mock directory was provided."));
        String user = email.orElseThrow(() ->
                new ValidationException("jira.api.email must be configured to call Jira REST API."));
        String apiToken = sanitizedToken
                .or(() -> token.filter(t -> !t.isBlank()))
                .orElseThrow(() -> new ValidationException("jira.api.token must be configured (or provided in the request) to call Jira REST API."));

        try {
            String encodedKey = URLEncoder.encode(issueKey, StandardCharsets.UTF_8);
            URI uri = URI.create(base + "/rest/api/3/issue/" + encodedKey + "?expand=renderedFields,changelog");
            String auth = Base64.getEncoder().encodeToString((user + ":" + apiToken).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .header("Authorization", "Basic " + auth)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ValidationException("Failed to fetch Jira issue " + issueKey + ": HTTP " + response.statusCode());
            }
            return mapper.readTree(response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ValidationException("Unable to call Jira API: " + e.getMessage());
        }
    }

    private JsonNode loadFromMock(String issueKey) {
        Path dir = Path.of(mockDir.get());
        Path file = dir.resolve(issueKey + ".json");
        if (!Files.exists(file)) {
            throw new ValidationException("Mock Jira issue file not found: " + file.toAbsolutePath());
        }
        try {
            return mapper.readTree(Files.readString(file));
        } catch (IOException e) {
            throw new ValidationException("Failed to read mock issue " + issueKey + ": " + e.getMessage());
        }
    }
}
