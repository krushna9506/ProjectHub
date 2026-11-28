package com.projecthub.models;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

public class Task {
    private String taskId;
    private String projectId;
    private String milestoneId;
    private String title;
    private String description;
    private String status;    // "TODO", "IN_PROGRESS", "IN_REVIEW", "DONE" or custom status
    private String priority;  // "LOW", "MEDIUM", "HIGH", "URGENT"
    private String issueType; // "TASK", "BUG", "FEATURE", "DOCUMENTATION"
    private int commentsCount;
    private String assignedTo;
    private String assignedToName;
    private long dueDate;
    private String taskKey;
    private String phase;
    private double timeSpentHours;
    private long startDate;
    private List<Subtask> subtasks;
    private String dependsOnTaskId;
    private String dependsOnTaskKey;
    private List<String> tags;
    private String githubIssueUrl;
    private int githubIssueNumber;

    public Task() {
        this.status = "TODO";
        this.priority = "MEDIUM";
        this.issueType = "TASK";
        this.phase = "DEVELOPMENT";
        this.commentsCount = 0;
        this.timeSpentHours = 0.0;
        this.startDate = System.currentTimeMillis();
        this.subtasks = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.githubIssueNumber = 0;
    }

    public Task(String taskId, String projectId, String milestoneId, String title, 
                String description, String status, String priority, 
                String assignedTo, String assignedToName, long dueDate) {
        this(taskId, projectId, milestoneId, title, description, status, priority, "TASK", assignedTo, assignedToName, dueDate);
    }

    public Task(String taskId, String projectId, String milestoneId, String title, 
                String description, String status, String priority, String issueType,
                String assignedTo, String assignedToName, long dueDate) {
        this.taskId = taskId;
        this.projectId = projectId;
        this.milestoneId = milestoneId;
        this.title = title;
        this.description = description;
        this.status = status != null ? status : "TODO";
        this.priority = priority != null ? priority : "MEDIUM";
        this.issueType = issueType != null ? issueType : "TASK";
        this.phase = "DEVELOPMENT";
        this.commentsCount = 0;
        this.assignedTo = assignedTo;
        this.assignedToName = assignedToName;
        this.dueDate = dueDate;
        this.startDate = System.currentTimeMillis();
        this.subtasks = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.githubIssueNumber = 0;
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

    public String getStatus() { return status != null ? status : "TODO"; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority != null ? priority : "MEDIUM"; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getIssueType() { return issueType != null ? issueType : "TASK"; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }

    public String getTaskKey() { return taskKey; }
    public void setTaskKey(String taskKey) { this.taskKey = taskKey; }

    public String getPhase() { return phase != null && !phase.isEmpty() ? phase : "DEVELOPMENT"; }
    public void setPhase(String phase) { this.phase = phase; }

    public double getTimeSpentHours() { return timeSpentHours; }
    public void setTimeSpentHours(double timeSpentHours) { this.timeSpentHours = timeSpentHours; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public List<Subtask> getSubtasks() { return subtasks != null ? subtasks : new ArrayList<>(); }
    public void setSubtasks(List<Subtask> subtasks) { this.subtasks = subtasks != null ? subtasks : new ArrayList<>(); }

    public String getDependsOnTaskId() { return dependsOnTaskId; }
    public void setDependsOnTaskId(String dependsOnTaskId) { this.dependsOnTaskId = dependsOnTaskId; }

    public String getDependsOnTaskKey() { return dependsOnTaskKey; }
    public void setDependsOnTaskKey(String dependsOnTaskKey) { this.dependsOnTaskKey = dependsOnTaskKey; }

    public List<String> getTags() { return tags != null ? tags : new ArrayList<>(); }
    public void setTags(List<String> tags) { this.tags = tags != null ? tags : new ArrayList<>(); }

    public String getGithubIssueUrl() { return githubIssueUrl; }
    public void setGithubIssueUrl(String githubIssueUrl) { this.githubIssueUrl = githubIssueUrl; }

    public int getGithubIssueNumber() { return githubIssueNumber; }
    public void setGithubIssueNumber(int githubIssueNumber) { this.githubIssueNumber = githubIssueNumber; }

    @Exclude
    public int getCompletedSubtasksCount() {
        if (subtasks == null) return 0;
        int count = 0;
        for (Subtask s : subtasks) {
            if (s != null && s.isCompleted()) count++;
        }
        return count;
    }

    @Exclude
    public int getTotalSubtasksCount() {
        return subtasks != null ? subtasks.size() : 0;
    }

    @Exclude
    public boolean areAllSubtasksCompleted() {
        if (subtasks == null || subtasks.isEmpty()) return false;
        for (Subtask s : subtasks) {
            if (s == null || !s.isCompleted()) return false;
        }
        return true;
    }
}
