package io.hoggmania.dashboard.model;

public class RenderItem {
    public float x;
    public float y;
    public String name;
    public String capability;
    public String domain;
    public String fill; // deprecated - use capabilityColor/maturityColor
    public String border;
    public String capabilityColor; // top-left gradient color
    public String maturityColor;   // bottom-right gradient color
    public String gradientId;      // unique gradient identifier
    public int initiatives; // if >0 display count badge
    public boolean showInitiatives;
    public boolean doubleBorder; // if true, render double border
    public String iconId; // symbol id for icon, e.g., "icon-shield"
    public java.util.List<String> nameLines;
    public java.util.List<String> capabilityLines;
    public float textX;
    public String textAnchor;
    public float iconX;
    public String initiativeStroke;

    public RenderItem() {}

    public RenderItem(float x, float y, String name, String capability, String domain, 
                      String capabilityColor, String maturityColor, String gradientId,
                      String border, boolean doubleBorder, String iconId) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.capability = capability;
        this.domain = domain;
        this.fill = capabilityColor; // backward compat
        this.capabilityColor = capabilityColor;
        this.maturityColor = maturityColor;
        this.gradientId = gradientId;
        this.border = border;
        this.initiatives = 0;
        this.showInitiatives = false;
        this.doubleBorder = doubleBorder;
        this.iconId = iconId;
        this.nameLines = java.util.Collections.singletonList(this.name != null ? this.name : "");
        this.capabilityLines = java.util.Collections.singletonList(this.capability != null ? this.capability : "");
        this.textX = 0;
        this.textAnchor = "middle";
        this.iconX = 0;
        this.initiativeStroke = "#FFFFFF";
    }

    public boolean hasInitiatives() {
        return initiatives > 0;
    }

    public int getInitiatives() {
        return initiatives;
    }
}
