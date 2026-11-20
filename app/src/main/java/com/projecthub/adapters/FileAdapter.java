package com.projecthub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        holder.tvFileName.setText(file.getName());
        holder.tvUploadedBy.setText("By: " + (file.getUploadedByName() != null ? file.getUploadedByName() : "Team"));

        holder.btnOpen.setOnClickListener(v -> {
            if (listener != null) listener.onOpenClick(file);
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName, tvUploadedBy;
        MaterialButton btnOpen;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvUploadedBy = itemView.findViewById(R.id.tvUploadedBy);
            btnOpen = itemView.findViewById(R.id.btnOpenFile);
        }
    }
}
