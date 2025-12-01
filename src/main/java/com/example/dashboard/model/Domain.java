package com.example.dashboard.model;

import java.util.List;

public class Domain {
    public String domain;
    public String icon; // optional icon for the domain heading
    public List<ComponentItem> components;
    @Override
    public String toString() {
        return "Domain [domain=" + domain + ", icon=" + icon + ", components=" + components + "]";
    }


    
}
