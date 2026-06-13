package com.example.myapplication.util;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(String dateStr);
    }

    /** Khliya dyal nhar. dayNumber==0 → khliya khawya (padding). */
    public static class Day {
        public final int dayNumber;     // 0 = khawya
        public final float ratio;       // 0..1 (done / total habits dak nhar)
        public final boolean isToday;
        public final String dateStr;    // "yyyy-MM-dd" or "" for empty cells

        public Day(int dayNumber, float ratio, boolean isToday, String dateStr) {
            this.dayNumber = dayNumber;
            this.ratio = ratio;
            this.isToday = isToday;
            this.dateStr = dateStr;
        }
    }

    private final List<Day> days = new ArrayList<>();
    private OnDayClickListener clickListener;

    public void setOnDayClickListener(OnDayClickListener l) {
        this.clickListener = l;
    }

    public void setData(List<Day> newDays) {
        days.clear();
        days.addAll(newDays);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        holder.bind(days.get(position), clickListener);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvDay;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.day_card);
            tvDay = itemView.findViewById(R.id.tv_day);
        }

        void bind(Day day, OnDayClickListener listener) {
            if (day.dayNumber == 0) {
                tvDay.setText("");
                card.setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
                card.setStrokeWidth(0);
                card.setOnClickListener(null);
                return;
            }

            tvDay.setText(String.valueOf(day.dayNumber));

            int bgColorRes;
            int textColorRes;
            if (day.ratio >= 1f) {
                bgColorRes = R.color.primary;
                textColorRes = R.color.on_primary;
            } else if (day.ratio > 0f) {
                bgColorRes = R.color.primary_container;
                textColorRes = R.color.on_primary_container;
            } else {
                bgColorRes = android.R.color.transparent;
                textColorRes = R.color.on_surface;
            }

            card.setCardBackgroundColor(
                    ContextCompat.getColor(card.getContext(), bgColorRes));
            tvDay.setTextColor(ContextCompat.getColor(card.getContext(), textColorRes));

            if (day.isToday) {
                card.setStrokeWidth(dp(card, 2));
                card.setStrokeColor(ColorStateList.valueOf(
                        ContextCompat.getColor(card.getContext(), R.color.primary)));
            } else {
                card.setStrokeWidth(0);
            }

            if (listener != null) {
                card.setOnClickListener(v -> listener.onDayClick(day.dateStr));
            } else {
                card.setOnClickListener(null);
            }
        }

        private int dp(View v, int value) {
            return Math.round(value * v.getResources().getDisplayMetrics().density);
        }
    }
}
