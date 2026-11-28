package com.projecthub.ui;

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
import com.projecthub.models.JoinRequest;
import com.projecthub.models.Project;
import com.projecthub.viewmodel.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExploreActivity extends AppCompatActivity {

    private DashboardViewModel dashboardViewModel;
    private ExploreAdapter exploreAdapter;
    private RecyclerView rvExploreProjects;
    private ProgressBar pbExploreLoading;
    private TextView tvEmptyExplore;
    private ImageButton btnBack, btnClearSearch;
    private TextInputEditText etSearchQuery;
    private ChipGroup chipGroupCategories;

    private List<Project> allProjectsList = new ArrayList<>();
    private String currentCategoryFilter = "ALL";
    private String currentQueryText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);

        rvExploreProjects = findViewById(R.id.rvExploreProjects);
        pbExploreLoading = findViewById(R.id.pbExploreLoading);
        tvEmptyExplore = findViewById(R.id.tvEmptyExplore);
        btnBack = findViewById(R.id.btnBack);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        etSearchQuery = findViewById(R.id.etSearchQuery);
        chipGroupCategories = findViewById(R.id.chipGroupExploreCategories);

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
                if (project.getJoinCode() != null && !project.getJoinCode().isEmpty()) {
                    new AlertDialog.Builder(ExploreActivity.this)
                        .setTitle("Join " + project.getTitle())
                        .setMessage("Join this team immediately using code: " + project.getJoinCode() + "?")
                        .setPositiveButton("Join Team", (dialog, which) -> {
                            dashboardViewModel.joinProject(userId, project.getJoinCode());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                } else {
                    showSendRequestDialog(project);
                }
            }
        });

        rvExploreProjects.setLayoutManager(new LinearLayoutManager(this));
        rvExploreProjects.setAdapter(exploreAdapter);

        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        dashboardViewModel.getPublicProjects().observe(this, projects -> {
            pbExploreLoading.setVisibility(View.GONE);
            allProjectsList = projects != null ? projects : new ArrayList<>();
            applySearchAndFilter();
        });

        dashboardViewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        setupSearchAndFilters();
    }

    private void setupSearchAndFilters() {
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
    }

    private void applySearchAndFilter() {
        if (allProjectsList == null) {
            exploreAdapter.setProjects(null);
            tvEmptyExplore.setVisibility(View.VISIBLE);
            return;
        }

        List<Project> filtered = new ArrayList<>();

        for (Project p : allProjectsList) {
            // Category check
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
                    matchesCategory = cat.contains("GENERAL") || cat.contains("ACADEMIC");
                }
            }

            if (!matchesCategory) continue;

            // Search query check
            if (!currentQueryText.isEmpty()) {
                String title = p.getTitle() != null ? p.getTitle().toLowerCase(Locale.getDefault()) : "";
                String desc = p.getDescription() != null ? p.getDescription().toLowerCase(Locale.getDefault()) : "";
                String lead = p.getLeadName() != null ? p.getLeadName().toLowerCase(Locale.getDefault()) : "";
                String code = p.getJoinCode() != null ? p.getJoinCode().toLowerCase(Locale.getDefault()) : "";
                String looking = p.getLookingFor() != null ? p.getLookingFor().toLowerCase(Locale.getDefault()) : "";
                String domain = p.getCollegeDomain() != null ? p.getCollegeDomain().toLowerCase(Locale.getDefault()) : "";

                boolean matchesQuery = title.contains(currentQueryText) ||
                                       code.contains(currentQueryText) ||
                                       lead.contains(currentQueryText) ||
                                       looking.contains(currentQueryText) ||
                                       desc.contains(currentQueryText) ||
                                       domain.contains(currentQueryText) ||
                                       cat.toLowerCase(Locale.getDefault()).contains(currentQueryText);

                if (!matchesQuery) continue;
            }

            filtered.add(p);
        }

        exploreAdapter.setProjects(filtered);
        tvEmptyExplore.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
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
