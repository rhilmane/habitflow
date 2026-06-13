package com.example.myapplication.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Habit;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    public interface OnHabitInteraction {
        void onHabitClick(Habit habit);
        void onToggleDone(Habit habit, boolean newDone);
    }

    private final List<Habit> habits = new ArrayList<>();
    private final Set<Long> doneToday = new HashSet<>();
    private final java.util.Map<Long, String> microProgress = new java.util.HashMap<>();
    private final OnHabitInteraction listener;

    public HabitAdapter(OnHabitInteraction listener) {
        this.listener = listener;
    }

    public void setData(List<Habit> newHabits, Set<Long> doneTodayIds) {
        habits.clear();
        habits.addAll(newHabits);
        doneToday.clear();
        doneToday.addAll(doneTodayIds);
        notifyDataSetChanged();
    }

    public void setMicroProgress(java.util.Map<Long, String> progress) {
        microProgress.clear();
        if (progress != null) microProgress.putAll(progress);
        notifyDataSetChanged();
    }

    public void setDone(long habitId, boolean done) {
        if (done) {
            doneToday.add(habitId);
        } else {
            doneToday.remove(habitId);
        }
        for (int i = 0; i < habits.size(); i++) {
            if (habits.get(i).id == habitId) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        holder.bind(habits.get(position));
    }

    @Override
    public int getItemCount() {
        return habits.size();
    }

    class HabitViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView root;
        final MaterialCardView iconContainer;
        final ImageView icon;
        final TextView name;
        final TextView subtitle;
        final MaterialButton check;

        HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.item_root);
            iconContainer = itemView.findViewById(R.id.icon_container);
            icon = itemView.findViewById(R.id.iv_habit_icon);
            name = itemView.findViewById(R.id.tv_habit_name);
            subtitle = itemView.findViewById(R.id.tv_habit_subtitle);
            check = itemView.findViewById(R.id.btn_check);
        }

        void bind(Habit habit) {
            name.setText(habit.name);

            String freqLabel = habit.frequency == null ? "Daily"
                    : (habit.frequency.name().equals("DAILY") ? "Daily" : "Specific days");
            String mp = microProgress.get(habit.id);
            subtitle.setText(mp != null ? freqLabel + " · " + mp + " steps" : freqLabel);

            HabitStyle.apply(iconContainer, icon, habit.iconResId, habit.colorTag);

            boolean done = doneToday.contains(habit.id);
            updateCheckState(done);

            root.setOnClickListener(v -> {
                if (listener != null) listener.onHabitClick(habit);
            });

            check.setOnClickListener(v -> {
                boolean newDone = !doneToday.contains(habit.id);
                setDone(habit.id, newDone);
                if (listener != null) listener.onToggleDone(habit, newDone);
            });
        }

        private void updateCheckState(boolean done) {
            float alpha = done ? 1f : 0.35f;
            check.setAlpha(alpha);
        }
    }
}
