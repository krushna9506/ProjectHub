package com.projecthub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projecthub.R;
import com.projecthub.adapters.NotificationAdapter;
import com.projecthub.models.AppNotification;
import com.projecthub.viewmodel.DashboardViewModel;

public class NotificationsActivity extends AppCompatActivity {

    private DashboardViewModel dashboardViewModel;
    private NotificationAdapter adapter;
    private RecyclerView rvNotifications;
    private ProgressBar pbLoading;
    private TextView tvEmpty;
    private ImageButton btnBack;
    private MaterialButton btnClearAll;
    private String currentUserId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        btnBack = findViewById(R.id.btnBack);
        btnClearAll = findViewById(R.id.btnClearAll);
        rvNotifications = findViewById(R.id.rvNotifications);
        pbLoading = findViewById(R.id.pbNotificationsLoading);
        tvEmpty = findViewById(R.id.tvEmptyNotifications);

        btnBack.setOnClickListener(v -> finish());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        }

        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        setupRecyclerView();
        observeNotifications();

        btnClearAll.setOnClickListener(v -> {
            if (!currentUserId.isEmpty()) {
                dashboardViewModel.clearAllNotifications(currentUserId);
                Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(notification -> {
            // Mark as read
            if (!notification.isRead() && !currentUserId.isEmpty()) {
                dashboardViewModel.markNotificationAsRead(currentUserId, notification.getNotificationId());
            }

            String notifType = notification.getType() != null ? notification.getType() : "";
            String targetId = notification.getProjectId();
            if (targetId != null && !targetId.isEmpty()) {
                if ("DM".equalsIgnoreCase(notifType) || "GROUP_CHAT".equalsIgnoreCase(notifType)) {
                    Intent intent = new Intent(this, ChatRoomActivity.class);
                    intent.putExtra("CONVERSATION_ID", targetId);
                    intent.putExtra("CONVERSATION_TITLE", notification.getProjectTitle());
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(this, ProjectDetailsActivity.class);
                    intent.putExtra("PROJECT_ID", targetId);
                    intent.putExtra("PROJECT_TITLE", notification.getProjectTitle());
                    startActivity(intent);
                }
            }
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    private void observeNotifications() {
        if (currentUserId.isEmpty()) {
            pbLoading.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        dashboardViewModel.getInAppNotifications(currentUserId).observe(this, list -> {
            pbLoading.setVisibility(View.GONE);
            if (list == null || list.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                adapter.setNotifications(null);
            } else {
                tvEmpty.setVisibility(View.GONE);
                adapter.setNotifications(list);
            }
        });
    }
}
