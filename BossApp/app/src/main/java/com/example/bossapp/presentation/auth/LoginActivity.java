package com.example.bossapp.presentation.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bossapp.MainActivity;
import com.example.bossapp.R;
import com.example.bossapp.business.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupClickListeners();

        authManager = new AuthManager(this);

        // Handle back button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });

        // Proveri da li je korisnik već ulogovan
        if (authManager.isUserLoggedIn()) {
            navigateToMainActivity();
        }
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> handleLogin());

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Reset lozinke - biće implementiran uskoro", Toast.LENGTH_SHORT).show();
            // TODO: Implementiraj ForgotPasswordActivity
        });
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validacija
        if (!validateInputs(email, password)) {
            return;
        }

        // Prikaži progress
        btnLogin.setEnabled(false);
        btnLogin.setText("Prijavljivanje...");

        authManager.loginUser(email, password, new AuthManager.OnLoginListener() {
            @Override
            public void onSuccess(com.example.bossapp.data.model.User user) {
                Toast.makeText(LoginActivity.this,
                        "Dobrodošao/la, " + user.getUsername() + "!",
                        Toast.LENGTH_SHORT).show();
                navigateToMainActivity();
            }

            @Override
            public void onEmailNotVerified() {
                btnLogin.setEnabled(true);
                btnLogin.setText("Prijavi se");
                Toast.makeText(LoginActivity.this,
                        "Molimo verifikujte email adresu. Proverite inbox.",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Prijavi se");
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateInputs(String email, String password) {
        // Email validacija
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email je obavezan");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Unesite ispravnu email adresu");
            etEmail.requestFocus();
            return false;
        }

        // Password validacija
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Lozinka je obavezna");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /*@Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }*/
}