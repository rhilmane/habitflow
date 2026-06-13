package com.example.myapplication.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.database.DbContract;
import com.example.myapplication.database.DbContract.HabitLogs;
import com.example.myapplication.database.DbHelper;
import com.example.myapplication.models.HabitLog;

import java.util.ArrayList;
import java.util.List;

public class HabitLogDao {

    private final DbHelper helper;

    public HabitLogDao(Context context) {
        this.helper = DbHelper.getInstance(context);
    }

    /** Insert wla update ila deja kayna l blasa dyal had nhar (UNIQUE habit_id+date). */
    public long upsert(HabitLog log) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(HabitLogs.HABIT_ID, log.habitId);
        v.put(HabitLogs.DATE, log.date);
        v.put(HabitLogs.DONE, log.done ? 1 : 0);
        v.put(DbContract.IS_SYNCED, 0);
        return db.insertWithOnConflict(HabitLogs.TABLE, null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<HabitLog> getLogsForHabit(long habitId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<HabitLog> result = new ArrayList<>();
        try (Cursor c = db.query(HabitLogs.TABLE, null,
                HabitLogs.HABIT_ID + " = ?", new String[]{String.valueOf(habitId)},
                null, null, HabitLogs.DATE + " DESC")) {
            while (c.moveToNext()) {
                result.add(map(c));
            }
        }
        return result;
    }

    public HabitLog getLog(long habitId, String date) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(HabitLogs.TABLE, null,
                HabitLogs.HABIT_ID + " = ? AND " + HabitLogs.DATE + " = ?",
                new String[]{String.valueOf(habitId), date},
                null, null, null, "1")) {
            return c.moveToFirst() ? map(c) : null;
        }
    }

    /** 3adad lihaml dyal nhar li t9ada (l statistics / streak). */
    public int countDone(long habitId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + HabitLogs.TABLE +
                        " WHERE " + HabitLogs.HABIT_ID + " = ? AND " + HabitLogs.DONE + " = 1",
                new String[]{String.valueOf(habitId)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private HabitLog map(Cursor c) {
        HabitLog log = new HabitLog();
        log.id = c.getLong(c.getColumnIndexOrThrow(HabitLogs.ID));
        log.habitId = c.getLong(c.getColumnIndexOrThrow(HabitLogs.HABIT_ID));
        log.date = c.getString(c.getColumnIndexOrThrow(HabitLogs.DATE));
        log.done = c.getInt(c.getColumnIndexOrThrow(HabitLogs.DONE)) == 1;
        return log;
    }
}
