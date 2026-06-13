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

import com.example.myapplication.R;
import com.example.myapplication.dao.UserDao;
import com.example.myapplication.models.User;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.PasswordUtil;
import com.example.myapplication.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

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

            com.example.myapplication.util.SessionManager session = new com.example.myapplication.util.SessionManager(this);
            if (!session.isLoggedIn()) return;
            long userId = session.getUserId();
            com.example.myapplication.dao.HabitDao habitDao = new com.example.myapplication.dao.HabitDao(this);
            com.example.myapplication.util.AppExecutors.io().execute(() -> {
                java.util.List<com.example.myapplication.models.Habit> habits = habitDao.getActiveHabits(userId);
                for (com.example.myapplication.models.Habit h : habits) {
                    if (h.reminderEnabled) {
                        if (isChecked) {
                            com.example.myapplication.util.ReminderScheduler.schedule(this, h.id, h.name, h.reminderTime, h.specificDays);
                        } else {
                            com.example.myapplication.util.ReminderScheduler.cancel(this, h.id);
                        }
                    }
                }
            });
        });




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
