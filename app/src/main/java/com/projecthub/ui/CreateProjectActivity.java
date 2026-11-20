package com.projecthub.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.projecthub.R;
import com.projecthub.models.Project;
import com.projecthub.viewmodel.DashboardViewModel;

import java.util.Random;

public class CreateProjectActivity extends AppCompatActivity {

    private DashboardViewModel dashboardViewModel;
    private TextInputEditText etTitle, etDesc, etDeadlineDays, etCategory, etLookingFor;
    private MaterialSwitch switchPublic;
    private MaterialButton btnSubmitCreate;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_project);

        etTitle = findViewById(R.id.etProjectTitle);
        etDesc = findViewById(R.id.etProjectDesc);
        etDeadlineDays = findViewById(R.id.etDeadlineDays);
        etCategory = findViewById(R.id.etCategory);
        etLookingFor = findViewById(R.id.etLookingFor);
        switchPublic = findViewById(R.id.switchPublic);
        btnSubmitCreate = findViewById(R.id.btnSubmitCreate);
        progressBar = findViewById(R.id.progressBar);

        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        dashboardViewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnSubmitCreate.setEnabled(!isLoading);
        });

        dashboardViewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null && msg.contains("created")) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                finish();
            } else if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });

        btnSubmitCreate.setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
            String daysStr = etDeadlineDays.getText() != null ? etDeadlineDays.getText().toString().trim() : "30";
            String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "General";
            String lookingFor = etLookingFor.getText() != null ? etLookingFor.getText().toString().trim() : "";
            boolean isPublic = switchPublic.isChecked();

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Title and description are required", Toast.LENGTH_SHORT).show();
                return;
            }

            int days = 30;
            try {
                days = Integer.parseInt(daysStr);
            } catch (NumberFormatException ignored) {}

            long deadline = System.currentTimeMillis() + ((long) days * 24 * 60 * 60 * 1000L);
            String joinCode = generateJoinCode(6);
            String leadId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                            FirebaseAuth.getInstance().getCurrentUser().getUid() : "unknown";
            String leadName = FirebaseAuth.getInstance().getCurrentUser() != null && 
                              FirebaseAuth.getInstance().getCurrentUser().getDisplayName() != null ?
                              FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : "Lead";

            Project project = new Project(null, title, desc, joinCode, leadId, leadName, deadline);
            project.setPublic(isPublic);
            project.setCategory(category.isEmpty() ? "General" : category);
            project.setLookingFor(lookingFor);

            String userEmail = FirebaseAuth.getInstance().getCurrentUser() != null && 
                               FirebaseAuth.getInstance().getCurrentUser().getEmail() != null ?
                               FirebaseAuth.getInstance().getCurrentUser().getEmail() : "";
            if (userEmail.contains("@")) {
                project.setCollegeDomain(userEmail.substring(userEmail.indexOf("@") + 1));
            }

            dashboardViewModel.createProject(project);
        });
    }

    private String generateJoinCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Removed confusing chars like O, 0, 1, I
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
