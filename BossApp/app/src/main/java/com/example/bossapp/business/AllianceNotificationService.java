package com.example.bossapp.business;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class AllianceNotificationService {
    private static final String TAG = "AllianceNotificationService";
    private Context context;
    private FirebaseFirestore db;
    private NotificationHelper notificationHelper;
    private ListenerRegistration notificationListener;

    public AllianceNotificationService(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.notificationHelper = new NotificationHelper(context);
    }

    public void startListening() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Listen for new notifications
        notificationListener = db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("read", false)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to notifications", error);
                        return;
                    }

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String type = doc.getString("type");
                            String message = doc.getString("message");

                            if ("alliance_accepted".equals(type)) {
                                String username = doc.getString("acceptedUsername");
                                String allianceName = doc.getString("allianceName");

                                notificationHelper.sendAllianceAcceptedNotification(username, allianceName);

                            } else if ("alliance_declined".equals(type)) {
                                String username = doc.getString("declinedUsername");
                                String allianceName = doc.getString("allianceName");

                                notificationHelper.sendAllianceDeclinedNotification(username, allianceName);
                            }

                            // Mark as read
                            doc.getReference().update("read", true);
                        }
                    }
                });
    }

    public void stopListening() {
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }
}