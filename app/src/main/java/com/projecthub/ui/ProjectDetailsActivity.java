package com.projecthub.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projecthub.R;
import com.projecthub.adapters.ChatMessageAdapter;
import com.projecthub.adapters.FileAdapter;
import com.projecthub.adapters.MilestoneAdapter;
import com.projecthub.adapters.TaskAdapter;
import com.projecthub.models.ChatMessage;
import com.projecthub.models.FileReference;
import com.projecthub.models.Milestone;
import com.projecthub.models.Project;
import com.projecthub.models.Task;
import com.projecthub.repository.ProjectRepository;
import com.projecthub.viewmodel.ProjectDetailsViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProjectDetailsActivity extends AppCompatActivity {

    private ProjectDetailsViewModel viewModel;
    private TaskAdapter taskAdapter;
    private MilestoneAdapter milestoneAdapter;
    private FileAdapter fileAdapter;
    private ChatMessageAdapter chatAdapter;

    private TextView tvTitle, tvDesc, tvJoinCode, tvOverallProgress, tvEmptyState;
    private TextView tvProjectLocation;
    private ProgressBar pbOverallProgress;
    private TabLayout tabLayout;

    // Tab Containers
    private View layoutKanban, layoutChat, layoutRoadmap, layoutAnalytics;
    private RecyclerView rvTasks, rvChatMessages, rvMilestones, rvFiles;
    private ExtendedFloatingActionButton fabAction;

    // Top Meeting & Share Action
    private MaterialButton btnLiveCall, btnShareCallInChat, btnShareProject;

    // Storage File Picker
    private ActivityResultLauncher<String> filePickerLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private Uri selectedFileUri = null;
    private String selectedFileName = "";
    private TextView tvSelectedFileStatus;
    private ProgressBar pbUploadStorage;
    private TextView tvUploadStoragePercent;
    private Project currentProject = null;

    // Chat Input Views
    private TextInputEditText etChatMessageInput;
    private MaterialButton btnSendChatMessage;

    // Kanban Filter Chips
    private ChipGroup chipGroupFilters;

    // Countdown / Sprint Widget Views
    private TextView tvCountdownCategory, tvCountdownDays, tvCountdownTargetDate, tvMilestonesRatio;
    private ProgressBar pbMilestonesRoadmap;

    // Analytics Views
    private TextView tvCountTodo, tvCountInProgress, tvCountInReview, tvCountDone;
    private TextView tvStatFeatures, tvStatBugs, tvStatTasks, tvStatDocs;
    private LinearLayout layoutMemberWorkload;

    private String projectId, projectTitle, projectDesc, projectCode;
    private long projectDeadline = 0;
    private int currentTab = 0; // 0=Kanban, 1=Chat, 2=Roadmap, 3=Files, 4=Analytics

    private List<Task> allTasks = new ArrayList<>();
    private List<Milestone> allMilestones = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_details);

        projectId = getIntent().getStringExtra("PROJECT_ID");
        projectTitle = getIntent().getStringExtra("PROJECT_TITLE");
        projectDesc = getIntent().getStringExtra("PROJECT_DESC");
        projectCode = getIntent().getStringExtra("PROJECT_CODE");
        projectDeadline = getIntent().getLongExtra("PROJECT_DEADLINE", 0);

        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    selectedFileName = getFileNameFromUri(uri);
                    if (tvSelectedFileStatus != null) {
                        tvSelectedFileStatus.setText("Selected: " + selectedFileName);
                    }
                }
            }
        );

        locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fine = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                Boolean coarse = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                if ((fine != null && fine) || (coarse != null && coarse)) {
                    detectAndSaveLocation();
                } else {
                    Toast.makeText(this, "Location permission denied. Enter venue manually.", Toast.LENGTH_SHORT).show();
                }
            }
        );

        initViews();
        setupTabs();
        setupRecyclerViews();
        setupFilterChips();
        setupCallButtons();

        viewModel = new ViewModelProvider(this).get(ProjectDetailsViewModel.class);
        viewModel.init(projectId);

        observeData();

        fabAction.setOnClickListener(v -> {
            if (currentTab == 0) {
                showAddTaskDialog();
            } else if (currentTab == 2) {
                showAddMilestoneDialog();
            } else if (currentTab == 3) {
                showAddFileDialog();
            }
        });

        btnSendChatMessage.setOnClickListener(v -> sendChatMessage());
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvProjectTitle);
        tvDesc = findViewById(R.id.tvProjectDesc);
        tvJoinCode = findViewById(R.id.tvProjectJoinCode);
        tvProjectLocation = findViewById(R.id.tvProjectLocation);
        tvOverallProgress = findViewById(R.id.tvOverallProgress);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        pbOverallProgress = findViewById(R.id.pbOverallProgress);
        tabLayout = findViewById(R.id.tabLayout);

        layoutKanban = findViewById(R.id.layoutKanban);
        layoutChat = findViewById(R.id.layoutChat);
        layoutRoadmap = findViewById(R.id.layoutRoadmap);
        layoutAnalytics = findViewById(R.id.layoutAnalytics);

        rvTasks = findViewById(R.id.rvTasks);
        rvChatMessages = findViewById(R.id.rvChatMessages);
        rvMilestones = findViewById(R.id.rvMilestones);
        rvFiles = findViewById(R.id.rvFiles);
        fabAction = findViewById(R.id.fabAction);

        btnLiveCall = findViewById(R.id.btnLiveCall);
        btnShareCallInChat = findViewById(R.id.btnShareCallInChat);
        btnShareProject = findViewById(R.id.btnShareProject);
        etChatMessageInput = findViewById(R.id.etChatMessageInput);
        btnSendChatMessage = findViewById(R.id.btnSendChatMessage);

        tvProjectLocation.setOnClickListener(v -> showLocationDialog());
        btnShareProject.setOnClickListener(v -> shareProjectSummary());

        chipGroupFilters = findViewById(R.id.chipGroupFilters);

        // Countdown widget
        tvCountdownCategory = findViewById(R.id.tvCountdownCategory);
        tvCountdownDays = findViewById(R.id.tvCountdownDays);
        tvCountdownTargetDate = findViewById(R.id.tvCountdownTargetDate);
        pbMilestonesRoadmap = findViewById(R.id.pbMilestonesRoadmap);
        tvMilestonesRatio = findViewById(R.id.tvMilestonesRatio);

        // Analytics
        tvCountTodo = findViewById(R.id.tvCountTodo);
        tvCountInProgress = findViewById(R.id.tvCountInProgress);
        tvCountInReview = findViewById(R.id.tvCountInReview);
        tvCountDone = findViewById(R.id.tvCountDone);

        tvStatFeatures = findViewById(R.id.tvStatFeatures);
        tvStatBugs = findViewById(R.id.tvStatBugs);
        tvStatTasks = findViewById(R.id.tvStatTasks);
        tvStatDocs = findViewById(R.id.tvStatDocs);

        layoutMemberWorkload = findViewById(R.id.layoutMemberWorkload);

        tvTitle.setText(projectTitle != null ? projectTitle : "Project");
        tvDesc.setText(projectDesc != null ? projectDesc : "");
        tvJoinCode.setText("Code: " + (projectCode != null ? projectCode : ""));
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Kanban Board"));
        tabLayout.addTab(tabLayout.newTab().setText("Team Chat"));
        tabLayout.addTab(tabLayout.newTab().setText("Roadmap & Sprints"));
        tabLayout.addTab(tabLayout.newTab().setText("Files & Code"));
        tabLayout.addTab(tabLayout.newTab().setText("Agile Analytics"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                updateTabVisibility();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateTabVisibility() {
        layoutKanban.setVisibility(currentTab == 0 ? View.VISIBLE : View.GONE);
        layoutChat.setVisibility(currentTab == 1 ? View.VISIBLE : View.GONE);
        layoutRoadmap.setVisibility(currentTab == 2 ? View.VISIBLE : View.GONE);
        rvFiles.setVisibility(currentTab == 3 ? View.VISIBLE : View.GONE);
        layoutAnalytics.setVisibility(currentTab == 4 ? View.VISIBLE : View.GONE);

        if (currentTab == 0) {
            fabAction.show();
            fabAction.setText("Add Issue");
            checkEmptyState(taskAdapter.getItemCount());
        } else if (currentTab == 1) {
            fabAction.hide();
            tvEmptyState.setVisibility(View.GONE);
        } else if (currentTab == 2) {
            fabAction.show();
            fabAction.setText("Add Milestone");
            checkEmptyState(milestoneAdapter.getItemCount());
        } else if (currentTab == 3) {
            fabAction.show();
            fabAction.setText("Share File / Code");
            checkEmptyState(fileAdapter.getItemCount());
        } else {
            fabAction.hide();
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void checkEmptyState(int count) {
        if (currentTab == 1 || currentTab == 4) {
            tvEmptyState.setVisibility(View.GONE);
            return;
        }
        tvEmptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    private void setupRecyclerViews() {
        // Kanban Tasks
        taskAdapter = new TaskAdapter(
            (task, newStatus) -> viewModel.updateTaskStatus(projectId, task.getTaskId(), newStatus),
            task -> openTaskDetails(task)
        );
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);

        // Team Chat
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = currentUser != null ? currentUser.getUid() : "";
        chatAdapter = new ChatMessageAdapter(currentUid);
        LinearLayoutManager chatLayoutManager = new LinearLayoutManager(this);
        chatLayoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(chatLayoutManager);
        rvChatMessages.setAdapter(chatAdapter);

        // Roadmap Milestones
        milestoneAdapter = new MilestoneAdapter((milestone, newStatus) -> {
            viewModel.updateMilestoneStatus(projectId, milestone.getMilestoneId(), newStatus);
        });
        rvMilestones.setLayoutManager(new LinearLayoutManager(this));
        rvMilestones.setAdapter(milestoneAdapter);

        // Files & Code Snippets
        fileAdapter = new FileAdapter(file -> {
            if (file.getUrl() != null && !file.getUrl().isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(file.getUrl()));
                try {
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Cannot open URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else if ("CODE".equalsIgnoreCase(file.getType())) {
                Toast.makeText(this, "Code snippet is ready to copy!", Toast.LENGTH_SHORT).show();
            }
        });
        rvFiles.setLayoutManager(new LinearLayoutManager(this));
        rvFiles.setAdapter(fileAdapter);
    }

    private void setupFilterChips() {
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            filterTasks(checkedIds.get(0));
        });
    }

    private void filterTasks(int checkedChipId) {
        if (allTasks == null) return;
        List<Task> filtered = new ArrayList<>();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentEmail = currentUser != null && currentUser.getEmail() != null ? 
            currentUser.getEmail().split("@")[0].toLowerCase() : "";

        for (Task t : allTasks) {
            String status = t.getStatus() != null ? t.getStatus() : "TODO";
            String issueType = t.getIssueType() != null ? t.getIssueType() : "TASK";
            String assignee = t.getAssignedToName() != null ? t.getAssignedToName().toLowerCase() : "";

            if (checkedChipId == R.id.chipFilterMyTasks) {
                if (!currentEmail.isEmpty() && (assignee.contains(currentEmail) || currentEmail.contains(assignee))) {
                    filtered.add(t);
                }
            } else if (checkedChipId == R.id.chipFilterTodo) {
                if ("TODO".equalsIgnoreCase(status)) filtered.add(t);
            } else if (checkedChipId == R.id.chipFilterProgress) {
                if ("IN_PROGRESS".equalsIgnoreCase(status)) filtered.add(t);
            } else if (checkedChipId == R.id.chipFilterReview) {
                if ("IN_REVIEW".equalsIgnoreCase(status)) filtered.add(t);
            } else if (checkedChipId == R.id.chipFilterDone) {
                if ("DONE".equalsIgnoreCase(status)) filtered.add(t);
            } else if (checkedChipId == R.id.chipFilterBugs) {
                if ("BUG".equalsIgnoreCase(issueType)) filtered.add(t);
            } else {
                filtered.add(t);
            }
        }

        taskAdapter.setTasks(filtered);
        if (currentTab == 0) checkEmptyState(filtered.size());
    }

    private void setupCallButtons() {
        View.OnClickListener callListener = v -> showLiveMeetingDialog();
        btnLiveCall.setOnClickListener(callListener);
        btnShareCallInChat.setOnClickListener(callListener);
    }

    private void showLiveMeetingDialog() {
        String safeId = projectId != null ? projectId.replaceAll("[^a-zA-Z0-9]", "") : "room";
        String meetingUrl = "https://meet.jit.si/ProjectHub_" + safeId;

        new AlertDialog.Builder(this)
            .setTitle("Live Team Video / Voice Call")
            .setMessage("Launch instant secure meeting room for project:\n" + projectTitle + "\n\nRoom Link:\n" + meetingUrl)
            .setPositiveButton("Join Call Now", (dialog, which) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(meetingUrl));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not open browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            })
            .setNeutralButton("Share Link in Chat", (dialog, which) -> {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String senderName = user != null && user.getEmail() != null ? user.getEmail().split("@")[0] : "Team Member";
                String senderId = user != null ? user.getUid() : "anon";

                ChatMessage callMsg = new ChatMessage(
                    null, projectId, senderId, senderName, user != null ? user.getEmail() : "",
                    "📞 Started team video call! Tap to join: " + meetingUrl,
                    "CALL", System.currentTimeMillis()
                );
                viewModel.sendChatMessage(projectId, callMsg);
                com.projecthub.utils.NotificationHelper.showSystemNotification(
                    ProjectDetailsActivity.this,
                    "Live Team Meeting",
                    "Meeting room opened for " + (projectTitle != null ? projectTitle : "project"),
                    projectId,
                    "CALL"
                );
                Toast.makeText(this, "Call link shared in Team Chat!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void sendChatMessage() {
        String text = etChatMessageInput.getText() != null ? etChatMessageInput.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String senderName = user != null && user.getEmail() != null ? user.getEmail().split("@")[0] : "Team Member";
        String senderId = user != null ? user.getUid() : "anon";
        String senderEmail = user != null && user.getEmail() != null ? user.getEmail() : "";

        ChatMessage message = new ChatMessage(
            null, projectId, senderId, senderName, senderEmail, text, "TEXT", System.currentTimeMillis()
        );

        viewModel.sendChatMessage(projectId, message);
        etChatMessageInput.setText("");
    }

    private void openTaskDetails(Task task) {
        Intent intent = new Intent(this, TaskDetailsActivity.class);
        intent.putExtra("PROJECT_ID", projectId);
        intent.putExtra("TASK_ID", task.getTaskId());
        intent.putExtra("TASK_TITLE", task.getTitle());
        intent.putExtra("TASK_DESC", task.getDescription());
        intent.putExtra("TASK_STATUS", task.getStatus());
        intent.putExtra("TASK_PRIORITY", task.getPriority());
        intent.putExtra("TASK_ISSUE_TYPE", task.getIssueType());
        intent.putExtra("TASK_ASSIGNEE", task.getAssignedToName());
        intent.putExtra("TASK_DUE_DATE", task.getDueDate());
        startActivity(intent);
    }

    private void observeData() {
        // Project details
        viewModel.getProject().observe(this, project -> {
            if (project != null) {
                currentProject = project;
                if (project.getTitle() != null) tvTitle.setText(project.getTitle());
                if (project.getDescription() != null) tvDesc.setText(project.getDescription());
                projectDeadline = project.getDeadline();
                updateCountdownWidget(project);

                if (project.getVenueLocation() != null && !project.getVenueLocation().isEmpty()) {
                    tvProjectLocation.setText("📍 " + project.getVenueLocation());
                } else {
                    tvProjectLocation.setText("📍 Set Campus / Lab Venue");
                }
            }
        });

        // Tasks
        viewModel.getTasks().observe(this, tasks -> {
            allTasks = tasks != null ? tasks : new ArrayList<>();
            int checkedId = chipGroupFilters.getCheckedChipId();
            if (checkedId == View.NO_ID) checkedId = R.id.chipFilterAll;
            filterTasks(checkedId);

            updateOverallProgress(allTasks);
            updateAnalytics(allTasks);
        });

        // Chat Messages
        viewModel.getChatMessages().observe(this, messages -> {
            if (messages != null) {
                chatAdapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    rvChatMessages.scrollToPosition(messages.size() - 1);
                }
            }
        });

        // Milestones
        viewModel.getMilestones().observe(this, milestones -> {
            allMilestones = milestones != null ? milestones : new ArrayList<>();
            milestoneAdapter.setMilestones(allMilestones);
            updateMilestonesRatio(allMilestones);
            if (currentTab == 2) checkEmptyState(allMilestones.size());
        });

        // Files & Code
        viewModel.getFiles().observe(this, files -> {
            fileAdapter.setFiles(files);
            if (currentTab == 3) checkEmptyState(files.size());
        });

        viewModel.getActionMessage().observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateOverallProgress(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            pbOverallProgress.setProgress(0);
            tvOverallProgress.setText("0%");
            return;
        }
        int completed = 0;
        for (Task t : tasks) {
            if ("DONE".equalsIgnoreCase(t.getStatus())) {
                completed++;
            }
        }
        int percent = (int) (((double) completed / tasks.size()) * 100);
        pbOverallProgress.setProgress(percent);
        tvOverallProgress.setText(percent + "% Completed (" + completed + "/" + tasks.size() + ")");
    }

    private void updateCountdownWidget(Project project) {
        String category = project.getCategory() != null ? project.getCategory() : "AGILE SPRINT";
        tvCountdownCategory.setText(category.toUpperCase(Locale.getDefault()) + " ROADMAP");

        long deadline = project.getDeadline();
        if (deadline > 0) {
            tvCountdownTargetDate.setText("Sprint Target: " + dateFormat.format(new Date(deadline)));

            long diff = deadline - System.currentTimeMillis();
            if (diff > 0) {
                long days = diff / (1000 * 60 * 60 * 24);
                long hours = (diff / (1000 * 60 * 60)) % 24;
                if (days > 0) {
                    tvCountdownDays.setText("⏳ " + days + "d " + hours + "h Remaining");
                } else {
                    tvCountdownDays.setText("⏳ " + hours + " Hours Left!");
                }
            } else {
                tvCountdownDays.setText("⚠️ Deadline Passed");
            }
        } else {
            tvCountdownDays.setText("⚡ Active Sprint");
            tvCountdownTargetDate.setText("Continuous Agile Delivery");
        }
    }

    private void updateMilestonesRatio(List<Milestone> milestones) {
        if (milestones == null || milestones.isEmpty()) {
            pbMilestonesRoadmap.setProgress(0);
            tvMilestonesRatio.setText("0 Milestones tracked");
            return;
        }
        int completed = 0;
        for (Milestone m : milestones) {
            if ("COMPLETED".equalsIgnoreCase(m.getStatus()) || "DONE".equalsIgnoreCase(m.getStatus())) {
                completed++;
            }
        }
        int percent = (int) (((double) completed / milestones.size()) * 100);
        pbMilestonesRoadmap.setProgress(percent);
        tvMilestonesRatio.setText(completed + " of " + milestones.size() + " Milestones Achieved (" + percent + "%)");
    }

    private void updateAnalytics(List<Task> tasks) {
        int todo = 0, progress = 0, review = 0, done = 0;
        int features = 0, bugs = 0, taskCount = 0, docs = 0;
        Map<String, Integer> memberTaskCount = new HashMap<>();

        for (Task t : tasks) {
            String status = t.getStatus() != null ? t.getStatus() : "TODO";
            if ("DONE".equalsIgnoreCase(status)) done++;
            else if ("IN_REVIEW".equalsIgnoreCase(status)) review++;
            else if ("IN_PROGRESS".equalsIgnoreCase(status)) progress++;
            else todo++;

            String type = t.getIssueType() != null ? t.getIssueType() : "TASK";
            if ("BUG".equalsIgnoreCase(type)) bugs++;
            else if ("FEATURE".equalsIgnoreCase(type)) features++;
            else if ("DOCUMENTATION".equalsIgnoreCase(type) || "DOC".equalsIgnoreCase(type)) docs++;
            else taskCount++;

            String assignee = t.getAssignedToName() != null && !t.getAssignedToName().isEmpty() ? 
                t.getAssignedToName() : "Unassigned";
            memberTaskCount.put(assignee, memberTaskCount.getOrDefault(assignee, 0) + 1);
        }

        tvCountTodo.setText(String.valueOf(todo));
        tvCountInProgress.setText(String.valueOf(progress));
        tvCountInReview.setText(String.valueOf(review));
        tvCountDone.setText(String.valueOf(done));

        tvStatFeatures.setText("✨ Features: " + features);
        tvStatBugs.setText("🐞 Bugs: " + bugs);
        tvStatTasks.setText("📋 Tasks: " + taskCount);
        tvStatDocs.setText("📄 Docs: " + docs);

        layoutMemberWorkload.removeAllViews();
        if (memberTaskCount.isEmpty()) {
            TextView tvNone = new TextView(this);
            tvNone.setText("No tasks currently assigned to members.");
            tvNone.setTextColor(getColor(R.color.text_secondary));
            tvNone.setTextSize(13);
            layoutMemberWorkload.addView(tvNone);
        } else {
            for (Map.Entry<String, Integer> entry : memberTaskCount.entrySet()) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 8, 0, 8);

                TextView tvName = new TextView(this);
                tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                tvName.setText("👤 " + entry.getKey());
                tvName.setTextColor(getColor(R.color.text_primary));
                tvName.setTextSize(13);

                TextView tvCount = new TextView(this);
                tvCount.setText(entry.getValue() + (entry.getValue() == 1 ? " task" : " tasks"));
                tvCount.setTextColor(getColor(R.color.primary));
                tvCount.setTextSize(13);

                row.addView(tvName);
                row.addView(tvCount);
                layoutMemberWorkload.addView(row);
            }
        }
    }

    private void showAddTaskDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        TextInputEditText etTitle = view.findViewById(R.id.etTaskTitle);
        TextInputEditText etDesc = view.findViewById(R.id.etTaskDesc);
        TextInputEditText etAssignee = view.findViewById(R.id.etAssignee);
        RadioGroup rgIssueType = view.findViewById(R.id.rgIssueType);
        RadioGroup rgPriority = view.findViewById(R.id.rgPriority);

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Create Issue", (dialog, which) -> {
                String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
                String assignee = etAssignee.getText() != null ? etAssignee.getText().toString().trim() : "";

                if (title.isEmpty()) {
                    Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                String issueType = "TASK";
                int checkedTypeId = rgIssueType.getCheckedRadioButtonId();
                if (checkedTypeId == R.id.rbTypeBug) issueType = "BUG";
                else if (checkedTypeId == R.id.rbTypeFeature) issueType = "FEATURE";
                else if (checkedTypeId == R.id.rbTypeDoc) issueType = "DOCUMENTATION";

                String priority = "MEDIUM";
                int checkedPriId = rgPriority.getCheckedRadioButtonId();
                if (checkedPriId == R.id.rbLow) priority = "LOW";
                else if (checkedPriId == R.id.rbHigh) priority = "HIGH";
                else if (checkedPriId == R.id.rbUrgent) priority = "URGENT";

                Task task = new Task(
                    null, projectId, null, title, desc, "TODO", priority, issueType, null, assignee, 
                    System.currentTimeMillis() + 86400000L * 7
                );
                viewModel.addTask(projectId, task);
                com.projecthub.utils.NotificationHelper.showSystemNotification(
                    ProjectDetailsActivity.this,
                    "New Task: " + title,
                    (assignee.isEmpty() ? "Unassigned" : "Assigned to " + assignee) + " • " + (projectTitle != null ? projectTitle : "Project"),
                    projectId,
                    "TASK"
                );
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showAddMilestoneDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_milestone, null);
        TextInputEditText etTitle = view.findViewById(R.id.etMilestoneTitle);
        TextInputEditText etDays = view.findViewById(R.id.etMilestoneDays);

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Add Milestone", (dialog, which) -> {
                String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                String daysStr = etDays.getText() != null ? etDays.getText().toString().trim() : "14";

                if (title.isEmpty()) {
                    Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                int days = 14;
                try { days = Integer.parseInt(daysStr); } catch (NumberFormatException ignored) {}
                long dueDate = System.currentTimeMillis() + ((long) days * 86400000L);

                Milestone milestone = new Milestone(null, projectId, title, "PENDING", dueDate);
                viewModel.addMilestone(projectId, milestone);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showAddFileDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_file, null);
        RadioGroup rgResourceType = view.findViewById(R.id.rgResourceType);
        TextInputEditText etName = view.findViewById(R.id.etFileName);
        TextInputLayout tilFileUrl = view.findViewById(R.id.tilFileUrl);
        TextInputEditText etUrl = view.findViewById(R.id.etFileUrl);
        TextInputLayout tilFileLang = view.findViewById(R.id.tilFileLang);
        TextInputEditText etLang = view.findViewById(R.id.etFileLang);
        TextInputLayout tilFileSnippet = view.findViewById(R.id.tilFileSnippet);
        TextInputEditText etSnippet = view.findViewById(R.id.etFileSnippet);

        View layoutDeviceFile = view.findViewById(R.id.layoutDeviceFile);
        MaterialButton btnPickDeviceFile = view.findViewById(R.id.btnPickDeviceFile);
        tvSelectedFileStatus = view.findViewById(R.id.tvSelectedFileStatus);
        pbUploadStorage = view.findViewById(R.id.pbUploadStorage);
        tvUploadStoragePercent = view.findViewById(R.id.tvUploadStoragePercent);

        selectedFileUri = null;
        selectedFileName = "";

        btnPickDeviceFile.setOnClickListener(v -> filePickerLauncher.launch("*/*"));

        rgResourceType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbTypeCode) {
                tilFileUrl.setVisibility(View.GONE);
                tilFileLang.setVisibility(View.VISIBLE);
                tilFileSnippet.setVisibility(View.VISIBLE);
                layoutDeviceFile.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbTypeDeviceFile) {
                tilFileUrl.setVisibility(View.GONE);
                tilFileLang.setVisibility(View.GONE);
                tilFileSnippet.setVisibility(View.GONE);
                layoutDeviceFile.setVisibility(View.VISIBLE);
            } else {
                tilFileUrl.setVisibility(View.VISIBLE);
                tilFileLang.setVisibility(View.GONE);
                tilFileSnippet.setVisibility(View.GONE);
                layoutDeviceFile.setVisibility(View.GONE);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Share", null)
            .setNegativeButton("Cancel", null)
            .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                int checkedType = rgResourceType.getCheckedRadioButtonId();

                if (checkedType == R.id.rbTypeDeviceFile) {
                    if (selectedFileUri == null) {
                        Toast.makeText(this, "Please choose a file from your device", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (name.isEmpty()) {
                        name = selectedFileName;
                        etName.setText(name);
                    }

                    pbUploadStorage.setVisibility(View.VISIBLE);
                    tvUploadStoragePercent.setVisibility(View.VISIBLE);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

                    String uploader = FirebaseAuth.getInstance().getCurrentUser() != null && 
                                      FirebaseAuth.getInstance().getCurrentUser().getEmail() != null ?
                                      FirebaseAuth.getInstance().getCurrentUser().getEmail().split("@")[0] : "Member";

                    viewModel.uploadProjectFile(projectId, selectedFileUri, name, uploader, new ProjectRepository.StorageUploadCallback() {
                        @Override
                        public void onProgress(int percent) {
                            runOnUiThread(() -> {
                                pbUploadStorage.setProgress(percent);
                                tvUploadStoragePercent.setText("Uploading: " + percent + "%");
                            });
                        }

                        @Override
                        public void onSuccess(FileReference fileReference) {
                            runOnUiThread(() -> {
                                Toast.makeText(ProjectDetailsActivity.this, "File uploaded to Storage successfully!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> {
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                pbUploadStorage.setVisibility(View.GONE);
                                tvUploadStoragePercent.setVisibility(View.GONE);
                                Toast.makeText(ProjectDetailsActivity.this, "Upload failed: " + message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                    return;
                }

                if (name.isEmpty()) {
                    Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                String type = "LINK";
                String url = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
                String snippet = "";
                String lang = "General";

                if (checkedType == R.id.rbTypeCode) {
                    type = "CODE";
                    snippet = etSnippet.getText() != null ? etSnippet.getText().toString().trim() : "";
                    lang = etLang.getText() != null ? etLang.getText().toString().trim() : "General";
                    if (snippet.isEmpty()) {
                        Toast.makeText(this, "Code snippet cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else if (checkedType == R.id.rbTypeGitHub) {
                    type = "GITHUB";
                }

                String uploader = FirebaseAuth.getInstance().getCurrentUser() != null && 
                                  FirebaseAuth.getInstance().getCurrentUser().getEmail() != null ?
                                  FirebaseAuth.getInstance().getCurrentUser().getEmail().split("@")[0] : "Member";

                FileReference ref = new FileReference(
                    null, projectId, name, url, type, snippet, lang, uploader, System.currentTimeMillis()
                );
                viewModel.addFileReference(projectId, ref);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private String getFileNameFromUri(Uri uri) {
        String name = "file_" + System.currentTimeMillis();
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return name;
    }

    private void showLocationDialog() {
        if (currentProject != null && currentProject.getVenueLocation() != null && !currentProject.getVenueLocation().isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("📍 Project Meetup Venue")
                .setMessage("Current Venue:\n" + currentProject.getVenueLocation() + 
                    (currentProject.getLatitude() != 0 ? "\n\nCoordinates: " + currentProject.getLatitude() + ", " + currentProject.getLongitude() : ""))
                .setPositiveButton("Open in Maps", (d, w) -> {
                    String mapUriStr;
                    if (currentProject.getLatitude() != 0 && currentProject.getLongitude() != 0) {
                        mapUriStr = "geo:" + currentProject.getLatitude() + "," + currentProject.getLongitude() + "?q=" + Uri.encode(currentProject.getVenueLocation());
                    } else {
                        mapUriStr = "geo:0,0?q=" + Uri.encode(currentProject.getVenueLocation());
                    }
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapUriStr));
                    try {
                        startActivity(mapIntent);
                    } catch (Exception e) {
                        Intent webMap = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(currentProject.getVenueLocation())));
                        startActivity(webMap);
                    }
                })
                .setNeutralButton("Update Venue", (d, w) -> promptForVenueInput())
                .setNegativeButton("Close", null)
                .show();
        } else {
            promptForVenueInput();
        }
    }

    private void promptForVenueInput() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("e.g. Innovation Lab Room 304, SIH Table 12");
        input.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(this)
            .setTitle("📍 Tag Project Venue")
            .setView(input)
            .setPositiveButton("Save Venue", (d, w) -> {
                String venue = input.getText().toString().trim();
                if (!venue.isEmpty()) {
                    viewModel.updateProjectLocation(projectId, venue, 0.0, 0.0);
                }
            })
            .setNeutralButton("Detect Current GPS", (d, w) -> checkAndRequestLocation())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void checkAndRequestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            detectAndSaveLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void detectAndSaveLocation() {
        try {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location location = null;
            if (locationManager != null) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                if (location == null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }

            if (location != null) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                String venueTag = "GPS: " + String.format(Locale.US, "%.4f, %.4f", lat, lng);
                viewModel.updateProjectLocation(projectId, venueTag, lat, lng);
                Toast.makeText(this, "Tagged project location: " + venueTag, Toast.LENGTH_SHORT).show();
            } else {
                viewModel.updateProjectLocation(projectId, "Campus Innovation Hub", 0.0, 0.0);
                Toast.makeText(this, "Tagged project location: Campus Innovation Hub", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission required to access device location", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareProjectSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("🚀 *ProjectHub Agile Workspace*\n");
        sb.append("📌 *Project:* ").append(projectTitle != null ? projectTitle : "Project").append("\n");
        if (projectDesc != null && !projectDesc.isEmpty()) {
            sb.append("📝 *Description:* ").append(projectDesc).append("\n");
        }
        sb.append("🔑 *Join Code:* ").append(projectCode != null ? projectCode : "").append("\n");
        if (currentProject != null && currentProject.getVenueLocation() != null && !currentProject.getVenueLocation().isEmpty()) {
            sb.append("📍 *Meetup Venue:* ").append(currentProject.getVenueLocation()).append("\n");
        }
        sb.append("\n📋 *Active Tasks (").append(allTasks.size()).append("):*\n");
        for (int i = 0; i < Math.min(allTasks.size(), 5); i++) {
            Task t = allTasks.get(i);
            sb.append("• [").append(t.getIssueType() != null ? t.getIssueType() : "TASK").append("] ")
              .append(t.getTitle()).append(" (").append(t.getStatus()).append(")\n");
        }
        if (allTasks.size() > 5) {
            sb.append("• ...and ").append(allTasks.size() - 5).append(" more tasks\n");
        }

        String safeId = projectId != null ? projectId.replaceAll("[^a-zA-Z0-9]", "") : "room";
        sb.append("\n📹 *Live Team Video Call Room:*\nhttps://meet.jit.si/ProjectHub_").append(safeId).append("\n");
        sb.append("\n_Shared via ProjectHub Collaboration App_");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "ProjectHub: " + projectTitle);
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Project with Teammates"));
    }
}
