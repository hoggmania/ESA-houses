package com.example.dashboard.model;

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

    public RenderItem() {}

    public RenderItem(float x, float y, String name, String capability, String domain, 
                      String capabilityColor, String maturityColor, String gradientId,
                      String border, boolean doubleBorder) {
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
    }

    public boolean hasInitiatives() {
        return initiatives > 0;
    }

    public int getInitiatives() {
        return initiatives;
    }
}
