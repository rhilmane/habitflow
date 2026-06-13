package com.example.myapplication.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.util.ArchiveAdapter;
import com.example.myapplication.dao.HabitDao;
import com.example.myapplication.models.Habit;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.NavHelper;
import com.example.myapplication.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class ArchivesActivity extends AppCompatActivity implements ArchiveAdapter.OnRestore, ArchiveAdapter.OnDelete {

    private HabitDao habitDao;
    private SessionManager session;
    private ArchiveAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archives);
        habitDao = new HabitDao(this);
        session = new SessionManager(this);

        tvEmpty = findViewById(R.id.tv_archives_empty);

        RecyclerView rv = findViewById(R.id.rv_archives);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ArchiveAdapter(this);
        adapter.setOnDeleteListener(this);
        rv.setAdapter(adapter);

        findViewById(R.id.btn_back_archives).setOnClickListener(v -> finish());

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        NavHelper.setup(this, nav, R.id.navigation_profile);

        loadArchived();
    }

    private void loadArchived() {
        long userId = session.getUserId();
        AppExecutors.io().execute(() -> {
            List<Habit> archived = habitDao.getArchived(userId);
            AppExecutors.main().execute(() -> {
                adapter.setData(archived);
                tvEmpty.setVisibility(archived.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    @Override
    public void onRestore(Habit habit) {
        adapter.remove(habit);
        AppExecutors.io().execute(() -> {
            habitDao.setArchived(habit.id, false);
            AppExecutors.main().execute(() -> {
                Toast.makeText(this, habit.name + " restored", Toast.LENGTH_SHORT).show();
                if (adapter.getItemCount() == 0) {
                    tvEmpty.setVisibility(View.VISIBLE);
                }
                
                android.content.SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
                if (prefs.getBoolean("reminders_enabled", true) && habit.reminderEnabled) {
                    com.example.myapplication.util.ReminderScheduler.schedule(this, habit.id, habit.name, habit.reminderTime, habit.specificDays);
                }
            });
        });
    }

    @Override
    public void onDelete(Habit habit) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Supprimer définitivement ?")
                .setMessage("\"" + habit.name + "\" sera supprimé avec tout son historique. Cette action est irréversible.")
                .setPositiveButton("Supprimer", (d, w) -> {
                    adapter.remove(habit);
                    AppExecutors.io().execute(() -> {
                        habitDao.delete(habit.id);
                        AppExecutors.main().execute(() -> {
                            Toast.makeText(this, habit.name + " supprimé", Toast.LENGTH_SHORT).show();
                            if (adapter.getItemCount() == 0) {
                                tvEmpty.setVisibility(View.VISIBLE);
                            }
                        });
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavHelper.resume(this, findViewById(R.id.bottom_navigation));
    }
}
