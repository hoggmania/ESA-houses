package io.hoggmania.dashboard.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.hoggmania.dashboard.exception.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class JiraClient {

    private final HttpClient httpClient;

    @Inject
    ObjectMapper mapper;

    @Inject
    public JiraClient(
            @ConfigProperty(name = "jira.trust-store") java.util.Optional<String> trustStorePath,
            @ConfigProperty(name = "jira.trust-store-password") java.util.Optional<String> trustStorePassword) {
        this.httpClient = createClient(trustStorePath, trustStorePassword);
    }

    private HttpClient createClient(java.util.Optional<String> trustStorePath, java.util.Optional<String> trustStorePassword) {
        try {
            HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10));
            if (trustStorePath.isPresent()) {
                String path = trustStorePath.get();
                char[] password = trustStorePassword.orElse("").toCharArray();
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                try (FileInputStream fis = new FileInputStream(path)) {
                    keyStore.load(fis, password);
                }
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
                builder.sslContext(sslContext);
            }
            return builder.build();
        } catch (Exception e) {
            throw new ValidationException("Failed to load Jira trust store: " + e.getMessage());
        }
    }

    public JsonNode fetchIssue(String baseUrl, String issueKey, String personalAccessToken) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new ValidationException("Jira base URL is required.");
        }
        if (issueKey == null || issueKey.isBlank()) {
            throw new ValidationException("Jira issue key is required.");
        }
        if (personalAccessToken == null || personalAccessToken.isBlank()) {
            throw new ValidationException("A Jira personal access token is required.");
        }

        try {
            String normalizedBase = normalizeBaseUrl(baseUrl);
            String encodedKey = URLEncoder.encode(issueKey, StandardCharsets.UTF_8);
            URI uri = URI.create(normalizedBase + "/rest/api/3/issue/" + encodedKey + "?expand=renderedFields,changelog");
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + personalAccessToken.trim())
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

    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl.trim();
        URI uri = URI.create(trimmed);
        String scheme = uri.getScheme();
        if (scheme == null || scheme.isBlank()) {
            throw new ValidationException("Jira location must include an https:// scheme.");
        }
        if (!scheme.equalsIgnoreCase("https")) {
            throw new ValidationException("Jira location must use HTTPS.");
        }
        if (!uri.isAbsolute()) {
            throw new ValidationException("Jira location must be an absolute URL.");
        }
        String normalized = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        return normalized;
    }
}
