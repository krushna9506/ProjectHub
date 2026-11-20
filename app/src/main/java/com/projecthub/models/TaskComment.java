package com.projecthub.models;

public class TaskComment {
    private String commentId;
    private String taskId;
    private String projectId;
    private String authorId;
    private String authorName;
    private String message;
    private long timestamp;

    public TaskComment() {}

    public TaskComment(String commentId, String taskId, String projectId, 
                       String authorId, String authorName, String message, long timestamp) {
        this.commentId = commentId;
        this.taskId = taskId;
        this.projectId = projectId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
