package com.projecthub.adapters;

import android.content.Context;
import android.graphics.Typeface;
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
import com.projecthub.models.Conversation;
import com.projecthub.utils.ConversationUtils;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConvViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    private List<Conversation> conversations = new ArrayList<>();
    private final String currentUserId;
    private final OnConversationClickListener listener;

    public ConversationAdapter(String currentUserId, OnConversationClickListener listener) {
        this.currentUserId = currentUserId != null ? currentUserId : "";
        this.listener = listener;
    }

    public void setConversations(List<Conversation> list) {
        this.conversations = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConvViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ConvViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConvViewHolder holder, int position) {
        Conversation conv = conversations.get(position);
        Context context = holder.itemView.getContext();

        String displayTitle = ConversationUtils.resolveDisplayTitle(conv, currentUserId);
        holder.tvTitle.setText(displayTitle);
        holder.tvAvatar.setText(ConversationUtils.buildAvatarInitials(displayTitle));

        boolean isGroup = conv.isGroup();
        holder.tvAvatar.getBackground().mutate().setTint(ContextCompat.getColor(
            context,
            isGroup ? R.color.accent : R.color.primary
        ));

        if (isGroup) {
            holder.tvTypeBadge.setText("GROUP • " + conv.getParticipantIds().size());
            holder.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_feature);
            holder.tvTypeBadge.setTextColor(ContextCompat.getColor(context, R.color.issue_feature));
        } else {
            holder.tvTypeBadge.setText("1:1");
            holder.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_task);
            holder.tvTypeBadge.setTextColor(ContextCompat.getColor(context, R.color.issue_task));
        }

        long ts = Math.max(conv.getLastMessageTimestamp(), conv.getCreatedAt());
        if (ts > 0) {
            holder.tvTime.setText(DateUtils.getRelativeTimeSpanString(
                ts, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
            ));
        } else {
            holder.tvTime.setText("");
        }

        String preview = conv.getLastMessage();
        if (preview.isEmpty()) {
            holder.tvLastMessage.setText(isGroup
                    ? ConversationUtils.formatSubtitle(conv)
                    : "Tap to start chatting");
        } else if (currentUserId.equals(conv.getLastMessageSenderId()) && !isGroup) {
            holder.tvLastMessage.setText("You: " + preview);
        } else if (isGroup && !conv.getLastMessageSenderName().isEmpty()
                && !preview.startsWith(conv.getLastMessageSenderName())) {
            holder.tvLastMessage.setText(conv.getLastMessageSenderName() + ": " + preview);
        } else {
            holder.tvLastMessage.setText(preview);
        }

        boolean unread = ConversationUtils.isUnread(conv, currentUserId);
        if (unread) {
            holder.viewUnreadDot.setVisibility(View.VISIBLE);
            holder.tvLastMessage.setTypeface(null, Typeface.BOLD);
            holder.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            holder.cardConversation.setStrokeColor(ContextCompat.getColor(context, R.color.primary));
            holder.cardConversation.setStrokeWidth(2);
        } else {
            holder.viewUnreadDot.setVisibility(View.GONE);
            holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
            holder.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.cardConversation.setStrokeColor(ContextCompat.getColor(context, R.color.divider));
            holder.cardConversation.setStrokeWidth(1);
        }

        holder.cardConversation.setOnClickListener(v -> {
            if (listener != null) listener.onConversationClick(conv);
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ConvViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardConversation;
        TextView tvAvatar, tvTitle, tvTypeBadge, tvTime, tvLastMessage;
        View viewUnreadDot;

        public ConvViewHolder(@NonNull View itemView) {
            super(itemView);
            cardConversation = itemView.findViewById(R.id.cardConversation);
            tvAvatar = itemView.findViewById(R.id.tvConvAvatar);
            tvTitle = itemView.findViewById(R.id.tvConvTitle);
            tvTypeBadge = itemView.findViewById(R.id.tvConvTypeBadge);
            tvTime = itemView.findViewById(R.id.tvConvTime);
            tvLastMessage = itemView.findViewById(R.id.tvConvLastMessage);
            viewUnreadDot = itemView.findViewById(R.id.viewConvUnreadDot);
        }
    }
}
