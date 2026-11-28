package com.projecthub.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projecthub.R;
import com.projecthub.adapters.ChatMessageAdapter;
import com.projecthub.models.ChatMessage;
import com.projecthub.models.Conversation;
import com.projecthub.models.User;
import com.projecthub.repository.ChatRepository;
import com.projecthub.utils.ConversationUtils;
import com.projecthub.utils.MentionParser;
import com.projecthub.utils.NotificationHelper;
import com.projecthub.utils.SafeUrlUtils;
import com.projecthub.viewmodel.AuthViewModel;
import com.projecthub.viewmodel.ChatViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRoomActivity extends AppCompatActivity {

    private ChatViewModel chatViewModel;
    private AuthViewModel authViewModel;
    private ChatMessageAdapter chatAdapter;

    private TextView tvAvatar, tvTitle, tvSubtitle, tvEmpty, tvReplyPreview;
    private ImageButton btnBack, btnCall, btnInfo, btnCancelReply;
    private MaterialButton btnMention, btnSend;
    private RecyclerView rvMessages;
    private ProgressBar pbLoading;
    private View layoutReplyPreview;
    private TextInputEditText etInput;

    private String conversationId = "";
    private String initialTitle = "";
    private String currentUserId = "";
    private User currentUserProfile;
    private Conversation currentConversation;
    private List<User> allCampusUsers = new ArrayList<>();
    private ChatMessage replyingToMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        conversationId = getIntent().getStringExtra("CONVERSATION_ID");
        initialTitle = getIntent().getStringExtra("CONVERSATION_TITLE");
        if (conversationId == null || conversationId.trim().isEmpty()) {
            finish();
            return;
        }

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = fbUser != null ? fbUser.getUid() : "";

        tvAvatar = findViewById(R.id.tvChatRoomAvatar);
        tvTitle = findViewById(R.id.tvChatRoomTitle);
        tvSubtitle = findViewById(R.id.tvChatRoomSubtitle);
        tvEmpty = findViewById(R.id.tvEmptyChatRoom);
        pbLoading = findViewById(R.id.pbChatRoomLoading);
        btnBack = findViewById(R.id.btnBack);
        btnCall = findViewById(R.id.btnChatRoomCall);
        btnInfo = findViewById(R.id.btnChatRoomInfo);
        btnCancelReply = findViewById(R.id.btnCancelChatReply);
        btnMention = findViewById(R.id.btnChatMention);
        btnSend = findViewById(R.id.btnSendChatRoom);
        rvMessages = findViewById(R.id.rvChatRoomMessages);
        layoutReplyPreview = findViewById(R.id.layoutChatReplyPreview);
        tvReplyPreview = findViewById(R.id.tvChatReplyPreview);
        etInput = findViewById(R.id.etChatRoomInput);

        if (initialTitle != null && !initialTitle.isEmpty()) {
            tvTitle.setText(initialTitle);
            tvAvatar.setText(ConversationUtils.buildAvatarInitials(initialTitle));
        }

        btnBack.setOnClickListener(v -> finish());

        chatAdapter = new ChatMessageAdapter(currentUserId, new ChatMessageAdapter.OnMessageActionListener() {
            @Override
            public void onMessageOptions(ChatMessage message) {
                showMessageOptions(message);
            }

            @Override
            public void onReactionClick(ChatMessage message, String emoji) {
                if (!currentUserId.isEmpty() && message.getMessageId() != null) {
                    chatViewModel.toggleReaction(conversationId, message.getMessageId(), emoji, currentUserId);
                }
            }
        });
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(chatAdapter);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        authViewModel.fetchCurrentUserProfile();
        authViewModel.getCurrentUser().observe(this, u -> currentUserProfile = u);
        authViewModel.getCampusUsers().observe(this, users ->
            allCampusUsers = users != null ? users : new ArrayList<>());

        chatViewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        chatViewModel.getConversation(conversationId).observe(this, conv -> {
            if (conv != null) {
                currentConversation = conv;
                bindConversationHeader(conv);
                if (ConversationUtils.isUnread(conv, currentUserId)) {
                    chatViewModel.markConversationRead(conversationId, currentUserId);
                }
            }
        });

        chatViewModel.getMessages(conversationId).observe(this, messages -> {
            pbLoading.setVisibility(View.GONE);
            List<ChatMessage> safe = messages != null ? messages : new ArrayList<>();
            chatAdapter.setMessages(safe);
            tvEmpty.setVisibility(safe.isEmpty() ? View.VISIBLE : View.GONE);
            if (!safe.isEmpty()) {
                rvMessages.scrollToPosition(safe.size() - 1);
                ChatMessage latest = safe.get(safe.size() - 1);
                if (latest.getSenderId() != null && !latest.getSenderId().equals(currentUserId)
                        && !currentUserId.isEmpty()) {
                    chatViewModel.markConversationRead(conversationId, currentUserId);
                }
            }
        });

        btnSend.setOnClickListener(v -> sendCurrentMessage());
        btnMention.setOnClickListener(v -> showMentionPicker());
        btnCancelReply.setOnClickListener(v -> clearReplyTarget());
        btnCall.setOnClickListener(v -> showCallDialog());
        btnInfo.setOnClickListener(v -> showConversationInfoDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!conversationId.isEmpty() && !currentUserId.isEmpty()) {
            chatViewModel.markConversationRead(conversationId, currentUserId);
        }
    }

    private void bindConversationHeader(Conversation conv) {
        String title = ConversationUtils.resolveDisplayTitle(conv, currentUserId);
        tvTitle.setText(title);
        tvSubtitle.setText(ConversationUtils.formatSubtitle(conv));
        tvAvatar.setText(ConversationUtils.buildAvatarInitials(title));
        tvAvatar.getBackground().mutate().setTint(ContextCompat.getColor(
            this,
            conv.isGroup() ? R.color.accent : R.color.primary
        ));

        btnMention.setVisibility(conv.isGroup() ? View.VISIBLE : View.GONE);
        etInput.setHint(conv.isGroup() ? "Message group (use @name to mention)..." : "Write a direct message...");
    }

    private void sendCurrentMessage() {
        if (currentConversation == null) {
            Toast.makeText(this, "Loading conversation...", Toast.LENGTH_SHORT).show();
            return;
        }
        String text = etInput.getText() != null ? etInput.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        String senderName = resolveSenderName(fbUser);
        String senderEmail = fbUser != null && fbUser.getEmail() != null ? fbUser.getEmail() : "";

        ChatMessage msg = new ChatMessage(
            null, conversationId, currentUserId, senderName, senderEmail, text, "TEXT", System.currentTimeMillis()
        );

        if (replyingToMessage != null) {
            msg.setReplyToMessageId(replyingToMessage.getMessageId());
            msg.setReplyToSenderName(replyingToMessage.getSenderName());
            msg.setReplyToPreview(replyingToMessage.getMessage());
        }

        if (currentConversation.isGroup()) {
            List<User> participants = resolveGroupParticipantUsers();
            List<User> mentioned = MentionParser.resolveMentionedUsers(text, participants);
            List<String> mentionIds = new ArrayList<>();
            for (User u : mentioned) {
                if (u.getUserId() != null) mentionIds.add(u.getUserId());
            }
            msg.setMentionedUserIds(mentionIds);
        }

        chatViewModel.sendMessage(currentConversation, msg);
        etInput.setText("");
        clearReplyTarget();
    }

    private void showMentionPicker() {
        if (currentConversation == null || !currentConversation.isGroup()) return;
        List<User> participants = new ArrayList<>();
        for (User u : resolveGroupParticipantUsers()) {
            if (!currentUserId.equals(u.getUserId())) {
                participants.add(u);
            }
        }
        if (participants.isEmpty()) {
            Toast.makeText(this, "No other group members to mention.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] labels = new String[participants.size()];
        for (int i = 0; i < participants.size(); i++) {
            User u = participants.get(i);
            labels[i] = (u.getName() != null ? u.getName() : u.getEmail())
                    + " (" + MentionParser.formatMentionHandle(u) + ")";
        }

        new AlertDialog.Builder(this)
            .setTitle("Mention Group Member")
            .setItems(labels, (d, which) -> {
                String handle = MentionParser.formatMentionHandle(participants.get(which));
                String current = etInput.getText() != null ? etInput.getText().toString() : "";
                String next = current.isEmpty() ? handle + " " : current + (current.endsWith(" ") ? "" : " ") + handle + " ";
                etInput.setText(next);
                etInput.setSelection(next.length());
            })
            .show();
    }

    private void showMessageOptions(ChatMessage msg) {
        if (msg == null || msg.getMessageId() == null) return;
        final String[] actions = {"Reply to message", "👍 React", "❤️ React", "🔥 React", "✅ React", "🎉 React", "👀 React"};
        new AlertDialog.Builder(this)
            .setTitle("Message Actions")
            .setItems(actions, (d, which) -> {
                if (which == 0) {
                    replyingToMessage = msg;
                    layoutReplyPreview.setVisibility(View.VISIBLE);
                    tvReplyPreview.setText("Replying to " + msg.getSenderName() + ": " + msg.getMessage());
                } else if (!currentUserId.isEmpty()) {
                    String emoji = actions[which].split(" ")[0];
                    chatViewModel.toggleReaction(conversationId, msg.getMessageId(), emoji, currentUserId);
                }
            })
            .show();
    }

    private void clearReplyTarget() {
        replyingToMessage = null;
        if (layoutReplyPreview != null) {
            layoutReplyPreview.setVisibility(View.GONE);
        }
    }

    private void showCallDialog() {
        String safeId = conversationId.replaceAll("[^a-zA-Z0-9]", "");
        String meetingUrl = "https://meet.jit.si/ProjectHub_Chat_" + safeId;
        String title = currentConversation != null
                ? ConversationUtils.resolveDisplayTitle(currentConversation, currentUserId)
                : "Conversation";

        new AlertDialog.Builder(this)
            .setTitle("Video / Voice Call")
            .setMessage("Join an instant meeting room with " + title + ":\n\n" + meetingUrl)
            .setPositiveButton("Join Call Now", (d, w) -> {
                SafeUrlUtils.openSafeHttpUrl(ChatRoomActivity.this, meetingUrl);
            })
            .setNeutralButton("Share Link in Chat", (d, w) -> {
                if (currentConversation == null) return;
                FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
                ChatMessage callMsg = new ChatMessage(
                    null,
                    conversationId,
                    currentUserId,
                    resolveSenderName(fbUser),
                    fbUser != null && fbUser.getEmail() != null ? fbUser.getEmail() : "",
                    "Started a video call! Tap to join: " + meetingUrl,
                    "CALL",
                    System.currentTimeMillis()
                );
                chatViewModel.sendMessage(currentConversation, callMsg);
                NotificationHelper.showSystemNotification(
                    this, "Live Call", "Meeting room opened in " + title,
                    conversationId, currentConversation.isGroup() ? "GROUP_CHAT" : "DM"
                );
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showConversationInfoDialog() {
        if (currentConversation == null) return;
        if (currentConversation.isDirect()) {
            String otherUid = ConversationUtils.resolveOtherParticipantId(currentConversation, currentUserId);
            String name = ConversationUtils.resolveDisplayTitle(currentConversation, currentUserId);
            String email = currentConversation.getParticipantEmails().getOrDefault(otherUid, "");
            StringBuilder details = new StringBuilder();
            details.append("Participant: ").append(name);
            if (!email.isEmpty()) details.append("\nEmail: ").append(email);

            for (User u : allCampusUsers) {
                if (otherUid.equals(u.getUserId())) {
                    if (u.getDepartment() != null && !u.getDepartment().isEmpty()) {
                        details.append("\nDepartment: ").append(u.getDepartment());
                    }
                    if (u.getSkills() != null && !u.getSkills().isEmpty()) {
                        details.append("\nSkills: ").append(TextUtils.join(", ", u.getSkills()));
                    }
                    break;
                }
            }
            new AlertDialog.Builder(this)
                .setTitle("Direct Chat Info")
                .setMessage(details.toString())
                .setPositiveButton("Close", null)
                .show();
            return;
        }

        int count = currentConversation.getParticipantIds().size();
        final String[] options = {
            "View Members (" + count + ")",
            "Add Members",
            "Rename Group",
            "Leave Group"
        };

        new AlertDialog.Builder(this)
            .setTitle(currentConversation.getTitle())
            .setItems(options, (d, which) -> {
                if (which == 0) {
                    showGroupMembersDialog();
                } else if (which == 1) {
                    showAddGroupMembersDialog();
                } else if (which == 2) {
                    showRenameGroupDialog();
                } else {
                    confirmLeaveGroup();
                }
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private void showGroupMembersDialog() {
        List<User> participants = resolveGroupParticipantUsers();
        String[] labels = new String[participants.size()];
        for (int i = 0; i < participants.size(); i++) {
            User u = participants.get(i);
            String name = u.getName() != null && !u.getName().isEmpty() ? u.getName() : u.getEmail();
            String suffix = u.getUserId().equals(currentUserId) ? " (You)" : " — Tap to DM";
            labels[i] = name + suffix;
        }

        new AlertDialog.Builder(this)
            .setTitle("Group Members (" + participants.size() + ")")
            .setItems(labels, (d, which) -> {
                User picked = participants.get(which);
                if (picked.getUserId() == null || picked.getUserId().equals(currentUserId)) return;
                User me = resolveCurrentUserObject();
                if (me == null) return;
                chatViewModel.getOrCreateDirectChat(me, picked, new ChatRepository.ConversationCallback() {
                    @Override
                    public void onSuccess(Conversation conversation) {
                        Intent intent = new Intent(ChatRoomActivity.this, ChatRoomActivity.class);
                        intent.putExtra("CONVERSATION_ID", conversation.getConversationId());
                        intent.putExtra("CONVERSATION_TITLE",
                            ConversationUtils.resolveDisplayTitle(conversation, currentUserId));
                        startActivity(intent);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(ChatRoomActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setPositiveButton("Close", null)
            .show();
    }

    private void showAddGroupMembersDialog() {
        List<String> existingIds = currentConversation.getParticipantIds();
        List<User> addable = new ArrayList<>();
        for (User u : allCampusUsers) {
            if (u != null && u.getUserId() != null && !existingIds.contains(u.getUserId())) {
                addable.add(u);
            }
        }
        if (addable.isEmpty()) {
            Toast.makeText(this, "All loaded campus users are already in this group.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[addable.size()];
        boolean[] checked = new boolean[addable.size()];
        for (int i = 0; i < addable.size(); i++) {
            User u = addable.get(i);
            String dept = u.getDepartment() != null && !u.getDepartment().isEmpty() ? " (" + u.getDepartment() + ")" : "";
            names[i] = (u.getName() != null && !u.getName().isEmpty() ? u.getName() : u.getEmail()) + dept;
        }

        new AlertDialog.Builder(this)
            .setTitle("Add Members to Group")
            .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
            .setPositiveButton("Add Selected", (d, w) -> {
                List<User> chosen = new ArrayList<>();
                for (int i = 0; i < addable.size(); i++) {
                    if (checked[i]) chosen.add(addable.get(i));
                }
                if (!chosen.isEmpty()) {
                    chatViewModel.addGroupMembers(
                        currentConversation,
                        chosen,
                        resolveSenderName(FirebaseAuth.getInstance().getCurrentUser())
                    );
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showRenameGroupDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, 0);

        TextInputEditText etTitle = new TextInputEditText(this);
        etTitle.setHint("Group Name");
        etTitle.setText(currentConversation.getTitle());
        layout.addView(etTitle);

        TextInputEditText etDesc = new TextInputEditText(this);
        etDesc.setHint("Topic / Description (optional)");
        etDesc.setText(currentConversation.getDescription());
        layout.addView(etDesc);

        new AlertDialog.Builder(this)
            .setTitle("Rename Group")
            .setView(layout)
            .setPositiveButton("Save", (d, w) -> {
                String newTitle = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                String newDesc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
                chatViewModel.renameGroup(conversationId, newTitle, newDesc);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void confirmLeaveGroup() {
        new AlertDialog.Builder(this)
            .setTitle("Leave Group?")
            .setMessage("You will stop receiving messages from \"" + currentConversation.getTitle() + "\".")
            .setPositiveButton("Leave", (d, w) -> chatViewModel.leaveGroup(
                currentConversation,
                currentUserId,
                new ChatRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(ChatRoomActivity.this, "Left group.", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(ChatRoomActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }
            ))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private List<User> resolveGroupParticipantUsers() {
        Map<String, User> byId = new HashMap<>();
        for (User u : allCampusUsers) {
            if (u != null && u.getUserId() != null) {
                byId.put(u.getUserId(), u);
            }
        }
        List<User> list = new ArrayList<>();
        if (currentConversation == null) return list;

        for (String uid : currentConversation.getParticipantIds()) {
            User found = byId.get(uid);
            if (found != null) {
                list.add(found);
            } else {
                String name = currentConversation.getParticipantNames().getOrDefault(uid, "Member");
                String email = currentConversation.getParticipantEmails().getOrDefault(uid, "");
                list.add(new User(uid, name, email, "", ""));
            }
        }
        return list;
    }

    private String resolveSenderName(FirebaseUser fbUser) {
        if (currentUserProfile != null && currentUserProfile.getName() != null
                && !currentUserProfile.getName().trim().isEmpty()) {
            return currentUserProfile.getName().trim();
        }
        if (fbUser != null && fbUser.getDisplayName() != null && !fbUser.getDisplayName().trim().isEmpty()) {
            return fbUser.getDisplayName().trim();
        }
        if (fbUser != null && fbUser.getEmail() != null && !fbUser.getEmail().isEmpty()) {
            return fbUser.getEmail().split("@")[0];
        }
        return "Member";
    }

    private User resolveCurrentUserObject() {
        if (currentUserProfile != null) return currentUserProfile;
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) return null;
        return new User(
            fbUser.getUid(),
            resolveSenderName(fbUser),
            fbUser.getEmail() != null ? fbUser.getEmail() : "",
            "",
            ""
        );
    }
}
