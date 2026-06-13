package com.example.myapplication.util;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.google.android.material.card.MaterialCardView;

/**
 * Centralisation dyal icons w colors dyal l'habits.
 * Kayt-istخدم f AddHabit (picker) + HabitAdapter + HabitDetail.
 */
public final class HabitStyle {

    private HabitStyle() {}

    /** Icons li yقder l'musta3mel yختار menhom. */
    public static final int[] ICONS = {
            R.drawable.ic_water,
            R.drawable.ic_fire,
            R.drawable.ic_star,
            R.drawable.ic_directions_run,
            R.drawable.ic_music,
            R.drawable.ic_check,
            R.drawable.ic_military_tech,
            R.drawable.ic_speed,
            R.drawable.ic_trending_up,
            R.drawable.ic_calendar,
            R.drawable.ic_person,
            R.drawable.ic_home,
    };

    /** Colors (ARGB) li yقder l'musta3mel yختار menhom. */
    public static final int[] COLORS = {
            0xFF0056D2, // blue
            0xFF006C49, // green
            0xFF006495, // teal-blue
            0xFFBA1A1A, // red
            0xFFF59E0B, // amber
            0xFF9333EA, // purple
            0xFFEC4899, // pink
            0xFF14B8A6, // teal
    };

    public static int iconOrDefault(int iconResId) {
        return iconResId != 0 ? iconResId : R.drawable.ic_star;
    }

    /**
     * Kayطبّق icon + color 3la circle (card) + image.
     * colorTag == 0 → style default (primary_fixed).
     */
    public static void apply(MaterialCardView circle, ImageView icon, int iconResId, int colorTag) {
        icon.setImageResource(iconOrDefault(iconResId));
        if (colorTag != 0) {
            circle.setCardBackgroundColor(colorTag);
            icon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        } else {
            circle.setCardBackgroundColor(
                    ContextCompat.getColor(circle.getContext(), R.color.primary_fixed));
            icon.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(circle.getContext(), R.color.on_primary_fixed)));
        }
    }
}
