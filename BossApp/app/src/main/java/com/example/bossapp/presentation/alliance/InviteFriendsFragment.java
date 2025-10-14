package com.example.bossapp.presentation.alliance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.data.model.Alliance;
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.repository.AllianceRepository;
import com.example.bossapp.data.repository.FriendRepository;
import com.example.bossapp.data.repository.UserRepository;
import com.example.bossapp.presentation.base.BaseFragment;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class InviteFriendsFragment extends BaseFragment {

    private static final String ARG_ALLIANCE_ID = "allianceId";
    private static final String ARG_ALLIANCE_NAME = "allianceName";

    private RecyclerView rvFriends;
    private ProgressBar progressBar;
    private InviteFriendsAdapter adapter;
    private List<User> friendsList = new ArrayList<>();

    private FriendRepository friendRepository;
    private AllianceRepository allianceRepository;
    private UserRepository userRepository;
    private String currentUserId;
    private User currentUser;
    private String allianceId;
    private String allianceName;
    private Alliance currentAlliance;

    public static InviteFriendsFragment newInstance(String allianceId, String allianceName) {
        InviteFriendsFragment fragment = new InviteFriendsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ALLIANCE_ID, allianceId);
        args.putString(ARG_ALLIANCE_NAME, allianceName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_invite_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar(view, R.id.toolbar);
        initViews(view);

        if (getArguments() != null) {
            allianceId = getArguments().getString(ARG_ALLIANCE_ID);
            allianceName = getArguments().getString(ARG_ALLIANCE_NAME);
        }

        friendRepository = new FriendRepository();
        allianceRepository = new AllianceRepository();
        userRepository = new UserRepository();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupRecyclerView();
        loadCurrentUser();
    }

    private void initViews(View view) {
        rvFriends = view.findViewById(R.id.rvFriends);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        adapter = new InviteFriendsAdapter(friendsList, currentAlliance,
                new InviteFriendsAdapter.OnInviteListener() {
                    @Override
                    public void onInvite(User user) {
                        sendInvitation(user);
                    }

                    @Override
                    public void onRemove(User user) {
                        showRemoveMemberDialog(user);
                    }
                });

        rvFriends.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFriends.setAdapter(adapter);
    }

    private void loadCurrentUser() {
        userRepository.getUserById(currentUserId, new UserRepository.OnUserLoadListener() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                loadCurrentAlliance();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCurrentAlliance() {
        allianceRepository.getUserAlliance(currentUserId, new AllianceRepository.OnAllianceLoadListener() {
            @Override
            public void onSuccess(Alliance alliance) {
                currentAlliance = alliance;
                adapter.updateAlliance(alliance);
                loadFriends();
            }

            @Override
            public void onError(Exception e) {
                loadFriends();
            }
        });
    }

    private void loadFriends() {
        progressBar.setVisibility(View.VISIBLE);

        friendRepository.getFriends(currentUserId, new UserRepository.OnUsersLoadListener() {
            @Override
            public void onSuccess(List<User> friends) {
                progressBar.setVisibility(View.GONE);
                friendsList.clear();
                friendsList.addAll(friends);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Error loading friends", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendInvitation(User user) {
        if (currentUser == null) return;

        // Check if user is already in an alliance
        if (user.getCurrentAllianceId() != null && !user.getCurrentAllianceId().isEmpty()) {
            showConfirmInviteDialog(user);
        } else {
            sendInvitationRequest(user);
        }
    }

    private void showConfirmInviteDialog(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Send Invitation")
                .setMessage(user.getUsername() + " is already in another alliance. " +
                        "If they accept your invitation, they will leave their current alliance. " +
                        "Do you want to send the invitation?")
                .setPositiveButton("Send", (dialog, which) -> sendInvitationRequest(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendInvitationRequest(User user) {
        allianceRepository.sendInvitation(
                allianceId,
                allianceName,
                currentUserId,
                currentUser.getUsername(),
                user.getUserId(),
                new AllianceRepository.OnAllianceActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(),
                                "Request sent to " + user.getUsername(),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        String message = e.getMessage();
                        if (message != null && message.contains("pending invitation")) {
                            Toast.makeText(requireContext(),
                                    "Request already sent to " + user.getUsername(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Error: " + message,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showRemoveMemberDialog(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Member")
                .setMessage("Are you sure you want to remove " + user.getUsername() + " from the alliance?")
                .setPositiveButton("Remove", (dialog, which) -> removeMember(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeMember(User user) {
        allianceRepository.leaveAlliance(user.getUserId(), allianceId,
                new AllianceRepository.OnAllianceActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(),
                                user.getUsername() + " removed from alliance",
                                Toast.LENGTH_SHORT).show();
                        loadCurrentAlliance();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(requireContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}