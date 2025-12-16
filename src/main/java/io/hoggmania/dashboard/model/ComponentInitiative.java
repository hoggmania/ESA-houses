package io.hoggmania.dashboard.model;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Describes a single initiative entry attached to a component.
 */
public class ComponentInitiative {
    public String key;
    public String link;
    public String summary;
    @JsonAlias({"businessBenefit", "business_benefit"})
    public String businessBenefit;
    @JsonAlias({"riskAppetite", "risk_appetite"})
    public String riskAppetite;
    @JsonAlias({"toolId", "tool_id", "pmToolId"})
    public String toolId;
    @JsonAlias({"dueDate", "due_date"})
    public String dueDate;
    public String rag;
    @JsonAlias({"issueType", "issue_type"})
    public String issueType;
}

