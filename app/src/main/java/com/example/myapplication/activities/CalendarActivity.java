package com.example.myapplication.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.util.CalendarAdapter;
import com.example.myapplication.dao.HabitDao;
import com.example.myapplication.dao.HabitLogDao;
import com.example.myapplication.models.Habit;
import com.example.myapplication.models.HabitLog;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.DateUtil;
import com.example.myapplication.util.NavHelper;
import com.example.myapplication.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity {

    private HabitDao habitDao;
    private HabitLogDao logDao;
    private SessionManager session;
    private CalendarAdapter adapter;

    private TextView tvMonth, tvCompletedToday, tvTotalHabits;

    // Data li kaybqa stored bach katkhdm fClick handler
    private List<Habit> allHabits = new ArrayList<>();
    private Map<String, Set<Long>> doneHabitsByDate = new HashMap<>();

    // Chhar li m3roud daba (kanثبتو 3la lowwel dyal chhar)
    private final Calendar displayedMonth = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        habitDao = new HabitDao(this);
        logDao = new HabitLogDao(this);
        session = new SessionManager(this);

        tvMonth = findViewById(R.id.tv_month);
        tvCompletedToday = findViewById(R.id.tv_completed_today);
        tvTotalHabits = findViewById(R.id.tv_total_habits);

        RecyclerView rv = findViewById(R.id.rv_calendar);
        adapter = new CalendarAdapter();
        rv.setAdapter(adapter);
        adapter.setOnDayClickListener(this::showDayDialog);

        MaterialButton btnPrev = findViewById(R.id.btn_prev_month);
        MaterialButton btnNext = findViewById(R.id.btn_next_month);
        btnPrev.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, -1);
            loadMonth();
        });
        btnNext.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, 1);
            loadMonth();
        });

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        NavHelper.setup(this, nav, R.id.navigation_calendar);

        android.view.View bell = findViewById(R.id.btn_notifications);
        if (bell != null) {
            bell.setOnClickListener(v -> startActivity(new android.content.Intent(this, SettingsActivity.class)));
        }

        displayedMonth.set(Calendar.DAY_OF_MONTH, 1);
        loadMonth();
    }

    private void loadMonth() {
        long userId = session.getUserId();
        final int year = displayedMonth.get(Calendar.YEAR);
        final int month = displayedMonth.get(Calendar.MONTH);

        SimpleDateFormat monthFmt = new SimpleDateFormat("MMMM yyyy", Locale.US);
        tvMonth.setText(monthFmt.format(displayedMonth.getTime()));

        AppExecutors.io().execute(() -> {
            List<Habit> habits = habitDao.getActiveHabits(userId);
            int totalHabits = habits.size();

            // Map: date → count + set of done habit IDs
            Map<String, Integer> doneByDate = new HashMap<>();
            Map<String, Set<Long>> doneIdsByDate = new HashMap<>();
            for (Habit h : habits) {
                for (HabitLog log : logDao.getLogsForHabit(h.id)) {
                    if (log.done) {
                        Integer prev = doneByDate.get(log.date);
                        doneByDate.put(log.date, prev == null ? 1 : prev + 1);
                        Set<Long> ids = doneIdsByDate.get(log.date);
                        if (ids == null) { ids = new HashSet<>(); doneIdsByDate.put(log.date, ids); }
                        ids.add(h.id);
                    }
                }
            }

            // Bni l'grid dyal chhar
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, 1);
            int firstDayOffset = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY; // 0=SUN
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            String today = DateUtil.today();

            List<CalendarAdapter.Day> cells = new ArrayList<>();
            for (int i = 0; i < firstDayOffset; i++) {
                cells.add(new CalendarAdapter.Day(0, 0f, false, ""));
            }
            for (int d = 1; d <= daysInMonth; d++) {
                String dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, d);
                Integer done = doneByDate.get(dateStr);
                float ratio = (totalHabits == 0 || done == null) ? 0f
                        : Math.min(1f, (float) done / totalHabits);
                cells.add(new CalendarAdapter.Day(d, ratio, dateStr.equals(today), dateStr));
            }

            int doneTodayCount = doneByDate.containsKey(today) ? doneByDate.get(today) : 0;
            final List<Habit> habitsCopy = new ArrayList<>(habits);

            AppExecutors.main().execute(() -> {
                allHabits = habitsCopy;
                doneHabitsByDate = doneIdsByDate;
                adapter.setData(cells);
                tvCompletedToday.setText(doneTodayCount + " / " + totalHabits);
                tvTotalHabits.setText(totalHabits + " Active");
            });
        });
    }

    private void showDayDialog(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return;
        Set<Long> doneIds = doneHabitsByDate.containsKey(dateStr)
                ? doneHabitsByDate.get(dateStr) : new HashSet<>();
        StringBuilder sb = new StringBuilder();
        for (Habit h : allHabits) {
            sb.append(doneIds.contains(h.id) ? "✓  " : "✗  ").append(h.name).append("\n");
        }
        String msg = allHabits.isEmpty() ? "Aucune habitude active." : sb.toString().trim();
        new MaterialAlertDialogBuilder(this)
                .setTitle(dateStr)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavHelper.resume(this, findViewById(R.id.bottom_navigation));
    }
}
