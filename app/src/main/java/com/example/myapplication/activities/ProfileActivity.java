package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.dao.HabitDao;
import com.example.myapplication.dao.HabitLogDao;
import com.example.myapplication.dao.UserDao;
import com.example.myapplication.models.Habit;
import com.example.myapplication.models.User;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.NavHelper;
import com.example.myapplication.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;

import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.ImageView;
import android.widget.EditText;
import android.view.View;
import androidx.appcompat.app.AlertDialog;

import android.text.InputType;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ProfileActivity extends AppCompatActivity {

    private UserDao userDao;
    private HabitDao habitDao;
    private HabitLogDao logDao;
    private SessionManager session;

    private TextView tvName, tvSubtitle;
    private Chip chipStreak, chipHabits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        userDao = new UserDao(this);
        habitDao = new HabitDao(this);
        logDao = new HabitLogDao(this);
        session = new SessionManager(this);

        tvName = findViewById(R.id.tv_profile_name);
        tvSubtitle = findViewById(R.id.tv_profile_subtitle);
        tvSubtitle.setOnClickListener(v -> showEditEmailDialog());
        chipStreak = findViewById(R.id.chip_streak);
        chipHabits = findViewById(R.id.chip_habits);

        findViewById(R.id.card_badge_gallery).setOnClickListener(v ->
                startActivity(new Intent(this, BadgeGalleryActivity.class)));
        findViewById(R.id.card_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.card_archives).setOnClickListener(v ->
                startActivity(new Intent(this, ArchivesActivity.class)));

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        NavHelper.setup(this, nav, R.id.navigation_profile);

        android.view.View bell = findViewById(R.id.btn_notifications);
        if (bell != null) {
            bell.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        }

        View btnEdit = findViewById(R.id.btn_edit_profile);
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> showEditNameDialog());
        }

        loadProfile();
    }


    private void loadProfile() {
        long userId = session.getUserId();
        AppExecutors.io().execute(() -> {
            User user = userDao.findById(userId);
            List<Habit> habits = habitDao.getActiveHabits(userId);

            int totalDone = 0;
            for (Habit h : habits) {
                totalDone += logDao.countDone(h.id);
            }
            int habitCount = habits.size();
            final int totalDoneF = totalDone;


            AppExecutors.main().execute(() -> {
                String displayName = "User";
                if (user != null && user.name != null && !user.name.isEmpty()) {
                    displayName = user.name;
                }
                tvName.setText(displayName);
                if (user != null) {
                    tvSubtitle.setText(user.email);
                }

                String initial = displayName.substring(0, 1).toUpperCase();
                Bitmap bmpTop = createTextBitmap(initial, 100, 100, getColor(R.color.primary));
                Bitmap bmpLarge = createTextBitmap(initial, 300, 300, getColor(R.color.primary));
                ((ImageView) findViewById(R.id.profile_avatar_top)).setImageBitmap(bmpTop);
                ((ImageView) findViewById(R.id.profile_avatar_large)).setImageBitmap(bmpLarge);

                chipStreak.setText(totalDoneF + " Completions");
                chipHabits.setText(habitCount + " Habits Built");

            });
        });
    }

    private void showEditEmailDialog() {
        long userId = session.getUserId();
        AppExecutors.io().execute(() -> {
            User user = userDao.findById(userId);
            AppExecutors.main().execute(() -> {
                if (user == null) return;
                EditText input = new EditText(this);
                input.setText(user.email);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                int padding = (int) (20 * getResources().getDisplayMetrics().density);
                input.setPadding(padding, padding, padding, padding);
                new AlertDialog.Builder(this)
                        .setTitle("Modifier l'email")
                        .setView(input)
                        .setPositiveButton("Enregistrer", (dialog, which) -> {
                            String newEmail = input.getText().toString().trim().toLowerCase();
                            if (TextUtils.isEmpty(newEmail) || !newEmail.contains("@")) {
                                Toast.makeText(this, "Email invalide", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            AppExecutors.io().execute(() -> {
                                User existing = userDao.findByEmail(newEmail);
                                AppExecutors.main().execute(() -> {
                                    if (existing != null && existing.id != user.id) {
                                        Toast.makeText(this, "Email déjà utilisé", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    user.email = newEmail;
                                    AppExecutors.io().execute(() -> {
                                        userDao.update(user);
                                        AppExecutors.main().execute(this::loadProfile);
                                    });
                                });
                            });
                        })
                        .setNegativeButton("Annuler", null)
                        .show();
            });
        });
    }

    private void showEditNameDialog() {
        long userId = session.getUserId();
        AppExecutors.io().execute(() -> {
            User user = userDao.findById(userId);
            AppExecutors.main().execute(() -> {
                if (user == null) return;
                EditText input = new EditText(this);
                input.setText(user.name);
                int padding = (int) (20 * getResources().getDisplayMetrics().density);
                input.setPadding(padding, padding, padding, padding);
                new AlertDialog.Builder(this)
                        .setTitle("Edit Name")
                        .setView(input)
                        .setPositiveButton("Save", (dialog, which) -> {
                            String newName = input.getText().toString().trim();
                            if (!newName.isEmpty()) {
                                user.name = newName;
                                AppExecutors.io().execute(() -> {
                                    userDao.update(user);
                                    AppExecutors.main().execute(this::loadProfile);
                                });
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        });
    }

    private Bitmap createTextBitmap(String text, int width, int height, int bgColor) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(bgColor);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTextSize(height / 2f);
        paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = paint.getFontMetrics();
        float x = width / 2f;
        float y = height / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(text, x, y, paint);
        return bitmap;
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavHelper.resume(this, findViewById(R.id.bottom_navigation));
        loadProfile();
    }
}
