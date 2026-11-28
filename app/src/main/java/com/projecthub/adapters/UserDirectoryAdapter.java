package com.projecthub.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.projecthub.R;
import com.projecthub.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserDirectoryAdapter extends RecyclerView.Adapter<UserDirectoryAdapter.UserViewHolder> {

    public interface OnUserInviteListener {
        void onInviteClick(User user);
    }

    public interface OnUserMessageListener {
        void onMessageClick(User user);
    }

    private List<User> users = new ArrayList<>();
    private final String currentUserId;
    private final OnUserInviteListener listener;
    private final OnUserMessageListener messageListener;

    public UserDirectoryAdapter(String currentUserId, OnUserInviteListener listener) {
        this(currentUserId, listener, null);
    }

    public UserDirectoryAdapter(String currentUserId, OnUserInviteListener listener,
                                OnUserMessageListener messageListener) {
        this.currentUserId = currentUserId != null ? currentUserId : "";
        this.listener = listener;
        this.messageListener = messageListener;
    }

    public void setUsers(List<User> users) {
        this.users = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_campus_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        String name = user.getName() != null && !user.getName().isEmpty() ? user.getName() : "Campus Member";
        holder.tvName.setText(name);
        holder.tvAvatar.setText(name.substring(0, 1).toUpperCase(Locale.getDefault()));

        boolean isFaculty = "FACULTY_MENTOR".equalsIgnoreCase(user.getRole());
        holder.tvRole.setText(isFaculty ? "FACULTY MENTOR" : "STUDENT");
        holder.tvRole.setTextColor(ContextCompat.getColor(
            holder.itemView.getContext(),
            isFaculty ? R.color.issue_feature : R.color.primary
        ));

        holder.tvVerified.setVisibility(user.isCampusVerified() ? View.VISIBLE : View.GONE);

        StringBuilder meta = new StringBuilder();
        if (user.getDepartment() != null && !user.getDepartment().isEmpty()) {
            meta.append(user.getDepartment());
        }
        if (user.getCollegeDomain() != null && !user.getCollegeDomain().isEmpty()) {
            if (meta.length() > 0) meta.append(" • ");
            meta.append(user.getCollegeDomain());
        }
        holder.tvMeta.setText(meta.length() > 0 ? meta.toString() : (user.getEmail() != null ? user.getEmail() : ""));

        if (user.getSkills() != null && !user.getSkills().isEmpty()) {
            holder.tvSkills.setVisibility(View.VISIBLE);
            holder.tvSkills.setText("Skills: " + TextUtils.join(" • ", user.getSkills()));
        } else {
            holder.tvSkills.setVisibility(View.GONE);
        }

        if (user.getBio() != null && !user.getBio().isEmpty()) {
            holder.tvBio.setVisibility(View.VISIBLE);
            holder.tvBio.setText(user.getBio());
        } else {
            holder.tvBio.setVisibility(View.GONE);
        }

        if (currentUserId.equals(user.getUserId())) {
            holder.layoutActions.setVisibility(View.GONE);
        } else {
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnInvite.setText(isFaculty ? "Invite Mentor" : "Invite");
            holder.btnInvite.setOnClickListener(v -> {
                if (listener != null) listener.onInviteClick(user);
            });
            if (holder.btnMessage != null) {
                holder.btnMessage.setVisibility(messageListener != null ? View.VISIBLE : View.GONE);
                holder.btnMessage.setOnClickListener(v -> {
                    if (messageListener != null) messageListener.onMessageClick(user);
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvVerified, tvMeta, tvRole, tvSkills, tvBio;
        MaterialButton btnInvite, btnMessage;
        View layoutActions;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvDirectoryAvatar);
            tvName = itemView.findViewById(R.id.tvDirectoryName);
            tvVerified = itemView.findViewById(R.id.tvDirectoryVerified);
            tvMeta = itemView.findViewById(R.id.tvDirectoryMeta);
            tvRole = itemView.findViewById(R.id.tvDirectoryRole);
            tvSkills = itemView.findViewById(R.id.tvDirectorySkills);
            tvBio = itemView.findViewById(R.id.tvDirectoryBio);
            btnInvite = itemView.findViewById(R.id.btnInviteToProject);
            btnMessage = itemView.findViewById(R.id.btnMessageUser);
            layoutActions = itemView.findViewById(R.id.layoutDirectoryActions);
        }
    }
}
