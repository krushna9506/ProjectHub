package com.projecthub.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.projecthub.R;
import com.projecthub.models.Subtask;

import java.util.ArrayList;
import java.util.List;

public class SubtaskAdapter extends RecyclerView.Adapter<SubtaskAdapter.SubtaskViewHolder> {

    public interface OnSubtaskActionListener {
        void onToggle(int position, boolean completed);
        void onDelete(int position);
    }

    private List<Subtask> subtasks = new ArrayList<>();
    private final OnSubtaskActionListener listener;

    public SubtaskAdapter(OnSubtaskActionListener listener) {
        this.listener = listener;
    }

    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks = subtasks != null ? new ArrayList<>(subtasks) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SubtaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subtask, parent, false);
        return new SubtaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubtaskViewHolder holder, int position) {
        Subtask item = subtasks.get(position);
        holder.cbDone.setOnCheckedChangeListener(null);
        holder.cbDone.setChecked(item.isCompleted());
        holder.tvTitle.setText(item.getTitle());

        if (item.isCompleted()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(0.6f);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setAlpha(1.0f);
        }

        holder.cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onToggle(pos, isChecked);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onDelete(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return subtasks.size();
    }

    static class SubtaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbDone;
        TextView tvTitle;
        ImageButton btnDelete;

        public SubtaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cbDone = itemView.findViewById(R.id.cbSubtaskDone);
            tvTitle = itemView.findViewById(R.id.tvSubtaskTitle);
            btnDelete = itemView.findViewById(R.id.btnDeleteSubtask);
        }
    }
}
