package com.example.bossapp.business;

import android.util.Log;
import com.example.bossapp.data.model.Task;
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.repository.UserRepository;

public class LevelManager {
    private static final String TAG = "LevelManager";
    private final UserRepository userRepository;

    public LevelManager() {
        this.userRepository = new UserRepository();
    }

    public interface OnLevelUpListener {
        void onLevelUp(int newLevel, String newTitle, int ppGained, int coinsGained);
        void onNoLevelUp();
        void onError(String message);
    }

    /**
     * Izračunava koliko XP je potrebno za prelazak SA trenutnog nivoa NA sledeći nivo
     *
     * Za nivo 0 -> 1: 200 XP
     * Za nivo 1 -> 2: 500 XP (200 * 2.5 = 500)
     * Za nivo 2 -> 3: 1250 XP (500 * 2.5 = 1250)
     *
     * Formula: previousRequired * 2.5, zaokruženo na prvu narednu stotinu
     */
    public static int calculateXPForLevel(int targetLevel) {
        if (targetLevel == 0) return 0; // Za level 0 ne treba XP
        if (targetLevel == 1) return 200; // Za prelazak na level 1 treba 200 XP

        // Za sve ostale nivoe
        int previousXP = 200; // XP potreban za level 1

        for (int i = 2; i <= targetLevel; i++) {
            // Formula: XP * 2 + XP / 2 = XP * 2.5
            double calculated = previousXP * 2.5;
            previousXP = (int) calculated;
            // Zaokruži na prvu narednu stotinu
            //previousXP = (int) (Math.ceil(calculated / 100.0) * 100);
        }

        return previousXP;
    }

    /**
     * Izračunava koliko PP korisnik dobija kada dostigne određeni nivo
     *
     * Za nivo 1: 40 PP
     * Za nivo 2: 70 PP (40 + 3/4 * 40 = 40 + 30 = 70)
     * Za nivo 3: 123 PP (70 + 3/4 * 70 = 70 + 52.5 ≈ 123)
     *
     * Formula: previousPP + (3/4 * previousPP) = previousPP * 1.75
     */
    public static int calculatePPForLevel(int level) {
        if (level == 0) return 0; // Level 0 nema PP
        if (level == 1) return 40; // Početni PP za level 1

        int previousPP = 40;

        for (int i = 2; i <= level; i++) {
            // Formula: PP + (3/4 * PP) = PP * 1.75
            double calculated = previousPP + (3.0 / 4.0 * previousPP);
            previousPP = (int) Math.round(calculated);
        }

        return previousPP;
    }

    /**
     * Izračunava XP za težinu zadatka na određenom nivou
     * Formula: XP težine za prethodni nivo + XP težine za prethodni nivo / 2
     */
    public static int calculateDifficultyXP(Task.Difficulty difficulty, int level) {
        int baseXP;
        switch (difficulty) {
            case VERY_EASY: baseXP = 1; break;
            case EASY: baseXP = 3; break;
            case HARD: baseXP = 7; break;
            case EXTREME: baseXP = 20; break;
            default: baseXP = 1;
        }

        if (level == 0) return baseXP;

        int currentXP = baseXP;
        for (int i = 1; i <= level; i++) {
            currentXP = (int) Math.round(currentXP * 1.5);
        }
        return currentXP;
    }

    /**
     * Izračunava XP za bitnost zadatka na određenom nivou
     * Formula: XP bitnosti za prethodni nivo + XP bitnosti za prethodni nivo / 2
     */
    public static int calculateImportanceXP(Task.Importance importance, int level) {
        int baseXP;
        switch (importance) {
            case NORMAL: baseXP = 1; break;
            case IMPORTANT: baseXP = 3; break;
            case VERY_IMPORTANT: baseXP = 10; break;
            case SPECIAL: baseXP = 100; break;
            default: baseXP = 1;
        }

        if (level == 0) return baseXP;

        int currentXP = baseXP;
        for (int i = 1; i <= level; i++) {
            currentXP = (int) Math.round(currentXP * 1.5);
        }
        return currentXP;
    }

    /**
     * Proverava da li korisnik treba da pređe na viši nivo
     * Ako jeste, automatski ga level-up-uje
     *
     * VAŽNO: Ova metoda može da level-up-uje korisnika VIŠE PUTA ako ima dovoljno XP!
     */
    public void checkAndProcessLevelUp(User user, OnLevelUpListener listener) {
        Log.d(TAG, "=== CHECKING LEVEL UP ===");
        Log.d(TAG, "Current Level: " + user.getLevel());
        Log.d(TAG, "Current XP: " + user.getXp());
        Log.d(TAG, "Current PP: " + user.getPp());

        // Proveravaj dok korisnik ima dovoljno XP za sledeći nivo
        int totalLevelsGained = 0;
        int totalPPGained = 0;

        while (true) {
            int currentLevel = user.getLevel();
            int requiredXP = calculateXPForLevel(currentLevel + 1);

            Log.d(TAG, "Required XP for level " + (currentLevel + 1) + ": " + requiredXP);

            if (user.getXp() >= requiredXP) {
                // Level up!
                int newLevel = currentLevel + 1;
                int ppGained = calculatePPForLevel(newLevel);

                Log.d(TAG, "🎉 LEVEL UP! " + currentLevel + " -> " + newLevel);
                Log.d(TAG, "PP gained: " + ppGained);

                // Ažuriraj korisnika
                user.setLevel(newLevel);
                user.setXp(user.getXp() - requiredXP); // Oduzmi potrošeni XP
                user.setPp(user.getPp() + ppGained); // Dodaj PP
                user.setTitle(User.getTitleForLevel(newLevel));

                totalLevelsGained++;
                totalPPGained += ppGained;

                Log.d(TAG, "New Level: " + newLevel);
                Log.d(TAG, "Remaining XP: " + user.getXp());
                Log.d(TAG, "New PP: " + user.getPp());
                Log.d(TAG, "New Title: " + user.getTitle());
            } else {
                // Nema dovoljno XP za sledeći nivo
                break;
            }
        }

        // *** ISPRAVKA: Kreiraj final kopije za inner class ***
        final int finalLevelsGained = totalLevelsGained;
        final int finalPPGained = totalPPGained;
        final int finalNewLevel = user.getLevel();
        final String finalNewTitle = user.getTitle();

        if (finalLevelsGained > 0) {
            // Sačuvaj korisnika u bazu
            Log.d(TAG, "Saving user with " + finalLevelsGained + " level(s) gained...");

            userRepository.saveUser(user, new UserRepository.OnUserSaveListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "✅ User saved successfully after level up!");
                    listener.onLevelUp(finalNewLevel, finalNewTitle, finalPPGained, 0);
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "❌ Error saving user after level up: " + e.getMessage());
                    listener.onError("Greška pri čuvanju level up-a: " + e.getMessage());
                }
            });
        } else {
            Log.d(TAG, "No level up - not enough XP");
            listener.onNoLevelUp();
        }
    }
    /**
     * Vraća procenat napretka ka sledećem nivou
     */
    public static int getProgressPercentage(int currentXP, int currentLevel) {
        int requiredXP = calculateXPForLevel(currentLevel + 1);
        if (requiredXP == 0) return 0;
        return Math.min(100, (int) ((currentXP / (float) requiredXP) * 100));
    }

    /**
     * Vraća preostali XP do sledećeg nivoa
     */
    public static int getRemainingXP(int currentXP, int currentLevel) {
        int requiredXP = calculateXPForLevel(currentLevel + 1);
        return Math.max(0, requiredXP - currentXP);
    }
}