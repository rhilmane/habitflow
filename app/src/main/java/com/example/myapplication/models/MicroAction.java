package com.example.myapplication.models;

public class MicroAction {
    public long id;
    public long habitId;
    public String text;          // smiya dyal l'micro action (khotwa sghira)
    public int position;         // tartib
    public String lastDoneDate;  // "yyyy-MM-dd" — akher nhar li tcheckat fih (null = jamais)

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
