package com.projecthub.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.projecthub.R;
import com.projecthub.models.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GanttAdapter extends RecyclerView.Adapter<GanttAdapter.GanttViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    private List<Task> tasks = new ArrayList<>();
    private final OnTaskClickListener listener;

    public GanttAdapter(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GanttViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gantt_task, parent, false);
        return new GanttViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GanttViewHolder holder, int position) {
        Task task = tasks.get(position);
        Context context = holder.itemView.getContext();

        String key = task.getTaskKey() != null && !task.getTaskKey().isEmpty() ? 
            task.getTaskKey() : "TS-" + (100 + position);
        holder.tvTaskId.setText(key);

        String phase = task.getPhase() != null && !task.getPhase().isEmpty() ? 
            task.getPhase().toUpperCase(Locale.getDefault()) : "DEVELOPMENT";
        holder.tvPhase.setText(phase);

        int phaseColor = ContextCompat.getColor(context, R.color.phase_dev);
        if (phase.contains("PLAN")) {
            phaseColor = ContextCompat.getColor(context, R.color.phase_planning);
        } else if (phase.contains("DESIGN")) {
            phaseColor = ContextCompat.getColor(context, R.color.phase_design);
        } else if (phase.contains("DEV")) {
            phaseColor = ContextCompat.getColor(context, R.color.phase_dev);
        } else if (phase.contains("REV") || phase.contains("TEST")) {
            phaseColor = ContextCompat.getColor(context, R.color.phase_review);
        }
        holder.tvPhase.setTextColor(phaseColor);

        holder.tvTaskName.setText(task.getTitle());
        holder.tvAssignee.setText((task.getAssignedToName() != null && !task.getAssignedToName().isEmpty() ? 
            task.getAssignedToName() : "Unassigned"));

        double time = task.getTimeSpentHours();
        holder.tvTimeSpent.setText(String.format(Locale.US, "%.1fh", time));

        String status = task.getStatus() != null ? task.getStatus().toUpperCase(Locale.getDefault()) : "TODO";
        int progress;
        int barColor;
        int statusTextColor;
        String statusLabel;

        if ("DONE".equals(status)) {
            progress = 100;
            barColor = ContextCompat.getColor(context, R.color.status_done);
            statusTextColor = ContextCompat.getColor(context, R.color.status_done_text);
            statusLabel = "DONE (100%)";
        } else if ("IN_REVIEW".equals(status)) {
            progress = 80;
            barColor = ContextCompat.getColor(context, R.color.status_review);
            statusTextColor = ContextCompat.getColor(context, R.color.status_review_text);
            statusLabel = "REVIEW (80%)";
        } else if ("IN_PROGRESS".equals(status)) {
            progress = 55;
            barColor = ContextCompat.getColor(context, R.color.status_progress);
            statusTextColor = ContextCompat.getColor(context, R.color.status_progress_text);
            statusLabel = "IN PROGRESS (55%)";
        } else if ("BLOCKED".equals(status)) {
            progress = 25;
            barColor = ContextCompat.getColor(context, R.color.status_blocked);
            statusTextColor = ContextCompat.getColor(context, R.color.status_blocked_text);
            statusLabel = "BLOCKED";
        } else {
            progress = 15;
            barColor = ContextCompat.getColor(context, R.color.status_todo);
            statusTextColor = ContextCompat.getColor(context, R.color.status_todo_text);
            statusLabel = "TO DO (15%)";
        }

        holder.pbTimeline.setProgress(progress);
        holder.pbTimeline.setProgressTintList(ColorStateList.valueOf(barColor));
        holder.tvStatus.setText(statusLabel);
        holder.tvStatus.setTextColor(statusTextColor);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(task);
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class GanttViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskId, tvPhase, tvTaskName, tvAssignee, tvTimeSpent, tvStatus;
        ProgressBar pbTimeline;

        public GanttViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskId = itemView.findViewById(R.id.tvGanttTaskId);
            tvPhase = itemView.findViewById(R.id.tvGanttPhase);
            tvTaskName = itemView.findViewById(R.id.tvGanttTaskName);
            tvAssignee = itemView.findViewById(R.id.tvGanttAssignee);
            tvTimeSpent = itemView.findViewById(R.id.tvGanttTimeSpent);
            tvStatus = itemView.findViewById(R.id.tvGanttStatus);
            pbTimeline = itemView.findViewById(R.id.pbGanttTimeline);
        }
    }
}
