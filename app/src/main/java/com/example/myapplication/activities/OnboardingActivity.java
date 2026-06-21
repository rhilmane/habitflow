package com.example.myapplication.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

/**
 * Écran dyal welcome li kayban ghir f lowwel marra (9bel Login).
 */
public class OnboardingActivity extends AppCompatActivity {

    public static final String PREFS = "AppSettings";
    public static final String KEY_DONE = "onboarding_done";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        findViewById(R.id.btn_get_started).setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_DONE, true).apply();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    public static boolean isDone(android.content.Context context) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_DONE, false);
    }
}
