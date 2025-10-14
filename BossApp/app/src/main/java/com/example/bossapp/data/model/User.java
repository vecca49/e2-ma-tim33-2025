package com.example.bossapp.data.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    private String userId;
    private String email;
    private String username;
    private int avatarIndex; // 0-4 for 5 predefined avatars
    private int level;
    private String title;
    private int xp;
    private int pp;
    private int coins;
    private long createdAt;
    private boolean emailVerified;
    private List<String> friendIds;
    private String currentAllianceId;

    public User() {
        this.friendIds = new ArrayList<>();
    }

    public User(String userId, String email, String username, int avatarIndex) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.avatarIndex = avatarIndex;
        this.level = 0;
        this.title = "Rookie"; // title at the beginning
        this.xp = 0;
        this.pp = 0;
        this.coins = 0;
        this.createdAt = System.currentTimeMillis();
        this.emailVerified = false;
        this.friendIds = new ArrayList<>();
        this.currentAllianceId = null;
    }

    // Convert to Map for Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("email", email);
        map.put("username", username);
        map.put("avatarIndex", avatarIndex);
        map.put("level", level);
        map.put("title", title);
        map.put("xp", xp);
        map.put("pp", pp);
        map.put("coins", coins);
        map.put("createdAt", createdAt);
        map.put("emailVerified", emailVerified);
        map.put("friendIds", friendIds != null ? friendIds : new ArrayList<>());
        map.put("currentAllianceId", currentAllianceId); // FIXED: was "currentAlianceId"
        return map;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getAvatarIndex() { return avatarIndex; }
    public void setAvatarIndex(int avatarIndex) { this.avatarIndex = avatarIndex; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public int getPp() { return pp; }
    public void setPp(int pp) { this.pp = pp; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public int getPowerPoints() {
        return pp;
    }

    public void setPowerPoints(int powerPoints) {
        this.pp = powerPoints;
    }

    public int getExperiencePoints() {
        return xp;
    }

    public void setExperiencePoints(int experiencePoints) {
        this.xp = experiencePoints;
    }

    public int getBadges() {
        // Za sada vraćaj 0, kasnije ćeš implementirati badges sistem
        return 0;
    }

    public void setBadges(int badges) {
        // TODO: Implement badges later
    }

    public List<String> getFriendIds() {
        return friendIds != null ? friendIds : new ArrayList<>();
    }
    public void setFriendIds(List<String> friendIds) {
        this.friendIds = friendIds;
    }

    public String getCurrentAllianceId() { return currentAllianceId; }
    public void setCurrentAllianceId(String currentAllianceId) {
        this.currentAllianceId = currentAllianceId;
    }

    public boolean isFriend(String userId) {
        return friendIds != null && friendIds.contains(userId);
    }
}