package com.example.myapplication.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.database.DbContract;
import com.example.myapplication.database.DbContract.Users;
import com.example.myapplication.database.DbHelper;
import com.example.myapplication.models.User;

public class UserDao {

    private final DbHelper helper;

    public UserDao(Context context) {
        this.helper = DbHelper.getInstance(context);
    }

    public long insert(User user) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(Users.NAME, user.name);
        v.put(Users.EMAIL, user.email);
        v.put(Users.PASSWORD_HASH, user.passwordHash);
        v.put(Users.AVATAR_URL, user.avatarUrl);
        v.put(Users.CREATED_AT, user.createdAt);
        v.put(Users.SECURITY_QUESTION, user.securityQuestion);
        v.put(Users.SECURITY_ANSWER, user.securityAnswer);
        v.put(DbContract.IS_SYNCED, 0);
        return db.insert(Users.TABLE, null, v);
    }

    /** Insert avec un ID précis (celui généré par Supabase). */
    public long insertWithId(User user) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(Users.ID, user.id);
        v.put(Users.NAME, user.name);
        v.put(Users.EMAIL, user.email);
        v.put(Users.PASSWORD_HASH, user.passwordHash);
        v.put(Users.AVATAR_URL, user.avatarUrl);
        v.put(Users.CREATED_AT, user.createdAt);
        v.put(Users.SECURITY_QUESTION, user.securityQuestion);
        v.put(Users.SECURITY_ANSWER, user.securityAnswer);
        v.put(DbContract.IS_SYNCED, 1); // already in Supabase
        return db.insertWithOnConflict(Users.TABLE, null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int update(User user) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(Users.NAME, user.name);
        v.put(Users.EMAIL, user.email);
        v.put(Users.PASSWORD_HASH, user.passwordHash);
        v.put(Users.AVATAR_URL, user.avatarUrl);
        v.put(Users.SECURITY_QUESTION, user.securityQuestion);
        v.put(Users.SECURITY_ANSWER, user.securityAnswer);
        v.put(DbContract.IS_SYNCED, 0);
        return db.update(Users.TABLE, v, Users.ID + " = ?",
                new String[]{String.valueOf(user.id)});
    }

    public User findByEmail(String email) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(Users.TABLE, null,
                Users.EMAIL + " = ?", new String[]{email},
                null, null, null, "1")) {
            return c.moveToFirst() ? map(c) : null;
        }
    }

    public User login(String email, String passwordHash) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(Users.TABLE, null,
                Users.EMAIL + " = ? AND " + Users.PASSWORD_HASH + " = ?",
                new String[]{email, passwordHash},
                null, null, null, "1")) {
            return c.moveToFirst() ? map(c) : null;
        }
    }

    public User findById(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(Users.TABLE, null,
                Users.ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null, "1")) {
            return c.moveToFirst() ? map(c) : null;
        }
    }

    private User map(Cursor c) {
        User u = new User();
        u.id           = c.getLong(c.getColumnIndexOrThrow(Users.ID));
        u.name         = c.getString(c.getColumnIndexOrThrow(Users.NAME));
        u.email        = c.getString(c.getColumnIndexOrThrow(Users.EMAIL));
        u.passwordHash = c.getString(c.getColumnIndexOrThrow(Users.PASSWORD_HASH));
        u.avatarUrl    = c.getString(c.getColumnIndexOrThrow(Users.AVATAR_URL));
        u.createdAt    = c.getLong(c.getColumnIndexOrThrow(Users.CREATED_AT));
        int sqIdx = c.getColumnIndex(Users.SECURITY_QUESTION);
        int saIdx = c.getColumnIndex(Users.SECURITY_ANSWER);
        if (sqIdx != -1) u.securityQuestion = c.getString(sqIdx);
        if (saIdx != -1) u.securityAnswer   = c.getString(saIdx);
        return u;
    }
}
