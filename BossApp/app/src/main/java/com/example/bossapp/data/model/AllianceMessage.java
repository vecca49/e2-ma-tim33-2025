package com.example.bossapp.data.model;

import java.util.HashMap;
import java.util.Map;

public class AllianceMessage {
    private String messageId;
    private String allianceId;
    private String senderId;
    private String senderUsername;
    private int senderAvatarIndex;
    private String messageText;
    private long timestamp;

    public AllianceMessage() {}

    public AllianceMessage(String allianceId, String senderId, String senderUsername,
                           int senderAvatarIndex, String messageText) {
        this.messageId = System.currentTimeMillis() + "_" + senderId;
        this.allianceId = allianceId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderAvatarIndex = senderAvatarIndex;
        this.messageText = messageText;
        this.timestamp = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("messageId", messageId);
        map.put("allianceId", allianceId);
        map.put("senderId", senderId);
        map.put("senderUsername", senderUsername);
        map.put("senderAvatarIndex", senderAvatarIndex);
        map.put("messageText", messageText);
        map.put("timestamp", timestamp);
        return map;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getAllianceId() {
        return allianceId;
    }

    public void setAllianceId(String allianceId) {
        this.allianceId = allianceId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public int getSenderAvatarIndex() {
        return senderAvatarIndex;
    }

    public void setSenderAvatarIndex(int senderAvatarIndex) {
        this.senderAvatarIndex = senderAvatarIndex;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}