package com.example.dashboard.model;

import java.util.List;

public class Domain {
    public String domain;
    public List<ComponentItem> components;
    @Override
    public String toString() {
        return "Domain [domain=" + domain + ", components=" + components + "]";
    }


    
}
