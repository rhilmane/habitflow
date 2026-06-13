package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.util.HabitAdapter;
import com.example.myapplication.dao.HabitDao;
import com.example.myapplication.dao.HabitLogDao;
import com.example.myapplication.dao.UserDao;
import com.example.myapplication.models.Habit;
import com.example.myapplication.models.HabitLog;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.AvatarUtil;
import com.example.myapplication.util.BadgeEvaluator;
import com.example.myapplication.util.DateUtil;
import com.example.myapplication.util.HabitSchedule;
import com.example.myapplication.util.NavHelper;
import com.example.myapplication.util.SessionManager;
import com.example.myapplication.util.SyncManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements HabitAdapter.OnHabitInteraction {

    private SessionManager session;
    private HabitDao habitDao;
    private HabitLogDao logDao;
    private HabitAdapter adapter;

    private TextView tvCompletionPercent, tvMomentumSubtitle, tvDashboardStreak, tvTodayEmpty;
    private TextView tvLongestStreak, tvWeeklyAvg;
    private LinearProgressIndicator progressMomentum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Onboarding gate: lowwel mرة → welcome screen
        if (!OnboardingActivity.isDone(this)) {
            Intent intent = new Intent(this, OnboardingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Auth gate: ila l'utilisateur machi connecté → siftou l Login
        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            goToLogin();
            return;
        }

        habitDao = new HabitDao(this);
        logDao = new HabitLogDao(this);

        // Android 13+: runtime permission pour les notifications
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        // Synchronisation Supabase en arrière-plan dès que l'utilisateur est connecté
        SyncManager.syncAll(this);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // L'insets dyal status bar / nav bar kayt-handلو b android:fitsSystemWindows="true" f l'layout root.

        tvCompletionPercent = findViewById(R.id.tv_completion_percent);
        tvMomentumSubtitle  = findViewById(R.id.tv_momentum_subtitle);
        tvDashboardStreak   = findViewById(R.id.tv_dashboard_streak);
        tvTodayEmpty        = findViewById(R.id.tv_today_empty);
        tvLongestStreak     = findViewById(R.id.tv_longest_streak);
        tvWeeklyAvg         = findViewById(R.id.tv_weekly_avg);
        progressMomentum    = findViewById(R.id.progress_momentum);

        RecyclerView rv = findViewById(R.id.rv_today_habits);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HabitAdapter(this);
        rv.setAdapter(adapter);

        setupNavigation();
        verifyUserExists();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavHelper.resume(this, findViewById(R.id.bottom_navigation));
        if (session != null && session.isLoggedIn()) {
            loadDashboard();
        }
    }

    private void loadDashboard() {
        long userId = session.getUserId();
        String today = DateUtil.today();
        AppExecutors.io().execute(() -> {
            List<Habit> all = habitDao.getActiveHabits(userId);
            List<Habit> todayHabits = new ArrayList<>();
            Set<Long> doneToday = new HashSet<>();

            // Préparer les 7 derniers jours
            String[] last7Dates = new String[7];
            String[] last7Codes = new String[7];
            for (int d = 6; d >= 0; d--) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -d);
                last7Dates[6 - d] = DateUtil.dayOffset(-d);
                last7Codes[6 - d] = toDayCode(cal.get(Calendar.DAY_OF_WEEK));
            }

            int maxStreak = 0, totalSched7 = 0, totalDone7 = 0;

            for (Habit h : all) {
                List<HabitLog> logs = logDao.getLogsForHabit(h.id);
                Set<String> doneDates = new HashSet<>();
                for (HabitLog log : logs) if (log.done) doneDates.add(log.date);

                // Max streak sur TOUTES les habitudes
                maxStreak = Math.max(maxStreak, computeStreak(doneDates, today));

                // Moyenne hebdomadaire sur 7 jours
                for (int i = 0; i < 7; i++) {
                    if (HabitSchedule.isScheduledOn(h, last7Codes[i])) {
                        totalSched7++;
                        if (doneDates.contains(last7Dates[i])) totalDone7++;
                    }
                }

                // Habitudes d'aujourd'hui
                if (HabitSchedule.isScheduledToday(h)) {
                    todayHabits.add(h);
                    if (doneDates.contains(today)) doneToday.add(h.id);
                }
            }

            int total = todayHabits.size();
            int done  = doneToday.size();
            int pct   = total == 0 ? 0 : Math.round(done * 100f / total);
            int weeklyAvg = totalSched7 == 0 ? 0 : Math.round(totalDone7 * 100f / totalSched7);
            final int maxStreakF  = maxStreak;
            final int weeklyAvgF = weeklyAvg;

            AppExecutors.main().execute(() -> {
                adapter.setData(todayHabits, doneToday);
                tvTodayEmpty.setVisibility(todayHabits.isEmpty() ? View.VISIBLE : View.GONE);
                tvCompletionPercent.setText(pct + "% Completed");
                progressMomentum.setProgress(pct);
                tvDashboardStreak.setText(maxStreakF + " Day Streak");
                if (tvLongestStreak != null) tvLongestStreak.setText(String.valueOf(maxStreakF));
                if (tvWeeklyAvg != null)     tvWeeklyAvg.setText(weeklyAvgF + "%");
                if (total == 0) {
                    tvMomentumSubtitle.setText("No habits yet. Add one to get started!");
                } else {
                    tvMomentumSubtitle.setText(done + " of " + total + " habits completed today.");
                }
            });
        });
    }

    private static String toDayCode(int dow) {
        switch (dow) {
            case Calendar.MONDAY:    return "MON";
            case Calendar.TUESDAY:   return "TUE";
            case Calendar.WEDNESDAY: return "WED";
            case Calendar.THURSDAY:  return "THU";
            case Calendar.FRIDAY:    return "FRI";
            case Calendar.SATURDAY:  return "SAT";
            default:                 return "SUN";
        }
    }

    private int computeStreak(Set<String> doneDates, String today) {
        int streak = 0;
        int offset = 0;
        if (!doneDates.contains(today)) offset = -1;
        while (doneDates.contains(DateUtil.dayOffset(offset))) {
            streak++;
            offset--;
        }
        return streak;
    }

    @Override
    public void onHabitClick(Habit habit) {
        Intent intent = new Intent(this, HabitDetailActivity.class);
        intent.putExtra(HabitDetailActivity.EXTRA_HABIT_ID, habit.id);
        startActivity(intent);
    }

    @Override
    public void onToggleDone(Habit habit, boolean newDone) {
        String today = DateUtil.today();
        long userId = session.getUserId();
        AppExecutors.io().execute(() -> {
            logDao.upsert(new HabitLog(habit.id, today, newDone));
            BadgeEvaluator.evaluate(getApplicationContext(), userId);
            AppExecutors.main().execute(this::loadDashboard);
        });
    }

    /**
     * Kanت'akkدو l'utilisateur dyal session mazal kayn f DB.
     * Ila tmse7 (mثلا mn ba3d upgrade dyal DB li drop l'tables) → clear session + Login.
     */
    private void verifyUserExists() {
        long userId = session.getUserId();
        UserDao userDao = new UserDao(this);
        AppExecutors.io().execute(() -> {
            boolean exists = userDao.findById(userId) != null;
            if (!exists) {
                AppExecutors.main().execute(() -> {
                    session.logout();
                    goToLogin();
                });
            }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupNavigation() {
        FloatingActionButton fab = findViewById(R.id.fab_add_habit);
        if (fab != null) {
            fab.setOnClickListener(v ->
                    startActivity(new Intent(this, AddHabitActivity.class)));
        }

        TextView viewAll = findViewById(R.id.tv_view_all);
        if (viewAll != null) {
            viewAll.setOnClickListener(v ->
                    startActivity(new Intent(this, HabitListActivity.class)));
        }

        ImageView profileImage = findViewById(R.id.profile_image);
        if (profileImage != null) {
            AvatarUtil.setInitial(profileImage, session.getName());
            profileImage.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class)));
        }

        View bell = findViewById(R.id.btn_notifications);
        if (bell != null) {
            bell.setOnClickListener(v ->
                    startActivity(new Intent(this, SettingsActivity.class)));
        }

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        com.example.myapplication.util.NavHelper.setup(this, nav, R.id.navigation_home);
    }

}
