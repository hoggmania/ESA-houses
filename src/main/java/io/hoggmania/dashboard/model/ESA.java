package io.hoggmania.dashboard.model;

import io.hoggmania.dashboard.exception.ValidationException;
import java.util.Map;

/**
 * Root object for ESA dashboard payloads.
 * Wraps the previous top-level fields under a single root for consistency.
 */
public class ESA {
    public String title;
    public String icon; // optional top-level icon for the dashboard
    public Map<String, String> attributes; // optional extra metadata for downstream consumers
    public Governance governance;
    public Capabilities capabilities;

    @Override
    public String toString() {
        return "ESA [title=" + title + ", icon=" + icon + ", attributes=" + attributes
                + ", governance=" + governance + ", capabilities=" + capabilities + "]";
    }


        /**
     * Validates the ESA model structure and required fields
     */
    public static void validateESA(ESA root) {
        if (root == null) {
            throw new ValidationException("ESA root object cannot be null");
        }
        
        // Validate governance section
        if (root.governance != null) {
            if (root.governance.components != null) {
                for (int i = 0; i < root.governance.components.size(); i++) {
                    ComponentItem comp = root.governance.components.get(i);
                    validateComponent(comp, "governance", i);
                }
            }
        }
        
        // Validate capabilities section
        if (root.capabilities != null && root.capabilities.domains != null) {
            for (int domainIdx = 0; domainIdx < root.capabilities.domains.size(); domainIdx++) {
                Domain domain = root.capabilities.domains.get(domainIdx);
                if (domain == null) {
                    throw new ValidationException("Domain at index " + domainIdx + " cannot be null");
                }
                
                if (domain.components != null) {
                    for (int compIdx = 0; compIdx < domain.components.size(); compIdx++) {
                        ComponentItem comp = domain.components.get(compIdx);
                        String location = "capabilities.domains[" + domainIdx + "]";
                        validateComponent(comp, location, compIdx);
                    }
                }
            }
        }
    }
    
    /**
     * Validates individual component items
     */
    private static void validateComponent(ComponentItem comp, String section, int index) {
        if (comp == null) {
            throw new ValidationException("Component at " + section + "[" + index + "] cannot be null");
        }
        
        String location = section + "[" + index + "]";
        
        // Validate required enums
        if (comp.status == null) {
            throw new ValidationException("Component at " + location + " missing required field: statusEnum. "
                + "Must be one of: NOT_EXISTING, LOW, MEDIUM, HIGH, EFFECTIVE");
        }
        
        if (comp.maturity == null) {
            throw new ValidationException("Component at " + location + " missing required field: maturityEnum. "
                + "Must be one of: NOT_EXISTING, INITIAL, REPEATABLE, DEFINED, MANAGED, OPTIMISED");
        }
        
        // Validate name is present
        if (comp.name == null || comp.name.trim().isEmpty()) {
            throw new ValidationException("Component at " + location + " missing required field: name");
        }
        
        // Validate initiatives is non-negative
        if (comp.initiatives < 0) {
            throw new ValidationException("Component at " + location + " has invalid initiatives count: " 
                + comp.initiatives + " (must be >= 0)");
        }
    }
}
