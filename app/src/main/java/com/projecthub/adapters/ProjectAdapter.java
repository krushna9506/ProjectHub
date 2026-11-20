package com.projecthub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.projecthub.R;
import com.projecthub.models.Project;

import java.util.ArrayList;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    private List<Project> projects = new ArrayList<>();
    private final OnProjectClickListener listener;

    public ProjectAdapter(OnProjectClickListener listener) {
        this.listener = listener;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects != null ? projects : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projects.get(position);
        holder.tvTitle.setText(project.getTitle());
        holder.tvDesc.setText(project.getDescription());
        holder.tvJoinCode.setText("Code: " + project.getJoinCode());
        holder.pbProgress.setProgress(project.getProgressPercentage());
        holder.tvProgressPercent.setText(project.getProgressPercentage() + "%");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProjectClick(project);
        });
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvJoinCode, tvProgressPercent;
        ProgressBar pbProgress;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvProjectTitle);
            tvDesc = itemView.findViewById(R.id.tvProjectDesc);
            tvJoinCode = itemView.findViewById(R.id.tvJoinCode);
            pbProgress = itemView.findViewById(R.id.pbProjectProgress);
            tvProgressPercent = itemView.findViewById(R.id.tvProgressPercent);
        }
    }
}
