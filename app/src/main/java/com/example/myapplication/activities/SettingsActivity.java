package com.example.myapplication.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.myapplication.R;
import com.example.myapplication.dao.HabitDao;
import com.example.myapplication.dao.UserDao;
import com.example.myapplication.models.Habit;
import com.example.myapplication.models.User;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.PasswordUtil;
import com.example.myapplication.util.ReminderScheduler;
import com.example.myapplication.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);

        // Initialize Views
        MaterialButton btnBack = findViewById(R.id.btn_back);
        MaterialSwitch switchReminders = findViewById(R.id.switch_reminders);
        MaterialSwitch switchDarkMode = findViewById(R.id.switch_dark_mode);
        MaterialButton btnLogout = findViewById(R.id.btn_logout);

        // Back Button
        btnBack.setOnClickListener(v -> finish());

        // Account Section
        findViewById(R.id.item_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        findViewById(R.id.item_security).setOnClickListener(v -> showChangePasswordDialog());

        // Preferences Section
        boolean remindersEnabled = sharedPreferences.getBoolean("reminders_enabled", true);
        switchReminders.setChecked(remindersEnabled);
        switchReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("reminders_enabled", isChecked).apply();
            String msg = isChecked ? "Reminders enabled" : "Reminders disabled";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

            SessionManager session = new SessionManager(this);
            if (!session.isLoggedIn()) return;
            long userId = session.getUserId();
            HabitDao habitDao = new HabitDao(this);
            AppExecutors.io().execute(() -> {
                List<Habit> habits = habitDao.getActiveHabits(userId);
                for (Habit h : habits) {
                    if (h.reminderEnabled) {
                        if (isChecked) {
                            ReminderScheduler.schedule(this, h.id, h.name, h.reminderTime, h.specificDays);
                        } else {
                            ReminderScheduler.cancel(this, h.id);
                        }
                    }
                }
            });
        });




        // Dark Mode
        boolean darkEnabled = sharedPreferences.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(darkEnabled);
        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Support
        findViewById(R.id.item_help_center).setOnClickListener(v ->
                Toast.makeText(this, "houssamrhilmane1@gmail.com", Toast.LENGTH_LONG).show());
        findViewById(R.id.item_rate_app).setOnClickListener(v ->
                Toast.makeText(this, "Thank you for using HabitFlow!", Toast.LENGTH_SHORT).show());

        // Legal
        findViewById(R.id.item_privacy_policy).setOnClickListener(v ->
                Toast.makeText(this, "Privacy Policy", Toast.LENGTH_SHORT).show());
        findViewById(R.id.item_terms_of_service).setOnClickListener(v ->
                Toast.makeText(this, "Terms of Service", Toast.LENGTH_SHORT).show());

        // Logout Logic (m3a confirmation)
        btnLogout.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Log out?")
                .setMessage("You'll need to sign in again.")
                .setPositiveButton("Log Out", (d, w) -> doLogout())
                .setNegativeButton("Cancel", null)
                .show());
    }

    private void doLogout() {
        SharedPreferences userPrefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        userPrefs.edit().clear().apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showChangePasswordDialog() {
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);

        EditText etOld = new EditText(this);
        etOld.setHint("Mot de passe actuel");
        etOld.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText etNew = new EditText(this);
        etNew.setHint("Nouveau mot de passe");
        etNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText etConfirm = new EditText(this);
        etConfirm.setHint("Confirmer nouveau mot de passe");
        etConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(etOld);
        container.addView(etNew);
        container.addView(etConfirm);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Changer le mot de passe")
                .setView(container)
                .setPositiveButton("Enregistrer", null)
                .setNegativeButton("Annuler", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String oldPass    = etOld.getText().toString();
            String newPass    = etNew.getText().toString();
            String confirmPass = etConfirm.getText().toString();

            if (TextUtils.isEmpty(oldPass)) { etOld.setError("Entrez votre mot de passe actuel"); return; }
            if (newPass.length() < 6)       { etNew.setError("6 caractères minimum"); return; }
            if (!newPass.equals(confirmPass)){ etConfirm.setError("Les mots de passe ne correspondent pas"); return; }

            SessionManager session = new SessionManager(this);
            UserDao userDao = new UserDao(this);
            long userId = session.getUserId();
            String oldHash = PasswordUtil.hash(oldPass);

            AppExecutors.io().execute(() -> {
                User user = userDao.findById(userId);
                AppExecutors.main().execute(() -> {
                    if (user == null) { Toast.makeText(this, "Erreur utilisateur", Toast.LENGTH_SHORT).show(); return; }
                    if (!oldHash.equals(user.passwordHash)) {
                        etOld.setError("Mot de passe actuel incorrect");
                        return;
                    }
                    String newHash = PasswordUtil.hash(newPass);
                    AppExecutors.io().execute(() -> {
                        user.passwordHash = newHash;
                        userDao.update(user);
                        AppExecutors.main().execute(() -> {
                            dialog.dismiss();
                            Toast.makeText(this, "Mot de passe mis à jour", Toast.LENGTH_SHORT).show();
                        });
                    });
                });
            });
        }));

        dialog.show();
    }

}
