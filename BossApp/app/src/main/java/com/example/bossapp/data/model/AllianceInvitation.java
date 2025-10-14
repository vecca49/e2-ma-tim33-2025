package com.example.bossapp.data.model;

import java.util.HashMap;
import java.util.Map;

public class AllianceInvitation {
    private String invitationId;
    private String allianceId;
    private String allianceName;
    private String senderId;
    private String senderUsername;
    private String receiverId;
    private long timestamp;
    private String status;

    public AllianceInvitation() {}

    public AllianceInvitation(String allianceId, String allianceName,
                              String senderId, String senderUsername, String receiverId) {
        this.invitationId = allianceId + "_" + receiverId + "_" + System.currentTimeMillis();
        this.allianceId = allianceId;
        this.allianceName = allianceName;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.receiverId = receiverId;
        this.timestamp = System.currentTimeMillis();
        this.status = "pending";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("invitationId", invitationId);
        map.put("allianceId", allianceId);
        map.put("allianceName", allianceName);
        map.put("senderId", senderId);
        map.put("senderUsername", senderUsername);
        map.put("receiverId", receiverId);
        map.put("timestamp", timestamp);
        map.put("status", status);
        return map;
    }

    // Getters and Setters
    public String getInvitationId() { return invitationId; }
    public void setInvitationId(String invitationId) { this.invitationId = invitationId; }

    public String getAllianceId() { return allianceId; }
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }

    public String getAllianceName() { return allianceName; }
    public void setAllianceName(String allianceName) { this.allianceName = allianceName; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}