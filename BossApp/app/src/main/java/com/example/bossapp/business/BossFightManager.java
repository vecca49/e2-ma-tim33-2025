package com.example.bossapp.business;

import com.example.bossapp.data.model.BossFight;
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.repository.UserRepository;

public class BossFightManager {
    private final UserRepository userRepository;

    public interface OnFightResultListener {
        void onSuccess(String message, int rewardCoins, boolean bossDefeated);
        void onError(String message);
    }

    public BossFightManager() {
        this.userRepository = new UserRepository();
    }

    public BossFight createBossFight(User user, int successRate) {
        int userPp = user.getPp() > 0 ? user.getPp() : 40; // fiksno ako nema nivoa
        return new BossFight("Boss 1", 200, userPp, successRate);
    }

    public void finishFight(User user, BossFight fight, OnFightResultListener listener) {
        int reward = fight.calculateReward();
        user.setCoins(user.getCoins() + reward);

        userRepository.saveUser(user, new UserRepository.OnUserSaveListener() {
            @Override
            public void onSuccess() {
                listener.onSuccess("Borba završena!", reward, fight.isBossDefeated());
            }

            @Override
            public void onError(Exception e) {
                listener.onError("Greška pri čuvanju korisnika: " + e.getMessage());
            }
        });
    }
}
