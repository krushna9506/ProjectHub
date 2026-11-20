package com.projecthub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.projecthub.R;
import com.projecthub.viewmodel.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private TextInputEditText etName, etEmail, etDepartment, etRollNumber, etPassword;
    private MaterialButton btnRegister;
    private ProgressBar progressBar;
    private TextView tvGoToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etDepartment = findViewById(R.id.etDepartment);
        etRollNumber = findViewById(R.id.etRollNumber);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        authViewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        authViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });

        authViewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnRegister.setEnabled(!isLoading);
        });

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String dept = etDepartment.getText() != null ? etDepartment.getText().toString().trim() : "";
            String roll = etRollNumber.getText() != null ? etRollNumber.getText().toString().trim() : "";
            String pass = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || dept.isEmpty() || roll.isEmpty()) {
                Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pass.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            authViewModel.register(email, pass, name, dept, roll);
        });

        tvGoToLogin.setOnClickListener(v -> finish());
    }
}
