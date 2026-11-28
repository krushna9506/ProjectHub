package com.projecthub.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.projecthub.models.ChatMessage;
import com.projecthub.models.Conversation;
import com.projecthub.models.User;
import com.projecthub.utils.ConversationUtils;
import com.projecthub.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatRepository {

    public interface ConversationCallback {
        void onSuccess(Conversation conversation);
        void onError(String message);
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(String message);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<ListenerRegistration> activeListeners = new ArrayList<>();

    public LiveData<List<Conversation>> getUserConversations(String userId) {
        MutableLiveData<List<Conversation>> liveData = new MutableLiveData<>(new ArrayList<>());
        if (userId == null || userId.isEmpty()) {
            return liveData;
        }
        ListenerRegistration reg = db.collection("conversations")
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener((snapshots, e) -> {
                if (snapshots != null) {
                    List<Conversation> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Conversation c = doc.toObject(Conversation.class);
                        if (c != null) {
                            c.setConversationId(doc.getId());
                            list.add(c);
                        }
                    }
                    list.sort((a, b) -> Long.compare(
                        Math.max(b.getLastMessageTimestamp(), b.getCreatedAt()),
                        Math.max(a.getLastMessageTimestamp(), a.getCreatedAt())
                    ));
                    liveData.setValue(list);
                }
            });
        activeListeners.add(reg);
        return liveData;
    }

    public LiveData<Conversation> getConversation(String conversationId) {
        MutableLiveData<Conversation> liveData = new MutableLiveData<>();
        if (conversationId == null || conversationId.isEmpty()) {
            return liveData;
        }
        ListenerRegistration reg = db.collection("conversations")
            .document(conversationId)
            .addSnapshotListener((snapshot, e) -> {
                if (snapshot != null && snapshot.exists()) {
                    Conversation conv = snapshot.toObject(Conversation.class);
                    if (conv != null) {
                        conv.setConversationId(snapshot.getId());
                        liveData.setValue(conv);
                    }
                }
            });
        activeListeners.add(reg);
        return liveData;
    }

    public LiveData<List<ChatMessage>> getMessages(String conversationId) {
        MutableLiveData<List<ChatMessage>> liveData = new MutableLiveData<>(new ArrayList<>());
        if (conversationId == null || conversationId.isEmpty()) {
            return liveData;
        }
        ListenerRegistration reg = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener((snapshots, e) -> {
                if (snapshots != null) {
                    List<ChatMessage> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        ChatMessage m = doc.toObject(ChatMessage.class);
                        if (m != null) {
                            m.setMessageId(doc.getId());
                            list.add(m);
                        }
                    }
                    liveData.setValue(list);
                }
            });
        activeListeners.add(reg);
        return liveData;
    }

    public void getOrCreateDirectChat(User currentUser, User otherUser, ConversationCallback callback) {
        if (currentUser == null || otherUser == null
                || currentUser.getUserId() == null || otherUser.getUserId() == null) {
            if (callback != null) callback.onError("Invalid user selected.");
            return;
        }
        String myUid = currentUser.getUserId();
        String otherUid = otherUser.getUserId();
        String dmId = ConversationUtils.buildDirectConversationId(myUid, otherUid);
        if (dmId.isEmpty()) {
            if (callback != null) callback.onError("Cannot start direct chat with yourself.");
            return;
        }

        DocumentReference docRef = db.collection("conversations").document(dmId);
        docRef.get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    Conversation existing = snapshot.toObject(Conversation.class);
                    if (existing != null) {
                        existing.setConversationId(snapshot.getId());
                        if (callback != null) callback.onSuccess(existing);
                        return;
                    }
                }

                long now = System.currentTimeMillis();
                Conversation dm = new Conversation();
                dm.setConversationId(dmId);
                dm.setType("DIRECT");
                dm.setTitle("");
                dm.setDescription("");
                dm.setCreatedBy(myUid);
                dm.setParticipantIds(ConversationUtils.sortedDirectParticipants(myUid, otherUid));

                Map<String, String> names = new HashMap<>();
                names.put(myUid, resolveUserName(currentUser));
                names.put(otherUid, resolveUserName(otherUser));
                dm.setParticipantNames(names);

                Map<String, String> emails = new HashMap<>();
                emails.put(myUid, currentUser.getEmail() != null ? currentUser.getEmail() : "");
                emails.put(otherUid, otherUser.getEmail() != null ? otherUser.getEmail() : "");
                dm.setParticipantEmails(emails);

                dm.setLastMessage("");
                dm.setLastMessageSenderId("");
                dm.setLastMessageSenderName("");
                dm.setCreatedAt(now);
                dm.setLastMessageTimestamp(now);

                Map<String, Long> readBy = new HashMap<>();
                readBy.put(myUid, now);
                dm.setReadBy(readBy);

                docRef.set(dm)
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null) callback.onSuccess(dm);
                    })
                    .addOnFailureListener(err -> {
                        if (callback != null) callback.onError(err.getMessage());
                    });
            })
            .addOnFailureListener(err -> {
                if (callback != null) callback.onError(err.getMessage());
            });
    }

    public void createGroupChat(User creator, String title, String description,
                                List<User> selectedMembers, ConversationCallback callback) {
        if (creator == null || creator.getUserId() == null || creator.getUserId().isEmpty()) {
            if (callback != null) callback.onError("User session missing.");
            return;
        }
        String cleanTitle = title != null ? title.trim() : "";
        if (cleanTitle.isEmpty()) {
            if (callback != null) callback.onError("Please enter a group name.");
            return;
        }

        DocumentReference docRef = db.collection("conversations").document();
        String convId = docRef.getId();
        long now = System.currentTimeMillis();
        String creatorUid = creator.getUserId();
        String creatorName = resolveUserName(creator);

        Set<String> idSet = new LinkedHashSet<>();
        Map<String, String> names = new HashMap<>();
        Map<String, String> emails = new HashMap<>();

        idSet.add(creatorUid);
        names.put(creatorUid, creatorName);
        emails.put(creatorUid, creator.getEmail() != null ? creator.getEmail() : "");

        if (selectedMembers != null) {
            for (User member : selectedMembers) {
                if (member != null && member.getUserId() != null && !member.getUserId().isEmpty()) {
                    String uid = member.getUserId();
                    idSet.add(uid);
                    names.put(uid, resolveUserName(member));
                    emails.put(uid, member.getEmail() != null ? member.getEmail() : "");
                }
            }
        }

        String systemText = creatorName + " created group \"" + cleanTitle + "\"";
        Conversation group = new Conversation();
        group.setConversationId(convId);
        group.setType("GROUP");
        group.setTitle(cleanTitle);
        group.setDescription(description != null ? description.trim() : "");
        group.setCreatedBy(creatorUid);
        group.setParticipantIds(new ArrayList<>(idSet));
        group.setParticipantNames(names);
        group.setParticipantEmails(emails);
        group.setLastMessage(truncatePreview(systemText));
        group.setLastMessageSenderId(creatorUid);
        group.setLastMessageSenderName(creatorName);
        group.setCreatedAt(now);
        group.setLastMessageTimestamp(now);

        Map<String, Long> readBy = new HashMap<>();
        readBy.put(creatorUid, now);
        group.setReadBy(readBy);

        docRef.set(group)
            .addOnSuccessListener(aVoid -> {
                DocumentReference msgRef = docRef.collection("messages").document();
                ChatMessage systemMsg = new ChatMessage(
                    msgRef.getId(),
                    convId,
                    creatorUid,
                    creatorName,
                    creator.getEmail() != null ? creator.getEmail() : "",
                    systemText,
                    "SYSTEM",
                    now
                );
                msgRef.set(systemMsg);

                for (String uid : group.getParticipantIds()) {
                    if (!uid.equals(creatorUid)) {
                        NotificationHelper.dispatchNotification(
                            null,
                            uid,
                            "Added to " + cleanTitle,
                            creatorName + " created a group chat with you",
                            "GROUP_CHAT",
                            convId,
                            cleanTitle
                        );
                    }
                }
                if (callback != null) callback.onSuccess(group);
            })
            .addOnFailureListener(err -> {
                if (callback != null) callback.onError(err.getMessage());
            });
    }

    public void sendMessage(Conversation conversation, ChatMessage message, OperationCallback callback) {
        if (conversation == null || conversation.getConversationId() == null || message == null) {
            if (callback != null) callback.onError("Invalid conversation.");
            return;
        }
        String convId = conversation.getConversationId();
        DocumentReference convRef = db.collection("conversations").document(convId);
        DocumentReference msgRef = convRef.collection("messages").document();

        long now = message.getTimestamp() > 0 ? message.getTimestamp() : System.currentTimeMillis();
        message.setMessageId(msgRef.getId());
        message.setProjectId(convId);
        message.setTimestamp(now);

        String senderId = message.getSenderId() != null ? message.getSenderId() : "";
        String senderName = message.getSenderName() != null && !message.getSenderName().isEmpty()
                ? message.getSenderName() : "Teammate";

        msgRef.set(message)
            .addOnSuccessListener(aVoid -> {
                Map<String, Object> convUpdates = new HashMap<>();
                convUpdates.put("lastMessage", truncatePreview(message.getMessage()));
                convUpdates.put("lastMessageSenderId", senderId);
                convUpdates.put("lastMessageSenderName", senderName);
                convUpdates.put("lastMessageTimestamp", now);
                if (!senderId.isEmpty()) {
                    convUpdates.put("readBy." + senderId, now);
                }
                convRef.update(convUpdates);

                Set<String> alreadyNotified = new LinkedHashSet<>();
                List<String> mentions = message.getMentionedUserIds();
                String convTitle = conversation.isGroup()
                        ? conversation.getTitle()
                        : senderName;

                if (mentions != null) {
                    for (String uid : mentions) {
                        if (uid != null && !uid.isEmpty() && !uid.equals(senderId)) {
                            alreadyNotified.add(uid);
                            NotificationHelper.dispatchNotification(
                                null,
                                uid,
                                senderName + " mentioned you in " + convTitle,
                                message.getMessage(),
                                "MENTION",
                                convId,
                                convTitle
                            );
                        }
                    }
                }

                String notifType = conversation.isGroup() ? "GROUP_CHAT" : "DM";
                String notifTitle = conversation.isGroup()
                        ? senderName + " in " + convTitle
                        : "Message from " + senderName;

                for (String uid : conversation.getParticipantIds()) {
                    if (uid != null && !uid.isEmpty() && !uid.equals(senderId) && !alreadyNotified.contains(uid)) {
                        NotificationHelper.dispatchNotification(
                            null,
                            uid,
                            notifTitle,
                            message.getMessage(),
                            notifType,
                            convId,
                            convTitle
                        );
                    }
                }

                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(err -> {
                if (callback != null) callback.onError(err.getMessage());
            });
    }

    public void toggleReaction(String conversationId, String messageId, String emoji, String userId) {
        if (conversationId == null || messageId == null || emoji == null || userId == null || userId.isEmpty()) {
            return;
        }
        DocumentReference msgRef = db.collection("conversations").document(conversationId)
            .collection("messages").document(messageId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(msgRef);
            ChatMessage msg = snapshot.toObject(ChatMessage.class);
            Map<String, List<String>> reactions = (msg != null && msg.getReactions() != null)
                    ? new HashMap<>(msg.getReactions())
                    : new HashMap<>();

            List<String> users = reactions.get(emoji) != null
                    ? new ArrayList<>(reactions.get(emoji))
                    : new ArrayList<>();

            if (users.contains(userId)) {
                users.remove(userId);
            } else {
                users.add(userId);
            }

            if (users.isEmpty()) {
                reactions.remove(emoji);
            } else {
                reactions.put(emoji, users);
            }
            transaction.update(msgRef, "reactions", reactions);
            return null;
        });
    }

    public void markConversationRead(String conversationId, String userId) {
        if (conversationId == null || conversationId.isEmpty() || userId == null || userId.isEmpty()) {
            return;
        }
        db.collection("conversations").document(conversationId)
            .update("readBy." + userId, System.currentTimeMillis());
    }

    public void addGroupMembers(Conversation conversation, List<User> newMembers, String actorName,
                                OperationCallback callback) {
        if (conversation == null || !conversation.isGroup()
                || conversation.getConversationId() == null || newMembers == null || newMembers.isEmpty()) {
            if (callback != null) callback.onError("Select at least one member.");
            return;
        }
        Set<String> updatedIds = new LinkedHashSet<>(conversation.getParticipantIds());
        Map<String, String> updatedNames = new HashMap<>(conversation.getParticipantNames());
        Map<String, String> updatedEmails = new HashMap<>(conversation.getParticipantEmails());

        List<User> trulyNew = new ArrayList<>();
        for (User u : newMembers) {
            if (u == null || u.getUserId() == null || u.getUserId().isEmpty()) continue;
            if (!updatedIds.contains(u.getUserId())) {
                trulyNew.add(u);
            }
            updatedIds.add(u.getUserId());
            updatedNames.put(u.getUserId(), resolveUserName(u));
            updatedEmails.put(u.getUserId(), u.getEmail() != null ? u.getEmail() : "");
        }

        if (trulyNew.isEmpty()) {
            if (callback != null) callback.onSuccess();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("participantIds", new ArrayList<>(updatedIds));
        updates.put("participantNames", updatedNames);
        updates.put("participantEmails", updatedEmails);

        db.collection("conversations").document(conversation.getConversationId())
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                String who = (actorName != null && !actorName.isEmpty()) ? actorName : "A member";
                for (User added : trulyNew) {
                    NotificationHelper.dispatchNotification(
                        null,
                        added.getUserId(),
                        "Added to " + conversation.getTitle(),
                        who + " added you to the group chat",
                        "GROUP_CHAT",
                        conversation.getConversationId(),
                        conversation.getTitle()
                    );
                }
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(err -> {
                if (callback != null) callback.onError(err.getMessage());
            });
    }

    public void renameGroup(String conversationId, String newTitle, String newDescription, OperationCallback callback) {
        if (conversationId == null || conversationId.isEmpty()) return;
        String cleanTitle = newTitle != null ? newTitle.trim() : "";
        if (cleanTitle.isEmpty()) {
            if (callback != null) callback.onError("Group name cannot be empty.");
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", cleanTitle);
        updates.put("description", newDescription != null ? newDescription.trim() : "");
        db.collection("conversations").document(conversationId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(err -> {
                if (callback != null) callback.onError(err.getMessage());
            });
    }

    public void leaveGroup(Conversation conversation, String userId, OperationCallback callback) {
        if (conversation == null || !conversation.isGroup()
                || conversation.getConversationId() == null || userId == null || userId.isEmpty()) {
            if (callback != null) callback.onError("Cannot leave this conversation.");
            return;
        }
        if (conversation.getParticipantIds().size() <= 1) {
            if (callback != null) callback.onError("You are the last member in this group.");
            return;
        }
        db.collection("conversations").document(conversation.getConversationId())
            .update("participantIds", FieldValue.arrayRemove(userId))
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(err -> {
                if (callback != null) callback.onError(err.getMessage());
            });
    }

    public void clearListeners() {
        for (ListenerRegistration reg : activeListeners) {
            reg.remove();
        }
        activeListeners.clear();
    }

    private String resolveUserName(User user) {
        if (user.getName() != null && !user.getName().trim().isEmpty()) {
            return user.getName().trim();
        }
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            return user.getEmail().split("@")[0];
        }
        return "Member";
    }

    private String truncatePreview(String text) {
        if (text == null) return "";
        String clean = text.trim();
        return clean.length() > 480 ? clean.substring(0, 480) : clean;
    }
}
