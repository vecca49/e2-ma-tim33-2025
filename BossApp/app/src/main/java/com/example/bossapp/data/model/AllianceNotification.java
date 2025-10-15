package com.example.bossapp.data.model;

import java.util.HashMap;
import java.util.Map;

public class AllianceNotification {
    private String notificationId;
    private String type; // "alliance_accepted" ili "alliance_declined"
    private String userId;
    private String username; // acceptedUsername ili declinedUsername
    private String allianceName;
    private String message;
    private long timestamp;
    private boolean read;

    public AllianceNotification() {}

    public AllianceNotification(String notificationId, String type, String userId,
                                String username, String allianceName, String message,
                                long timestamp, boolean read) {
        this.notificationId = notificationId;
        this.type = type;
        this.userId = userId;
        this.username = username;
        this.allianceName = allianceName;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("notificationId", notificationId);
        map.put("type", type);
        map.put("userId", userId);
        map.put("username", username);
        map.put("allianceName", allianceName);
        map.put("message", message);
        map.put("timestamp", timestamp);
        map.put("read", read);
        return map;
    }

    // Getters and Setters
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAllianceName() { return allianceName; }
    public void setAllianceName(String allianceName) { this.allianceName = allianceName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}