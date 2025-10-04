package com.example.bossapp.presentation.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.business.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etUsername, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private TextView tvLogin;
    private RecyclerView rvAvatars;
    private AvatarAdapter avatarAdapter;
    private AuthManager authManager;
    private int selectedAvatarIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupAvatarSelection();
        setupClickListeners();

        authManager = new AuthManager(this);
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        rvAvatars = findViewById(R.id.rvAvatars);
    }

    private void setupAvatarSelection() {
        // 5 predefinisanih avatara
        int[] avatarResources = {
                R.drawable.avatar,
                R.drawable.bow,
                R.drawable.magician,
                R.drawable.pinkily,
                R.drawable.swordsman
        };

        avatarAdapter = new AvatarAdapter(avatarResources, position -> {
            selectedAvatarIndex = position;
        });

        rvAvatars.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvAvatars.setAdapter(avatarAdapter);
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> handleRegistration());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void handleRegistration() {
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validacija
        if (!validateInputs(email, username, password, confirmPassword)) {
            return;
        }

        // Prikaži progress
        btnRegister.setEnabled(false);
        btnRegister.setText("Registracija...");

        authManager.registerUser(email, password, username, selectedAvatarIndex,
                new AuthManager.OnRegistrationListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(RegisterActivity.this,
                                "Registracija uspešna! Proveri email za verifikaciju.",
                                Toast.LENGTH_LONG).show();

                        // Prebaci korisnika na Login ekran
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onUsernameExists() {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Registruj se");
                        Toast.makeText(RegisterActivity.this,
                                "Korisničko ime već postoji",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Registruj se");
                        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInputs(String email, String username,
                                   String password, String confirmPassword) {
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

        // Username validacija
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Korisničko ime je obavezno");
            etUsername.requestFocus();
            return false;
        }

        if (username.length() < 3) {
            etUsername.setError("Korisničko ime mora imati najmanje 3 karaktera");
            etUsername.requestFocus();
            return false;
        }

        // Password validacija
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Lozinka je obavezna");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Lozinka mora imati najmanje 6 karaktera");
            etPassword.requestFocus();
            return false;
        }

        // Confirm password validacija
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Lozinke se ne poklapaju");
            etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }
}