package com.example.bossapp.presentation.boss;

import android.animation.ObjectAnimator;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bossapp.R;
import com.example.bossapp.business.AuthManager;
import com.example.bossapp.business.BossBattleManager;
import com.example.bossapp.business.BossManager;
import com.example.bossapp.business.EquipmentManager;
import com.example.bossapp.business.TaskManager;
import com.example.bossapp.business.UserManager;
import com.example.bossapp.data.model.Boss;
import com.example.bossapp.data.model.Equipment;
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.repository.EquipmentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BossBattleActivity extends AppCompatActivity {

    private static final String TAG = "BossBattleActivity";
    private ProgressBar bossHpBar, playerPpBar;
    private TextView bossHpText, playerPpText, attackCountText, chanceText, rewardText;
    private ImageView bossImage, chestImage, coinIcon, itemIcon;
    private Button attackButton;
    private TextView tvActiveEquipment;
    private LinearLayout resultLayout;

    private BossBattleManager battleManager;
    private boolean battleEnded = false;
    private boolean chestOpened = false;

    private User player;
    private AuthManager authManager;
    private UserManager userManager;

    private final Handler handler = new Handler();

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastX, lastY, lastZ;
    private long lastShakeTime = 0;
    private static final int SHAKE_THRESHOLD = 400;

    private List<Equipment> activeEquipment = new ArrayList<>();
    private int originalPP = 0;
    private int bonusPP = 0;
    private int bonusSuccessRate = 0;
    private int extraAttacks = 0;
    private boolean equipmentLoaded = false;
    private EquipmentManager equipmentManager;
    private EquipmentRepository equipmentRepository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boss_battle);

        initUI();
        authManager = new AuthManager(this);
        userManager = new UserManager();
        equipmentManager = new EquipmentManager();
        equipmentRepository = new EquipmentRepository();


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        loadPlayerData();
    }

    private void initUI() {
        bossHpBar = findViewById(R.id.bossHpBar);
        playerPpBar = findViewById(R.id.playerPpBar);
        bossHpText = findViewById(R.id.bossHpText);
        playerPpText = findViewById(R.id.playerPpText);
        attackCountText = findViewById(R.id.attackCountText);
        chanceText = findViewById(R.id.chanceText);
        tvActiveEquipment = findViewById(R.id.tvActiveEquipment);
        bossImage = findViewById(R.id.bossImage);
        attackButton = findViewById(R.id.attackButton);

        resultLayout = findViewById(R.id.resultLayout);
        chestImage = findViewById(R.id.chestImage);
        rewardText = findViewById(R.id.rewardText);
        coinIcon = findViewById(R.id.coinIcon);
        itemIcon = findViewById(R.id.itemIcon);

        bossImage.setImageResource(R.mipmap.boss_idle_foreground);
    }

    private void loadPlayerData() {
        if (authManager.getCurrentUser() == null) {
            Toast.makeText(this, "Niste prijavljeni!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = authManager.getCurrentUser().getUid();

        userManager.getUserById(userId, new UserManager.OnUserLoadListener() {
            @Override
            public void onSuccess(User user) {
                player = user;
                if (player == null) {
                    Toast.makeText(BossBattleActivity.this, "Greška: korisnik nije pronađen!", Toast.LENGTH_SHORT).show();
                    return;
                }

                originalPP = player.getPp();

                Toast.makeText(BossBattleActivity.this,
                        "Dobrodošao, " + player.getUsername(),
                        Toast.LENGTH_SHORT).show();

                loadActiveEquipment();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(BossBattleActivity.this,
                        "Greška pri učitavanju korisnika: " + message,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    // 🟢 NOVA METODA - UČITAJ AKTIVNU OPREMU
    private void loadActiveEquipment() {
        equipmentRepository.getActiveEquipment(player.getUserId(),
                new EquipmentRepository.OnEquipmentListListener() {
                    @Override
                    public void onSuccess(List<Equipment> equipment) {
                        activeEquipment = equipment;
                        applyEquipmentBonuses();
                        equipmentLoaded = true;

                        Log.d(TAG, "Active equipment loaded: " + equipment.size() + " items");
                        initBossBattle();
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Error loading equipment: " + message);
                        equipmentLoaded = true;
                        initBossBattle();
                    }
                });
    }

    // 🟢 NOVA METODA - PRIMENI BONUSE OD OPREME
    private void applyEquipmentBonuses() {
        bonusPP = 0;
        bonusSuccessRate = 0;
        extraAttacks = 0;

        StringBuilder equipmentText = new StringBuilder("Active: ");
        boolean hasEquipment = false;

        for (Equipment equipment : activeEquipment) {
            hasEquipment = true;

            switch (equipment.getType()) {
                case POTION:
                    Equipment.PotionType potion = Equipment.PotionType.valueOf(equipment.getSubType());
                    int ppBonus = (int) (originalPP * potion.powerBoost);
                    bonusPP += ppBonus;
                    equipmentText.append(equipment.getDisplayName()).append(" (+").append(ppBonus).append(" PP), ");
                    Log.d(TAG, "Potion bonus: +" + ppBonus + " PP");
                    break;

                case ARMOR:
                    Equipment.ArmorType armor = Equipment.ArmorType.valueOf(equipment.getSubType());

                    if (armor.bonusType.equals("powerBoost")) {
                        int armorPpBonus = (int) (originalPP * armor.value);
                        bonusPP += armorPpBonus;
                        equipmentText.append("Gloves (+").append(armorPpBonus).append(" PP), ");
                        Log.d(TAG, "Gloves bonus: +" + armorPpBonus + " PP");

                    } else if (armor.bonusType.equals("successRate")) {
                        int successBonus = (int) (armor.value * 100);
                        bonusSuccessRate += successBonus;
                        equipmentText.append("Shield (+").append(successBonus).append("% Success), ");
                        Log.d(TAG, "Shield bonus: +" + successBonus + "% Success Rate");

                    } else if (armor.bonusType.equals("extraAttack")) {
                        Random random = new Random();
                        if (random.nextInt(100) < (armor.value * 100)) {
                            extraAttacks++;
                            equipmentText.append("Boots (+1 Attack), ");
                            Log.d(TAG, "Boots bonus: +1 extra attack");
                        }
                    }
                    break;

                case WEAPON:
                    Equipment.WeaponType weapon = Equipment.WeaponType.valueOf(equipment.getSubType());

                    if (weapon.bonusType.equals("powerBoost")) {
                        int weaponPpBonus = (int) (originalPP * equipment.getCurrentValue());
                        bonusPP += weaponPpBonus;
                        equipmentText.append(equipment.getDisplayName()).append(" (+").append(weaponPpBonus).append(" PP), ");
                        Log.d(TAG, "Weapon bonus: +" + weaponPpBonus + " PP");
                    }
                    break;
            }
        }
        // Ažuriraj PP igrača
        player.setPp(originalPP + bonusPP);

        if (hasEquipment) {
            equipmentText.setLength(equipmentText.length() - 2);
            tvActiveEquipment.setText(equipmentText.toString());
            tvActiveEquipment.setVisibility(View.VISIBLE);
        } else {
            tvActiveEquipment.setVisibility(View.GONE);
        }

        Log.d(TAG, "Total bonuses: PP=" + bonusPP + ", Success Rate=" + bonusSuccessRate + "%, Extra Attacks=" + extraAttacks);
    }

    private void initBossBattle() {
        if (!equipmentLoaded) { // 🟢 DODATO - ČEKAJ NA OPREMU
            Log.d(TAG, "Waiting for equipment to load...");
            return;
        }

        BossManager bossManager = new BossManager();

        bossManager.loadCurrentBoss(player, new BossManager.OnBossLoadListener() {
            @Override
            public void onSuccess(Boss boss) {
                if (!bossManager.shouldShowBoss(player, boss)) {
                    hideBossUI();
                    Toast.makeText(BossBattleActivity.this,
                            "Nema dostupnog bossa za ovaj nivo!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                TaskManager taskManager = new TaskManager();
                taskManager.calculateSuccessRate(player.getUserId(), successRate -> {
                    // 🟢 DODAJ BONUS SUCCESS RATE OD OPREME
                    int finalSuccessRate = Math.min(100, successRate + bonusSuccessRate);

                    battleManager = new BossBattleManager(player, boss, finalSuccessRate); // 🟢 KORISTI finalSuccessRate

                    chanceText.setText("Šansa: " + finalSuccessRate + "%"); // 🟢 PRIKAŽI SA BONUSOM

                    showBossUI();
                    setupUI(bossManager, boss);
                });

            }

            @Override
            public void onError(String message) {
                Toast.makeText(BossBattleActivity.this, "Greška: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*private void initBossBattle() {
        BossManager bossManager = new BossManager();

        bossManager.loadCurrentBoss(player, new BossManager.OnBossLoadListener() {
            @Override
            public void onSuccess(Boss boss) {
                if (!bossManager.shouldShowBoss(player, boss)) {
                    hideBossUI();
                    Toast.makeText(BossBattleActivity.this,
                            "Nema dostupnog bossa za ovaj nivo!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                TaskManager taskManager = new TaskManager();
                taskManager.calculateSuccessRate(player.getUserId(), successRate -> {
                    battleManager = new BossBattleManager(player, boss, successRate);

                    chanceText.setText("Šansa: " + successRate + "%");

                    showBossUI();
                    setupUI(bossManager, boss);
                });

            }

            @Override
            public void onError(String message) {
                Toast.makeText(BossBattleActivity.this, "Greška: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }*/

    private void hideBossUI() {
        bossImage.setVisibility(View.GONE);
        bossHpBar.setVisibility(View.GONE);
        playerPpBar.setVisibility(View.GONE);
        attackButton.setEnabled(false);
    }

    private void showBossUI() {
        bossImage.setVisibility(View.VISIBLE);
        bossHpBar.setVisibility(View.VISIBLE);
        playerPpBar.setVisibility(View.VISIBLE);
        attackButton.setEnabled(true);
    }

    private void setupUI(BossManager bossManager, Boss boss) {
        bossHpBar.setMax(battleManager.getBossHpMax());
        bossHpBar.setProgress(battleManager.getBossHpRemaining());
        playerPpBar.setProgress(player.getPowerPoints());
        updateTexts();

        attackButton.setOnClickListener(v -> {
            if (battleEnded) return;

            boolean hit = battleManager.performAttack((bossDefeated, coins, itemDropped) -> {
                battleEnded = true;
                attackButton.setEnabled(false);

                if (bossDefeated) {
                    bossManager.markBossAsDefeated(player, boss, new BossManager.OnBossSaveListener() {
                        @Override
                        public void onSuccess() {
                            player.setCurrentBossNumber(boss.getBossNumber());
                            Toast.makeText(BossBattleActivity.this, "Boss je poražen!", Toast.LENGTH_LONG).show();
                            processEquipmentAfterBattle();
                        }

                        @Override
                        public void onError(String message) {
                            Log.e("BossBattle", "Greška pri ažuriranju bossa: " + message);
                        }
                    });
                } else {
                    processEquipmentAfterBattle();
                }

                showResult(coins, itemDropped);
            });

            if (hit) playBossHitAnimation();
            else playMissAnimation();

            updateTexts();
        });
    }

    private void processEquipmentAfterBattle() {
        Log.d(TAG, "PROCESSING EQUIPMENT AFTER BATTLE - START");
        Log.d(TAG, "User ID: " + player.getUserId());
        Log.d(TAG, "Active equipment count: " + activeEquipment.size());

        for (Equipment eq : activeEquipment) {
            Log.d(TAG, "Active equipment: " + eq.getDisplayName() +
                    ", duration=" + eq.getRemainingDuration() +
                    ", isActive=" + eq.getIsActive());
        }

        equipmentManager.processFightEnd(player.getUserId(),
                new EquipmentRepository.OnEquipmentListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Equipment processed successfully after battle");

                        // RELOAD EQUIPMENT NAKON OBRADE
                        loadActiveEquipment();
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Error processing equipment: " + message);
                    }
                });
    }

    private void updateTexts() {
        bossHpBar.setProgress(battleManager.getBossHpRemaining());
        bossHpText.setText("Boss HP: " + battleManager.getBossHpRemaining() + "/" + battleManager.getBossHpMax());
        attackCountText.setText("Napadi: " + battleManager.getAttacksLeft() + " / 5");
        playerPpText.setText("PP: " + player.getPowerPoints());

        // 🟢 PRIKAŽI PP SA BONUSOM
        if (bonusPP > 0) {
            playerPpText.setText("PP: " + player.getPowerPoints() + " (+" + bonusPP + " bonus)");
        } else {
            playerPpText.setText("PP: " + player.getPowerPoints());
        }
    }

    private void playMissAnimation() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(bossImage, "alpha", 1, 0.5f, 1);
        anim.setDuration(300);
        anim.start();
    }

    private void playBossHitAnimation() {
        bossImage.setImageResource(R.mipmap.boss_hit);
        handler.postDelayed(() -> bossImage.setImageResource(R.mipmap.boss_idle_foreground), 300);

        ObjectAnimator shake = ObjectAnimator.ofFloat(bossImage, "translationX", 0, 20, -20, 0);
        shake.setDuration(300);
        shake.start();
    }

    private void showResult(int coins, boolean itemDropped) {
        resultLayout.setVisibility(View.VISIBLE);
        chestImage.setImageResource(R.mipmap.ic_chest_closed);

        coinIcon.setVisibility(View.GONE);
        itemIcon.setVisibility(View.GONE);
        chestOpened = false;

        chestImage.setTag(new ChestData(coins, itemDropped));

        chestImage.setOnClickListener(v -> {
            ChestData data = (ChestData) chestImage.getTag();
            openChest(data.coins, data.itemDropped);
        });
    }


    private final SensorEventListener shakeListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            long currentTime = System.currentTimeMillis();

            if ((currentTime - lastShakeTime) > 200) {
                float deltaX = x - lastX;
                float deltaY = y - lastY;
                float deltaZ = z - lastZ;

                double speed = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) * 100;

                Log.d("SHAKE", "speed=" + speed);

                if (speed > SHAKE_THRESHOLD) {
                    lastShakeTime = currentTime;

                    if (!battleEnded) {
                        performShakeAttack();
                    } else if (resultLayout.getVisibility() == View.VISIBLE && !chestOpened) {
                        ChestData data = (ChestData) chestImage.getTag();
                        if (data != null) {
                            openChest(data.coins, data.itemDropped);
                        }
                    }

                }

                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private void openChest(int coins, boolean itemDropped) {
        if (chestOpened) return;

        chestOpened = true;
        chestImage.setImageResource(R.mipmap.ic_chest_open_foreground);

        ObjectAnimator shake = ObjectAnimator.ofFloat(chestImage, "translationX", 0, 25, -25, 0);
        shake.setDuration(400);
        shake.start();

        coinIcon.setVisibility(View.VISIBLE);
        if (itemDropped) itemIcon.setVisibility(View.VISIBLE);

        rewardText.setText("Osvojio si " + coins + " 🪙 novčića!");
    }

    private static class ChestData {
        int coins;
        boolean itemDropped;

        ChestData(int coins, boolean itemDropped) {
            this.coins = coins;
            this.itemDropped = itemDropped;
        }
    }



    private void performShakeAttack() {
        if (battleManager == null || battleEnded) return;

        boolean hit = battleManager.performAttack((bossDefeated, coins, itemDropped) -> {
            attackButton.setEnabled(false);

            if (bossDefeated) {
                BossManager bossManager = new BossManager();
                bossManager.markBossAsDefeated(player, battleManager.getCurrentBoss(), new BossManager.OnBossSaveListener() {
                    @Override
                    public void onSuccess() {
                        player.setCurrentBossNumber(battleManager.getCurrentBoss().getBossNumber());
                        Toast.makeText(BossBattleActivity.this, "🎉 Boss je poražen!", Toast.LENGTH_LONG).show();
                        processEquipmentAfterBattle();
                    }

                    @Override
                    public void onError(String message) {
                        Log.e("BossBattle", "Greška pri ažuriranju bossa: " + message);
                    }
                });
            } else {
                processEquipmentAfterBattle();
            }

            showResult(coins, itemDropped);
            updateTexts();
        });

        if (hit) playBossHitAnimation();
        else playMissAnimation();

        battleEnded = battleManager.getAttacksLeft() <= 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeListener);
        }
    }
}
