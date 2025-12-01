package io.hoggmania.dashboard.model;

import java.util.List;

public class DashboardModel {
    public String title;
    public List<ComponentItem> components;

    public DashboardModel() {}

    @Override
    public String toString() {
        return "DashboardModel [title=" + title + ", components=" + components + "]";
    }

    
}
