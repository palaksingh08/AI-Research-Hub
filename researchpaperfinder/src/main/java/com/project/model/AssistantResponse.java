package com.project.model;

import java.util.ArrayList;
import java.util.List;

public class AssistantResponse {

    private String reply;
    private String suggestedQuery;
    private List<String> followUps = new ArrayList<>();

    public AssistantResponse() {
    }

    public AssistantResponse(String reply, String suggestedQuery, List<String> followUps) {
        this.reply = reply;
        this.suggestedQuery = suggestedQuery;
        this.followUps = followUps != null ? followUps : new ArrayList<>();
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getSuggestedQuery() {
        return suggestedQuery;
    }

    public void setSuggestedQuery(String suggestedQuery) {
        this.suggestedQuery = suggestedQuery;
    }

    public List<String> getFollowUps() {
        return followUps;
    }

    public void setFollowUps(List<String> followUps) {
        this.followUps = followUps != null ? followUps : new ArrayList<>();
    }
}
