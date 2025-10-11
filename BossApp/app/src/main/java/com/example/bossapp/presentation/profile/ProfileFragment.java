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
import com.example.bossapp.data.repository.FriendRepository;
import com.example.bossapp.data.repository.UserRepository;
import com.example.bossapp.presentation.base.BaseFragment;
import com.example.bossapp.presentation.friends.QRScannerActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends BaseFragment {

    private static final String TAG = "ProfileFragment";
    private static final int QR_SCAN_REQUEST = 100;
    private static final String ARG_USER_ID = "userId";

    private ImageView ivAvatar, ivQRCode;
    private TextView tvUsername, tvLevel, tvTitle, tvPowerPoints, tvExperiencePoints, tvCoins, tvBadges;
    private MaterialButton btnChangePassword, btnScanQR, btnFriendAction;
    private View statsContainer;

    private UserRepository userRepository;
    private FriendRepository friendRepository;
    private String currentUserId;
    private String displayUserId;
    private boolean isOwnProfile;
    private User displayedUser;

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
        friendRepository = new FriendRepository();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (getArguments() != null && getArguments().containsKey(ARG_USER_ID)) {
            String argUserId = getArguments().getString(ARG_USER_ID);
            if (argUserId != null && !argUserId.isEmpty()) {
                displayUserId = argUserId;
            } else {
                displayUserId = currentUserId;
            }
        } else {
            displayUserId = currentUserId;
        }

        isOwnProfile = currentUserId.equals(displayUserId);

        setupButtons();
        configureVisibility();
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
        btnFriendAction = view.findViewById(R.id.btnFriendAction);
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

        btnFriendAction.setOnClickListener(v -> handleFriendAction());
    }

    private void configureVisibility() {
        if (isOwnProfile) {
            // My profile - show everything
            btnChangePassword.setVisibility(View.VISIBLE);
            btnScanQR.setVisibility(View.VISIBLE);
            btnFriendAction.setVisibility(View.GONE); // Hide friend button on own profile
            tvCoins.setVisibility(View.VISIBLE);
            tvPowerPoints.setVisibility(View.VISIBLE);
        } else {
            // Other user's profile
            btnChangePassword.setVisibility(View.GONE);
            btnScanQR.setVisibility(View.GONE);
            btnFriendAction.setVisibility(View.VISIBLE); // Show friend button
            tvCoins.setVisibility(View.GONE);
            tvPowerPoints.setVisibility(View.GONE);
        }
    }

    private void loadUserProfile() {
        userRepository.getUserById(displayUserId, new UserRepository.OnUserLoadListener() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG, "User loaded: " + user.getUsername());
                displayedUser = user;
                displayUserProfile(user);
                updateFriendButton();
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

        if (isOwnProfile) {
            tvPowerPoints.setText(String.valueOf(user.getPowerPoints()));
        }

        int nextLevelXP = calculateXPForNextLevel(user.getLevel());
        tvExperiencePoints.setText(user.getExperiencePoints() + " / " + nextLevelXP);

        if (isOwnProfile) {
            tvCoins.setText(String.valueOf(user.getCoins()));
        }

        tvBadges.setText(String.valueOf(user.getBadges()));
    }

    private void updateFriendButton() {
        if (isOwnProfile || displayedUser == null) {
            return;
        }

        // Check if already friends
        userRepository.getUserById(currentUserId, new UserRepository.OnUserLoadListener() {
            @Override
            public void onSuccess(User currentUser) {
                if (currentUser.isFriend(displayUserId)) {
                    // Already friends
                    btnFriendAction.setText("Remove Friend");
                    btnFriendAction.setIcon(getResources().getDrawable(android.R.drawable.ic_delete));
                } else {
                    // Not friends
                    btnFriendAction.setText("Add Friend");
                    btnFriendAction.setIcon(getResources().getDrawable(android.R.drawable.ic_input_add));
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error checking friendship status", e);
            }
        });
    }

    private void handleFriendAction() {
        if (displayedUser == null) return;

        userRepository.getUserById(currentUserId, new UserRepository.OnUserLoadListener() {
            @Override
            public void onSuccess(User currentUser) {
                if (currentUser.isFriend(displayUserId)) {
                    // Remove friend
                    removeFriend();
                } else {
                    // Send friend request
                    sendFriendRequest(currentUser);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendFriendRequest(User currentUser) {
        btnFriendAction.setEnabled(false);
        btnFriendAction.setText("Sending...");

        friendRepository.sendFriendRequest(
                currentUserId,
                displayUserId,
                currentUser.getUsername(),
                currentUser.getAvatarIndex(),
                new FriendRepository.OnFriendRequestListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(),
                                "Friend request sent!", Toast.LENGTH_SHORT).show();
                        btnFriendAction.setEnabled(true);
                        btnFriendAction.setText("Request Sent");
                        btnFriendAction.setEnabled(false);
                    }

                    @Override
                    public void onError(Exception e) {
                        btnFriendAction.setEnabled(true);
                        updateFriendButton();
                        String message = e.getMessage();
                        if (message != null && message.contains("already exists")) {
                            Toast.makeText(requireContext(),
                                    "Friend request already sent", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Error sending request: " + message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void removeFriend() {
        btnFriendAction.setEnabled(false);
        btnFriendAction.setText("Removing...");

        friendRepository.removeFriend(currentUserId, displayUserId,
                new FriendRepository.OnFriendRequestListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(),
                                "Friend removed", Toast.LENGTH_SHORT).show();
                        btnFriendAction.setEnabled(true);
                        updateFriendButton();
                    }

                    @Override
                    public void onError(Exception e) {
                        btnFriendAction.setEnabled(true);
                        updateFriendButton();
                        Toast.makeText(requireContext(),
                                "Error removing friend: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void generateAndDisplayQRCode() {
        new Thread(() -> {
            Bitmap qrBitmap = QRCodeManager.generateQRCode(displayUserId);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (qrBitmap != null) {
                        ivQRCode.setImageBitmap(qrBitmap);
                        Log.d(TAG, "QR code generated successfully");
                    } else {
                        Log.e(TAG, "Error generating QR code");
                        Toast.makeText(requireContext(),
                                "Error generating QR code",
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
                        "Scanned User ID: " + scannedUserId,
                        Toast.LENGTH_LONG).show();
                loadScannedUserProfile(scannedUserId);
            }
        }
    }

    private void loadScannedUserProfile(String userId) {
        ProfileFragment fragment = ProfileFragment.newInstance(userId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}