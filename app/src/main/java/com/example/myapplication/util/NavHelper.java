package com.example.myapplication.util;

import android.app.Activity;
import android.content.Intent;

import com.example.myapplication.R;
import com.example.myapplication.activities.CalendarActivity;
import com.example.myapplication.activities.MainActivity;
import com.example.myapplication.activities.ProfileActivity;
import com.example.myapplication.activities.StatisticsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public final class NavHelper {

    private NavHelper() {}

    /**
     * Appeler depuis onCreate(). Stocke selectedId dans le tag du nav pour onResume.
     */
    public static void setup(Activity current, BottomNavigationView nav, int selectedId) {
        if (nav == null) return;

        // Stocker selectedId dans le tag — récupéré par resume()
        nav.setTag(selectedId);

        // Supprimer les listeners avant de changer la sélection (évite navigation parasite)
        nav.setOnItemSelectedListener(null);
        nav.setOnItemReselectedListener(null);

        if (selectedId != 0) {
            nav.setSelectedItemId(selectedId);
        }

        nav.setOnItemSelectedListener(item -> route(current, item.getItemId()));
        nav.setOnItemReselectedListener(item -> {});
    }

    /**
     * Appeler depuis onResume() pour corriger la sélection après retour via BACK.
     * Le tag stocké dans setup() contient le bon selectedId.
     */
    public static void resume(BottomNavigationView nav) {
        if (nav == null || nav.getTag() == null) return;
        int selectedId = (int) nav.getTag();
        if (selectedId != 0 && nav.getSelectedItemId() != selectedId) {
            // Enlever le listener pour éviter de déclencher navigation
            nav.setOnItemSelectedListener(null);
            nav.setSelectedItemId(selectedId);
            // Le listener sera remis au prochain setup() — mais il est déjà configuré
            // depuis onCreate donc on le remet tout de suite
        }
    }

    /**
     * Version complète de resume() qui remet aussi les listeners.
     * Appeler depuis onResume() en passant current.
     */
    public static void resume(Activity current, BottomNavigationView nav) {
        if (nav == null || nav.getTag() == null) return;
        int selectedId = (int) nav.getTag();
        if (selectedId == 0) return;

        // Retirer les listeners d'abord
        nav.setOnItemSelectedListener(null);
        nav.setOnItemReselectedListener(null);

        // Forcer la bonne sélection
        nav.setSelectedItemId(selectedId);

        // Remettre les listeners
        nav.setOnItemSelectedListener(item -> route(current, item.getItemId()));
        nav.setOnItemReselectedListener(item -> {});
    }

    private static boolean route(Activity current, int id) {
        Class<?> target = null;
        if (id == R.id.navigation_home)          target = MainActivity.class;
        else if (id == R.id.navigation_calendar) target = CalendarActivity.class;
        else if (id == R.id.navigation_stats)    target = StatisticsActivity.class;
        else if (id == R.id.navigation_profile)  target = ProfileActivity.class;

        if (target != null && !current.getClass().equals(target)) {
            Intent intent = new Intent(current, target);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            current.startActivity(intent);
            current.overridePendingTransition(0, 0);
        }
        return true;
    }
}
