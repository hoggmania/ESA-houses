package io.hoggmania.dashboard.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.hoggmania.dashboard.exception.ValidationException;
import io.hoggmania.dashboard.util.UrlUtils;
import io.hoggmania.dashboard.util.StringUtils;
import io.hoggmania.dashboard.util.RateLimiter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * HTTP client for interacting with Jira REST API.
 * Supports custom trust stores for enterprise Jira instances
 * and includes rate limiting to prevent API throttling.
 */
@ApplicationScoped
public class JiraClient {

    private final HttpClient httpClient;
    private final RateLimiter rateLimiter;

    @Inject
    ObjectMapper mapper;

    @Inject
    public JiraClient(
            @ConfigProperty(name = "jira.trust-store") java.util.Optional<String> trustStorePath,
            @ConfigProperty(name = "jira.trust-store-password") java.util.Optional<String> trustStorePassword,
            @ConfigProperty(name = "jira.rate-limit.max-requests", defaultValue = "100") int maxRequests,
            @ConfigProperty(name = "jira.rate-limit.window-seconds", defaultValue = "60") int windowSeconds) {
        this.httpClient = createClient(trustStorePath, trustStorePassword);
        this.rateLimiter = new RateLimiter(maxRequests, Duration.ofSeconds(windowSeconds));
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

    /**
     * Fetches a Jira issue by key with all linked issues and changelog.
     * Includes rate limiting per Jira instance to prevent overwhelming the API.
     * 
     * @param baseUrl the Jira instance base URL (e.g., https://jira.example.com)
     * @param issueKey the issue key (e.g., PROJ-123)
     * @param personalAccessToken the personal access token for authentication
     * @return the issue data as a JsonNode
     * @throws ValidationException if parameters are invalid, rate limit is exceeded, or the API call fails
     */
    public JsonNode fetchIssue(String baseUrl, String issueKey, String personalAccessToken, Map<String, String> extraHeaders) {
        if (StringUtils.isBlank(baseUrl)) {
            throw new ValidationException("Jira base URL is required.");
        }
        if (StringUtils.isBlank(issueKey)) {
            throw new ValidationException("Jira issue key is required.");
        }
        if (StringUtils.isBlank(personalAccessToken)) {
            throw new ValidationException("A Jira personal access token is required.");
        }

        // Rate limiting per base URL to prevent overwhelming the Jira instance
        if (!rateLimiter.tryAcquire(baseUrl)) {
            throw new ValidationException("Rate limit exceeded for Jira instance: " + baseUrl + 
                ". Please try again later.");
        }

        try {
            String normalizedBase = UrlUtils.normalizeBaseUrl(baseUrl);
            String encodedKey = UrlUtils.encode(issueKey);
            URI uri = URI.create(normalizedBase + "/rest/api/3/issue/" + encodedKey + "?expand=renderedFields,changelog");
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + personalAccessToken.trim());
            if (extraHeaders != null && !extraHeaders.isEmpty()) {
                for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
                    String name = entry.getKey();
                    String value = entry.getValue();
                    if (StringUtils.isBlank(name)) {
                        continue;
                    }
                    if ("authorization".equalsIgnoreCase(name) || "accept".equalsIgnoreCase(name)) {
                        continue;
                    }
                    builder.header(name.trim(), value == null ? "" : value);
                }
            }
            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ValidationException("Failed to fetch Jira issue " + issueKey + ": HTTP " + response.statusCode());
            }
            return mapper.readTree(response.body());
        } catch (ValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new ValidationException("Unable to call Jira API: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ValidationException("Jira API call was interrupted: " + e.getMessage());
        }
    }

    /**
     * Executes a Jira JQL search and returns the raw response body.
     *
     * @param baseUrl the Jira instance base URL
     * @param jql the JQL query
     * @param personalAccessToken the personal access token for authentication
     * @param extraHeaders extra headers to pass to Jira
     * @return the search response as a JsonNode
     */
    public JsonNode searchIssues(String baseUrl, String jql, String personalAccessToken, Map<String, String> extraHeaders) {
        if (StringUtils.isBlank(baseUrl)) {
            throw new ValidationException("Jira base URL is required.");
        }
        if (StringUtils.isBlank(jql)) {
            throw new ValidationException("JQL query is required.");
        }
        if (StringUtils.isBlank(personalAccessToken)) {
            throw new ValidationException("A Jira personal access token is required.");
        }

        if (!rateLimiter.tryAcquire(baseUrl)) {
            throw new ValidationException("Rate limit exceeded for Jira instance: " + baseUrl +
                ". Please try again later.");
        }

        try {
            String normalizedBase = UrlUtils.normalizeBaseUrl(baseUrl);
            String encodedJql = UrlUtils.encode(jql);
            URI uri = URI.create(normalizedBase + "/rest/api/3/search?jql=" + encodedJql + "&maxResults=50&fields=summary,labels,issuetype");
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + personalAccessToken.trim());
            if (extraHeaders != null && !extraHeaders.isEmpty()) {
                for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
                    String name = entry.getKey();
                    String value = entry.getValue();
                    if (StringUtils.isBlank(name)) {
                        continue;
                    }
                    if ("authorization".equalsIgnoreCase(name) || "accept".equalsIgnoreCase(name)) {
                        continue;
                    }
                    builder.header(name.trim(), value == null ? "" : value);
                }
            }
            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ValidationException("Jira search failed: HTTP " + response.statusCode());
            }
            return mapper.readTree(response.body());
        } catch (ValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new ValidationException("Unable to call Jira API: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ValidationException("Jira API call was interrupted: " + e.getMessage());
        }
    }
}
