package com.projecthub.models;

public class Task {
    private String taskId;
    private String projectId;
    private String milestoneId;
    private String title;
    private String description;
    private String status;    // "TODO", "IN_PROGRESS", "DONE"
    private String priority;  // "LOW", "MEDIUM", "HIGH"
    private String assignedTo;
    private String assignedToName;
    private long dueDate;

    public Task() {}

    public Task(String taskId, String projectId, String milestoneId, String title, 
                String description, String status, String priority, 
                String assignedTo, String assignedToName, long dueDate) {
        this.taskId = taskId;
        this.projectId = projectId;
        this.milestoneId = milestoneId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.assignedTo = assignedTo;
        this.assignedToName = assignedToName;
        this.dueDate = dueDate;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getMilestoneId() { return milestoneId; }
    public void setMilestoneId(String milestoneId) { this.milestoneId = milestoneId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }
}
