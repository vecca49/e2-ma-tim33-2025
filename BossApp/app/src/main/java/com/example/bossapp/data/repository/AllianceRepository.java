package com.example.bossapp.data.repository;

import android.util.Log;

import com.example.bossapp.data.model.Alliance;
import com.example.bossapp.data.model.AllianceInvitation;
import com.example.bossapp.data.model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllianceRepository {
    private static final String TAG = "AllianceRepository";
    private static final String COLLECTION_ALLIANCES = "alliances";
    private static final String COLLECTION_ALLIANCE_INVITATIONS = "allianceInvitations";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";

    private FirebaseFirestore db;

    public AllianceRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // ===== INTERFACES =====
    public interface OnAllianceActionListener {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnAllianceLoadListener {
        void onSuccess(Alliance alliance);
        void onError(Exception e);
    }

    public interface OnAlliancesLoadListener {
        void onSuccess(List<Alliance> alliances);
        void onError(Exception e);
    }

    public interface OnInvitationsLoadListener {
        void onSuccess(List<AllianceInvitation> invitations);
        void onError(Exception e);
    }

    // ===== CREATE ALLIANCE =====
    public void createAlliance(String allianceName, String leaderId, String leaderUsername,
                               OnAllianceActionListener listener) {
        String allianceId = db.collection(COLLECTION_ALLIANCES).document().getId();
        Alliance alliance = new Alliance(allianceId, allianceName, leaderId, leaderUsername);

        db.collection(COLLECTION_ALLIANCES)
                .document(allianceId)
                .set(alliance.toMap())
                .addOnSuccessListener(aVoid -> {
                    db.collection(COLLECTION_USERS)
                            .document(leaderId)
                            .update("currentAllianceId", allianceId)
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d(TAG, "Alliance created successfully");
                                listener.onSuccess();
                            })
                            .addOnFailureListener(listener::onError);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating alliance", e);
                    listener.onError(e);
                });
    }

    // ===== GET USER'S ALLIANCE =====
    public void getUserAlliance(String userId, OnAllianceLoadListener listener) {
        db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user == null || user.getCurrentAllianceId() == null) {
                        listener.onSuccess(null);
                        return;
                    }

                    db.collection(COLLECTION_ALLIANCES)
                            .document(user.getCurrentAllianceId())
                            .get()
                            .addOnSuccessListener(allianceDoc -> {
                                if (allianceDoc.exists()) {
                                    Alliance alliance = allianceDoc.toObject(Alliance.class);
                                    listener.onSuccess(alliance);
                                } else {
                                    db.collection(COLLECTION_USERS)
                                            .document(userId)
                                            .update("currentAllianceId", null)
                                            .addOnSuccessListener(v -> listener.onSuccess(null))
                                            .addOnFailureListener(e -> listener.onSuccess(null));
                                }
                            })
                            .addOnFailureListener(listener::onError);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user alliance", e);
                    listener.onError(e);
                });
    }

    // ===== SEND INVITATION =====
    public void sendInvitation(String allianceId, String allianceName, String senderId,
                               String senderUsername, String receiverId,
                               OnAllianceActionListener listener) {
        db.collection(COLLECTION_ALLIANCES)
                .document(allianceId)
                .get()
                .addOnSuccessListener(allianceDoc -> {
                    if (!allianceDoc.exists()) {
                        listener.onError(new Exception("Alliance no longer exists"));
                        return;
                    }

                    checkUserAllianceStatus(receiverId, new OnAllianceActionListener() {
                        @Override
                        public void onSuccess() {
                            AllianceInvitation invitation = new AllianceInvitation(
                                    allianceId, allianceName, senderId, senderUsername, receiverId);

                            db.collection(COLLECTION_ALLIANCE_INVITATIONS)
                                    .document(invitation.getInvitationId())
                                    .set(invitation.toMap())
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Alliance invitation sent");
                                        listener.onSuccess();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error sending invitation", e);
                                        listener.onError(e);
                                    });
                        }

                        @Override
                        public void onError(Exception e) {
                            listener.onError(e);
                        }
                    });
                })
                .addOnFailureListener(listener::onError);
    }

    private void checkUserAllianceStatus(String userId, OnAllianceActionListener listener) {
        db.collection(COLLECTION_ALLIANCE_INVITATIONS)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        listener.onError(new Exception("User already has pending invitation"));
                    } else {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    // ===== GET PENDING INVITATIONS =====
    public void getPendingInvitations(String userId, OnInvitationsLoadListener listener) {
        db.collection(COLLECTION_ALLIANCE_INVITATIONS)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<AllianceInvitation> validInvitations = new ArrayList<>();
                    List<String> invalidInvitationIds = new ArrayList<>();

                    if (querySnapshot.isEmpty()) {
                        listener.onSuccess(validInvitations);
                        return;
                    }

                    int[] totalInvitations = {querySnapshot.size()};
                    int[] processedInvitations = {0};

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        AllianceInvitation invitation = doc.toObject(AllianceInvitation.class);
                        if (invitation != null) {
                            db.collection(COLLECTION_ALLIANCES)
                                    .document(invitation.getAllianceId())
                                    .get()
                                    .addOnSuccessListener(allianceDoc -> {
                                        if (allianceDoc.exists()) {
                                            validInvitations.add(invitation);
                                        } else {
                                            invalidInvitationIds.add(invitation.getInvitationId());
                                        }

                                        processedInvitations[0]++;
                                        if (processedInvitations[0] == totalInvitations[0]) {
                                            deleteInvalidInvitations(invalidInvitationIds);
                                            validInvitations.sort((i1, i2) ->
                                                    Long.compare(i2.getTimestamp(), i1.getTimestamp()));
                                            listener.onSuccess(validInvitations);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        processedInvitations[0]++;
                                        if (processedInvitations[0] == totalInvitations[0]) {
                                            validInvitations.sort((i1, i2) ->
                                                    Long.compare(i2.getTimestamp(), i1.getTimestamp()));
                                            listener.onSuccess(validInvitations);
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading invitations", e);
                    listener.onError(e);
                });
    }

    private void deleteInvalidInvitations(List<String> invitationIds) {
        if (invitationIds.isEmpty()) return;

        WriteBatch batch = db.batch();
        for (String invitationId : invitationIds) {
            batch.delete(db.collection(COLLECTION_ALLIANCE_INVITATIONS).document(invitationId));
        }
        batch.commit().addOnSuccessListener(v ->
                Log.d(TAG, "Deleted " + invitationIds.size() + " invalid invitations"));
    }

    // ===== ACCEPT INVITATION =====
    public void acceptInvitation(String invitationId, String allianceId, String userId,
                                 String currentAllianceId, boolean isCurrentUserLeader,
                                 OnAllianceActionListener listener) {

        Log.d(TAG, "Starting acceptInvitation for user: " + userId);

        // First, get the invitation to retrieve sender information
        db.collection(COLLECTION_ALLIANCE_INVITATIONS)
                .document(invitationId)
                .get()
                .addOnSuccessListener(invitationDoc -> {
                    if (!invitationDoc.exists()) {
                        Log.e(TAG, "Invitation not found");
                        listener.onError(new Exception("Invitation not found"));
                        return;
                    }

                    AllianceInvitation invitation = invitationDoc.toObject(AllianceInvitation.class);
                    if (invitation == null) {
                        Log.e(TAG, "Failed to parse invitation");
                        listener.onError(new Exception("Failed to parse invitation"));
                        return;
                    }

                    Log.d(TAG, "Invitation found. Sender: " + invitation.getSenderId() +
                            ", Alliance: " + invitation.getAllianceName());

                    WriteBatch batch = db.batch();
                    batch.delete(db.collection(COLLECTION_ALLIANCE_INVITATIONS).document(invitationId));

                    if (currentAllianceId != null && !currentAllianceId.isEmpty() && isCurrentUserLeader) {
                        db.collection(COLLECTION_ALLIANCES)
                                .document(currentAllianceId)
                                .get()
                                .addOnSuccessListener(allianceDoc -> {
                                    if (allianceDoc.exists()) {
                                        Alliance currentAlliance = allianceDoc.toObject(Alliance.class);
                                        if (currentAlliance != null) {
                                            disbandAllianceCompletely(currentAllianceId,
                                                    currentAlliance.getMemberIds(),
                                                    new OnAllianceActionListener() {
                                                        @Override
                                                        public void onSuccess() {
                                                            joinNewAllianceAndNotify(batch, allianceId,
                                                                    userId, invitation, listener);
                                                        }

                                                        @Override
                                                        public void onError(Exception e) {
                                                            listener.onError(e);
                                                        }
                                                    });
                                            return;
                                        }
                                    }
                                    joinNewAllianceAndNotify(batch, allianceId, userId, invitation, listener);
                                })
                                .addOnFailureListener(e ->
                                        joinNewAllianceAndNotify(batch, allianceId, userId, invitation, listener));
                    } else {
                        if (currentAllianceId != null && !currentAllianceId.isEmpty()) {
                            batch.update(db.collection(COLLECTION_ALLIANCES).document(currentAllianceId),
                                    "memberIds", FieldValue.arrayRemove(userId));
                        }
                        joinNewAllianceAndNotify(batch, allianceId, userId, invitation, listener);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading invitation", e);
                    listener.onError(e);
                });
    }

    private void joinNewAllianceAndNotify(WriteBatch batch, String allianceId, String userId,
                                          AllianceInvitation invitation,
                                          OnAllianceActionListener listener) {
        Log.d(TAG, "Joining new alliance and sending notification");

        batch.update(db.collection(COLLECTION_ALLIANCES).document(allianceId),
                "memberIds", FieldValue.arrayUnion(userId));

        batch.update(db.collection(COLLECTION_USERS).document(userId),
                "currentAllianceId", allianceId);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Alliance invitation accepted, now sending notification");

                    // Send notification to alliance leader
                    sendAcceptanceNotification(invitation.getSenderId(), userId,
                            invitation.getAllianceName());

                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error accepting invitation", e);
                    listener.onError(e);
                });
    }

    private void sendAcceptanceNotification(String leaderId, String acceptedUserId,
                                            String allianceName) {
        Log.d(TAG, "Sending acceptance notification to leader: " + leaderId);

        db.collection(COLLECTION_USERS)
                .document(acceptedUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        User acceptedUser = userDoc.toObject(User.class);
                        if (acceptedUser != null) {
                            Log.d(TAG, "Creating notification for user: " + acceptedUser.getUsername());

                            Map<String, Object> notificationData = new HashMap<>();
                            notificationData.put("type", "alliance_accepted");
                            notificationData.put("userId", leaderId);
                            notificationData.put("acceptedUsername", acceptedUser.getUsername());
                            notificationData.put("allianceName", allianceName);
                            notificationData.put("message", acceptedUser.getUsername() +
                                    " accepted your invitation to " + allianceName);
                            notificationData.put("timestamp", System.currentTimeMillis());
                            notificationData.put("read", false);

                            db.collection(COLLECTION_NOTIFICATIONS)
                                    .add(notificationData)
                                    .addOnSuccessListener(documentReference -> {
                                        Log.d(TAG, "Acceptance notification sent successfully. Doc ID: " +
                                                documentReference.getId());
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error sending acceptance notification", e);
                                    });
                        } else {
                            Log.e(TAG, "Failed to parse accepted user");
                        }
                    } else {
                        Log.e(TAG, "Accepted user document doesn't exist");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user for notification", e);
                });
    }

    // ===== DECLINE INVITATION =====
    public void declineInvitation(String invitationId, OnAllianceActionListener listener) {
        Log.d(TAG, "Starting declineInvitation");

        db.collection(COLLECTION_ALLIANCE_INVITATIONS)
                .document(invitationId)
                .get()
                .addOnSuccessListener(invitationDoc -> {
                    if (invitationDoc.exists()) {
                        AllianceInvitation invitation = invitationDoc.toObject(AllianceInvitation.class);
                        if (invitation != null) {
                            Log.d(TAG, "Invitation found, sending decline notification");

                            // Send notification to leader
                            sendDeclineNotification(invitation.getSenderId(),
                                    invitation.getReceiverId(),
                                    invitation.getAllianceName());

                            // Delete invitation
                            db.collection(COLLECTION_ALLIANCE_INVITATIONS)
                                    .document(invitationId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Alliance invitation declined and deleted");
                                        listener.onSuccess();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error declining invitation", e);
                                        listener.onError(e);
                                    });
                        } else {
                            listener.onError(new Exception("Failed to parse invitation"));
                        }
                    } else {
                        listener.onError(new Exception("Invitation not found"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    private void sendDeclineNotification(String leaderId, String declinedUserId,
                                         String allianceName) {
        Log.d(TAG, "Sending decline notification to leader: " + leaderId);

        db.collection(COLLECTION_USERS)
                .document(declinedUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        User declinedUser = userDoc.toObject(User.class);
                        if (declinedUser != null) {
                            Log.d(TAG, "Creating decline notification for user: " + declinedUser.getUsername());

                            Map<String, Object> notificationData = new HashMap<>();
                            notificationData.put("type", "alliance_declined");
                            notificationData.put("userId", leaderId);
                            notificationData.put("declinedUsername", declinedUser.getUsername());
                            notificationData.put("allianceName", allianceName);
                            notificationData.put("message", declinedUser.getUsername() +
                                    " declined your invitation to " + allianceName);
                            notificationData.put("timestamp", System.currentTimeMillis());
                            notificationData.put("read", false);

                            db.collection(COLLECTION_NOTIFICATIONS)
                                    .add(notificationData)
                                    .addOnSuccessListener(documentReference -> {
                                        Log.d(TAG, "Decline notification sent successfully. Doc ID: " +
                                                documentReference.getId());
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error sending decline notification", e);
                                    });
                        } else {
                            Log.e(TAG, "Failed to parse declined user");
                        }
                    } else {
                        Log.e(TAG, "Declined user document doesn't exist");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user for decline notification", e);
                });
    }

    // ===== LEAVE ALLIANCE =====
    public void leaveAlliance(String userId, String allianceId, OnAllianceActionListener listener) {
        WriteBatch batch = db.batch();

        batch.update(db.collection(COLLECTION_ALLIANCES).document(allianceId),
                "memberIds", FieldValue.arrayRemove(userId));

        batch.update(db.collection(COLLECTION_USERS).document(userId),
                "currentAllianceId", null);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User left alliance");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error leaving alliance", e);
                    listener.onError(e);
                });
    }

    // ===== CHECK IF CAN LEAVE ALLIANCE =====
    public void canLeaveAlliance(String allianceId, OnAllianceActionListener listener) {
        db.collection(COLLECTION_ALLIANCES)
                .document(allianceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Alliance alliance = doc.toObject(Alliance.class);
                        if (alliance != null) {
                            String missionId = alliance.getCurrentMissionId();
                            if (missionId != null && !missionId.isEmpty()) {
                                listener.onError(new Exception("Cannot leave alliance during active mission"));
                            } else {
                                listener.onSuccess();
                            }
                            return;
                        }
                    }
                    listener.onError(new Exception("Alliance not found"));
                })
                .addOnFailureListener(listener::onError);
    }

    // ===== DISBAND ALLIANCE COMPLETELY =====
    public void disbandAllianceCompletely(String allianceId, List<String> memberIds,
                                          OnAllianceActionListener listener) {
        WriteBatch batch = db.batch();

        batch.delete(db.collection(COLLECTION_ALLIANCES).document(allianceId));

        if (memberIds != null) {
            for (String memberId : memberIds) {
                batch.update(db.collection(COLLECTION_USERS).document(memberId),
                        "currentAllianceId", null);
            }
        }

        db.collection(COLLECTION_ALLIANCE_INVITATIONS)
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Alliance completely disbanded and deleted");
                                listener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error disbanding alliance", e);
                                listener.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Alliance disbanded (without invitation cleanup)");
                                listener.onSuccess();
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "Error disbanding alliance", e2);
                                listener.onError(e2);
                            });
                });
    }

    // ===== GET ALLIANCE MEMBERS =====
    public void getAllianceMembers(List<String> memberIds, UserRepository.OnUsersLoadListener listener) {
        if (memberIds == null || memberIds.isEmpty()) {
            listener.onSuccess(new ArrayList<>());
            return;
        }

        db.collection(COLLECTION_USERS)
                .whereIn("userId", memberIds)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> members = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            members.add(user);
                        }
                    }
                    listener.onSuccess(members);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading alliance members", e);
                    listener.onError(e);
                });
    }
}