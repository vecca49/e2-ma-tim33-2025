package com.example.bossapp.presentation.equipment;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.business.EquipmentManager;
import com.example.bossapp.business.UserManager;
import com.example.bossapp.data.model.Equipment;
import com.example.bossapp.data.model.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ShopActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private TextView tvCoins;
    private ProgressBar progressBar;

    private EquipmentAdapter adapter;
    private EquipmentManager equipmentManager;
    private UserManager userManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        initViews();
        setupToolbar();
        setupRecyclerView();

        equipmentManager = new EquipmentManager();
        userManager = new UserManager();

        loadUserData();
        setupTabs();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.rvShopItems);
        tvCoins = findViewById(R.id.tvShopCoins);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Shop");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new EquipmentAdapter(new EquipmentAdapter.OnEquipmentActionListener() {
            @Override
            public void onBuyClick(Equipment equipment, int price) {
                purchaseEquipment(equipment, price);
            }

            @Override
            public void onActivateClick(Equipment equipment) {
                // Not used in shop
            }

            @Override
            public void onDeactivateClick(Equipment equipment) {
                // Not used in shop
            }

            @Override
            public void onUpgradeClick(Equipment equipment, int upgradeCost) {
                // Not used in shop
            }
        }, true, currentUser != null ? currentUser.getLevel() : 0);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadUserData() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);

        userManager.getUserById(userId, new UserManager.OnUserLoadListener() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                tvCoins.setText("Coins: " + user.getCoins());
                progressBar.setVisibility(View.GONE);

                // Reload adapter sa novim user level-om
                adapter = new EquipmentAdapter(new EquipmentAdapter.OnEquipmentActionListener() {
                    @Override
                    public void onBuyClick(Equipment equipment, int price) {
                        purchaseEquipment(equipment, price);
                    }

                    @Override
                    public void onActivateClick(Equipment equipment) {}

                    @Override
                    public void onDeactivateClick(Equipment equipment) {}

                    @Override
                    public void onUpgradeClick(Equipment equipment, int upgradeCost) {}
                }, true, user.getLevel());

                recyclerView.setAdapter(adapter);
                loadShopItems(0); // Load potions by default
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ShopActivity.this,
                        "Error loading user: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Potions"));
        tabLayout.addTab(tabLayout.newTab().setText("Armor"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadShopItems(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadShopItems(int tabPosition) {
        List<Equipment> shopItems = new ArrayList<>();

        if (tabPosition == 0) {
            // Potions
            for (Equipment.PotionType potion : Equipment.PotionType.values()) {
                Equipment equipment = new Equipment(currentUser.getUserId(), potion);
                shopItems.add(equipment);
            }
        } else if (tabPosition == 1) {
            // Armor
            for (Equipment.ArmorType armor : Equipment.ArmorType.values()) {
                Equipment equipment = new Equipment(currentUser.getUserId(), armor);
                shopItems.add(equipment);
            }
        }

        adapter.setEquipmentList(shopItems);
    }

    private void purchaseEquipment(Equipment equipment, int price) {
        if (currentUser == null) return;

        progressBar.setVisibility(View.VISIBLE);

        equipmentManager.purchaseEquipment(currentUser, equipment.getType(),
                equipment.getSubType(), new EquipmentManager.OnPurchaseListener() {
                    @Override
                    public void onSuccess(String message) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ShopActivity.this, message, Toast.LENGTH_SHORT).show();

                        // Reload user data
                        loadUserData();
                    }

                    @Override
                    public void onError(String message) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ShopActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}