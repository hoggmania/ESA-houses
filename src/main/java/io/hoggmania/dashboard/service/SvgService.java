package io.hoggmania.dashboard.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.logging.Log;
import io.quarkus.qute.Location;
import io.hoggmania.dashboard.model.RenderItem;
import io.hoggmania.dashboard.model.DomainGroup;
import io.hoggmania.dashboard.model.ESA;
import io.hoggmania.dashboard.model.Governance;
import io.hoggmania.dashboard.model.Capabilities;
import io.hoggmania.dashboard.model.ComponentItem;
import io.hoggmania.dashboard.model.Domain;


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
        final int maxRowsPerColumn = 8;
        final float domainSectionGap = headerOffset + headerHeight; // leave enough room for next domain header
        final float textLeftX = 12f;
        final float textCenterX = boxW / 2f;
        final float iconPosX = boxW - 20f;

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
            java.util.List<DomainColumnLayout> columns = buildDomainColumns(domains, boxW, spaceW, maxRowsPerColumn);
            float currentX = domainStartX;
            float rowTopY = domainStartY;
            float rowBottomY = rowTopY;
            int columnsInRow = 0;
            java.util.Map<String, HeaderSpan> headerSpans = new java.util.HashMap<>();
            for (DomainColumnLayout column : columns) {
                float columnWidth = column.spacer ? column.width : boxW;
                if (columnsInRow >= maxDomainColumnsPerRow || currentX + columnWidth > rightLimit) {
                    rowTopY = rowBottomY + rowGapY;
                    rowBottomY = rowTopY;
                    currentX = domainStartX;
                    columnsInRow = 0;
                }

                if (column.spacer) {
                    currentX += columnWidth + gapX;
                    columnsInRow++;
                    continue;
                }

                float sectionStartY = rowTopY;
                float columnBottom = rowTopY;

                for (DomainSectionChunk section : column.sections) {
                    java.util.List<RenderItem> domainItems = new java.util.ArrayList<>();
                    java.util.List<ComponentItem> comps = section.components;
                    for (int compIdx = 0; compIdx < comps.size(); compIdx++) {
                        var comp = comps.get(compIdx);
                        float y = sectionStartY + compIdx * (boxH + gapY);

                        String gradientId = "grad_dom_" + domainGroups.size() + "_" + compIdx;
                        String border = "#333";
                        if (comp.rag != null && "red".equalsIgnoreCase(comp.rag)) border = "red";

                        String iconKey2 = comp.icon;
                        String iconId2 = (iconKey2 != null && !iconKey2.isBlank()) ? ("icon-" + iconKey2.trim()) : null;

                        domainItems.add(new RenderItem(
                            currentX, y,
                            comp.name != null ? comp.name : "",
                            comp.capability != null ? comp.capability : "",
                            section.domainName,
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
                    }

                    DomainGroup group = new DomainGroup(section.domainName, section.iconId, domainItems);
                    group.headerX = currentX;
                    group.headerY = sectionStartY - headerOffset;
                    group.headerTextY = group.headerY + 15f;
                    group.headerIconY = group.headerY + 3f;
                    group.headerWidth = columnWidth;
                    group.headerTextX = columnWidth / 2f;
                    group.showHeader = true;

                    String headerKey = section.domainName + "@" + (int) rowTopY;
                    HeaderSpan span = headerSpans.get(headerKey);
                    if (span == null) {
                        span = new HeaderSpan();
                        span.startX = currentX;
                        span.rowY = group.headerY;
                        span.width = columnWidth;
                        span.primaryGroup = group;
                        headerSpans.put(headerKey, span);
                    } else {
                        span.width = (currentX + columnWidth) - span.startX;
                        group.showHeader = false;
                        group.headerWidth = 0;
                        group.headerTextX = 0;
                        if (span.primaryGroup != null) {
                            span.primaryGroup.headerWidth = span.width;
                            span.primaryGroup.headerTextX = span.width / 2f;
                        }
                    }

                    domainGroups.add(group);

                    if (!comps.isEmpty()) {
                        float lastY = sectionStartY + (comps.size() - 1) * (boxH + gapY);
                        float sectionBottom = lastY + boxH;
                        columnBottom = Math.max(columnBottom, sectionBottom);
                        sectionStartY = sectionBottom + domainSectionGap;
                    }
                }

                rowBottomY = Math.max(rowBottomY, columnBottom);
                currentX += columnWidth + gapX;
                columnsInRow++;
            }

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
            .data("initiativeCircleX", (int)(boxW - 10))
            .data("initiativeCircleY", (int)(boxH - 12))
            .data("initiativeTextY", (int)(boxH - 8));
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

    private java.util.List<DomainColumnLayout> buildDomainColumns(java.util.List<Domain> domains, float boxWidth, float spaceWidth, int maxRowsPerColumn) {
        java.util.List<DomainColumnLayout> columns = new java.util.ArrayList<>();
        for (Domain domain : domains) {
            if (domain == null) {
                continue;
            }
            String domainName = domain.domain != null ? domain.domain : "Domain";
            if ("SPACE".equalsIgnoreCase(domainName)) {
                columns.add(DomainColumnLayout.spacer(spaceWidth));
                continue;
            }
            java.util.List<ComponentItem> components = domain.components;
            int originalSize = components != null ? components.size() : 0;
            boolean domainIsSmall = originalSize > 0 && originalSize <= 3;
            java.util.List<DomainSectionChunk> chunks = splitDomainIntoChunks(domainName, domain.icon, components, maxRowsPerColumn);
            for (DomainSectionChunk chunk : chunks) {
                int rows = chunk.size();
                if (rows == 0) continue;
                boolean isSmall = domainIsSmall && rows <= 3;
                DomainColumnLayout target = columns.isEmpty() ? null : columns.get(columns.size() - 1);
                if (isSmall && target != null && target.canAcceptSmall(rows, maxRowsPerColumn)) {
                    target.sections.add(chunk);
                    target.rowsUsed += rows;
                } else {
                    DomainColumnLayout newCol = DomainColumnLayout.normal(boxWidth, isSmall);
                    newCol.sections.add(chunk);
                    newCol.rowsUsed = rows;
                    columns.add(newCol);
                }
            }
        }
        return columns;
    }

    private java.util.List<DomainSectionChunk> splitDomainIntoChunks(String domainName, String icon, java.util.List<ComponentItem> components, int maxRowsPerColumn) {
        java.util.List<DomainSectionChunk> chunks = new java.util.ArrayList<>();
        if (components == null || components.isEmpty()) {
            return chunks;
        }
        String iconId = (icon != null && !icon.isBlank()) ? ("icon-" + icon.trim()) : null;
        int total = components.size();
        if (total <= maxRowsPerColumn) {
            chunks.add(new DomainSectionChunk(domainName, iconId, new java.util.ArrayList<>(components)));
            return chunks;
        }
        int firstChunk = (int) Math.ceil(total / 2.0);
        firstChunk = Math.min(firstChunk, maxRowsPerColumn);
        chunks.add(new DomainSectionChunk(domainName, iconId, new java.util.ArrayList<>(components.subList(0, firstChunk))));
        int index = firstChunk;
        int remaining = total - firstChunk;
        while (remaining > 0) {
            int chunkSize = Math.min(maxRowsPerColumn, remaining);
            chunks.add(new DomainSectionChunk(domainName, iconId, new java.util.ArrayList<>(components.subList(index, index + chunkSize))));
            index += chunkSize;
            remaining -= chunkSize;
        }
        return chunks;
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
        return trimmed + "...";
    }

    private static class DomainSectionChunk {
        final String domainName;
        final String iconId;
        final java.util.List<ComponentItem> components;

        DomainSectionChunk(String domainName, String iconId, java.util.List<ComponentItem> components) {
            this.domainName = domainName;
            this.iconId = iconId;
            this.components = components;
        }

        int size() {
            return components != null ? components.size() : 0;
        }
    }

    private static class DomainColumnLayout {
        final boolean spacer;
        final float width;
        final java.util.List<DomainSectionChunk> sections = new java.util.ArrayList<>();
        int rowsUsed = 0;
        boolean smallOnly;

        DomainColumnLayout(boolean spacer, float width, boolean smallOnly) {
            this.spacer = spacer;
            this.width = width;
            this.smallOnly = smallOnly;
        }

        static DomainColumnLayout spacer(float width) {
            return new DomainColumnLayout(true, width, true);
        }

        static DomainColumnLayout normal(float width, boolean smallOnly) {
            return new DomainColumnLayout(false, width, smallOnly);
        }

        boolean canAcceptSmall(int additionalRows, int maxRowsPerColumn) {
            return !spacer && smallOnly && rowsUsed + additionalRows <= maxRowsPerColumn;
        }
    }

    private static class HeaderSpan {
        float startX;
        float rowY;
        float width;
        DomainGroup primaryGroup;
    }
}
