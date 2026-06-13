package com.example.myapplication.models;

public class Habit {
    public long id;
    public long userId;
    public String name;            // "What do you want to build?"
    public int iconResId;          // Choose Icon
    public int colorTag;           // Color Tag
    public Frequency frequency;    // DAILY / SPECIFIC_DAYS
    public String specificDays;    // "MON,TUE,WED" ila kant SPECIFIC_DAYS
    public String category;        // "Health & Fitness", "Work & Focus", etc.
    public boolean reminderEnabled;
    public String reminderTime;    // "08:00 AM"
    public boolean archived;       // l Archives
    public long createdAt;

    public Habit() {
        this.frequency = Frequency.DAILY;
        this.category = "Other";
        this.createdAt = System.currentTimeMillis();
    }
}
