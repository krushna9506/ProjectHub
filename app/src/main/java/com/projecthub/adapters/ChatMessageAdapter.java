package com.projecthub.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.projecthub.R;
import com.projecthub.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ChatViewHolder> {

    private List<ChatMessage> messages = new ArrayList<>();
    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    public ChatMessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId != null ? currentUserId : "";
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        boolean isSent = currentUserId.equals(msg.getSenderId());

        String formattedTime = "";
        if (msg.getTimestamp() > 0) {
            Date date = new Date(msg.getTimestamp());
            if (DateUtils.isToday(msg.getTimestamp())) {
                formattedTime = timeFormat.format(date);
            } else {
                formattedTime = dateTimeFormat.format(date);
            }
        }

        if (isSent) {
            holder.layoutSent.setVisibility(View.VISIBLE);
            holder.layoutReceived.setVisibility(View.GONE);
            holder.tvMessageSent.setText(msg.getMessage());
            holder.tvTimeSent.setText(formattedTime);
        } else {
            holder.layoutSent.setVisibility(View.GONE);
            holder.layoutReceived.setVisibility(View.VISIBLE);

            String sender = msg.getSenderName() != null && !msg.getSenderName().isEmpty() 
                ? msg.getSenderName() : "Teammate";
            holder.tvSenderName.setText(sender);

            String initial = sender.substring(0, 1).toUpperCase(Locale.getDefault());
            holder.tvAvatarReceived.setText(initial);

            holder.tvMessageReceived.setText(msg.getMessage());
            holder.tvTimeReceived.setText(formattedTime);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutSent, layoutReceived;
        TextView tvMessageSent, tvTimeSent;
        TextView tvAvatarReceived, tvSenderName, tvMessageReceived, tvTimeReceived;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutSent = itemView.findViewById(R.id.layoutSent);
            layoutReceived = itemView.findViewById(R.id.layoutReceived);
            tvMessageSent = itemView.findViewById(R.id.tvMessageSent);
            tvTimeSent = itemView.findViewById(R.id.tvTimeSent);
            tvAvatarReceived = itemView.findViewById(R.id.tvAvatarReceived);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvMessageReceived = itemView.findViewById(R.id.tvMessageReceived);
            tvTimeReceived = itemView.findViewById(R.id.tvTimeReceived);
        }
    }
}
