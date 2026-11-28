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
        void onJoinDirectly(Project project);
    }

    private List<Project> projectList = new ArrayList<>();
    private final OnProjectClickListener listener;
    private final String currentUserId;

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

        holder.tvProjectTitle.setText(project.getTitle() != null ? project.getTitle() : "Untitled");
        String leadText = "Lead: " + (project.getLeadName() != null ? project.getLeadName() : "Team Lead");
        if (project.getMentorName() != null && !project.getMentorName().isEmpty()) {
            leadText += " • Mentor: " + project.getMentorName();
        }
        holder.tvLeadName.setText(leadText);
        holder.tvProjectDesc.setText(project.getDescription() != null ? project.getDescription() : "");
        holder.tvCategory.setText(project.getCategory() != null ? project.getCategory().toUpperCase() : "GENERAL");

        // Do not leak private join codes on public listings; users request to join or use an invite code directly
        holder.tvJoinCodeBadge.setVisibility(View.GONE);

        if (project.getLookingFor() != null && !project.getLookingFor().isEmpty()) {
            holder.layoutLookingFor.setVisibility(View.VISIBLE);
            holder.tvLookingFor.setText(project.getLookingFor());
        } else {
            holder.layoutLookingFor.setVisibility(View.GONE);
        }

        // Check membership status
        boolean isMemberOrLead = false;
        if (currentUserId != null && !currentUserId.isEmpty()) {
            if (currentUserId.equals(project.getLeadId())) {
                isMemberOrLead = true;
            } else if (project.getMembers() != null && project.getMembers().containsKey(currentUserId)) {
                isMemberOrLead = true;
            }
        }

        if (isMemberOrLead) {
            holder.layoutActionsRow.setVisibility(View.GONE);
            holder.btnAlreadyMember.setVisibility(View.VISIBLE);
        } else {
            holder.layoutActionsRow.setVisibility(View.VISIBLE);
            holder.btnAlreadyMember.setVisibility(View.GONE);
            holder.btnQuickJoin.setVisibility(View.GONE);

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
        TextView tvProjectTitle, tvCategory, tvLeadName, tvProjectDesc, tvLookingFor, tvJoinCodeBadge;
        View layoutLookingFor, layoutActionsRow;
        MaterialButton btnQuickJoin, btnRequestJoin, btnAlreadyMember;

        public ExploreViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProjectTitle = itemView.findViewById(R.id.tvProjectTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvLeadName = itemView.findViewById(R.id.tvLeadName);
            tvProjectDesc = itemView.findViewById(R.id.tvProjectDesc);
            tvLookingFor = itemView.findViewById(R.id.tvLookingFor);
            tvJoinCodeBadge = itemView.findViewById(R.id.tvJoinCodeBadge);
            layoutLookingFor = itemView.findViewById(R.id.layoutLookingFor);
            layoutActionsRow = itemView.findViewById(R.id.layoutActionsRow);
            btnQuickJoin = itemView.findViewById(R.id.btnQuickJoin);
            btnRequestJoin = itemView.findViewById(R.id.btnRequestJoin);
            btnAlreadyMember = itemView.findViewById(R.id.btnAlreadyMember);
        }
    }
}
