package com.example.bossapp.business;

import android.util.Log;
import androidx.annotation.NonNull;
import com.example.bossapp.data.model.Boss;
import com.example.bossapp.data.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BossManager {

    private static final String TAG = "BossManager";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnBossLoadListener {
        void onSuccess(Boss boss);
        void onError(String message);
    }

    public interface OnBossSaveListener {
        void onSuccess();
        void onError(String message);
    }

    public void loadAllBosses(User user, OnBossesLoaded listener) {
        db.collection("users")
                .document(user.getUserId())
                .collection("bosses")
                .get()
                .addOnSuccessListener(query -> {
                    List<Boss> bosses = new ArrayList<>();
                    for (var doc : query) {
                        bosses.add(doc.toObject(Boss.class));
                    }
                    listener.onSuccess(bosses);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public interface OnBossesLoaded {
        void onSuccess(List<Boss> bosses);
        void onError(String message);
    }

    public void loadCurrentBoss(User user, OnBossLoadListener listener) {
        db.collection("users")
                .document(user.getUserId())
                .collection("bosses")
                .document(String.valueOf(user.getLevel()))
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boss boss = doc.toObject(Boss.class);
                        listener.onSuccess(boss);
                    } else {
                        Boss newBoss = new Boss(user.getLevel(), Boss.calculateBossHp(user.getLevel()), false);
                        saveBoss(user, newBoss, new OnBossSaveListener() {
                            @Override
                            public void onSuccess() {
                                listener.onSuccess(newBoss);
                            }

                            @Override
                            public void onError(String message) {
                                listener.onError(message);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }


    public void saveBoss(User user, Boss boss, OnBossSaveListener listener) {
        db.collection("users")
                .document(user.getUserId())
                .collection("bosses")
                .document(String.valueOf(boss.getBossNumber()))
                .set(boss)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Greška pri čuvanju bossa: ", e);
                    listener.onError(e.getMessage());
                });
    }

    public boolean shouldShowBoss(User user, Boss boss) {
        if (boss.isDefeated()) return false;
        return user.getLevel() >= boss.getBossNumber();
    }


    public void markBossAsDefeated(User user, Boss boss, OnBossSaveListener listener) {
        boss.setDefeated(true);
        saveBoss(user, boss, listener);
    }
}
