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
import com.projecthub.models.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskStatusChangeListener {
        void onStatusChange(Task task, String newStatus);
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    private List<Task> tasks = new ArrayList<>();
    private final OnTaskStatusChangeListener statusChangeListener;
    private final OnTaskClickListener clickListener;

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

        holder.tvTitle.setText(task.getTitle());
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
            holder.tvCommentsCount.setText("💬 " + commentsCount);
        } else {
            holder.tvCommentsCount.setVisibility(View.GONE);
        }

        // Assignee
        String assignedText = task.getAssignedToName() != null && !task.getAssignedToName().isEmpty() 
            ? "👤 " + task.getAssignedToName() : "👤 Unassigned";
        holder.tvAssignedTo.setText(assignedText);

        // Status Tag & Quick Workflow Cycling: TODO -> IN_PROGRESS -> IN_REVIEW -> DONE -> TODO
        String currentStatus = task.getStatus() != null ? task.getStatus() : "TODO";
        if ("DONE".equalsIgnoreCase(currentStatus)) {
            holder.tvStatusBadge.setText("● DONE");
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_done));
            holder.btnStatusToggle.setText("Reopen");
            holder.btnStatusToggle.setOnClickListener(v -> {
                if (statusChangeListener != null) statusChangeListener.onStatusChange(task, "TODO");
            });
        } else if ("IN_REVIEW".equalsIgnoreCase(currentStatus)) {
            holder.tvStatusBadge.setText("● IN REVIEW");
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_review));
            holder.btnStatusToggle.setText("Done ✓");
            holder.btnStatusToggle.setOnClickListener(v -> {
                if (statusChangeListener != null) statusChangeListener.onStatusChange(task, "DONE");
            });
        } else if ("IN_PROGRESS".equalsIgnoreCase(currentStatus)) {
            holder.tvStatusBadge.setText("● IN PROGRESS");
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_progress));
            holder.btnStatusToggle.setText("Review →");
            holder.btnStatusToggle.setOnClickListener(v -> {
                if (statusChangeListener != null) statusChangeListener.onStatusChange(task, "IN_REVIEW");
            });
        } else {
            holder.tvStatusBadge.setText("● TO DO");
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_todo));
            holder.btnStatusToggle.setText("Start →");
            holder.btnStatusToggle.setOnClickListener(v -> {
                if (statusChangeListener != null) statusChangeListener.onStatusChange(task, "IN_PROGRESS");
            });
        }

        // Card Click opens Task Details
        holder.cardTask.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTaskClick(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardTask;
        TextView tvIssueType, tvPriority, tvCommentsCount;
        TextView tvTitle, tvDesc, tvAssignedTo, tvStatusBadge;
        MaterialButton btnStatusToggle;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTask = itemView.findViewById(R.id.cardTask);
            tvIssueType = itemView.findViewById(R.id.tvTaskIssueType);
            tvPriority = itemView.findViewById(R.id.tvTaskPriority);
            tvCommentsCount = itemView.findViewById(R.id.tvTaskCommentsCount);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDesc = itemView.findViewById(R.id.tvTaskDesc);
            tvAssignedTo = itemView.findViewById(R.id.tvAssignedTo);
            tvStatusBadge = itemView.findViewById(R.id.tvTaskStatusBadge);
            btnStatusToggle = itemView.findViewById(R.id.btnStatusToggle);
        }
    }
}
