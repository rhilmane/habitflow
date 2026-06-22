package com.example.myapplication.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import com.example.myapplication.database.DbContract;
import com.example.myapplication.database.DbHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Synchronise les données locales SQLite vers Supabase PostgreSQL.
 *
 * Fonctionnement :
 *  1. Chaque table SQLite a une colonne "is_synced" (0 = non envoyé, 1 = envoyé).
 *  2. syncAll() lit les lignes is_synced=0, les POST vers Supabase, puis marque is_synced=1.
 *  3. L'ordre est important : users → habits → micro_actions → habit_logs → badges.
 */
public class SyncManager {

    private static final String TAG = "SyncManager";
    private static final String IS_SYNCED = DbContract.IS_SYNCED;

    // ─── Point d'entrée public ────────────────────────────────────────────────

    /** Lance la synchronisation complète en arrière-plan. */
    public static void syncAll(Context context) {
        AppExecutors.io().execute(() -> {
            if (!isNetworkAvailable(context)) {
                Log.d(TAG, "Pas de réseau — sync annulée");
                return;
            }
            long userId = new SessionManager(context).getUserId();
            if (userId < 0) {
                Log.d(TAG, "Aucun utilisateur connecté — sync annulée");
                return;
            }
            Log.d(TAG, "Début synchronisation pour userId=" + userId);

            syncUsers(context, userId);
            syncHabits(context, userId);
            syncMicroActions(context, userId);
            syncHabitLogs(context, userId);
            syncBadges(context, userId);

            Log.d(TAG, "Synchronisation terminée");
        });
    }

    // ─── Sync par table ───────────────────────────────────────────────────────

    private static void syncUsers(Context context, long userId) {
        DbHelper helper = DbHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT * FROM " + DbContract.Users.TABLE +
                " WHERE " + DbContract.Users.ID + " = ?" +
                " AND " + IS_SYNCED + " = 0",
                new String[]{String.valueOf(userId)});

        try {
            JSONArray arr = cursorToJson(c);
            if (arr.length() == 0) return;

            JSONObject userObj = arr.getJSONObject(0);
            String email = userObj.optString("email", "");

            // 1. Wach l-email kayn deja f Supabase (sجل mn device ukhor) ?
            long supabaseId = -1;
            String existingJson = SupabaseClient.fetch("users",
                    "email=eq." + email + "&limit=1");
            if (existingJson != null) {
                JSONArray existingArr = new JSONArray(existingJson);
                if (existingArr.length() > 0) {
                    supabaseId = existingArr.getJSONObject(0).getLong("id");
                }
            }

            // 2. Ila ma kaynch → insertReturning baش Supabase y3ti ID frid
            if (supabaseId < 0) {
                JSONObject toInsert = new JSONObject();
                toInsert.put("name",          userObj.optString("name", ""));
                toInsert.put("email",         email);
                toInsert.put("password_hash", userObj.optString("password_hash", ""));
                if (!userObj.isNull("security_question"))
                    toInsert.put("security_question", userObj.optString("security_question"));
                if (!userObj.isNull("security_answer"))
                    toInsert.put("security_answer", userObj.optString("security_answer"));
                toInsert.put("created_at",
                        userObj.optLong("created_at", System.currentTimeMillis()));
                supabaseId = SupabaseClient.insertReturning("users", toInsert.toString());
            }

            if (supabaseId < 0) {
                Log.e(TAG, "syncUsers: impossible d'obtenir un id Supabase");
                return;
            }

            // 3. Ila tbddl l-ID → bddl f SQLite + session
            if (supabaseId != userId) {
                reassignUserId(context, userId, supabaseId);
                SessionManager session = new SessionManager(context);
                session.saveSession(supabaseId, session.getName(), session.getEmail());
                Log.d(TAG, "syncUsers: id " + userId + " → " + supabaseId);
            } else {
                markSynced(helper, DbContract.Users.TABLE,
                        DbContract.Users.ID + " = ?",
                        new String[]{String.valueOf(userId)});
            }
            Log.d(TAG, "users synced (id=" + supabaseId + ")");

        } catch (JSONException e) {
            Log.e(TAG, "syncUsers JSON error: " + e.getMessage());
        } finally {
            c.close();
        }
    }

    /**
     * Kybddl user_id f SQLite: copies l-user b id jdid, update habits.user_id,
     * w delete l-user l-qdim. FK constraints disabled temporairement.
     */
    private static void reassignUserId(Context context, long oldId, long newId) {
        DbHelper helper = DbHelper.getInstance(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        // PRAGMA foreign_keys khass ikun barra transaction
        db.execSQL("PRAGMA foreign_keys = OFF");
        db.beginTransaction();
        try {
            // 1. Copy user avec newId
            Cursor c = db.rawQuery(
                    "SELECT * FROM " + DbContract.Users.TABLE +
                    " WHERE " + DbContract.Users.ID + " = ?",
                    new String[]{String.valueOf(oldId)});
            if (c.moveToFirst()) {
                ContentValues cv = new ContentValues();
                cv.put(DbContract.Users.ID, newId);
                for (int i = 0; i < c.getColumnCount(); i++) {
                    String col = c.getColumnName(i);
                    if (col.equals(DbContract.Users.ID)) continue;
                    switch (c.getType(i)) {
                        case Cursor.FIELD_TYPE_INTEGER: cv.put(col, c.getLong(i));   break;
                        case Cursor.FIELD_TYPE_FLOAT:   cv.put(col, c.getDouble(i)); break;
                        case Cursor.FIELD_TYPE_STRING:  cv.put(col, c.getString(i)); break;
                        default:                        cv.putNull(col);              break;
                    }
                }
                cv.put(IS_SYNCED, 1);
                db.insertWithOnConflict(DbContract.Users.TABLE, null, cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            c.close();

            // 2. Update habits.user_id oldId → newId
            ContentValues hcv = new ContentValues();
            hcv.put(DbContract.Habits.USER_ID, newId);
            db.update(DbContract.Habits.TABLE, hcv,
                    DbContract.Habits.USER_ID + " = ?",
                    new String[]{String.valueOf(oldId)});

            // 3. Delete l-user l-qdim (FK off → ma kaynch cascade delete)
            db.delete(DbContract.Users.TABLE,
                    DbContract.Users.ID + " = ?",
                    new String[]{String.valueOf(oldId)});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.execSQL("PRAGMA foreign_keys = ON");
        }
    }

    private static void syncHabits(Context context, long userId) {
        DbHelper helper = DbHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT * FROM " + DbContract.Habits.TABLE +
                " WHERE " + DbContract.Habits.USER_ID + " = ?" +
                " AND " + IS_SYNCED + " = 0",
                new String[]{String.valueOf(userId)});

        try {
            JSONArray arr = cursorToJson(c);
            if (arr.length() == 0) return;

            if (SupabaseClient.upsert("habits", arr.toString())) {
                markSynced(helper, DbContract.Habits.TABLE,
                        DbContract.Habits.USER_ID + " = ? AND " + IS_SYNCED + " = 0",
                        new String[]{String.valueOf(userId)});
                Log.d(TAG, "habits synced (" + arr.length() + " lignes)");
            }
        } catch (JSONException e) {
            Log.e(TAG, "syncHabits JSON error: " + e.getMessage());
        } finally {
            c.close();
        }
    }

    private static void syncMicroActions(Context context, long userId) {
        DbHelper helper = DbHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT ma.* FROM " + DbContract.MicroActions.TABLE + " ma" +
                " INNER JOIN " + DbContract.Habits.TABLE + " h" +
                " ON ma." + DbContract.MicroActions.HABIT_ID +
                " = h." + DbContract.Habits.ID +
                " WHERE h." + DbContract.Habits.USER_ID + " = ?" +
                " AND ma." + IS_SYNCED + " = 0",
                new String[]{String.valueOf(userId)});

        try {
            JSONArray arr = cursorToJson(c);
            if (arr.length() == 0) return;

            if (SupabaseClient.upsert("micro_actions", arr.toString())) {
                markSynced(helper, DbContract.MicroActions.TABLE,
                        DbContract.MicroActions.ID + " IN (" +
                        "SELECT ma." + DbContract.MicroActions.ID +
                        " FROM " + DbContract.MicroActions.TABLE + " ma" +
                        " INNER JOIN " + DbContract.Habits.TABLE + " h" +
                        " ON ma." + DbContract.MicroActions.HABIT_ID + " = h." + DbContract.Habits.ID +
                        " WHERE h." + DbContract.Habits.USER_ID + " = ?" +
                        " AND ma." + IS_SYNCED + " = 0)",
                        new String[]{String.valueOf(userId)});
                Log.d(TAG, "micro_actions synced (" + arr.length() + " lignes)");
            }
        } catch (JSONException e) {
            Log.e(TAG, "syncMicroActions JSON error: " + e.getMessage());
        } finally {
            c.close();
        }
    }

    private static void syncHabitLogs(Context context, long userId) {
        DbHelper helper = DbHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT hl.* FROM " + DbContract.HabitLogs.TABLE + " hl" +
                " INNER JOIN " + DbContract.Habits.TABLE + " h" +
                " ON hl." + DbContract.HabitLogs.HABIT_ID +
                " = h." + DbContract.Habits.ID +
                " WHERE h." + DbContract.Habits.USER_ID + " = ?" +
                " AND hl." + IS_SYNCED + " = 0",
                new String[]{String.valueOf(userId)});

        try {
            JSONArray arr = cursorToJson(c);
            if (arr.length() == 0) return;

            if (SupabaseClient.upsert("habit_logs", arr.toString())) {
                markSynced(helper, DbContract.HabitLogs.TABLE,
                        DbContract.HabitLogs.ID + " IN (" +
                        "SELECT hl." + DbContract.HabitLogs.ID +
                        " FROM " + DbContract.HabitLogs.TABLE + " hl" +
                        " INNER JOIN " + DbContract.Habits.TABLE + " h" +
                        " ON hl." + DbContract.HabitLogs.HABIT_ID + " = h." + DbContract.Habits.ID +
                        " WHERE h." + DbContract.Habits.USER_ID + " = ?" +
                        " AND hl." + IS_SYNCED + " = 0)",
                        new String[]{String.valueOf(userId)});
                Log.d(TAG, "habit_logs synced (" + arr.length() + " lignes)");
            }
        } catch (JSONException e) {
            Log.e(TAG, "syncHabitLogs JSON error: " + e.getMessage());
        } finally {
            c.close();
        }
    }

    private static void syncBadges(Context context, long userId) {
        DbHelper helper = DbHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();

        // Seulement les badges débloqués (unlocked=1) et non encore synchronisés
        Cursor c = db.rawQuery(
                "SELECT * FROM " + DbContract.Badges.TABLE +
                " WHERE " + DbContract.Badges.UNLOCKED + " = 1" +
                " AND " + IS_SYNCED + " = 0",
                null);

        try {
            JSONArray arr = new JSONArray();
            while (c.moveToNext()) {
                JSONObject obj = rowToJson(c);
                obj.put("user_id", userId); // ajouter user_id (absent dans SQLite)
                arr.put(obj);
            }
            if (arr.length() == 0) return;

            if (SupabaseClient.upsert("badges", arr.toString())) {
                markSynced(helper, DbContract.Badges.TABLE,
                        DbContract.Badges.UNLOCKED + " = 1 AND " + IS_SYNCED + " = 0",
                        null);
                Log.d(TAG, "badges synced (" + arr.length() + " lignes)");
            }
        } catch (JSONException e) {
            Log.e(TAG, "syncBadges JSON error: " + e.getMessage());
        } finally {
            c.close();
        }
    }

    // ─── Download (Supabase → SQLite) ────────────────────────────────────────

    /** Télécharge les données cloud et les insère en local (INSERT OR REPLACE). */
    public static void downloadAll(Context context) {
        AppExecutors.io().execute(() -> {
            if (!isNetworkAvailable(context)) {
                Log.d(TAG, "Pas de réseau — download annulé");
                return;
            }
            long userId = new SessionManager(context).getUserId();
            if (userId < 0) {
                Log.d(TAG, "Aucun utilisateur — download annulé");
                return;
            }
            Log.d(TAG, "Début téléchargement pour userId=" + userId);

            downloadUsers(context, userId);
            List<Long> habitIds = downloadHabits(context, userId);
            if (!habitIds.isEmpty()) {
                String inClause = buildInClause(habitIds);
                downloadMicroActions(context, inClause);
                downloadHabitLogs(context, inClause);
            }
            downloadBadges(context, userId);

            Log.d(TAG, "Téléchargement terminé");
        });
    }

    private static void downloadUsers(Context context, long userId) {
        String json = SupabaseClient.fetch("users", "id=eq." + userId);
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            SQLiteDatabase db = DbHelper.getInstance(context).getWritableDatabase();
            for (int i = 0; i < arr.length(); i++) {
                ContentValues cv = jsonToContentValues(arr.getJSONObject(i), null);
                db.insertWithOnConflict(DbContract.Users.TABLE, null, cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            Log.d(TAG, "users downloaded: " + arr.length());
        } catch (JSONException e) {
            Log.e(TAG, "downloadUsers error: " + e.getMessage());
        }
    }

    private static List<Long> downloadHabits(Context context, long userId) {
        List<Long> ids = new ArrayList<>();
        String json = SupabaseClient.fetch("habits", "user_id=eq." + userId);
        if (json == null) return ids;
        try {
            JSONArray arr = new JSONArray(json);
            SQLiteDatabase db = DbHelper.getInstance(context).getWritableDatabase();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                ids.add(obj.getLong("id"));
                ContentValues cv = jsonToContentValues(obj, null);
                db.insertWithOnConflict(DbContract.Habits.TABLE, null, cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            Log.d(TAG, "habits downloaded: " + arr.length());
        } catch (JSONException e) {
            Log.e(TAG, "downloadHabits error: " + e.getMessage());
        }
        return ids;
    }

    private static void downloadMicroActions(Context context, String inClause) {
        String json = SupabaseClient.fetch("micro_actions", "habit_id=in.(" + inClause + ")");
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            SQLiteDatabase db = DbHelper.getInstance(context).getWritableDatabase();
            for (int i = 0; i < arr.length(); i++) {
                ContentValues cv = jsonToContentValues(arr.getJSONObject(i), null);
                db.insertWithOnConflict(DbContract.MicroActions.TABLE, null, cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            Log.d(TAG, "micro_actions downloaded: " + arr.length());
        } catch (JSONException e) {
            Log.e(TAG, "downloadMicroActions error: " + e.getMessage());
        }
    }

    private static void downloadHabitLogs(Context context, String inClause) {
        String json = SupabaseClient.fetch("habit_logs", "habit_id=in.(" + inClause + ")");
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            SQLiteDatabase db = DbHelper.getInstance(context).getWritableDatabase();
            for (int i = 0; i < arr.length(); i++) {
                ContentValues cv = jsonToContentValues(arr.getJSONObject(i), null);
                db.insertWithOnConflict(DbContract.HabitLogs.TABLE, null, cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            Log.d(TAG, "habit_logs downloaded: " + arr.length());
        } catch (JSONException e) {
            Log.e(TAG, "downloadHabitLogs error: " + e.getMessage());
        }
    }

    private static void downloadBadges(Context context, long userId) {
        // En Supabase les badges ont user_id, mais pas dans SQLite local → on le skip
        Set<String> skip = Collections.singleton("user_id");
        String json = SupabaseClient.fetch("badges", "user_id=eq." + userId);
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            SQLiteDatabase db = DbHelper.getInstance(context).getWritableDatabase();
            for (int i = 0; i < arr.length(); i++) {
                ContentValues cv = jsonToContentValues(arr.getJSONObject(i), skip);
                db.insertWithOnConflict(DbContract.Badges.TABLE, null, cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            Log.d(TAG, "badges downloaded: " + arr.length());
        } catch (JSONException e) {
            Log.e(TAG, "downloadBadges error: " + e.getMessage());
        }
    }

    private static ContentValues jsonToContentValues(JSONObject obj, Set<String> skipKeys)
            throws JSONException {
        ContentValues cv = new ContentValues();
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (IS_SYNCED.equals(key)) continue;
            if (skipKeys != null && skipKeys.contains(key)) continue;
            Object val = obj.get(key);
            if (val == JSONObject.NULL) {
                cv.putNull(key);
            } else if (val instanceof Integer) {
                cv.put(key, (Integer) val);
            } else if (val instanceof Long) {
                cv.put(key, (Long) val);
            } else if (val instanceof Double) {
                cv.put(key, (Double) val);
            } else if (val instanceof Boolean) {
                cv.put(key, (Boolean) val ? 1 : 0);
            } else {
                cv.put(key, val.toString());
            }
        }
        cv.put(IS_SYNCED, 1);
        return cv;
    }

    private static String buildInClause(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(ids.get(i));
        }
        return sb.toString();
    }

    // ─── Utilitaires ──────────────────────────────────────────────────────────

    /** Convertit toutes les lignes d'un Cursor en JSONArray (sans la colonne is_synced). */
    private static JSONArray cursorToJson(Cursor c) throws JSONException {
        JSONArray arr = new JSONArray();
        while (c.moveToNext()) {
            arr.put(rowToJson(c));
        }
        return arr;
    }

    /** Convertit la ligne courante d'un Cursor en JSONObject (sans is_synced). */
    private static JSONObject rowToJson(Cursor c) throws JSONException {
        JSONObject obj = new JSONObject();
        for (int i = 0; i < c.getColumnCount(); i++) {
            String col = c.getColumnName(i);
            if (IS_SYNCED.equals(col)) continue; // ne pas envoyer à Supabase
            switch (c.getType(i)) {
                case Cursor.FIELD_TYPE_INTEGER: obj.put(col, c.getLong(i));   break;
                case Cursor.FIELD_TYPE_FLOAT:   obj.put(col, c.getDouble(i)); break;
                case Cursor.FIELD_TYPE_STRING:  obj.put(col, c.getString(i)); break;
                default:                        obj.put(col, JSONObject.NULL); break;
            }
        }
        return obj;
    }

    /** Marque les lignes d'une table comme synchronisées (is_synced = 1). */
    private static void markSynced(DbHelper helper, String table,
                                   String where, String[] whereArgs) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(IS_SYNCED, 1);
        db.update(table, cv, where, whereArgs);
    }

    /** Vérifie si une connexion internet active est disponible. */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps =
                cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }
}
