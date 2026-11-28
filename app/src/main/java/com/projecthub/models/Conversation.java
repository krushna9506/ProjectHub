package com.projecthub.models;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Conversation {
    private String conversationId;
    private String type; // "DIRECT" or "GROUP"
    private String title;
    private String description;
    private String createdBy;
    private List<String> participantIds;
    private Map<String, String> participantNames;
    private Map<String, String> participantEmails;
    private String lastMessage;
    private String lastMessageSenderId;
    private String lastMessageSenderName;
    private long lastMessageTimestamp;
    private long createdAt;
    private Map<String, Long> readBy;

    public Conversation() {
        this.type = "DIRECT";
        this.title = "";
        this.description = "";
        this.participantIds = new ArrayList<>();
        this.participantNames = new HashMap<>();
        this.participantEmails = new HashMap<>();
        this.lastMessage = "";
        this.lastMessageSenderId = "";
        this.lastMessageSenderName = "";
        this.readBy = new HashMap<>();
    }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getType() { return type != null ? type : "DIRECT"; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public List<String> getParticipantIds() {
        return participantIds != null ? participantIds : new ArrayList<>();
    }
    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds != null ? participantIds : new ArrayList<>();
    }

    public Map<String, String> getParticipantNames() {
        return participantNames != null ? participantNames : new HashMap<>();
    }
    public void setParticipantNames(Map<String, String> participantNames) {
        this.participantNames = participantNames != null ? participantNames : new HashMap<>();
    }

    public Map<String, String> getParticipantEmails() {
        return participantEmails != null ? participantEmails : new HashMap<>();
    }
    public void setParticipantEmails(Map<String, String> participantEmails) {
        this.participantEmails = participantEmails != null ? participantEmails : new HashMap<>();
    }

    public String getLastMessage() { return lastMessage != null ? lastMessage : ""; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastMessageSenderId() { return lastMessageSenderId != null ? lastMessageSenderId : ""; }
    public void setLastMessageSenderId(String lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }

    public String getLastMessageSenderName() { return lastMessageSenderName != null ? lastMessageSenderName : ""; }
    public void setLastMessageSenderName(String lastMessageSenderName) { this.lastMessageSenderName = lastMessageSenderName; }

    public long getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(long lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, Long> getReadBy() {
        return readBy != null ? readBy : new HashMap<>();
    }
    public void setReadBy(Map<String, Long> readBy) {
        this.readBy = readBy != null ? readBy : new HashMap<>();
    }

    @Exclude
    public boolean isGroup() {
        return "GROUP".equalsIgnoreCase(type);
    }

    @Exclude
    public boolean isDirect() {
        return !"GROUP".equalsIgnoreCase(type);
    }
}
