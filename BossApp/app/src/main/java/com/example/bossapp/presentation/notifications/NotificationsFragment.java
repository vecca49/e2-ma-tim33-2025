package com.example.bossapp.presentation.notifications;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.data.model.Alliance;
import com.example.bossapp.data.model.AllianceInvitation;
import com.example.bossapp.data.model.FriendRequest;
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.repository.AllianceRepository;
import com.example.bossapp.data.repository.FriendRepository;
import com.example.bossapp.data.repository.UserRepository;
import com.example.bossapp.presentation.alliance.AllianceInvitationAdapter;
import com.example.bossapp.presentation.base.BaseFragment;
import com.example.bossapp.presentation.friends.FriendRequestAdapter;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends BaseFragment {

    private RecyclerView rvFriendRequests;
    private RecyclerView rvAllianceInvitations;
    private LinearLayout tvEmptyState;

    private FriendRequestAdapter friendRequestAdapter;
    private AllianceInvitationAdapter allianceInvitationAdapter;

    private List<FriendRequest> friendRequestList = new ArrayList<>();
    private List<AllianceInvitation> allianceInvitationList = new ArrayList<>();

    private FriendRepository friendRepository;
    private AllianceRepository allianceRepository;
    private UserRepository userRepository;
    private User currentUser;
    private Alliance currentAlliance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar(view, R.id.toolbar);
        initViews(view);

        friendRepository = new FriendRepository();
        allianceRepository = new AllianceRepository();
        userRepository = new UserRepository();

        setupRecyclerViews();
        loadCurrentUser();
    }

    private void initViews(View view) {
        rvFriendRequests = view.findViewById(R.id.rvFriendRequests);
        rvAllianceInvitations = view.findViewById(R.id.rvAllianceInvitations);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
    }

    private void setupRecyclerViews() {
        // Friend Requests
        friendRequestAdapter = new FriendRequestAdapter(friendRequestList,
                new FriendRequestAdapter.OnRequestActionListener() {
                    @Override
                    public void onAccept(FriendRequest request) {
                        handleAcceptFriend(request);
                    }

                    @Override
                    public void onReject(FriendRequest request) {
                        handleRejectFriend(request);
                    }
                });

        rvFriendRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFriendRequests.setAdapter(friendRequestAdapter);

        // Alliance Invitations
        allianceInvitationAdapter = new AllianceInvitationAdapter(allianceInvitationList,
                new AllianceInvitationAdapter.OnInvitationActionListener() {
                    @Override
                    public void onAccept(AllianceInvitation invitation) {
                        handleAcceptAlliance(invitation);
                    }

                    @Override
                    public void onDecline(AllianceInvitation invitation) {
                        handleDeclineAlliance(invitation);
                    }
                });

        rvAllianceInvitations.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAllianceInvitations.setAdapter(allianceInvitationAdapter);
    }

    private void loadCurrentUser() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        userRepository.getUserById(currentUserId, new UserRepository.OnUserLoadListener() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                loadCurrentAlliance();
            }

            @Override
            public void onError(Exception e) {
                loadFriendRequests();
                loadAllianceInvitations();
            }
        });
    }

    private void loadCurrentAlliance() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        allianceRepository.getUserAlliance(currentUserId, new AllianceRepository.OnAllianceLoadListener() {
            @Override
            public void onSuccess(Alliance alliance) {
                currentAlliance = alliance;
                loadFriendRequests();
                loadAllianceInvitations();
            }

            @Override
            public void onError(Exception e) {
                currentAlliance = null;
                loadFriendRequests();
                loadAllianceInvitations();
            }
        });
    }

    private void loadFriendRequests() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        friendRepository.getPendingRequests(currentUserId,
                new FriendRepository.OnFriendRequestsLoadListener() {
                    @Override
                    public void onSuccess(List<FriendRequest> requests) {
                        friendRequestList.clear();
                        friendRequestList.addAll(requests);
                        friendRequestAdapter.notifyDataSetChanged();
                        updateEmptyState();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(requireContext(),
                                "Error loading friend requests: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        updateEmptyState();
                    }
                });
    }

    private void loadAllianceInvitations() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        allianceRepository.getPendingInvitations(currentUserId,
                new AllianceRepository.OnInvitationsLoadListener() {
                    @Override
                    public void onSuccess(List<AllianceInvitation> invitations) {
                        allianceInvitationList.clear();
                        allianceInvitationList.addAll(invitations);
                        allianceInvitationAdapter.notifyDataSetChanged();
                        updateEmptyState();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(requireContext(),
                                "Error loading alliance invitations: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        updateEmptyState();
                    }
                });
    }

    private void updateEmptyState() {
        boolean hasNotifications = !friendRequestList.isEmpty() || !allianceInvitationList.isEmpty();

        if (hasNotifications) {
            tvEmptyState.setVisibility(View.GONE);
            rvFriendRequests.setVisibility(friendRequestList.isEmpty() ? View.GONE : View.VISIBLE);
            rvAllianceInvitations.setVisibility(allianceInvitationList.isEmpty() ? View.GONE : View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvFriendRequests.setVisibility(View.GONE);
            rvAllianceInvitations.setVisibility(View.GONE);
        }
    }

    private void handleAcceptFriend(FriendRequest request) {
        friendRepository.acceptFriendRequest(
                request.getRequestId(),
                request.getSenderId(),
                request.getReceiverId(),
                new FriendRepository.OnFriendRequestListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(),
                                "Friend request accepted!",
                                Toast.LENGTH_SHORT).show();
                        loadFriendRequests();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(requireContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleRejectFriend(FriendRequest request) {
        friendRepository.rejectFriendRequest(
                request.getRequestId(),
                new FriendRepository.OnFriendRequestListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(),
                                "Friend request rejected",
                                Toast.LENGTH_SHORT).show();
                        loadFriendRequests();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(requireContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleAcceptAlliance(AllianceInvitation invitation) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String currentAllianceId = currentUser != null ? currentUser.getCurrentAllianceId() : null;
        boolean isLeader = currentAlliance != null && currentAlliance.isLeader(currentUserId);

        // If user has no alliance, accept directly
        if (currentAllianceId == null || currentAllianceId.isEmpty()) {
            acceptInvitation(invitation, currentUserId, null, false);
            return;
        }

        // If user is a LEADER, show special warning about disbanding
        if (isLeader) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Leave Current Alliance")
                    .setMessage("You are the leader of '" + currentAlliance.getAllianceName() +
                            "'. If you join '" + invitation.getAllianceName() +
                            "', your current alliance will be DISBANDED and all members will be removed. " +
                            "Do you want to continue?")
                    .setPositiveButton("Yes, Disband & Join", (dialog, which) -> {
                        acceptInvitation(invitation, currentUserId, currentAllianceId, true);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            // If user is just a member, normal warning
            new AlertDialog.Builder(requireContext())
                    .setTitle("Leave Current Alliance")
                    .setMessage("Are you sure you want to leave your current alliance and join " +
                            invitation.getAllianceName() + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        acceptInvitation(invitation, currentUserId, currentAllianceId, false);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void acceptInvitation(AllianceInvitation invitation, String currentUserId,
                                  String currentAllianceId, boolean isCurrentUserLeader) {
        allianceRepository.acceptInvitation(
                invitation.getInvitationId(),
                invitation.getAllianceId(),
                currentUserId,
                currentAllianceId,
                isCurrentUserLeader,
                new AllianceRepository.OnAllianceActionListener() {
                    @Override
                    public void onSuccess() {
                        String message = isCurrentUserLeader ?
                                "Your alliance was disbanded. Joined alliance: " + invitation.getAllianceName() :
                                "Joined alliance: " + invitation.getAllianceName();

                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        loadCurrentUser();
                        loadAllianceInvitations();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(requireContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleDeclineAlliance(AllianceInvitation invitation) {
        allianceRepository.declineInvitation(
                invitation.getInvitationId(),
                new AllianceRepository.OnAllianceActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(),
                                "Alliance invitation declined",
                                Toast.LENGTH_SHORT).show();
                        loadAllianceInvitations();
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