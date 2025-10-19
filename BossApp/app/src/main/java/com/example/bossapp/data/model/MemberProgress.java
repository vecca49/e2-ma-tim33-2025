package com.example.bossapp.data.model;

import java.util.HashMap;
import java.util.Map;

public class MemberProgress {
    private String username;
    private int storePurchases;
    private int bossHits;
    private int easyTasks;
    private int otherTasks;
    private boolean noUnfinishedTasks;
    private int messageDays;

    public MemberProgress() {}

    public MemberProgress(String username, int storePurchases, int bossHits,
                          int easyTasks, int otherTasks, boolean noUnfinishedTasks, int messageDays) {
        this.username = username;
        this.storePurchases = storePurchases;
        this.bossHits = bossHits;
        this.easyTasks = easyTasks;
        this.otherTasks = otherTasks;
        this.noUnfinishedTasks = noUnfinishedTasks;
        this.messageDays = messageDays;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("username", username);
        map.put("storePurchases", storePurchases);
        map.put("bossHits", bossHits);
        map.put("easyTasks", easyTasks);
        map.put("otherTasks", otherTasks);
        map.put("noUnfinishedTasks", noUnfinishedTasks);
        map.put("messageDays", messageDays);
        return map;
    }

    public int calculateTotalDamage() {
        int damage = 0;
        damage += Math.min(storePurchases, 5) * 2;
        damage += Math.min(bossHits, 10) * 2;
        damage += Math.min(easyTasks, 10);
        damage += Math.min(otherTasks, 6) * 4;
        if (noUnfinishedTasks) damage += 10;
        damage += messageDays * 4;
        return damage;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getStorePurchases() { return storePurchases; }
    public void setStorePurchases(int storePurchases) { this.storePurchases = storePurchases; }

    public int getBossHits() { return bossHits; }
    public void setBossHits(int bossHits) { this.bossHits = bossHits; }

    public int getEasyTasks() { return easyTasks; }
    public void setEasyTasks(int easyTasks) { this.easyTasks = easyTasks; }

    public int getOtherTasks() { return otherTasks; }
    public void setOtherTasks(int otherTasks) { this.otherTasks = otherTasks; }

    public boolean isNoUnfinishedTasks() { return noUnfinishedTasks; }
    public void setNoUnfinishedTasks(boolean noUnfinishedTasks) { this.noUnfinishedTasks = noUnfinishedTasks; }

    public int getMessageDays() { return messageDays; }
    public void setMessageDays(int messageDays) { this.messageDays = messageDays; }
}
