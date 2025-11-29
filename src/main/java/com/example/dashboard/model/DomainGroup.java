package com.example.dashboard.model;

import java.util.List;

public class DomainGroup {
    public String domainName;
    public List<RenderItem> items;

    public DomainGroup() {}

    public DomainGroup(String domainName, List<RenderItem> items) {
        this.domainName = domainName;
        this.items = items;
    }

    @Override
    public String toString() {
        return "DomainGroup [domainName=" + domainName + ", items=" + items + "]";
    }

    
}
