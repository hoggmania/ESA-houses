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
        final float boxW = 260f;
        final float boxH = 45f;
        final float gapX = 16f;
        final float gapY = 12f;

        String title = root != null ? root.title : null;

        // Governance items (horizontal row)
        java.util.List<RenderItem> governanceItems = new java.util.ArrayList<>();
        Governance gov = root != null ? root.governance : null;
        if (gov != null && gov.components != null) {
            String govTitle = gov.title;
            float govStartX = 20f;
            float govY = 80f;
            for (int i = 0; i < gov.components.size(); i++) {
                ComponentItem comp = gov.components.get(i);
                float x = govStartX + i * (boxW + gapX);
                String gradientId = "grad_gov_" + i;
                String border = "#333";
                if (comp.rag != null && "red".equalsIgnoreCase(comp.rag)) border = "red";

                governanceItems.add(new RenderItem(
                    x, govY,
                    comp.name != null ? comp.name : "",
                    comp.capability != null ? comp.capability : "",
                    govTitle != null ? govTitle : "",
                    comp.status.hex,
                    comp.maturity.hex,
                    gradientId,
                    border,
                    comp.doubleBorder
                ));
                RenderItem lastGov = governanceItems.get(governanceItems.size()-1);
                lastGov.initiatives = comp.initiatives;
                lastGov.showInitiatives = lastGov.initiatives > 0;
            }
        }

        // Capabilities domains (columns)
        java.util.List<DomainGroup> domainGroups = new java.util.ArrayList<>();
        Capabilities capabilities = root != null ? root.capabilities : null;
        java.util.List<Domain> domains = capabilities != null ? capabilities.domains : null;
        if (domains != null) {
            // Start domains lower to leave a visual gap after capabilities title bar
            float domainStartY = 230f; // was 200f
            float domainStartX = 20f;
            
            for (int domainIdx = 0; domainIdx < domains.size(); domainIdx++) {
                Domain d = domains.get(domainIdx);
                String domainName = d != null && d.domain != null ? d.domain : ("Domain " + (domainIdx + 1));
                
                java.util.List<RenderItem> domainItems = new java.util.ArrayList<>();
                java.util.List<com.example.dashboard.model.ComponentItem> comps = d != null ? d.components : null;
                
                if (comps != null) {
                    float colX = domainStartX + domainIdx * (boxW + gapX);
                    
                    for (int compIdx = 0; compIdx < comps.size(); compIdx++) {
                        var comp = comps.get(compIdx);
                        float y = domainStartY + compIdx * (boxH + gapY);
                        
                        String gradientId = "grad_dom_" + domainIdx + "_" + compIdx;
                        String border = "#333";
                        if (comp.rag != null && "red".equalsIgnoreCase(comp.rag)) border = "red";

                        domainItems.add(new RenderItem(
                            colX, y,
                            comp.name != null ? comp.name : "",
                            comp.capability != null ? comp.capability : "",
                            domainName,
                            comp.status.hex,
                            comp.maturity.hex,
                            gradientId,
                            border,
                            comp.doubleBorder
                        ));


                        RenderItem lastDomainItem = domainItems.get(domainItems.size()-1);
                        lastDomainItem.initiatives = comp.initiatives;
                        lastDomainItem.showInitiatives = lastDomainItem.initiatives > 0;
                    }
                }
                
                domainGroups.add(new DomainGroup(domainName, domainItems));
            }
        }

        TemplateInstance data = dashboard
            .data("title", title)
            .data("governanceTitle", gov != null ? gov.title : null)
            .data("governanceItems", governanceItems)
            .data("capabilitiesTitle", capabilities != null ? capabilities.title : null)
            .data("domainGroups", domainGroups)
            .data("boxW", (int) boxW)
            .data("boxH", (int) boxH)
            .data("halfBoxW", boxW / 2f)
            .data("pageCenter", 700); // viewBox width 1400 -> center 700
            // coordinates for initiatives badge inside a box (local to group)
            data = data
            .data("initiativeCircleX", (int)(boxW - 22))
            .data("initiativeCircleY", 18)
            .data("initiativeTextY", 22);
        return data.render();
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
}
