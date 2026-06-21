package com.example.myapplication.activities;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.dao.HabitDao;
import com.example.myapplication.dao.MicroActionDao;
import com.example.myapplication.models.Frequency;
import com.example.myapplication.models.Habit;
import com.example.myapplication.models.MicroAction;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.BadgeEvaluator;
import com.example.myapplication.util.HabitStyle;
import com.example.myapplication.util.ReminderScheduler;
import com.example.myapplication.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AddHabitActivity extends AppCompatActivity {

    public static final String EXTRA_HABIT_ID = "habit_id";

    private HabitDao habitDao;
    private MicroActionDao microActionDao;
    private SessionManager session;

    private TextView tvScreenTitle;
    private EditText nameInput;
    private MaterialButtonToggleGroup toggleFrequency;
    private ChipGroup chipsDays;
    private ChipGroup chipsCategory;
    private MaterialSwitch switchReminder;
    private TextView tvReminderTime;
    private MaterialButton btnSave;

    private MaterialCardView iconCircle, colorDot;
    private ImageView ivChosenIcon;

    private TextInputEditText microActionInput;
    private ChipGroup microActionsChipGroup;
    private final List<String> microActions = new ArrayList<>();

    private int reminderHour = 8;
    private int reminderMinute = 0;
    private int selectedIcon = 0;
    private int selectedColor = 0;

    private long editHabitId = -1; // -1 = add mode

    private ActivityResultLauncher<String> notifPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_habit);
        habitDao = new HabitDao(this);
        microActionDao = new MicroActionDao(this);
        session = new SessionManager(this);

        editHabitId = getIntent().getLongExtra(EXTRA_HABIT_ID, -1);

        tvScreenTitle = findViewById(R.id.tv_screen_title);
        nameInput = findViewById(R.id.habit_name_input);
        toggleFrequency = findViewById(R.id.toggle_frequency);
        chipsDays = findViewById(R.id.chips_days);
        chipsCategory = findViewById(R.id.chips_category);
        switchReminder = findViewById(R.id.switch_reminder);
        tvReminderTime = findViewById(R.id.tv_reminder_time);
        btnSave = findViewById(R.id.btn_save_habit);
        iconCircle = findViewById(R.id.icon_circle);
        ivChosenIcon = findViewById(R.id.iv_chosen_icon);
        colorDot = findViewById(R.id.color_dot);
        microActionInput = findViewById(R.id.micro_action_input);
        microActionsChipGroup = findViewById(R.id.micro_actions_chipgroup);

        findViewById(R.id.btn_add_micro).setOnClickListener(v -> addMicroAction());
        microActionInput.setOnEditorActionListener((v, actionId, event) -> {
            addMicroAction();
            return true;
        });

        findViewById(R.id.card_choose_icon).setOnClickListener(v -> showIconPicker());
        findViewById(R.id.card_color_tag).setOnClickListener(v -> showColorPicker());

        toggleFrequency.check(R.id.btn_daily);
        toggleFrequency.addOnButtonCheckedListener((g, id, checked) -> updateDaysVisibility());
        updateDaysVisibility();
        updateReminderTimeLabel();
        applyIconColorPreview();

        notifPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (!granted) {
                        Toast.makeText(this,
                                "Notifications off — enable them in settings to get reminders",
                                Toast.LENGTH_LONG).show();
                    }
                });

        switchReminder.setOnCheckedChangeListener((v, checked) -> {
            if (checked)
                requestNotifPermissionIfNeeded();
        });
        tvReminderTime.setOnClickListener(v -> showTimePicker());

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveHabit());

        if (editHabitId != -1) {
            tvScreenTitle.setText("Edit Habit");
            btnSave.setText("Update");
            loadForEdit();
        }
    }

    private void updateDaysVisibility() {
        chipsDays.setVisibility(
                toggleFrequency.getCheckedButtonId() == R.id.btn_specific ? View.VISIBLE : View.GONE);
    }

    // ---------- Icon / Color pickers ----------

    private void applyIconColorPreview() {
        HabitStyle.apply(iconCircle, ivChosenIcon, selectedIcon, selectedColor);
        int dotColor = selectedColor != 0 ? selectedColor
                : ContextCompat.getColor(this, R.color.primary);
        colorDot.setCardBackgroundColor(dotColor);
    }

    private void showIconPicker() {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        int pad = dp(16);
        grid.setPadding(pad, pad, pad, pad);

        AppCompatDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Choose Icon")
                .setView(grid)
                .create();

        for (int iconRes : HabitStyle.ICONS) {
            ImageView iv = new ImageView(this);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(48);
            lp.height = dp(48);
            lp.setMargins(dp(8), dp(8), dp(8), dp(8));
            iv.setLayoutParams(lp);
            iv.setImageResource(iconRes);
            iv.setPadding(dp(8), dp(8), dp(8), dp(8));
            iv.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.on_surface)));
            iv.setOnClickListener(v -> {
                selectedIcon = iconRes;
                applyIconColorPreview();
                dialog.dismiss();
            });
            grid.addView(iv);
        }
        dialog.show();
    }

    private void showColorPicker() {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        int pad = dp(16);
        grid.setPadding(pad, pad, pad, pad);

        AppCompatDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Color Tag")
                .setView(grid)
                .create();

        for (int color : HabitStyle.COLORS) {
            View dot = new View(this);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(44);
            lp.height = dp(44);
            lp.setMargins(dp(10), dp(10), dp(10), dp(10));
            dot.setLayoutParams(lp);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(color);
            dot.setBackground(circle);
            dot.setOnClickListener(v -> {
                selectedColor = color;
                applyIconColorPreview();
                dialog.dismiss();
            });
            grid.addView(dot);
        }
        dialog.show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ---------- Micro actions ----------

    private void addMicroAction() {
        String text = microActionInput.getText() == null ? "" : microActionInput.getText().toString().trim();
        if (TextUtils.isEmpty(text))
            return;
        addMicroActionChip(text);
        microActionInput.setText("");
    }

    private void addMicroActionChip(String text) {
        microActions.add(text);
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            microActions.remove(text);
            microActionsChipGroup.removeView(chip);
        });
        microActionsChipGroup.addView(chip);
    }

    // ---------- Reminder time ----------

    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hour, minute) -> {
            reminderHour = hour;
            reminderMinute = minute;
            updateReminderTimeLabel();
        }, reminderHour, reminderMinute, false);
        dialog.show();
    }

    private void updateReminderTimeLabel() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, reminderHour);
        cal.set(Calendar.MINUTE, reminderMinute);
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("hh:mm a", Locale.US);
        tvReminderTime.setText(fmt.format(cal.getTime()));
    }

    private void requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    // ---------- Days helpers ----------

    private String collectSelectedDays() {
        List<String> days = new ArrayList<>();
        for (int i = 0; i < chipsDays.getChildCount(); i++) {
            View child = chipsDays.getChildAt(i);
            if (child instanceof Chip && ((Chip) child).isChecked()) {
                Object tag = child.getTag();
                if (tag != null)
                    days.add(tag.toString());
            }
        }
        return TextUtils.join(",", days);
    }

    private void applySelectedDays(String specificDays) {
        if (TextUtils.isEmpty(specificDays))
            return;
        Set<String> set = new HashSet<>(Arrays.asList(specificDays.split(",")));
        for (int i = 0; i < chipsDays.getChildCount(); i++) {
            View child = chipsDays.getChildAt(i);
            if (child instanceof Chip && child.getTag() != null) {
                ((Chip) child).setChecked(set.contains(child.getTag().toString()));
            }
        }
    }

    // ---------- Edit prefill ----------

    private void loadForEdit() {
        AppExecutors.io().execute(() -> {
            Habit habit = habitDao.findById(editHabitId);
            List<MicroAction> micros = microActionDao.getForHabit(editHabitId);
            AppExecutors.main().execute(() -> {
                if (habit == null) {
                    Toast.makeText(this, "Habit not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                nameInput.setText(habit.name);
                selectedIcon = habit.iconResId;
                selectedColor = habit.colorTag;
                applyIconColorPreview();

                if (habit.frequency == Frequency.SPECIFIC_DAYS) {
                    toggleFrequency.check(R.id.btn_specific);
                    applySelectedDays(habit.specificDays);
                } else {
                    toggleFrequency.check(R.id.btn_daily);
                }
                updateDaysVisibility();

                switchReminder.setChecked(habit.reminderEnabled);
                if (habit.reminderTime != null && habit.reminderTime.contains(":")) {
                    try {
                        String[] p = habit.reminderTime.split(":");
                        reminderHour = Integer.parseInt(p[0]);
                        reminderMinute = Integer.parseInt(p[1]);
                    } catch (NumberFormatException ignored) {
                    }
                    updateReminderTimeLabel();
                }

                if ("Health & Fitness".equals(habit.category)) {
                    chipsCategory.check(R.id.chip_cat_health);
                } else if ("Work & Focus".equals(habit.category)) {
                    chipsCategory.check(R.id.chip_cat_work);
                } else {
                    chipsCategory.check(R.id.chip_cat_other);
                }

                for (MicroAction m : micros) {
                    addMicroActionChip(m.text);
                }
            });
        });
    }

    // ---------- Save ----------

    private void saveHabit() {
        String name = nameInput.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Enter a habit name");
            nameInput.requestFocus();
            return;
        }

        boolean specific = toggleFrequency.getCheckedButtonId() == R.id.btn_specific;
        String days = specific ? collectSelectedDays() : null;
        if (specific && TextUtils.isEmpty(days)) {
            Toast.makeText(this, "Pick at least one day", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean reminderOn = switchReminder.isChecked();
        String timeStored = String.format(Locale.US, "%02d:%02d", reminderHour, reminderMinute);

        Habit habit = new Habit();
        habit.id = editHabitId; // -1 ila add
        habit.userId = session.getUserId();
        habit.name = name;
        habit.frequency = specific ? Frequency.SPECIFIC_DAYS : Frequency.DAILY;
        habit.specificDays = days;
        habit.reminderEnabled = reminderOn;
        habit.reminderTime = reminderOn ? timeStored : null;
        habit.iconResId = selectedIcon;
        habit.colorTag = selectedColor;

        int checkedCat = chipsCategory.getCheckedChipId();
        if (checkedCat == R.id.chip_cat_health)
            habit.category = "Health & Fitness";
        else if (checkedCat == R.id.chip_cat_work)
            habit.category = "Work & Focus";
        else
            habit.category = "Other";

        List<String> microSnapshot = new ArrayList<>(microActions);
        boolean isEdit = editHabitId != -1;

        btnSave.setEnabled(false);
        AppExecutors.io().execute(() -> {
            long id;
            if (isEdit) {
                habitDao.update(habit);
                id = editHabitId;
                // 3awd bni micro actions
                microActionDao.deleteForHabit(id);
            } else {
                id = habitDao.insert(habit);
            }
            if (id > 0) {
                for (int i = 0; i < microSnapshot.size(); i++) {
                    microActionDao.insert(new MicroAction(id, microSnapshot.get(i), i));
                }
            }
            final long fid = id;
            AppExecutors.main().execute(() -> {
                btnSave.setEnabled(true);
                if (fid > 0 || isEdit) {
                    // reminders: cancel + reschedule (wla cancel )
                    ReminderScheduler.cancel(this, fid);
                    if (reminderOn) {
                        ReminderScheduler.schedule(this, fid, name, timeStored, days);
                    }
                    BadgeEvaluator.evaluateAsync(this, habit.userId);
                    Toast.makeText(this, isEdit ? "Habit updated" : "Habit saved",
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Could not save habit", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
