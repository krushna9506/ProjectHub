package com.projecthub.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.projecthub.R;
import com.projecthub.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ChatViewHolder> {

    public interface OnMessageActionListener {
        void onMessageOptions(ChatMessage message);
        void onReactionClick(ChatMessage message, String emoji);
    }

    private static final Pattern MENTION_HIGHLIGHT_PATTERN = Pattern.compile("@[A-Za-z0-9._-]+");

    private List<ChatMessage> messages = new ArrayList<>();
    private final String currentUserId;
    private final OnMessageActionListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    public ChatMessageAdapter(String currentUserId) {
        this(currentUserId, null);
    }

    public ChatMessageAdapter(String currentUserId, OnMessageActionListener listener) {
        this.currentUserId = currentUserId != null ? currentUserId : "";
        this.listener = listener;
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
        Context context = holder.itemView.getContext();
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
            int sentMentionColor = ContextCompat.getColor(context, R.color.on_primary);
            holder.tvMessageSent.setText(highlightMentions(msg.getMessage(), sentMentionColor));
            holder.tvTimeSent.setText(formattedTime);

            bindReplyQuote(holder.tvReplyQuoteSent, msg);
            bindReactions(context, holder.layoutReactionsSent, msg);

            View.OnLongClickListener longClick = v -> {
                if (listener != null) listener.onMessageOptions(msg);
                return true;
            };
            holder.bubbleSentContainer.setOnLongClickListener(longClick);
            holder.bubbleSentContainer.setOnClickListener(v -> {
                if (listener != null) listener.onMessageOptions(msg);
            });
        } else {
            holder.layoutSent.setVisibility(View.GONE);
            holder.layoutReceived.setVisibility(View.VISIBLE);

            String sender = msg.getSenderName() != null && !msg.getSenderName().isEmpty()
                    ? msg.getSenderName() : "Teammate";
            holder.tvSenderName.setText(sender);

            String initial = sender.substring(0, 1).toUpperCase(Locale.getDefault());
            holder.tvAvatarReceived.setText(initial);

            int mentionColor = ContextCompat.getColor(context, R.color.primary);
            holder.tvMessageReceived.setText(highlightMentions(msg.getMessage(), mentionColor));
            holder.tvTimeReceived.setText(formattedTime);

            bindReplyQuote(holder.tvReplyQuoteReceived, msg);
            bindReactions(context, holder.layoutReactionsReceived, msg);

            holder.bubbleReceivedContainer.setOnLongClickListener(v -> {
                if (listener != null) listener.onMessageOptions(msg);
                return true;
            });
            holder.bubbleReceivedContainer.setOnClickListener(v -> {
                if (listener != null) listener.onMessageOptions(msg);
            });
        }
    }

    private void bindReplyQuote(TextView tvQuote, ChatMessage msg) {
        if (msg.getReplyToPreview() != null && !msg.getReplyToPreview().isEmpty()) {
            tvQuote.setVisibility(View.VISIBLE);
            String who = msg.getReplyToSenderName() != null ? msg.getReplyToSenderName() : "Teammate";
            tvQuote.setText("Reply to " + who + ": " + msg.getReplyToPreview());
        } else {
            tvQuote.setVisibility(View.GONE);
        }
    }

    private void bindReactions(Context context, LinearLayout container, ChatMessage msg) {
        container.removeAllViews();
        Map<String, List<String>> reactions = msg.getReactions();
        if (reactions == null || reactions.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }
        boolean hasAny = false;
        for (Map.Entry<String, List<String>> entry : reactions.entrySet()) {
            List<String> users = entry.getValue();
            if (users == null || users.isEmpty()) continue;
            hasAny = true;

            TextView pill = new TextView(context);
            pill.setText(entry.getKey() + " " + users.size());
            pill.setTextSize(11f);
            boolean reactedByMe = users.contains(currentUserId);
            pill.setTextColor(ContextCompat.getColor(context, reactedByMe ? R.color.primary_dark : R.color.text_primary));
            pill.setBackgroundResource(reactedByMe ? R.drawable.bg_code_pill : R.drawable.bg_status_pill);
            pill.setPadding(14, 4, 14, 4);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMarginEnd(8);
            pill.setLayoutParams(params);
            pill.setOnClickListener(v -> {
                if (listener != null) listener.onReactionClick(msg, entry.getKey());
            });
            container.addView(pill);
        }
        container.setVisibility(hasAny ? View.VISIBLE : View.GONE);
    }

    private CharSequence highlightMentions(String text, int color) {
        if (text == null) return "";
        SpannableString spannable = new SpannableString(text);
        Matcher matcher = MENTION_HIGHLIGHT_PATTERN.matcher(text);
        while (matcher.find()) {
            spannable.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutSent, layoutReceived;
        LinearLayout bubbleSentContainer, bubbleReceivedContainer;
        LinearLayout layoutReactionsSent, layoutReactionsReceived;
        TextView tvMessageSent, tvTimeSent, tvReplyQuoteSent;
        TextView tvAvatarReceived, tvSenderName, tvMessageReceived, tvTimeReceived, tvReplyQuoteReceived;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutSent = itemView.findViewById(R.id.layoutSent);
            layoutReceived = itemView.findViewById(R.id.layoutReceived);
            bubbleSentContainer = itemView.findViewById(R.id.bubbleSentContainer);
            bubbleReceivedContainer = itemView.findViewById(R.id.bubbleReceivedContainer);
            layoutReactionsSent = itemView.findViewById(R.id.layoutReactionsSent);
            layoutReactionsReceived = itemView.findViewById(R.id.layoutReactionsReceived);
            tvReplyQuoteSent = itemView.findViewById(R.id.tvReplyQuoteSent);
            tvReplyQuoteReceived = itemView.findViewById(R.id.tvReplyQuoteReceived);
            tvMessageSent = itemView.findViewById(R.id.tvMessageSent);
            tvTimeSent = itemView.findViewById(R.id.tvTimeSent);
            tvAvatarReceived = itemView.findViewById(R.id.tvAvatarReceived);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvMessageReceived = itemView.findViewById(R.id.tvMessageReceived);
            tvTimeReceived = itemView.findViewById(R.id.tvTimeReceived);
        }
    }
}
