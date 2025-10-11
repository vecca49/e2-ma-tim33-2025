package com.example.bossapp.data.model;

import java.util.HashMap;
import java.util.Map;

public class FriendRequest {
    private String requestId;
    private String senderId;
    private String receiverId;
    private String senderUsername;
    private int senderAvatarIndex;
    private long timestamp;
    private String status; // "pending", "accepted", "rejected"

    public FriendRequest() {}

    public FriendRequest(String senderId, String receiverId,
                         String senderUsername, int senderAvatarIndex) {
        this.requestId = senderId + "_" + receiverId + "_" + System.currentTimeMillis();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderUsername = senderUsername;
        this.senderAvatarIndex = senderAvatarIndex;
        this.timestamp = System.currentTimeMillis();
        this.status = "pending";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestId", requestId);
        map.put("senderId", senderId);
        map.put("receiverId", receiverId);
        map.put("senderUsername", senderUsername);
        map.put("senderAvatarIndex", senderAvatarIndex);
        map.put("timestamp", timestamp);
        map.put("status", status);
        return map;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public int getSenderAvatarIndex() { return senderAvatarIndex; }
    public void setSenderAvatarIndex(int senderAvatarIndex) {
        this.senderAvatarIndex = senderAvatarIndex;
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}