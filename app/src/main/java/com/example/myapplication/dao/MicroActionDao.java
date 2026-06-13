package com.example.myapplication.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.database.DbContract;
import com.example.myapplication.database.DbContract.MicroActions;
import com.example.myapplication.database.DbHelper;
import com.example.myapplication.models.MicroAction;

import java.util.ArrayList;
import java.util.List;

public class MicroActionDao {

    private final DbHelper helper;

    public MicroActionDao(Context context) {
        this.helper = DbHelper.getInstance(context);
    }

    public long insert(MicroAction m) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(MicroActions.HABIT_ID, m.habitId);
        v.put(MicroActions.TEXT, m.text);
        v.put(MicroActions.POSITION, m.position);
        v.put(MicroActions.LAST_DONE_DATE, m.lastDoneDate);
        v.put(DbContract.IS_SYNCED, 0);
        return db.insert(MicroActions.TABLE, null, v);
    }

    public List<MicroAction> getForHabit(long habitId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<MicroAction> result = new ArrayList<>();
        try (Cursor c = db.query(MicroActions.TABLE, null,
                MicroActions.HABIT_ID + " = ?", new String[]{String.valueOf(habitId)},
                null, null, MicroActions.POSITION + " ASC")) {
            while (c.moveToNext()) {
                result.add(map(c));
            }
        }
        return result;
    }

    /** Kayسجّل nhar dyal completion (date) wla kaymse7 (null) ila tfekkat. */
    public int setDoneDate(long id, String date) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(MicroActions.LAST_DONE_DATE, date);
        return db.update(MicroActions.TABLE, v, MicroActions.ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    public int delete(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(MicroActions.TABLE, MicroActions.ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    public int deleteForHabit(long habitId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(MicroActions.TABLE, MicroActions.HABIT_ID + " = ?",
                new String[]{String.valueOf(habitId)});
    }

    private MicroAction map(Cursor c) {
        MicroAction m = new MicroAction();
        m.id = c.getLong(c.getColumnIndexOrThrow(MicroActions.ID));
        m.habitId = c.getLong(c.getColumnIndexOrThrow(MicroActions.HABIT_ID));
        m.text = c.getString(c.getColumnIndexOrThrow(MicroActions.TEXT));
        m.position = c.getInt(c.getColumnIndexOrThrow(MicroActions.POSITION));
        m.lastDoneDate = c.getString(c.getColumnIndexOrThrow(MicroActions.LAST_DONE_DATE));
        return m;
    }
}
