package com.example.dashboard.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public class Capabilities {
    public String title;
     @JsonAlias({"domains", "domains"})
    public java.util.List<Domain> domains;
     @Override
     public String toString() {
        return "Capabilities [title=" + title + ", domains=" + domains + "]";
     }



    
}
