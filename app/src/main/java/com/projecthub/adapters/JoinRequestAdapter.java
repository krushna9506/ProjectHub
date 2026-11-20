package com.projecthub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.projecthub.R;
import com.projecthub.models.JoinRequest;

import java.util.ArrayList;
import java.util.List;

public class JoinRequestAdapter extends RecyclerView.Adapter<JoinRequestAdapter.RequestViewHolder> {

    public interface OnRequestListener {
        void onAccept(JoinRequest request);
        void onReject(JoinRequest request);
    }

    private List<JoinRequest> requestList = new ArrayList<>();
    private final OnRequestListener listener;
    private final boolean isLeadView;

    public JoinRequestAdapter(boolean isLeadView, OnRequestListener listener) {
        this.isLeadView = isLeadView;
        this.listener = listener;
    }

    public void setRequests(List<JoinRequest> requests) {
        this.requestList = requests != null ? requests : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_join_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        JoinRequest request = requestList.get(position);

        holder.tvApplicantName.setText(request.getApplicantName() != null ? request.getApplicantName() : "Applicant");
        holder.tvApplicantEmail.setText(request.getApplicantEmail() != null ? request.getApplicantEmail() : "");
        holder.tvRequestProjectTitle.setText("For Project: " + (request.getProjectTitle() != null ? request.getProjectTitle() : ""));
        holder.tvRequestMessage.setText(request.getMessage() != null && !request.getMessage().isEmpty() ? 
                                        request.getMessage() : "(No message provided)");

        String status = request.getStatus() != null ? request.getStatus() : "PENDING";
        holder.tvRequestStatus.setText(status);

        if ("ACCEPTED".equalsIgnoreCase(status)) {
            holder.tvRequestStatus.setTextColor(holder.itemView.getContext().getColor(R.color.status_done));
            holder.layoutActionButtons.setVisibility(View.GONE);
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            holder.tvRequestStatus.setTextColor(holder.itemView.getContext().getColor(R.color.priority_high));
            holder.layoutActionButtons.setVisibility(View.GONE);
        } else {
            holder.tvRequestStatus.setTextColor(holder.itemView.getContext().getColor(R.color.primary));
            if (isLeadView) {
                holder.layoutActionButtons.setVisibility(View.VISIBLE);
                holder.btnAccept.setOnClickListener(v -> {
                    if (listener != null) listener.onAccept(request);
                });
                holder.btnReject.setOnClickListener(v -> {
                    if (listener != null) listener.onReject(request);
                });
            } else {
                holder.layoutActionButtons.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvApplicantName, tvApplicantEmail, tvRequestProjectTitle, tvRequestMessage, tvRequestStatus;
        View layoutActionButtons;
        MaterialButton btnAccept, btnReject;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvApplicantName = itemView.findViewById(R.id.tvApplicantName);
            tvApplicantEmail = itemView.findViewById(R.id.tvApplicantEmail);
            tvRequestProjectTitle = itemView.findViewById(R.id.tvRequestProjectTitle);
            tvRequestMessage = itemView.findViewById(R.id.tvRequestMessage);
            tvRequestStatus = itemView.findViewById(R.id.tvRequestStatus);
            layoutActionButtons = itemView.findViewById(R.id.layoutActionButtons);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
