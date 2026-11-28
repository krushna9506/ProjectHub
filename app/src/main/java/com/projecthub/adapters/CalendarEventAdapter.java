package com.projecthub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.projecthub.R;
import com.projecthub.models.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarEventAdapter extends RecyclerView.Adapter<CalendarEventAdapter.EventViewHolder> {

    public static class CalendarEventItem {
        public final String kind; // "TASK" or "MILESTONE"
        public final String title;
        public final String status;
        public final long dueDate;
        public final Task task;

        public CalendarEventItem(String kind, String title, String status, long dueDate, Task task) {
            this.kind = kind;
            this.title = title;
            this.status = status;
            this.dueDate = dueDate;
            this.task = task;
        }
    }

    public interface OnEventClickListener {
        void onEventClick(CalendarEventItem item);
    }

    private List<CalendarEventItem> items = new ArrayList<>();
    private final OnEventClickListener listener;
    private final SimpleDateFormat fmt = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public CalendarEventAdapter(OnEventClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<CalendarEventItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        CalendarEventItem item = items.get(position);
        Context context = holder.itemView.getContext();

        boolean isMilestone = "MILESTONE".equalsIgnoreCase(item.kind);
        holder.tvKind.setText(isMilestone ? "MILESTONE" : "TASK");
        holder.tvKind.setBackgroundResource(isMilestone ? R.drawable.bg_badge_feature : R.drawable.bg_badge_task);
        holder.tvKind.setTextColor(ContextCompat.getColor(context, isMilestone ? R.color.issue_feature : R.color.issue_task));

        holder.tvTitle.setText(item.title);
        String dateStr = item.dueDate > 0 ? fmt.format(new Date(item.dueDate)) : "No date";
        holder.tvSubtitle.setText(dateStr + " • " + (item.status != null ? item.status : ""));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvKind, tvTitle, tvSubtitle;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKind = itemView.findViewById(R.id.tvCalendarItemKind);
            tvTitle = itemView.findViewById(R.id.tvCalendarItemTitle);
            tvSubtitle = itemView.findViewById(R.id.tvCalendarItemSubtitle);
        }
    }
}
