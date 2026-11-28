package com.projecthub.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.projecthub.R;
import com.projecthub.repository.GitHubRepository;
import com.projecthub.utils.AppPreferences;
import com.projecthub.utils.NotificationHelper;
import com.projecthub.viewmodel.AuthViewModel;
import com.projecthub.viewmodel.DashboardViewModel;

public class SettingsActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private DashboardViewModel dashboardViewModel;

    private TextView tvSettingsAvatar, tvSettingsName, tvSettingsEmail, tvThemeStatus, tvGitHubTokenStatus;
    private MaterialButtonToggleGroup toggleThemeGroup;
    private MaterialSwitch switchSystemNotifications, switchDeadlineReminders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvSettingsAvatar = findViewById(R.id.tvSettingsAvatar);
        tvSettingsName = findViewById(R.id.tvSettingsName);
        tvSettingsEmail = findViewById(R.id.tvSettingsEmail);
        tvThemeStatus = findViewById(R.id.tvThemeStatus);
        tvGitHubTokenStatus = findViewById(R.id.tvGitHubTokenStatus);
        toggleThemeGroup = findViewById(R.id.toggleThemeGroup);
        switchSystemNotifications = findViewById(R.id.switchSystemNotifications);
        switchDeadlineReminders = findViewById(R.id.switchDeadlineReminders);

        View layoutAccountRow = findViewById(R.id.layoutAccountRow);
        View rowJoinByCode = findViewById(R.id.rowJoinByCode);
        View rowManageRequests = findViewById(R.id.rowManageRequests);
        View rowCampusProfile = findViewById(R.id.rowCampusProfile);
        View rowNotificationsInbox = findViewById(R.id.rowNotificationsInbox);
        View rowDeviceNotificationSettings = findViewById(R.id.rowDeviceNotificationSettings);
        View rowGitHubToken = findViewById(R.id.rowGitHubToken);
        View rowClearNotifCache = findViewById(R.id.rowClearNotifCache);
        MaterialButton btnSettingsLogout = findViewById(R.id.btnSettingsLogout);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        authViewModel.fetchCurrentUserProfile();
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                String name = user.getName() != null && !user.getName().isEmpty() ? user.getName() : "Campus Account";
                String badge = user.isCampusVerified() ? " ✓" : "";
                tvSettingsName.setText(name + badge);
                tvSettingsAvatar.setText(name.substring(0, 1).toUpperCase());
                String email = user.getEmail() != null ? user.getEmail() : "";
                String dept = user.getDepartment() != null ? user.getDepartment() : "";
                if (!email.isEmpty() && !dept.isEmpty()) {
                    tvSettingsEmail.setText(email + " • " + dept);
                } else if (!email.isEmpty()) {
                    tvSettingsEmail.setText(email);
                }
            }
        });

        dashboardViewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        setupThemeSelector();
        setupSwitches();
        updateGitHubTokenStatus();

        layoutAccountRow.setOnClickListener(v ->
            startActivity(new Intent(SettingsActivity.this, ProfileActivity.class)));
        rowCampusProfile.setOnClickListener(v ->
            startActivity(new Intent(SettingsActivity.this, ProfileActivity.class)));
        rowJoinByCode.setOnClickListener(v -> showJoinProjectDialog());
        rowManageRequests.setOnClickListener(v ->
            startActivity(new Intent(SettingsActivity.this, ManageRequestsActivity.class)));
        rowNotificationsInbox.setOnClickListener(v ->
            startActivity(new Intent(SettingsActivity.this, NotificationsActivity.class)));
        rowDeviceNotificationSettings.setOnClickListener(v -> openSystemNotificationSettings());
        rowGitHubToken.setOnClickListener(v -> showGitHubTokenDialog());
        rowClearNotifCache.setOnClickListener(v -> {
            NotificationHelper.clearNotificationDedupCache(SettingsActivity.this);
            Toast.makeText(SettingsActivity.this, "Alert history cache cleared", Toast.LENGTH_SHORT).show();
        });
        btnSettingsLogout.setOnClickListener(v -> confirmLogout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGitHubTokenStatus();
    }

    private void setupThemeSelector() {
        String savedMode = AppPreferences.getThemeMode(this);
        int checkedId = R.id.btnThemeSystem;
        if (AppPreferences.THEME_LIGHT.equals(savedMode)) {
            checkedId = R.id.btnThemeLight;
        } else if (AppPreferences.THEME_DARK.equals(savedMode)) {
            checkedId = R.id.btnThemeDark;
        }

        // Select initial button BEFORE attaching listener to avoid spurious recreation loops.
        toggleThemeGroup.check(checkedId);
        updateThemeStatusLabel(savedMode);

        toggleThemeGroup.addOnButtonCheckedListener((group, btnId, isChecked) -> {
            if (!isChecked) return;
            String newMode = AppPreferences.THEME_SYSTEM;
            if (btnId == R.id.btnThemeLight) {
                newMode = AppPreferences.THEME_LIGHT;
            } else if (btnId == R.id.btnThemeDark) {
                newMode = AppPreferences.THEME_DARK;
            }
            if (newMode.equals(AppPreferences.getThemeMode(SettingsActivity.this))) {
                return;
            }
            updateThemeStatusLabel(newMode);
            AppPreferences.setThemeMode(SettingsActivity.this, newMode);
        });
    }

    private void updateThemeStatusLabel(String mode) {
        if (AppPreferences.THEME_LIGHT.equals(mode)) {
            tvThemeStatus.setText("Always use Light mode");
        } else if (AppPreferences.THEME_DARK.equals(mode)) {
            tvThemeStatus.setText("Always use Dark mode");
        } else {
            tvThemeStatus.setText("Following device theme");
        }
    }

    private void setupSwitches() {
        switchSystemNotifications.setChecked(AppPreferences.isSystemNotificationsEnabled(this));
        switchDeadlineReminders.setChecked(AppPreferences.isDeadlineRemindersEnabled(this));

        switchSystemNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPreferences.setSystemNotificationsEnabled(SettingsActivity.this, isChecked);
            Toast.makeText(
                SettingsActivity.this,
                isChecked ? "System notifications enabled" : "System notifications muted",
                Toast.LENGTH_SHORT
            ).show();
        });

        switchDeadlineReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPreferences.setDeadlineRemindersEnabled(SettingsActivity.this, isChecked);
            Toast.makeText(
                SettingsActivity.this,
                isChecked ? "48-hour deadline reminders enabled" : "Deadline reminders paused",
                Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void updateGitHubTokenStatus() {
        String token = GitHubRepository.getToken(this);
        if (token != null && !token.isEmpty()) {
            tvGitHubTokenStatus.setText("Personal Access Token configured");
        } else {
            tvGitHubTokenStatus.setText("Not configured (optional for private repos)");
        }
    }

    private void showGitHubTokenDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padH = Math.round(20 * getResources().getDisplayMetrics().density);
        int padTop = Math.round(12 * getResources().getDisplayMetrics().density);
        container.setPadding(padH, padTop, padH, 0);

        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("Personal Access Token");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setText(GitHubRepository.getToken(this));
        container.addView(input);

        new AlertDialog.Builder(this)
            .setTitle("GitHub API Token")
            .setMessage("Used locally to authenticate GitHub repo requests and increase rate limits. Cleared automatically on logout.")
            .setView(container)
            .setPositiveButton("Save", (d, w) -> {
                String token = input.getText() != null ? input.getText().toString().trim() : "";
                GitHubRepository.saveToken(SettingsActivity.this, token);
                updateGitHubTokenStatus();
                Toast.makeText(
                    SettingsActivity.this,
                    token.isEmpty() ? "GitHub token cleared" : "GitHub token saved locally",
                    Toast.LENGTH_SHORT
                ).show();
            })
            .setNeutralButton("Clear", (d, w) -> {
                GitHubRepository.clearToken(SettingsActivity.this);
                updateGitHubTokenStatus();
                Toast.makeText(SettingsActivity.this, "GitHub token cleared", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showJoinProjectDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_join_project, null);
        TextInputEditText etJoinCode = dialogView.findViewById(R.id.etJoinCode);

        new AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Join", (dialog, which) -> {
                String code = etJoinCode.getText() != null ? etJoinCode.getText().toString().trim() : "";
                String uid = authViewModel.getCurrentUserId();
                if (uid == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
                    uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                }
                if (!code.isEmpty() && uid != null) {
                    dashboardViewModel.joinProject(uid, code);
                } else {
                    Toast.makeText(this, "Please enter a valid join code", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void openSystemNotificationSettings() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
        }
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open device notification settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out of ProjectHub?")
            .setPositiveButton("Log Out", (d, w) -> {
                GitHubRepository.clearToken(SettingsActivity.this);
                authViewModel.logout();
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
