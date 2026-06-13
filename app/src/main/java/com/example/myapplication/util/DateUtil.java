package com.example.myapplication.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Helpers dyal dates b format "yyyy-MM-dd" (nafs li kayttkhzen f habit_logs).
 */
public final class DateUtil {

    private static final String FMT = "yyyy-MM-dd";

    private DateUtil() {}

    private static SimpleDateFormat formatter() {
        return new SimpleDateFormat(FMT, Locale.US);
    }

    /** Nhar dyal daba "yyyy-MM-dd". */
    public static String today() {
        return formatter().format(Calendar.getInstance().getTime());
    }

    /** Nhar b offset dyal ayyam (-1 = lbare7, -7 = simana). */
    public static String dayOffset(int daysFromToday) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, daysFromToday);
        return formatter().format(cal.getTime());
    }

    /** Code dyal nhar dyal l'youm: "MON".."SUN" (nafs format dyal specificDays). */
    public static String todayDayCode() {
        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
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
}
