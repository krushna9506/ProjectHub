package com.projecthub.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.projecthub.R;
import com.projecthub.models.GitHubItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GitHubItemAdapter extends RecyclerView.Adapter<GitHubItemAdapter.GitHubViewHolder> {

    public interface OnGitHubItemActionListener {
        void onOpen(GitHubItem item);
        void onImportAsTask(GitHubItem item);
    }

    private List<GitHubItem> items = new ArrayList<>();
    private final OnGitHubItemActionListener listener;

    public GitHubItemAdapter(OnGitHubItemActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<GitHubItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GitHubViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_github, parent, false);
        return new GitHubViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GitHubViewHolder holder, int position) {
        GitHubItem item = items.get(position);
        Context context = holder.itemView.getContext();

        if (item.isPullRequest()) {
            holder.tvType.setText("PULL REQUEST");
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_feature);
            holder.tvType.setTextColor(ContextCompat.getColor(context, R.color.issue_feature));
        } else {
            holder.tvType.setText("ISSUE");
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_task);
            holder.tvType.setTextColor(ContextCompat.getColor(context, R.color.issue_task));
        }

        holder.tvNumber.setText("#" + item.getNumber());
        holder.tvTitle.setText(item.getTitle());

        boolean isOpen = "open".equalsIgnoreCase(item.getState());
        holder.tvState.setText(isOpen ? "OPEN" : "CLOSED");
        holder.tvState.setBackgroundResource(isOpen ? R.drawable.bg_status_done : R.drawable.bg_status_todo);
        holder.tvState.setTextColor(ContextCompat.getColor(context, isOpen ? R.color.status_done_text : R.color.status_todo_text));

        StringBuilder meta = new StringBuilder();
        if (item.getAuthor() != null && !item.getAuthor().isEmpty()) {
            meta.append("@").append(item.getAuthor());
        }
        if (item.getLabels() != null && !item.getLabels().isEmpty()) {
            if (meta.length() > 0) meta.append(" • ");
            meta.append(TextUtils.join(", ", item.getLabels()));
        }
        if (item.getUpdatedAt() != null && item.getUpdatedAt().length() >= 10) {
            if (meta.length() > 0) meta.append(" • ");
            meta.append(item.getUpdatedAt().substring(0, 10));
        }
        holder.tvMeta.setText(meta.toString().toLowerCase(Locale.getDefault()).isEmpty() ? "GitHub" : meta.toString());

        holder.btnOpen.setOnClickListener(v -> {
            if (listener != null) listener.onOpen(item);
        });
        holder.btnImport.setOnClickListener(v -> {
            if (listener != null) listener.onImportAsTask(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class GitHubViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvNumber, tvState, tvTitle, tvMeta;
        MaterialButton btnImport, btnOpen;

        public GitHubViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvGithubTypeBadge);
            tvNumber = itemView.findViewById(R.id.tvGithubNumber);
            tvState = itemView.findViewById(R.id.tvGithubState);
            tvTitle = itemView.findViewById(R.id.tvGithubItemTitle);
            tvMeta = itemView.findViewById(R.id.tvGithubMeta);
            btnImport = itemView.findViewById(R.id.btnGithubImportTask);
            btnOpen = itemView.findViewById(R.id.btnGithubOpenUrl);
        }
    }
}
