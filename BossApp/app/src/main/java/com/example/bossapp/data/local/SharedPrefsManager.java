package com.example.bossapp.data.local;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsManager {
    private static final String PREFS_NAME = "BossAppPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_AVATAR_INDEX = "avatarIndex";

    private SharedPreferences prefs;

    public SharedPrefsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Čuvanje user ID-a
    public void saveUserId(String userId) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply();
    }

    // Dobijanje user ID-a
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    // Provera da li je korisnik ulogovan
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Čuvanje dodatnih podataka korisnika
    public void saveUserData(String username, int avatarIndex) {
        prefs.edit()
                .putString(KEY_USERNAME, username)
                .putInt(KEY_AVATAR_INDEX, avatarIndex)
                .apply();
    }

    // Dobijanje username-a
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    // Dobijanje avatar index-a
    public int getAvatarIndex() {
        return prefs.getInt(KEY_AVATAR_INDEX, 0);
    }

    // Brisanje sesije (logout)
    public void clearSession() {
        prefs.edit()
                .clear()
                .apply();
    }
}