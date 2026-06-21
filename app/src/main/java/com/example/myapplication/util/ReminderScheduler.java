package com.example.myapplication.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.myapplication.util.ReminderReceiver;

import java.util.Calendar;

/**
 * Kay-schedula / kay-cancel reminders journaliers b AlarmManager.
 * Kanst3mlo setInexactRepeating bach ma n7tajoch permission dyal exact alarms.
 */
public final class ReminderScheduler {

    private ReminderScheduler() {}

    /**
     * @param time         format "HH:mm" (24h). Ila null wla khata → 08:00.
     * @param specificDays "MON,TUE,..." ila l'3ada specific days, wla null l DAILY.
     *                     L'alarm kayt9ad kol nhar, walakin l'receiver kayفلتر 3la ayyam.
     */
    public static void schedule(Context context, long habitId, String habitName,
                                String time, String specificDays) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        int[] hm = parseTime(time);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hm[0]);
        cal.set(Calendar.MINUTE, hm[1]);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // Ila l'wa9t deja fat l'youm → bda mn ghedda
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        am.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                buildPendingIntent(context, habitId, habitName, specificDays));
    }

    public static void cancel(Context context, long habitId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        // Extras machi parti men equality dyal PendingIntent → cancel kaymatch 3la requestCode
        PendingIntent pi = buildPendingIntent(context, habitId, null, null);
        am.cancel(pi);
        pi.cancel();
    }

    private static PendingIntent buildPendingIntent(Context context, long habitId,
                                                    String habitName, String specificDays) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId);
        if (habitName != null) {
            intent.putExtra(ReminderReceiver.EXTRA_HABIT_NAME, habitName);
        }
        if (specificDays != null) {
            intent.putExtra(ReminderReceiver.EXTRA_DAYS, specificDays);
        }
        return PendingIntent.getBroadcast(
                context, (int) habitId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Kayfse9 "HH:mm" → [hour, minute]. Default 08:00. */
    private static int[] parseTime(String time) {
        int hour = 8, minute = 0;
        if (time != null && time.contains(":")) {
            try {
                String[] parts = time.trim().split(":");
                hour = Integer.parseInt(parts[0].trim());
                minute = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
                hour = 8;
                minute = 0;
            }
        }
        return new int[]{hour, minute};
    }
}
