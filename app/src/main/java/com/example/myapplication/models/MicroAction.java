package com.example.myapplication.models;

public class MicroAction {
    public long id;
    public long habitId;
    public String text;
    public int position;
    public String lastDoneDate;

    public MicroAction() {
    }

    public MicroAction(long habitId, String text, int position) {
        this.habitId = habitId;
        this.text = text;
        this.position = position;
    }

    /** Wach hadi done f nhar mu3ayan. */
    public boolean isDoneOn(String date) {
        return lastDoneDate != null && lastDoneDate.equals(date);
    }
}
