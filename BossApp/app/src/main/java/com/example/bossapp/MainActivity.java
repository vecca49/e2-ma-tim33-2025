package com.example.bossapp;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import com.example.bossapp.presentation.home.HomeFragment;
import com.example.bossapp.presentation.profile.ProfileFragment;
import com.example.bossapp.presentation.statistics.StatisticsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private String currentFragmentTag = "HOME";
    private List<String> navigationStack = new ArrayList<>();

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
                    // Ne šaljemo userId argument - prikazujemo MOJ profil
                    selectedFragment = new ProfileFragment();
                    tag = "PROFILE";
                } else if (itemId == R.id.nav_statistics) {
                    selectedFragment = new StatisticsFragment();
                    tag = "STATISTICS";
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment, tag);
                    addToNavigationStack(tag);
                    return true;
                }
                return false;
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
}