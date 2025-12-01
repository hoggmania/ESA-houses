package io.hoggmania.dashboard.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAlias;

public class Governance {
    public String title;
    @JsonAlias({"components", "components"})
    public List<ComponentItem> components;
    @Override
    public String toString() {
        return "Governance [title=" + title + ", components=" + components + "]";
    }

    
}
