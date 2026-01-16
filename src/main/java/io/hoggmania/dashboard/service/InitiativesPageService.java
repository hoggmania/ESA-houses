package io.hoggmania.dashboard.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.hoggmania.dashboard.model.ComponentInitiative;
import io.hoggmania.dashboard.model.ComponentItem;
import io.hoggmania.dashboard.model.Domain;
import io.hoggmania.dashboard.model.ESA;
import io.hoggmania.dashboard.model.Governance;
import io.hoggmania.dashboard.model.InitiativeRow;
import io.hoggmania.dashboard.util.ColorPalette;
import io.hoggmania.dashboard.util.InitiativeAnchor;
import io.hoggmania.dashboard.util.StringUtils;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for rendering HTML pages that list all initiatives from an ESA model.
 * Extracts initiatives from all components across governance and capability domains,
 * formats dates, and applies RAG coloring.
 */
@ApplicationScoped
public class InitiativesPageService {

    private static final DateTimeFormatter TARGET_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MMM/yy", Locale.UK);
    private static final DateTimeFormatter[] INPUT_DATE_FORMATS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.UK)
    };
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy HH:mm 'UTC'", Locale.UK);

    @Inject
    @Location("initiatives.html.qute")
    Template initiativesTemplate;

    /**
     * Renders a standalone HTML page listing all initiatives.
     * 
     * @param root the ESA model
     * @return the rendered HTML page
     */
    public String renderInitiativesPage(ESA root) {
        return renderWithMode(root, true, null);
    }

    /**
     * Renders a standalone HTML page listing all initiatives with the raw JSON payload displayed.
     * 
     * @param root the ESA model
     * @param payloadRaw the raw JSON payload as a string (optional, for display purposes)
     * @return the rendered HTML page
     */
    public String renderInitiativesPage(ESA root, String payloadRaw) {
        return renderWithMode(root, true, payloadRaw);
    }

    /**
     * Renders an HTML fragment (without full page structure) listing all initiatives.
     * Suitable for embedding in other pages.
     * 
     * @param root the ESA model
     * @return the rendered HTML fragment
     */
    public String renderInitiativesFragment(ESA root) {
        return renderWithMode(root, false, null);
    }

    private String renderWithMode(ESA root, boolean standalone, String payloadRaw) {
        ESA.validateESA(root);
        List<InitiativeRow> rows = collectInitiatives(root);
        TemplateInstance ti = initiativesTemplate
                .data("title", safeText(root != null ? root.title : null, "Initiatives Overview"))
                .data("rows", rows)
                .data("generatedOn", TIMESTAMP_FORMAT.format(ZonedDateTime.now(ZoneOffset.UTC)))
                .data("standalone", standalone)
                .data("payloadRaw", payloadRaw);
        return ti.render();
    }

    private List<InitiativeRow> collectInitiatives(ESA root) {
        List<InitiativeRow> rows = new ArrayList<>();
        if (root == null) {
            return rows;
        }

        Governance governance = root.governance;
        if (governance != null && governance.components != null) {
            String capabilityLabel = safeText(governance.title, "Governance");
            for (ComponentItem comp : governance.components) {
                addComponentInitiatives(rows, capabilityLabel, comp);
            }
        }

        if (root.capabilities != null && root.capabilities.domains != null) {
            for (Domain domain : root.capabilities.domains) {
                if (domain == null || domain.components == null || domain.components.isEmpty()) {
                    continue;
                }
                String capabilityLabel = safeText(domain.domain, "Capability");
                for (ComponentItem comp : domain.components) {
                    addComponentInitiatives(rows, capabilityLabel, comp);
                }
            }
        }
        return rows;
    }

    private void addComponentInitiatives(List<InitiativeRow> rows, String capabilityLabel, ComponentItem component) {
        if (component == null || component.initiativeDetails == null || component.initiativeDetails.isEmpty()) {
            return;
        }
        String componentLabel = safeText(component.name, component.capability, "Component");
        String componentCapability = safeText(component.capability, "Capability");
        String statusColor = component != null && component.status != null ? component.status.hex : "#9CA3AF";
        String maturityColor = component != null && component.maturity != null ? component.maturity.hex : "#D1D5DB";
        for (ComponentInitiative initiative : component.initiativeDetails) {
            if (initiative == null) {
                continue;
            }
            rows.add(buildRow(capabilityLabel, componentLabel, componentCapability, initiative, statusColor, maturityColor));
        }
    }

    private InitiativeRow buildRow(String capabilityLabel, String componentLabel, String componentCapability, ComponentInitiative initiative,
            String statusColor, String maturityColor) {
        String key = firstNonBlank(initiative.key, "TBC");
        String rag = firstNonBlank(initiative.rag, "");
        String summary = firstNonBlank(initiative.summary, "");
        String benefit = firstNonBlank(initiative.businessBenefit, "");
        String riskAppetite = firstNonBlank(initiative.riskAppetite, "");
        String toolId = firstNonBlank(initiative.toolId, "In-Demand");
        String dueDate = formatDate(initiative.dueDate);
        String anchorId = InitiativeAnchor.toAnchorId(key);
        return new InitiativeRow(
                capabilityLabel,
                componentLabel,
                key,
                blankToNull(initiative.link),
                anchorId,
                rag.toUpperCase(Locale.ENGLISH),
                ragColor(rag),
                summary,
                benefit,
                riskAppetite,
                toolId,
                dueDate,
                statusColor,
                maturityColor,
                componentCapability
        );
    }

    private String ragColor(String rag) {
        return ColorPalette.getRagColor(rag);
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        for (DateTimeFormatter parser : INPUT_DATE_FORMATS) {
            try {
                LocalDate date = LocalDate.parse(trimmed, parser);
                return TARGET_DATE_FORMAT.format(date);
            } catch (DateTimeParseException ignored) {
                // try next pattern
            }
        }
        return trimmed;
    }

    private String safeText(String primary, String fallback) {
        return safeText(primary, null, fallback);
    }

    private String safeText(String primary, String secondary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (secondary != null && !secondary.isBlank()) {
            return secondary.trim();
        }
        return fallback;
    }

    private String firstNonBlank(String value, String fallback) {
        return StringUtils.firstNonBlank(value, fallback);
    }

    private String blankToNull(String value) {
        return StringUtils.blankToNull(value);
    }
}
