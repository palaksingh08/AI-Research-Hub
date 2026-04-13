package com.project.model;

import java.util.ArrayList;
import java.util.List;

public class AssistantRequest {

    private String message;
    private String currentQuery;
    private List<Paper> currentResults = new ArrayList<>();

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCurrentQuery() {
        return currentQuery;
    }

    public void setCurrentQuery(String currentQuery) {
        this.currentQuery = currentQuery;
    }

    public List<Paper> getCurrentResults() {
        return currentResults;
    }

    public void setCurrentResults(List<Paper> currentResults) {
        this.currentResults = currentResults != null ? currentResults : new ArrayList<>();
    }
}
