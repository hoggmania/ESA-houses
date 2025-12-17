package io.hoggmania.dashboard.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StringUtilsTest {

    @Test
    public void testIsBlank() {
        assertTrue(StringUtils.isBlank(null));
        assertTrue(StringUtils.isBlank(""));
        assertTrue(StringUtils.isBlank("   "));
        assertFalse(StringUtils.isBlank("test"));
        assertFalse(StringUtils.isBlank(" test "));
    }

    @Test
    public void testIsNotBlank() {
        assertFalse(StringUtils.isNotBlank(null));
        assertFalse(StringUtils.isNotBlank(""));
        assertFalse(StringUtils.isNotBlank("   "));
        assertTrue(StringUtils.isNotBlank("test"));
        assertTrue(StringUtils.isNotBlank(" test "));
    }

    @Test
    public void testFirstNonBlank() {
        assertEquals("first", StringUtils.firstNonBlank("first", "second"));
        assertEquals("second", StringUtils.firstNonBlank(null, "second", "third"));
        assertEquals("third", StringUtils.firstNonBlank("", "  ", "third"));
        assertEquals("", StringUtils.firstNonBlank(null, "", "  "));
    }

    @Test
    public void testBlankToNull() {
        assertNull(StringUtils.blankToNull(null));
        assertNull(StringUtils.blankToNull(""));
        assertNull(StringUtils.blankToNull("   "));
        assertEquals("test", StringUtils.blankToNull("test"));
        assertEquals("test", StringUtils.blankToNull(" test "));
    }

    @Test
    public void testEscapeXml() {
        assertEquals("", StringUtils.escapeXml(null));
        assertEquals("", StringUtils.escapeXml(""));
        assertEquals("test", StringUtils.escapeXml("test"));
        assertEquals("&lt;tag&gt;", StringUtils.escapeXml("<tag>"));
        assertEquals("&amp;&lt;&gt;&quot;&#39;", StringUtils.escapeXml("&<>\"'"));
        assertEquals("Hello &amp; goodbye", StringUtils.escapeXml("Hello & goodbye"));
    }
}

