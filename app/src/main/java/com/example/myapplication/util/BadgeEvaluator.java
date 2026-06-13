package com.example.myapplication.util;

import android.content.Context;

import com.example.myapplication.dao.BadgeDao;
import com.example.myapplication.dao.HabitDao;
import com.example.myapplication.dao.HabitLogDao;
import com.example.myapplication.models.Badge;
import com.example.myapplication.models.Habit;
import com.example.myapplication.models.HabitLog;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Kayقيّم les stats dyal l'utilisateur w kayفك (unlock) badges li wsel l'criteria dyalhom.
 * Smiyat dyal badges khass yt-match m3a li seedنا f DbHelper.
 */
public final class BadgeEvaluator {

    private BadgeEvaluator() {}

    /** Kayكمّل f background thread; ma kayrje3 walou. */
    public static void evaluateAsync(Context context, long userId) {
        Context appCtx = context.getApplicationContext();
        AppExecutors.io().execute(() -> evaluate(appCtx, userId));
    }

    /** Khass yt3ayet f background thread (kayمس DB). */
    public static void evaluate(Context context, long userId) {
        HabitDao habitDao = new HabitDao(context);
        HabitLogDao logDao = new HabitLogDao(context);
        BadgeDao badgeDao = new BadgeDao(context);

        List<Habit> habits = habitDao.getActiveHabits(userId);
        int habitCount = habits.size();

        int totalCompletions = 0;
        int maxStreak = 0;
        for (Habit h : habits) {
            totalCompletions += logDao.countDone(h.id);

            Set<String> doneDates = new HashSet<>();
            for (HabitLog log : logDao.getLogsForHabit(h.id)) {
                if (log.done) doneDates.add(log.date);
            }
            maxStreak = Math.max(maxStreak, computeStreak(doneDates));
        }

        for (Badge badge : badgeDao.getAll()) {
            if (badge.unlocked) continue;
            if (meetsCriteria(badge.name, habitCount, totalCompletions, maxStreak)) {
                badgeDao.unlock(badge.id);
            }
        }
    }

    private static boolean meetsCriteria(String name, int habitCount,
                                         int totalCompletions, int maxStreak) {
        if (name == null) return false;
        switch (name) {
            // Catégorie A — Habitudes créées
            case "Recrue":      return habitCount >= 1;
            case "Stratège":    return habitCount >= 5;
            case "Architecte":  return habitCount >= 10;
            // Catégorie B — Completions totales
            case "Soldat":      return totalCompletions >= 1;
            case "Caporal":     return totalCompletions >= 10;
            case "Sergent":     return totalCompletions >= 25;
            case "Commandant":  return totalCompletions >= 50;
            case "Colonel":     return totalCompletions >= 100;
            case "Maréchal":    return totalCompletions >= 250;
            case "Légende":     return totalCompletions >= 500;
            // Catégorie C — Streak maximum
            case "Lieutenant":  return maxStreak >= 7;
            case "Général":     return maxStreak >= 30;
            case "Légionnaire": return maxStreak >= 365;
            default:            return false;
        }
    }

    /** Streak = ayyam mتتالية done 7tal l'youm (wla mn lbare7 ila l'youm mazal). */
    private static int computeStreak(Set<String> doneDates) {
        int streak = 0;
        int offset = 0;
        if (!doneDates.contains(DateUtil.today())) {
            offset = -1;
        }
        while (doneDates.contains(DateUtil.dayOffset(offset))) {
            streak++;
            offset--;
        }
        return streak;
    }
}
