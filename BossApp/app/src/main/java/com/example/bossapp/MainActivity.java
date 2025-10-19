package com.example.bossapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import com.example.bossapp.business.LocalNotificationHelper;
import com.example.bossapp.presentation.alliance.AllianceFragment;
import com.example.bossapp.presentation.boss.BossFightActivity;
import com.example.bossapp.presentation.home.HomeFragment;
import com.example.bossapp.presentation.profile.ProfileFragment;
import com.example.bossapp.presentation.statistics.StatisticsFragment;
import com.example.bossapp.presentation.task.TaskFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNavigationView;
    private String currentFragmentTag = "HOME";
    private List<String> navigationStack = new ArrayList<>();

    private List<ListenerRegistration> notificationListeners = new ArrayList<>();
    private LocalNotificationHelper notificationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        notificationHelper = new LocalNotificationHelper(this);

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
                } else if (itemId == R.id.nav_tasks) {
                    selectedFragment = new TaskFragment();
                    tag = "TASKS";
                } else if (itemId == R.id.nav_alliance) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        1
                );
            }
        }

        // *** DODAJ LISTENERE ZA NOTIFIKACIJE ***
        startListeningForNotifications();

        // Rukuj intent-om iz notifikacije
        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void startListeningForNotifications() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            Log.e(TAG, "No current user, cannot start notification listeners");
            return;
        }

        Log.d(TAG, "Starting notification listeners for user: " + currentUserId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // *** VAŽNO: Sačuvaj trenutni timestamp kada pokrećemo listener ***
        final long listenerStartTime = System.currentTimeMillis();
        Log.d(TAG, "Listener start time: " + listenerStartTime);

        // 1. Friend Requests
        ListenerRegistration friendRequestListener = db.collection("friendRequests")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Friend request listener error", error);
                        return;
                    }

                    if (snapshots == null) return;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.ADDED) {
                            // *** PROVERI DA LI JE DOKUMENT NOVIJI OD LISTENER START TIME ***
                            Long timestamp = change.getDocument().getLong("timestamp");
                            if (timestamp != null && timestamp > listenerStartTime) {
                                String senderUsername = change.getDocument().getString("senderUsername");
                                String senderId = change.getDocument().getString("senderId");

                                Log.d(TAG, "NEW friend request from: " + senderUsername +
                                        " (timestamp: " + timestamp + ")");
                                notificationHelper.showFriendRequestNotification(senderUsername, senderId);
                            } else {
                                Log.d(TAG, "Ignoring OLD friend request (timestamp: " + timestamp + ")");
                            }
                        }
                    }
                });
        notificationListeners.add(friendRequestListener);

        // 2. Friend Request Accepted
        ListenerRegistration friendAcceptedListener = db.collection("friendRequests")
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("status", "accepted")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Friend accepted listener error", error);
                        return;
                    }

                    if (snapshots == null) return;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.MODIFIED) {
                            // *** ZA MODIFIED dokumente, proveravamo vreme modifikacije ***
                            Long timestamp = change.getDocument().getLong("timestamp");
                            if (timestamp != null && timestamp > listenerStartTime) {
                                String receiverId = change.getDocument().getString("receiverId");

                                Log.d(TAG, "Friend request accepted (timestamp: " + timestamp + ")");

                                db.collection("users").document(receiverId).get()
                                        .addOnSuccessListener(doc -> {
                                            if (doc.exists()) {
                                                String username = doc.getString("username");
                                                Log.d(TAG, username + " accepted friend request");
                                                notificationHelper.showFriendAcceptedNotification(username);
                                            }
                                        });
                            } else {
                                Log.d(TAG, "Ignoring OLD friend acceptance (timestamp: " + timestamp + ")");
                            }
                        }
                    }
                });
        notificationListeners.add(friendAcceptedListener);

        // 3. Alliance Invitations
        ListenerRegistration allianceInviteListener = db.collection("allianceInvitations")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Alliance invitation listener error", error);
                        return;
                    }

                    if (snapshots == null) return;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.ADDED) {
                            Long timestamp = change.getDocument().getLong("timestamp");
                            if (timestamp != null && timestamp > listenerStartTime) {
                                String senderUsername = change.getDocument().getString("senderUsername");
                                String allianceName = change.getDocument().getString("allianceName");
                                String allianceId = change.getDocument().getString("allianceId");

                                Log.d(TAG, "NEW alliance invitation from: " + senderUsername +
                                        " (timestamp: " + timestamp + ")");
                                notificationHelper.showAllianceInvitationNotification(
                                        senderUsername, allianceName, allianceId);
                            } else {
                                Log.d(TAG, "Ignoring OLD alliance invitation (timestamp: " + timestamp + ")");
                            }
                        }
                    }
                });
        notificationListeners.add(allianceInviteListener);

        // 4. Alliance Notifications (accepted/declined/messages)
        ListenerRegistration allianceNotifListener = db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("read", false)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Alliance notification listener error", error);
                        return;
                    }

                    if (snapshots == null) return;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.ADDED) {
                            Long timestamp = change.getDocument().getLong("timestamp");
                            if (timestamp != null && timestamp > listenerStartTime) {
                                String type = change.getDocument().getString("type");

                                if ("alliance_accepted".equals(type)) {
                                    String username = change.getDocument().getString("acceptedUsername");
                                    String allianceName = change.getDocument().getString("allianceName");

                                    Log.d(TAG, username + " accepted alliance invitation (timestamp: " +
                                            timestamp + ")");
                                    notificationHelper.showAllianceAcceptedNotification(username, allianceName);

                                } else if ("alliance_declined".equals(type)) {
                                    String username = change.getDocument().getString("declinedUsername");
                                    String allianceName = change.getDocument().getString("allianceName");

                                    Log.d(TAG, username + " declined alliance invitation (timestamp: " +
                                            timestamp + ")");
                                    notificationHelper.showAllianceDeclinedNotification(username, allianceName);

                                } else if ("alliance_message".equals(type)) {
                                    String senderUsername = change.getDocument().getString("senderUsername");
                                    String messageText = change.getDocument().getString("messageText");
                                    String allianceId = change.getDocument().getString("allianceId");

                                    Log.d(TAG, "NEW alliance message from: " + senderUsername +
                                            " (timestamp: " + timestamp + ")");
                                    notificationHelper.showAllianceMessageNotification(
                                            senderUsername, messageText, allianceId);
                                }
                            } else {
                                Log.d(TAG, "Ignoring OLD notification (timestamp: " + timestamp + ")");
                            }
                        }
                    }
                });
        notificationListeners.add(allianceNotifListener);

        Log.d(TAG, "All notification listeners started successfully");
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent == null) {
            Log.d(TAG, "Intent is null");
            return;
        }

        String openFragment = intent.getStringExtra("openFragment");

        if (openFragment != null) {
            Log.d(TAG, "Opening fragment from notification: " + openFragment);

            Fragment fragment = null;
            String tag = "";

            switch (openFragment) {
                case "notifications":
                    fragment = new com.example.bossapp.presentation.notifications.NotificationsFragment();
                    tag = "NOTIFICATIONS";
                    break;

                case "alliance":
                    fragment = new AllianceFragment();
                    tag = "ALLIANCE";
                    bottomNavigationView.setSelectedItemId(R.id.nav_alliance);
                    break;

                case "alliance_chat":
                    String allianceId = intent.getStringExtra("allianceId");
                    if (allianceId != null) {
                        Log.d(TAG, "Opening alliance chat: " + allianceId);

                        FirebaseFirestore.getInstance()
                                .collection("alliances")
                                .document(allianceId)
                                .get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        String allianceName = doc.getString("allianceName");

                                        com.example.bossapp.presentation.alliance.AllianceChatFragment chatFragment =
                                                com.example.bossapp.presentation.alliance.AllianceChatFragment.newInstance(
                                                        allianceId,
                                                        allianceName != null ? allianceName : "Alliance Chat"
                                                );

                                        getSupportFragmentManager()
                                                .beginTransaction()
                                                .replace(R.id.fragmentContainer, chatFragment)
                                                .addToBackStack(null)
                                                .commit();
                                    }
                                });

                        intent.removeExtra("openFragment");
                        intent.removeExtra("allianceId");
                        return;
                    }
                    break;

                case "profile":
                    fragment = new ProfileFragment();
                    tag = "PROFILE";
                    bottomNavigationView.setSelectedItemId(R.id.nav_profile);
                    break;
            }

            if (fragment != null) {
                loadFragment(fragment, tag);
                addToNavigationStack(tag);
            }

            intent.removeExtra("openFragment");
        } else {
            Log.d(TAG, "No openFragment extra in intent");
        }
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
            } else if (previousTag.equals("ALLIANCE")) {
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

        // Ukloni sve listenere
        for (ListenerRegistration listener : notificationListeners) {
            listener.remove();
        }
        notificationListeners.clear();

        Log.d(TAG, "All notification listeners removed");
    }
}