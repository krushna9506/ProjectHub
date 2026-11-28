package com.projecthub.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.projecthub.R;
import com.projecthub.adapters.ProjectAdapter;
import com.projecthub.models.AppNotification;
import com.projecthub.models.Conversation;
import com.projecthub.utils.AppPreferences;
import com.projecthub.utils.ConversationUtils;
import com.projecthub.utils.NotificationHelper;
import com.projecthub.viewmodel.AuthViewModel;
import com.projecthub.viewmodel.ChatViewModel;
import com.projecthub.viewmodel.DashboardViewModel;

public class MainActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private DashboardViewModel dashboardViewModel;
    private ChatViewModel chatViewModel;
    private ProjectAdapter projectAdapter;

    private TextView tvGreeting, tvStudentInfo, tvUserAvatar, tvProjectCountBadge, tvNotificationBadge, tvChatUnreadBadge;
    private View layoutEmptyProjects, layoutHeaderProfile;
    private ProgressBar pbLoading;
    private RecyclerView rvProjects;
    private MaterialButton btnCreateProject, btnExploreCampus, btnEmptyJoinCode;
    private ImageButton btnSettings, btnNotifications, btnChats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layoutHeaderProfile = findViewById(R.id.layoutHeaderProfile);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvStudentInfo = findViewById(R.id.tvStudentInfo);
        tvUserAvatar = findViewById(R.id.tvUserAvatar);
        tvProjectCountBadge = findViewById(R.id.tvProjectCountBadge);
        layoutEmptyProjects = findViewById(R.id.layoutEmptyProjects);
        pbLoading = findViewById(R.id.pbLoading);
        rvProjects = findViewById(R.id.rvProjects);
        btnCreateProject = findViewById(R.id.btnCreateProject);
        btnExploreCampus = findViewById(R.id.btnExploreCampus);
        btnEmptyJoinCode = findViewById(R.id.btnEmptyJoinCode);
        btnSettings = findViewById(R.id.btnSettings);
        btnNotifications = findViewById(R.id.btnNotifications);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        btnChats = findViewById(R.id.btnChats);
        tvChatUnreadBadge = findViewById(R.id.tvChatUnreadBadge);

        NotificationHelper.createNotificationChannel(this);
        checkNotificationPermission();
        scheduleDeadlineWorker();

        if (layoutHeaderProfile != null) {
            layoutHeaderProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
        }

        btnChats.setOnClickListener(v ->
            startActivity(new Intent(MainActivity.this, ChatsActivity.class)));

        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NotificationsActivity.class));
        });

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        projectAdapter = new ProjectAdapter(project -> {
            Intent intent = new Intent(MainActivity.this, ProjectDetailsActivity.class);
            intent.putExtra("PROJECT_ID", project.getProjectId());
            intent.putExtra("PROJECT_TITLE", project.getTitle());
            intent.putExtra("PROJECT_DESC", project.getDescription());
            intent.putExtra("PROJECT_CODE", project.getJoinCode());
            intent.putExtra("PROJECT_PROGRESS", project.getProgressPercentage());
            startActivity(intent);
        });

        rvProjects.setLayoutManager(new LinearLayoutManager(this));
        rvProjects.setAdapter(projectAdapter);

        // Fetch User Info
        authViewModel.fetchCurrentUserProfile();
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                String badge = user.isCampusVerified() ? " ✓" : "";
                tvGreeting.setText("Hello, " + user.getName() + badge);
                if (user.getName() != null && !user.getName().isEmpty()) {
                    tvUserAvatar.setText(user.getName().substring(0, 1).toUpperCase());
                }
                String roleLabel = "FACULTY_MENTOR".equalsIgnoreCase(user.getRole()) ? "Mentor" : user.getRollNumber();
                tvStudentInfo.setText(user.getDepartment() + " • " + roleLabel);
                loadProjects(user.getUserId());
                registerFcmToken(user.getUserId());
                observeNotifications(user.getUserId());
                observeUnreadChats(user.getUserId());
            }
        });

        // Dashboard Status Messages (e.g. Join success/error)
        dashboardViewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        btnCreateProject.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CreateProjectActivity.class));
        });

        btnExploreCampus.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ExploreActivity.class));
        });

        if (btnEmptyJoinCode != null) {
            btnEmptyJoinCode.setOnClickListener(v -> showJoinProjectDialog());
        }

        btnSettings.setOnClickListener(v ->
            startActivity(new Intent(MainActivity.this, SettingsActivity.class)));
    }

    private void loadProjects(String userId) {
        dashboardViewModel.getProjects(userId).observe(this, projects -> {
            pbLoading.setVisibility(View.GONE);
            if (projects == null || projects.isEmpty()) {
                if (layoutEmptyProjects != null) layoutEmptyProjects.setVisibility(View.VISIBLE);
                if (tvProjectCountBadge != null) tvProjectCountBadge.setText("0 projects");
                projectAdapter.setProjects(null);
            } else {
                if (layoutEmptyProjects != null) layoutEmptyProjects.setVisibility(View.GONE);
                if (tvProjectCountBadge != null) {
                    int size = projects.size();
                    tvProjectCountBadge.setText(size + (size == 1 ? " project" : " projects"));
                }
                projectAdapter.setProjects(projects);
            }
        });
    }

    private void registerFcmToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
            .addOnSuccessListener(token -> {
                if (token != null && !token.isEmpty()) {
                    dashboardViewModel.saveFcmToken(userId, token);
                }
            })
            .addOnFailureListener(e -> {
                // FCM token retrieval ignored in offline or dev mode
            });
    }

    private void observeNotifications(String userId) {
        dashboardViewModel.getInAppNotifications(userId).observe(this, list -> {
            if (list == null || list.isEmpty()) {
                tvNotificationBadge.setVisibility(View.GONE);
                return;
            }
            int unread = 0;
            for (AppNotification n : list) {
                if (!n.isRead()) unread++;
            }
            if (unread > 0) {
                tvNotificationBadge.setVisibility(View.VISIBLE);
                tvNotificationBadge.setText(unread > 9 ? "9+" : String.valueOf(unread));
                if (!list.isEmpty() && !list.get(0).isRead()) {
                    AppNotification top = list.get(0);
                    String dedupKey = "notif_" + (top.getNotificationId() != null ? top.getNotificationId() : top.getTimestamp());
                    if (NotificationHelper.shouldNotifyOnce(MainActivity.this, dedupKey)) {
                        NotificationHelper.showSystemNotification(
                            MainActivity.this,
                            top.getTitle() != null ? top.getTitle() : "ProjectHub Alert",
                            top.getMessage() != null ? top.getMessage() : "New project activity",
                            top.getProjectId(),
                            top.getType()
                        );
                    }
                }
            } else {
                tvNotificationBadge.setVisibility(View.GONE);
            }
        });
    }

    private void observeUnreadChats(String userId) {
        chatViewModel.getConversations(userId).observe(this, list -> {
            if (tvChatUnreadBadge == null) return;
            int unreadCount = 0;
            if (list != null) {
                for (Conversation c : list) {
                    if (ConversationUtils.isUnread(c, userId)) {
                        unreadCount++;
                    }
                }
            }
            if (unreadCount > 0) {
                tvChatUnreadBadge.setVisibility(View.VISIBLE);
                tvChatUnreadBadge.setText(unreadCount > 9 ? "9+" : String.valueOf(unreadCount));
            } else {
                tvChatUnreadBadge.setVisibility(View.GONE);
            }
        });
    }

    private void scheduleDeadlineWorker() {
        AppPreferences.syncDeadlineReminderSchedule(this);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    1001
                );
            }
        }
    }

    private void showJoinProjectDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_join_project, null);
        TextInputEditText etJoinCode = dialogView.findViewById(R.id.etJoinCode);

        new AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Join", (dialog, which) -> {
                String code = etJoinCode.getText() != null ? etJoinCode.getText().toString().trim() : "";
                String uid = authViewModel.getCurrentUserId();
                if (uid == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
                    uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                }
                if (!code.isEmpty() && uid != null) {
                    dashboardViewModel.joinProject(uid, code);
                } else {
                    Toast.makeText(this, "Please enter a valid join code", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
