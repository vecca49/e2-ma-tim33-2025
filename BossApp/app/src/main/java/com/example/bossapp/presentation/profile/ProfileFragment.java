package com.example.bossapp.presentation.profile;

import android.content.Intent;
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
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.repository.UserRepository;
import com.example.bossapp.presentation.base.BaseFragment;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends BaseFragment {

    private static final String TAG = "ProfileFragment";

    private ImageView ivAvatar, ivQRCode;
    private TextView tvUsername, tvLevel, tvTitle, tvPowerPoints, tvExperiencePoints, tvCoins, tvBadges;
    private Button btnChangePassword;

    private UserRepository userRepository;
    private String userId;

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
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        loadUserProfile();
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
    }

    private void loadUserProfile() {
        userRepository.getUserById(userId, new UserRepository.OnUserLoadListener() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG, "User loaded: " + user.getUsername());
                displayUserProfile(user);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading user", e);
                Toast.makeText(requireContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
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
        tvPowerPoints.setText(String.valueOf(user.getPowerPoints()));

        int nextLevelXP = calculateXPForNextLevel(user.getLevel());
        tvExperiencePoints.setText(user.getExperiencePoints() + " / " + nextLevelXP);

        tvCoins.setText(String.valueOf(user.getCoins()));
        tvBadges.setText(String.valueOf(user.getBadges()));

        // TODO: Implement QR code generation
        ivQRCode.setImageResource(android.R.drawable.ic_menu_gallery);
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
}