package com.example.bossapp.data.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllianceMission {
    private String missionId;
    private String allianceId;
    private long startTime;
    private long endTime;
    private int totalBossHp;
    private int currentHp;
    private boolean active;
    private Map<String, MemberProgress> memberProgressMap;

    public AllianceMission() {}

    public AllianceMission(String missionId, String allianceId, int memberCount) {
        this.missionId = missionId;
        this.allianceId = allianceId;
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + 14L * 24 * 60 * 60 * 1000;
        this.totalBossHp = 100 * memberCount;
        this.currentHp = this.totalBossHp;
        this.active = true;
        this.memberProgressMap = new HashMap<>();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("missionId", missionId);
        map.put("allianceId", allianceId);
        map.put("startTime", startTime);
        map.put("endTime", endTime);
        map.put("totalBossHp", totalBossHp);
        map.put("currentHp", currentHp);
        map.put("active", active);
        map.put("memberProgressMap", memberProgressMap);
        return map;
    }

    public void reduceBossHp(int amount) {
        currentHp = Math.max(0, currentHp - amount);
        if (currentHp == 0) active = false;
    }

    public boolean isFinished() {
        return !active || System.currentTimeMillis() > endTime;
    }

    public String getMissionId() { return missionId; }
    public void setMissionId(String missionId) { this.missionId = missionId; }

    public String getAllianceId() { return allianceId; }
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public int getTotalBossHp() { return totalBossHp; }
    public void setTotalBossHp(int totalBossHp) { this.totalBossHp = totalBossHp; }

    public int getCurrentHp() { return currentHp; }
    public void setCurrentHp(int currentHp) { this.currentHp = currentHp; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Map<String, MemberProgress> getMemberProgressMap() { return memberProgressMap; }
    public void setMemberProgressMap(Map<String, MemberProgress> memberProgressMap) { this.memberProgressMap = memberProgressMap; }
}
