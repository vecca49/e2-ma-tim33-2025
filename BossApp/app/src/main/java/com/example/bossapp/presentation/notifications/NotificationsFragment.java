package com.example.bossapp.presentation.notifications;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
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
import com.example.bossapp.data.model.AllianceNotification;
import com.example.bossapp.data.model.FriendRequest;
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.repository.AllianceRepository;
import com.example.bossapp.data.repository.FriendRepository;
import com.example.bossapp.data.repository.UserRepository;
import com.example.bossapp.presentation.alliance.AllianceChatFragment;
import com.example.bossapp.presentation.alliance.AllianceInvitationAdapter;
import com.example.bossapp.presentation.base.BaseFragment;
import com.example.bossapp.presentation.friends.FriendRequestAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends BaseFragment {

    private static final String TAG = "NotificationsFragment";

    private RecyclerView rvFriendRequests;
    private RecyclerView rvAllianceInvitations;
    private RecyclerView rvAllianceNotifications;
    private LinearLayout tvEmptyState;

    private FriendRequestAdapter friendRequestAdapter;
    private AllianceInvitationAdapter allianceInvitationAdapter;
    private AllianceNotificationAdapter allianceNotificationAdapter;

    private List<FriendRequest> friendRequestList = new ArrayList<>();
    private List<AllianceInvitation> allianceInvitationList = new ArrayList<>();
    private List<AllianceNotification> allianceNotificationList = new ArrayList<>();

    private FriendRepository friendRepository;
    private AllianceRepository allianceRepository;
    private UserRepository userRepository;
    private User currentUser;
    private Alliance currentAlliance;

    private ListenerRegistration allianceNotificationListener;

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
        startListeningForAllianceNotifications();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (allianceNotificationListener != null) {
            allianceNotificationListener.remove();
        }
    }

    private void initViews(View view) {
        rvFriendRequests = view.findViewById(R.id.rvFriendRequests);
        rvAllianceInvitations = view.findViewById(R.id.rvAllianceInvitations);
        rvAllianceNotifications = view.findViewById(R.id.rvAllianceNotifications);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
    }

    private void setupRecyclerViews() {
        // Alliance Notifications
        allianceNotificationAdapter = new AllianceNotificationAdapter(allianceNotificationList,
                new AllianceNotificationAdapter.OnNotificationActionListener() {
                    @Override
                    public void onDismiss(AllianceNotification notification) {
                        handleDismissNotification(notification);
                    }

                    @Override
                    public void onOpenChat(AllianceNotification notification) {
                        handleOpenChat(notification);
                    }
                });

        rvAllianceNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAllianceNotifications.setAdapter(allianceNotificationAdapter);

        // Friend Requests (postojeći kod ostaje isti)
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

        // Alliance Invitations (postojeći kod ostaje isti)
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

    private void handleOpenChat(AllianceNotification notification) {
        if (notification.getAllianceId() == null || notification.getAllianceId().isEmpty()) {
            Toast.makeText(requireContext(), "Alliance not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // First dismiss the notification
        FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(notification.getNotificationId())
                .update("read", true)
                .addOnSuccessListener(aVoid -> {
                    // Load alliance details to get alliance name
                    FirebaseFirestore.getInstance()
                            .collection("alliances")
                            .document(notification.getAllianceId())
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    String allianceName = doc.getString("allianceName");

                                    // Open chat
                                    AllianceChatFragment  chatFragment = AllianceChatFragment.newInstance(
                                            notification.getAllianceId(),
                                            allianceName != null ? allianceName : "Alliance Chat");

                                    requireActivity().getSupportFragmentManager()
                                            .beginTransaction()
                                            .replace(R.id.fragmentContainer, chatFragment)
                                            .addToBackStack(null)
                                            .commit();
                                } else {
                                    Toast.makeText(requireContext(),
                                            "Alliance no longer exists",
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(requireContext(),
                                        "Error loading alliance: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(),
                            "Error dismissing notification: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void startListeningForAllianceNotifications() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Log.d(TAG, "Starting to listen for alliance notifications");

        allianceNotificationListener = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("read", false)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to notifications", error);
                        return;
                    }

                    if (querySnapshot != null) {
                        allianceNotificationList.clear();

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String type = doc.getString("type");

                            // Handle alliance accepted/declined AND message notifications
                            if ("alliance_accepted".equals(type) ||
                                    "alliance_declined".equals(type) ||
                                    "alliance_message".equals(type)) {

                                AllianceNotification notification = new AllianceNotification();
                                notification.setNotificationId(doc.getId());
                                notification.setType(type);
                                notification.setUserId(doc.getString("userId"));

                                if ("alliance_accepted".equals(type)) {
                                    notification.setUsername(doc.getString("acceptedUsername"));
                                    notification.setAllianceName(doc.getString("allianceName"));
                                } else if ("alliance_declined".equals(type)) {
                                    notification.setUsername(doc.getString("declinedUsername"));
                                    notification.setAllianceName(doc.getString("allianceName"));
                                } else if ("alliance_message".equals(type)) {
                                    notification.setSenderUsername(doc.getString("senderUsername"));
                                    notification.setMessageText(doc.getString("messageText"));
                                    notification.setAllianceId(doc.getString("allianceId"));
                                }

                                notification.setMessage(doc.getString("message"));

                                Long timestampLong = doc.getLong("timestamp");
                                if (timestampLong != null) {
                                    notification.setTimestamp(timestampLong);
                                }

                                Boolean readBoolean = doc.getBoolean("read");
                                if (readBoolean != null) {
                                    notification.setRead(readBoolean);
                                }

                                allianceNotificationList.add(notification);
                            }
                        }

                        // Sort by timestamp (newest first)
                        allianceNotificationList.sort((n1, n2) ->
                                Long.compare(n2.getTimestamp(), n1.getTimestamp()));

                        allianceNotificationAdapter.notifyDataSetChanged();
                        updateEmptyState();
                    }
                });
    }

    private void handleDismissNotification(AllianceNotification notification) {
        FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(notification.getNotificationId())
                .update("read", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Notification dismissed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
        boolean hasNotifications = !friendRequestList.isEmpty() ||
                !allianceInvitationList.isEmpty() ||
                !allianceNotificationList.isEmpty();

        if (hasNotifications) {
            tvEmptyState.setVisibility(View.GONE);
            rvAllianceNotifications.setVisibility(allianceNotificationList.isEmpty() ? View.GONE : View.VISIBLE);
            rvFriendRequests.setVisibility(friendRequestList.isEmpty() ? View.GONE : View.VISIBLE);
            rvAllianceInvitations.setVisibility(allianceInvitationList.isEmpty() ? View.GONE : View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvAllianceNotifications.setVisibility(View.GONE);
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

        if (currentAllianceId == null || currentAllianceId.isEmpty()) {
            acceptInvitation(invitation, currentUserId, null, false);
            return;
        }

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
        if (currentAllianceId != null && !currentAllianceId.isEmpty()) {
            allianceRepository.canLeaveAlliance(currentAllianceId,
                    new AllianceRepository.OnAllianceActionListener() {
                        @Override
                        public void onSuccess() {
                            proceedWithAcceptance(invitation, currentUserId, currentAllianceId, isCurrentUserLeader);
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(requireContext(),
                                    "Cannot leave current alliance: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            proceedWithAcceptance(invitation, currentUserId, null, false);
        }
    }

    private void proceedWithAcceptance(AllianceInvitation invitation, String currentUserId,
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