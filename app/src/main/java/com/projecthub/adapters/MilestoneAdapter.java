package com.projecthub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.projecthub.R;
import com.projecthub.models.Milestone;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MilestoneAdapter extends RecyclerView.Adapter<MilestoneAdapter.MilestoneViewHolder> {

    private List<Milestone> milestones = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public void setMilestones(List<Milestone> milestones) {
        this.milestones = milestones != null ? milestones : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MilestoneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_milestone, parent, false);
        return new MilestoneViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MilestoneViewHolder holder, int position) {
        Milestone milestone = milestones.get(position);
        holder.tvTitle.setText(milestone.getTitle());
        holder.tvStatus.setText(milestone.getStatus() != null ? milestone.getStatus() : "PENDING");
        
        if (milestone.getDueDate() > 0) {
            holder.tvDueDate.setText("Due: " + dateFormat.format(new Date(milestone.getDueDate())));
        } else {
            holder.tvDueDate.setText("No due date");
        }
    }

    @Override
    public int getItemCount() {
        return milestones.size();
    }

    static class MilestoneViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDueDate, tvStatus;

        public MilestoneViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvMilestoneTitle);
            tvDueDate = itemView.findViewById(R.id.tvMilestoneDueDate);
            tvStatus = itemView.findViewById(R.id.tvMilestoneStatus);
        }
    }
}
