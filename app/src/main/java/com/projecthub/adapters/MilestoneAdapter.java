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
import com.projecthub.R;
import com.projecthub.models.Milestone;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MilestoneAdapter extends RecyclerView.Adapter<MilestoneAdapter.MilestoneViewHolder> {

    public interface OnMilestoneStatusChangeListener {
        void onStatusChange(Milestone milestone, String newStatus);
    }

    private List<Milestone> milestones = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final OnMilestoneStatusChangeListener listener;

    public MilestoneAdapter(OnMilestoneStatusChangeListener listener) {
        this.listener = listener;
    }

    public MilestoneAdapter() {
        this(null);
    }

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
        Context context = holder.itemView.getContext();

        holder.tvNode.setText(String.valueOf(position + 1));
        holder.tvTitle.setText(milestone.getTitle());

        if (milestone.getDueDate() > 0) {
            holder.tvDueDate.setText("Target: " + dateFormat.format(new Date(milestone.getDueDate())));
        } else {
            holder.tvDueDate.setText("No deadline set");
        }

        String status = milestone.getStatus() != null ? milestone.getStatus() : "PENDING";
        if ("COMPLETED".equalsIgnoreCase(status) || "DONE".equalsIgnoreCase(status)) {
            holder.tvNode.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.status_done));
            holder.btnStatus.setText("✓ Done");
            holder.btnStatus.setOnClickListener(v -> {
                if (listener != null) listener.onStatusChange(milestone, "PENDING");
            });
        } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            holder.tvNode.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.status_progress));
            holder.btnStatus.setText("Complete →");
            holder.btnStatus.setOnClickListener(v -> {
                if (listener != null) listener.onStatusChange(milestone, "COMPLETED");
            });
        } else {
            holder.tvNode.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.status_todo));
            holder.btnStatus.setText("Start →");
            holder.btnStatus.setOnClickListener(v -> {
                if (listener != null) listener.onStatusChange(milestone, "IN_PROGRESS");
            });
        }
    }

    @Override
    public int getItemCount() {
        return milestones.size();
    }

    static class MilestoneViewHolder extends RecyclerView.ViewHolder {
        TextView tvNode, tvTitle, tvDueDate;
        MaterialButton btnStatus;

        public MilestoneViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNode = itemView.findViewById(R.id.tvMilestoneNode);
            tvTitle = itemView.findViewById(R.id.tvMilestoneTitle);
            tvDueDate = itemView.findViewById(R.id.tvMilestoneDueDate);
            btnStatus = itemView.findViewById(R.id.btnMilestoneStatus);
        }
    }
}
