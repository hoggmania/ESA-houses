package io.hoggmania.dashboard.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public class Capabilities {
    public String title;
   public String icon; // optional icon representing the capabilities section
     @JsonAlias({"domains", "domains"})
    public java.util.List<Domain> domains;
     @Override
     public String toString() {
      return "Capabilities [title=" + title + ", icon=" + icon + ", domains=" + domains + "]";
     }



    
}
