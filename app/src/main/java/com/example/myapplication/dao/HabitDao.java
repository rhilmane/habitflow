package com.example.myapplication.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.database.DbContract;
import com.example.myapplication.database.DbContract.Habits;
import com.example.myapplication.database.DbHelper;
import com.example.myapplication.models.Frequency;
import com.example.myapplication.models.Habit;

import java.util.ArrayList;
import java.util.List;

public class HabitDao {

    private final DbHelper helper;

    public HabitDao(Context context) {
        this.helper = DbHelper.getInstance(context);
    }

    public long insert(Habit h) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.insert(Habits.TABLE, null, toValues(h));
    }

    public int update(Habit h) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.update(Habits.TABLE, toValues(h), Habits.ID + " = ?",
                new String[]{String.valueOf(h.id)});
    }

    public int setArchived(long habitId, boolean archived) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(Habits.ARCHIVED, archived ? 1 : 0);
        v.put(DbContract.IS_SYNCED, 0);
        return db.update(Habits.TABLE, v, Habits.ID + " = ?",
                new String[]{String.valueOf(habitId)});
    }

    /** true ila kayna habit b nfs l-ism l-user, machi had l-habit nfso (ila edit) */
    public boolean existsByName(long userId, String name, long excludeHabitId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(Habits.TABLE,
                new String[]{"COUNT(*)"},
                Habits.USER_ID + " = ? AND LOWER(" + Habits.NAME + ") = LOWER(?) AND " + Habits.ID + " != ?",
                new String[]{String.valueOf(userId), name, String.valueOf(excludeHabitId)},
                null, null, null)) {
            return c.moveToFirst() && c.getInt(0) > 0;
        }
    }

    public int delete(long habitId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(Habits.TABLE, Habits.ID + " = ?",
                new String[]{String.valueOf(habitId)});
    }

    public List<Habit> getActiveHabits(long userId) {
        return query(Habits.USER_ID + " = ? AND " + Habits.ARCHIVED + " = 0",
                new String[]{String.valueOf(userId)},
                Habits.CREATED_AT + " DESC");
    }

    public List<Habit> getArchived(long userId) {
        return query(Habits.USER_ID + " = ? AND " + Habits.ARCHIVED + " = 1",
                new String[]{String.valueOf(userId)},
                Habits.CREATED_AT + " DESC");
    }

    public Habit findById(long id) {
        List<Habit> list = query(Habits.ID + " = ?",
                new String[]{String.valueOf(id)}, null);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<Habit> query(String where, String[] args, String orderBy) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Habit> result = new ArrayList<>();
        try (Cursor c = db.query(Habits.TABLE, null, where, args, null, null, orderBy)) {
            while (c.moveToNext()) {
                result.add(map(c));
            }
        }
        return result;
    }

    private ContentValues toValues(Habit h) {
        ContentValues v = new ContentValues();
        v.put(Habits.USER_ID, h.userId);
        v.put(Habits.NAME, h.name);
        v.put(Habits.ICON_RES_ID, h.iconResId);
        v.put(Habits.COLOR_TAG, h.colorTag);
        v.put(Habits.FREQUENCY, h.frequency == null ? Frequency.DAILY.name() : h.frequency.name());
        v.put(Habits.SPECIFIC_DAYS, h.specificDays);
        v.put(Habits.CATEGORY, h.category);
        v.put(Habits.REMINDER_ENABLED, h.reminderEnabled ? 1 : 0);
        v.put(Habits.REMINDER_TIME, h.reminderTime);
        v.put(Habits.ARCHIVED, h.archived ? 1 : 0);
        v.put(Habits.CREATED_AT, h.createdAt);
        v.put(DbContract.IS_SYNCED, 0);
        return v;
    }

    private Habit map(Cursor c) {
        Habit h = new Habit();
        h.id = c.getLong(c.getColumnIndexOrThrow(Habits.ID));
        h.userId = c.getLong(c.getColumnIndexOrThrow(Habits.USER_ID));
        h.name = c.getString(c.getColumnIndexOrThrow(Habits.NAME));
        h.iconResId = c.getInt(c.getColumnIndexOrThrow(Habits.ICON_RES_ID));
        h.colorTag = c.getInt(c.getColumnIndexOrThrow(Habits.COLOR_TAG));
        h.frequency = Frequency.valueOf(c.getString(c.getColumnIndexOrThrow(Habits.FREQUENCY)));
        h.specificDays = c.getString(c.getColumnIndexOrThrow(Habits.SPECIFIC_DAYS));
        h.category = c.getString(c.getColumnIndexOrThrow(Habits.CATEGORY));
        h.reminderEnabled = c.getInt(c.getColumnIndexOrThrow(Habits.REMINDER_ENABLED)) == 1;
        h.reminderTime = c.getString(c.getColumnIndexOrThrow(Habits.REMINDER_TIME));
        h.archived = c.getInt(c.getColumnIndexOrThrow(Habits.ARCHIVED)) == 1;
        h.createdAt = c.getLong(c.getColumnIndexOrThrow(Habits.CREATED_AT));
        return h;
    }
}
