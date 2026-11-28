package com.projecthub.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatMessage {
    private String messageId;
    private String projectId;
    private String senderId;
    private String senderName;
    private String senderEmail;
    private String message;
    private String messageType; // "TEXT", "CODE", "LINK", "CALL", "AUTOMATION"
    private long timestamp;
    private String replyToMessageId;
    private String replyToSenderName;
    private String replyToPreview;
    private Map<String, List<String>> reactions; // emoji -> list of userIds
    private List<String> mentionedUserIds;

    public ChatMessage() {
        this.messageType = "TEXT";
        this.reactions = new HashMap<>();
        this.mentionedUserIds = new ArrayList<>();
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
        this.reactions = new HashMap<>();
        this.mentionedUserIds = new ArrayList<>();
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

    public String getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public String getReplyToSenderName() { return replyToSenderName; }
    public void setReplyToSenderName(String replyToSenderName) { this.replyToSenderName = replyToSenderName; }

    public String getReplyToPreview() { return replyToPreview; }
    public void setReplyToPreview(String replyToPreview) { this.replyToPreview = replyToPreview; }

    public Map<String, List<String>> getReactions() { return reactions != null ? reactions : new HashMap<>(); }
    public void setReactions(Map<String, List<String>> reactions) { this.reactions = reactions != null ? reactions : new HashMap<>(); }

    public List<String> getMentionedUserIds() { return mentionedUserIds != null ? mentionedUserIds : new ArrayList<>(); }
    public void setMentionedUserIds(List<String> mentionedUserIds) { this.mentionedUserIds = mentionedUserIds != null ? mentionedUserIds : new ArrayList<>(); }
}
