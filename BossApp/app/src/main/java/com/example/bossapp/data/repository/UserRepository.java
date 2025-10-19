package com.example.bossapp.data.repository;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.bossapp.data.model.User;

import java.util.List;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private static final String COLLECTION_USERS = "users";
    private FirebaseFirestore db;

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface OnUserCheckListener {
        void onResult(boolean exists);
        void onError(Exception e);
    }

    public interface OnUserSaveListener {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnUsersLoadListener {
        void onSuccess(List<User> users);
        void onError(Exception e);
    }

    public void checkUsernameExists(String username, OnUserCheckListener listener) {
        Log.d(TAG, "Username verification: " + username);

        db.collection(COLLECTION_USERS)
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean exists = !querySnapshot.isEmpty();
                    Log.d(TAG, "Username exists: " + exists);
                    listener.onResult(exists);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking username", e);
                    listener.onError(e);
                });
    }

    public void saveUser(User user, OnUserSaveListener listener) {
        Log.d(TAG, "Saving user: " + user.getUsername());

        db.collection(COLLECTION_USERS)
                .document(user.getUserId())
                .set(user.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User successfully saved");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user", e);
                    listener.onError(e);
                });
    }

    // Učitavanje korisnika iz Firestore
    public void getUserById(String userId, OnUserLoadListener listener) {
        Log.d(TAG, "Loading user: " + userId);

        db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        Log.d(TAG, "User loaded: " + user.getUsername());
                        listener.onSuccess(user);
                    } else {
                        Log.e(TAG, "User does not exist.");
                        listener.onError(new Exception("User does not exist."));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user", e);
                    listener.onError(e);
                });
    }

    public interface OnUserLoadListener {
        void onSuccess(User user);
        void onError(Exception e);
    }
}