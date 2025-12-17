package io.hoggmania.dashboard.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.logging.Log;
import io.quarkus.qute.Location;
import io.hoggmania.dashboard.config.DashboardLayoutConfig;
import io.hoggmania.dashboard.util.ColorPalette;
import io.hoggmania.dashboard.util.StringUtils;
import io.hoggmania.dashboard.util.UrlUtils;
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

    /**
     * Renders an SVG dashboard from an ESA model.
     * The dashboard includes governance components, capability domains, and a legend.
     * Layout is automatically calculated based on the number of components and domains.
     * 
     * @param root the ESA model containing dashboard data
     * @return the rendered SVG as a string
     * @throws ValidationException if the ESA model is invalid
     */
    public String renderSvg(ESA root) {
        // Validate input first
        ESA.validateESA(root);
        Log.info(root.toString());
        // Layout constants from config
        final int maxDomainColumnsPerRow = DashboardLayoutConfig.MAX_DOMAIN_COLUMNS_PER_ROW;
        final float gapX = DashboardLayoutConfig.GAP_X;
        final float gapY = DashboardLayoutConfig.GAP_Y;
        final float canvasWidth = DashboardLayoutConfig.DEFAULT_CANVAS_WIDTH;
        final float leftMargin = DashboardLayoutConfig.LEFT_MARGIN;
        final float rightLimit = canvasWidth - leftMargin;
        final float domainStartX = DashboardLayoutConfig.DOMAIN_START_X;
        final float boxW = DashboardLayoutConfig.calculateBoxWidth(canvasWidth, leftMargin, domainStartX, maxDomainColumnsPerRow);
        final float boxH = DashboardLayoutConfig.BOX_HEIGHT;
        final float headerOffset = DashboardLayoutConfig.HEADER_OFFSET;
        final float headerHeight = DashboardLayoutConfig.HEADER_HEIGHT;
        final float rowGapY = DashboardLayoutConfig.ROW_GAP_Y;
        final float spaceW = DashboardLayoutConfig.calculateSpaceWidth(boxW);
        final float governanceHeaderY = DashboardLayoutConfig.GOVERNANCE_HEADER_Y;
        final float governanceHeaderHeight = DashboardLayoutConfig.GOVERNANCE_HEADER_HEIGHT;
        final float governanceHeaderToRowGap = DashboardLayoutConfig.GOVERNANCE_HEADER_TO_ROW_GAP;
        final float governanceRowGap = DashboardLayoutConfig.GOVERNANCE_ROW_GAP;
        final float governanceToCapabilitiesGap = DashboardLayoutConfig.GOVERNANCE_TO_CAPABILITIES_GAP;
        final float capabilitiesHeaderToDomainsGap = DashboardLayoutConfig.CAPABILITIES_HEADER_TO_DOMAINS_GAP;
        final int nameCharsPerLine = DashboardLayoutConfig.NAME_CHARS_PER_LINE;
        final int capabilityCharsPerLine = DashboardLayoutConfig.CAPABILITY_CHARS_PER_LINE;
        final int maxNameLines = DashboardLayoutConfig.MAX_NAME_LINES;
        final int maxCapabilityLines = DashboardLayoutConfig.MAX_CAPABILITY_LINES;
        final int maxRowsPerColumn = DashboardLayoutConfig.MAX_ROWS_PER_COLUMN;
        final float domainSectionGap = DashboardLayoutConfig.DOMAIN_SECTION_GAP;
        final float textLeftX = DashboardLayoutConfig.TEXT_LEFT_X;
        final float textCenterX = boxW / 2f;
        final float iconPosX = boxW - DashboardLayoutConfig.ICON_POS_X_OFFSET;

        LinkText titleLink = parseLinkField(root != null ? root.title : null);
        String title = sanitizeNullable(titleLink.text);
        java.util.List<InitiativeGradient> initiativeGradients = new java.util.ArrayList<>();
        float domainStartY = 230f;
        float capabilitiesHeaderY = 155f;
        float capabilitiesHeaderTextY = capabilitiesHeaderY + 18f;
        float capabilitiesIconY = capabilitiesHeaderY + 3f;
        float governanceContentBottom = governanceHeaderY + governanceHeaderHeight + governanceHeaderToRowGap + boxH;

        // Governance items (horizontal row)
        java.util.List<RenderItem> governanceItems = new java.util.ArrayList<>();
        Governance gov = root != null ? root.governance : null;
        LinkText govTitleLink = parseLinkField(gov != null ? gov.title : null);
        String govTitle = gov != null ? sanitizeNullable(govTitleLink.text) : null;
        String govTitleHref = govTitleLink.href;
        if (gov != null && gov.components != null) {
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

                String itemName = comp.name != null ? comp.name : "";
                String itemCapability = comp.capability != null ? comp.capability : "";
                LinkText nameLink = parseLinkField(itemName);
                LinkText capabilityLink = parseLinkField(itemCapability);
                String cleanName = nameLink.text != null ? nameLink.text : "";
                String cleanCapability = capabilityLink.text != null ? capabilityLink.text : "";
                governanceItems.add(new RenderItem(
                    x, y,
                    cleanName,
                    cleanCapability,
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
                lastGov.nameLines = escapeLines(wrapText(cleanName, nameCharsPerLine, maxNameLines));
                lastGov.capabilityLines = escapeLines(wrapText(cleanCapability, capabilityCharsPerLine, maxCapabilityLines));
                lastGov.nameHref = nameLink.href;
                lastGov.capabilityHref = capabilityLink.href;
                lastGov.initiativeStroke = computeInitiativeStroke(comp, gradientId, initiativeGradients);
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
        LinkText capabilitiesTitleLink = parseLinkField(capabilities != null ? capabilities.title : null);
        String capabilitiesTitle = capabilities != null ? sanitizeNullable(capabilitiesTitleLink.text) : null;
        String capabilitiesTitleHref = capabilitiesTitleLink.href;
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

                        String itemName = comp.name != null ? comp.name : "";
                        String itemCapability = comp.capability != null ? comp.capability : "";
                        LinkText nameLink2 = parseLinkField(itemName);
                        LinkText capabilityLink2 = parseLinkField(itemCapability);
                        String cleanName2 = nameLink2.text != null ? nameLink2.text : "";
                        String cleanCapability2 = capabilityLink2.text != null ? capabilityLink2.text : "";
                        domainItems.add(new RenderItem(
                            currentX, y,
                            cleanName2,
                            cleanCapability2,
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
                        lastDomainItem.nameLines = escapeLines(wrapText(cleanName2, nameCharsPerLine, maxNameLines));
                        lastDomainItem.capabilityLines = escapeLines(wrapText(cleanCapability2, capabilityCharsPerLine, maxCapabilityLines));
                        lastDomainItem.nameHref = nameLink2.href;
                        lastDomainItem.capabilityHref = capabilityLink2.href;
                        lastDomainItem.initiativeStroke = computeInitiativeStroke(comp, gradientId, initiativeGradients);
                        configureTextLayout(lastDomainItem, textCenterX, textLeftX, iconPosX);
                    }

                    LinkText domainLink = parseLinkField(section.domainName);
                    String domainLabel = sanitizeNullable(domainLink.text);
                    DomainGroup group = new DomainGroup(domainLabel != null ? domainLabel : "", section.iconId, domainItems);
                    group.domainHref = domainLink.href;
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

            float legendStartY = rowBottomY + DashboardLayoutConfig.LEGEND_TOP_MARGIN;
            legendY = (int) legendStartY;
        } else {
            legendY = (int) (domainStartY + DashboardLayoutConfig.LEGEND_TOP_MARGIN);
        }

        // Compute legend position defaults to below first row when no domains exist
        if (legendY == 0) {
            legendY = (int)(domainStartY + DashboardLayoutConfig.LEGEND_TOP_MARGIN);
        }

        // Prepare legend data: Status on left, Maturity on right with precomputed x offsets
        java.util.List<java.util.Map<String, Object>> statusLegend = new java.util.ArrayList<>();
        statusLegend.add(legendEntry(ComponentItem.Status.NOT_EXISTING.displayName, ComponentItem.Status.NOT_EXISTING.hex, 0));
        statusLegend.add(legendEntry(ComponentItem.Status.LOW.displayName, ComponentItem.Status.LOW.hex, 140));
        statusLegend.add(legendEntry(ComponentItem.Status.MEDIUM.displayName, ComponentItem.Status.MEDIUM.hex, 280));
        statusLegend.add(legendEntry(ComponentItem.Status.HIGH.displayName, ComponentItem.Status.HIGH.hex, 420));
        statusLegend.add(legendEntry(ComponentItem.Status.EFFECTIVE.displayName, ComponentItem.Status.EFFECTIVE.hex, 560));

        java.util.List<java.util.Map<String, Object>> maturityLegend = new java.util.ArrayList<>();
        maturityLegend.add(legendEntry(ComponentItem.Maturity.NOT_EXISTING.displayName, ComponentItem.Maturity.NOT_EXISTING.hex, 720));
        maturityLegend.add(legendEntry(ComponentItem.Maturity.INITIAL.displayName, ComponentItem.Maturity.INITIAL.hex, 860));
        maturityLegend.add(legendEntry(ComponentItem.Maturity.REPEATABLE.displayName, ComponentItem.Maturity.REPEATABLE.hex, 1000));
        maturityLegend.add(legendEntry(ComponentItem.Maturity.DEFINED.displayName, ComponentItem.Maturity.DEFINED.hex, 1140));
        maturityLegend.add(legendEntry(ComponentItem.Maturity.MANAGED.displayName, ComponentItem.Maturity.MANAGED.hex, 1280));
        maturityLegend.add(legendEntry(ComponentItem.Maturity.OPTIMISED.displayName, ComponentItem.Maturity.OPTIMISED.hex, 1420));

        int svgHeight = legendY + DashboardLayoutConfig.LEGEND_HEIGHT + DashboardLayoutConfig.BOTTOM_MARGIN;
        float mmPerPixel = DashboardLayoutConfig.A4_WIDTH_MM / canvasWidth;
        float svgHeightMm = svgHeight * mmPerPixel;

        TemplateInstance data = dashboard
            .data("title", title)
            .data("titleHref", titleLink.href)
            .data("governanceTitle", gov != null ? govTitle : null)
            .data("governanceTitleHref", govTitleHref)
            .data("governanceItems", governanceItems)
            .data("capabilitiesTitle", capabilities != null ? capabilitiesTitle : null)
            .data("capabilitiesTitleHref", capabilitiesTitleHref)
            .data("capabilitiesHeaderY", (int) capabilitiesHeaderY)
            .data("capabilitiesTextY", (int) capabilitiesHeaderTextY)
            .data("capabilitiesIconY", (int) capabilitiesIconY)
            .data("capabilitiesIcon", (capabilities != null && capabilities.icon != null && !capabilities.icon.isBlank()) ? ("icon-" + capabilities.icon.trim()) : null)
            .data("esaIcon", (root != null && root.icon != null && !root.icon.isBlank()) ? ("icon-" + root.icon.trim()) : null)
            .data("domainGroups", domainGroups)
            .data("boxW", (int) boxW)
            .data("boxH", (int) boxH)
            .data("halfBoxW", boxW / 2f)
            .data("pageCenter", (int)(canvasWidth / 2f))
            .data("legendY", legendY)
            .data("statusLegend", statusLegend)
            .data("maturityLegend", maturityLegend)
            .data("initiativeGradients", initiativeGradients)
            .data("canvasWidth", (int) canvasWidth)
            .data("svgHeight", svgHeight)
            .data("svgHeightMm", svgHeightMm)
            .data("a4WidthMm", DashboardLayoutConfig.A4_WIDTH_MM);
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

    /**
     * Converts SVG content to PNG format using Apache Batik.
     * 
     * @param svgContent the SVG content as a string
     * @param dpi the DPI (dots per inch) for the PNG output
     * @return the PNG image as a byte array
     * @throws Exception if the conversion fails
     */
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

    private String computeInitiativeStroke(ComponentItem comp, String baseId, java.util.List<InitiativeGradient> gradients) {
        if (comp == null || comp.initiatives <= 0) {
            return "#FFFFFF";
        }
        String rag = comp.iRag;
        if (rag == null) {
            return "#FFFFFF";
        }
        String normalized = rag.replaceAll("[^RAGrag]", "").toUpperCase();
        if (normalized.isEmpty()) {
            return "#FFFFFF";
        }
        String gradientId = baseId + "_initiative";
        InitiativeGradient gradient = new InitiativeGradient(gradientId);
        int len = normalized.length();
        for (int i = 0; i < len; i++) {
            float start = (float) i / len;
            float end = (float) (i + 1) / len;
            String color = colorForInitiativeRag(normalized.charAt(i));
            gradient.stops.add(new GradientStop(start * 100f, color));
            gradient.stops.add(new GradientStop(end * 100f, color));
        }
        gradients.add(gradient);
        return "url(#" + gradientId + ")";
    }

    private String colorForInitiativeRag(char c) {
        return ColorPalette.getInitiativeRagColor(c);
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

    private java.util.List<String> escapeLines(java.util.List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return java.util.Collections.singletonList("");
        }
        java.util.List<String> escaped = new java.util.ArrayList<>(lines.size());
        for (String line : lines) {
            escaped.add(escapeXml(line));
        }
        return escaped;
    }

    private String sanitizeNullable(String value) {
        return value == null ? null : StringUtils.escapeXml(value);
    }

    private String escapeXml(String value) {
        return StringUtils.escapeXml(value);
    }

    private java.util.Map<String, Object> legendEntry(String label, String color, int x) {
        java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
        String safeLabel = sanitizeNullable(label);
        entry.put("label", safeLabel != null ? safeLabel : "");
        entry.put("color", color);
        entry.put("x", x);
        return entry;
    }

    private LinkText parseLinkField(String value) {
        if (value == null) {
            return new LinkText(null, null);
        }
        String trimmed = value.trim();
        if (trimmed.endsWith("]")) {
            int open = trimmed.lastIndexOf('[');
            if (open >= 0 && open < trimmed.length() - 1) {
                String encodedUrl = trimmed.substring(open + 1, trimmed.length() - 1).trim();
                if (!encodedUrl.isBlank()) {
                    String label = trimmed.substring(0, open).trim();
                    String decoded = decodeUrl(encodedUrl);
                    return new LinkText(label.isEmpty() ? null : label, decoded);
                }
            }
        }
        return new LinkText(value, null);
    }

    private String decodeUrl(String value) {
        return UrlUtils.decode(value);
    }

    private static class LinkText {
        final String text;
        final String href;

        LinkText(String text, String href) {
            this.text = text;
            this.href = href;
        }
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

    private static class InitiativeGradient {
        public final String id;
        public final java.util.List<GradientStop> stops = new java.util.ArrayList<>();

        InitiativeGradient(String id) {
            this.id = id;
        }
    }

    private static class GradientStop {
        public final float offset;
        public final String color;

        GradientStop(float offset, String color) {
            this.offset = offset;
            this.color = color;
        }
    }
}
