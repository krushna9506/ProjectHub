package com.projecthub.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
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
import com.projecthub.adapters.TaskCommentAdapter;
import com.projecthub.models.TaskComment;
import com.projecthub.viewmodel.ProjectDetailsViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskDetailsActivity extends AppCompatActivity {

    private ProjectDetailsViewModel viewModel;
    private TaskCommentAdapter commentAdapter;

    private String projectId;
    private String taskId;
    private String taskTitle;
    private String taskDesc;
    private String taskStatus;
    private String taskPriority;
    private String taskIssueType;
    private String taskAssignee;
    private long taskDueDate;
    private String taskPhase;
    private double taskTimeSpent;

    private TextView tvIssueType, tvPriority, tvDueDate, tvTitle, tvDesc, tvAssignee;
    private TextView tvDetailPhase, tvDetailTimeSpent;
    private TextView tvCommentsCountBadge, tvEmptyComments;
    private ChipGroup chipGroupStatus;
    private Chip chipTodo, chipInProgress, chipInReview, chipDone;
    private RecyclerView rvComments;
    private TextInputEditText etCommentInput;
    private MaterialButton btnSendComment, btnLogWorkTime;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private boolean isUpdatingChipSelection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        projectId = getIntent().getStringExtra("PROJECT_ID");
        taskId = getIntent().getStringExtra("TASK_ID");
        taskTitle = getIntent().getStringExtra("TASK_TITLE");
        taskDesc = getIntent().getStringExtra("TASK_DESC");
        taskStatus = getIntent().getStringExtra("TASK_STATUS");
        taskPriority = getIntent().getStringExtra("TASK_PRIORITY");
        taskIssueType = getIntent().getStringExtra("TASK_ISSUE_TYPE");
        taskAssignee = getIntent().getStringExtra("TASK_ASSIGNEE");
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
        setupStatusWorkflow();
        setupComments();

        viewModel = new ViewModelProvider(this).get(ProjectDetailsViewModel.class);
        observeComments();
    }

    private void initViews() {
        tvIssueType = findViewById(R.id.tvDetailIssueType);
        tvPriority = findViewById(R.id.tvDetailPriority);
        tvDueDate = findViewById(R.id.tvDetailDueDate);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvDesc = findViewById(R.id.tvDetailDesc);
        tvAssignee = findViewById(R.id.tvDetailAssignee);
        tvCommentsCountBadge = findViewById(R.id.tvCommentsCountBadge);
        tvEmptyComments = findViewById(R.id.tvEmptyComments);

        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        chipTodo = findViewById(R.id.chipStatusTodo);
        chipInProgress = findViewById(R.id.chipStatusInProgress);
        chipInReview = findViewById(R.id.chipStatusInReview);
        chipDone = findViewById(R.id.chipStatusDone);

        rvComments = findViewById(R.id.rvTaskComments);
        etCommentInput = findViewById(R.id.etCommentInput);
        btnSendComment = findViewById(R.id.btnSendComment);

        tvDetailPhase = findViewById(R.id.tvDetailPhase);
        tvDetailTimeSpent = findViewById(R.id.tvDetailTimeSpent);
        btnLogWorkTime = findViewById(R.id.btnLogWorkTime);
        btnLogWorkTime.setOnClickListener(v -> showLogTimeDialog());
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

        // Format Due Date
        if (taskDueDate > 0) {
            tvDueDate.setText("Due: " + dateFormat.format(new Date(taskDueDate)));
        } else {
            tvDueDate.setText("No due date");
        }

        // Issue Type Badge Styling
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

        // Priority Pill Styling
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

        // Phase & Time Spent (Zoho Projects Feature)
        tvDetailPhase.setText("PHASE: " + (taskPhase != null ? taskPhase.toUpperCase() : "DEVELOPMENT"));
        tvDetailTimeSpent.setText(String.format(Locale.US, "⏱️ %.1f hrs logged", taskTimeSpent));
    }

    private void showLogTimeDialog() {
        final android.widget.EditText etHours = new android.widget.EditText(this);
        etHours.setHint("Hours spent (e.g. 2.5)");
        etHours.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etHours.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(this)
            .setTitle("⏱️ Log Work Time (Timesheet)")
            .setMessage("Record time spent by your team on this task:")
            .setView(etHours)
            .setPositiveButton("Log Hours", (dialog, which) -> {
                String val = etHours.getText().toString().trim();
                if (!val.isEmpty()) {
                    try {
                        double hrs = Double.parseDouble(val);
                        if (hrs > 0) {
                            viewModel.logTaskWorkTime(projectId, taskId, hrs);
                            taskTimeSpent += hrs;
                            tvDetailTimeSpent.setText(String.format(Locale.US, "⏱️ %.1f hrs logged", taskTimeSpent));
                            Toast.makeText(this, "Logged " + hrs + " hours to timesheet!", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException ignored) {}
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void setupStatusWorkflow() {
        selectChipForStatus(taskStatus);

        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (isUpdatingChipSelection || checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            String newStatus = "TODO";
            if (checkedId == R.id.chipStatusInProgress) {
                newStatus = "IN_PROGRESS";
            } else if (checkedId == R.id.chipStatusInReview) {
                newStatus = "IN_REVIEW";
            } else if (checkedId == R.id.chipStatusDone) {
                newStatus = "DONE";
            }

            if (!newStatus.equalsIgnoreCase(taskStatus)) {
                taskStatus = newStatus;
                viewModel.updateTaskStatus(projectId, taskId, newStatus);
                Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectChipForStatus(String status) {
        isUpdatingChipSelection = true;
        if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            chipInProgress.setChecked(true);
        } else if ("IN_REVIEW".equalsIgnoreCase(status)) {
            chipInReview.setChecked(true);
        } else if ("DONE".equalsIgnoreCase(status)) {
            chipDone.setChecked(true);
        } else {
            chipTodo.setChecked(true);
        }
        isUpdatingChipSelection = false;
    }

    private void setupComments() {
        commentAdapter = new TaskCommentAdapter();
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);

        btnSendComment.setOnClickListener(v -> postComment());
    }

    private void observeComments() {
        if (projectId == null || taskId == null) return;

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
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
        String authorName = currentUser != null && currentUser.getEmail() != null ? 
            currentUser.getEmail().split("@")[0] : "Team Member";

        TaskComment comment = new TaskComment(
            null, taskId, projectId, authorId, authorName, message, System.currentTimeMillis()
        );

        viewModel.addTaskComment(projectId, taskId, comment);
        etCommentInput.setText("");
    }
}
