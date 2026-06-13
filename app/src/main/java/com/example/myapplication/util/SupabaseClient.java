package com.example.myapplication.util;

import android.util.Log;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Singleton OkHttpClient pré-configuré pour l'API REST Supabase.
 * Ajoute automatiquement les headers apikey + Authorization sur chaque requête.
 */
public class SupabaseClient {

    private static final String TAG = "SupabaseClient";

    public static final String BASE_URL =
            "https://jjftqurrfuwuvzbvwaxp.supabase.co/rest/v1/";

    private static final String API_KEY =
            "sb_publishable_IIQfUfTrt_Nm71xLvVAsbA_892sPFVN";

    public static final MediaType JSON_TYPE =
            MediaType.get("application/json; charset=utf-8");

    private static OkHttpClient sClient;

    private SupabaseClient() {}

    public static OkHttpClient get() {
        if (sClient == null) {
            sClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request req = original.newBuilder()
                                .header("apikey", API_KEY)
                                .header("Authorization", "Bearer " + API_KEY)
                                .header("Content-Type", "application/json")
                                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                                .build();
                        return chain.proceed(req);
                    })
                    .build();
        }
        return sClient;
    }

    /**
     * Upsert (insert ou update si id existe déjà) d'un tableau JSON dans une table Supabase.
     * @param table  nom de la table (ex: "habits")
     * @param json   tableau JSON ex: [{"id":1,"name":"Sport",...}]
     * @return true si succès (2xx)
     */
    public static boolean upsert(String table, String json) {
        RequestBody body = RequestBody.create(json, JSON_TYPE);
        Request request = new Request.Builder()
                .url(BASE_URL + table)
                .post(body)
                .build();
        try (Response response = get().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "upsert " + table + " failed: "
                        + response.code() + " " + response.message());
            }
            return response.isSuccessful();
        } catch (IOException e) {
            Log.e(TAG, "upsert " + table + " IOException: " + e.getMessage());
            return false;
        }
    }
}
