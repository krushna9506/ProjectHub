package com.projecthub.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.projecthub.R;
import com.projecthub.models.FileReference;

import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    public interface OnFileClickListener {
        void onOpenClick(FileReference file);
    }

    private List<FileReference> files = new ArrayList<>();
    private final OnFileClickListener listener;

    public FileAdapter(OnFileClickListener listener) {
        this.listener = listener;
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

        String type = file.getType() != null ? file.getType().toUpperCase() : "LINK";
        if ("CODE".equalsIgnoreCase(type)) {
            String lang = file.getLanguage() != null && !file.getLanguage().isEmpty() ? file.getLanguage() : "Code";
            holder.tvFileType.setText("CODE • " + lang.toUpperCase());
            holder.layoutCodeSnippet.setVisibility(View.VISIBLE);
            holder.tvCodePreview.setText(file.getSnippetContent() != null ? file.getSnippetContent() : "// Empty snippet");
            holder.btnCopyCode.setVisibility(View.VISIBLE);
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
        } else {
            holder.tvFileType.setText("RESOURCE LINK");
            holder.layoutCodeSnippet.setVisibility(View.GONE);
            holder.btnCopyCode.setVisibility(View.GONE);
            holder.btnOpen.setVisibility(View.VISIBLE);
            holder.btnOpen.setText("Open Resource ↗");
        }

        holder.btnOpen.setOnClickListener(v -> {
            if (listener != null) listener.onOpenClick(file);
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName, tvFileType, tvUploadedBy, tvCodePreview;
        LinearLayout layoutCodeSnippet;
        MaterialButton btnCopyCode, btnOpen;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileType = itemView.findViewById(R.id.tvFileType);
            tvUploadedBy = itemView.findViewById(R.id.tvUploadedBy);
            tvCodePreview = itemView.findViewById(R.id.tvCodeSnippetPreview);
            layoutCodeSnippet = itemView.findViewById(R.id.layoutCodeSnippet);
            btnCopyCode = itemView.findViewById(R.id.btnCopyCode);
            btnOpen = itemView.findViewById(R.id.btnOpenFile);
        }
    }
}
