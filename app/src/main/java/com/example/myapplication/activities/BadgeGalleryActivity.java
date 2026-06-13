package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.util.BadgeAdapter;
import com.example.myapplication.dao.BadgeDao;
import com.example.myapplication.models.Badge;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.AvatarUtil;
import com.example.myapplication.util.BadgeEvaluator;
import com.example.myapplication.util.NavHelper;
import com.example.myapplication.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class BadgeGalleryActivity extends AppCompatActivity {

    private BadgeDao badgeDao;
    private SessionManager session;
    private BadgeAdapter adapter;
    private TextView tvEarned, tvLocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_badge_gallery);
        badgeDao = new BadgeDao(this);
        session = new SessionManager(this);

        RecyclerView rv = findViewById(R.id.rv_badges);
        adapter = new BadgeAdapter();
        rv.setAdapter(adapter);
        tvEarned = findViewById(R.id.tv_earned_count);
        tvLocked = findViewById(R.id.tv_locked_count);

        ImageView avatar = findViewById(R.id.profile_avatar);
        if (avatar != null) {
            AvatarUtil.setInitial(avatar, session.getName());
            avatar.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        NavHelper.setup(this, nav, R.id.navigation_profile);

        loadBadges();
    }

    private void loadBadges() {
        long userId = session.getUserId();
        AppExecutors.io().execute(() -> {
            // 9bel l'affichage, n3awdo nقيمو badges bach nfekkو li t-istahaqqo
            BadgeEvaluator.evaluate(getApplicationContext(), userId);
            List<Badge> badges = badgeDao.getAll();
            long earned = 0, locked = 0;
            for (Badge b : badges) { if (b.unlocked) earned++; else locked++; }
            final long fEarned = earned, fLocked = locked;
            AppExecutors.main().execute(() -> {
                adapter.setData(badges);
                tvEarned.setText(String.valueOf(fEarned));
                tvLocked.setText(String.valueOf(fLocked));
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavHelper.resume(this, findViewById(R.id.bottom_navigation));
    }
}
