package io.hoggmania.dashboard.model;

import java.util.List;

public class JiraEsaRequest {
    public String jiraBase;
    public String jiraUrl;
    public String jiraToken;
    public List<AttributePair> headers;
    public List<AttributePair> attributes;

    public JiraEsaRequest() {}
}
