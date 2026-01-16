package io.hoggmania.dashboard.util;

/**
 * Utility for generating stable anchor ids for initiative links.
 */
public final class InitiativeAnchor {

    private InitiativeAnchor() {
        // Utility class
    }

    public static String toAnchorId(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        String trimmed = key.trim();
        StringBuilder sb = new StringBuilder("initiative-");
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                sb.append((char) (c + ('a' - 'A')));
            } else if (c == '-' || c == '_') {
                sb.append(c);
            } else {
                sb.append('-');
            }
        }
        return sb.toString();
    }
}
