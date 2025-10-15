package com.example.bossapp.data.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Alliance {
    private String allianceId;
    private String allianceName;
    private String leaderId; // vođa saveza
    private String leaderUsername;
    private List<String> memberIds; // lista ID-jeva članova
    private long createdAt;
    private boolean isActive;
    private String currentMissionId; // za buduću implementaciju misija

    public Alliance() {
        this.memberIds = new ArrayList<>();
    }

    public Alliance(String allianceId, String allianceName, String leaderId, String leaderUsername) {
        this.allianceId = allianceId;
        this.allianceName = allianceName;
        this.leaderId = leaderId;
        this.leaderUsername = leaderUsername;
        this.memberIds = new ArrayList<>();
        this.memberIds.add(leaderId); // vođa je automatski član
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
        this.currentMissionId = null;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("allianceId", allianceId);
        map.put("allianceName", allianceName);
        map.put("leaderId", leaderId);
        map.put("leaderUsername", leaderUsername);
        map.put("memberIds", memberIds != null ? memberIds : new ArrayList<>());
        map.put("createdAt", createdAt);
        map.put("isActive", isActive);
        map.put("currentMissionId", currentMissionId);
        return map;
    }

    // Getters and Setters
    public String getAllianceId() {
        return allianceId;
    }

    public void setAllianceId(String allianceId) {
        this.allianceId = allianceId;
    }

    public String getAllianceName() {
        return allianceName;
    }

    public void setAllianceName(String allianceName) {
        this.allianceName = allianceName;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    public String getLeaderUsername() {
        return leaderUsername;
    }

    public void setLeaderUsername(String leaderUsername) {
        this.leaderUsername = leaderUsername;
    }

    public List<String> getMemberIds() {
        return memberIds != null ? memberIds : new ArrayList<>();
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getCurrentMissionId() {
        return currentMissionId;
    }

    public void setCurrentMissionId(String currentMissionId) {
        this.currentMissionId = currentMissionId;
    }

    public int getMemberCount() {
        return memberIds != null ? memberIds.size() : 0;
    }

    public boolean isMember(String userId) {
        return memberIds != null && memberIds.contains(userId);
    }

    public boolean isLeader(String userId) {
        return leaderId != null && leaderId.equals(userId);
    }

    public boolean hasMission() {
        return currentMissionId != null && !currentMissionId.isEmpty();
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }
}