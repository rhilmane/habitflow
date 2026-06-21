package com.example.myapplication.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import com.example.myapplication.R;
import com.example.myapplication.database.DbContract.Badges;
import com.example.myapplication.database.DbContract.HabitLogs;
import com.example.myapplication.database.DbContract.Habits;
import com.example.myapplication.database.DbContract.MicroActions;
import com.example.myapplication.database.DbContract.Users;

public class DbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "habits.db";
    public static final int DB_VERSION = 9;

    private static DbHelper instance;

    private DbHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    public static synchronized DbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DbHelper(context);
        }
        return instance;
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Users.TABLE + " (" +
                Users.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                Users.NAME + " TEXT, " +
                Users.EMAIL + " TEXT UNIQUE NOT NULL, " +
                Users.PASSWORD_HASH + " TEXT NOT NULL, " +
                Users.AVATAR_URL + " TEXT, " +
                Users.CREATED_AT + " INTEGER, " +
                Users.SECURITY_QUESTION + " TEXT, " +
                Users.SECURITY_ANSWER + " TEXT, " +
                DbContract.IS_SYNCED + " INTEGER DEFAULT 0" +
                ")");

        db.execSQL("CREATE TABLE " + Habits.TABLE + " (" +
                Habits.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                Habits.USER_ID + " INTEGER NOT NULL, " +
                Habits.NAME + " TEXT NOT NULL, " +
                Habits.ICON_RES_ID + " INTEGER DEFAULT 0, " +
                Habits.COLOR_TAG + " INTEGER DEFAULT 0, " +
                Habits.FREQUENCY + " TEXT DEFAULT 'DAILY', " +
                Habits.SPECIFIC_DAYS + " TEXT, " +
                Habits.CATEGORY + " TEXT, " +
                Habits.REMINDER_ENABLED + " INTEGER DEFAULT 0, " +
                Habits.REMINDER_TIME + " TEXT, " +
                Habits.ARCHIVED + " INTEGER DEFAULT 0, " +
                Habits.CREATED_AT + " INTEGER, " +
                DbContract.IS_SYNCED + " INTEGER DEFAULT 0, " +
                "FOREIGN KEY(" + Habits.USER_ID + ") REFERENCES " +
                Users.TABLE + "(" + Users.ID + ") ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE TABLE " + HabitLogs.TABLE + " (" +
                HabitLogs.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                HabitLogs.HABIT_ID + " INTEGER NOT NULL, " +
                HabitLogs.DATE + " TEXT NOT NULL, " +
                HabitLogs.DONE + " INTEGER DEFAULT 0, " +
                DbContract.IS_SYNCED + " INTEGER DEFAULT 0, " +
                "UNIQUE(" + HabitLogs.HABIT_ID + ", " + HabitLogs.DATE + "), " +
                "FOREIGN KEY(" + HabitLogs.HABIT_ID + ") REFERENCES " +
                Habits.TABLE + "(" + Habits.ID + ") ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE TABLE " + Badges.TABLE + " (" +
                Badges.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                Badges.NAME + " TEXT NOT NULL, " +
                Badges.DESCRIPTION + " TEXT, " +
                Badges.ICON_RES_ID + " INTEGER DEFAULT 0, " +
                Badges.UNLOCKED + " INTEGER DEFAULT 0, " +
                Badges.UNLOCKED_AT + " INTEGER DEFAULT 0, " +
                DbContract.IS_SYNCED + " INTEGER DEFAULT 0" +
                ")");

        db.execSQL("CREATE TABLE " + MicroActions.TABLE + " (" +
                MicroActions.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MicroActions.HABIT_ID + " INTEGER NOT NULL, " +
                MicroActions.TEXT + " TEXT NOT NULL, " +
                MicroActions.POSITION + " INTEGER DEFAULT 0, " +
                MicroActions.LAST_DONE_DATE + " TEXT, " +
                DbContract.IS_SYNCED + " INTEGER DEFAULT 0, " +
                "FOREIGN KEY(" + MicroActions.HABIT_ID + ") REFERENCES " +
                Habits.TABLE + "(" + Habits.ID + ") ON DELETE CASCADE" +
                ")");

        seedBadges(db);
    }

    private void seedBadges(@NonNull SQLiteDatabase db) {
        // Catégorie A — Habitudes créées
        insertBadge(db, "Recrue", "Crée ta première habitude", R.drawable.ic_badge_recrue);
        insertBadge(db, "Stratège", "Crée 5 habitudes", R.drawable.ic_badge_stratege);
        insertBadge(db, "Architecte", "Crée 10 habitudes", R.drawable.ic_badge_architecte);
        // Catégorie B — Completions totales
        insertBadge(db, "Soldat", "Complète ta première habitude", R.drawable.ic_badge_soldat);
        insertBadge(db, "Caporal", "Atteins 10 completions", R.drawable.ic_badge_caporal);
        insertBadge(db, "Sergent", "Atteins 25 completions", R.drawable.ic_badge_sergent);
        insertBadge(db, "Commandant", "Atteins 50 completions", R.drawable.ic_badge_commandant);
        insertBadge(db, "Colonel", "Atteins 100 completions", R.drawable.ic_badge_colonel);
        insertBadge(db, "Maréchal", "Atteins 250 completions", R.drawable.ic_badge_marechal);
        insertBadge(db, "Légende", "Atteins 500 completions", R.drawable.ic_badge_legende);
        // Catégorie C — Streak maximum
        insertBadge(db, "Lieutenant", "Maintiens un streak de 7 jours", R.drawable.ic_badge_lieutenant);
        insertBadge(db, "Général", "Maintiens un streak de 30 jours", R.drawable.ic_badge_general);
        insertBadge(db, "Légionnaire", "Maintiens un streak de 365 jours", R.drawable.ic_badge_legionnaire);
    }

    private void insertBadge(@NonNull SQLiteDatabase db, String name, String desc, int iconResId) {
        ContentValues v = new ContentValues();
        v.put(Badges.NAME, name);
        v.put(Badges.DESCRIPTION, desc);
        v.put(Badges.ICON_RES_ID, iconResId);
        v.put(Badges.UNLOCKED, 0);
        v.put(Badges.UNLOCKED_AT, 0);
        db.insert(Badges.TABLE, null, v);
    }

    @Override
    public void onConfigure(@NonNull SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 6) {
            String addCol = " ADD COLUMN " + DbContract.IS_SYNCED + " INTEGER DEFAULT 0";
            db.execSQL("ALTER TABLE " + Users.TABLE + addCol);
            db.execSQL("ALTER TABLE " + Habits.TABLE + addCol);
            db.execSQL("ALTER TABLE " + HabitLogs.TABLE + addCol);
            db.execSQL("ALTER TABLE " + Badges.TABLE + addCol);
            db.execSQL("ALTER TABLE " + MicroActions.TABLE + addCol);
        }
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE " + Users.TABLE + " ADD COLUMN " + Users.SECURITY_QUESTION + " TEXT");
            db.execSQL("ALTER TABLE " + Users.TABLE + " ADD COLUMN " + Users.SECURITY_ANSWER + " TEXT");
        }
        if (oldVersion < 9) {
            db.execSQL("DELETE FROM " + Badges.TABLE);
            seedBadges(db);
        }
    }
}
