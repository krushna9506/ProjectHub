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
import com.google.firebase.messaging.FirebaseMessaging;
import com.projecthub.R;
import com.projecthub.adapters.ProjectAdapter;
import com.projecthub.models.AppNotification;
import com.projecthub.utils.NotificationHelper;
import com.projecthub.viewmodel.AuthViewModel;
import com.projecthub.viewmodel.DashboardViewModel;

public class MainActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private DashboardViewModel dashboardViewModel;
    private ProjectAdapter projectAdapter;

    private TextView tvGreeting, tvStudentInfo, tvUserAvatar, tvProjectCountBadge, tvNotificationBadge;
    private View layoutEmptyProjects;
    private ProgressBar pbLoading;
    private RecyclerView rvProjects;
    private MaterialButton btnCreateProject, btnJoinProject, btnExploreCampus, btnManageRequests;
    private ImageButton btnLogout, btnNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvGreeting = findViewById(R.id.tvGreeting);
        tvStudentInfo = findViewById(R.id.tvStudentInfo);
        tvUserAvatar = findViewById(R.id.tvUserAvatar);
        tvProjectCountBadge = findViewById(R.id.tvProjectCountBadge);
        layoutEmptyProjects = findViewById(R.id.layoutEmptyProjects);
        pbLoading = findViewById(R.id.pbLoading);
        rvProjects = findViewById(R.id.rvProjects);
        btnCreateProject = findViewById(R.id.btnCreateProject);
        btnJoinProject = findViewById(R.id.btnJoinProject);
        btnExploreCampus = findViewById(R.id.btnExploreCampus);
        btnManageRequests = findViewById(R.id.btnManageRequests);
        btnLogout = findViewById(R.id.btnLogout);
        btnNotifications = findViewById(R.id.btnNotifications);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);

        NotificationHelper.createNotificationChannel(this);
        checkNotificationPermission();

        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NotificationsActivity.class));
        });

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

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
                tvGreeting.setText("Hello, " + user.getName());
                if (user.getName() != null && !user.getName().isEmpty()) {
                    tvUserAvatar.setText(user.getName().substring(0, 1).toUpperCase());
                }
                tvStudentInfo.setText(user.getDepartment() + " • " + user.getRollNumber());
                loadProjects(user.getUserId());
                registerFcmToken(user.getUserId());
                observeNotifications(user.getUserId());
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

        btnManageRequests.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ManageRequestsActivity.class));
        });

        btnJoinProject.setOnClickListener(v -> showJoinProjectDialog());

        btnLogout.setOnClickListener(v -> {
            authViewModel.logout();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
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
                    NotificationHelper.showSystemNotification(
                        MainActivity.this,
                        top.getTitle() != null ? top.getTitle() : "ProjectHub Alert",
                        top.getMessage() != null ? top.getMessage() : "New project activity",
                        top.getProjectId(),
                        top.getType()
                    );
                }
            } else {
                tvNotificationBadge.setVisibility(View.GONE);
            }
        });
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
                if (!code.isEmpty() && authViewModel.getCurrentUserId() != null) {
                    dashboardViewModel.joinProject(authViewModel.getCurrentUserId(), code);
                } else {
                    Toast.makeText(this, "Please enter a valid join code", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
