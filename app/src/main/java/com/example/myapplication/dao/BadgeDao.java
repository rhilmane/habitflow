package com.example.myapplication.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.database.DbContract;
import com.example.myapplication.database.DbContract.Badges;
import com.example.myapplication.database.DbHelper;
import com.example.myapplication.models.Badge;

import java.util.ArrayList;
import java.util.List;

public class BadgeDao {

    private final DbHelper helper;

    public BadgeDao(Context context) {
        this.helper = DbHelper.getInstance(context);
    }

    public long insert(Badge b) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.insert(Badges.TABLE, null, toValues(b));
    }

    public int update(Badge b) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.update(Badges.TABLE, toValues(b), Badges.ID + " = ?",
                new String[]{String.valueOf(b.id)});
    }

    public int unlock(long badgeId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(Badges.UNLOCKED, 1);
        v.put(Badges.UNLOCKED_AT, System.currentTimeMillis());
        v.put(DbContract.IS_SYNCED, 0);
        return db.update(Badges.TABLE, v, Badges.ID + " = ?",
                new String[]{String.valueOf(badgeId)});
    }

    public List<Badge> getAll() {
        return query(null, null);
    }

    public List<Badge> getUnlocked() {
        return query(Badges.UNLOCKED + " = 1", null);
    }

    private List<Badge> query(String where, String[] args) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Badge> result = new ArrayList<>();
        try (Cursor c = db.query(Badges.TABLE, null, where, args, null, null, null)) {
            while (c.moveToNext()) {
                result.add(map(c));
            }
        }
        return result;
    }

    private ContentValues toValues(Badge b) {
        ContentValues v = new ContentValues();
        v.put(Badges.NAME, b.name);
        v.put(Badges.DESCRIPTION, b.description);
        v.put(Badges.ICON_RES_ID, b.iconResId);
        v.put(Badges.UNLOCKED, b.unlocked ? 1 : 0);
        v.put(Badges.UNLOCKED_AT, b.unlockedAt);
        return v;
    }

    private Badge map(Cursor c) {
        Badge b = new Badge();
        b.id = c.getLong(c.getColumnIndexOrThrow(Badges.ID));
        b.name = c.getString(c.getColumnIndexOrThrow(Badges.NAME));
        b.description = c.getString(c.getColumnIndexOrThrow(Badges.DESCRIPTION));
        b.iconResId = c.getInt(c.getColumnIndexOrThrow(Badges.ICON_RES_ID));
        b.unlocked = c.getInt(c.getColumnIndexOrThrow(Badges.UNLOCKED)) == 1;
        b.unlockedAt = c.getLong(c.getColumnIndexOrThrow(Badges.UNLOCKED_AT));
        return b;
    }
}
