package com.example.myapplication.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Badge;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {

    private final List<Badge> badges = new ArrayList<>();

    public void setData(List<Badge> newBadges) {
        badges.clear();
        badges.addAll(newBadges);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge, parent, false);
        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        holder.bind(badges.get(position));
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    static class BadgeViewHolder extends RecyclerView.ViewHolder {
        final View iconBg;
        final ImageView icon;
        final TextView name;
        final TextView desc;

        BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            iconBg = itemView.findViewById(R.id.badge_icon_bg);
            icon = itemView.findViewById(R.id.iv_badge_icon);
            name = itemView.findViewById(R.id.tv_badge_name);
            desc = itemView.findViewById(R.id.tv_badge_desc);
        }

        void bind(Badge badge) {
            name.setText(badge.name);
            desc.setText(badge.description);
            if (badge.iconResId != 0) {
                icon.setImageResource(badge.iconResId);
                icon.setImageTintList(null);
            } else {
                icon.setImageResource(R.drawable.ic_star);
                icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        icon.getContext().getColor(R.color.on_primary_fixed)));
            }
            float alpha = badge.unlocked ? 1f : 0.35f;
            iconBg.setAlpha(alpha);
            name.setAlpha(alpha);
            desc.setAlpha(alpha);

            itemView.setOnClickListener(v -> {
                String statut = badge.unlocked ? "Débloqué" : "Verrouillé";
                String date = "";
                if (badge.unlocked && badge.unlockedAt > 0) {
                    date = "\nDébloqué le : " + new SimpleDateFormat("dd/MM/yyyy", Locale.US)
                            .format(new Date(badge.unlockedAt));
                }
                new MaterialAlertDialogBuilder(v.getContext())
                        .setTitle(badge.name)
                        .setMessage(badge.description + "\n\nStatut : " + statut + date)
                        .setPositiveButton("OK", null)
                        .show();
            });
        }
    }
}
