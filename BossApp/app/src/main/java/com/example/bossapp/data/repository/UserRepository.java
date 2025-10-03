package com.example.bossapp.data.repository;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.bossapp.data.model.User;

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

    // Provera da li username već postoji
    public void checkUsernameExists(String username, OnUserCheckListener listener) {
        Log.d(TAG, "Provera username-a: " + username);

        db.collection(COLLECTION_USERS)
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean exists = !querySnapshot.isEmpty();
                    Log.d(TAG, "Username postoji: " + exists);
                    listener.onResult(exists);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Greška pri proveri username-a", e);
                    listener.onError(e);
                });
    }

    // Čuvanje korisnika u Firestore
    public void saveUser(User user, OnUserSaveListener listener) {
        Log.d(TAG, "Čuvanje korisnika: " + user.getUsername());

        db.collection(COLLECTION_USERS)
                .document(user.getUserId())
                .set(user.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Korisnik uspešno sačuvan");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Greška pri čuvanju korisnika", e);
                    listener.onError(e);
                });
    }

    // Učitavanje korisnika iz Firestore
    public void getUserById(String userId, OnUserLoadListener listener) {
        Log.d(TAG, "Učitavanje korisnika: " + userId);

        db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        Log.d(TAG, "Korisnik učitan: " + user.getUsername());
                        listener.onSuccess(user);
                    } else {
                        Log.e(TAG, "Korisnik ne postoji");
                        listener.onError(new Exception("Korisnik ne postoji"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Greška pri učitavanju korisnika", e);
                    listener.onError(e);
                });
    }

    public interface OnUserLoadListener {
        void onSuccess(User user);
        void onError(Exception e);
    }
}