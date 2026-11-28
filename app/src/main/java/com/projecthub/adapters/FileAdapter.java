package com.projecthub.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.projecthub.R;
import com.projecthub.models.FileReference;

import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    public interface OnFileClickListener {
        void onOpenClick(FileReference file);
        default void onDeleteClick(FileReference file) {}
    }

    private List<FileReference> files = new ArrayList<>();
    private final OnFileClickListener listener;
    private String currentUserId = "";
    private String projectLeadId = "";

    public FileAdapter(OnFileClickListener listener) {
        this.listener = listener;
    }

    public void setCurrentUserContext(String currentUserId, String projectLeadId) {
        this.currentUserId = currentUserId != null ? currentUserId : "";
        this.projectLeadId = projectLeadId != null ? projectLeadId : "";
        notifyDataSetChanged();
    }

    public void setFiles(List<FileReference> files) {
        this.files = files != null ? files : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileReference file = files.get(position);
        Context context = holder.itemView.getContext();

        holder.tvFileName.setText(file.getName());
        holder.tvUploadedBy.setText("Shared by: " + (file.getUploadedByName() != null ? file.getUploadedByName() : "Team Member"));

        String access = file.getAccessLevel();
        if ("LEAD_ONLY".equalsIgnoreCase(access)) {
            holder.tvAccessLevel.setText("LEAD ONLY");
        } else if ("PUBLIC".equalsIgnoreCase(access)) {
            holder.tvAccessLevel.setText("PUBLIC");
        } else {
            holder.tvAccessLevel.setText("MEMBERS ONLY");
        }

        if (file.getFileSizeBytes() > 0) {
            holder.tvFileSize.setVisibility(View.VISIBLE);
            holder.tvFileSize.setText(Formatter.formatShortFileSize(context, file.getFileSizeBytes()));
        } else {
            holder.tvFileSize.setVisibility(View.GONE);
        }

        boolean isLead = !currentUserId.isEmpty() && currentUserId.equals(projectLeadId);
        boolean isUploader = !currentUserId.isEmpty() && currentUserId.equals(file.getUploadedById());
        boolean restrictedOut = "LEAD_ONLY".equalsIgnoreCase(access) && !isLead && !isUploader;

        holder.btnDelete.setVisibility((isLead || isUploader) ? View.VISIBLE : View.GONE);
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(file);
        });

        String type = file.getType() != null ? file.getType().toUpperCase() : "LINK";
        if ("IMAGE".equalsIgnoreCase(type) && !restrictedOut && file.getUrl() != null && !file.getUrl().isEmpty()) {
            holder.ivPreview.setVisibility(View.VISIBLE);
            Glide.with(context).load(file.getUrl()).centerCrop().into(holder.ivPreview);
        } else {
            holder.ivPreview.setVisibility(View.GONE);
        }

        if ("CODE".equalsIgnoreCase(type)) {
            String lang = file.getLanguage() != null && !file.getLanguage().isEmpty() ? file.getLanguage() : "Code";
            holder.tvFileType.setText("CODE • " + lang.toUpperCase());
            holder.layoutCodeSnippet.setVisibility(restrictedOut ? View.GONE : View.VISIBLE);
            holder.tvCodePreview.setText(file.getSnippetContent() != null ? file.getSnippetContent() : "// Empty snippet");
            holder.btnCopyCode.setVisibility(restrictedOut ? View.GONE : View.VISIBLE);
            holder.btnCopyCode.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Code Snippet", file.getSnippetContent());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show();
            });

            if (file.getUrl() != null && !file.getUrl().isEmpty()) {
                holder.btnOpen.setVisibility(View.VISIBLE);
                holder.btnOpen.setText("Open Link ↗");
            } else {
                holder.btnOpen.setVisibility(View.GONE);
            }
        } else if ("GITHUB".equalsIgnoreCase(type)) {
            holder.tvFileType.setText("GITHUB REPO");
            holder.layoutCodeSnippet.setVisibility(View.GONE);
            holder.btnCopyCode.setVisibility(View.GONE);
            holder.btnOpen.setVisibility(View.VISIBLE);
            holder.btnOpen.setText("View on GitHub ↗");
        } else if ("IMAGE".equalsIgnoreCase(type)) {
            holder.tvFileType.setText("PROJECT IMAGE");
            holder.layoutCodeSnippet.setVisibility(View.GONE);
            holder.btnCopyCode.setVisibility(View.GONE);
            holder.btnOpen.setVisibility(View.VISIBLE);
            holder.btnOpen.setText("View Image");
        } else if ("DOCUMENT".equalsIgnoreCase(type)) {
            holder.tvFileType.setText("PROJECT DOC");
            holder.layoutCodeSnippet.setVisibility(View.GONE);
            holder.btnCopyCode.setVisibility(View.GONE);
            holder.btnOpen.setVisibility(View.VISIBLE);
            holder.btnOpen.setText("Download Document");
        } else {
            holder.tvFileType.setText("RESOURCE LINK");
            holder.layoutCodeSnippet.setVisibility(View.GONE);
            holder.btnCopyCode.setVisibility(View.GONE);
            holder.btnOpen.setVisibility(View.VISIBLE);
            holder.btnOpen.setText("Open Resource");
        }

        holder.btnOpen.setOnClickListener(v -> {
            if (restrictedOut) {
                Toast.makeText(context, "Access restricted: Lead Only file.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) listener.onOpenClick(file);
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName, tvFileType, tvUploadedBy, tvCodePreview, tvAccessLevel, tvFileSize;
        LinearLayout layoutCodeSnippet;
        ImageView ivPreview;
        MaterialButton btnCopyCode, btnOpen, btnDelete;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileType = itemView.findViewById(R.id.tvFileType);
            tvUploadedBy = itemView.findViewById(R.id.tvUploadedBy);
            tvAccessLevel = itemView.findViewById(R.id.tvFileAccessLevel);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            ivPreview = itemView.findViewById(R.id.ivFilePreview);
            tvCodePreview = itemView.findViewById(R.id.tvCodeSnippetPreview);
            layoutCodeSnippet = itemView.findViewById(R.id.layoutCodeSnippet);
            btnCopyCode = itemView.findViewById(R.id.btnCopyCode);
            btnOpen = itemView.findViewById(R.id.btnOpenFile);
            btnDelete = itemView.findViewById(R.id.btnDeleteFile);
        }
    }
}
