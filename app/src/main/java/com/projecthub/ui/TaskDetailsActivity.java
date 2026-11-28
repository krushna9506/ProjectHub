package com.projecthub.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projecthub.R;
import com.projecthub.adapters.SubtaskAdapter;
import com.projecthub.adapters.TaskCommentAdapter;
import com.projecthub.models.Project;
import com.projecthub.models.Subtask;
import com.projecthub.models.Task;
import com.projecthub.models.TaskComment;
import com.projecthub.models.User;
import com.projecthub.repository.ProjectRepository;
import com.projecthub.utils.MentionParser;
import com.projecthub.utils.SafeUrlUtils;
import com.projecthub.viewmodel.ProjectDetailsViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskDetailsActivity extends AppCompatActivity {

    private ProjectDetailsViewModel viewModel;
    private TaskCommentAdapter commentAdapter;
    private SubtaskAdapter subtaskAdapter;

    private String projectId, projectTitle;
    private String taskId;
    private String taskTitle, taskDesc, taskStatus, taskPriority, taskIssueType;
    private String taskAssignee, taskAssigneeId, taskPhase;
    private long taskDueDate;
    private double taskTimeSpent;

    private Task currentTask;
    private Project currentProject;
    private List<Task> allProjectTasks = new ArrayList<>();
    private List<User> projectMembersList = new ArrayList<>();
    private List<Subtask> currentSubtasks = new ArrayList<>();
    private String lastMembersSig = "";

    private TextView tvIssueType, tvPriority, tvDueDate, tvTitle, tvDesc, tvAssignee;
    private TextView tvDetailPhase, tvDetailTimeSpent, tvDetailBlocker;
    private TextView tvCommentsCountBadge, tvEmptyComments, tvSubtaskProgress;
    private ProgressBar pbSubtasks;
    private ChipGroup chipGroupStatus;
    private RecyclerView rvComments, rvSubtasks;
    private TextInputEditText etCommentInput, etNewSubtask;
    private MaterialButton btnSendComment, btnLogWorkTime, btnEditTask, btnDeleteTask, btnDetailOpenGithub;
    private MaterialButton btnAddSubtask, btnCommentMention;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private boolean isUpdatingChipSelection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        projectId = getIntent().getStringExtra("PROJECT_ID");
        projectTitle = getIntent().getStringExtra("PROJECT_TITLE");
        taskId = getIntent().getStringExtra("TASK_ID");
        taskTitle = getIntent().getStringExtra("TASK_TITLE");
        taskDesc = getIntent().getStringExtra("TASK_DESC");
        taskStatus = getIntent().getStringExtra("TASK_STATUS");
        taskPriority = getIntent().getStringExtra("TASK_PRIORITY");
        taskIssueType = getIntent().getStringExtra("TASK_ISSUE_TYPE");
        taskAssignee = getIntent().getStringExtra("TASK_ASSIGNEE");
        taskAssigneeId = getIntent().getStringExtra("TASK_ASSIGNEE_ID");
        taskDueDate = getIntent().getLongExtra("TASK_DUE_DATE", 0);
        taskPhase = getIntent().getStringExtra("TASK_PHASE");
        taskTimeSpent = getIntent().getDoubleExtra("TASK_TIME_SPENT", 0.0);

        if (taskStatus == null) taskStatus = "TODO";
        if (taskPriority == null) taskPriority = "MEDIUM";
        if (taskIssueType == null) taskIssueType = "TASK";
        if (taskPhase == null) taskPhase = "DEVELOPMENT";

        initViews();
        setupToolbar();
        populateTaskDetails();
        renderStatusChips(Project.defaultStatuses());
        setupSubtasks();
        setupComments();

        viewModel = new ViewModelProvider(this).get(ProjectDetailsViewModel.class);
        if (projectId != null && !projectId.isEmpty()) {
            viewModel.init(projectId);
        }
        observeAllData();
    }

    private void initViews() {
        tvIssueType = findViewById(R.id.tvDetailIssueType);
        tvPriority = findViewById(R.id.tvDetailPriority);
        tvDueDate = findViewById(R.id.tvDetailDueDate);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvDesc = findViewById(R.id.tvDetailDesc);
        tvAssignee = findViewById(R.id.tvDetailAssignee);
        tvDetailBlocker = findViewById(R.id.tvDetailBlocker);
        tvCommentsCountBadge = findViewById(R.id.tvCommentsCountBadge);
        tvEmptyComments = findViewById(R.id.tvEmptyComments);
        tvSubtaskProgress = findViewById(R.id.tvSubtaskProgress);
        pbSubtasks = findViewById(R.id.pbSubtasks);

        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        rvComments = findViewById(R.id.rvTaskComments);
        rvSubtasks = findViewById(R.id.rvSubtasks);
        etCommentInput = findViewById(R.id.etCommentInput);
        etNewSubtask = findViewById(R.id.etNewSubtask);
        btnSendComment = findViewById(R.id.btnSendComment);
        btnCommentMention = findViewById(R.id.btnCommentMention);
        btnAddSubtask = findViewById(R.id.btnAddSubtask);

        tvDetailPhase = findViewById(R.id.tvDetailPhase);
        tvDetailTimeSpent = findViewById(R.id.tvDetailTimeSpent);
        btnLogWorkTime = findViewById(R.id.btnLogWorkTime);
        btnEditTask = findViewById(R.id.btnEditTask);
        btnDeleteTask = findViewById(R.id.btnDeleteTask);
        btnDetailOpenGithub = findViewById(R.id.btnDetailOpenGithub);

        btnLogWorkTime.setOnClickListener(v -> showLogTimeDialog());
        btnEditTask.setOnClickListener(v -> showEditTaskDialog());
        btnDeleteTask.setOnClickListener(v -> confirmDeleteTask());
        btnDetailOpenGithub.setOnClickListener(v -> {
            if (currentTask != null && currentTask.getGithubIssueUrl() != null && !currentTask.getGithubIssueUrl().isEmpty()) {
                SafeUrlUtils.openSafeHttpUrl(TaskDetailsActivity.this, currentTask.getGithubIssueUrl());
            }
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Task Details");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void populateTaskDetails() {
        tvTitle.setText(taskTitle != null ? taskTitle : "Untitled Task");
        tvDesc.setText(taskDesc != null && !taskDesc.isEmpty() ? taskDesc : "No detailed description provided.");
        tvAssignee.setText(taskAssignee != null && !taskAssignee.isEmpty() ? taskAssignee : "Unassigned");

        if (taskDueDate > 0) {
            tvDueDate.setText("Due: " + dateFormat.format(new Date(taskDueDate)));
        } else {
            tvDueDate.setText("No due date");
        }

        tvIssueType.setText(taskIssueType);
        if ("BUG".equalsIgnoreCase(taskIssueType)) {
            tvIssueType.setBackgroundResource(R.drawable.bg_badge_bug);
            tvIssueType.setTextColor(ContextCompat.getColor(this, R.color.issue_bug));
        } else if ("FEATURE".equalsIgnoreCase(taskIssueType)) {
            tvIssueType.setBackgroundResource(R.drawable.bg_badge_feature);
            tvIssueType.setTextColor(ContextCompat.getColor(this, R.color.issue_feature));
        } else if ("DOCUMENTATION".equalsIgnoreCase(taskIssueType) || "DOC".equalsIgnoreCase(taskIssueType)) {
            tvIssueType.setBackgroundResource(R.drawable.bg_badge_doc);
            tvIssueType.setTextColor(ContextCompat.getColor(this, R.color.issue_doc));
        } else {
            tvIssueType.setBackgroundResource(R.drawable.bg_badge_task);
            tvIssueType.setTextColor(ContextCompat.getColor(this, R.color.issue_task));
        }

        tvPriority.setText(taskPriority);
        int priorityColor;
        if ("URGENT".equalsIgnoreCase(taskPriority)) {
            priorityColor = ContextCompat.getColor(this, R.color.priority_urgent);
        } else if ("HIGH".equalsIgnoreCase(taskPriority)) {
            priorityColor = ContextCompat.getColor(this, R.color.priority_high);
        } else if ("LOW".equalsIgnoreCase(taskPriority)) {
            priorityColor = ContextCompat.getColor(this, R.color.priority_low);
        } else {
            priorityColor = ContextCompat.getColor(this, R.color.priority_medium);
        }
        tvPriority.getBackground().mutate().setTint(priorityColor);

        tvDetailPhase.setText("PHASE: " + (taskPhase != null ? taskPhase.toUpperCase() : "DEVELOPMENT"));
        tvDetailTimeSpent.setText(String.format(Locale.US, "%.1f hrs logged", taskTimeSpent));

        if (currentTask != null && currentTask.getDependsOnTaskKey() != null && !currentTask.getDependsOnTaskKey().isEmpty()) {
            tvDetailBlocker.setVisibility(View.VISIBLE);
            tvDetailBlocker.setText("Blocked by " + currentTask.getDependsOnTaskKey());
        } else {
            tvDetailBlocker.setVisibility(View.GONE);
        }

        if (currentTask != null && currentTask.getGithubIssueUrl() != null && !currentTask.getGithubIssueUrl().isEmpty()) {
            btnDetailOpenGithub.setVisibility(View.VISIBLE);
        } else {
            btnDetailOpenGithub.setVisibility(View.GONE);
        }
    }

    private void renderStatusChips(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) statuses = Project.defaultStatuses();
        isUpdatingChipSelection = true;
        chipGroupStatus.removeAllViews();
        for (String st : statuses) {
            Chip chip = new Chip(this);
            chip.setText(st.replace('_', ' '));
            chip.setTag(st);
            chip.setCheckable(true);
            chip.setChecked(st.equalsIgnoreCase(taskStatus));
            chipGroupStatus.addView(chip);
        }
        isUpdatingChipSelection = false;

        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (isUpdatingChipSelection || checkedIds.isEmpty()) return;
            View checkedView = group.findViewById(checkedIds.get(0));
            if (checkedView != null && checkedView.getTag() instanceof String) {
                String newStatus = (String) checkedView.getTag();
                if (!newStatus.equalsIgnoreCase(taskStatus)) {
                    taskStatus = newStatus;
                    viewModel.updateTaskStatus(projectId, taskId, newStatus);
                    Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupSubtasks() {
        subtaskAdapter = new SubtaskAdapter(new SubtaskAdapter.OnSubtaskActionListener() {
            @Override
            public void onToggle(int position, boolean completed) {
                if (position >= 0 && position < currentSubtasks.size()) {
                    currentSubtasks.get(position).setCompleted(completed);
                    updateSubtasksUi();
                    viewModel.updateTaskSubtasks(projectId, taskId, currentSubtasks);
                }
            }

            @Override
            public void onDelete(int position) {
                if (position >= 0 && position < currentSubtasks.size()) {
                    currentSubtasks.remove(position);
                    updateSubtasksUi();
                    viewModel.updateTaskSubtasks(projectId, taskId, currentSubtasks);
                }
            }
        });
        rvSubtasks.setLayoutManager(new LinearLayoutManager(this));
        rvSubtasks.setAdapter(subtaskAdapter);

        btnAddSubtask.setOnClickListener(v -> {
            String title = etNewSubtask.getText() != null ? etNewSubtask.getText().toString().trim() : "";
            if (title.isEmpty()) {
                Toast.makeText(this, "Enter a subtask title", Toast.LENGTH_SHORT).show();
                return;
            }
            currentSubtasks.add(new Subtask(null, title, false));
            etNewSubtask.setText("");
            updateSubtasksUi();
            viewModel.updateTaskSubtasks(projectId, taskId, currentSubtasks);
        });
    }

    private void updateSubtasksUi() {
        subtaskAdapter.setSubtasks(currentSubtasks);
        int total = currentSubtasks.size();
        int done = 0;
        for (Subtask s : currentSubtasks) {
            if (s != null && s.isCompleted()) done++;
        }
        tvSubtaskProgress.setText(done + "/" + total + " done");
        int pct = total > 0 ? (int) (((double) done / total) * 100) : 0;
        pbSubtasks.setProgress(pct);
    }

    private void showEditTaskDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        TextView tvHeading = view.findViewById(R.id.tvDialogTaskHeading);
        TextInputEditText etTitle = view.findViewById(R.id.etTaskTitle);
        TextInputEditText etDesc = view.findViewById(R.id.etTaskDesc);
        Spinner spinnerAssignee = view.findViewById(R.id.spinnerAssignee);
        Spinner spinnerStatus = view.findViewById(R.id.spinnerTaskStatus);
        Spinner spinnerDependsOn = view.findViewById(R.id.spinnerDependsOnTask);
        MaterialButton btnPickDueDate = view.findViewById(R.id.btnPickTaskDueDate);
        RadioGroup rgIssueType = view.findViewById(R.id.rgIssueType);
        RadioGroup rgPriority = view.findViewById(R.id.rgPriority);
        RadioGroup rgPhase = view.findViewById(R.id.rgPhase);

        tvHeading.setText("Edit Issue / Task");
        etTitle.setText(taskTitle);
        etDesc.setText(taskDesc);

        // Assignee picker
        List<String> memberLabels = new ArrayList<>();
        memberLabels.add("Unassigned");
        int selectedAssigneeIdx = 0;
        for (int i = 0; i < projectMembersList.size(); i++) {
            User u = projectMembersList.get(i);
            String label = (u.getName() != null && !u.getName().isEmpty()) ? u.getName() : u.getEmail();
            memberLabels.add(label);
            if ((taskAssigneeId != null && taskAssigneeId.equals(u.getUserId()))
                    || (taskAssignee != null && taskAssignee.equalsIgnoreCase(label))) {
                selectedAssigneeIdx = i + 1;
            }
        }
        spinnerAssignee.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, memberLabels));
        spinnerAssignee.setSelection(selectedAssigneeIdx);

        // Status picker
        List<String> statuses = currentProject != null ? currentProject.getEffectiveStatuses() : Project.defaultStatuses();
        spinnerStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statuses));
        for (int i = 0; i < statuses.size(); i++) {
            if (statuses.get(i).equalsIgnoreCase(taskStatus)) {
                spinnerStatus.setSelection(i);
                break;
            }
        }

        // Blocker picker
        List<Task> candidates = new ArrayList<>();
        List<String> depLabels = new ArrayList<>();
        depLabels.add("None (No Blocker)");
        int selectedDepIdx = 0;
        String existingDepId = currentTask != null ? currentTask.getDependsOnTaskId() : null;
        for (Task t : allProjectTasks) {
            if (taskId != null && taskId.equals(t.getTaskId())) continue;
            candidates.add(t);
            depLabels.add((t.getTaskKey() != null ? t.getTaskKey() : "TASK") + " — " + t.getTitle());
            if (existingDepId != null && existingDepId.equals(t.getTaskId())) {
                selectedDepIdx = candidates.size();
            }
        }
        spinnerDependsOn.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, depLabels));
        spinnerDependsOn.setSelection(selectedDepIdx);

        // Due Date
        final long[] chosenDue = {taskDueDate > 0 ? taskDueDate : System.currentTimeMillis() + 7L * 86400000L};
        btnPickDueDate.setText("Due Date: " + dateFormat.format(new Date(chosenDue[0])));
        btnPickDueDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(chosenDue[0]);
            new DatePickerDialog(this, (picker, y, m, d) -> {
                Calendar picked = Calendar.getInstance();
                picked.set(y, m, d, 17, 0, 0);
                chosenDue[0] = picked.getTimeInMillis();
                btnPickDueDate.setText("Due Date: " + dateFormat.format(new Date(chosenDue[0])));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Preselect radio groups
        if ("BUG".equalsIgnoreCase(taskIssueType)) rgIssueType.check(R.id.rbTypeBug);
        else if ("FEATURE".equalsIgnoreCase(taskIssueType)) rgIssueType.check(R.id.rbTypeFeature);
        else if ("DOCUMENTATION".equalsIgnoreCase(taskIssueType) || "DOC".equalsIgnoreCase(taskIssueType)) rgIssueType.check(R.id.rbTypeDoc);
        else rgIssueType.check(R.id.rbTypeTask);

        if ("LOW".equalsIgnoreCase(taskPriority)) rgPriority.check(R.id.rbLow);
        else if ("HIGH".equalsIgnoreCase(taskPriority)) rgPriority.check(R.id.rbHigh);
        else if ("URGENT".equalsIgnoreCase(taskPriority)) rgPriority.check(R.id.rbUrgent);
        else rgPriority.check(R.id.rbMedium);

        if ("PLANNING".equalsIgnoreCase(taskPhase)) rgPhase.check(R.id.rbPhasePlanning);
        else if ("DESIGN".equalsIgnoreCase(taskPhase)) rgPhase.check(R.id.rbPhaseDesign);
        else if ("REVIEW".equalsIgnoreCase(taskPhase)) rgPhase.check(R.id.rbPhaseReview);
        else rgPhase.check(R.id.rbPhaseDev);

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Save Changes", (dialog, which) -> {
                String newTitle = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                String newDesc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
                if (newTitle.isEmpty()) {
                    Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                String prevAssigneeId = taskAssigneeId;
                String prevStatus = taskStatus;

                int memberIdx = spinnerAssignee.getSelectedItemPosition();
                String newAssigneeId = "";
                String newAssigneeName = "";
                if (memberIdx > 0 && memberIdx - 1 < projectMembersList.size()) {
                    User u = projectMembersList.get(memberIdx - 1);
                    newAssigneeId = u.getUserId();
                    newAssigneeName = u.getName() != null ? u.getName() : u.getEmail();
                }

                String newStatus = statuses.get(Math.max(0, spinnerStatus.getSelectedItemPosition()));

                String depId = null;
                String depKey = null;
                int depIdx = spinnerDependsOn.getSelectedItemPosition();
                if (depIdx > 0 && depIdx - 1 < candidates.size()) {
                    Task blocker = candidates.get(depIdx - 1);
                    depId = blocker.getTaskId();
                    depKey = blocker.getTaskKey() != null ? blocker.getTaskKey() : blocker.getTitle();
                }

                String newIssueType = "TASK";
                int tId = rgIssueType.getCheckedRadioButtonId();
                if (tId == R.id.rbTypeBug) newIssueType = "BUG";
                else if (tId == R.id.rbTypeFeature) newIssueType = "FEATURE";
                else if (tId == R.id.rbTypeDoc) newIssueType = "DOCUMENTATION";

                String newPriority = "MEDIUM";
                int pId = rgPriority.getCheckedRadioButtonId();
                if (pId == R.id.rbLow) newPriority = "LOW";
                else if (pId == R.id.rbHigh) newPriority = "HIGH";
                else if (pId == R.id.rbUrgent) newPriority = "URGENT";

                String newPhase = "DEVELOPMENT";
                int phId = rgPhase.getCheckedRadioButtonId();
                if (phId == R.id.rbPhasePlanning) newPhase = "PLANNING";
                else if (phId == R.id.rbPhaseDesign) newPhase = "DESIGN";
                else if (phId == R.id.rbPhaseReview) newPhase = "REVIEW";

                Task updated = currentTask != null ? currentTask : new Task();
                updated.setTaskId(taskId);
                updated.setProjectId(projectId);
                updated.setTitle(newTitle);
                updated.setDescription(newDesc);
                updated.setAssignedTo(newAssigneeId);
                updated.setAssignedToName(newAssigneeName);
                updated.setStatus(newStatus);
                updated.setPriority(newPriority);
                updated.setIssueType(newIssueType);
                updated.setPhase(newPhase);
                updated.setDueDate(chosenDue[0]);
                updated.setDependsOnTaskId(depId);
                updated.setDependsOnTaskKey(depKey);

                viewModel.updateTask(projectId, updated, prevAssigneeId, prevStatus);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void confirmDeleteTask() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Task?")
            .setMessage("Permanently delete \"" + (taskTitle != null ? taskTitle : "this task") + "\"?")
            .setPositiveButton("Delete", (d, w) -> viewModel.deleteTask(projectId, taskId, new ProjectRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(TaskDetailsActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(TaskDetailsActivity.this, "Delete failed: " + message, Toast.LENGTH_SHORT).show();
                }
            }))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showLogTimeDialog() {
        final android.widget.EditText etHours = new android.widget.EditText(this);
        etHours.setHint("Hours spent (e.g. 2.5)");
        etHours.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etHours.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(this)
            .setTitle("Log Work Time (Timesheet)")
            .setMessage("Record time spent by your team on this task:")
            .setView(etHours)
            .setPositiveButton("Log Hours", (dialog, which) -> {
                String val = etHours.getText().toString().trim();
                if (!val.isEmpty()) {
                    try {
                        double hrs = Double.parseDouble(val);
                        if (hrs > 0 && hrs <= 1000) {
                            viewModel.logTaskWorkTime(projectId, taskId, hrs);
                            taskTimeSpent += hrs;
                            tvDetailTimeSpent.setText(String.format(Locale.US, "%.1f hrs logged", taskTimeSpent));
                        } else {
                            Toast.makeText(this, "Enter valid hours (0.1 to 1000).", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException ignored) {
                        Toast.makeText(this, "Enter valid numeric hours.", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void setupComments() {
        commentAdapter = new TaskCommentAdapter();
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);

        btnSendComment.setOnClickListener(v -> postComment());
        if (btnCommentMention != null) {
            btnCommentMention.setOnClickListener(v -> showCommentMentionPicker());
        }
    }

    private void showCommentMentionPicker() {
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
                String current = etCommentInput.getText() != null ? etCommentInput.getText().toString() : "";
                String next = current.isEmpty() ? handle + " " : current + (current.endsWith(" ") ? "" : " ") + handle + " ";
                etCommentInput.setText(next);
                etCommentInput.setSelection(next.length());
            })
            .show();
    }

    private void observeAllData() {
        if (projectId == null || taskId == null) return;

        viewModel.getTask(projectId, taskId).observe(this, task -> {
            if (task != null) {
                currentTask = task;
                taskTitle = task.getTitle();
                taskDesc = task.getDescription();
                taskStatus = task.getStatus();
                taskPriority = task.getPriority();
                taskIssueType = task.getIssueType();
                taskAssignee = task.getAssignedToName();
                taskAssigneeId = task.getAssignedTo();
                taskDueDate = task.getDueDate();
                taskPhase = task.getPhase();
                taskTimeSpent = task.getTimeSpentHours();

                currentSubtasks = new ArrayList<>(task.getSubtasks());
                updateSubtasksUi();
                populateTaskDetails();
                renderStatusChips(currentProject != null ? currentProject.getEffectiveStatuses() : Project.defaultStatuses());
            }
        });

        if (viewModel.getProject() != null) {
            viewModel.getProject().observe(this, project -> {
                if (project != null) {
                    currentProject = project;
                    if (project.getTitle() != null) projectTitle = project.getTitle();
                    renderStatusChips(project.getEffectiveStatuses());
                    if (project.getMembers() != null) {
                        List<String> ids = new ArrayList<>(project.getMembers().keySet());
                        String sig = TextUtils.join(",", ids);
                        if (!sig.equals(lastMembersSig)) {
                            lastMembersSig = sig;
                            viewModel.loadMembers(ids);
                        }
                    }
                }
            });
        }

        if (viewModel.getTasks() != null) {
            viewModel.getTasks().observe(this, tasks -> {
                allProjectTasks = tasks != null ? tasks : new ArrayList<>();
            });
        }

        viewModel.getProjectMembers().observe(this, members -> {
            projectMembersList = members != null ? members : new ArrayList<>();
        });

        viewModel.getTaskComments(projectId, taskId).observe(this, comments -> {
            if (comments != null) {
                commentAdapter.setComments(comments);
                tvCommentsCountBadge.setText(comments.size() + (comments.size() == 1 ? " Comment" : " Comments"));
                tvEmptyComments.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
                if (!comments.isEmpty()) {
                    rvComments.scrollToPosition(comments.size() - 1);
                }
            } else {
                tvEmptyComments.setVisibility(View.VISIBLE);
                tvCommentsCountBadge.setText("0 Comments");
            }
        });

        viewModel.getActionMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void postComment() {
        String message = etCommentInput.getText() != null ? etCommentInput.getText().toString().trim() : "";
        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter a comment message", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String authorId = currentUser != null ? currentUser.getUid() : "anonymous";
        String authorName = currentUser != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()
                ? currentUser.getDisplayName()
                : (currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail().split("@")[0] : "Team Member");

        TaskComment comment = new TaskComment(
            null, taskId, projectId, authorId, authorName, message, System.currentTimeMillis()
        );

        List<User> mentioned = MentionParser.resolveMentionedUsers(message, projectMembersList);
        List<String> mentionedIds = new ArrayList<>();
        for (User u : mentioned) {
            if (u.getUserId() != null) mentionedIds.add(u.getUserId());
        }

        Task contextTask = currentTask != null ? currentTask : new Task(taskId, projectId, null, taskTitle, taskDesc, taskStatus, taskPriority, taskAssigneeId, taskAssignee, taskDueDate);
        viewModel.addTaskCommentWithMentions(projectId, taskId, comment, contextTask, projectTitle, mentionedIds);
        etCommentInput.setText("");
    }
}
