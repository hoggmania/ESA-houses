package io.hoggmania.dashboard.config;

/**
 * Configuration constants for dashboard layout and styling.
 * All layout dimensions and spacing values are defined here for easy customization.
 */
public final class DashboardLayoutConfig {
    
    private DashboardLayoutConfig() {
        // Prevent instantiation
    }
    
    // Canvas dimensions
    public static final float DEFAULT_CANVAS_WIDTH = 1400f;
    public static final float A4_WIDTH_MM = 297f;
    
    // Component box dimensions
    public static final float BOX_WIDTH_DIVISOR = 7f; // maxDomainColumnsPerRow
    public static final float BOX_HEIGHT = 45f;
    
    // Spacing
    public static final float GAP_X = 16f;
    public static final float GAP_Y = 12f;
    public static final float ROW_GAP_Y = 60f;
    public static final float LEFT_MARGIN = 10f;
    
    // Header dimensions
    public static final float HEADER_OFFSET = 35f;
    public static final float HEADER_HEIGHT = 22f;
    
    // Governance section
    public static final float GOVERNANCE_HEADER_Y = 50f;
    public static final float GOVERNANCE_HEADER_HEIGHT = 25f;
    public static final float GOVERNANCE_HEADER_TO_ROW_GAP = 5f;
    public static final float GOVERNANCE_ROW_GAP = GAP_Y;
    public static final float GOVERNANCE_TO_CAPABILITIES_GAP = 30f;
    
    // Capabilities section
    public static final float CAPABILITIES_HEADER_TO_DOMAINS_GAP = 75f;
    
    // Text layout
    public static final int NAME_CHARS_PER_LINE = 24;
    public static final int CAPABILITY_CHARS_PER_LINE = 22;
    public static final int MAX_NAME_LINES = 2;
    public static final int MAX_CAPABILITY_LINES = 1;
    
    // Column layout
    public static final int MAX_DOMAIN_COLUMNS_PER_ROW = 7;
    public static final int MAX_ROWS_PER_COLUMN = 8;
    public static final float SPACE_COLUMN_WIDTH_FACTOR = 1f / 3f; // SPACE column width: 1/3 of normal
    
    // Domain section
    public static final float DOMAIN_SECTION_GAP = HEADER_OFFSET + HEADER_HEIGHT;
    public static final float DOMAIN_START_X = 20f;
    
    // Text positioning within boxes
    public static final float TEXT_LEFT_X = 12f;
    public static final float ICON_POS_X_OFFSET = 20f;
    
    // Legend
    public static final int LEGEND_HEIGHT = 90;
    public static final int BOTTOM_MARGIN = 20;
    public static final int LEGEND_TOP_MARGIN = 30;
    
    // Legend item spacing
    public static final int LEGEND_ITEM_WIDTH = 140;
    public static final int LEGEND_STATUS_START_X = 0;
    public static final int LEGEND_MATURITY_START_X = 720;
    
    // Title bar
    public static final float MAIN_TITLE_Y = 10f;
    public static final float MAIN_TITLE_HEIGHT = 35f;
    public static final float MAIN_TITLE_BORDER_RADIUS = 4f;
    
    // Section title bars
    public static final float SECTION_TITLE_BORDER_RADIUS = 3f;
    
    /**
     * Calculates the box width based on available canvas width.
     * 
     * @param canvasWidth the total canvas width
     * @param leftMargin the left margin
     * @param domainStartX the starting X position for domains
     * @param maxColumns the maximum number of columns per row
     * @return the calculated box width
     */
    public static float calculateBoxWidth(float canvasWidth, float leftMargin, float domainStartX, int maxColumns) {
        float rightLimit = canvasWidth - leftMargin;
        float availableWidth = Math.max(200f, rightLimit - domainStartX);
        return (availableWidth - GAP_X * (maxColumns - 1)) / maxColumns;
    }
    
    /**
     * Calculates the space column width (typically 1/3 of normal box width).
     * 
     * @param boxWidth the normal box width
     * @return the space column width
     */
    public static float calculateSpaceWidth(float boxWidth) {
        return boxWidth * SPACE_COLUMN_WIDTH_FACTOR;
    }
}

