package com.example.myapplication.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_HABIT_ID = "habit_id";
    public static final String EXTRA_HABIT_NAME = "habit_name";
    public static final String EXTRA_DAYS = "days";

    @Override
    public void onReceive(Context context, Intent intent) {
        long habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1);
        String habitName = intent.getStringExtra(EXTRA_HABIT_NAME);
        if (habitName == null) habitName = "your habit";

        String days = intent.getStringExtra(EXTRA_DAYS);
        if (!TextUtils.isEmpty(days) && !isTodayIn(days)) {
            return;
        }

        NotificationHelper.showReminder(context, habitId, habitName);
    }

    private boolean isTodayIn(String days) {
        String today = DateUtil.todayDayCode();
        for (String d : days.split(",")) {
            if (d.trim().equals(today)) return true;
        }
        return false;
    }
}
