package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.dao.HabitDao;
import com.example.myapplication.dao.HabitLogDao;
import com.example.myapplication.dao.MicroActionDao;
import com.example.myapplication.models.Habit;
import com.example.myapplication.models.HabitLog;
import com.example.myapplication.models.MicroAction;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.BadgeEvaluator;
import com.example.myapplication.util.DateUtil;
import com.example.myapplication.util.HabitStyle;
import com.example.myapplication.util.ReminderScheduler;
import com.example.myapplication.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HabitDetailActivity extends AppCompatActivity {

    public static final String EXTRA_HABIT_ID = "habit_id";

    private HabitDao habitDao;
    private HabitLogDao habitLogDao;
    private MicroActionDao microActionDao;
    private long habitId;

    private TextView tvName, tvStreak, tvWeekCount;
    private MaterialButton btnEdit, btnArchive, btnMarkDone;
    private View cardMicroActions;
    private LinearLayout microActionsList;
    private LinearLayout weekChartContainer, weekChartLabels;
    private MaterialCardView detailIconCircle;
    private ImageView ivDetailIcon;
    private SessionManager session;
    private boolean isDoneToday = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_detail);
        habitDao = new HabitDao(this);
        habitLogDao = new HabitLogDao(this);
        microActionDao = new MicroActionDao(this);
        session = new SessionManager(this);
        habitId = getIntent().getLongExtra(EXTRA_HABIT_ID, -1);

        tvName = findViewById(R.id.tv_habit_name);
        tvStreak = findViewById(R.id.tv_streak);
        tvWeekCount = findViewById(R.id.tv_week_count);
        btnEdit = findViewById(R.id.btn_edit);
        btnArchive = findViewById(R.id.btn_archive);
        btnMarkDone = findViewById(R.id.btn_mark_done);
        cardMicroActions = findViewById(R.id.card_micro_actions);
        microActionsList = findViewById(R.id.micro_actions_list);
        weekChartContainer = findViewById(R.id.week_chart_container);
        weekChartLabels = findViewById(R.id.week_chart_labels);
        detailIconCircle = findViewById(R.id.detail_icon_circle);
        ivDetailIcon = findViewById(R.id.iv_detail_icon);

        btnMarkDone.setOnClickListener(v -> toggleDoneToday());

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddHabitActivity.class);
            intent.putExtra(AddHabitActivity.EXTRA_HABIT_ID, habitId);
            startActivity(intent);
        });
        btnArchive.setOnClickListener(v -> confirmArchive());
        findViewById(R.id.btn_delete).setOnClickListener(v -> confirmDelete());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        View bell = findViewById(R.id.btn_notifications);
        if (bell != null) {
            bell.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        }

        if (habitId == -1) {
            Toast.makeText(this, "Habit not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (habitId != -1) loadHabit();
    }

    private void loadHabit() {
        AppExecutors.io().execute(() -> {
            Habit habit = habitDao.findById(habitId);
            List<HabitLog> logs = habitLogDao.getLogsForHabit(habitId);
            List<MicroAction> microActions = microActionDao.getForHabit(habitId);

            Set<String> doneDates = new HashSet<>();
            for (HabitLog log : logs) {
                if (log.done) doneDates.add(log.date);
            }
            int streak = computeStreak(doneDates);
            int weekCount = computeWeekCount(doneDates);
            boolean doneToday = doneDates.contains(DateUtil.today());

            AppExecutors.main().execute(() -> {
                if (habit == null) {
                    Toast.makeText(this, "Habit not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                tvName.setText(habit.name);
                tvStreak.setText(streak + " Day Streak");
                tvWeekCount.setText(weekCount + "/7 Days");
                HabitStyle.apply(detailIconCircle, ivDetailIcon, habit.iconResId, habit.colorTag);
                populateMicroActions(microActions);
                buildWeekChart(doneDates);
                isDoneToday = doneToday;
                updateMarkDoneButton();
            });
        });
    }

    private void toggleDoneToday() {
        isDoneToday = !isDoneToday;
        updateMarkDoneButton();
        String today = DateUtil.today();
        long userId = session.getUserId();
        boolean doneNow = isDoneToday;
        AppExecutors.io().execute(() -> {
            habitLogDao.upsert(new HabitLog(habitId, today, doneNow));
            if (doneNow) BadgeEvaluator.evaluate(getApplicationContext(), userId);
        });
    }

    private void updateMarkDoneButton() {
        if (btnMarkDone == null) return;
        if (isDoneToday) {
            btnMarkDone.setText("✓  Fait aujourd'hui");
            btnMarkDone.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getColor(R.color.secondary)));
        } else {
            btnMarkDone.setText("Marquer comme fait");
            btnMarkDone.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getColor(R.color.primary)));
        }
    }

    private void populateMicroActions(List<MicroAction> microActions) {
        microActionsList.removeAllViews();
        if (microActions.isEmpty()) {
            cardMicroActions.setVisibility(View.GONE);
            return;
        }
        cardMicroActions.setVisibility(View.VISIBLE);
        String today = DateUtil.today();
        int px12 = dp(12);
        int px16 = dp(16);
        for (int i = 0; i < microActions.size(); i++) {
            MicroAction m = microActions.get(i);

            // Divider bain les items (sauf qbel lowwel)
            if (i > 0) {
                View divider = new View(this);
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
                divider.setLayoutParams(dlp);
                divider.setBackgroundColor(getColor(R.color.surface_variant));
                microActionsList.addView(divider);
            }

            MaterialCheckBox cb = new MaterialCheckBox(this);
            cb.setText(m.text);
            cb.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            cb.setTextColor(getColor(R.color.on_surface));
            cb.setPadding(px12, px16, px12, px16);
            cb.setChecked(m.isDoneOn(today));
            cb.setOnCheckedChangeListener((v, checked) ->
                    AppExecutors.io().execute(() ->
                            microActionDao.setDoneDate(m.id, checked ? today : null)));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cb.setLayoutParams(lp);
            microActionsList.addView(cb);
        }
    }

    private void buildWeekChart(Set<String> doneDates) {
        weekChartContainer.removeAllViews();
        weekChartLabels.removeAllViews();
        String today = DateUtil.today();
        String[] dayLabels = {"S", "M", "T", "W", "T", "F", "S"};
        Calendar cal = Calendar.getInstance();
        // Build last 7 days oldest→newest
        String[] last7 = new String[7];
        String[] last7Labels = new String[7];
        for (int i = 6; i >= 0; i--) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, -i);
            last7[6 - i] = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
            last7Labels[6 - i] = dayLabels[c.get(Calendar.DAY_OF_WEEK) - 1];
        }
        int barMaxDp = 80;
        int barMinDp = 10;
        for (int i = 0; i < 7; i++) {
            boolean done = doneDates.contains(last7[i]);
            boolean isToday = last7[i].equals(today);
            // Bar
            View bar = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    dp(done ? barMaxDp : barMinDp));
            lp.weight = 1;
            lp.setMargins(dp(3), 0, dp(3), 0);
            bar.setLayoutParams(lp);
            int radius = dp(4);
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
            if (done) {
                shape.setColor(getColor(isToday ? R.color.primary : R.color.secondary));
            } else {
                shape.setColor(getColor(R.color.surface_variant));
            }
            bar.setBackground(shape);
            weekChartContainer.addView(bar);
            // Label
            TextView label = new TextView(this);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            llp.weight = 1;
            label.setLayoutParams(llp);
            label.setText(last7Labels[i]);
            label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10);
            label.setTextColor(getColor(isToday ? R.color.primary : R.color.on_surface_variant));
            label.setGravity(android.view.Gravity.CENTER);
            weekChartLabels.addView(label);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** Streak = ayyam mtatalia done 7tal l'youm (wla lbare7 ila l'youm mazal). */
    private int computeStreak(Set<String> doneDates) {
        int streak = 0;
        int offset = 0;
        if (!doneDates.contains(DateUtil.today())) {
            offset = -1; // l'youm mazal ma t9ada, nbdaw mn lbare7
        }
        while (doneDates.contains(DateUtil.dayOffset(offset))) {
            streak++;
            offset--;
        }
        return streak;
    }

    /** 3adad dyal l'ayyam done f akhir 7 ayyam. */
    private int computeWeekCount(Set<String> doneDates) {
        int count = 0;
        for (int d = 0; d > -7; d--) {
            if (doneDates.contains(DateUtil.dayOffset(d))) count++;
        }
        return count;
    }

    private void confirmArchive() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Archive habit?")
                .setMessage("It will be moved to Archives. You can restore it later.")
                .setPositiveButton("Archive", (d, w) -> archiveHabit())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void archiveHabit() {
        btnArchive.setEnabled(false);
        // Kanlghiw reminder dyal had l'3ada
        ReminderScheduler.cancel(this, habitId);
        AppExecutors.io().execute(() -> {
            habitDao.setArchived(habitId, true);
            AppExecutors.main().execute(() -> {
                Toast.makeText(this, "Habit archived", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete habit?")
                .setMessage("This permanently deletes the habit and all its history. This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> deleteHabit())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteHabit() {
        ReminderScheduler.cancel(this, habitId);
        AppExecutors.io().execute(() -> {
            habitDao.delete(habitId); // CASCADE kaymse7 logs + micro actions
            AppExecutors.main().execute(() -> {
                Toast.makeText(this, "Habit deleted", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}
