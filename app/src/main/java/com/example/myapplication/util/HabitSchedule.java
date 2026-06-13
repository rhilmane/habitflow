package com.example.myapplication.util;

import android.text.TextUtils;

import com.example.myapplication.models.Frequency;
import com.example.myapplication.models.Habit;

/**
 * Kayقرّر wach 3ada khassها tban f nhar mu3ayan 7sab frequency dyalها.
 */
public final class HabitSchedule {

    private HabitSchedule() {}

    /** Wach l'3ada programmée l'youm. */
    public static boolean isScheduledToday(Habit habit) {
        return isScheduledOn(habit, DateUtil.todayDayCode());
    }

    /** Wach l'3ada programmée f nhar (dayCode = "MON".."SUN"). */
    public static boolean isScheduledOn(Habit habit, String dayCode) {
        if (habit.frequency != Frequency.SPECIFIC_DAYS) {
            return true; // DAILY → kol nhar
        }
        if (TextUtils.isEmpty(habit.specificDays)) {
            return true; // ma 7ddedch ayyam → kol nhar (fallback)
        }
        for (String d : habit.specificDays.split(",")) {
            if (d.trim().equals(dayCode)) return true;
        }
        return false;
    }
}
