package com.projecthub.models;

import java.util.HashMap;
import java.util.Map;

public class Project {
    private String projectId;
    private String title;
    private String description;
    private String joinCode;
    private String leadId;
    private String leadName;
    private Map<String, String> members; // userId -> Role ("LEAD", "MEMBER", "FACULTY")
    private int progressPercentage;
    private long createdAt;
    private long deadline;

    public Project() {
        this.members = new HashMap<>();
    }

    public Project(String projectId, String title, String description, String joinCode, 
                   String leadId, String leadName, long deadline) {
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.joinCode = joinCode;
        this.leadId = leadId;
        this.leadName = leadName;
        this.members = new HashMap<>();
        this.members.put(leadId, "LEAD");
        this.progressPercentage = 0;
        this.createdAt = System.currentTimeMillis();
        this.deadline = deadline;
    }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getJoinCode() { return joinCode; }
    public void setJoinCode(String joinCode) { this.joinCode = joinCode; }

    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }

    public String getLeadName() { return leadName; }
    public void setLeadName(String leadName) { this.leadName = leadName; }

    public Map<String, String> getMembers() { return members; }
    public void setMembers(Map<String, String> members) { this.members = members; }

    public int getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getDeadline() { return deadline; }
    public void setDeadline(long deadline) { this.deadline = deadline; }
}
