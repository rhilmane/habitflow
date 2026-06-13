package com.example.myapplication.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.myapplication.dao.HabitDao;
import com.example.myapplication.models.Habit;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
            if (!prefs.getBoolean("reminders_enabled", true)) {
                return;
            }

            SessionManager session = new SessionManager(context);
            if (!session.isLoggedIn()) return;
            long userId = session.getUserId();

            HabitDao habitDao = new HabitDao(context);
            AppExecutors.io().execute(() -> {
                List<Habit> habits = habitDao.getActiveHabits(userId);
                for (Habit h : habits) {
                    if (h.reminderEnabled) {
                        ReminderScheduler.schedule(context, h.id, h.name, h.reminderTime, h.specificDays);
                    }
                }
            });
        }
    }
}
