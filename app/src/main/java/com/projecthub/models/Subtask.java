package com.projecthub.models;

import java.util.UUID;

public class Subtask {
    private String subtaskId;
    private String title;
    private boolean completed;

    public Subtask() {
        this.subtaskId = UUID.randomUUID().toString();
        this.completed = false;
    }

    public Subtask(String subtaskId, String title, boolean completed) {
        this.subtaskId = (subtaskId != null && !subtaskId.isEmpty()) ? subtaskId : UUID.randomUUID().toString();
        this.title = title;
        this.completed = completed;
    }

    public String getSubtaskId() { return subtaskId; }
    public void setSubtaskId(String subtaskId) { this.subtaskId = subtaskId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
