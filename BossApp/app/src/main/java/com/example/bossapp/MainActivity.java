package com.example.bossapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import com.example.bossapp.presentation.alliance.AllianceFragment;
import com.example.bossapp.presentation.boss.BossFightActivity;
import com.example.bossapp.presentation.home.HomeFragment;
import com.example.bossapp.presentation.profile.ProfileFragment;
import com.example.bossapp.presentation.statistics.StatisticsFragment;
import com.example.bossapp.presentation.task.TaskFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.example.bossapp.presentation.category.CreateCategoryFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNavigationView;
    private String currentFragmentTag = "HOME";
    private List<String> navigationStack = new ArrayList<>();

    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigation);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "HOME");
            navigationStack.add("HOME");
        } else {
            currentFragmentTag = savedInstanceState.getString("currentFragment", "HOME");
            navigationStack = savedInstanceState.getStringArrayList("navigationStack");
            if (navigationStack == null) {
                navigationStack = new ArrayList<>();
                navigationStack.add("HOME");
            }
        }

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                String tag = "";

                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                    tag = "HOME";
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new ProfileFragment();
                    tag = "PROFILE";
                } else if (itemId == R.id.nav_statistics) {
                    selectedFragment = new StatisticsFragment();
                    tag = "STATISTICS";
                }
                else if (itemId == R.id.nav_categories) {
                    Intent intent = new Intent(MainActivity.this, com.example.bossapp.presentation.boss.BossBattleActivity.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    return true;
                }

                else if (itemId == R.id.nav_tasks) {
                    selectedFragment = new TaskFragment();
                    tag = "TASKS";
                }
                else if (itemId == R.id.nav_alliance) {
                    selectedFragment = new AllianceFragment();
                    tag = "ALLIANCE";
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment, tag);
                    addToNavigationStack(tag);
                    return true;
                }
                return false;
            }
        });

        // Start listening for notifications
        startListeningForNotifications();
    }

    private void startListeningForNotifications() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Log.d(TAG, "Starting notification listener for user: " + currentUserId);

        notificationListener = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("read", false)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to notifications", error);
                        return;
                    }

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        Log.d(TAG, "Received " + querySnapshot.size() + " new notifications");

                        // Samo loguj da su stigle notifikacije
                        // NotificationsFragment će ih prikazati
                    }
                });
    }

    private void loadFragment(Fragment fragment, String tag) {
        currentFragmentTag = tag;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment, tag)
                .commit();
    }

    private void addToNavigationStack(String tag) {
        navigationStack.remove(tag);
        navigationStack.add(tag);
    }

    public void navigateBack() {
        if (navigationStack.size() > 1) {
            navigationStack.remove(navigationStack.size() - 1);
            String previousTag = navigationStack.get(navigationStack.size() - 1);

            if (previousTag.equals("HOME")) {
                bottomNavigationView.setSelectedItemId(R.id.nav_home);
            } else if (previousTag.equals("PROFILE")) {
                bottomNavigationView.setSelectedItemId(R.id.nav_profile);
            } else if (previousTag.equals("STATISTICS")) {
                bottomNavigationView.setSelectedItemId(R.id.nav_statistics);
            } else if (previousTag.equals("TASKS")) {
                bottomNavigationView.setSelectedItemId(R.id.nav_tasks);
            }
            else if (previousTag.equals("ALLIANCE")) {
                bottomNavigationView.setSelectedItemId(R.id.nav_alliance);
            }

        } else {
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentFragment", currentFragmentTag);
        outState.putStringArrayList("navigationStack", new ArrayList<>(navigationStack));
    }

    @Override
    public void onBackPressed() {
        if (!currentFragmentTag.equals("HOME")) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
            Log.d(TAG, "Notification listener removed");
        }
    }
}