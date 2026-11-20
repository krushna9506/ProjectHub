package com.projecthub.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.projecthub.R;
import com.projecthub.models.TaskComment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskCommentAdapter extends RecyclerView.Adapter<TaskCommentAdapter.CommentViewHolder> {

    private List<TaskComment> comments = new ArrayList<>();
    private final SimpleDateFormat fullDateFormat = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault());

    public void setComments(List<TaskComment> comments) {
        this.comments = comments != null ? comments : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        TaskComment comment = comments.get(position);
        
        String author = comment.getAuthorName() != null && !comment.getAuthorName().isEmpty() 
            ? comment.getAuthorName() : "Team Member";
        holder.tvAuthor.setText(author);

        String initial = author.substring(0, 1).toUpperCase(Locale.getDefault());
        holder.tvAvatar.setText(initial);

        holder.tvMessage.setText(comment.getMessage() != null ? comment.getMessage() : "");

        long time = comment.getTimestamp();
        if (time > 0) {
            long now = System.currentTimeMillis();
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                time, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
            );
            holder.tvTime.setText(relativeTime);
        } else {
            holder.tvTime.setText("Just now");
        }
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvAuthor, tvTime, tvMessage;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvCommentAvatar);
            tvAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvTime = itemView.findViewById(R.id.tvCommentTime);
            tvMessage = itemView.findViewById(R.id.tvCommentMessage);
        }
    }
}
