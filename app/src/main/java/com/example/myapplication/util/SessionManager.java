package com.example.myapplication.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Kaydabber session dyal l'utilisateur f SharedPreferences ("UserSession").
 * SettingsActivity deja kaymse7 had l-prefs f logout.
 */
public class SessionManager {

    private static final String PREFS = "UserSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_EMAIL = "user_email";

    private static final long NO_USER = -1L;

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveSession(long userId, String name, String email) {
        prefs.edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getLong(KEY_USER_ID, NO_USER) != NO_USER;
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, NO_USER);
    }

    public String getName() {
        return prefs.getString(KEY_NAME, "");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
