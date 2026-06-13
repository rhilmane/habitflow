package com.example.myapplication.activities;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.dao.HabitDao;
import com.example.myapplication.dao.HabitLogDao;
import com.example.myapplication.models.Habit;
import com.example.myapplication.models.HabitLog;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.DateUtil;
import com.example.myapplication.util.HabitSchedule;
import com.example.myapplication.util.NavHelper;
import com.example.myapplication.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class StatisticsActivity extends AppCompatActivity {

    private HabitDao habitDao;
    private HabitLogDao habitLogDao;
    private SessionManager session;

    private TextView tvTotalCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        habitDao = new HabitDao(this);
        habitLogDao = new HabitLogDao(this);
        session = new SessionManager(this);

        tvTotalCount = findViewById(R.id.tv_total_count);

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        NavHelper.setup(this, nav, R.id.navigation_stats);

        loadStats();
    }

    private void loadStats() {
        long userId = session.getUserId();
        AppExecutors.io().execute(() -> {
            List<Habit> habits = habitDao.getActiveHabits(userId);
            int totalDone = 0;
            
            int[] scheduledCounts = new int[7];
            int[] doneCounts = new int[7];
            int totalScheduled7Days = 0;
            int totalDone7Days = 0;

            String[] last7Dates = new String[7];
            String[] last7DayCodes = new String[7];

            for (int d = 6; d >= 0; d--) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -d);
                last7Dates[6 - d] = DateUtil.dayOffset(-d);
                int dow = cal.get(Calendar.DAY_OF_WEEK);
                switch (dow) {
                    case Calendar.MONDAY:    last7DayCodes[6 - d] = "MON"; break;
                    case Calendar.TUESDAY:   last7DayCodes[6 - d] = "TUE"; break;
                    case Calendar.WEDNESDAY: last7DayCodes[6 - d] = "WED"; break;
                    case Calendar.THURSDAY:  last7DayCodes[6 - d] = "THU"; break;
                    case Calendar.FRIDAY:    last7DayCodes[6 - d] = "FRI"; break;
                    case Calendar.SATURDAY:  last7DayCodes[6 - d] = "SAT"; break;
                    default:                 last7DayCodes[6 - d] = "SUN"; break;
                }
            }

            Map<Long, Set<String>> doneDatesByHabit = new HashMap<>();
            for (Habit h : habits) {
                List<HabitLog> logs = habitLogDao.getLogsForHabit(h.id);
                Set<String> doneDates = new HashSet<>();
                for (HabitLog l : logs) if (l.done) doneDates.add(l.date);
                doneDatesByHabit.put(h.id, doneDates);
                totalDone += doneDates.size();

                for (int i = 0; i < 7; i++) {
                    if (HabitSchedule.isScheduledOn(h, last7DayCodes[i])) {
                        scheduledCounts[i]++;
                        totalScheduled7Days++;
                        if (doneDates.contains(last7Dates[i])) {
                            doneCounts[i]++;
                            totalDone7Days++;
                        }
                    }
                }
            }

            // Par jour : liste "✓/✗ Nom habitude" pour les habitudes planifiées
            List<List<String>> weekStatus = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                List<String> dayStatus = new ArrayList<>();
                for (Habit h : habits) {
                    if (HabitSchedule.isScheduledOn(h, last7DayCodes[i])) {
                        Set<String> d = doneDatesByHabit.get(h.id);
                        boolean done = d != null && d.contains(last7Dates[i]);
                        dayStatus.add((done ? "✓  " : "✗  ") + h.name);
                    }
                }
                weekStatus.add(dayStatus);
            }
            final List<List<String>> finalWeekStatus = weekStatus;

            int completionRate = totalScheduled7Days == 0 ? 0
                    : Math.round(totalDone7Days * 100f / totalScheduled7Days);

            // Normaliser par le max des habitudes FAITES (pas planifiées)
            int maxDone = 1;
            for (int d : doneCounts) if (d > maxDone) maxDone = d;

            final int finalTotalDone    = totalDone;
            final int finalCompletionRate = completionRate;
            final int finalMaxDone      = maxDone;
            final int[] finalDoneCounts = doneCounts;
            final String[] finalDayCodes = last7DayCodes;
            final String todayDate      = DateUtil.today();
            final String[] finalDates   = last7Dates;

            AppExecutors.main().execute(() -> {
                tvTotalCount.setText(String.valueOf(finalTotalDone));

                TextView tvRate = findViewById(R.id.tv_completion_rate);
                if (tvRate != null) {
                    tvRate.setText(finalCompletionRate + "% this week");
                }

                float density = getResources().getDisplayMetrics().density;
                int maxH   = (int) (160 * density);
                int margin = (int) (5 * density);
                int radius = (int) (6 * density);

                LinearLayout chartContainer = findViewById(R.id.chart_container);
                if (chartContainer != null) {
                    chartContainer.removeAllViews();
                    for (int i = 0; i < 7; i++) {
                        boolean isToday = todayDate.equals(finalDates[i]);

                        int h = (int) (maxH * finalDoneCounts[i] / (float) finalMaxDone);
                        if (h < (int) (8 * density)) h = (int) (8 * density);

                        View bar = new View(StatisticsActivity.this);
                        GradientDrawable gd = new GradientDrawable();
                        gd.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
                        gd.setColor(getColor(R.color.primary));
                        bar.setBackground(gd);
                        bar.setAlpha(isToday ? 1f : 0.35f);

                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, h);
                        lp.weight = 1;
                        lp.setMargins(margin, 0, margin, 0);
                        bar.setLayoutParams(lp);

                        final int idx = i;
                        bar.setOnClickListener(v -> {
                            List<String> status = finalWeekStatus.get(idx);
                            String msg = status.isEmpty()
                                    ? "Aucune habitude planifiée ce jour"
                                    : TextUtils.join("\n", status);
                            new MaterialAlertDialogBuilder(StatisticsActivity.this)
                                    .setTitle(dayLabel(finalDayCodes[idx]) + " — " + finalDates[idx])
                                    .setMessage(msg)
                                    .setPositiveButton("OK", null)
                                    .show();
                        });

                        chartContainer.addView(bar);
                    }
                }

                LinearLayout chartLabels = findViewById(R.id.chart_labels);
                if (chartLabels != null) {
                    chartLabels.removeAllViews();
                    for (int i = 0; i < 7; i++) {
                        boolean isToday = todayDate.equals(finalDates[i]);
                        TextView lbl = new TextView(StatisticsActivity.this);
                        lbl.setText(dayLabel(finalDayCodes[i]));
                        lbl.setTextColor(isToday
                                ? getColor(R.color.primary)
                                : getColor(R.color.outline));
                        lbl.setTextSize(11);
                        lbl.setTypeface(null, isToday
                                ? android.graphics.Typeface.BOLD
                                : android.graphics.Typeface.NORMAL);
                        lbl.setGravity(android.view.Gravity.CENTER);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp.weight = 1;
                        lbl.setLayoutParams(lp);
                        chartLabels.addView(lbl);
                    }
                }
            });
        });
    }

    private static String dayLabel(String code) {
        switch (code) {
            case "MON": return "Mo";
            case "TUE": return "Tu";
            case "WED": return "We";
            case "THU": return "Th";
            case "FRI": return "Fr";
            case "SAT": return "Sa";
            case "SUN": return "Su";
            default:    return code.substring(0, 2);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavHelper.resume(this, findViewById(R.id.bottom_navigation));
    }
}
