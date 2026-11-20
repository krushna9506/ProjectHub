package com.projecthub.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.projecthub.R;
import com.projecthub.models.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskStatusChangeListener {
        void onStatusChange(Task task, String newStatus);
    }

    private List<Task> tasks = new ArrayList<>();
    private final OnTaskStatusChangeListener listener;

    public TaskAdapter(OnTaskStatusChangeListener listener) {
        this.listener = listener;
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
        holder.tvTitle.setText(task.getTitle());
        holder.tvDesc.setText(task.getDescription());
        holder.tvPriority.setText(task.getPriority() != null ? task.getPriority() : "MED");
        
        String assignedText = task.getAssignedToName() != null && !task.getAssignedToName().isEmpty() 
            ? "Assigned: " + task.getAssignedToName() : "Unassigned";
        holder.tvAssignedTo.setText(assignedText);

        // Status-dependent UI & transitions
        String currentStatus = task.getStatus() != null ? task.getStatus() : "TODO";
        if ("DONE".equalsIgnoreCase(currentStatus)) {
            holder.btnStatusToggle.setText("✓ Completed");
            holder.btnStatusToggle.setOnClickListener(v -> {
                if (listener != null) listener.onStatusChange(task, "TODO");
            });
        } else if ("IN_PROGRESS".equalsIgnoreCase(currentStatus)) {
            holder.btnStatusToggle.setText("Mark Done");
            holder.btnStatusToggle.setOnClickListener(v -> {
                if (listener != null) listener.onStatusChange(task, "DONE");
            });
        } else {
            holder.btnStatusToggle.setText("Start Task");
            holder.btnStatusToggle.setOnClickListener(v -> {
                if (listener != null) listener.onStatusChange(task, "IN_PROGRESS");
            });
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvPriority, tvAssignedTo;
        MaterialButton btnStatusToggle;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDesc = itemView.findViewById(R.id.tvTaskDesc);
            tvPriority = itemView.findViewById(R.id.tvTaskPriority);
            tvAssignedTo = itemView.findViewById(R.id.tvAssignedTo);
            btnStatusToggle = itemView.findViewById(R.id.btnStatusToggle);
        }
    }
}
