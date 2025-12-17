package io.hoggmania.dashboard.util;

/**
 * Centralized color palette for dashboard visualization.
 * All colors follow the application's design system.
 */
public final class ColorPalette {
    
    private ColorPalette() {
        // Prevent instantiation
    }
    
    // RAG Status Colors
    public static final String RAG_RED = "#DC2626";
    public static final String RAG_AMBER = "#F97316";
    public static final String RAG_GREEN = "#22C55E";
    public static final String RAG_YELLOW = "#FBBF24";
    public static final String RAG_DEFAULT = "#D1D5DB";
    
    // Default Colors
    public static final String WHITE = "#FFFFFF";
    public static final String GRAY = "#9CA3AF";
    public static final String LIGHT_GRAY = "#D1D5DB";
    public static final String DARK_GRAY = "#333";
    
    // Primary Blue
    public static final String PRIMARY_BLUE = "#1E3A8A";
    
    /**
     * Gets the color for a RAG (Red-Amber-Green) status character.
     * 
     * @param ragChar the RAG status character (R, A, or G)
     * @return the hex color code for the status
     */
    public static String getInitiativeRagColor(char ragChar) {
        switch (ragChar) {
            case 'R':
                return RAG_RED;
            case 'A':
                return RAG_AMBER;
            case 'G':
                return RAG_GREEN;
            default:
                return WHITE;
        }
    }
    
    /**
     * Gets the color for a RAG status string.
     * 
     * @param rag the RAG status string (red, amber, green, or yellow)
     * @return the hex color code for the status
     */
    public static String getRagColor(String rag) {
        if (rag == null) {
            return RAG_DEFAULT;
        }
        switch (rag.trim().toLowerCase()) {
            case "red":
                return RAG_RED;
            case "amber":
                return RAG_AMBER;
            case "yellow":
                return RAG_YELLOW;
            case "green":
                return RAG_GREEN;
            default:
                return RAG_DEFAULT;
        }
    }
}

