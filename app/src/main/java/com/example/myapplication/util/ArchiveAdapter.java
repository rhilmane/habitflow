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

import java.util.ArrayList;
import java.util.List;

public class ArchiveAdapter extends RecyclerView.Adapter<ArchiveAdapter.ArchiveViewHolder> {

    public interface OnRestore {
        void onRestore(Habit habit);
    }

    public interface OnDelete {
        void onDelete(Habit habit);
    }

    private final List<Habit> habits = new ArrayList<>();
    private final OnRestore listener;
    private OnDelete deleteListener;

    public ArchiveAdapter(OnRestore listener) {
        this.listener = listener;
    }

    public void setOnDeleteListener(OnDelete l) {
        this.deleteListener = l;
    }

    public void setData(List<Habit> newHabits) {
        habits.clear();
        habits.addAll(newHabits);
        notifyDataSetChanged();
    }

    public void remove(Habit habit) {
        int index = habits.indexOf(habit);
        if (index >= 0) {
            habits.remove(index);
            notifyItemRemoved(index);
        }
    }

    @NonNull
    @Override
    public ArchiveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_archive, parent, false);
        return new ArchiveViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArchiveViewHolder holder, int position) {
        holder.bind(habits.get(position));
    }

    @Override
    public int getItemCount() {
        return habits.size();
    }

    class ArchiveViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView subtitle;
        final MaterialButton btnRestore;
        final MaterialButton btnDelete;

        ArchiveViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.iv_archive_icon);
            name = itemView.findViewById(R.id.tv_archive_name);
            subtitle = itemView.findViewById(R.id.tv_archive_subtitle);
            btnRestore = itemView.findViewById(R.id.btn_restore);
            btnDelete = itemView.findViewById(R.id.btn_delete_archive);
        }

        void bind(Habit habit) {
            name.setText(habit.name);
            String freqLabel = habit.frequency == null ? "Daily"
                    : (habit.frequency.name().equals("DAILY") ? "Daily" : "Specific days");
            subtitle.setText(freqLabel);

            if (habit.iconResId != 0) {
                icon.setImageResource(habit.iconResId);
            } else {
                icon.setImageResource(R.drawable.ic_star);
            }

            btnRestore.setOnClickListener(v -> {
                if (listener != null) listener.onRestore(habit);
            });

            btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) deleteListener.onDelete(habit);
            });
        }
    }
}
