package com.example.bossapp.presentation.level;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bossapp.R;
import com.example.bossapp.business.LevelManager;
import com.example.bossapp.business.UserManager;
import com.example.bossapp.data.model.Task;
import com.example.bossapp.data.model.User;
import com.example.bossapp.presentation.base.BaseFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

public class LevelProgressFragment extends BaseFragment {

    private TextView tvCurrentLevel, tvCurrentTitle, tvCurrentPP;
    private TextView tvCurrentXP, tvRequiredXP, tvRemainingXP;
    private TextView tvProgressPercent;
    private ProgressBar progressXP;

    // XP za težinu
    private TextView tvVeryEasyXP, tvEasyXP, tvHardXP, tvExtremeXP;

    // XP za bitnost
    private TextView tvNormalXP, tvImportantXP, tvVeryImportantXP, tvSpecialXP;

    // Sledeći nivo info
    private TextView tvNextLevel, tvNextTitle, tvNextPP, tvNextRequiredXP;

    private UserManager userManager;
    private LevelManager levelManager;
    private String currentUserId;
    private ListenerRegistration userListener;
    private Button btnTestFormulas;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_level_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar(view, R.id.toolbar);
        initViews(view);

        userManager = new UserManager();
        levelManager = new LevelManager();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        userManager = new UserManager();
        levelManager = new LevelManager();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // *** DODAJ LISTENER ZA TEST DUGME ***
        btnTestFormulas.setOnClickListener(v -> testFormulas());

        loadUserData();
    }

    private void testFormulas() {
        Log.d("TEST", "=== TESTING XP FORMULA ===");
        Log.d("TEST", "Expected values:");
        Log.d("TEST", "Level 0 -> 1: 200 XP, 40 PP");
        Log.d("TEST", "Level 1 -> 2: 500 XP, 70 PP");
        Log.d("TEST", "Level 2 -> 3: 1250 XP, 123 PP");
        Log.d("TEST", "");
        Log.d("TEST", "Calculated values:");

        for (int i = 0; i <= 5; i++) {
            int xp = LevelManager.calculateXPForLevel(i);
            int pp = LevelManager.calculatePPForLevel(i);
            Log.d("TEST", "Level " + i + " - Required XP: " + xp + ", PP Reward: " + pp);
        }

        Log.d("TEST", "");
        Log.d("TEST", "=== TESTING DIFFICULTY XP ===");
        for (int level = 0; level <= 3; level++) {
            Log.d("TEST", "--- Level " + level + " ---");
            Log.d("TEST", "Very Easy: " + LevelManager.calculateDifficultyXP(Task.Difficulty.VERY_EASY, level) + " XP");
            Log.d("TEST", "Easy: " + LevelManager.calculateDifficultyXP(Task.Difficulty.EASY, level) + " XP");
            Log.d("TEST", "Hard: " + LevelManager.calculateDifficultyXP(Task.Difficulty.HARD, level) + " XP");
            Log.d("TEST", "Extreme: " + LevelManager.calculateDifficultyXP(Task.Difficulty.EXTREME, level) + " XP");
        }

        Log.d("TEST", "");
        Log.d("TEST", "=== TESTING IMPORTANCE XP ===");
        for (int level = 0; level <= 3; level++) {
            Log.d("TEST", "--- Level " + level + " ---");
            Log.d("TEST", "Normal: " + LevelManager.calculateImportanceXP(Task.Importance.NORMAL, level) + " XP");
            Log.d("TEST", "Important: " + LevelManager.calculateImportanceXP(Task.Importance.IMPORTANT, level) + " XP");
            Log.d("TEST", "Very Important: " + LevelManager.calculateImportanceXP(Task.Importance.VERY_IMPORTANT, level) + " XP");
            Log.d("TEST", "Special: " + LevelManager.calculateImportanceXP(Task.Importance.SPECIAL, level) + " XP");
        }

        Toast.makeText(requireContext(), "Check Logcat for test results!", Toast.LENGTH_LONG).show();
    }

    private void initViews(View view) {
        // Trenutni nivo info
        tvCurrentLevel = view.findViewById(R.id.tvCurrentLevel);
        tvCurrentTitle = view.findViewById(R.id.tvCurrentTitle);
        tvCurrentPP = view.findViewById(R.id.tvCurrentPP);
        tvCurrentXP = view.findViewById(R.id.tvCurrentXP);
        tvRequiredXP = view.findViewById(R.id.tvRequiredXP);
        tvRemainingXP = view.findViewById(R.id.tvRemainingXP);
        tvProgressPercent = view.findViewById(R.id.tvProgressPercent);
        progressXP = view.findViewById(R.id.progressXP);

        // XP za težinu
        tvVeryEasyXP = view.findViewById(R.id.tvVeryEasyXP);
        tvEasyXP = view.findViewById(R.id.tvEasyXP);
        tvHardXP = view.findViewById(R.id.tvHardXP);
        tvExtremeXP = view.findViewById(R.id.tvExtremeXP);

        // XP za bitnost
        tvNormalXP = view.findViewById(R.id.tvNormalXP);
        tvImportantXP = view.findViewById(R.id.tvImportantXP);
        tvVeryImportantXP = view.findViewById(R.id.tvVeryImportantXP);
        tvSpecialXP = view.findViewById(R.id.tvSpecialXP);

        // Sledeći nivo
        tvNextLevel = view.findViewById(R.id.tvNextLevel);
        tvNextTitle = view.findViewById(R.id.tvNextTitle);
        tvNextPP = view.findViewById(R.id.tvNextPP);
        tvNextRequiredXP = view.findViewById(R.id.tvNextRequiredXP);

        btnTestFormulas = view.findViewById(R.id.btnTestFormulas);
    }

    private void loadUserData() {
        userListener = userManager.observeUserChanges(currentUserId, new UserManager.OnUserLoadListener() {
            @Override
            public void onSuccess(User user) {
                displayUserProgress(user);
            }

            @Override
            public void onError(String message) {
                // Handle error
            }
        });
    }

    private void displayUserProgress(User user) {
        int currentLevel = user.getLevel();
        int currentXP = user.getXp();
        int requiredXP = LevelManager.calculateXPForLevel(currentLevel + 1);
        int remainingXP = LevelManager.getRemainingXP(currentXP, currentLevel);
        int progressPercent = LevelManager.getProgressPercentage(currentXP, currentLevel);

        // Trenutni nivo
        tvCurrentLevel.setText("Level " + currentLevel);
        tvCurrentTitle.setText(user.getTitle());
        tvCurrentPP.setText("PP: " + user.getPp());
        tvCurrentXP.setText(String.valueOf(currentXP));
        tvRequiredXP.setText(String.valueOf(requiredXP));
        tvRemainingXP.setText(String.valueOf(remainingXP));
        tvProgressPercent.setText(progressPercent + "%");

        progressXP.setMax(requiredXP);
        progressXP.setProgress(currentXP);

        // XP vrednosti za težinu na trenutnom nivou
        tvVeryEasyXP.setText(LevelManager.calculateDifficultyXP(Task.Difficulty.VERY_EASY, currentLevel) + " XP");
        tvEasyXP.setText(LevelManager.calculateDifficultyXP(Task.Difficulty.EASY, currentLevel) + " XP");
        tvHardXP.setText(LevelManager.calculateDifficultyXP(Task.Difficulty.HARD, currentLevel) + " XP");
        tvExtremeXP.setText(LevelManager.calculateDifficultyXP(Task.Difficulty.EXTREME, currentLevel) + " XP");

        // XP vrednosti za bitnost na trenutnom nivou
        tvNormalXP.setText(LevelManager.calculateImportanceXP(Task.Importance.NORMAL, currentLevel) + " XP");
        tvImportantXP.setText(LevelManager.calculateImportanceXP(Task.Importance.IMPORTANT, currentLevel) + " XP");
        tvVeryImportantXP.setText(LevelManager.calculateImportanceXP(Task.Importance.VERY_IMPORTANT, currentLevel) + " XP");
        tvSpecialXP.setText(LevelManager.calculateImportanceXP(Task.Importance.SPECIAL, currentLevel) + " XP");

        // Sledeći nivo
        int nextLevel = currentLevel + 1;
        tvNextLevel.setText("Level " + nextLevel);
        tvNextTitle.setText(User.getTitleForLevel(nextLevel));
        tvNextPP.setText("+" + LevelManager.calculatePPForLevel(nextLevel) + " PP");
        tvNextRequiredXP.setText(LevelManager.calculateXPForLevel(nextLevel) + " XP");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            userListener.remove();
        }
    }
}