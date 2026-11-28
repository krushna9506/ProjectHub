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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    private List<Project> projects = new ArrayList<>();
    private final OnProjectClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

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

        String title = project.getTitle() != null ? project.getTitle() : "Untitled";
        holder.tvTitle.setText(title);

        String initial = !title.isEmpty() ? title.substring(0, 1).toUpperCase(Locale.getDefault()) : "P";
        holder.tvAvatar.setText(initial);

        String category = project.getCategory() != null && !project.getCategory().isEmpty()
                ? project.getCategory().toUpperCase(Locale.getDefault())
                : "GENERAL";
        holder.tvCategory.setText(category);

        String desc = project.getDescription() != null && !project.getDescription().isEmpty()
                ? project.getDescription()
                : "No project description provided.";
        holder.tvDesc.setText(desc);

        holder.tvJoinCode.setText(project.getJoinCode() != null ? project.getJoinCode() : "");

        int memberCount = project.getMembers() != null ? project.getMembers().size() : 1;
        holder.tvMembersCount.setText("👥 " + memberCount + (memberCount == 1 ? " member" : " members"));

        long deadline = project.getDeadline();
        if (deadline > 0) {
            long diff = deadline - System.currentTimeMillis();
            if (diff > 0) {
                long days = diff / (1000 * 60 * 60 * 24);
                holder.tvDeadline.setText("⏳ " + days + "d remaining");
            } else {
                holder.tvDeadline.setText("⚠️ Deadline passed");
            }
        } else {
            holder.tvDeadline.setText("⚡ Active workspace");
        }

        int progress = project.getProgressPercentage();
        holder.pbProgress.setProgress(progress);
        holder.tvProgressPercent.setText(progress + "%");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onProjectClick(project);
        });
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvTitle, tvCategory, tvJoinCode, tvDesc;
        TextView tvMembersCount, tvDeadline, tvProgressPercent;
        ProgressBar pbProgress;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvProjectAvatar);
            tvTitle = itemView.findViewById(R.id.tvProjectTitle);
            tvCategory = itemView.findViewById(R.id.tvProjectCategory);
            tvJoinCode = itemView.findViewById(R.id.tvJoinCode);
            tvDesc = itemView.findViewById(R.id.tvProjectDesc);
            tvMembersCount = itemView.findViewById(R.id.tvProjectMembersCount);
            tvDeadline = itemView.findViewById(R.id.tvProjectDeadline);
            pbProgress = itemView.findViewById(R.id.pbProjectProgress);
            tvProgressPercent = itemView.findViewById(R.id.tvProgressPercent);
        }
    }
}
