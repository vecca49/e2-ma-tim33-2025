package com.example.bossapp.data.repository;

import android.util.Log;

import com.example.bossapp.data.model.AllianceMessage;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllianceChatRepository {
    private static final String TAG = "AllianceChatRepository";
    private static final String COLLECTION_MESSAGES = "allianceMessages";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static final String COLLECTION_ALLIANCES = "alliances";

    private FirebaseFirestore db;

    public AllianceChatRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface OnMessageSendListener {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnMessagesLoadListener {
        void onSuccess(List<AllianceMessage> messages);
        void onError(Exception e);
    }

    // Send message
    public void sendMessage(AllianceMessage message, OnMessageSendListener listener) {
        db.collection(COLLECTION_MESSAGES)
                .document(message.getMessageId())
                .set(message.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message sent successfully");

                    // Send notifications to other alliance members
                    sendMessageNotifications(message);

                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message", e);
                    listener.onError(e);
                });
    }

    // Send notifications to alliance members (except sender)
    private void sendMessageNotifications(AllianceMessage message) {
        // Get alliance members
        db.collection(COLLECTION_ALLIANCES)
                .document(message.getAllianceId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> memberIds = (List<String>) doc.get("memberIds");
                        if (memberIds != null) {
                            for (String memberId : memberIds) {
                                // Don't send notification to sender
                                if (!memberId.equals(message.getSenderId())) {
                                    sendNotification(memberId, message);
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading alliance members for notifications", e);
                });
    }

    private void sendNotification(String userId, AllianceMessage message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "alliance_message");
        notification.put("userId", userId);
        notification.put("allianceId", message.getAllianceId());
        notification.put("senderUsername", message.getSenderUsername());
        notification.put("messageText", message.getMessageText());
        notification.put("message", message.getSenderUsername() + ": " +
                (message.getMessageText().length() > 50 ?
                        message.getMessageText().substring(0, 50) + "..." :
                        message.getMessageText()));
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);

        db.collection(COLLECTION_NOTIFICATIONS)
                .add(notification)
                .addOnSuccessListener(ref -> {
                    Log.d(TAG, "Message notification sent to user: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message notification", e);
                });
    }

    // Load messages for alliance (one-time load)
    public void loadMessages(String allianceId, OnMessagesLoadListener listener) {
        Log.d(TAG, "Loading messages for alliance: " + allianceId);

        db.collection(COLLECTION_MESSAGES)
                .whereEqualTo("allianceId", allianceId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Messages loaded successfully. Count: " + querySnapshot.size());
                    List<AllianceMessage> messages = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        AllianceMessage msg = doc.toObject(AllianceMessage.class);
                        if (msg != null) {
                            messages.add(msg);
                            Log.d(TAG, "Message from: " + msg.getSenderUsername());
                        }
                    }
                    listener.onSuccess(messages);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading messages: " + e.getMessage(), e);
                    listener.onError(e);
                });
    }

    // Real-time listener for messages
    public ListenerRegistration listenToMessages(String allianceId,
                                                 com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot> eventListener) {
        Log.d(TAG, "Starting real-time listener for alliance: " + allianceId);

        return db.collection(COLLECTION_MESSAGES)
                .whereEqualTo("allianceId", allianceId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(eventListener);
    }
}