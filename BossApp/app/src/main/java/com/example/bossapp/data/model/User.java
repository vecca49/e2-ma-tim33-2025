package com.example.bossapp.data.model;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String userId;
    private String email;
    private String username;
    private int avatarIndex; // 0-4 za 5 predefinisanih avatara
    private int level;
    private String title;
    private int xp;
    private int pp;
    private int coins;
    private long createdAt;
    private boolean emailVerified;

    public User() {

    }

    public User(String userId, String email, String username, int avatarIndex) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.avatarIndex = avatarIndex;
        this.level = 0;
        this.title = "Rookie"; // Početna titula
        this.xp = 0;
        this.pp = 0;
        this.coins = 0;
        this.createdAt = System.currentTimeMillis();
        this.emailVerified = false;
    }

    // Konverzija u Map za Firebase
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
}
