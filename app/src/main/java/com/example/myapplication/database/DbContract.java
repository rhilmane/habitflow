package com.example.myapplication.database;

/**
 * Smiyat l-jadawil w l-colonnes kamlin f blasa wahda.
 */
public final class DbContract {

    private DbContract() {
    }

    /**
     * Colonne commune à toutes les tables pour le suivi de synchronisation
     * Supabase.
     */
    public static final String IS_SYNCED = "is_synced";

    public static final class Users {
        public static final String TABLE = "users";
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String EMAIL = "email";
        public static final String PASSWORD_HASH = "password_hash";
        public static final String AVATAR_URL = "avatar_url";
        public static final String CREATED_AT = "created_at";
        public static final String SECURITY_QUESTION = "security_question";
        public static final String SECURITY_ANSWER = "security_answer";
    }

    public static final class Habits {
        public static final String TABLE = "habits";
        public static final String ID = "id";
        public static final String USER_ID = "user_id";
        public static final String NAME = "name";
        public static final String ICON_RES_ID = "icon_res_id";
        public static final String COLOR_TAG = "color_tag";
        public static final String FREQUENCY = "frequency";
        public static final String SPECIFIC_DAYS = "specific_days";
        public static final String CATEGORY = "category";
        public static final String REMINDER_ENABLED = "reminder_enabled";
        public static final String REMINDER_TIME = "reminder_time";
        public static final String ARCHIVED = "archived";
        public static final String CREATED_AT = "created_at";
    }

    public static final class HabitLogs {
        public static final String TABLE = "habit_logs";
        public static final String ID = "id";
        public static final String HABIT_ID = "habit_id";
        public static final String DATE = "date";
        public static final String DONE = "done";
    }

    public static final class Badges {
        public static final String TABLE = "badges";
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String ICON_RES_ID = "icon_res_id";
        public static final String UNLOCKED = "unlocked";
        public static final String UNLOCKED_AT = "unlocked_at";
    }

    public static final class MicroActions {
        public static final String TABLE = "micro_actions";
        public static final String ID = "id";
        public static final String HABIT_ID = "habit_id";
        public static final String TEXT = "text";
        public static final String POSITION = "position";
        public static final String LAST_DONE_DATE = "last_done_date";
    }
}
