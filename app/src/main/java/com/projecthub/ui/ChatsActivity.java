package com.projecthub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projecthub.R;
import com.projecthub.adapters.ConversationAdapter;
import com.projecthub.models.Conversation;
import com.projecthub.models.User;
import com.projecthub.repository.ChatRepository;
import com.projecthub.utils.ConversationUtils;
import com.projecthub.viewmodel.AuthViewModel;
import com.projecthub.viewmodel.ChatViewModel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ChatsActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private ChatViewModel chatViewModel;
    private ConversationAdapter adapter;

    private RecyclerView rvConversations;
    private ProgressBar pbLoading;
    private View layoutEmpty;
    private TextInputEditText etSearch;
    private ImageButton btnBack, btnClearSearch;
    private MaterialButton btnNewGroupChat;
    private ChipGroup chipGroupFilter;
    private ExtendedFloatingActionButton fabNewDirectChat;

    private String currentUserId = "";
    private User currentUserProfile;
    private List<Conversation> allConversations = new ArrayList<>();
    private List<User> allCampusUsers = new ArrayList<>();
    private String currentFilter = "ALL"; // ALL, DIRECT, GROUP
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = fbUser != null ? fbUser.getUid() : "";

        btnBack = findViewById(R.id.btnBack);
        btnNewGroupChat = findViewById(R.id.btnNewGroupChat);
        etSearch = findViewById(R.id.etChatSearch);
        btnClearSearch = findViewById(R.id.btnClearChatSearch);
        chipGroupFilter = findViewById(R.id.chipGroupChatFilter);
        rvConversations = findViewById(R.id.rvConversations);
        layoutEmpty = findViewById(R.id.layoutEmptyChats);
        pbLoading = findViewById(R.id.pbChatsLoading);
        fabNewDirectChat = findViewById(R.id.fabNewDirectChat);

        btnBack.setOnClickListener(v -> finish());

        adapter = new ConversationAdapter(currentUserId, this::openChatRoom);
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        rvConversations.setAdapter(adapter);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        authViewModel.fetchCurrentUserProfile();
        authViewModel.getCurrentUser().observe(this, user -> currentUserProfile = user);
        authViewModel.getCampusUsers().observe(this, users ->
            allCampusUsers = users != null ? users : new ArrayList<>());

        if (!currentUserId.isEmpty()) {
            chatViewModel.getConversations(currentUserId).observe(this, list -> {
                pbLoading.setVisibility(View.GONE);
                allConversations = list != null ? list : new ArrayList<>();
                applyFilters();
            });
        } else {
            pbLoading.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        }

        chatViewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        setupSearchAndFilters();

        fabNewDirectChat.setOnClickListener(v -> showNewDirectChatDialog());
        btnNewGroupChat.setOnClickListener(v -> showCreateGroupDialog());
    }

    private void setupSearchAndFilters() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString().trim().toLowerCase(Locale.getDefault()) : "";
                btnClearSearch.setVisibility(currentQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentFilter = "ALL";
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chipChatFilterDirect) {
                    currentFilter = "DIRECT";
                } else if (id == R.id.chipChatFilterGroup) {
                    currentFilter = "GROUP";
                } else {
                    currentFilter = "ALL";
                }
            }
            applyFilters();
        });
    }

    private void applyFilters() {
        List<Conversation> filtered = new ArrayList<>();
        for (Conversation c : allConversations) {
            if ("DIRECT".equals(currentFilter) && !c.isDirect()) continue;
            if ("GROUP".equals(currentFilter) && !c.isGroup()) continue;

            if (!currentQuery.isEmpty()) {
                String title = ConversationUtils.resolveDisplayTitle(c, currentUserId).toLowerCase(Locale.getDefault());
                String desc = c.getDescription().toLowerCase(Locale.getDefault());
                String preview = c.getLastMessage().toLowerCase(Locale.getDefault());
                if (!title.contains(currentQuery) && !desc.contains(currentQuery) && !preview.contains(currentQuery)) {
                    continue;
                }
            }
            filtered.add(c);
        }
        adapter.setConversations(filtered);
        layoutEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showNewDirectChatDialog() {
        List<User> candidates = new ArrayList<>();
        for (User u : allCampusUsers) {
            if (u != null && u.getUserId() != null && !u.getUserId().equals(currentUserId)) {
                candidates.add(u);
            }
        }
        if (candidates.isEmpty()) {
            Toast.makeText(this, "No other campus users loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, 0);

        EditText etFilter = new EditText(this);
        etFilter.setHint("Search student or mentor...");
        container.addView(etFilter);

        ListView listView = new ListView(this);
        LinearLayout.LayoutParams lvParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (int) (300 * getResources().getDisplayMetrics().density));
        listView.setLayoutParams(lvParams);
        container.addView(listView);

        List<User> visibleUsers = new ArrayList<>(candidates);
        List<String> labels = new ArrayList<>();
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        listView.setAdapter(listAdapter);

        Runnable rebuildList = () -> {
            String q = etFilter.getText() != null ? etFilter.getText().toString().trim().toLowerCase(Locale.getDefault()) : "";
            visibleUsers.clear();
            labels.clear();
            for (User u : candidates) {
                String name = u.getName() != null ? u.getName() : "";
                String dept = u.getDepartment() != null ? u.getDepartment() : "";
                String email = u.getEmail() != null ? u.getEmail() : "";
                if (q.isEmpty()
                        || name.toLowerCase(Locale.getDefault()).contains(q)
                        || dept.toLowerCase(Locale.getDefault()).contains(q)
                        || email.toLowerCase(Locale.getDefault()).contains(q)) {
                    visibleUsers.add(u);
                    String role = "FACULTY_MENTOR".equalsIgnoreCase(u.getRole()) ? "Mentor" : "Student";
                    String meta = !dept.isEmpty() ? dept + " • " + role : role;
                    labels.add((name.isEmpty() ? email : name) + " (" + meta + ")");
                }
            }
            listAdapter.notifyDataSetChanged();
        };
        rebuildList.run();

        etFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                rebuildList.run();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Start 1:1 Direct Chat")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= visibleUsers.size()) return;
            User target = visibleUsers.get(position);
            dialog.dismiss();
            startDirectChatWith(target);
        });

        dialog.show();
    }

    private void showCreateGroupDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_group_chat, null);
        TextInputEditText etGroupName = view.findViewById(R.id.etGroupName);
        TextInputEditText etGroupDesc = view.findViewById(R.id.etGroupDesc);
        TextInputEditText etMemberSearch = view.findViewById(R.id.etGroupMemberSearch);
        LinearLayout layoutChecks = view.findViewById(R.id.layoutGroupMemberChecks);
        TextView tvNoCandidates = view.findViewById(R.id.tvNoGroupCandidates);

        List<User> candidates = new ArrayList<>();
        for (User u : allCampusUsers) {
            if (u != null && u.getUserId() != null && !u.getUserId().equals(currentUserId)) {
                candidates.add(u);
            }
        }

        Set<String> checkedIds = new LinkedHashSet<>();

        Runnable renderCheckboxes = () -> {
            String q = etMemberSearch.getText() != null
                    ? etMemberSearch.getText().toString().trim().toLowerCase(Locale.getDefault())
                    : "";
            layoutChecks.removeAllViews();
            int shown = 0;
            for (User u : candidates) {
                String name = u.getName() != null ? u.getName() : "";
                String dept = u.getDepartment() != null ? u.getDepartment() : "";
                String email = u.getEmail() != null ? u.getEmail() : "";
                if (!q.isEmpty()
                        && !name.toLowerCase(Locale.getDefault()).contains(q)
                        && !dept.toLowerCase(Locale.getDefault()).contains(q)
                        && !email.toLowerCase(Locale.getDefault()).contains(q)) {
                    continue;
                }
                CheckBox cb = new CheckBox(this);
                String label = (!name.isEmpty() ? name : email) + (!dept.isEmpty() ? " (" + dept + ")" : "");
                cb.setText(label);
                cb.setChecked(checkedIds.contains(u.getUserId()));
                cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        checkedIds.add(u.getUserId());
                    } else {
                        checkedIds.remove(u.getUserId());
                    }
                });
                layoutChecks.addView(cb);
                shown++;
            }
            tvNoCandidates.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
        };
        renderCheckboxes.run();

        etMemberSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderCheckboxes.run();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Create Group", (d, w) -> {
                String title = etGroupName.getText() != null ? etGroupName.getText().toString().trim() : "";
                String desc = etGroupDesc.getText() != null ? etGroupDesc.getText().toString().trim() : "";
                if (title.isEmpty()) {
                    Toast.makeText(this, "Group name is required.", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<User> selectedMembers = new ArrayList<>();
                for (User u : candidates) {
                    if (checkedIds.contains(u.getUserId())) {
                        selectedMembers.add(u);
                    }
                }
                User me = resolveCurrentUserObject();
                if (me == null) {
                    Toast.makeText(this, "Please wait for sign-in to complete.", Toast.LENGTH_SHORT).show();
                    return;
                }
                chatViewModel.createGroupChat(me, title, desc, selectedMembers, new ChatRepository.ConversationCallback() {
                    @Override
                    public void onSuccess(Conversation conversation) {
                        openChatRoom(conversation);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(ChatsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void startDirectChatWith(User target) {
        User me = resolveCurrentUserObject();
        if (me == null) {
            Toast.makeText(this, "Sign-in required to start messaging.", Toast.LENGTH_SHORT).show();
            return;
        }
        chatViewModel.getOrCreateDirectChat(me, target, new ChatRepository.ConversationCallback() {
            @Override
            public void onSuccess(Conversation conversation) {
                openChatRoom(conversation);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ChatsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openChatRoom(Conversation conv) {
        if (conv == null || conv.getConversationId() == null) return;
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("CONVERSATION_ID", conv.getConversationId());
        intent.putExtra("CONVERSATION_TITLE", ConversationUtils.resolveDisplayTitle(conv, currentUserId));
        startActivity(intent);
    }

    private User resolveCurrentUserObject() {
        if (currentUserProfile != null) return currentUserProfile;
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) return null;
        String name = fbUser.getDisplayName();
        if (name == null || name.isEmpty()) {
            name = fbUser.getEmail() != null ? fbUser.getEmail().split("@")[0] : "Member";
        }
        return new User(fbUser.getUid(), name, fbUser.getEmail() != null ? fbUser.getEmail() : "", "", "");
    }
}
