package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.util.HabitAdapter;
import com.example.myapplication.dao.HabitDao;
import com.example.myapplication.dao.HabitLogDao;
import com.example.myapplication.models.Habit;
import com.example.myapplication.models.HabitLog;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.AvatarUtil;
import com.example.myapplication.util.BadgeEvaluator;
import com.example.myapplication.util.DateUtil;
import com.example.myapplication.util.HabitSchedule;
import com.example.myapplication.util.NavHelper;
import com.example.myapplication.util.SessionManager;

import android.text.Editable;
import android.text.TextWatcher;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HabitListActivity extends AppCompatActivity implements HabitAdapter.OnHabitInteraction {

    private HabitDao habitDao;
    private HabitLogDao logDao;
    private com.example.myapplication.dao.MicroActionDao microActionDao;
    private SessionManager session;
    private HabitAdapter adapter;
    private String currentSearchQuery = "";
    private String currentCategoryFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_list);
        habitDao = new HabitDao(this);
        logDao = new HabitLogDao(this);
        microActionDao = new com.example.myapplication.dao.MicroActionDao(this);
        session = new SessionManager(this);

        RecyclerView rv = findViewById(R.id.rv_habits);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HabitAdapter(this);
        rv.setAdapter(adapter);

        findViewById(R.id.btn_add_habit_bottom).setOnClickListener(v ->
                startActivity(new Intent(this, AddHabitActivity.class)));

        android.view.View bell = findViewById(R.id.btn_notifications);
        if (bell != null) {
            bell.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        }

        android.widget.ImageView avatar = findViewById(R.id.profile_avatar);
        if (avatar != null) {
            AvatarUtil.setInitial(avatar, session.getName());
            avatar.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }

        TextInputEditText etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                loadHabits();
            }
        });

        ChipGroup chipGroupFilter = findViewById(R.id.chip_group_filter);
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentCategoryFilter = "All";
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chip_filter_health) currentCategoryFilter = "Health & Fitness";
                else if (id == R.id.chip_filter_work) currentCategoryFilter = "Work & Focus";
                else currentCategoryFilter = "All"; // chip_filter_all ou autre
            }
            loadHabits();
        });

        com.google.android.material.bottomnavigation.BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        if (nav != null) com.example.myapplication.util.NavHelper.setup(this, nav, R.id.navigation_home);
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavHelper.resume(this, findViewById(R.id.bottom_navigation));
        loadHabits();
    }

    private void loadHabits() {
        long userId = session.getUserId();
        String today = DateUtil.today();
        AppExecutors.io().execute(() -> {
            List<Habit> all = habitDao.getActiveHabits(userId);
            List<Habit> habits = new ArrayList<>();
            Set<Long> doneToday = new HashSet<>();
            java.util.Map<Long, String> microProgress = new java.util.HashMap<>();
            for (Habit h : all) {
                if (!currentSearchQuery.isEmpty() && !h.name.toLowerCase().contains(currentSearchQuery)) {
                    continue;
                }
                if (!"All".equals(currentCategoryFilter) && !currentCategoryFilter.equals(h.category)) {
                    continue;
                }

                habits.add(h);
                HabitLog log = logDao.getLog(h.id, today);
                if (log != null && log.done) {
                    doneToday.add(h.id);
                }

                List<com.example.myapplication.models.MicroAction> micros = microActionDao.getForHabit(h.id);
                if (!micros.isEmpty()) {
                    int done = 0;
                    for (com.example.myapplication.models.MicroAction m : micros) {
                        if (m.isDoneOn(today)) done++;
                    }
                    microProgress.put(h.id, done + "/" + micros.size());
                }
            }
            AppExecutors.main().execute(() -> {
                adapter.setData(habits, doneToday);
                adapter.setMicroProgress(microProgress);
            });
        });
    }

    @Override
    public void onHabitClick(Habit habit) {
        Intent intent = new Intent(this, HabitDetailActivity.class);
        intent.putExtra("habit_id", habit.id);
        startActivity(intent);
    }

    @Override
    public void onToggleDone(Habit habit, boolean newDone) {
        String today = DateUtil.today();
        long userId = session.getUserId();
        AppExecutors.io().execute(() -> {
            logDao.upsert(new HabitLog(habit.id, today, newDone));
            // 3awd qiyem badges mn ba3d kola toggle
            BadgeEvaluator.evaluate(getApplicationContext(), userId);
        });
    }
}
