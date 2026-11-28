package com.projecthub.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.projecthub.models.ChatMessage;
import com.projecthub.models.Conversation;
import com.projecthub.models.User;
import com.projecthub.repository.ChatRepository;

import java.util.List;

public class ChatViewModel extends ViewModel {

    private final ChatRepository repository = new ChatRepository();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private LiveData<List<Conversation>> conversations;

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<List<Conversation>> getConversations(String userId) {
        if (conversations == null) {
            conversations = repository.getUserConversations(userId);
        }
        return conversations;
    }

    public LiveData<Conversation> getConversation(String conversationId) {
        return repository.getConversation(conversationId);
    }

    public LiveData<List<ChatMessage>> getMessages(String conversationId) {
        return repository.getMessages(conversationId);
    }

    public void getOrCreateDirectChat(User currentUser, User otherUser,
                                      ChatRepository.ConversationCallback callback) {
        repository.getOrCreateDirectChat(currentUser, otherUser, new ChatRepository.ConversationCallback() {
            @Override
            public void onSuccess(Conversation conversation) {
                if (callback != null) callback.onSuccess(conversation);
            }

            @Override
            public void onError(String message) {
                statusMessage.setValue("Error: " + message);
                if (callback != null) callback.onError(message);
            }
        });
    }

    public void createGroupChat(User creator, String title, String description,
                                List<User> members, ChatRepository.ConversationCallback callback) {
        repository.createGroupChat(creator, title, description, members, new ChatRepository.ConversationCallback() {
            @Override
            public void onSuccess(Conversation conversation) {
                statusMessage.setValue("Group chat created!");
                if (callback != null) callback.onSuccess(conversation);
            }

            @Override
            public void onError(String message) {
                statusMessage.setValue("Error: " + message);
                if (callback != null) callback.onError(message);
            }
        });
    }

    public void sendMessage(Conversation conversation, ChatMessage message) {
        repository.sendMessage(conversation, message, new ChatRepository.OperationCallback() {
            @Override
            public void onSuccess() {}

            @Override
            public void onError(String messageText) {
                statusMessage.setValue("Failed to send: " + messageText);
            }
        });
    }

    public void toggleReaction(String conversationId, String messageId, String emoji, String userId) {
        repository.toggleReaction(conversationId, messageId, emoji, userId);
    }

    public void markConversationRead(String conversationId, String userId) {
        repository.markConversationRead(conversationId, userId);
    }

    public void addGroupMembers(Conversation conversation, List<User> newMembers, String actorName) {
        repository.addGroupMembers(conversation, newMembers, actorName, new ChatRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                statusMessage.setValue("Members added to group.");
            }

            @Override
            public void onError(String message) {
                statusMessage.setValue("Error: " + message);
            }
        });
    }

    public void renameGroup(String conversationId, String newTitle, String newDescription) {
        repository.renameGroup(conversationId, newTitle, newDescription, new ChatRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                statusMessage.setValue("Group updated.");
            }

            @Override
            public void onError(String message) {
                statusMessage.setValue("Error: " + message);
            }
        });
    }

    public void leaveGroup(Conversation conversation, String userId, ChatRepository.OperationCallback callback) {
        repository.leaveGroup(conversation, userId, new ChatRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onError(String message) {
                statusMessage.setValue(message);
                if (callback != null) callback.onError(message);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.clearListeners();
    }
}
