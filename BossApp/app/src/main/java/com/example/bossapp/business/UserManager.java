package com.example.bossapp.business;

import android.util.Log;

import com.example.bossapp.data.model.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class UserManager {

    private static final String TAG = "UserManager";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnUserLoadListener {
        void onSuccess(User user);
        void onError(String message);
    }

    public interface OnUserOperationListener {
        void onSuccess();
        void onError(String message);
    }

    public ListenerRegistration observeUserChanges(String userId, OnUserLoadListener listener) {
        DocumentReference userRef = db.collection("users").document(userId);

        return userRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Greška u snapshot listeneru: ", e);
                listener.onError(e.getMessage());
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                User user = snapshot.toObject(User.class);
                listener.onSuccess(user);
            } else {
                listener.onError("Korisnik nije pronađen");
            }
        });
    }

    public void getUserById(String userId, OnUserLoadListener listener) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        listener.onSuccess(doc.toObject(User.class));
                    } else {
                        listener.onError("Korisnik ne postoji");
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }


    public void updateUser(User user, OnUserOperationListener listener) {
        db.collection("users").document(user.getUserId())
                .set(user)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
}
