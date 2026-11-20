package com.projecthub.ui;

import android.content.Intent;
import android.os.Bundle;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.projecthub.R;
import com.projecthub.adapters.ProjectAdapter;
import com.projecthub.viewmodel.AuthViewModel;
import com.projecthub.viewmodel.DashboardViewModel;

public class MainActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private DashboardViewModel dashboardViewModel;
    private ProjectAdapter projectAdapter;

    private TextView tvGreeting, tvStudentInfo, tvEmptyProjects;
    private ProgressBar pbLoading;
    private RecyclerView rvProjects;
    private MaterialButton btnCreateProject, btnJoinProject, btnExploreCampus, btnManageRequests;
    private ImageButton btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvGreeting = findViewById(R.id.tvGreeting);
        tvStudentInfo = findViewById(R.id.tvStudentInfo);
        tvEmptyProjects = findViewById(R.id.tvEmptyProjects);
        pbLoading = findViewById(R.id.pbLoading);
        rvProjects = findViewById(R.id.rvProjects);
        btnCreateProject = findViewById(R.id.btnCreateProject);
        btnJoinProject = findViewById(R.id.btnJoinProject);
        btnExploreCampus = findViewById(R.id.btnExploreCampus);
        btnManageRequests = findViewById(R.id.btnManageRequests);
        btnLogout = findViewById(R.id.btnLogout);

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
                tvStudentInfo.setText(user.getDepartment() + " • " + user.getRollNumber());
                loadProjects(user.getUserId());
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
                tvEmptyProjects.setVisibility(View.VISIBLE);
                projectAdapter.setProjects(null);
            } else {
                tvEmptyProjects.setVisibility(View.GONE);
                projectAdapter.setProjects(projects);
            }
        });
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
