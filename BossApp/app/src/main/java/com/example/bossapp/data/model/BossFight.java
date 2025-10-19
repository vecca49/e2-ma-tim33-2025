package com.example.bossapp.data.model;

import java.util.Random;

public class BossFight {
    private String bossName;
    private int bossHp;
    private int userPp;
    private int maxAttempts;
    private int attemptsUsed;
    private int successRate;

    public BossFight(String bossName, int bossHp, int userPp, int successRate) {
        this.bossName = bossName;
        this.bossHp = bossHp;
        this.userPp = userPp;
        this.maxAttempts = 5;
        this.attemptsUsed = 0;
        this.successRate = successRate;
    }

    public boolean isBossDefeated() {
        return bossHp <= 0;
    }

    public boolean isFightOver() {
        return attemptsUsed >= maxAttempts || isBossDefeated();
    }

    public int getRemainingHp() {
        return Math.max(bossHp, 0);
    }

    public int getAttemptsLeft() {
        return maxAttempts - attemptsUsed;
    }

    public String attack() {
        if (isFightOver()) return "Borba je završena!";

        attemptsUsed++;
        Random random = new Random();
        int roll = random.nextInt(100);

        if (roll < successRate) {
            bossHp -= userPp;
            if (bossHp <= 0) {
                bossHp = 0;
                return "Kritični udarac! Bos je poražen!";
            }
            return "Pogodak! Bos sada ima " + bossHp + " HP.";
        } else {
            return "Promašaj! Bos i dalje ima " + bossHp + " HP.";
        }
    }

    public int calculateReward() {
        if (isBossDefeated()) return 200;
        else return 100;
    }

    public String getBossName() {
        return bossName;
    }
}
