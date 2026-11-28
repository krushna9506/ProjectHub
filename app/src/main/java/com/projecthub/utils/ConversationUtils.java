package com.projecthub.utils;

import com.projecthub.models.Conversation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConversationUtils {

    private ConversationUtils() {}

    /**
     * Builds a deterministic 1:1 conversation ID from two distinct user IDs sorted lexicographically
     * so both Android and Web resolve the exact same document ID without duplicates.
     */
    public static String buildDirectConversationId(String uid1, String uid2) {
        String a = uid1 != null ? uid1.trim() : "";
        String b = uid2 != null ? uid2.trim() : "";
        if (a.isEmpty() || b.isEmpty() || a.equals(b)) {
            return "";
        }
        return a.compareTo(b) < 0 ? "dm_" + a + "_" + b : "dm_" + b + "_" + a;
    }

    public static List<String> sortedDirectParticipants(String uid1, String uid2) {
        String a = uid1 != null ? uid1.trim() : "";
        String b = uid2 != null ? uid2.trim() : "";
        if (a.isEmpty() || b.isEmpty() || a.equals(b)) {
            return new ArrayList<>();
        }
        return a.compareTo(b) < 0 ? Arrays.asList(a, b) : Arrays.asList(b, a);
    }

    public static String resolveOtherParticipantId(Conversation conversation, String currentUserId) {
        if (conversation == null || conversation.isGroup()) return "";
        String me = currentUserId != null ? currentUserId : "";
        for (String uid : conversation.getParticipantIds()) {
            if (uid != null && !uid.equals(me)) {
                return uid;
            }
        }
        return "";
    }

    public static String resolveDisplayTitle(Conversation conversation, String currentUserId) {
        if (conversation == null) return "Conversation";
        if (conversation.isGroup()) {
            String title = conversation.getTitle().trim();
            return title.isEmpty() ? "Group Chat" : title;
        }
        String otherUid = resolveOtherParticipantId(conversation, currentUserId);
        Map<String, String> names = conversation.getParticipantNames();
        if (!otherUid.isEmpty() && names.containsKey(otherUid)) {
            String partnerName = names.get(otherUid);
            if (partnerName != null && !partnerName.trim().isEmpty()) {
                return partnerName.trim();
            }
        }
        Map<String, String> emails = conversation.getParticipantEmails();
        if (!otherUid.isEmpty() && emails.containsKey(otherUid)) {
            String partnerEmail = emails.get(otherUid);
            if (partnerEmail != null && !partnerEmail.trim().isEmpty()) {
                return partnerEmail.trim();
            }
        }
        return "Direct Message";
    }

    public static boolean isUnread(Conversation conversation, String currentUserId) {
        if (conversation == null || currentUserId == null || currentUserId.isEmpty()) {
            return false;
        }
        long lastTs = conversation.getLastMessageTimestamp();
        if (lastTs <= 0) {
            return false;
        }
        if (currentUserId.equals(conversation.getLastMessageSenderId())) {
            return false;
        }
        Long readAt = conversation.getReadBy().get(currentUserId);
        return readAt == null || readAt < lastTs;
    }

    public static String formatSubtitle(Conversation conversation) {
        if (conversation == null) return "";
        if (conversation.isGroup()) {
            int count = conversation.getParticipantIds().size();
            String base = count + (count == 1 ? " member" : " members");
            String desc = conversation.getDescription().trim();
            return desc.isEmpty() ? base : base + " • " + desc;
        }
        return "Direct Message";
    }

    public static String buildAvatarInitials(String title) {
        if (title == null || title.trim().isEmpty()) return "C";
        String[] parts = title.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.getDefault());
        }
        return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.getDefault());
    }
}
