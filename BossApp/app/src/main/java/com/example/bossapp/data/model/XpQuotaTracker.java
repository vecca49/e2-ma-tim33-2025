package com.example.bossapp.data.model;

import com.example.bossapp.data.model.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class XpQuotaTracker {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnQuotaLoadListener {
        void onSuccess(QuotaData quota);
        void onError(String message);
    }

    public static class QuotaData {
        private final Map<String, Integer> difficultyCount = new HashMap<>();
        private final Map<String, Integer> importanceCount = new HashMap<>();

        public boolean canAdd(Task.Difficulty difficulty) {
            switch (difficulty) {
                case VERY_EASY: case EASY:
                    return getCount(difficulty.name()) < 5;
                case HARD:
                    return getCount(difficulty.name()) < 2;
                case EXTREME:
                    return getCount(difficulty.name()) < 1;
            }
            return true;
        }

        public boolean canAdd(Task.Importance importance) {
            switch (importance) {
                case NORMAL: case IMPORTANT:
                    return getCount(importance.name()) < 5;
                case VERY_IMPORTANT:
                    return getCount(importance.name()) < 2;
                case SPECIAL:
                    return getCount(importance.name()) < 1;
            }
            return true;
        }

        public int getCount(String key) {
            return difficultyCount.getOrDefault(key, 0);
        }

        public void increment(Task.Difficulty difficulty, Task.Importance importance) {
            difficultyCount.put(difficulty.name(), getCount(difficulty.name()) + 1);
            importanceCount.put(importance.name(), getCount(importance.name()) + 1);
        }
    }

    public void getDailyXpStats(String userId, OnQuotaLoadListener listener) {
        db.collection("xpLog")
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    QuotaData quota = new QuotaData();
                    if (snapshot.exists() && snapshot.getData() != null) {
                        Map<String, Object> data = snapshot.getData();
                        for (String key : data.keySet()) {
                            quota.difficultyCount.put(key, ((Long) data.get(key)).intValue());
                        }
                    }
                    listener.onSuccess(quota);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void incrementQuota(String userId, Task.Difficulty difficulty, Task.Importance importance) {
        db.collection("xpLog").document(userId)
                .update(difficulty.name(), com.google.firebase.firestore.FieldValue.increment(1),
                        importance.name(), com.google.firebase.firestore.FieldValue.increment(1));
    }
}

