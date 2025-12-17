package io.hoggmania.dashboard.util;

/**
 * Utility methods for string manipulation and validation.
 */
public final class StringUtils {
    
    private StringUtils() {
        // Prevent instantiation
    }
    
    /**
     * Checks if a string is null, empty, or contains only whitespace.
     * 
     * @param value the string to check
     * @return true if the string is blank, false otherwise
     */
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
    
    /**
     * Checks if a string is not null and contains non-whitespace characters.
     * 
     * @param value the string to check
     * @return true if the string is not blank, false otherwise
     */
    public static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
    
    /**
     * Returns the first non-blank string from the provided values.
     * 
     * @param values strings to check in order
     * @return the first non-blank string, or empty string if all are blank
     */
    public static String firstNonBlank(String... values) {
        for (String value : values) {
            if (isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }
    
    /**
     * Returns null if the string is blank, otherwise returns the trimmed string.
     * 
     * @param value the string to process
     * @return null if blank, trimmed string otherwise
     */
    public static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
    
    /**
     * Escapes special XML/HTML characters to prevent XSS.
     * 
     * @param value the string to escape
     * @return the escaped string, or empty string if input is null
     */
    public static String escapeXml(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}

