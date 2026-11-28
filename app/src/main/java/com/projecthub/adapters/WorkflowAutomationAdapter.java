package com.projecthub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.projecthub.R;
import com.projecthub.models.WorkflowRule;
import com.projecthub.utils.WorkflowAutomationEngine;

import java.util.ArrayList;
import java.util.List;

public class WorkflowAutomationAdapter extends RecyclerView.Adapter<WorkflowAutomationAdapter.RuleViewHolder> {

    public interface OnRuleActionListener {
        void onToggle(WorkflowRule rule, boolean enabled);
        void onDelete(WorkflowRule rule);
    }

    private List<WorkflowRule> rules = new ArrayList<>();
    private final OnRuleActionListener listener;

    public WorkflowAutomationAdapter(OnRuleActionListener listener) {
        this.listener = listener;
    }

    public void setRules(List<WorkflowRule> rules) {
        this.rules = rules != null ? rules : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_automation_rule, parent, false);
        return new RuleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RuleViewHolder holder, int position) {
        WorkflowRule rule = rules.get(position);
        holder.tvName.setText(rule.getName());
        holder.tvSummary.setText(WorkflowAutomationEngine.describeRule(rule));

        holder.switchEnabled.setOnCheckedChangeListener(null);
        holder.switchEnabled.setChecked(rule.isEnabled());
        holder.switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onToggle(rule, isChecked);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(rule);
        });
    }

    @Override
    public int getItemCount() {
        return rules.size();
    }

    static class RuleViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSummary;
        MaterialSwitch switchEnabled;
        ImageButton btnDelete;

        public RuleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRuleName);
            tvSummary = itemView.findViewById(R.id.tvRuleSummary);
            switchEnabled = itemView.findViewById(R.id.switchRuleEnabled);
            btnDelete = itemView.findViewById(R.id.btnDeleteRule);
        }
    }
}
