package com.projecthub.models;

public class AppNotification {
    private String notificationId;
    private String userId; // Recipient user id
    private String title;
    private String message;
    private String type; // "TASK", "JOIN_REQUEST", "JOIN_ACCEPTED", "JOIN_REJECTED", "CHAT", "CALL"
    private String projectId;
    private String projectTitle;
    private long timestamp;
    private boolean read;

    public AppNotification() {
        this.read = false;
        this.timestamp = System.currentTimeMillis();
    }

    public AppNotification(String notificationId, String userId, String title, 
                           String message, String type, String projectId, 
                           String projectTitle, long timestamp) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type != null ? type : "TASK";
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
        this.read = false;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type != null ? type : "TASK"; }
    public void setType(String type) { this.type = type; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
