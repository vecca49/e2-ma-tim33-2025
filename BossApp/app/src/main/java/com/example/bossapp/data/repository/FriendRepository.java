package com.example.bossapp.data.repository;

import android.util.Log;

import com.example.bossapp.data.model.FriendRequest;
import com.example.bossapp.data.model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FriendRepository {
    private static final String TAG = "FriendRepository";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_FRIEND_REQUESTS = "friendRequests";

    public FirebaseFirestore db;

    public FriendRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface OnFriendRequestListener {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnFriendRequestCheckListener {
        void onResult(boolean exists, String requestId);
        void onError(Exception e);
    }

    public interface OnFriendRequestsLoadListener {
        void onSuccess(List<FriendRequest> requests);
        void onError(Exception e);
    }

    public void sendFriendRequest(String senderId, String receiverId,
                                  String senderUsername, int senderAvatarIndex,
                                  OnFriendRequestListener listener) {
        checkIfAlreadyFriendsOrRequested(senderId, receiverId, new OnFriendRequestCheckListener() {
            @Override
            public void onResult(boolean exists, String requestId) {
                if (exists) {
                    listener.onError(new Exception("Friend request already exists or you are already friends"));
                    return;
                }

                FriendRequest request = new FriendRequest(senderId, receiverId,
                        senderUsername, senderAvatarIndex);

                db.collection(COLLECTION_FRIEND_REQUESTS)
                        .document(request.getRequestId())
                        .set(request.toMap())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Friend request sent successfully");
                            listener.onSuccess();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error sending friend request", e);
                            listener.onError(e);
                        });
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e);
            }
        });
    }

    private void checkIfAlreadyFriendsOrRequested(String user1Id, String user2Id,
                                                  OnFriendRequestCheckListener listener) {
        db.collection(COLLECTION_USERS)
                .document(user1Id)
                .get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user != null && user.isFriend(user2Id)) {
                        listener.onResult(true, null);
                        return;
                    }

                    db.collection(COLLECTION_FRIEND_REQUESTS)
                            .whereEqualTo("senderId", user1Id)
                            .whereEqualTo("receiverId", user2Id)
                            .whereEqualTo("status", "pending")
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    listener.onResult(true, querySnapshot.getDocuments().get(0).getId());
                                } else {
                                    db.collection(COLLECTION_FRIEND_REQUESTS)
                                            .whereEqualTo("senderId", user2Id)
                                            .whereEqualTo("receiverId", user1Id)
                                            .whereEqualTo("status", "pending")
                                            .get()
                                            .addOnSuccessListener(qs2 -> {
                                                if (!qs2.isEmpty()) {
                                                    listener.onResult(true, qs2.getDocuments().get(0).getId());
                                                } else {
                                                    listener.onResult(false, null);
                                                }
                                            })
                                            .addOnFailureListener(listener::onError);
                                }
                            })
                            .addOnFailureListener(listener::onError);
                })
                .addOnFailureListener(listener::onError);
    }

    public void acceptFriendRequest(String requestId, String senderId, String receiverId,
                                    OnFriendRequestListener listener) {
        // Use a batch write to ensure atomicity
        WriteBatch batch = db.batch();

        // Update request status
        batch.update(db.collection(COLLECTION_FRIEND_REQUESTS).document(requestId),
                "status", "accepted");

        // Add to both users' friend lists
        batch.update(db.collection(COLLECTION_USERS).document(senderId),
                "friendIds", FieldValue.arrayUnion(receiverId));

        batch.update(db.collection(COLLECTION_USERS).document(receiverId),
                "friendIds", FieldValue.arrayUnion(senderId));

        // Commit the batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Friend request accepted and friends added to both users");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error accepting friend request", e);
                    listener.onError(e);
                });
    }

    public void rejectFriendRequest(String requestId, OnFriendRequestListener listener) {
        db.collection(COLLECTION_FRIEND_REQUESTS)
                .document(requestId)
                .update("status", "rejected")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Friend request rejected");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error rejecting friend request", e);
                    listener.onError(e);
                });
    }

    public void getPendingRequests(String userId, OnFriendRequestsLoadListener listener) {
        db.collection(COLLECTION_FRIEND_REQUESTS)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<FriendRequest> requests = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        FriendRequest request = doc.toObject(FriendRequest.class);
                        if (request != null) {
                            requests.add(request);
                        }
                    }
                    requests.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                    listener.onSuccess(requests);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading friend requests", e);
                    listener.onError(e);
                });
    }

    public void removeFriend(String userId, String friendId, OnFriendRequestListener listener) {
        WriteBatch batch = db.batch();

        batch.update(db.collection(COLLECTION_USERS).document(userId),
                "friendIds", FieldValue.arrayRemove(friendId));

        batch.update(db.collection(COLLECTION_USERS).document(friendId),
                "friendIds", FieldValue.arrayRemove(userId));

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Friendship removed from both users");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing friend", e);
                    listener.onError(e);
                });
    }

    public void getFriends(String userId, UserRepository.OnUsersLoadListener listener) {
        db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user == null || user.getFriendIds().isEmpty()) {
                        listener.onSuccess(new ArrayList<>());
                        return;
                    }

                    db.collection(COLLECTION_USERS)
                            .whereIn("userId", user.getFriendIds())
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<User> friends = new ArrayList<>();
                                for (DocumentSnapshot friendDoc : querySnapshot.getDocuments()) {
                                    User friend = friendDoc.toObject(User.class);
                                    if (friend != null) {
                                        friends.add(friend);
                                    }
                                }
                                listener.onSuccess(friends);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error loading friends", e);
                                listener.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user", e);
                    listener.onError(e);
                });
    }

    public void searchUsersByUsername(String query, String currentUserId,
                                      UserRepository.OnUsersLoadListener listener) {
        db.collection(COLLECTION_USERS)
                .orderBy("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null && !user.getUserId().equals(currentUserId)) {
                            users.add(user);
                        }
                    }
                    listener.onSuccess(users);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching users", e);
                    listener.onError(e);
                });
    }

    public void getAllUsers(String currentUserId, UserRepository.OnUsersLoadListener listener) {
        db.collection(COLLECTION_USERS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null && !user.getUserId().equals(currentUserId)) {
                            users.add(user);
                        }
                    }
                    listener.onSuccess(users);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading all users", e);
                    listener.onError(e);
                });
    }

    public ListenerRegistration listenToFriendRequests(String userId,
                                                       com.google.firebase.firestore.EventListener<QuerySnapshot> listener) {
        return db.collection(COLLECTION_FRIEND_REQUESTS)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener(listener);
    }
}