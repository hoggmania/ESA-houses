package io.hoggmania.dashboard.model;

/**
 * Flattened initiative data used by the initiatives render template.
 */
public class InitiativeRow {
    public final String capability;
    public final String component;
    public final String componentCapability;
    public final String key;
    public final String keyHref;
    public final String rag;
    public final String ragColor;
    public final String summary;
    public final String businessBenefit;
    public final String riskAppetite;
    public final String toolId;
    public final String dueDate;
    public final String statusColor;
    public final String maturityColor;

    public InitiativeRow(
            String capability,
            String component,
            String key,
            String keyHref,
            String rag,
            String ragColor,
            String summary,
            String businessBenefit,
            String riskAppetite,
            String toolId,
            String dueDate,
            String statusColor,
            String maturityColor,
            String componentCapability) {
        this.capability = capability;
        this.component = component;
        this.componentCapability = componentCapability;
        this.key = key;
        this.keyHref = keyHref;
        this.rag = rag;
        this.ragColor = ragColor;
        this.summary = summary;
        this.businessBenefit = businessBenefit;
        this.riskAppetite = riskAppetite;
        this.toolId = toolId;
        this.dueDate = dueDate;
        this.statusColor = statusColor;
        this.maturityColor = maturityColor;
    }
}
