package com.example.dashboard.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ComponentItem {
    public String capability; // e.g., "Static Code Scanning"
    public String name; // e.g., "SAST"

    
    // Enumerations for status and maturity with associated HEX colors
    public enum Status {
        NOT_EXISTING("#908782", "Not Existing"),
        LOW("#d6761f", "Low"),
        MEDIUM("#dfd005", "Medium"),
        HIGH("#80aa2e", "High"),
        EFFECTIVE("#0e8a39", "Effective");

        public final String hex;
        public final String displayName;
        Status(String hex, String displayName) { 
            this.hex = hex;
            this.displayName = displayName;
        }
    }

    public enum Maturity {
        NOT_EXISTING("#908782", "Not Existing"),
        INITIAL("#d6761f", "Initial"),
        REPEATABLE("#d59704", "Repeatable"),
        DEFINED("#dfd005", "Defined"),
        MANAGED("#80aa2e", "Managed"),
        OPTIMISED("#0e8a39", "Optimised"); // spelling variant accepted in parser

        public final String hex;
        public final String displayName;
        Maturity(String hex, String displayName) { 
            this.hex = hex;
            this.displayName = displayName;
        }
    }

    // Enum fields with JSON aliases to accept 'status' or 'statusEnum'
    @JsonProperty("status")
    @JsonAlias({"status"})
    public Status status;
    
    @JsonProperty("maturity")
    @JsonAlias({"maturity"})
    public Maturity maturity;

    public String rag; // e.g., "green"|"amber"|"red"
    public String summary;
    public int initiatives; // number of related initiatives; if >0 render badge
    public boolean doubleBorder; // if true, render with double border
    public String icon; // optional logical icon key, e.g., "shield","lock","scan","waf"

    // default ctor for Jackson
    public ComponentItem() {}

    @Override
    public String toString() {
        return "ComponentItem [capability=" + capability + ", name=" + name + ", status="
                + status + ", maturity=" + maturity + ", rag=" + rag + ", summary=" + summary + ", initiatives="
                + initiatives + ", doubleBorder=" + doubleBorder + "]";
    }

    

}
