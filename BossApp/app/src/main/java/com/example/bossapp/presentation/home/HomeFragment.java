package com.example.bossapp.presentation.home;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.repository.FriendRepository;
import com.example.bossapp.data.repository.UserRepository;
import com.example.bossapp.presentation.base.BaseFragment;
import com.example.bossapp.presentation.friends.AllUsersFragment;
import com.example.bossapp.presentation.friends.FindFriendsAdapter;
import com.example.bossapp.presentation.profile.ProfileFragment;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends BaseFragment {

    private RecyclerView rvFindFriends;
    private TextView tvSeeAll;
    private FindFriendsAdapter friendsAdapter;
    private List<User> suggestionsList = new ArrayList<>();

    private UserRepository userRepository;
    private FriendRepository friendRepository;
    private String currentUserId;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar(view, R.id.toolbar);
        initViews(view);

        userRepository = new UserRepository();
        friendRepository = new FriendRepository();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupFindFriendsWidget();
        loadCurrentUser();
    }

    private void initViews(View view) {
        rvFindFriends = view.findViewById(R.id.rvFindFriends);
        tvSeeAll = view.findViewById(R.id.tvSeeAll);
    }

    private void setupFindFriendsWidget() {
        friendsAdapter = new FindFriendsAdapter(suggestionsList, null,
                new FindFriendsAdapter.OnUserActionListener() {
                    @Override
                    public void onAddFriend(User user) {
                        handleAddFriend(user);
                    }

                    @Override
                    public void onRemoveFriend(User user) {
                        handleRemoveFriend(user);
                    }

                    @Override
                    public void onViewProfile(User user) {
                        openUserProfile(user.getUserId());
                    }
                });

        LinearLayoutManager layoutManager = new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvFindFriends.setLayoutManager(layoutManager);
        rvFindFriends.setAdapter(friendsAdapter);

        tvSeeAll.setOnClickListener(v -> openAllUsersScreen());
    }

    private void loadCurrentUser() {
        userRepository.getUserById(currentUserId, new UserRepository.OnUserLoadListener() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                friendsAdapter.updateCurrentUser(user);
                loadUserSuggestions();
            }

            @Override
            public void onError(Exception e) {
                loadUserSuggestions();
            }
        });
    }

    private void loadUserSuggestions() {
        friendRepository.getAllUsers(currentUserId, new UserRepository.OnUsersLoadListener() {
            @Override
            public void onSuccess(List<User> users) {
                suggestionsList.clear();
                int limit = Math.min(users.size(), 10);
                suggestionsList.addAll(users.subList(0, limit));
                friendsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(),
                        "Error loading suggestions",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleAddFriend(User user) {
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
                                "Friend request sent!",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        String message = e.getMessage();
                        if (message != null && message.contains("already exists")) {
                            Toast.makeText(requireContext(),
                                    "Request already sent",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Error: " + message,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void handleRemoveFriend(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Friend")
                .setMessage("Are you sure you want to remove " + user.getUsername() + " from your friends?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    friendRepository.removeFriend(currentUserId, user.getUserId(),
                            new FriendRepository.OnFriendRequestListener() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(requireContext(),
                                            "Friend removed",
                                            Toast.LENGTH_SHORT).show();
                                    loadCurrentUser();
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

    private void openUserProfile(String userId) {
        ProfileFragment fragment = ProfileFragment.newInstance(userId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openAllUsersScreen() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new AllUsersFragment())
                .addToBackStack(null)
                .commit();
    }
}