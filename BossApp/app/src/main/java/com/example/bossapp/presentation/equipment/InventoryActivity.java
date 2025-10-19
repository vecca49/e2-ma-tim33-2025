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
import com.example.bossapp.data.repository.EquipmentRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private ProgressBar progressBar;

    private EquipmentAdapter adapter;
    private EquipmentManager equipmentManager;
    private UserManager userManager;
    private EquipmentRepository equipmentRepository;
    private User currentUser;
    private List<Equipment> allEquipment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        initViews();
        setupToolbar();
        setupRecyclerView();

        equipmentManager = new EquipmentManager();
        userManager = new UserManager();
        equipmentRepository = new EquipmentRepository();
        allEquipment = new ArrayList<>();

        loadUserData();
        setupTabs();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.rvInventoryItems);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Inventory");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new EquipmentAdapter(new EquipmentAdapter.OnEquipmentActionListener() {
            @Override
            public void onBuyClick(Equipment equipment, int price) {
                // Not used in inventory
            }

            @Override
            public void onActivateClick(Equipment equipment) {
                activateEquipment(equipment);
            }

            @Override
            public void onDeactivateClick(Equipment equipment) {
                deactivateEquipment(equipment);
            }

            @Override
            public void onUpgradeClick(Equipment equipment, int upgradeCost) {
                upgradeWeapon(equipment, upgradeCost);
            }
        }, false, currentUser != null ? currentUser.getLevel() : 0);

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

                // Update adapter sa novim level-om
                adapter = new EquipmentAdapter(new EquipmentAdapter.OnEquipmentActionListener() {
                    @Override
                    public void onBuyClick(Equipment equipment, int price) {}

                    @Override
                    public void onActivateClick(Equipment equipment) {
                        activateEquipment(equipment);
                    }

                    @Override
                    public void onDeactivateClick(Equipment equipment) {
                        deactivateEquipment(equipment);
                    }

                    @Override
                    public void onUpgradeClick(Equipment equipment, int upgradeCost) {
                        upgradeWeapon(equipment, upgradeCost);
                    }
                }, false, user.getLevel());

                recyclerView.setAdapter(adapter);
                loadInventory();
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(InventoryActivity.this,
                        "Error loading user: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Potions"));
        tabLayout.addTab(tabLayout.newTab().setText("Armor"));
        tabLayout.addTab(tabLayout.newTab().setText("Weapons"));
        tabLayout.addTab(tabLayout.newTab().setText("Active"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterInventory(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadInventory() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);

        equipmentRepository.getUserEquipment(userId,
                new EquipmentRepository.OnEquipmentListListener() {
                    @Override
                    public void onSuccess(List<Equipment> equipmentList) {
                        progressBar.setVisibility(View.GONE);
                        allEquipment = equipmentList;
                        filterInventory(0); // Show all by default
                    }

                    @Override
                    public void onError(String message) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(InventoryActivity.this,
                                "Error loading inventory: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterInventory(int tabPosition) {
        List<Equipment> filteredList = new ArrayList<>();

        for (Equipment equipment : allEquipment) {
            switch (tabPosition) {
                case 0: // All
                    filteredList.add(equipment);
                    break;
                case 1: // Potions
                    if (equipment.getType() == Equipment.EquipmentType.POTION) {
                        filteredList.add(equipment);
                    }
                    break;
                case 2: // Armor
                    if (equipment.getType() == Equipment.EquipmentType.ARMOR) {
                        filteredList.add(equipment);
                    }
                    break;
                case 3: // Weapons
                    if (equipment.getType() == Equipment.EquipmentType.WEAPON) {
                        filteredList.add(equipment);
                    }
                    break;
                case 4: // Active
                    if (equipment.getIsActive()) {
                        filteredList.add(equipment);
                    }
                    break;
            }
        }

        if (filteredList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.setEquipmentList(filteredList);
        }
    }

    private void activateEquipment(Equipment equipment) {
        progressBar.setVisibility(View.VISIBLE);

        equipmentManager.activateEquipment(equipment,
                new EquipmentManager.OnPurchaseListener() {
                    @Override
                    public void onSuccess(String message) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(InventoryActivity.this, message, Toast.LENGTH_SHORT).show();
                        loadInventory(); // Reload
                    }

                    @Override
                    public void onError(String message) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(InventoryActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deactivateEquipment(Equipment equipment) {
        progressBar.setVisibility(View.VISIBLE);

        equipmentManager.deactivateEquipment(equipment,
                new EquipmentManager.OnPurchaseListener() {
                    @Override
                    public void onSuccess(String message) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(InventoryActivity.this, message, Toast.LENGTH_SHORT).show();
                        loadInventory(); // Reload
                    }

                    @Override
                    public void onError(String message) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(InventoryActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void upgradeWeapon(Equipment equipment, int upgradeCost) {
        if (currentUser == null) return;

        progressBar.setVisibility(View.VISIBLE);

        equipmentManager.upgradeWeapon(currentUser, equipment,
                new EquipmentManager.OnPurchaseListener() {
                    @Override
                    public void onSuccess(String message) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(InventoryActivity.this, message, Toast.LENGTH_SHORT).show();
                        loadUserData(); // Reload user i inventory
                    }

                    @Override
                    public void onError(String message) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(InventoryActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}