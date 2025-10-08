package com.example.bossapp.presentation.profile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bossapp.R;
import com.example.bossapp.business.QRCodeManager;
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.repository.UserRepository;
import com.example.bossapp.presentation.base.BaseFragment;
import com.example.bossapp.presentation.friends.QRScannerActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends BaseFragment {

    private static final String TAG = "ProfileFragment";
    private static final int QR_SCAN_REQUEST = 100;
    private static final String ARG_USER_ID = "userId";

    private ImageView ivAvatar, ivQRCode;
    private TextView tvUsername, tvLevel, tvTitle, tvPowerPoints, tvExperiencePoints, tvCoins, tvBadges;
    private Button btnChangePassword, btnScanQR;
    private View statsContainer;

    private UserRepository userRepository;
    private String currentUserId; // ID prijavljenog korisnika
    private String displayUserId; // ID korisnika čiji se profil prikazuje
    private boolean isOwnProfile; // Da li je ovo moj profil

    public static ProfileFragment newInstance(String userId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar(view, R.id.toolbar);
        initViews(view);

        userRepository = new UserRepository();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Proveri čiji profil prikazujemo
        if (getArguments() != null && getArguments().containsKey(ARG_USER_ID)) {
            String argUserId = getArguments().getString(ARG_USER_ID);
            // Ako je argument null ili prazan, prikaži moj profil
            if (argUserId != null && !argUserId.isEmpty()) {
                displayUserId = argUserId;
            } else {
                displayUserId = currentUserId;
            }
        } else {
            displayUserId = currentUserId; // Ako nema argumenta, prikaži moj profil
        }

        isOwnProfile = currentUserId.equals(displayUserId);

        setupButtons();
        configureVisibility(); // Podesi šta je vidljivo
        loadUserProfile();
        generateAndDisplayQRCode();
    }

    private void initViews(View view) {
        ivAvatar = view.findViewById(R.id.ivAvatar);
        ivQRCode = view.findViewById(R.id.ivQRCode);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvLevel = view.findViewById(R.id.tvLevel);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvPowerPoints = view.findViewById(R.id.tvPowerPoints);
        tvExperiencePoints = view.findViewById(R.id.tvExperiencePoints);
        tvCoins = view.findViewById(R.id.tvCoins);
        tvBadges = view.findViewById(R.id.tvBadges);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnScanQR = view.findViewById(R.id.btnScanQR);
        statsContainer = view.findViewById(R.id.statsContainer);
    }

    private void setupButtons() {
        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        btnScanQR.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), QRScannerActivity.class);
            startActivityForResult(intent, QR_SCAN_REQUEST);
        });
    }

    private void configureVisibility() {
        if (isOwnProfile) {
            // Moj profil - prikaži sve
            btnChangePassword.setVisibility(View.VISIBLE);
            btnScanQR.setVisibility(View.VISIBLE);

            // Prikaži novčiće i PP (samo vlasnik ih vidi)
            tvCoins.setVisibility(View.VISIBLE);
            tvPowerPoints.setVisibility(View.VISIBLE);
        } else {
            // Tuđi profil - sakrij privatne informacije
            btnChangePassword.setVisibility(View.GONE);
            btnScanQR.setVisibility(View.GONE);

            // Sakrij novčiće i PP od drugih korisnika
            tvCoins.setVisibility(View.GONE);
            tvPowerPoints.setVisibility(View.GONE);

            // Prikaži samo: Avatar, Username, Level, Titulu, QR, XP, Bedževe i Opremu
        }
    }

    private void loadUserProfile() {
        userRepository.getUserById(displayUserId, new UserRepository.OnUserLoadListener() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG, "Korisnik učitan: " + user.getUsername());
                displayUserProfile(user);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Greška pri učitavanju korisnika", e);
                Toast.makeText(requireContext(), "Greška pri učitavanju profila", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUserProfile(User user) {
        tvUsername.setText(user.getUsername());

        int[] avatarResources = {
                R.drawable.avatar,
                R.drawable.bow,
                R.drawable.magician,
                R.drawable.pinkily,
                R.drawable.swordsman
        };
        ivAvatar.setImageResource(avatarResources[user.getAvatarIndex()]);

        tvLevel.setText(String.valueOf(user.getLevel()));
        tvTitle.setText(user.getTitle());

        // Prikaži PP samo ako je moj profil
        if (isOwnProfile) {
            tvPowerPoints.setText(String.valueOf(user.getPowerPoints()));
        }

        int nextLevelXP = calculateXPForNextLevel(user.getLevel());
        tvExperiencePoints.setText(user.getExperiencePoints() + " / " + nextLevelXP);

        // Prikaži novčiće samo ako je moj profil
        if (isOwnProfile) {
            tvCoins.setText(String.valueOf(user.getCoins()));
        }

        tvBadges.setText(String.valueOf(user.getBadges()));
    }

    private void generateAndDisplayQRCode() {
        new Thread(() -> {
            Bitmap qrBitmap = QRCodeManager.generateQRCode(displayUserId);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (qrBitmap != null) {
                        ivQRCode.setImageBitmap(qrBitmap);
                        Log.d(TAG, "QR kod uspešno generisan");
                    } else {
                        Log.e(TAG, "Greška pri generisanju QR koda");
                        Toast.makeText(requireContext(),
                                "Greška pri generisanju QR koda",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private int calculateXPForNextLevel(int currentLevel) {
        if (currentLevel == 0) {
            return 200;
        }
        int previousXP = 200;
        for (int i = 1; i <= currentLevel; i++) {
            previousXP = (int) Math.ceil((previousXP * 2 + previousXP / 2.0) / 100.0) * 100;
        }
        return previousXP;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == QR_SCAN_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            String scannedUserId = data.getStringExtra(QRScannerActivity.EXTRA_USER_ID);
            if (scannedUserId != null) {
                Toast.makeText(requireContext(),
                        "Skenirani User ID: " + scannedUserId,
                        Toast.LENGTH_LONG).show();

                // Učitaj profil skeniranog korisnika
                loadScannedUserProfile(scannedUserId);
            }
        }
    }

    private void loadScannedUserProfile(String userId) {
        // Zameni trenutni fragment sa profilom skeniranog korisnika
        ProfileFragment fragment = ProfileFragment.newInstance(userId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}