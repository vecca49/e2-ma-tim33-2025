package com.example.bossapp.business;

import android.util.Log;
import com.example.bossapp.data.model.Boss;
import com.example.bossapp.data.model.User;
import java.util.Random;

public class BossBattleManager {
    private static final String TAG = "BossBattleManager";
    private static final int MAX_ATTACKS = 5;

    private int attacksLeft;
    private Boss currentBoss;
    private User player;
    private int bossHpRemaining;
    private int successRate;
    private int coinsReward;
    private boolean battleEnded;


    public void setSuccessRate(int rate) {
        this.successRate = rate;
    }


    public interface OnBattleResultListener {
        void onBattleResult(boolean bossDefeated, int coinsEarned, boolean itemDropped);
    }

    public BossBattleManager(User player, Boss currentBoss, int successRate) {
        this.player = player;
        this.currentBoss = currentBoss;
        this.successRate = successRate;
        this.bossHpRemaining = currentBoss.getHp();
        this.attacksLeft = MAX_ATTACKS;
        this.battleEnded = false;
    }

    public int getAttacksLeft() { return attacksLeft; }
    public int getBossHpRemaining() { return bossHpRemaining; }
    public int getBossHpMax() { return currentBoss.getHp(); }
    public boolean isBattleEnded() { return battleEnded; }

    public boolean performAttack(OnBattleResultListener listener) {
        if (battleEnded || attacksLeft <= 0) return false;

        attacksLeft--;

        Random random = new Random();
        int roll = random.nextInt(100);

        boolean hit = roll < successRate;

        if (hit) {
            bossHpRemaining -= player.getPowerPoints();
            Log.d(TAG, "💥 Pogodak! - " + player.getPowerPoints() + " štete bosiću!");
            if (bossHpRemaining <= 0) {
                bossHpRemaining = 0;
                battleEnded = true;
                handleVictory(listener);
            }
        } else {
            Log.d(TAG, "😬 Promašaj! (roll=" + roll + ", successRate=" + successRate + ")");
            if (attacksLeft == 0 && !battleEnded) {
                handleDefeat(listener);
            }
        }

        if (attacksLeft == 0 && !battleEnded) {
            handleDefeat(listener);
        }

        return hit;
    }

    private void handleVictory(OnBattleResultListener listener) {
        Log.d(TAG, "🎉 Boss poražen!");

        if (currentBoss.getBossNumber() == 1) {
            coinsReward = 200;
        } else {
            int prevCoins = (int) (200 * Math.pow(1.2, currentBoss.getBossNumber() - 1));
            coinsReward = prevCoins;
        }

        player.setCoins(player.getCoins() + coinsReward);
        currentBoss.setDefeated(true);

        boolean itemDropped = new Random().nextInt(100) < 20; // 20% šanse
        listener.onBattleResult(true, coinsReward, itemDropped);
    }

    private void handleDefeat(OnBattleResultListener listener) {
        Log.d(TAG, "💀 Boss preživeo borbu...");
        int hpLostPercent = (int) (100 - ((bossHpRemaining * 100.0) / currentBoss.getHp()));

        boolean halfReward = hpLostPercent >= 50;
        int baseCoins = currentBoss.getBossNumber() == 1 ? 200 : (int) (200 * Math.pow(1.2, currentBoss.getBossNumber() - 1));
        coinsReward = halfReward ? baseCoins / 2 : 0;

        boolean itemDropped = halfReward && (new Random().nextInt(100) < 10);

        if (coinsReward > 0) {
            player.setCoins(player.getCoins() + coinsReward);
        }

        battleEnded = true;
        listener.onBattleResult(false, coinsReward, itemDropped);
    }

    public boolean canShowBoss(User player) {
        if (currentBoss.isDefeated()) return true;

        if (player.getLevel() < currentBoss.getBossNumber()) return false;


        if (currentBoss.getBossNumber() > 1) {
            int prevBossNumber = currentBoss.getBossNumber() - 1;

            boolean prevBossDefeated = false;
            if (!prevBossDefeated) return false;
        }

        return true;
    }


    public String getBossLockedMessage(User player) {
        if (player.getLevel() < currentBoss.getBossNumber()) {
            return "You must complete the previous level to challenge this boss!";
        }
        return "Defeat the previous boss to unlock this one!";
    }


}
