package com.projecthub.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projecthub.R;
import com.projecthub.adapters.CalendarEventAdapter;
import com.projecthub.adapters.ChatMessageAdapter;
import com.projecthub.adapters.FileAdapter;
import com.projecthub.adapters.GanttAdapter;
import com.projecthub.adapters.GitHubItemAdapter;
import com.projecthub.adapters.MilestoneAdapter;
import com.projecthub.adapters.TaskAdapter;
import com.projecthub.adapters.WorkflowAutomationAdapter;
import com.projecthub.models.ChatMessage;
import com.projecthub.models.Conversation;
import com.projecthub.models.FileReference;
import com.projecthub.models.GitHubItem;
import com.projecthub.models.Milestone;
import com.projecthub.models.Project;
import com.projecthub.models.Task;
import com.projecthub.models.User;
import com.projecthub.models.WorkflowRule;
import com.projecthub.repository.ChatRepository;
import com.projecthub.repository.GitHubRepository;
import com.projecthub.repository.ProjectRepository;
import com.projecthub.utils.ConversationUtils;
import com.projecthub.utils.MentionParser;
import com.projecthub.utils.NotificationHelper;
import com.projecthub.utils.WorkflowAutomationEngine;
import com.projecthub.viewmodel.ProjectDetailsViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProjectDetailsActivity extends AppCompatActivity {

    private ProjectDetailsViewModel viewModel;
    private final GitHubRepository gitHubRepository = new GitHubRepository();

    private TaskAdapter taskAdapter;
    private MilestoneAdapter milestoneAdapter;
    private FileAdapter fileAdapter;
    private ChatMessageAdapter chatAdapter;
    private GanttAdapter ganttAdapter;
    private CalendarEventAdapter calendarAdapter;
    private GitHubItemAdapter gitHubAdapter;

    private TextView tvTitle, tvDesc, tvJoinCode, tvOverallProgress, tvEmptyState;
    private TextView tvProjectLocation;
    private ProgressBar pbOverallProgress;
    private TabLayout tabLayout;

    // Workspace Tool Buttons
    private MaterialButton btnTeamDirectory, btnAutomations, btnCustomStatuses;

    // Tab Containers
    private View layoutKanban, layoutChat, layoutRoadmap, layoutGitHub, layoutAnalytics;
    private RecyclerView rvTasks, rvChatMessages, rvMilestones, rvFiles, rvGanttTasks, rvCalendarEvents, rvGithubItems;
    private View hsvBoardColumns;
    private LinearLayout layoutBoardColumns;
    private ExtendedFloatingActionButton fabAction;

    // Task Search, Priority & Board/List Toggle
    private TextInputEditText etTaskSearch;
    private ImageButton btnClearTaskSearch;
    private MaterialButton btnFilterPriority, btnToggleTaskView;
    private String taskSearchQuery = "";
    private String taskPriorityFilter = "ALL";
    private boolean isBoardView = false;

    // Top Meeting & Share Action
    private MaterialButton btnLiveCall, btnShareCallInChat, btnShareProject;

    // Storage File Picker
    private ActivityResultLauncher<String> filePickerLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private Uri selectedFileUri = null;
    private String selectedFileName = "";
    private long selectedFileSize = 0L;
    private TextView tvSelectedFileStatus;
    private ProgressBar pbUploadStorage;
    private TextView tvUploadStoragePercent;
    private Project currentProject = null;

    // Chat Input, Reply & Mentions Views
    private TextInputEditText etChatMessageInput;
    private MaterialButton btnSendChatMessage, btnMentionPicker;
    private View layoutReplyPreview;
    private TextView tvReplyPreviewText;
    private ImageButton btnCancelReply;
    private ChatMessage replyingToMessage = null;

    // Kanban Filter Chips
    private ChipGroup chipGroupFilters;

    // Roadmap: Gantt, Milestones, Calendar
    private View layoutGanttSection, layoutMilestonesSection, layoutCalendarSection;
    private ChipGroup chipGroupRoadmapMode;
    private Chip chipModeGantt, chipModeMilestones, chipModeCalendar;
    private CalendarView calendarView;
    private TextView tvCalendarSelectedLabel;
    private long selectedCalendarDayMs = System.currentTimeMillis();

    // GitHub Views
    private TextView tvGithubRepoName, tvGithubRepoDesc, tvGithubRepoStats, tvGithubEmpty;
    private MaterialButton btnGithubLinkRepo, btnGithubRefresh, btnGithubToken, btnGithubOpenExternal;
    private ChipGroup chipGroupGithubFilter;
    private ProgressBar pbGithubLoading;
    private List<GitHubItem> allGithubItems = new ArrayList<>();
    private String githubFilterMode = "ALL";
    private String lastLoadedGithubUrl = "";

    // Countdown / Sprint Widget Views
    private TextView tvCountdownCategory, tvCountdownDays, tvCountdownTargetDate, tvMilestonesRatio;
    private ProgressBar pbMilestonesRoadmap;

    // Analytics Views
    private TextView tvCountTodo, tvCountInProgress, tvCountInReview, tvCountDone;
    private TextView tvStatFeatures, tvStatBugs, tvStatTasks, tvStatDocs;
    private TextView tvTotalHoursLogged, tvPhaseHoursBreakdown;
    private LinearLayout layoutMemberWorkload;

    private String projectId, projectTitle, projectDesc, projectCode;
    private long projectDeadline = 0;
    private int currentTab = 0; // 0=Tasks, 1=Chat, 2=Roadmap/Calendar, 3=Files, 4=GitHub, 5=Analytics

    private List<Task> allTasks = new ArrayList<>();
    private List<Task> currentFilteredTasks = new ArrayList<>();
    private List<Milestone> allMilestones = new ArrayList<>();
    private List<User> projectMembersList = new ArrayList<>();
    private List<WorkflowRule> automationRulesList = new ArrayList<>();
    private String lastMembersSignature = "";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat dayMatchFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

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
                    selectedFileSize = getFileSizeFromUri(uri);
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
        setupTaskSearchAndBoardControls();
        setupChatControls();
        setupCalendarControls();
        setupGitHubControls();
        setupCallButtons();

        viewModel = new ViewModelProvider(this).get(ProjectDetailsViewModel.class);
        viewModel.init(projectId);

        observeData();

        fabAction.setOnClickListener(v -> {
            if (currentTab == 0) {
                showAddTaskDialog();
            } else if (currentTab == 2) {
                if (chipModeMilestones != null && chipModeMilestones.isChecked()) {
                    showAddMilestoneDialog();
                } else {
                    showAddTaskDialog();
                }
            } else if (currentTab == 3) {
                showAddFileDialog();
            }
        });
    }

    private void initViews() {
        ImageButton btnProjectBack = findViewById(R.id.btnProjectBack);
        if (btnProjectBack != null) {
            btnProjectBack.setOnClickListener(v -> finish());
        }

        tvTitle = findViewById(R.id.tvProjectTitle);
        tvDesc = findViewById(R.id.tvProjectDesc);
        tvJoinCode = findViewById(R.id.tvProjectJoinCode);
        tvProjectLocation = findViewById(R.id.tvProjectLocation);
        tvOverallProgress = findViewById(R.id.tvOverallProgress);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        pbOverallProgress = findViewById(R.id.pbOverallProgress);
        tabLayout = findViewById(R.id.tabLayout);

        btnTeamDirectory = findViewById(R.id.btnTeamDirectory);
        btnAutomations = findViewById(R.id.btnAutomations);
        btnCustomStatuses = findViewById(R.id.btnCustomStatuses);

        btnTeamDirectory.setOnClickListener(v -> showTeamMembersDialog());
        btnAutomations.setOnClickListener(v -> showAutomationsDialog());
        btnCustomStatuses.setOnClickListener(v -> showCustomStatusesDialog());

        layoutKanban = findViewById(R.id.layoutKanban);
        layoutChat = findViewById(R.id.layoutChat);
        layoutRoadmap = findViewById(R.id.layoutRoadmap);
        layoutGitHub = findViewById(R.id.layoutGitHub);
        layoutAnalytics = findViewById(R.id.layoutAnalytics);

        rvTasks = findViewById(R.id.rvTasks);
        hsvBoardColumns = findViewById(R.id.hsvBoardColumns);
        layoutBoardColumns = findViewById(R.id.layoutBoardColumns);
        rvChatMessages = findViewById(R.id.rvChatMessages);
        rvMilestones = findViewById(R.id.rvMilestones);
        rvFiles = findViewById(R.id.rvFiles);
        fabAction = findViewById(R.id.fabAction);

        etTaskSearch = findViewById(R.id.etTaskSearch);
        btnClearTaskSearch = findViewById(R.id.btnClearTaskSearch);
        btnFilterPriority = findViewById(R.id.btnFilterPriority);
        btnToggleTaskView = findViewById(R.id.btnToggleTaskView);

        btnLiveCall = findViewById(R.id.btnLiveCall);
        btnShareCallInChat = findViewById(R.id.btnShareCallInChat);
        btnShareProject = findViewById(R.id.btnShareProject);
        etChatMessageInput = findViewById(R.id.etChatMessageInput);
        btnSendChatMessage = findViewById(R.id.btnSendChatMessage);
        btnMentionPicker = findViewById(R.id.btnMentionPicker);
        layoutReplyPreview = findViewById(R.id.layoutReplyPreview);
        tvReplyPreviewText = findViewById(R.id.tvReplyPreviewText);
        btnCancelReply = findViewById(R.id.btnCancelReply);

        tvProjectLocation.setOnClickListener(v -> showLocationDialog());
        btnShareProject.setOnClickListener(v -> shareProjectSummary());

        chipGroupFilters = findViewById(R.id.chipGroupFilters);

        // Roadmap: Gantt, Milestones, Calendar
        rvGanttTasks = findViewById(R.id.rvGanttTasks);
        layoutGanttSection = findViewById(R.id.layoutGanttSection);
        layoutMilestonesSection = findViewById(R.id.layoutMilestonesSection);
        layoutCalendarSection = findViewById(R.id.layoutCalendarSection);
        chipGroupRoadmapMode = findViewById(R.id.chipGroupRoadmapMode);
        chipModeGantt = findViewById(R.id.chipModeGantt);
        chipModeMilestones = findViewById(R.id.chipModeMilestones);
        chipModeCalendar = findViewById(R.id.chipModeCalendar);
        calendarView = findViewById(R.id.calendarView);
        tvCalendarSelectedLabel = findViewById(R.id.tvCalendarSelectedLabel);
        rvCalendarEvents = findViewById(R.id.rvCalendarEvents);

        // GitHub Tab Views
        tvGithubRepoName = findViewById(R.id.tvGithubRepoName);
        tvGithubRepoDesc = findViewById(R.id.tvGithubRepoDesc);
        tvGithubRepoStats = findViewById(R.id.tvGithubRepoStats);
        tvGithubEmpty = findViewById(R.id.tvGithubEmpty);
        btnGithubLinkRepo = findViewById(R.id.btnGithubLinkRepo);
        btnGithubRefresh = findViewById(R.id.btnGithubRefresh);
        btnGithubToken = findViewById(R.id.btnGithubToken);
        btnGithubOpenExternal = findViewById(R.id.btnGithubOpenExternal);
        chipGroupGithubFilter = findViewById(R.id.chipGroupGithubFilter);
        pbGithubLoading = findViewById(R.id.pbGithubLoading);
        rvGithubItems = findViewById(R.id.rvGithubItems);

        tvTotalHoursLogged = findViewById(R.id.tvTotalHoursLogged);
        tvPhaseHoursBreakdown = findViewById(R.id.tvPhaseHoursBreakdown);

        if (chipGroupRoadmapMode != null) {
            chipGroupRoadmapMode.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int id = checkedIds.get(0);
                boolean isGantt = (id == R.id.chipModeGantt);
                boolean isMilestones = (id == R.id.chipModeMilestones);
                boolean isCalendar = (id == R.id.chipModeCalendar);

                if (layoutGanttSection != null) layoutGanttSection.setVisibility(isGantt ? View.VISIBLE : View.GONE);
                if (layoutMilestonesSection != null) layoutMilestonesSection.setVisibility(isMilestones ? View.VISIBLE : View.GONE);
                if (layoutCalendarSection != null) layoutCalendarSection.setVisibility(isCalendar ? View.VISIBLE : View.GONE);

                if (currentTab == 2) {
                    fabAction.setText(isMilestones ? "Add Milestone" : "Add Task");
                    updateTabEmptyStateForRoadmap();
                }
            });
        }

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
        tvJoinCode.setText(projectCode != null && !projectCode.isEmpty() ? projectCode : "CODE");
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Tasks"));
        tabLayout.addTab(tabLayout.newTab().setText("Team Chat"));
        tabLayout.addTab(tabLayout.newTab().setText("Roadmap"));
        tabLayout.addTab(tabLayout.newTab().setText("Files & Code"));
        tabLayout.addTab(tabLayout.newTab().setText("GitHub"));
        tabLayout.addTab(tabLayout.newTab().setText("Analytics"));

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
        layoutGitHub.setVisibility(currentTab == 4 ? View.VISIBLE : View.GONE);
        layoutAnalytics.setVisibility(currentTab == 5 ? View.VISIBLE : View.GONE);

        if (currentTab == 0) {
            fabAction.show();
            fabAction.setText("Add Issue");
            checkEmptyState(currentFilteredTasks.size());
        } else if (currentTab == 1) {
            fabAction.hide();
            tvEmptyState.setVisibility(View.GONE);
        } else if (currentTab == 2) {
            fabAction.show();
            boolean isMilestones = (chipModeMilestones != null && chipModeMilestones.isChecked());
            fabAction.setText(isMilestones ? "Add Milestone" : "Add Task");
            updateTabEmptyStateForRoadmap();
        } else if (currentTab == 3) {
            fabAction.show();
            fabAction.setText("Share File / Code");
            checkEmptyState(fileAdapter.getItemCount());
        } else {
            fabAction.hide();
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void updateTabEmptyStateForRoadmap() {
        if (chipModeCalendar != null && chipModeCalendar.isChecked()) {
            tvEmptyState.setVisibility(View.GONE);
        } else if (chipModeMilestones != null && chipModeMilestones.isChecked()) {
            checkEmptyState(milestoneAdapter.getItemCount());
        } else {
            checkEmptyState(ganttAdapter != null ? ganttAdapter.getItemCount() : 0);
        }
    }

    private void checkEmptyState(int count) {
        if (currentTab == 1 || currentTab == 4 || currentTab == 5) {
            tvEmptyState.setVisibility(View.GONE);
            return;
        }
        tvEmptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    private void setupRecyclerViews() {
        // Tasks (List View)
        taskAdapter = new TaskAdapter(
            (task, newStatus) -> viewModel.updateTaskStatus(projectId, task.getTaskId(), newStatus),
            this::openTaskDetails
        );
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);

        // Team Chat
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = currentUser != null ? currentUser.getUid() : "";
        chatAdapter = new ChatMessageAdapter(currentUid, new ChatMessageAdapter.OnMessageActionListener() {
            @Override
            public void onMessageOptions(ChatMessage message) {
                showChatMessageOptions(message);
            }

            @Override
            public void onReactionClick(ChatMessage message, String emoji) {
                if (!currentUid.isEmpty() && message.getMessageId() != null) {
                    viewModel.toggleChatReaction(projectId, message.getMessageId(), emoji, currentUid);
                }
            }
        });
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

        // Gantt Timeline
        ganttAdapter = new GanttAdapter(this::openTaskDetails);
        rvGanttTasks.setLayoutManager(new LinearLayoutManager(this));
        rvGanttTasks.setAdapter(ganttAdapter);

        // Calendar Events
        calendarAdapter = new CalendarEventAdapter(item -> {
            if (item.task != null) {
                openTaskDetails(item.task);
            }
        });
        rvCalendarEvents.setLayoutManager(new LinearLayoutManager(this));
        rvCalendarEvents.setAdapter(calendarAdapter);

        // Files & Code Snippets
        fileAdapter = new FileAdapter(new FileAdapter.OnFileClickListener() {
            @Override
            public void onOpenClick(FileReference file) {
                if (file.getUrl() != null && !file.getUrl().isEmpty()) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(file.getUrl()));
                    try {
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        Toast.makeText(ProjectDetailsActivity.this, "Cannot open URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else if ("CODE".equalsIgnoreCase(file.getType())) {
                    Toast.makeText(ProjectDetailsActivity.this, "Code snippet is ready to copy!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDeleteClick(FileReference file) {
                new AlertDialog.Builder(ProjectDetailsActivity.this)
                    .setTitle("Delete Resource?")
                    .setMessage("Remove \"" + file.getName() + "\" from project files?")
                    .setPositiveButton("Delete", (d, w) -> viewModel.deleteFileReference(projectId, file))
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });
        rvFiles.setLayoutManager(new LinearLayoutManager(this));
        rvFiles.setAdapter(fileAdapter);

        // GitHub Items
        gitHubAdapter = new GitHubItemAdapter(new GitHubItemAdapter.OnGitHubItemActionListener() {
            @Override
            public void onOpen(GitHubItem item) {
                if (item.getHtmlUrl() != null && !item.getHtmlUrl().isEmpty()) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.getHtmlUrl())));
                }
            }

            @Override
            public void onImportAsTask(GitHubItem item) {
                String prefix = item.isPullRequest() ? "[PR #" : "[GH #";
                String issueType = item.isPullRequest() ? "FEATURE" : "BUG";
                String status = "closed".equalsIgnoreCase(item.getState()) ? "DONE" : "TODO";
                Task imported = new Task(
                    null,
                    projectId,
                    null,
                    prefix + item.getNumber() + "] " + item.getTitle(),
                    "Imported from GitHub: " + item.getHtmlUrl(),
                    status,
                    "MEDIUM",
                    issueType,
                    null,
                    "",
                    System.currentTimeMillis() + 7L * 86400000L
                );
                imported.setTaskKey("GH-" + item.getNumber());
                imported.setGithubIssueNumber(item.getNumber());
                imported.setGithubIssueUrl(item.getHtmlUrl());
                viewModel.addTask(projectId, imported);
            }
        });
        rvGithubItems.setLayoutManager(new LinearLayoutManager(this));
        rvGithubItems.setAdapter(gitHubAdapter);
    }

    private void setupTaskSearchAndBoardControls() {
        if (etTaskSearch != null) {
            etTaskSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    taskSearchQuery = s != null ? s.toString().trim().toLowerCase(Locale.getDefault()) : "";
                    if (btnClearTaskSearch != null) {
                        btnClearTaskSearch.setVisibility(taskSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    applyCurrentTaskFilters();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        if (btnClearTaskSearch != null) {
            btnClearTaskSearch.setOnClickListener(v -> {
                if (etTaskSearch != null) etTaskSearch.setText("");
            });
        }

        if (btnFilterPriority != null) {
            btnFilterPriority.setOnClickListener(v -> {
                final String[] options = {"ALL", "URGENT", "HIGH", "MEDIUM", "LOW"};
                new AlertDialog.Builder(this)
                    .setTitle("Filter by Priority")
                    .setItems(options, (d, which) -> {
                        taskPriorityFilter = options[which];
                        btnFilterPriority.setText("Pri: " + taskPriorityFilter);
                        applyCurrentTaskFilters();
                    })
                    .show();
            });
        }

        if (btnToggleTaskView != null) {
            btnToggleTaskView.setOnClickListener(v -> {
                isBoardView = !isBoardView;
                btnToggleTaskView.setText(isBoardView ? "List" : "Board");
                rvTasks.setVisibility(isBoardView ? View.GONE : View.VISIBLE);
                hsvBoardColumns.setVisibility(isBoardView ? View.VISIBLE : View.GONE);
                if (isBoardView) {
                    renderBoardColumns(currentFilteredTasks);
                }
            });
        }
    }

    private void setupChatControls() {
        btnSendChatMessage.setOnClickListener(v -> sendChatMessage());
        if (btnCancelReply != null) {
            btnCancelReply.setOnClickListener(v -> clearReplyTarget());
        }
        if (btnMentionPicker != null) {
            btnMentionPicker.setOnClickListener(v -> showMentionMemberPicker());
        }
    }

    private void showMentionMemberPicker() {
        if (projectMembersList == null || projectMembersList.isEmpty()) {
            Toast.makeText(this, "No teammates loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[projectMembersList.size()];
        for (int i = 0; i < projectMembersList.size(); i++) {
            User u = projectMembersList.get(i);
            labels[i] = (u.getName() != null ? u.getName() : u.getEmail()) + " (" + MentionParser.formatMentionHandle(u) + ")";
        }
        new AlertDialog.Builder(this)
            .setTitle("Mention Teammate")
            .setItems(labels, (d, which) -> {
                String handle = MentionParser.formatMentionHandle(projectMembersList.get(which));
                String current = etChatMessageInput.getText() != null ? etChatMessageInput.getText().toString() : "";
                String next = current.isEmpty() ? handle + " " : current + (current.endsWith(" ") ? "" : " ") + handle + " ";
                etChatMessageInput.setText(next);
                etChatMessageInput.setSelection(next.length());
            })
            .show();
    }

    private void showChatMessageOptions(ChatMessage msg) {
        if (msg == null || msg.getMessageId() == null) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "";

        final String[] actions = {"Reply to message", "👍 React", "❤️ React", "🔥 React", "✅ React", "🎉 React", "👀 React"};
        new AlertDialog.Builder(this)
            .setTitle("Message Actions")
            .setItems(actions, (d, which) -> {
                if (which == 0) {
                    replyingToMessage = msg;
                    if (layoutReplyPreview != null && tvReplyPreviewText != null) {
                        layoutReplyPreview.setVisibility(View.VISIBLE);
                        tvReplyPreviewText.setText("Replying to " + msg.getSenderName() + ": " + msg.getMessage());
                    }
                } else {
                    String emoji = actions[which].split(" ")[0];
                    if (!uid.isEmpty()) {
                        viewModel.toggleChatReaction(projectId, msg.getMessageId(), emoji, uid);
                    }
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

    private void setupCalendarControls() {
        if (calendarView != null) {
            calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, dayOfMonth, 12, 0, 0);
                selectedCalendarDayMs = cal.getTimeInMillis();
                refreshCalendarSchedule();
            });
        }
    }

    private void refreshCalendarSchedule() {
        if (calendarAdapter == null || tvCalendarSelectedLabel == null) return;
        String targetDay = dayMatchFormat.format(new Date(selectedCalendarDayMs));
        List<CalendarEventAdapter.CalendarEventItem> dayMatches = new ArrayList<>();
        List<CalendarEventAdapter.CalendarEventItem> allDated = new ArrayList<>();

        for (Task t : allTasks) {
            if (t.getDueDate() > 0) {
                CalendarEventAdapter.CalendarEventItem item = new CalendarEventAdapter.CalendarEventItem(
                    "TASK", t.getTitle(), t.getStatus(), t.getDueDate(), t);
                allDated.add(item);
                if (targetDay.equals(dayMatchFormat.format(new Date(t.getDueDate())))) {
                    dayMatches.add(item);
                }
            }
        }
        for (Milestone m : allMilestones) {
            if (m.getDueDate() > 0) {
                CalendarEventAdapter.CalendarEventItem item = new CalendarEventAdapter.CalendarEventItem(
                    "MILESTONE", m.getTitle(), m.getStatus(), m.getDueDate(), null);
                allDated.add(item);
                if (targetDay.equals(dayMatchFormat.format(new Date(m.getDueDate())))) {
                    dayMatches.add(item);
                }
            }
        }

        if (!dayMatches.isEmpty()) {
            tvCalendarSelectedLabel.setText("Due on " + dateFormat.format(new Date(selectedCalendarDayMs))
                    + " (" + dayMatches.size() + ")");
            calendarAdapter.setItems(dayMatches);
        } else {
            tvCalendarSelectedLabel.setText("No deadlines on " + dateFormat.format(new Date(selectedCalendarDayMs))
                    + " • Showing all scheduled (" + allDated.size() + ")");
            calendarAdapter.setItems(allDated);
        }
    }

    private void setupGitHubControls() {
        btnGithubLinkRepo.setOnClickListener(v -> showLinkGithubRepoDialog());
        btnGithubRefresh.setOnClickListener(v -> {
            String url = currentProject != null ? currentProject.getGithubRepoUrl() : "";
            if (url == null || url.trim().isEmpty()) {
                showLinkGithubRepoDialog();
            } else {
                loadGitHubRepository(url);
            }
        });
        btnGithubToken.setOnClickListener(v -> showGitHubTokenDialog());
        btnGithubOpenExternal.setOnClickListener(v -> {
            String url = currentProject != null ? currentProject.getGithubRepoUrl() : "";
            if (url != null && !url.isEmpty()) {
                String normalized = url.startsWith("http") ? url : "https://github.com/" + GitHubRepository.parseOwnerRepo(url);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(normalized)));
            }
        });

        if (chipGroupGithubFilter != null) {
            chipGroupGithubFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int id = checkedIds.get(0);
                if (id == R.id.chipGithubIssues) githubFilterMode = "ISSUES";
                else if (id == R.id.chipGithubPrs) githubFilterMode = "PRS";
                else if (id == R.id.chipGithubOpen) githubFilterMode = "OPEN";
                else githubFilterMode = "ALL";
                applyGithubFilters();
            });
        }
    }

    private void showLinkGithubRepoDialog() {
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("https://github.com/owner/repo or owner/repo");
        if (currentProject != null && currentProject.getGithubRepoUrl() != null) {
            input.setText(currentProject.getGithubRepoUrl());
        }

        new AlertDialog.Builder(this)
            .setTitle("Link GitHub Repository")
            .setMessage("Enter a public GitHub repo (or private repo with a saved token):")
            .setView(input)
            .setPositiveButton("Save & Sync", (d, w) -> {
                String url = input.getText() != null ? input.getText().toString().trim() : "";
                if (url.isEmpty()) return;
                viewModel.updateProjectGithubUrl(projectId, url);
                loadGitHubRepository(url);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showGitHubTokenDialog() {
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("Personal Access Token (optional for higher rate limit / private repos)");
        input.setText(GitHubRepository.getToken(this));

        new AlertDialog.Builder(this)
            .setTitle("GitHub API Token")
            .setView(input)
            .setPositiveButton("Save Token", (d, w) -> {
                String token = input.getText() != null ? input.getText().toString().trim() : "";
                GitHubRepository.saveToken(this, token);
                Toast.makeText(this, token.isEmpty() ? "Token cleared" : "GitHub token saved locally", Toast.LENGTH_SHORT).show();
                if (currentProject != null && currentProject.getGithubRepoUrl() != null && !currentProject.getGithubRepoUrl().isEmpty()) {
                    loadGitHubRepository(currentProject.getGithubRepoUrl());
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void loadGitHubRepository(String urlOrSlug) {
        if (urlOrSlug == null || urlOrSlug.trim().isEmpty()) return;
        lastLoadedGithubUrl = urlOrSlug;
        pbGithubLoading.setVisibility(View.VISIBLE);
        tvGithubEmpty.setVisibility(View.GONE);
        String token = GitHubRepository.getToken(this);

        gitHubRepository.fetchRepositoryDetails(urlOrSlug, token, new GitHubRepository.GitHubCallback() {
            @Override
            public void onSuccess(GitHubRepository.RepoOverview overview) {
                pbGithubLoading.setVisibility(View.GONE);
                tvGithubRepoName.setText(overview.fullName);
                tvGithubRepoDesc.setText(overview.description != null && !overview.description.isEmpty()
                        ? overview.description : "Connected GitHub Repository");
                tvGithubRepoStats.setVisibility(View.VISIBLE);
                tvGithubRepoStats.setText(overview.stars + " stars • " + overview.items.size() + " recent issues & PRs");
                btnGithubOpenExternal.setVisibility(View.VISIBLE);
                allGithubItems = overview.items != null ? overview.items : new ArrayList<>();
                applyGithubFilters();
            }

            @Override
            public void onError(String message) {
                pbGithubLoading.setVisibility(View.GONE);
                tvGithubEmpty.setVisibility(View.VISIBLE);
                tvGithubEmpty.setText("Could not load GitHub data: " + message);
                allGithubItems = new ArrayList<>();
                gitHubAdapter.setItems(allGithubItems);
            }
        });
    }

    private void applyGithubFilters() {
        List<GitHubItem> filtered = new ArrayList<>();
        for (GitHubItem item : allGithubItems) {
            if ("ISSUES".equals(githubFilterMode) && item.isPullRequest()) continue;
            if ("PRS".equals(githubFilterMode) && !item.isPullRequest()) continue;
            if ("OPEN".equals(githubFilterMode) && !"open".equalsIgnoreCase(item.getState())) continue;
            filtered.add(item);
        }
        gitHubAdapter.setItems(filtered);
        tvGithubEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        if (filtered.isEmpty() && !allGithubItems.isEmpty()) {
            tvGithubEmpty.setText("No matching GitHub items for the selected filter.");
        }
    }

    private void setupFilterChips() {
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            filterTasks(checkedIds.get(0));
        });
    }

    private void applyCurrentTaskFilters() {
        int checkedId = chipGroupFilters.getCheckedChipId();
        if (checkedId == View.NO_ID) checkedId = R.id.chipFilterAll;
        filterTasks(checkedId);
    }

    private void filterTasks(int checkedChipId) {
        if (allTasks == null) return;
        List<Task> filtered = new ArrayList<>();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = currentUser != null ? currentUser.getUid() : "";
        String currentEmail = currentUser != null && currentUser.getEmail() != null
                ? currentUser.getEmail().split("@")[0].toLowerCase(Locale.getDefault()) : "";

        for (Task t : allTasks) {
            String status = t.getStatus() != null ? t.getStatus() : "TODO";
            String issueType = t.getIssueType() != null ? t.getIssueType() : "TASK";
            String assigneeId = t.getAssignedTo() != null ? t.getAssignedTo() : "";
            String assigneeName = t.getAssignedToName() != null ? t.getAssignedToName().toLowerCase(Locale.getDefault()) : "";

            boolean matchesChip = true;
            if (checkedChipId == R.id.chipFilterMyTasks) {
                boolean idMatch = !currentUid.isEmpty() && currentUid.equals(assigneeId);
                boolean nameMatch = !currentEmail.isEmpty() && !assigneeName.isEmpty()
                        && (assigneeName.contains(currentEmail) || currentEmail.contains(assigneeName));
                matchesChip = idMatch || nameMatch;
            } else if (checkedChipId == R.id.chipFilterTodo) {
                matchesChip = "TODO".equalsIgnoreCase(status);
            } else if (checkedChipId == R.id.chipFilterProgress) {
                matchesChip = "IN_PROGRESS".equalsIgnoreCase(status);
            } else if (checkedChipId == R.id.chipFilterReview) {
                matchesChip = "IN_REVIEW".equalsIgnoreCase(status);
            } else if (checkedChipId == R.id.chipFilterDone) {
                matchesChip = "DONE".equalsIgnoreCase(status);
            } else if (checkedChipId == R.id.chipFilterBugs) {
                matchesChip = "BUG".equalsIgnoreCase(issueType);
            }
            if (!matchesChip) continue;

            if (!"ALL".equalsIgnoreCase(taskPriorityFilter)) {
                String pri = t.getPriority() != null ? t.getPriority() : "MEDIUM";
                if (!taskPriorityFilter.equalsIgnoreCase(pri)) continue;
            }

            if (!taskSearchQuery.isEmpty()) {
                String title = t.getTitle() != null ? t.getTitle().toLowerCase(Locale.getDefault()) : "";
                String desc = t.getDescription() != null ? t.getDescription().toLowerCase(Locale.getDefault()) : "";
                String key = t.getTaskKey() != null ? t.getTaskKey().toLowerCase(Locale.getDefault()) : "";
                boolean matchSearch = title.contains(taskSearchQuery)
                        || desc.contains(taskSearchQuery)
                        || key.contains(taskSearchQuery)
                        || assigneeName.contains(taskSearchQuery);
                if (!matchSearch) continue;
            }

            filtered.add(t);
        }

        currentFilteredTasks = filtered;
        taskAdapter.setTasks(filtered);
        if (isBoardView) {
            renderBoardColumns(filtered);
        }
        if (currentTab == 0) checkEmptyState(filtered.size());
    }

    private void renderBoardColumns(List<Task> tasks) {
        if (layoutBoardColumns == null) return;
        layoutBoardColumns.removeAllViews();

        List<String> statuses = currentProject != null
                ? currentProject.getEffectiveStatuses()
                : Project.defaultStatuses();

        float density = getResources().getDisplayMetrics().density;
        int colWidth = (int) (250 * density);
        int marginPx = (int) (8 * density);

        for (String status : statuses) {
            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            android.graphics.drawable.GradientDrawable colBg = new android.graphics.drawable.GradientDrawable();
            colBg.setColor(getColor(R.color.surface_variant));
            colBg.setCornerRadius(14f * density);
            colBg.setStroke((int) density, getColor(R.color.divider));
            column.setBackground(colBg);
            column.setPadding(marginPx, marginPx, marginPx, marginPx);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                colWidth, LinearLayout.LayoutParams.MATCH_PARENT);
            colParams.setMargins(marginPx / 2, marginPx, marginPx / 2, marginPx);
            column.setLayoutParams(colParams);

            List<Task> colTasks = new ArrayList<>();
            for (Task t : tasks) {
                if (status.equalsIgnoreCase(t.getStatus())) {
                    colTasks.add(t);
                }
            }

            TextView tvColHeader = new TextView(this);
            tvColHeader.setText(status.replace('_', ' ') + " (" + colTasks.size() + ")");
            tvColHeader.setTextSize(12.5f);
            tvColHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            int headerBgRes = R.color.primary_light;
            int headerTextRes = R.color.on_primary_container;
            if ("DONE".equalsIgnoreCase(status)) {
                headerBgRes = R.color.status_done_bg;
                headerTextRes = R.color.status_done_text;
            } else if ("IN_REVIEW".equalsIgnoreCase(status)) {
                headerBgRes = R.color.status_review_bg;
                headerTextRes = R.color.status_review_text;
            } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
                headerBgRes = R.color.status_progress_bg;
                headerTextRes = R.color.status_progress_text;
            } else if ("BLOCKED".equalsIgnoreCase(status)) {
                headerBgRes = R.color.status_blocked_bg;
                headerTextRes = R.color.status_blocked_text;
            } else if ("TODO".equalsIgnoreCase(status)) {
                headerBgRes = R.color.status_todo_bg;
                headerTextRes = R.color.status_todo_text;
            }
            android.graphics.drawable.GradientDrawable headerBg = new android.graphics.drawable.GradientDrawable();
            headerBg.setColor(getColor(headerBgRes));
            headerBg.setCornerRadius(10f * density);
            tvColHeader.setBackground(headerBg);
            tvColHeader.setTextColor(getColor(headerTextRes));
            int padH = (int) (10 * density);
            int padV = (int) (5 * density);
            tvColHeader.setPadding(padH, padV, padH, padV);
            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            headerParams.setMargins(0, 0, 0, marginPx);
            tvColHeader.setLayoutParams(headerParams);
            column.addView(tvColHeader);

            for (Task t : colTasks) {
                MaterialCardView card = new MaterialCardView(this);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, 0, 0, marginPx);
                card.setLayoutParams(cardParams);
                card.setRadius(12f * density);
                card.setCardElevation(0f);
                card.setCardBackgroundColor(getColor(R.color.surface));
                card.setStrokeWidth((int) density);
                card.setStrokeColor(getColor(R.color.divider));

                LinearLayout inner = new LinearLayout(this);
                inner.setOrientation(LinearLayout.VERTICAL);
                inner.setPadding(marginPx + 4, marginPx + 4, marginPx + 4, marginPx + 4);

                TextView tvTaskTitle = new TextView(this);
                tvTaskTitle.setText((t.getTaskKey() != null ? t.getTaskKey() + " • " : "") + t.getTitle());
                tvTaskTitle.setTextSize(13.5f);
                tvTaskTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                tvTaskTitle.setTextColor(getColor(R.color.text_primary));
                inner.addView(tvTaskTitle);

                TextView tvTaskMeta = new TextView(this);
                String who = t.getAssignedToName() != null && !t.getAssignedToName().isEmpty() ? t.getAssignedToName() : "Unassigned";
                String subInfo = t.getTotalSubtasksCount() > 0
                        ? " • " + t.getCompletedSubtasksCount() + "/" + t.getTotalSubtasksCount() + " subtasks" : "";
                tvTaskMeta.setText(t.getPriority() + " • " + who + subInfo);
                tvTaskMeta.setTextSize(11.5f);
                tvTaskMeta.setTextColor(getColor(R.color.text_secondary));
                tvTaskMeta.setPadding(0, 6, 0, 0);
                inner.addView(tvTaskMeta);

                card.addView(inner);
                card.setOnClickListener(v -> openTaskDetails(t));
                card.setOnLongClickListener(v -> {
                    String[] statusArr = statuses.toArray(new String[0]);
                    new AlertDialog.Builder(this)
                        .setTitle("Move \"" + t.getTitle() + "\" to:")
                        .setItems(statusArr, (d, which) ->
                            viewModel.updateTaskStatus(projectId, t.getTaskId(), statusArr[which]))
                        .show();
                    return true;
                });
                column.addView(card);
            }

            layoutBoardColumns.addView(column);
        }
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
                String senderName = resolveCurrentUserDisplayName(user);
                String senderId = user != null ? user.getUid() : "anon";

                ChatMessage callMsg = new ChatMessage(
                    null, projectId, senderId, senderName, user != null ? user.getEmail() : "",
                    "📞 Started team video call! Tap to join: " + meetingUrl,
                    "CALL", System.currentTimeMillis()
                );
                viewModel.sendChatMessage(projectId, callMsg, projectTitle);
                NotificationHelper.showSystemNotification(
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

    private String resolveCurrentUserDisplayName(FirebaseUser fbUser) {
        if (fbUser == null) return "Team Member";
        for (User member : projectMembersList) {
            if (fbUser.getUid().equals(member.getUserId()) && member.getName() != null && !member.getName().isEmpty()) {
                return member.getName();
            }
        }
        if (fbUser.getDisplayName() != null && !fbUser.getDisplayName().isEmpty()) {
            return fbUser.getDisplayName();
        }
        return fbUser.getEmail() != null ? fbUser.getEmail().split("@")[0] : "Team Member";
    }

    private void sendChatMessage() {
        String text = etChatMessageInput.getText() != null ? etChatMessageInput.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String senderName = resolveCurrentUserDisplayName(user);
        String senderId = user != null ? user.getUid() : "anon";
        String senderEmail = user != null && user.getEmail() != null ? user.getEmail() : "";

        ChatMessage message = new ChatMessage(
            null, projectId, senderId, senderName, senderEmail, text, "TEXT", System.currentTimeMillis()
        );

        if (replyingToMessage != null) {
            message.setReplyToMessageId(replyingToMessage.getMessageId());
            message.setReplyToSenderName(replyingToMessage.getSenderName());
            message.setReplyToPreview(replyingToMessage.getMessage());
        }

        List<User> mentionedUsers = MentionParser.resolveMentionedUsers(text, projectMembersList);
        List<String> mentionedIds = new ArrayList<>();
        for (User u : mentionedUsers) {
            if (u.getUserId() != null) mentionedIds.add(u.getUserId());
        }
        message.setMentionedUserIds(mentionedIds);

        viewModel.sendChatMessage(projectId, message, currentProject != null ? currentProject.getTitle() : projectTitle);
        etChatMessageInput.setText("");
        clearReplyTarget();
    }

    private void openTaskDetails(Task task) {
        Intent intent = new Intent(this, TaskDetailsActivity.class);
        intent.putExtra("PROJECT_ID", projectId);
        intent.putExtra("PROJECT_TITLE", currentProject != null ? currentProject.getTitle() : projectTitle);
        intent.putExtra("TASK_ID", task.getTaskId());
        intent.putExtra("TASK_TITLE", task.getTitle());
        intent.putExtra("TASK_DESC", task.getDescription());
        intent.putExtra("TASK_STATUS", task.getStatus());
        intent.putExtra("TASK_PRIORITY", task.getPriority());
        intent.putExtra("TASK_ISSUE_TYPE", task.getIssueType());
        intent.putExtra("TASK_ASSIGNEE", task.getAssignedToName());
        intent.putExtra("TASK_ASSIGNEE_ID", task.getAssignedTo());
        intent.putExtra("TASK_DUE_DATE", task.getDueDate());
        intent.putExtra("TASK_PHASE", task.getPhase());
        intent.putExtra("TASK_TIME_SPENT", task.getTimeSpentHours());
        startActivity(intent);
    }

    private void observeData() {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = fbUser != null ? fbUser.getUid() : "";

        // Project details
        viewModel.getProject().observe(this, project -> {
            if (project != null) {
                currentProject = project;
                if (project.getTitle() != null) {
                    projectTitle = project.getTitle();
                    tvTitle.setText(projectTitle);
                }
                if (project.getDescription() != null) {
                    projectDesc = project.getDescription();
                    tvDesc.setText(projectDesc);
                }
                projectDeadline = project.getDeadline();
                updateCountdownWidget(project);

                taskAdapter.setAvailableStatuses(project.getEffectiveStatuses());
                fileAdapter.setCurrentUserContext(currentUid, project.getLeadId());
                boolean isLead = !currentUid.isEmpty() && currentUid.equals(project.getLeadId());
                viewModel.ensureFilesLoadedForRole(projectId, currentUid, isLead);

                if (project.getVenueLocation() != null && !project.getVenueLocation().isEmpty()) {
                    tvProjectLocation.setText("Venue: " + project.getVenueLocation());
                } else {
                    tvProjectLocation.setText("Set campus venue");
                }

                if (project.getMembers() != null) {
                    btnTeamDirectory.setText("Team (" + project.getMembers().size() + ")");
                    List<String> memberIds = new ArrayList<>(project.getMembers().keySet());
                    String sig = TextUtils.join(",", memberIds);
                    if (!sig.equals(lastMembersSignature)) {
                        lastMembersSignature = sig;
                        viewModel.loadMembers(memberIds);
                    }
                }

                String ghUrl = project.getGithubRepoUrl();
                if (ghUrl != null && !ghUrl.trim().isEmpty() && !ghUrl.equals(lastLoadedGithubUrl)) {
                    loadGitHubRepository(ghUrl);
                }

                if (isBoardView) {
                    renderBoardColumns(currentFilteredTasks);
                }
            }
        });

        viewModel.getProjectMembers().observe(this, members -> {
            projectMembersList = members != null ? members : new ArrayList<>();
        });

        viewModel.getAutomations().observe(this, rules -> {
            automationRulesList = rules != null ? rules : new ArrayList<>();
        });

        // Tasks
        viewModel.getTasks().observe(this, tasks -> {
            allTasks = tasks != null ? tasks : new ArrayList<>();
            applyCurrentTaskFilters();

            if (ganttAdapter != null) {
                ganttAdapter.setTasks(allTasks);
            }

            updateOverallProgress(allTasks);
            updateAnalytics(allTasks);
            refreshCalendarSchedule();
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
            refreshCalendarSchedule();
            if (currentTab == 2) updateTabEmptyStateForRoadmap();
        });

        // Files & Code
        viewModel.getFiles().observe(this, files -> {
            fileAdapter.setFiles(files);
            if (currentTab == 3) checkEmptyState(files.size());
        });

        viewModel.getActionMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void showTeamMembersDialog() {
        Map<String, String> rolesMap = currentProject != null && currentProject.getMembers() != null
                ? currentProject.getMembers() : new HashMap<>();

        if (projectMembersList.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("Project Team & Mentors (0)")
                .setMessage("Loading member profiles...")
                .setPositiveButton("Close", null)
                .show();
            return;
        }

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        String myUid = fbUser != null ? fbUser.getUid() : "";
        String[] labels = new String[projectMembersList.size()];
        for (int i = 0; i < projectMembersList.size(); i++) {
            User u = projectMembersList.get(i);
            String projRole = rolesMap.getOrDefault(u.getUserId(), "MEMBER");
            String verified = u.isCampusVerified() ? " ✓" : "";
            String name = u.getName() != null && !u.getName().isEmpty() ? u.getName() : u.getEmail();
            String actionHint = u.getUserId().equals(myUid) ? " (You)" : " — Tap to Message";
            String dept = u.getDepartment() != null && !u.getDepartment().isEmpty() ? " • " + u.getDepartment() : "";
            labels[i] = name + verified + " [" + projRole + "]" + dept + actionHint;
        }

        new AlertDialog.Builder(this)
            .setTitle("Project Team & Mentors (" + projectMembersList.size() + ")")
            .setItems(labels, (d, which) -> {
                User picked = projectMembersList.get(which);
                if (picked.getUserId() == null || picked.getUserId().equals(myUid) || fbUser == null) return;
                User me = new User(
                    myUid,
                    resolveCurrentUserDisplayName(fbUser),
                    fbUser.getEmail() != null ? fbUser.getEmail() : "",
                    "",
                    ""
                );
                new ChatRepository().getOrCreateDirectChat(me, picked, new ChatRepository.ConversationCallback() {
                    @Override
                    public void onSuccess(Conversation conversation) {
                        Intent intent = new Intent(ProjectDetailsActivity.this, ChatRoomActivity.class);
                        intent.putExtra("CONVERSATION_ID", conversation.getConversationId());
                        intent.putExtra("CONVERSATION_TITLE",
                            ConversationUtils.resolveDisplayTitle(conversation, myUid));
                        startActivity(intent);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(ProjectDetailsActivity.this, "Error opening chat: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setPositiveButton("Close", null)
            .show();
    }

    private void showCustomStatusesDialog() {
        List<String> current = currentProject != null
                ? currentProject.getEffectiveStatuses() : Project.defaultStatuses();
        final TextInputEditText input = new TextInputEditText(this);
        input.setText(TextUtils.join(", ", current));
        input.setHint("Comma-separated statuses (e.g. TODO, IN_PROGRESS, IN_REVIEW, DONE)");

        new AlertDialog.Builder(this)
            .setTitle("Customize Workflow Statuses")
            .setMessage("Enter status names separated by commas:")
            .setView(input)
            .setPositiveButton("Save Statuses", (d, w) -> {
                String raw = input.getText() != null ? input.getText().toString().trim() : "";
                List<String> next = new ArrayList<>();
                for (String s : raw.split(",")) {
                    String clean = s.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
                    if (!clean.isEmpty() && !next.contains(clean)) {
                        next.add(clean);
                    }
                }
                if (next.isEmpty()) {
                    Toast.makeText(this, "Enter at least one status", Toast.LENGTH_SHORT).show();
                    return;
                }
                viewModel.updateProjectCustomStatuses(projectId, next);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showAutomationsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_automations, null);
        RecyclerView rvRules = view.findViewById(R.id.rvAutomationRules);
        TextInputEditText etRuleName = view.findViewById(R.id.etRuleName);
        Spinner spinnerTrigger = view.findViewById(R.id.spinnerTriggerType);
        TextInputEditText etTriggerVal = view.findViewById(R.id.etTriggerValue);
        Spinner spinnerAction = view.findViewById(R.id.spinnerActionType);
        TextInputEditText etActionVal = view.findViewById(R.id.etActionValue);
        MaterialButton btnAddRule = view.findViewById(R.id.btnAddAutomationRule);

        WorkflowAutomationAdapter adapter = new WorkflowAutomationAdapter(new WorkflowAutomationAdapter.OnRuleActionListener() {
            @Override
            public void onToggle(WorkflowRule rule, boolean enabled) {
                viewModel.toggleAutomationRule(projectId, rule.getRuleId(), enabled);
            }

            @Override
            public void onDelete(WorkflowRule rule) {
                viewModel.deleteAutomationRule(projectId, rule.getRuleId());
            }
        });
        rvRules.setLayoutManager(new LinearLayoutManager(this));
        rvRules.setAdapter(adapter);
        adapter.setRules(automationRulesList);
        viewModel.getAutomations().observe(this, rules -> {
            if (rules != null) adapter.setRules(rules);
        });

        final String[] triggerTypes = {
            WorkflowAutomationEngine.TRIGGER_TASK_ASSIGNED,
            WorkflowAutomationEngine.TRIGGER_TASK_STATUS_CHANGED,
            WorkflowAutomationEngine.TRIGGER_ALL_SUBTASKS_COMPLETED,
            WorkflowAutomationEngine.TRIGGER_MILESTONE_COMPLETED
        };
        spinnerTrigger.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, triggerTypes));

        final String[] actionTypes = {
            WorkflowAutomationEngine.ACTION_NOTIFY_ASSIGNEE,
            WorkflowAutomationEngine.ACTION_NOTIFY_LEAD,
            WorkflowAutomationEngine.ACTION_POST_CHAT_UPDATE,
            WorkflowAutomationEngine.ACTION_SET_TASK_STATUS,
            WorkflowAutomationEngine.ACTION_SET_TASK_PHASE
        };
        spinnerAction.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, actionTypes));

        btnAddRule.setOnClickListener(v -> {
            String name = etRuleName.getText() != null ? etRuleName.getText().toString().trim() : "";
            String trigType = triggerTypes[spinnerTrigger.getSelectedItemPosition()];
            String trigVal = etTriggerVal.getText() != null ? etTriggerVal.getText().toString().trim() : "";
            String actType = actionTypes[spinnerAction.getSelectedItemPosition()];
            String actVal = etActionVal.getText() != null ? etActionVal.getText().toString().trim() : "";

            if (name.isEmpty()) {
                name = WorkflowAutomationEngine.formatTriggerLabel(trigType, trigVal)
                        + " -> " + WorkflowAutomationEngine.formatActionLabel(actType, actVal);
            }
            WorkflowRule rule = new WorkflowRule(null, projectId, name, trigType, trigVal, actType, actVal, true);
            viewModel.addAutomationRule(projectId, rule);
            etRuleName.setText("");
            etTriggerVal.setText("");
            etActionVal.setText("");
        });

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Done", null)
            .show();
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
                    tvCountdownDays.setText(days + "d " + hours + "h Remaining");
                } else {
                    tvCountdownDays.setText(hours + " Hours Left");
                }
            } else {
                tvCountdownDays.setText("Deadline Passed");
            }
        } else {
            tvCountdownDays.setText("Active Sprint");
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
        double totalHours = 0;
        double planningHours = 0, designHours = 0, devHours = 0, reviewHours = 0;
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

            double hours = t.getTimeSpentHours();
            totalHours += hours;
            String phase = t.getPhase() != null ? t.getPhase().toUpperCase() : "DEVELOPMENT";
            if ("PLANNING".equals(phase)) planningHours += hours;
            else if ("DESIGN".equals(phase)) designHours += hours;
            else if ("REVIEW".equals(phase) || "TESTING".equals(phase)) reviewHours += hours;
            else devHours += hours;

            String assignee = t.getAssignedToName() != null && !t.getAssignedToName().isEmpty()
                    ? t.getAssignedToName() : "Unassigned";
            memberTaskCount.put(assignee, memberTaskCount.getOrDefault(assignee, 0) + 1);
        }

        tvCountTodo.setText(String.valueOf(todo));
        tvCountInProgress.setText(String.valueOf(progress));
        tvCountInReview.setText(String.valueOf(review));
        tvCountDone.setText(String.valueOf(done));

        tvStatFeatures.setText("Features: " + features);
        tvStatBugs.setText("Bugs: " + bugs);
        tvStatTasks.setText("Tasks: " + taskCount);
        tvStatDocs.setText("Docs: " + docs);

        if (tvTotalHoursLogged != null) {
            tvTotalHoursLogged.setText(String.format(Locale.getDefault(), "%.1f hrs logged", totalHours));
        }
        if (tvPhaseHoursBreakdown != null) {
            tvPhaseHoursBreakdown.setText(String.format(
                Locale.getDefault(),
                "Planning: %.1fh • Design: %.1fh • Dev: %.1fh • Review: %.1fh",
                planningHours, designHours, devHours, reviewHours
            ));
        }

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
                tvName.setText(entry.getKey());
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
        Spinner spinnerAssignee = view.findViewById(R.id.spinnerAssignee);
        Spinner spinnerStatus = view.findViewById(R.id.spinnerTaskStatus);
        Spinner spinnerDependsOn = view.findViewById(R.id.spinnerDependsOnTask);
        MaterialButton btnPickDueDate = view.findViewById(R.id.btnPickTaskDueDate);
        RadioGroup rgIssueType = view.findViewById(R.id.rgIssueType);
        RadioGroup rgPriority = view.findViewById(R.id.rgPriority);
        RadioGroup rgPhase = view.findViewById(R.id.rgPhase);

        // Real project members dropdown
        List<String> memberLabels = new ArrayList<>();
        memberLabels.add("Unassigned");
        for (User u : projectMembersList) {
            memberLabels.add((u.getName() != null && !u.getName().isEmpty()) ? u.getName() : u.getEmail());
        }
        spinnerAssignee.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, memberLabels));

        // Statuses dropdown
        List<String> statuses = currentProject != null ? currentProject.getEffectiveStatuses() : Project.defaultStatuses();
        spinnerStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statuses));

        // Dependencies dropdown
        List<String> depLabels = new ArrayList<>();
        depLabels.add("None (No Blocker)");
        for (Task t : allTasks) {
            depLabels.add((t.getTaskKey() != null ? t.getTaskKey() : "TASK") + " — " + t.getTitle());
        }
        spinnerDependsOn.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, depLabels));

        final long[] chosenDueDate = {System.currentTimeMillis() + 86400000L * 7};
        btnPickDueDate.setText("Due Date: " + dateFormat.format(new Date(chosenDueDate[0])));
        btnPickDueDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(chosenDueDate[0]);
            new DatePickerDialog(this, (picker, y, m, d) -> {
                Calendar picked = Calendar.getInstance();
                picked.set(y, m, d, 17, 0, 0);
                chosenDueDate[0] = picked.getTimeInMillis();
                btnPickDueDate.setText("Due Date: " + dateFormat.format(new Date(chosenDueDate[0])));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Create Issue", (dialog, which) -> {
                String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";

                if (title.isEmpty()) {
                    Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                int memberIdx = spinnerAssignee.getSelectedItemPosition();
                String assigneeId = null;
                String assigneeName = "";
                if (memberIdx > 0 && memberIdx - 1 < projectMembersList.size()) {
                    User selectedUser = projectMembersList.get(memberIdx - 1);
                    assigneeId = selectedUser.getUserId();
                    assigneeName = selectedUser.getName() != null ? selectedUser.getName() : selectedUser.getEmail();
                }

                String selectedStatus = statuses.get(Math.max(0, spinnerStatus.getSelectedItemPosition()));

                String depId = null;
                String depKey = null;
                int depIdx = spinnerDependsOn.getSelectedItemPosition();
                if (depIdx > 0 && depIdx - 1 < allTasks.size()) {
                    Task blocker = allTasks.get(depIdx - 1);
                    depId = blocker.getTaskId();
                    depKey = blocker.getTaskKey() != null ? blocker.getTaskKey() : blocker.getTitle();
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

                String phase = "DEVELOPMENT";
                int checkedPhaseId = rgPhase.getCheckedRadioButtonId();
                if (checkedPhaseId == R.id.rbPhasePlanning) phase = "PLANNING";
                else if (checkedPhaseId == R.id.rbPhaseDesign) phase = "DESIGN";
                else if (checkedPhaseId == R.id.rbPhaseReview) phase = "REVIEW";

                Task task = new Task(
                    null, projectId, null, title, desc, selectedStatus, priority, issueType,
                    assigneeId, assigneeName, chosenDueDate[0]
                );
                task.setPhase(phase);
                task.setDependsOnTaskId(depId);
                task.setDependsOnTaskKey(depKey);
                viewModel.addTask(projectId, task);
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
        RadioGroup rgAccessLevel = view.findViewById(R.id.rgFileAccessLevel);
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
        selectedFileSize = 0L;

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
                String accessLevel = "MEMBERS_ONLY";
                if (rgAccessLevel != null) {
                    int checkedAccess = rgAccessLevel.getCheckedRadioButtonId();
                    if (checkedAccess == R.id.rbAccessLead) accessLevel = "LEAD_ONLY";
                    else if (checkedAccess == R.id.rbAccessPublic) accessLevel = "PUBLIC";
                }

                FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
                String uploaderId = fbUser != null ? fbUser.getUid() : "";
                String uploaderName = resolveCurrentUserDisplayName(fbUser);

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

                    viewModel.uploadProjectFile(projectId, selectedFileUri, name, uploaderId, uploaderName, accessLevel, selectedFileSize,
                        new ProjectRepository.StorageUploadCallback() {
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

                if (!"CODE".equals(type) && !url.isEmpty()
                        && !url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }

                FileReference ref = new FileReference(
                    null, projectId, name, url, type, snippet, lang, uploaderName, System.currentTimeMillis()
                );
                ref.setUploadedById(uploaderId);
                ref.setAccessLevel(accessLevel);
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

    private long getFileSizeFromUri(Uri uri) {
        long size = 0L;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return size;
    }

    private void showLocationDialog() {
        if (currentProject != null && currentProject.getVenueLocation() != null && !currentProject.getVenueLocation().isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("Project Meetup Venue")
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
            .setTitle("Tag Project Venue")
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
