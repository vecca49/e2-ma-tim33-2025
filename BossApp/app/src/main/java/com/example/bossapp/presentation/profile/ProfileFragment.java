package com.example.bossapp.presentation.profile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.business.QRCodeManager;
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.repository.FriendRepository;
import com.example.bossapp.data.repository.UserRepository;
import com.example.bossapp.presentation.base.BaseFragment;
import com.example.bossapp.presentation.friends.FindFriendsAdapter;
import com.example.bossapp.presentation.friends.QRScannerActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends BaseFragment {

    private static final String TAG = "ProfileFragment";
    private static final int QR_SCAN_REQUEST = 100;
    private static final String ARG_USER_ID = "userId";

    private ImageView ivAvatar, ivQRCode;
    private TextView tvUsername, tvLevel, tvTitle, tvPowerPoints, tvExperiencePoints, tvCoins, tvBadges;
    private TextView tvFriendsCount, tvSeeAllFriends, tvNoFriends;
    private MaterialButton btnChangePassword, btnScanQR, btnFriendAction;
    private View statsContainer;
    private RecyclerView rvFriends;

    private FindFriendsAdapter friendsAdapter;
    private List<User> friendsList = new ArrayList<>();

    private UserRepository userRepository;
    private FriendRepository friendRepository;
    private String currentUserId;
    private String displayUserId;
    private boolean isOwnProfile;
    private User displayedUser;
    private User currentUser;

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
        setupFriendsWidget();
        configureVisibility();
        loadCurrentUserData();
        loadUserProfile();
        loadFriends();
        generateAndDisplayQRCode();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isOwnProfile) {
            loadCurrentUserData();
        }
        loadFriends();
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
        rvFriends = view.findViewById(R.id.rvFriends);
        tvFriendsCount = view.findViewById(R.id.tvFriendsCount);
        tvSeeAllFriends = view.findViewById(R.id.tvSeeAllFriends);
        tvNoFriends = view.findViewById(R.id.tvNoFriends);
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

        tvSeeAllFriends.setOnClickListener(v -> openAllFriendsScreen());
    }

    private void setupFriendsWidget() {
        friendsAdapter = new FindFriendsAdapter(friendsList, null,
                new FindFriendsAdapter.OnUserActionListener() {
                    @Override
                    public void onAddFriend(User user) {
                        handleAddFriendFromWidget(user);
                    }

                    @Override
                    public void onRemoveFriend(User user) {
                        handleRemoveFriendFromWidget(user);
                    }

                    @Override
                    public void onViewProfile(User user) {
                        openUserProfile(user.getUserId());
                    }
                }, isOwnProfile); // Show buttons only on own profile

        LinearLayoutManager layoutManager = new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvFriends.setLayoutManager(layoutManager);
        rvFriends.setAdapter(friendsAdapter);
    }

    private void handleAddFriendFromWidget(User user) {
        if (currentUser == null) return;

        friendRepository.sendFriendRequest(
                currentUserId,
                user.getUserId(),
                currentUser.getUsername(),
                currentUser.getAvatarIndex(),
                new FriendRepository.OnFriendRequestListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(),
                                "Friend request sent!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        String message = e.getMessage();
                        if (message != null && message.contains("already exists")) {
                            Toast.makeText(requireContext(),
                                    "Request already sent", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Error: " + message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void handleRemoveFriendFromWidget(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Friend")
                .setMessage("Are you sure you want to remove " + user.getUsername() + " from your friends?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    friendRepository.removeFriend(currentUserId, user.getUserId(),
                            new FriendRepository.OnFriendRequestListener() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(requireContext(),
                                            user.getUsername() + " removed from friends",
                                            Toast.LENGTH_SHORT).show();
                                    loadCurrentUserData();
                                    loadFriends();
                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(requireContext(),
                                            "Error: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void configureVisibility() {
        if (isOwnProfile) {
            btnChangePassword.setVisibility(View.VISIBLE);
            btnScanQR.setVisibility(View.VISIBLE);
            btnFriendAction.setVisibility(View.GONE);
            tvCoins.setVisibility(View.VISIBLE);
            tvPowerPoints.setVisibility(View.VISIBLE);
        } else {
            btnChangePassword.setVisibility(View.GONE);
            btnScanQR.setVisibility(View.GONE);
            btnFriendAction.setVisibility(View.VISIBLE);
            tvCoins.setVisibility(View.GONE);
            tvPowerPoints.setVisibility(View.GONE);
        }
    }

    private void loadCurrentUserData() {
        userRepository.getUserById(currentUserId, new UserRepository.OnUserLoadListener() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                friendsAdapter.updateCurrentUser(user);
                if (!isOwnProfile) {
                    updateFriendButton();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading current user", e);
            }
        });
    }

    private void loadUserProfile() {
        userRepository.getUserById(displayUserId, new UserRepository.OnUserLoadListener() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG, "User loaded: " + user.getUsername());
                displayedUser = user;
                displayUserProfile(user);
                if (!isOwnProfile) {
                    updateFriendButton();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading user", e);
                Toast.makeText(requireContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFriends() {
        friendRepository.getFriends(displayUserId, new UserRepository.OnUsersLoadListener() {
            @Override
            public void onSuccess(List<User> friends) {
                friendsList.clear();
                friendsList.addAll(friends);
                friendsAdapter.notifyDataSetChanged();

                tvFriendsCount.setText(String.valueOf(friends.size()));

                if (friends.isEmpty()) {
                    rvFriends.setVisibility(View.GONE);
                    tvNoFriends.setVisibility(View.VISIBLE);
                } else {
                    rvFriends.setVisibility(View.VISIBLE);
                    tvNoFriends.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading friends", e);
                tvFriendsCount.setText("0");
                rvFriends.setVisibility(View.GONE);
                tvNoFriends.setVisibility(View.VISIBLE);
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
        if (isOwnProfile || displayedUser == null || currentUser == null) {
            return;
        }

        boolean isFriend = currentUser.isFriend(displayUserId);

        if (isFriend) {
            btnFriendAction.setText("Remove Friend");
            btnFriendAction.setIcon(getResources().getDrawable(android.R.drawable.ic_delete));
        } else {
            btnFriendAction.setText("Add Friend");
            btnFriendAction.setIcon(getResources().getDrawable(android.R.drawable.ic_input_add));
        }
    }

    private void handleFriendAction() {
        if (displayedUser == null || currentUser == null) return;

        if (currentUser.isFriend(displayUserId)) {
            showRemoveFriendDialog();
        } else {
            sendFriendRequest();
        }
    }

    private void showRemoveFriendDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Friend")
                .setMessage("Are you sure you want to remove " + displayedUser.getUsername() + " from your friends?")
                .setPositiveButton("Yes", (dialog, which) -> removeFriend())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendFriendRequest() {
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
                        btnFriendAction.setText("Request Sent");
                        btnFriendAction.setEnabled(false);
                    }

                    @Override
                    public void onError(Exception e) {
                        btnFriendAction.setEnabled(true);
                        String message = e.getMessage();
                        if (message != null && message.contains("already exists")) {
                            Toast.makeText(requireContext(),
                                    "Friend request already sent", Toast.LENGTH_SHORT).show();
                            btnFriendAction.setText("Request Sent");
                            btnFriendAction.setEnabled(false);
                        } else {
                            Toast.makeText(requireContext(),
                                    "Error sending request: " + message, Toast.LENGTH_SHORT).show();
                            updateFriendButton();
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
                        loadCurrentUserData();
                        loadFriends();
                    }

                    @Override
                    public void onError(Exception e) {
                        btnFriendAction.setEnabled(true);
                        Toast.makeText(requireContext(),
                                "Error removing friend: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        updateFriendButton();
                    }
                });
    }

    private void openUserProfile(String userId) {
        if (userId.equals(currentUserId)) {
            // If clicking on own profile in friends list, just reload
            if (!isOwnProfile) {
                ProfileFragment fragment = ProfileFragment.newInstance(userId);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        } else {
            ProfileFragment fragment = ProfileFragment.newInstance(userId);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void openAllFriendsScreen() {
        // TODO: Create AllFriendsFragment if you want a dedicated friends list page
        Toast.makeText(requireContext(), "All friends view - Coming soon!", Toast.LENGTH_SHORT).show();
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