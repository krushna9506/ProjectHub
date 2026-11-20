package com.projecthub.models;

public class Milestone {
    private String milestoneId;
    private String projectId;
    private String title;
    private String status; // "PENDING", "IN_PROGRESS", "COMPLETED"
    private long dueDate;

    public Milestone() {}

    public Milestone(String milestoneId, String projectId, String title, String status, long dueDate) {
        this.milestoneId = milestoneId;
        this.projectId = projectId;
        this.title = title;
        this.status = status;
        this.dueDate = dueDate;
    }

    public String getMilestoneId() { return milestoneId; }
    public void setMilestoneId(String milestoneId) { this.milestoneId = milestoneId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }
}
