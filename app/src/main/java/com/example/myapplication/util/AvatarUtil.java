package com.example.myapplication.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.example.myapplication.R;

/**
 * Kayصاوب avatar b l'7arf l'awwel dyal smiya (3la khlfiya mlowna).
 * L'ImageView (ShapeableImageView) houwa li kayدير l'cercle.
 */
public final class AvatarUtil {

    private AvatarUtil() {}

    public static void setInitial(ImageView iv, String name) {
        int color = ContextCompat.getColor(iv.getContext(), R.color.primary);
        setInitial(iv, name, color);
    }

    public static void setInitial(ImageView iv, String name, int bgColor) {
        String initial = (name == null || name.trim().isEmpty())
                ? "?" : name.trim().substring(0, 1).toUpperCase();

        int size = 200;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(bgColor);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTextSize(size * 0.5f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics fm = paint.getFontMetrics();
        float y = size / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(initial, size / 2f, y, paint);

        iv.setImageBitmap(bmp);
    }
}
