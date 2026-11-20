package com.projecthub.models;

public class ChatMessage {
    private String messageId;
    private String projectId;
    private String senderId;
    private String senderName;
    private String senderEmail;
    private String message;
    private String messageType; // "TEXT", "CODE", "LINK", "CALL"
    private long timestamp;

    public ChatMessage() {
        this.messageType = "TEXT";
    }

    public ChatMessage(String messageId, String projectId, String senderId, 
                       String senderName, String senderEmail, String message, 
                       String messageType, long timestamp) {
        this.messageId = messageId;
        this.projectId = projectId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderEmail = senderEmail;
        this.message = message;
        this.messageType = messageType != null ? messageType : "TEXT";
        this.timestamp = timestamp;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
