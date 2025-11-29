package com.example.dashboard.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ComponentItem {
    public String capability; // e.g., "Static Code Scanning"
    public String name; // e.g., "SAST"

    
    // Enumerations for status and maturity with associated HEX colors
    public enum Status {
        NOT_EXISTING("#908782"),
        LOW("#d6761f"),
        MEDIUM("#dfd005"),
        HIGH("#80aa2e"),
        EFFECTIVE("#0e8a39");

        public final String hex;
        Status(String hex) { this.hex = hex; }
    }

    public enum Maturity {
        NOT_EXISTING("#908782"),
        INITIAL("#d6761f"),
        REPEATABLE("#d59704"),
        DEFINED("#dfd005"),
        MANAGED("#80aa2e"),
        OPTIMISED("#0e8a39"); // spelling variant accepted in parser

        public final String hex;
        Maturity(String hex) { this.hex = hex; }
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

    // default ctor for Jackson
    public ComponentItem() {}

    @Override
    public String toString() {
        return "ComponentItem [capability=" + capability + ", name=" + name + ", status="
                + status + ", maturity=" + maturity + ", rag=" + rag + ", summary=" + summary + ", initiatives="
                + initiatives + ", doubleBorder=" + doubleBorder + "]";
    }

    

}
