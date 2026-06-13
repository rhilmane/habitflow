package com.example.myapplication.models;

public class HabitLog {
    public long id;
    public long habitId;
    public String date;   // "2026-06-02"
    public boolean done;

    public HabitLog() {
    }

    public HabitLog(long habitId, String date, boolean done) {
        this.habitId = habitId;
        this.date = date;
        this.done = done;
    }
}
