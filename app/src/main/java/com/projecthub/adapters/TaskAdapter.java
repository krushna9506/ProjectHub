package com.projecthub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.projecthub.R;
import com.projecthub.models.Project;
import com.projecthub.models.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskStatusChangeListener {
        void onStatusChange(Task task, String newStatus);
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    private List<Task> tasks = new ArrayList<>();
    private List<String> availableStatuses = Project.defaultStatuses();
    private final OnTaskStatusChangeListener statusChangeListener;
    private final OnTaskClickListener clickListener;
    private final SimpleDateFormat dueFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

    public TaskAdapter(OnTaskStatusChangeListener statusChangeListener, OnTaskClickListener clickListener) {
        this.statusChangeListener = statusChangeListener;
        this.clickListener = clickListener;
    }

    public TaskAdapter(OnTaskStatusChangeListener statusChangeListener) {
        this(statusChangeListener, null);
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setAvailableStatuses(List<String> statuses) {
        if (statuses != null && !statuses.isEmpty()) {
            this.availableStatuses = new ArrayList<>(statuses);
        } else {
            this.availableStatuses = Project.defaultStatuses();
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        Context context = holder.itemView.getContext();

        String keyPrefix = (task.getTaskKey() != null && !task.getTaskKey().isEmpty())
                ? "[" + task.getTaskKey() + "] " : "";
        holder.tvTitle.setText(keyPrefix + (task.getTitle() != null ? task.getTitle() : "Untitled"));
        holder.tvDesc.setText(task.getDescription() != null ? task.getDescription() : "");

        // Issue Type Badge
        String issueType = task.getIssueType() != null ? task.getIssueType() : "TASK";
        holder.tvIssueType.setText(issueType);
        if ("BUG".equalsIgnoreCase(issueType)) {
            holder.tvIssueType.setBackgroundResource(R.drawable.bg_badge_bug);
            holder.tvIssueType.setTextColor(ContextCompat.getColor(context, R.color.issue_bug));
        } else if ("FEATURE".equalsIgnoreCase(issueType)) {
            holder.tvIssueType.setBackgroundResource(R.drawable.bg_badge_feature);
            holder.tvIssueType.setTextColor(ContextCompat.getColor(context, R.color.issue_feature));
        } else if ("DOCUMENTATION".equalsIgnoreCase(issueType) || "DOC".equalsIgnoreCase(issueType)) {
            holder.tvIssueType.setBackgroundResource(R.drawable.bg_badge_doc);
            holder.tvIssueType.setTextColor(ContextCompat.getColor(context, R.color.issue_doc));
        } else {
            holder.tvIssueType.setBackgroundResource(R.drawable.bg_badge_task);
            holder.tvIssueType.setTextColor(ContextCompat.getColor(context, R.color.issue_task));
        }

        // Priority Badge
        String priority = task.getPriority() != null ? task.getPriority() : "MEDIUM";
        holder.tvPriority.setText(priority);
        int priorityColor;
        if ("URGENT".equalsIgnoreCase(priority)) {
            priorityColor = ContextCompat.getColor(context, R.color.priority_urgent);
        } else if ("HIGH".equalsIgnoreCase(priority)) {
            priorityColor = ContextCompat.getColor(context, R.color.priority_high);
        } else if ("LOW".equalsIgnoreCase(priority)) {
            priorityColor = ContextCompat.getColor(context, R.color.priority_low);
        } else {
            priorityColor = ContextCompat.getColor(context, R.color.priority_medium);
        }
        holder.tvPriority.getBackground().mutate().setTint(priorityColor);

        // Comments Count
        int commentsCount = task.getCommentsCount();
        if (commentsCount > 0) {
            holder.tvCommentsCount.setVisibility(View.VISIBLE);
            holder.tvCommentsCount.setText(commentsCount + (commentsCount == 1 ? " reply" : " replies"));
        } else {
            holder.tvCommentsCount.setVisibility(View.GONE);
        }

        // Subtasks, Blocker, Due Date badges
        int totalSub = task.getTotalSubtasksCount();
        int doneSub = task.getCompletedSubtasksCount();
        if (totalSub > 0) {
            holder.tvSubtasksBadge.setVisibility(View.VISIBLE);
            holder.tvSubtasksBadge.setText(doneSub + "/" + totalSub + " subtasks");
        } else {
            holder.tvSubtasksBadge.setVisibility(View.GONE);
        }

        if (task.getDependsOnTaskKey() != null && !task.getDependsOnTaskKey().isEmpty()) {
            holder.tvBlockerBadge.setVisibility(View.VISIBLE);
            holder.tvBlockerBadge.setText("Blocked: " + task.getDependsOnTaskKey());
        } else {
            holder.tvBlockerBadge.setVisibility(View.GONE);
        }

        if (task.getDueDate() > 0) {
            holder.tvDueDateBadge.setVisibility(View.VISIBLE);
            holder.tvDueDateBadge.setText("Due " + dueFormat.format(new Date(task.getDueDate())));
        } else {
            holder.tvDueDateBadge.setVisibility(View.GONE);
        }

        // Assignee
        String assignedText = task.getAssignedToName() != null && !task.getAssignedToName().isEmpty()
                ? task.getAssignedToName() : "Unassigned";
        holder.tvAssignedTo.setText(assignedText);

        // Status Tag & Cycling (supports custom statuses)
        String currentStatus = task.getStatus() != null ? task.getStatus() : "TODO";
        String nextStatus = computeNextStatus(currentStatus);

        if ("DONE".equalsIgnoreCase(currentStatus)) {
            holder.tvStatusBadge.setText("● DONE");
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_done);
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_done_text));
            holder.btnStatusToggle.setText("Reopen");
        } else if ("IN_REVIEW".equalsIgnoreCase(currentStatus)) {
            holder.tvStatusBadge.setText("● IN REVIEW");
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_review);
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_review_text));
            holder.btnStatusToggle.setText("Next: " + nextStatus);
        } else if ("IN_PROGRESS".equalsIgnoreCase(currentStatus)) {
            holder.tvStatusBadge.setText("● IN PROGRESS");
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_progress);
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_progress_text));
            holder.btnStatusToggle.setText("Next: " + nextStatus);
        } else if ("BLOCKED".equalsIgnoreCase(currentStatus)) {
            holder.tvStatusBadge.setText("● BLOCKED");
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_blocked);
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_blocked_text));
            holder.btnStatusToggle.setText("Next: " + nextStatus);
        } else if ("TODO".equalsIgnoreCase(currentStatus)) {
            holder.tvStatusBadge.setText("● TO DO");
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_todo);
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_todo_text));
            holder.btnStatusToggle.setText("Start →");
        } else {
            holder.tvStatusBadge.setText("● " + currentStatus.replace('_', ' '));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_task);
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.issue_task));
            holder.btnStatusToggle.setText("Next: " + nextStatus);
        }

        holder.btnStatusToggle.setOnClickListener(v -> {
            if (statusChangeListener != null) {
                statusChangeListener.onStatusChange(task, nextStatus);
            }
        });

        // Card Click opens Task Details
        holder.cardTask.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTaskClick(task);
            }
        });
    }

    private String computeNextStatus(String currentStatus) {
        if (availableStatuses == null || availableStatuses.isEmpty()) {
            return "IN_PROGRESS";
        }
        for (int i = 0; i < availableStatuses.size(); i++) {
            if (availableStatuses.get(i).equalsIgnoreCase(currentStatus)) {
                return availableStatuses.get((i + 1) % availableStatuses.size());
            }
        }
        return availableStatuses.get(0);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardTask;
        TextView tvIssueType, tvPriority, tvCommentsCount;
        TextView tvTitle, tvDesc, tvAssignedTo, tvStatusBadge;
        TextView tvSubtasksBadge, tvBlockerBadge, tvDueDateBadge;
        MaterialButton btnStatusToggle;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTask = itemView.findViewById(R.id.cardTask);
            tvIssueType = itemView.findViewById(R.id.tvTaskIssueType);
            tvPriority = itemView.findViewById(R.id.tvTaskPriority);
            tvCommentsCount = itemView.findViewById(R.id.tvTaskCommentsCount);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDesc = itemView.findViewById(R.id.tvTaskDesc);
            tvSubtasksBadge = itemView.findViewById(R.id.tvTaskSubtasksBadge);
            tvBlockerBadge = itemView.findViewById(R.id.tvTaskBlockerBadge);
            tvDueDateBadge = itemView.findViewById(R.id.tvTaskDueDateBadge);
            tvAssignedTo = itemView.findViewById(R.id.tvAssignedTo);
            tvStatusBadge = itemView.findViewById(R.id.tvTaskStatusBadge);
            btnStatusToggle = itemView.findViewById(R.id.btnStatusToggle);
        }
    }
}
