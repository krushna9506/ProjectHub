package com.projecthub.ui;

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

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projecthub.R;
import com.projecthub.adapters.ExploreAdapter;
import com.projecthub.models.JoinRequest;
import com.projecthub.models.Project;
import com.projecthub.viewmodel.DashboardViewModel;

public class ExploreActivity extends AppCompatActivity {

    private DashboardViewModel dashboardViewModel;
    private ExploreAdapter exploreAdapter;
    private RecyclerView rvExploreProjects;
    private ProgressBar pbExploreLoading;
    private TextView tvEmptyExplore;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);

        rvExploreProjects = findViewById(R.id.rvExploreProjects);
        pbExploreLoading = findViewById(R.id.pbExploreLoading);
        tvEmptyExplore = findViewById(R.id.tvEmptyExplore);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = currentUser != null ? currentUser.getUid() : "";

        exploreAdapter = new ExploreAdapter(userId, this::showSendRequestDialog);
        rvExploreProjects.setLayoutManager(new LinearLayoutManager(this));
        rvExploreProjects.setAdapter(exploreAdapter);

        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        dashboardViewModel.getPublicProjects().observe(this, projects -> {
            pbExploreLoading.setVisibility(View.GONE);
            if (projects == null || projects.isEmpty()) {
                tvEmptyExplore.setVisibility(View.VISIBLE);
                exploreAdapter.setProjects(null);
            } else {
                tvEmptyExplore.setVisibility(View.GONE);
                exploreAdapter.setProjects(projects);
            }
        });

        dashboardViewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
                
                String applicantName = currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty() ?
                                      currentUser.getDisplayName() : currentUser.getEmail();

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

                com.projecthub.utils.NotificationHelper.dispatchNotification(
                    ExploreActivity.this,
                    project.getLeadId(),
                    "New Join Request",
                    applicantName + " requested to join " + project.getTitle(),
                    "JOIN_REQUEST",
                    project.getProjectId(),
                    project.getTitle()
                );
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
