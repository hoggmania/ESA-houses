package io.hoggmania.dashboard.util;

import io.hoggmania.dashboard.exception.ValidationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UrlUtilsTest {

    @Test
    public void testNormalizeBaseUrl_Valid() {
        assertEquals("https://jira.example.com", 
            UrlUtils.normalizeBaseUrl("https://jira.example.com"));
        assertEquals("https://jira.example.com", 
            UrlUtils.normalizeBaseUrl("https://jira.example.com/"));
        assertEquals("https://jira.example.com", 
            UrlUtils.normalizeBaseUrl(" https://jira.example.com "));
    }

    @Test
    public void testNormalizeBaseUrl_RequiresHttps() {
        assertThrows(ValidationException.class, () -> 
            UrlUtils.normalizeBaseUrl("http://jira.example.com"));
    }

    @Test
    public void testNormalizeBaseUrl_RequiresScheme() {
        assertThrows(ValidationException.class, () -> 
            UrlUtils.normalizeBaseUrl("jira.example.com"));
    }

    @Test
    public void testNormalizeBaseUrl_Blank() {
        assertThrows(ValidationException.class, () -> 
            UrlUtils.normalizeBaseUrl(null));
        assertThrows(ValidationException.class, () -> 
            UrlUtils.normalizeBaseUrl(""));
        assertThrows(ValidationException.class, () -> 
            UrlUtils.normalizeBaseUrl("   "));
    }

    @Test
    public void testExtractIssueKey() {
        assertEquals("PROJ-123", 
            UrlUtils.extractIssueKey("https://jira.example.com/browse/PROJ-123"));
        assertEquals("PROJ-456", 
            UrlUtils.extractIssueKey("PROJ-456"));
        assertEquals("KEY-789", 
            UrlUtils.extractIssueKey("/jira/browse/KEY-789"));
    }

    @Test
    public void testBuildBrowseUrl() {
        assertEquals("https://jira.example.com/browse/PROJ-123", 
            UrlUtils.buildBrowseUrl("https://jira.example.com", "PROJ-123"));
        assertEquals("https://jira.example.com/browse/PROJ-123", 
            UrlUtils.buildBrowseUrl("https://jira.example.com/", "PROJ-123"));
    }

    @Test
    public void testBuildBrowseUrl_WithSpecialCharacters() {
        String result = UrlUtils.buildBrowseUrl("https://jira.example.com", "PROJ 123");
        assertTrue(result.contains("/browse/"));
        assertFalse(result.contains(" "));
    }

    @Test
    public void testEncode() {
        assertEquals("test", UrlUtils.encode("test"));
        assertEquals("test+value", UrlUtils.encode("test value"));
        assertEquals("test%26value", UrlUtils.encode("test&value"));
        assertNull(UrlUtils.encode(null));
    }

    @Test
    public void testDecode() {
        assertEquals("test", UrlUtils.decode("test"));
        assertEquals("test value", UrlUtils.decode("test+value"));
        assertEquals("test&value", UrlUtils.decode("test%26value"));
        assertNull(UrlUtils.decode(null));
    }
}

