package com.example.bossapp.presentation.boss;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bossapp.R;
import com.example.bossapp.data.model.User;

import java.util.Random;

public class BossFightActivity extends AppCompatActivity {

    private TextView tvBossName, tvBossHP, tvBattleResult;
    private ProgressBar progressBossHP;
    private Button btnAttack;
    private ImageView imgBoss;

    private int bossHP = 200;
    private int bossMaxHP = 200;
    private int userPP = 40;
    private int attackCount = 0;
    private final int maxAttacks = 5;
    private double successRate = 0.7;
    private Random random = new Random();

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boss_fight);

        tvBossName = findViewById(R.id.tvBossName);
        tvBossHP = findViewById(R.id.tvBossHP);
        tvBattleResult = findViewById(R.id.tvBattleResult);
        progressBossHP = findViewById(R.id.progressBossHP);
        btnAttack = findViewById(R.id.btnAttack);
        imgBoss = findViewById(R.id.imgBoss);

        tvBossName.setText("Boss: Shadow Titan");
        updateBossHP();

        btnAttack.setOnClickListener(v -> handleAttack());
    }

    private void handleAttack() {
        if (attackCount >= maxAttacks) {
            Toast.makeText(this, "Iskoristio si sve napade!", Toast.LENGTH_SHORT).show();
            return;
        }

        attackCount++;

        boolean hit = random.nextDouble() < successRate;

        if (hit) {
            bossHP -= userPP;
            if (bossHP < 0) bossHP = 0;
            tvBattleResult.setText("Napad #" + attackCount + ": Pogodak! (" + userPP + " štete)");
        } else {
            tvBattleResult.setText("Napad #" + attackCount + ": Promašaj!");
        }

        updateBossHP();

        if (bossHP <= 0) {
            onBossDefeated();
        } else if (attackCount == maxAttacks) {
            onBattleEnd();
        }
    }

    private void updateBossHP() {
        progressBossHP.setProgress(bossHP);
        tvBossHP.setText("HP: " + bossHP + " / " + bossMaxHP);
    }

    private void onBossDefeated() {
        tvBattleResult.setText("🎉 Pobeda! Boss je poražen!");
        btnAttack.setEnabled(false);
        rewardUser(true);
    }

    private void onBattleEnd() {
        if (bossHP > 0) {
            tvBattleResult.setText("❌ Boss je preživeo! HP preostalo: " + bossHP);
            rewardUser(false);
        }
        btnAttack.setEnabled(false);
    }

    private void rewardUser(boolean victory) {
        int coinsEarned = victory ? 200 : 100;
        Toast.makeText(this,
                "Dobio si " + coinsEarned + " novčića!",
                Toast.LENGTH_LONG).show();
    }
}
