package com.example.bossapp.presentation.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bossapp.R;
import com.example.bossapp.business.AuthManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ChangePasswordActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputEditText etOldPassword, etNewPassword, etConfirmPassword;
    private MaterialButton btnChangePassword, btnCancel;  // Promenjeno sa Button u MaterialButton
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        initViews();
        setupToolbar();

        authManager = new AuthManager(this);

        btnChangePassword.setOnClickListener(v -> handleChangePassword());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnCancel = findViewById(R.id.btnCancelPassword);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void handleChangePassword() {
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(oldPassword)) {
            etOldPassword.setError("Enter old password");
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError("Enter new password");
            return;
        }

        if (newPassword.length() < 6) {
            etNewPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords don't match");
            return;
        }

        btnChangePassword.setEnabled(false);
        btnCancel.setEnabled(false);
        btnChangePassword.setText("Changing...");

        authManager.changePassword(oldPassword, newPassword, new AuthManager.OnPasswordChangeListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(ChangePasswordActivity.this,
                        "Password changed successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                btnChangePassword.setEnabled(true);
                btnCancel.setEnabled(true);
                btnChangePassword.setText("Change Password");

                if (e.getMessage().contains("wrong-password")) {
                    etOldPassword.setError("Incorrect old password");
                } else {
                    Toast.makeText(ChangePasswordActivity.this,
                            "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}