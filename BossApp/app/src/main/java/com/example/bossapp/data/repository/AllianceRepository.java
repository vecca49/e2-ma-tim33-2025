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
import java.util.List;

public class AllianceRepository {
    private static final String TAG = "AllianceRepository";
    private static final String COLLECTION_ALLIANCES = "alliances";
    private static final String COLLECTION_ALLIANCE_INVITATIONS = "allianceInvitations";
    private static final String COLLECTION_USERS = "users";

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
                    // Update user's currentAllianceId
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
                                    // Savez ne postoji više, ukloni ga kod korisnika
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
        // First check if alliance still exists
        db.collection(COLLECTION_ALLIANCES)
                .document(allianceId)
                .get()
                .addOnSuccessListener(allianceDoc -> {
                    if (!allianceDoc.exists()) {
                        listener.onError(new Exception("Alliance no longer exists"));
                        return;
                    }

                    // Check if user already has pending invitation
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
        // Check only for pending invitations
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

    // ===== GET PENDING INVITATIONS (with alliance existence check) =====
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
                            // Check if alliance still exists
                            db.collection(COLLECTION_ALLIANCES)
                                    .document(invitation.getAllianceId())
                                    .get()
                                    .addOnSuccessListener(allianceDoc -> {
                                        if (allianceDoc.exists()) {
                                            validInvitations.add(invitation);
                                        } else {
                                            // Mark for deletion
                                            invalidInvitationIds.add(invitation.getInvitationId());
                                        }

                                        processedInvitations[0]++;
                                        if (processedInvitations[0] == totalInvitations[0]) {
                                            // Delete invalid invitations
                                            deleteInvalidInvitations(invalidInvitationIds);

                                            // Sort and return valid invitations
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
        WriteBatch batch = db.batch();

        // Delete invitation instead of updating status
        batch.delete(db.collection(COLLECTION_ALLIANCE_INVITATIONS).document(invitationId));

        // If user is leader of current alliance, disband it completely
        if (currentAllianceId != null && !currentAllianceId.isEmpty() && isCurrentUserLeader) {
            // Get current alliance to find all members
            db.collection(COLLECTION_ALLIANCES)
                    .document(currentAllianceId)
                    .get()
                    .addOnSuccessListener(allianceDoc -> {
                        if (allianceDoc.exists()) {
                            Alliance currentAlliance = allianceDoc.toObject(Alliance.class);
                            if (currentAlliance != null) {
                                // Complete disbanding - delete alliance and clear all members
                                disbandAllianceCompletely(currentAllianceId,
                                        currentAlliance.getMemberIds(),
                                        new OnAllianceActionListener() {
                                            @Override
                                            public void onSuccess() {
                                                // Now join new alliance
                                                joinNewAlliance(batch, allianceId, userId, listener);
                                            }

                                            @Override
                                            public void onError(Exception e) {
                                                listener.onError(e);
                                            }
                                        });
                                return;
                            }
                        }
                        // If no current alliance, just join new one
                        joinNewAlliance(batch, allianceId, userId, listener);
                    })
                    .addOnFailureListener(e -> joinNewAlliance(batch, allianceId, userId, listener));
        } else {
            // Leave current alliance if exists (but not leader)
            if (currentAllianceId != null && !currentAllianceId.isEmpty()) {
                batch.update(db.collection(COLLECTION_ALLIANCES).document(currentAllianceId),
                        "memberIds", FieldValue.arrayRemove(userId));
            }

            // Join new alliance
            joinNewAlliance(batch, allianceId, userId, listener);
        }
    }

    private void joinNewAlliance(WriteBatch batch, String allianceId, String userId,
                                 OnAllianceActionListener listener) {
        // Add user to new alliance
        batch.update(db.collection(COLLECTION_ALLIANCES).document(allianceId),
                "memberIds", FieldValue.arrayUnion(userId));

        // Update user's currentAllianceId
        batch.update(db.collection(COLLECTION_USERS).document(userId),
                "currentAllianceId", allianceId);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Alliance invitation accepted");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error accepting invitation", e);
                    listener.onError(e);
                });
    }

    // ===== DECLINE INVITATION =====
    public void declineInvitation(String invitationId, OnAllianceActionListener listener) {
        // DELETE invitation instead of updating status
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
    }

    // ===== LEAVE ALLIANCE =====
    public void leaveAlliance(String userId, String allianceId, OnAllianceActionListener listener) {
        WriteBatch batch = db.batch();

        // Remove user from alliance members
        batch.update(db.collection(COLLECTION_ALLIANCES).document(allianceId),
                "memberIds", FieldValue.arrayRemove(userId));

        // Clear user's currentAllianceId
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

    // ===== DISBAND ALLIANCE COMPLETELY (delete from DB) =====
    public void disbandAllianceCompletely(String allianceId, List<String> memberIds,
                                          OnAllianceActionListener listener) {
        WriteBatch batch = db.batch();

        // DELETE alliance document completely
        batch.delete(db.collection(COLLECTION_ALLIANCES).document(allianceId));

        // Remove alliance from all members
        if (memberIds != null) {
            for (String memberId : memberIds) {
                batch.update(db.collection(COLLECTION_USERS).document(memberId),
                        "currentAllianceId", null);
            }
        }

        // Delete all pending invitations for this alliance
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
                    // If we can't get invitations, still commit the batch
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