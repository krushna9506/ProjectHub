package com.projecthub.ui;

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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projecthub.R;
import com.projecthub.adapters.JoinRequestAdapter;
import com.projecthub.models.JoinRequest;
import com.projecthub.viewmodel.DashboardViewModel;

public class ManageRequestsActivity extends AppCompatActivity {

    private DashboardViewModel dashboardViewModel;
    private JoinRequestAdapter requestAdapter;
    private RecyclerView rvJoinRequests;
    private ProgressBar pbRequestsLoading;
    private TextView tvEmptyRequests;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_requests);

        rvJoinRequests = findViewById(R.id.rvJoinRequests);
        pbRequestsLoading = findViewById(R.id.pbRequestsLoading);
        tvEmptyRequests = findViewById(R.id.tvEmptyRequests);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getUid() : "";

        requestAdapter = new JoinRequestAdapter(true, new JoinRequestAdapter.OnRequestListener() {
            @Override
            public void onAccept(JoinRequest request) {
                dashboardViewModel.acceptJoinRequest(request);
            }

            @Override
            public void onReject(JoinRequest request) {
                dashboardViewModel.rejectJoinRequest(request);
            }
        });

        rvJoinRequests.setLayoutManager(new LinearLayoutManager(this));
        rvJoinRequests.setAdapter(requestAdapter);

        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        dashboardViewModel.getJoinRequestsForLead(currentUserId).observe(this, requests -> {
            pbRequestsLoading.setVisibility(View.GONE);
            if (requests == null || requests.isEmpty()) {
                tvEmptyRequests.setVisibility(View.VISIBLE);
                requestAdapter.setRequests(null);
            } else {
                tvEmptyRequests.setVisibility(View.GONE);
                requestAdapter.setRequests(requests);
            }
        });

        dashboardViewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
