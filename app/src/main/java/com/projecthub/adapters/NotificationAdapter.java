package com.projecthub.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.projecthub.R;
import com.projecthub.models.AppNotification;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification);
    }

    private List<AppNotification> notifications = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setNotifications(List<AppNotification> list) {
        this.notifications = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        AppNotification notification = notifications.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(notification.getTitle());
        holder.tvMessage.setText(notification.getMessage());

        // Relative timestamp
        long time = notification.getTimestamp();
        if (time > 0) {
            CharSequence relative = DateUtils.getRelativeTimeSpanString(
                time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
            );
            holder.tvTime.setText(relative);
        } else {
            holder.tvTime.setText("Just now");
        }

        // Icon based on notification type
        String type = notification.getType() != null ? notification.getType() : "DEFAULT";
        switch (type) {
            case "JOIN_REQUEST":
                holder.tvIcon.setText("JR");
                holder.tvIcon.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.accent));
                break;
            case "JOIN_ACCEPTED":
                holder.tvIcon.setText("OK");
                holder.tvIcon.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.status_done));
                break;
            case "JOIN_REJECTED":
                holder.tvIcon.setText("--");
                holder.tvIcon.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.text_secondary));
                break;
            case "TASK":
            case "TASK_ASSIGNED":
                holder.tvIcon.setText("T");
                holder.tvIcon.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.primary));
                break;
            case "MENTION":
                holder.tvIcon.setText("@");
                holder.tvIcon.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.issue_feature));
                break;
            case "COMMENT":
                holder.tvIcon.setText("C");
                holder.tvIcon.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.accent));
                break;
            case "DEADLINE":
                holder.tvIcon.setText("!");
                holder.tvIcon.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.priority_high));
                break;
            case "TEAM_INVITE":
                holder.tvIcon.setText("IN");
                holder.tvIcon.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.status_done));
                break;
            case "AUTOMATION":
                holder.tvIcon.setText("AU");
                holder.tvIcon.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.phase_planning));
                break;
            case "CHAT":
                holder.tvIcon.setText("M");
                holder.tvIcon.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.issue_feature));
                break;
            case "DM":
                holder.tvIcon.setText("DM");
                holder.tvIcon.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.primary));
                break;
            case "GROUP_CHAT":
                holder.tvIcon.setText("GC");
                holder.tvIcon.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.accent));
                break;
            case "CALL":
                holder.tvIcon.setText("VC");
                holder.tvIcon.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.priority_urgent));
                break;
            default:
                holder.tvIcon.setText("N");
                holder.tvIcon.getBackground().mutate().setTint(ContextCompat.getColor(context, R.color.primary));
                break;
        }

        // Unread styling
        if (!notification.isRead()) {
            holder.viewUnreadDot.setVisibility(View.VISIBLE);
            holder.cardNotification.setStrokeColor(ContextCompat.getColor(context, R.color.primary));
            holder.cardNotification.setStrokeWidth(2);
        } else {
            holder.viewUnreadDot.setVisibility(View.GONE);
            holder.cardNotification.setStrokeColor(ContextCompat.getColor(context, R.color.divider));
            holder.cardNotification.setStrokeWidth(1);
        }

        holder.cardNotification.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(notification);
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardNotification;
        TextView tvIcon, tvTitle, tvMessage, tvTime;
        View viewUnreadDot;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNotification = itemView.findViewById(R.id.cardNotification);
            tvIcon = itemView.findViewById(R.id.tvNotificationIcon);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
        }
    }
}
