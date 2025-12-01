package com.example.dashboard.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.logging.Log;
import io.quarkus.qute.Location;
import com.example.dashboard.model.RenderItem;
import com.example.dashboard.model.DomainGroup;
import com.example.dashboard.model.ESA;
import com.example.dashboard.model.Governance;
import com.example.dashboard.model.Capabilities;
import com.example.dashboard.model.ComponentItem;
import com.example.dashboard.model.Domain;


import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class SvgService {

    @Inject
    @Location("dashboard.svg.qute")
    Template dashboard; // Explicitly locate templates/dashboard.svg.qute



    public String renderSvg(ESA root) {
        // Validate input first
        ESA.validateESA(root);
        Log.info(root.toString());
        // Layout constants
        final int maxDomainColumnsPerRow = 7;
        final float gapX = 16f;
        final float gapY = 12f;
        final float canvasWidth = 1400f;
        final float leftMargin = 10f;
        final float rightLimit = canvasWidth - leftMargin;
        final float domainStartX = 20f;
        final float availableWidth = Math.max(200f, rightLimit - domainStartX);
        final float boxW = (availableWidth - gapX * (maxDomainColumnsPerRow - 1)) / maxDomainColumnsPerRow;
        final float boxH = 45f;
        final float headerOffset = 35f; // distance between header bar and first component row
        final float headerHeight = 22f;
        final float rowGapY = 60f; // additional spacing between wrapped rows
        final float spaceW = boxW / 3f; // SPACE column width: 1/3 of normal
        final float governanceHeaderY = 50f;
        final float governanceHeaderHeight = 25f;
        final float governanceHeaderToRowGap = 5f;
        final float governanceRowGap = gapY;
        final float governanceToCapabilitiesGap = 30f;
        final float capabilitiesHeaderToDomainsGap = 75f;
        final int nameCharsPerLine = 24;
        final int capabilityCharsPerLine = 22;
        final int maxNameLines = 2;
        final int maxCapabilityLines = 1;
        final float textLeftX = 12f;
        final float textCenterX = boxW / 2f;
        final float iconPosX = boxW - 46f;

        String title = root != null ? root.title : null;
        float domainStartY = 230f;
        float capabilitiesHeaderY = 155f;
        float capabilitiesHeaderTextY = capabilitiesHeaderY + 18f;
        float capabilitiesIconY = capabilitiesHeaderY + 3f;
        float governanceContentBottom = governanceHeaderY + governanceHeaderHeight + governanceHeaderToRowGap + boxH;

        // Governance items (horizontal row)
        java.util.List<RenderItem> governanceItems = new java.util.ArrayList<>();
        Governance gov = root != null ? root.governance : null;
        if (gov != null && gov.components != null) {
            String govTitle = gov.title;
            float govStartX = 20f;
            float govCurrentX = govStartX;
            float govRowTopY = governanceHeaderY + governanceHeaderHeight + governanceHeaderToRowGap;
            float govRowBottom = govRowTopY + boxH;
            boolean hasGovItems = false;
            for (int i = 0; i < gov.components.size(); i++) {
                ComponentItem comp = gov.components.get(i);
                if (govCurrentX + boxW > rightLimit) {
                    govRowTopY = govRowBottom + governanceRowGap;
                    govRowBottom = govRowTopY + boxH;
                    govCurrentX = govStartX;
                }
                float x = govCurrentX;
                float y = govRowTopY;
                String gradientId = "grad_gov_" + i;
                String border = "#333";
                if (comp.rag != null && "red".equalsIgnoreCase(comp.rag)) border = "red";

                // Determine icon
                String iconKey = comp.icon;
                String iconId = (iconKey != null && !iconKey.isBlank()) ? ("icon-" + iconKey.trim()) : null;

                governanceItems.add(new RenderItem(
                    x, y,
                    comp.name != null ? comp.name : "",
                    comp.capability != null ? comp.capability : "",
                    govTitle != null ? govTitle : "",
                    comp.status.hex,
                    comp.maturity.hex,
                    gradientId,
                    border,
                    comp.doubleBorder,
                    iconId
                ));
                RenderItem lastGov = governanceItems.get(governanceItems.size()-1);
                lastGov.initiatives = comp.initiatives;
                lastGov.showInitiatives = lastGov.initiatives > 0;
                lastGov.nameLines = wrapText(lastGov.name, nameCharsPerLine, maxNameLines);
                lastGov.capabilityLines = wrapText(lastGov.capability, capabilityCharsPerLine, maxCapabilityLines);
                configureTextLayout(lastGov, textCenterX, textLeftX, iconPosX);

                govCurrentX += boxW + gapX;
                govRowBottom = Math.max(govRowBottom, y + boxH);
                hasGovItems = true;
            }
            if (hasGovItems) {
                governanceContentBottom = govRowBottom;
            }
        }

        capabilitiesHeaderY = governanceContentBottom + governanceToCapabilitiesGap;
        capabilitiesHeaderTextY = capabilitiesHeaderY + 18f;
        capabilitiesIconY = capabilitiesHeaderY + 3f;
        domainStartY = capabilitiesHeaderY + capabilitiesHeaderToDomainsGap;

        // Capabilities domains (columns)
        java.util.List<DomainGroup> domainGroups = new java.util.ArrayList<>();
        Capabilities capabilities = root != null ? root.capabilities : null;
        java.util.List<Domain> domains = capabilities != null ? capabilities.domains : null;
        int legendY = 0;
        if (domains != null) {
            // Start domains lower to leave a visual gap after capabilities title bar
            float currentX = domainStartX;
            float rowTopY = domainStartY;
            float rowBottomY = rowTopY;
            int columnsInRow = 0;
                
            for (int domainIdx = 0; domainIdx < domains.size(); domainIdx++) {
                Domain d = domains.get(domainIdx);
                String domainName = d != null && d.domain != null ? d.domain : ("Domain " + (domainIdx + 1));
                boolean isSpace = "SPACE".equalsIgnoreCase(domainName);
                String domainIcon = (d != null && d.icon != null && !d.icon.isBlank()) ? ("icon-" + d.icon.trim()) : null;
                float columnWidth = isSpace ? spaceW : boxW;

                if (columnsInRow >= maxDomainColumnsPerRow || currentX + columnWidth > rightLimit) {
                    // Wrap to a new row
                    rowTopY = rowBottomY + rowGapY;
                    rowBottomY = rowTopY;
                    currentX = domainStartX;
                    columnsInRow = 0;
                }
                
                java.util.List<RenderItem> domainItems = new java.util.ArrayList<>();
                java.util.List<com.example.dashboard.model.ComponentItem> comps = d != null ? d.components : null;
                float headerY = rowTopY - headerOffset;
                float headerTextY = headerY + 15f;
                float headerIconY = headerY + 3f;
                float columnBottom = headerY + headerHeight;
                
                // Only render components if not a SPACE domain
                if (!isSpace && comps != null) {
                    for (int compIdx = 0; compIdx < comps.size(); compIdx++) {
                        var comp = comps.get(compIdx);
                        float y = rowTopY + compIdx * (boxH + gapY);
                        
                        String gradientId = "grad_dom_" + domainIdx + "_" + compIdx;
                        String border = "#333";
                        if (comp.rag != null && "red".equalsIgnoreCase(comp.rag)) border = "red";

                        // Determine icon
                        String iconKey2 = comp.icon;
                        String iconId2 = (iconKey2 != null && !iconKey2.isBlank()) ? ("icon-" + iconKey2.trim()) : null;

                        domainItems.add(new RenderItem(
                            currentX, y,
                            comp.name != null ? comp.name : "",
                            comp.capability != null ? comp.capability : "",
                            domainName,
                            comp.status.hex,
                            comp.maturity.hex,
                            gradientId,
                            border,
                            comp.doubleBorder,
                            iconId2
                        ));


                        RenderItem lastDomainItem = domainItems.get(domainItems.size()-1);
                        lastDomainItem.initiatives = comp.initiatives;
                        lastDomainItem.showInitiatives = lastDomainItem.initiatives > 0;
                        lastDomainItem.nameLines = wrapText(lastDomainItem.name, nameCharsPerLine, maxNameLines);
                        lastDomainItem.capabilityLines = wrapText(lastDomainItem.capability, capabilityCharsPerLine, maxCapabilityLines);
                        configureTextLayout(lastDomainItem, textCenterX, textLeftX, iconPosX);
                        columnBottom = Math.max(columnBottom, y + boxH);
                    }
                    rowBottomY = Math.max(rowBottomY, columnBottom);
                }
                
                DomainGroup group = new DomainGroup(domainName, domainIcon, domainItems);
                group.headerX = currentX;
                group.headerY = headerY;
                group.headerTextY = headerTextY;
                group.headerIconY = headerIconY;
                domainGroups.add(group);
                
                columnsInRow++;
                // Advance currentX based on column type
                if (isSpace) {
                    currentX += spaceW + gapX;
                } else {
                    currentX += boxW + gapX;
                }
            }

            // After laying out all domains, compute where the legend should start
            float legendStartY = rowBottomY + 30f;
            legendY = (int) legendStartY;
        } else {
            legendY = (int) (domainStartY + 30f);
        }

        // Compute legend position defaults to below first row when no domains exist
        if (legendY == 0) {
            legendY = (int)(domainStartY + 30f);
        }

        // Prepare legend data: Status on left, Maturity on right with precomputed x offsets
        java.util.List<java.util.Map<String, Object>> statusLegend = new java.util.ArrayList<>();
        statusLegend.add(java.util.Map.of("label", ComponentItem.Status.NOT_EXISTING.displayName, "color", ComponentItem.Status.NOT_EXISTING.hex, "x", 0));
        statusLegend.add(java.util.Map.of("label", ComponentItem.Status.LOW.displayName, "color", ComponentItem.Status.LOW.hex, "x", 140));
        statusLegend.add(java.util.Map.of("label", ComponentItem.Status.MEDIUM.displayName, "color", ComponentItem.Status.MEDIUM.hex, "x", 280));
        statusLegend.add(java.util.Map.of("label", ComponentItem.Status.HIGH.displayName, "color", ComponentItem.Status.HIGH.hex, "x", 420));
        statusLegend.add(java.util.Map.of("label", ComponentItem.Status.EFFECTIVE.displayName, "color", ComponentItem.Status.EFFECTIVE.hex, "x", 560));

        java.util.List<java.util.Map<String, Object>> maturityLegend = new java.util.ArrayList<>();
        maturityLegend.add(java.util.Map.of("label", ComponentItem.Maturity.NOT_EXISTING.displayName, "color", ComponentItem.Maturity.NOT_EXISTING.hex, "x", 720));
        maturityLegend.add(java.util.Map.of("label", ComponentItem.Maturity.INITIAL.displayName, "color", ComponentItem.Maturity.INITIAL.hex, "x", 860));
        maturityLegend.add(java.util.Map.of("label", ComponentItem.Maturity.REPEATABLE.displayName, "color", ComponentItem.Maturity.REPEATABLE.hex, "x", 1000));
        maturityLegend.add(java.util.Map.of("label", ComponentItem.Maturity.DEFINED.displayName, "color", ComponentItem.Maturity.DEFINED.hex, "x", 1140));
        maturityLegend.add(java.util.Map.of("label", ComponentItem.Maturity.MANAGED.displayName, "color", ComponentItem.Maturity.MANAGED.hex, "x", 1280));
        maturityLegend.add(java.util.Map.of("label", ComponentItem.Maturity.OPTIMISED.displayName, "color", ComponentItem.Maturity.OPTIMISED.hex, "x", 1420));

        TemplateInstance data = dashboard
            .data("title", title)
            .data("governanceTitle", gov != null ? gov.title : null)
            .data("governanceItems", governanceItems)
            .data("capabilitiesTitle", capabilities != null ? capabilities.title : null)
            .data("capabilitiesHeaderY", (int) capabilitiesHeaderY)
            .data("capabilitiesTextY", (int) capabilitiesHeaderTextY)
            .data("capabilitiesIconY", (int) capabilitiesIconY)
            .data("capabilitiesIcon", (capabilities != null && capabilities.icon != null && !capabilities.icon.isBlank()) ? ("icon-" + capabilities.icon.trim()) : null)
            .data("esaIcon", (root != null && root.icon != null && !root.icon.isBlank()) ? ("icon-" + root.icon.trim()) : null)
            .data("domainGroups", domainGroups)
            .data("boxW", (int) boxW)
            .data("boxH", (int) boxH)
            .data("halfBoxW", boxW / 2f)
            .data("pageCenter", 700) // viewBox width 1400 -> center 700
            .data("legendY", legendY)
            .data("statusLegend", statusLegend)
            .data("maturityLegend", maturityLegend);
            // coordinates for initiatives badge inside a box (local to group)
            data = data
            .data("initiativeCircleX", (int)(boxW - 22))
            .data("initiativeCircleY", 18)
            .data("initiativeTextY", 22);
        String rendered = data.render();
        // Basic sanity check in logs for debugging in tests
        if (rendered != null) {
            String head = rendered.substring(0, Math.min(40, rendered.length())).replaceAll("\n", "\\n");
            Log.debugf("Rendered SVG head: %s", head);
        } else {
            Log.warn("Rendered SVG is null");
        }
        return rendered;
    }

    public byte[] renderPngFromSvg(String svgContent, float dpi) throws Exception {
        // Convert SVG string to PNG bytes using Batik
        PNGTranscoder transcoder = new PNGTranscoder();
        // set DPI if needed
        transcoder.addTranscodingHint(PNGTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 25.4f / dpi);

        TranscoderInput input = new TranscoderInput(new ByteArrayInputStream(svgContent.getBytes(StandardCharsets.UTF_8)));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TranscoderOutput output = new TranscoderOutput(baos);
        transcoder.transcode(input, output);
        baos.flush();
        return baos.toByteArray();
    }

    private void configureTextLayout(RenderItem item, float textCenterX, float textLeftX, float iconPosX) {
        boolean hasDecorations = item.showInitiatives || (item.iconId != null && !item.iconId.isBlank());
        item.textAnchor = hasDecorations ? "start" : "middle";
        item.textX = hasDecorations ? textLeftX : textCenterX;
        item.iconX = iconPosX;
    }

    private java.util.List<String> wrapText(String value, int maxCharsPerLine, int maxLines) {
        if (value == null || value.isBlank()) {
            return java.util.Collections.singletonList("");
        }
        int safeMaxLines = Math.max(1, maxLines);
        int safeMaxChars = Math.max(1, maxCharsPerLine);
        String[] words = value.trim().split("\\s+");
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        int index = 0;
        while (index < words.length) {
            String word = words[index];
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (candidate.length() <= safeMaxChars) {
                current = new StringBuilder(candidate);
                index++;
            } else {
                if (current.length() == 0) {
                    current = new StringBuilder(word.substring(0, Math.min(safeMaxChars, word.length())));
                    index++;
                }
                lines.add(current.toString());
                current = new StringBuilder();
                if (lines.size() == safeMaxLines) {
                    if (index < words.length) {
                        lines.set(lines.size() - 1, addEllipsis(lines.get(lines.size() - 1), safeMaxChars));
                    }
                    return lines;
                }
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    private String addEllipsis(String line, int maxCharsPerLine) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.length() >= Math.max(1, maxCharsPerLine)) {
            trimmed = trimmed.substring(0, Math.max(0, maxCharsPerLine - 1));
        }
        return trimmed + "…";
    }
}
