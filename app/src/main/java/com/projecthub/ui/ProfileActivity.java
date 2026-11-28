package com.projecthub.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.projecthub.R;
import com.projecthub.models.User;
import com.projecthub.utils.CampusVerifier;
import com.projecthub.viewmodel.AuthViewModel;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private User currentUserObj;

    private TextView tvVerificationStatus, tvVerificationDomain;
    private TextInputEditText etName, etDept, etRoll, etSkills, etGithub, etBio;
    private RadioGroup rgRole;
    private RadioButton rbStudent, rbFaculty;
    private MaterialButton btnSendVerification, btnRefreshVerification, btnSaveProfile;
    private ProgressBar pbProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvVerificationStatus = findViewById(R.id.tvVerificationStatus);
        tvVerificationDomain = findViewById(R.id.tvVerificationDomain);
        etName = findViewById(R.id.etProfileName);
        etDept = findViewById(R.id.etProfileDepartment);
        etRoll = findViewById(R.id.etProfileRoll);
        etSkills = findViewById(R.id.etProfileSkills);
        etGithub = findViewById(R.id.etProfileGithub);
        etBio = findViewById(R.id.etProfileBio);
        rgRole = findViewById(R.id.rgProfileRole);
        rbStudent = findViewById(R.id.rbProfileStudent);
        rbFaculty = findViewById(R.id.rbProfileFaculty);
        btnSendVerification = findViewById(R.id.btnSendVerification);
        btnRefreshVerification = findViewById(R.id.btnRefreshVerification);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        pbProfile = findViewById(R.id.pbProfile);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        authViewModel.fetchCurrentUserProfile();

        authViewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                currentUserObj = user;
                bindUser(user);
            }
        });

        authViewModel.getIsLoading().observe(this, loading -> {
            pbProfile.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnSaveProfile.setEnabled(!loading);
        });

        authViewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        authViewModel.getErrorMessage().observe(this, err -> {
            if (err != null && !err.isEmpty()) {
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
            }
        });

        btnSendVerification.setOnClickListener(v -> authViewModel.sendVerificationEmail());
        btnRefreshVerification.setOnClickListener(v -> authViewModel.syncVerificationStatus());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void bindUser(User user) {
        etName.setText(user.getName());
        etDept.setText(user.getDepartment());
        etRoll.setText(user.getRollNumber());
        etGithub.setText(user.getGithubUsername());
        etBio.setText(user.getBio());
        if (user.getSkills() != null && !user.getSkills().isEmpty()) {
            etSkills.setText(TextUtils.join(", ", user.getSkills()));
        }

        if ("FACULTY_MENTOR".equalsIgnoreCase(user.getRole())) {
            rbFaculty.setChecked(true);
        } else {
            rbStudent.setChecked(true);
        }

        String domain = CampusVerifier.extractDomain(user.getEmail());
        boolean academic = CampusVerifier.isAcademicDomain(domain);
        if (user.isCampusVerified()) {
            tvVerificationStatus.setText("✓ Verified Campus Profile");
            tvVerificationDomain.setText("Academic email verified: " + user.getEmail());
        } else if (academic) {
            tvVerificationStatus.setText("Academic Domain Detected (Unverified)");
            tvVerificationDomain.setText("Domain: " + domain + " — verify your inbox link and tap Refresh Status.");
        } else {
            tvVerificationStatus.setText("Standard Account");
            tvVerificationDomain.setText("Email: " + user.getEmail() + " (use a .edu or .ac.* email for verified campus badge)");
        }
    }

    private void saveProfile() {
        if (currentUserObj == null) return;
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String dept = etDept.getText() != null ? etDept.getText().toString().trim() : "";
        String roll = etRoll.getText() != null ? etRoll.getText().toString().trim() : "";
        String skillsStr = etSkills.getText() != null ? etSkills.getText().toString().trim() : "";
        String github = etGithub.getText() != null ? etGithub.getText().toString().trim() : "";
        String bio = etBio.getText() != null ? etBio.getText().toString().trim() : "";

        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUserObj.setName(name);
        currentUserObj.setDepartment(dept);
        currentUserObj.setRollNumber(roll);
        currentUserObj.setGithubUsername(github);
        currentUserObj.setBio(bio);
        currentUserObj.setRole(rgRole.getCheckedRadioButtonId() == R.id.rbProfileFaculty
                ? "FACULTY_MENTOR" : "STUDENT");

        List<String> skills = new ArrayList<>();
        if (!skillsStr.isEmpty()) {
            for (String s : skillsStr.split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) skills.add(t);
            }
        }
        currentUserObj.setSkills(skills);

        authViewModel.updateProfile(currentUserObj);
    }
}
