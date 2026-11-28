package com.projecthub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projecthub.R;
import com.projecthub.adapters.ExploreAdapter;
import com.projecthub.adapters.UserDirectoryAdapter;
import com.projecthub.models.Conversation;
import com.projecthub.models.JoinRequest;
import com.projecthub.models.Project;
import com.projecthub.models.User;
import com.projecthub.repository.ChatRepository;
import com.projecthub.utils.ConversationUtils;
import com.projecthub.viewmodel.AuthViewModel;
import com.projecthub.viewmodel.ChatViewModel;
import com.projecthub.viewmodel.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExploreActivity extends AppCompatActivity {

    private DashboardViewModel dashboardViewModel;
    private AuthViewModel authViewModel;
    private ChatViewModel chatViewModel;
    private ExploreAdapter exploreAdapter;
    private UserDirectoryAdapter userDirectoryAdapter;

    private RecyclerView rvExploreProjects, rvCampusUsers;
    private ProgressBar pbExploreLoading;
    private TextView tvEmptyExplore;
    private ImageButton btnBack, btnClearSearch;
    private TextInputEditText etSearchQuery;
    private ChipGroup chipGroupMode, chipGroupCategories, chipGroupPeopleFilters;
    private View hsvProjectCategories, hsvPeopleFilters;

    private List<Project> allProjectsList = new ArrayList<>();
    private List<Project> myProjectsList = new ArrayList<>();
    private List<User> allUsersList = new ArrayList<>();
    private User currentUserProfile;

    private boolean isPeopleMode = false;
    private String currentCategoryFilter = "ALL";
    private String currentPeopleFilter = "ALL";
    private String currentQueryText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);

        rvExploreProjects = findViewById(R.id.rvExploreProjects);
        rvCampusUsers = findViewById(R.id.rvCampusUsers);
        pbExploreLoading = findViewById(R.id.pbExploreLoading);
        tvEmptyExplore = findViewById(R.id.tvEmptyExplore);
        btnBack = findViewById(R.id.btnBack);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        etSearchQuery = findViewById(R.id.etSearchQuery);
        chipGroupMode = findViewById(R.id.chipGroupExploreMode);
        chipGroupCategories = findViewById(R.id.chipGroupExploreCategories);
        chipGroupPeopleFilters = findViewById(R.id.chipGroupPeopleFilters);
        hsvProjectCategories = findViewById(R.id.hsvProjectCategories);
        hsvPeopleFilters = findViewById(R.id.hsvPeopleFilters);

        btnBack.setOnClickListener(v -> finish());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = currentUser != null ? currentUser.getUid() : "";

        exploreAdapter = new ExploreAdapter(userId, new ExploreAdapter.OnProjectClickListener() {
            @Override
            public void onRequestJoin(Project project) {
                showSendRequestDialog(project);
            }

            @Override
            public void onJoinDirectly(Project project) {
                showSendRequestDialog(project);
            }
        });

        rvExploreProjects.setLayoutManager(new LinearLayoutManager(this));
        rvExploreProjects.setAdapter(exploreAdapter);

        userDirectoryAdapter = new UserDirectoryAdapter(userId, this::showInviteUserDialog, this::startDirectChatWithUser);
        rvCampusUsers.setLayoutManager(new LinearLayoutManager(this));
        rvCampusUsers.setAdapter(userDirectoryAdapter);

        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        authViewModel.fetchCurrentUserProfile();
        authViewModel.getCurrentUser().observe(this, u -> currentUserProfile = u);

        if (!userId.isEmpty()) {
            dashboardViewModel.getProjects(userId).observe(this, projects -> {
                myProjectsList = projects != null ? projects : new ArrayList<>();
            });
        }

        dashboardViewModel.getPublicProjects().observe(this, projects -> {
            pbExploreLoading.setVisibility(View.GONE);
            allProjectsList = projects != null ? projects : new ArrayList<>();
            if (!isPeopleMode) applySearchAndFilter();
        });

        authViewModel.getCampusUsers().observe(this, users -> {
            allUsersList = users != null ? users : new ArrayList<>();
            if (isPeopleMode) applySearchAndFilter();
        });

        dashboardViewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        setupSearchAndFilters();
    }

    private void setupSearchAndFilters() {
        if (chipGroupMode != null) {
            chipGroupMode.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                isPeopleMode = checkedIds.get(0) == R.id.chipExploreModePeople;
                hsvProjectCategories.setVisibility(isPeopleMode ? View.GONE : View.VISIBLE);
                hsvPeopleFilters.setVisibility(isPeopleMode ? View.VISIBLE : View.GONE);
                rvExploreProjects.setVisibility(isPeopleMode ? View.GONE : View.VISIBLE);
                rvCampusUsers.setVisibility(isPeopleMode ? View.VISIBLE : View.GONE);
                if (etSearchQuery != null) {
                    etSearchQuery.setHint(isPeopleMode
                            ? "Search teammates, skills (Android, ML), dept..."
                            : "Search public project name, lead, skill...");
                }
                applySearchAndFilter();
            });
        }

        if (etSearchQuery != null) {
            etSearchQuery.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentQueryText = s != null ? s.toString().trim().toLowerCase(Locale.getDefault()) : "";
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(currentQueryText.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    applySearchAndFilter();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (etSearchQuery != null) etSearchQuery.setText("");
            });
        }

        if (chipGroupCategories != null) {
            chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) {
                    currentCategoryFilter = "ALL";
                } else {
                    int checkedId = checkedIds.get(0);
                    if (checkedId == R.id.chipCategorySIH) {
                        currentCategoryFilter = "SIH";
                    } else if (checkedId == R.id.chipCategoryAI) {
                        currentCategoryFilter = "AI";
                    } else if (checkedId == R.id.chipCategoryMobile) {
                        currentCategoryFilter = "MOBILE";
                    } else if (checkedId == R.id.chipCategoryWeb) {
                        currentCategoryFilter = "WEB";
                    } else if (checkedId == R.id.chipCategoryGeneral) {
                        currentCategoryFilter = "GENERAL";
                    } else {
                        currentCategoryFilter = "ALL";
                    }
                }
                applySearchAndFilter();
            });
        }

        if (chipGroupPeopleFilters != null) {
            chipGroupPeopleFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) {
                    currentPeopleFilter = "ALL";
                } else {
                    int checkedId = checkedIds.get(0);
                    if (checkedId == R.id.chipPeopleMentors) {
                        currentPeopleFilter = "MENTORS";
                    } else if (checkedId == R.id.chipPeopleVerified) {
                        currentPeopleFilter = "VERIFIED";
                    } else if (checkedId == R.id.chipPeopleStudents) {
                        currentPeopleFilter = "STUDENTS";
                    } else {
                        currentPeopleFilter = "ALL";
                    }
                }
                applySearchAndFilter();
            });
        }
    }

    private void applySearchAndFilter() {
        if (isPeopleMode) {
            filterPeople();
        } else {
            filterProjects();
        }
    }

    private void filterProjects() {
        if (allProjectsList == null) {
            exploreAdapter.setProjects(null);
            tvEmptyExplore.setVisibility(View.VISIBLE);
            return;
        }

        List<Project> filtered = new ArrayList<>();
        for (Project p : allProjectsList) {
            boolean matchesCategory = true;
            String cat = p.getCategory() != null ? p.getCategory().toUpperCase(Locale.getDefault()) : "GENERAL";
            if (!"ALL".equals(currentCategoryFilter)) {
                if ("SIH".equals(currentCategoryFilter)) {
                    matchesCategory = cat.contains("SIH") || cat.contains("HACKATHON");
                } else if ("AI".equals(currentCategoryFilter)) {
                    matchesCategory = cat.contains("AI") || cat.contains("ML");
                } else if ("MOBILE".equals(currentCategoryFilter)) {
                    matchesCategory = cat.contains("MOBILE") || cat.contains("APP") || cat.contains("ANDROID") || cat.contains("IOS");
                } else if ("WEB".equals(currentCategoryFilter)) {
                    matchesCategory = cat.contains("WEB") || cat.contains("FULLSTACK") || cat.contains("FRONTEND");
                } else if ("GENERAL".equals(currentCategoryFilter)) {
                    matchesCategory = cat.contains("GENERAL") || cat.contains("ACADEMIC") || cat.contains("CAPSTONE") || cat.contains("RESEARCH");
                }
            }
            if (!matchesCategory) continue;

            if (!currentQueryText.isEmpty()) {
                String title = p.getTitle() != null ? p.getTitle().toLowerCase(Locale.getDefault()) : "";
                String desc = p.getDescription() != null ? p.getDescription().toLowerCase(Locale.getDefault()) : "";
                String lead = p.getLeadName() != null ? p.getLeadName().toLowerCase(Locale.getDefault()) : "";
                String looking = p.getLookingFor() != null ? p.getLookingFor().toLowerCase(Locale.getDefault()) : "";
                String domain = p.getCollegeDomain() != null ? p.getCollegeDomain().toLowerCase(Locale.getDefault()) : "";

                boolean matchesQuery = title.contains(currentQueryText)
                        || lead.contains(currentQueryText)
                        || looking.contains(currentQueryText)
                        || desc.contains(currentQueryText)
                        || domain.contains(currentQueryText)
                        || cat.toLowerCase(Locale.getDefault()).contains(currentQueryText);
                if (!matchesQuery) continue;
            }
            filtered.add(p);
        }

        exploreAdapter.setProjects(filtered);
        tvEmptyExplore.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void filterPeople() {
        List<User> filtered = new ArrayList<>();
        for (User u : allUsersList) {
            if ("MENTORS".equals(currentPeopleFilter) && !"FACULTY_MENTOR".equalsIgnoreCase(u.getRole())) {
                continue;
            }
            if ("VERIFIED".equals(currentPeopleFilter) && !u.isCampusVerified()) {
                continue;
            }
            if ("STUDENTS".equals(currentPeopleFilter) && "FACULTY_MENTOR".equalsIgnoreCase(u.getRole())) {
                continue;
            }

            if (!currentQueryText.isEmpty()) {
                String name = u.getName() != null ? u.getName().toLowerCase(Locale.getDefault()) : "";
                String dept = u.getDepartment() != null ? u.getDepartment().toLowerCase(Locale.getDefault()) : "";
                String domain = u.getCollegeDomain() != null ? u.getCollegeDomain().toLowerCase(Locale.getDefault()) : "";
                String bio = u.getBio() != null ? u.getBio().toLowerCase(Locale.getDefault()) : "";
                String skillsJoined = u.getSkills() != null ? String.join(" ", u.getSkills()).toLowerCase(Locale.getDefault()) : "";

                boolean matches = name.contains(currentQueryText)
                        || dept.contains(currentQueryText)
                        || domain.contains(currentQueryText)
                        || bio.contains(currentQueryText)
                        || skillsJoined.contains(currentQueryText);
                if (!matches) continue;
            }
            filtered.add(u);
        }
        userDirectoryAdapter.setUsers(filtered);
        tvEmptyExplore.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showInviteUserDialog(User targetUser) {
        if (myProjectsList == null || myProjectsList.isEmpty()) {
            Toast.makeText(this, "Create or join a project first to send invitations.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] titles = new String[myProjectsList.size()];
        for (int i = 0; i < myProjectsList.size(); i++) {
            titles[i] = myProjectsList.get(i).getTitle() != null ? myProjectsList.get(i).getTitle() : "Project";
        }

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        String inviterName = currentUserProfile != null && currentUserProfile.getName() != null
                ? currentUserProfile.getName()
                : (fbUser != null && fbUser.getDisplayName() != null ? fbUser.getDisplayName() : "Teammate");

        String roleToInvite = "FACULTY_MENTOR".equalsIgnoreCase(targetUser.getRole()) ? "FACULTY" : "MEMBER";

        new AlertDialog.Builder(this)
            .setTitle("Invite " + targetUser.getName() + " to:")
            .setItems(titles, (dialog, which) -> {
                Project selected = myProjectsList.get(which);
                dashboardViewModel.sendTeamOrMentorInvite(targetUser, selected, inviterName, roleToInvite);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void startDirectChatWithUser(User targetUser) {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null || targetUser == null) return;
        User me = currentUserProfile;
        if (me == null) {
            String fallbackName = fbUser.getDisplayName() != null && !fbUser.getDisplayName().isEmpty()
                    ? fbUser.getDisplayName()
                    : (fbUser.getEmail() != null ? fbUser.getEmail().split("@")[0] : "Member");
            me = new User(fbUser.getUid(), fallbackName, fbUser.getEmail() != null ? fbUser.getEmail() : "", "", "");
        }
        String myUid = fbUser.getUid();
        chatViewModel.getOrCreateDirectChat(me, targetUser, new ChatRepository.ConversationCallback() {
            @Override
            public void onSuccess(Conversation conversation) {
                Intent intent = new Intent(ExploreActivity.this, ChatRoomActivity.class);
                intent.putExtra("CONVERSATION_ID", conversation.getConversationId());
                intent.putExtra("CONVERSATION_TITLE",
                    ConversationUtils.resolveDisplayTitle(conversation, myUid));
                startActivity(intent);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ExploreActivity.this, "Could not open chat: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSendRequestDialog(Project project) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_send_request, null);
        TextInputEditText etRequestMessage = dialogView.findViewById(R.id.etRequestMessage);

        new AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Send Request", (dialog, which) -> {
                String message = etRequestMessage.getText() != null ? 
                                etRequestMessage.getText().toString().trim() : "";

                String applicantName = currentUserProfile != null && currentUserProfile.getName() != null
                        ? currentUserProfile.getName()
                        : (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()
                            ? currentUser.getDisplayName() : currentUser.getEmail());

                JoinRequest request = new JoinRequest(
                    null,
                    project.getProjectId(),
                    project.getTitle(),
                    project.getLeadId(),
                    currentUser.getUid(),
                    applicantName,
                    currentUser.getEmail(),
                    message,
                    "PENDING",
                    System.currentTimeMillis()
                );

                dashboardViewModel.sendJoinRequest(request);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
