package io.hoggmania.dashboard.util;

import io.hoggmania.dashboard.exception.ValidationException;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Utility methods for URL manipulation and validation.
 */
public final class UrlUtils {
    
    private UrlUtils() {
        // Prevent instantiation
    }
    
    /**
     * Normalizes a base URL by ensuring it has HTTPS scheme and no trailing slash.
     * 
     * @param baseUrl the URL to normalize
     * @return the normalized URL
     * @throws ValidationException if the URL is invalid or not HTTPS
     */
    public static String normalizeBaseUrl(String baseUrl) {
        if (StringUtils.isBlank(baseUrl)) {
            throw new ValidationException("Base URL cannot be null or blank.");
        }
        
        String trimmed = baseUrl.trim();
        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid URL format: " + e.getMessage());
        }
        
        String scheme = uri.getScheme();
        if (StringUtils.isBlank(scheme)) {
            throw new ValidationException("URL must include an https:// scheme.");
        }
        if (!scheme.equalsIgnoreCase("https")) {
            throw new ValidationException("URL must use HTTPS.");
        }
        if (!uri.isAbsolute()) {
            throw new ValidationException("URL must be absolute.");
        }
        
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
    
    /**
     * Extracts the issue key from a Jira URL or returns the key if already extracted.
     * 
     * @param urlOrKey the URL or issue key
     * @return the extracted issue key
     */
    public static String extractIssueKey(String urlOrKey) {
        if (StringUtils.isBlank(urlOrKey)) {
            return urlOrKey;
        }
        
        try {
            URI uri = URI.create(urlOrKey);
            String path = uri.getPath();
            if (StringUtils.isBlank(path)) {
                return urlOrKey;
            }
            String[] segments = path.split("/");
            return segments[segments.length - 1];
        } catch (IllegalArgumentException ex) {
            return urlOrKey;
        }
    }
    
    /**
     * Builds a Jira browse URL for an issue.
     * 
     * @param baseUrl the Jira base URL
     * @param issueKey the issue key
     * @return the full browse URL
     */
    public static String buildBrowseUrl(String baseUrl, String issueKey) {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String encodedKey = URLEncoder.encode(issueKey, StandardCharsets.UTF_8);
        return normalized + "/browse/" + encodedKey;
    }
    
    /**
     * URL-encodes a string using UTF-8.
     * 
     * @param value the string to encode
     * @return the URL-encoded string
     */
    public static String encode(String value) {
        if (value == null) {
            return null;
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
    
    /**
     * URL-decodes a string using UTF-8.
     * 
     * @param value the string to decode
     * @return the decoded string, or the original if decoding fails
     */
    public static String decode(String value) {
        if (value == null) {
            return null;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }
}

