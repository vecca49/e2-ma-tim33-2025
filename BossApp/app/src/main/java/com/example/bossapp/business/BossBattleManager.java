package com.example.bossapp.business;

import android.util.Log;

import com.example.bossapp.data.model.Boss;
import com.example.bossapp.data.model.MemberProgress;
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.repository.AllianceRepository;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
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

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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

    public Boss getCurrentBoss() {
        return currentBoss;
    }


    public boolean performAttack(OnBattleResultListener listener) {
        if (battleEnded || attacksLeft <= 0) return false;

        attacksLeft--;
        Random random = new Random();
        int roll = random.nextInt(100);
        boolean hit = roll < successRate;

        if (hit) {
            bossHpRemaining -= player.getPowerPoints();
            Log.d(TAG, "💥 Pogodak! -" + player.getPowerPoints() + " štete!");





            if (bossHpRemaining <= 0) {
                bossHpRemaining = 0;
                battleEnded = true;
                handleVictory(listener);
            }
        } else {
            Log.d(TAG, "😬 Promašaj! (roll=" + roll + ", successRate=" + successRate + ")");
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
            coinsReward = (int) (200 * Math.pow(1.2, currentBoss.getBossNumber() - 1));
        }

        boolean itemDropped = new Random().nextInt(100) < 20;

        if (itemDropped) {
            boolean isWeapon = new Random().nextInt(100) < 5;
            Log.d(TAG, "🎁 Dobijen item: " + (isWeapon ? "Oružje" : "Odeća"));
        }

        updatePlayerCoins(coinsReward);
        currentBoss.setDefeated(true);
        listener.onBattleResult(true, coinsReward, itemDropped);
    }

    private void handleDefeat(OnBattleResultListener listener) {
        Log.d(TAG, "💀 Boss preživeo borbu...");

        int hpLostPercent = (int) (100 - ((bossHpRemaining * 100.0) / currentBoss.getHp()));
        boolean halfReward = hpLostPercent >= 50;

        int baseCoins = currentBoss.getBossNumber() == 1
                ? 200
                : (int) (200 * Math.pow(1.2, currentBoss.getBossNumber() - 1));

        if (halfReward) {
            coinsReward = baseCoins / 2;
            Log.d(TAG, "🪙 Polovična nagrada jer je skinuto više od 50% HP-a bossa!");
        } else {
            coinsReward = 0;
        }

        boolean itemDropped = halfReward && (new Random().nextInt(100) < 10);

        if (coinsReward > 0) {
            updatePlayerCoins(coinsReward);
        }

        battleEnded = true;
        listener.onBattleResult(false, coinsReward, itemDropped);
    }

    private void updatePlayerCoins(int coinsEarned) {
        int newTotal = player.getCoins() + coinsEarned;
        player.setCoins(newTotal);

        db.collection("users").document(player.getUserId())
                .update("coins", newTotal)
                .addOnSuccessListener(unused -> Log.d(TAG, "💰 Coins updated: " + newTotal))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Greška pri ažuriranju coins-a: " + e.getMessage()));
    }
}
