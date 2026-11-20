package com.projecthub.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.projecthub.R;
import com.projecthub.adapters.FileAdapter;
import com.projecthub.adapters.MilestoneAdapter;
import com.projecthub.adapters.TaskAdapter;
import com.projecthub.models.FileReference;
import com.projecthub.models.Milestone;
import com.projecthub.models.Task;
import com.projecthub.viewmodel.ProjectDetailsViewModel;

import java.util.List;

public class ProjectDetailsActivity extends AppCompatActivity {

    private ProjectDetailsViewModel viewModel;
    private TaskAdapter taskAdapter;
    private MilestoneAdapter milestoneAdapter;
    private FileAdapter fileAdapter;

    private TextView tvTitle, tvDesc, tvJoinCode, tvOverallProgress, tvEmptyState;
    private ProgressBar pbOverallProgress;
    private TabLayout tabLayout;
    private RecyclerView rvTasks, rvMilestones, rvFiles;
    private ExtendedFloatingActionButton fabAction;

    private String projectId, projectTitle, projectDesc, projectCode;
    private int currentTab = 0; // 0 = Tasks, 1 = Milestones, 2 = Files

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_details);

        projectId = getIntent().getStringExtra("PROJECT_ID");
        projectTitle = getIntent().getStringExtra("PROJECT_TITLE");
        projectDesc = getIntent().getStringExtra("PROJECT_DESC");
        projectCode = getIntent().getStringExtra("PROJECT_CODE");

        tvTitle = findViewById(R.id.tvProjectTitle);
        tvDesc = findViewById(R.id.tvProjectDesc);
        tvJoinCode = findViewById(R.id.tvProjectJoinCode);
        tvOverallProgress = findViewById(R.id.tvOverallProgress);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        pbOverallProgress = findViewById(R.id.pbOverallProgress);
        tabLayout = findViewById(R.id.tabLayout);
        rvTasks = findViewById(R.id.rvTasks);
        rvMilestones = findViewById(R.id.rvMilestones);
        rvFiles = findViewById(R.id.rvFiles);
        fabAction = findViewById(R.id.fabAction);

        tvTitle.setText(projectTitle);
        tvDesc.setText(projectDesc);
        tvJoinCode.setText("Code: " + projectCode);

        setupTabs();
        setupRecyclerViews();

        viewModel = new ViewModelProvider(this).get(ProjectDetailsViewModel.class);
        viewModel.init(projectId);

        observeData();

        fabAction.setOnClickListener(v -> {
            if (currentTab == 0) {
                showAddTaskDialog();
            } else if (currentTab == 1) {
                showAddMilestoneDialog();
            } else {
                showAddFileDialog();
            }
        });
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Tasks"));
        tabLayout.addTab(tabLayout.newTab().setText("Milestones"));
        tabLayout.addTab(tabLayout.newTab().setText("Files & Links"));

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
        rvTasks.setVisibility(currentTab == 0 ? View.VISIBLE : View.GONE);
        rvMilestones.setVisibility(currentTab == 1 ? View.VISIBLE : View.GONE);
        rvFiles.setVisibility(currentTab == 2 ? View.VISIBLE : View.GONE);

        if (currentTab == 0) {
            fabAction.setText("Add Task");
            checkEmptyState(taskAdapter.getItemCount());
        } else if (currentTab == 1) {
            fabAction.setText("Add Milestone");
            checkEmptyState(milestoneAdapter.getItemCount());
        } else {
            fabAction.setText("Add Resource");
            checkEmptyState(fileAdapter.getItemCount());
        }
    }

    private void checkEmptyState(int count) {
        tvEmptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    private void setupRecyclerViews() {
        // Tasks
        taskAdapter = new TaskAdapter((task, newStatus) -> {
            viewModel.updateTaskStatus(projectId, task.getTaskId(), newStatus);
        });
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);

        // Milestones
        milestoneAdapter = new MilestoneAdapter();
        rvMilestones.setLayoutManager(new LinearLayoutManager(this));
        rvMilestones.setAdapter(milestoneAdapter);

        // Files
        fileAdapter = new FileAdapter(file -> {
            if (file.getUrl() != null && !file.getUrl().isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(file.getUrl()));
                try {
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Cannot open URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        rvFiles.setLayoutManager(new LinearLayoutManager(this));
        rvFiles.setAdapter(fileAdapter);
    }

    private void observeData() {
        viewModel.getTasks().observe(this, tasks -> {
            taskAdapter.setTasks(tasks);
            updateProgress(tasks);
            if (currentTab == 0) checkEmptyState(tasks.size());
        });

        viewModel.getMilestones().observe(this, milestones -> {
            milestoneAdapter.setMilestones(milestones);
            if (currentTab == 1) checkEmptyState(milestones.size());
        });

        viewModel.getFiles().observe(this, files -> {
            fileAdapter.setFiles(files);
            if (currentTab == 2) checkEmptyState(files.size());
        });

        viewModel.getActionMessage().observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateProgress(List<Task> tasks) {
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

    private void showAddTaskDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        TextInputEditText etTitle = view.findViewById(R.id.etTaskTitle);
        TextInputEditText etDesc = view.findViewById(R.id.etTaskDesc);
        TextInputEditText etAssignee = view.findViewById(R.id.etAssignee);
        RadioGroup rgPriority = view.findViewById(R.id.rgPriority);

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Create Task", (dialog, which) -> {
                String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
                String assignee = etAssignee.getText() != null ? etAssignee.getText().toString().trim() : "";

                if (title.isEmpty()) {
                    Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                String priority = "MEDIUM";
                int checkedId = rgPriority.getCheckedRadioButtonId();
                if (checkedId == R.id.rbLow) priority = "LOW";
                else if (checkedId == R.id.rbHigh) priority = "HIGH";

                Task task = new Task(null, projectId, null, title, desc, "TODO", priority, null, assignee, System.currentTimeMillis() + 86400000L * 7);
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
        TextInputEditText etName = view.findViewById(R.id.etFileName);
        TextInputEditText etUrl = view.findViewById(R.id.etFileUrl);

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Save Reference", (dialog, which) -> {
                String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                String url = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";

                if (name.isEmpty() || url.isEmpty()) {
                    Toast.makeText(this, "Name and URL are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                String uploader = FirebaseAuth.getInstance().getCurrentUser() != null && 
                                  FirebaseAuth.getInstance().getCurrentUser().getEmail() != null ?
                                  FirebaseAuth.getInstance().getCurrentUser().getEmail() : "Member";

                FileReference ref = new FileReference(null, projectId, name, url, "LINK", uploader, System.currentTimeMillis());
                viewModel.addFileReference(projectId, ref);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
