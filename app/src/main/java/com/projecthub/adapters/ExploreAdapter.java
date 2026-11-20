package com.projecthub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.projecthub.R;
import com.projecthub.models.Project;

import java.util.ArrayList;
import java.util.List;

public class ExploreAdapter extends RecyclerView.Adapter<ExploreAdapter.ExploreViewHolder> {

    public interface OnProjectClickListener {
        void onRequestJoin(Project project);
    }

    private List<Project> projectList = new ArrayList<>();
    private final OnProjectClickListener listener;
    private String currentUserId;

    public ExploreAdapter(String currentUserId, OnProjectClickListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setProjects(List<Project> projects) {
        this.projectList = projects != null ? projects : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExploreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_explore_project, parent, false);
        return new ExploreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExploreViewHolder holder, int position) {
        Project project = projectList.get(position);

        holder.tvProjectTitle.setText(project.getTitle());
        holder.tvLeadName.setText("Lead: " + (project.getLeadName() != null ? project.getLeadName() : "Unknown"));
        holder.tvProjectDesc.setText(project.getDescription());
        holder.tvCategory.setText(project.getCategory() != null ? project.getCategory().toUpperCase() : "GENERAL");

        if (project.getLookingFor() != null && !project.getLookingFor().isEmpty()) {
            holder.layoutLookingFor.setVisibility(View.VISIBLE);
            holder.tvLookingFor.setText(project.getLookingFor());
        } else {
            holder.layoutLookingFor.setVisibility(View.GONE);
        }

        // Disable button if current user is lead or already a member
        boolean isMemberOrLead = project.getMembers() != null && project.getMembers().containsKey(currentUserId);
        if (isMemberOrLead) {
            holder.btnRequestJoin.setText("Already Member");
            holder.btnRequestJoin.setEnabled(false);
        } else {
            holder.btnRequestJoin.setText("Request to Join");
            holder.btnRequestJoin.setEnabled(true);
            holder.btnRequestJoin.setOnClickListener(v -> {
                if (listener != null) listener.onRequestJoin(project);
            });
        }
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    static class ExploreViewHolder extends RecyclerView.ViewHolder {
        TextView tvProjectTitle, tvCategory, tvLeadName, tvProjectDesc, tvLookingFor;
        View layoutLookingFor;
        MaterialButton btnRequestJoin;

        public ExploreViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProjectTitle = itemView.findViewById(R.id.tvProjectTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvLeadName = itemView.findViewById(R.id.tvLeadName);
            tvProjectDesc = itemView.findViewById(R.id.tvProjectDesc);
            tvLookingFor = itemView.findViewById(R.id.tvLookingFor);
            layoutLookingFor = itemView.findViewById(R.id.layoutLookingFor);
            btnRequestJoin = itemView.findViewById(R.id.btnRequestJoin);
        }
    }
}
