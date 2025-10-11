package com.example.bossapp.presentation.friends;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.repository.FriendRepository;
import com.example.bossapp.data.repository.UserRepository;
import com.example.bossapp.presentation.base.BaseFragment;
import com.example.bossapp.presentation.profile.ProfileFragment;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class AllUsersFragment extends BaseFragment {

    private RecyclerView rvUsers;
    private ProgressBar progressBar;
    private FindFriendsAdapter adapter;
    private List<User> usersList = new ArrayList<>();

    private UserRepository userRepository;
    private FriendRepository friendRepository;
    private String currentUserId;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_all_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar(view, R.id.toolbar);
        initViews(view);

        userRepository = new UserRepository();
        friendRepository = new FriendRepository();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupRecyclerView();
        loadCurrentUser();
    }

    private void initViews(View view) {
        rvUsers = view.findViewById(R.id.rvUsers);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        adapter = new FindFriendsAdapter(usersList, null, new FindFriendsAdapter.OnUserActionListener() {
            @Override
            public void onAddFriend(User user) {
                handleAddFriend(user);
            }

            @Override
            public void onRemoveFriend(User user) {
                showRemoveFriendDialog(user);
            }

            @Override
            public void onViewProfile(User user) {
                openUserProfile(user.getUserId());
            }
        });

        rvUsers.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvUsers.setAdapter(adapter);
    }

    private void loadCurrentUser() {
        userRepository.getUserById(currentUserId, new UserRepository.OnUserLoadListener() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                adapter.updateCurrentUser(user);
                loadAllUsers();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                loadAllUsers();
            }
        });
    }

    private void loadAllUsers() {
        progressBar.setVisibility(View.VISIBLE);

        friendRepository.getAllUsers(currentUserId, new UserRepository.OnUsersLoadListener() {
            @Override
            public void onSuccess(List<User> users) {
                progressBar.setVisibility(View.GONE);
                usersList.clear();
                usersList.addAll(users);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(),
                        "Error loading users: " + e.getMessage(),
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
                                "Friend request sent to " + user.getUsername(),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        String message = e.getMessage();
                        if (message != null && message.contains("already exists")) {
                            Toast.makeText(requireContext(),
                                    "Request already sent or you're already friends",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Error: " + message,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showRemoveFriendDialog(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Friend")
                .setMessage("Are you sure you want to remove " + user.getUsername() + " from your friends?")
                .setPositiveButton("Yes", (dialog, which) -> handleRemoveFriend(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleRemoveFriend(User user) {
        friendRepository.removeFriend(currentUserId, user.getUserId(),
                new FriendRepository.OnFriendRequestListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(),
                                user.getUsername() + " removed from friends",
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
    }

    private void openUserProfile(String userId) {
        ProfileFragment fragment = ProfileFragment.newInstance(userId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}