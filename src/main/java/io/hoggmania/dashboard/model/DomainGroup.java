package io.hoggmania.dashboard.model;

import java.util.List;

public class DomainGroup {
    public String domainName;
    public String icon; // optional icon to render next to domain heading
    public List<RenderItem> items;
    public float headerX;
    public float headerY;
    public float headerTextY;
    public float headerIconY;
    public float headerWidth;
    public float headerTextX;
    public boolean showHeader = true;

    public DomainGroup() {}

    public DomainGroup(String domainName, String icon, List<RenderItem> items) {
        this.domainName = domainName;
        this.icon = icon;
        this.items = items;
    }

    @Override
    public String toString() {
        return "DomainGroup [domainName=" + domainName + ", icon=" + icon + ", headerX=" + headerX
                + ", headerY=" + headerY + ", items=" + items + "]";
    }

    
}
