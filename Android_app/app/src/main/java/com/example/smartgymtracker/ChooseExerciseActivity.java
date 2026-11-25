package com.example.smartgymtracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

// 1.1.3 · Choose exercise, grouped by muscle (APP-1.4 / APP-1.5)
// Two ways to open it:
//  - plan mode (EXTRA_PLAN): before the workout, pick several exercises, then Start (the timer starts then)
//  - normal: during the workout, tap one exercise to add it
public class ChooseExerciseActivity extends AppCompatActivity {

    public static final String EXTRA_PLAN = "plan";
    private static final int SETS_PER_EXERCISE = 3;

    private boolean planMode;
    private final ArrayList<String> picked = new ArrayList<>();   // "name|equipment", in the order picked
    private MaterialButton startButton;

    private LinearLayout list;
    private EditText search;
    private TextView filterAll;
    private TextView filterCable;
    private TextView filterMachine;
    private String filter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_exercise);

        list = findViewById(R.id.exercise_list);
        search = findViewById(R.id.search_input);
        filterAll = findViewById(R.id.filter_all);
        filterCable = findViewById(R.id.filter_cable);
        filterMachine = findViewById(R.id.filter_machine);

        planMode = getIntent().getBooleanExtra(EXTRA_PLAN, false);
        startButton = findViewById(R.id.start_plan_button);
        if (planMode) {
            ((TextView) findViewById(R.id.page_title)).setText("Plan your workout");
            startButton.setVisibility(View.VISIBLE);
            startButton.setOnClickListener(v -> startPlannedWorkout());
            if (savedInstanceState != null) {
                ArrayList<String> saved = savedInstanceState.getStringArrayList("picked");
                if (saved != null) {
                    picked.addAll(saved);
                }
            }
            updateStartButton();
        }

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        filterAll.setOnClickListener(v -> setFilter("All"));
        filterCable.setOnClickListener(v -> setFilter(WorkoutOptions.CABLE));
        filterMachine.setOnClickListener(v -> setFilter(WorkoutOptions.MACHINE));

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            public void afterTextChanged(Editable s) {
                showList();
            }
        });

        setFilter("All");
    }

    private void setFilter(String newFilter) {
        filter = newFilter;
        styleFilter(filterAll, filter.equals("All"), R.color.gym_accent, R.color.gym_accent_tint);
        styleFilter(filterCable, filter.equals(WorkoutOptions.CABLE), R.color.gym_cyan, R.color.gym_cyan_tint);
        styleFilter(filterMachine, filter.equals(WorkoutOptions.MACHINE), R.color.gym_amber, R.color.gym_amber_tint);
        showList();
    }

    // Selected filter: full color with dark text. Others: dark tile with colored text.
    private void styleFilter(TextView chip, boolean selected, int colorRes, int tintRes) {
        WorkoutUi.tint(chip, selected ? colorRes : tintRes);
        chip.setTextColor(WorkoutUi.color(this, selected ? R.color.gym_on_accent : colorRes));
    }

    private boolean matches(int i) {
        String text = search.getText().toString().trim().toLowerCase();
        boolean textOk = WorkoutOptions.EXERCISES[i].toLowerCase().contains(text);
        boolean filterOk = filter.equals("All") || WorkoutOptions.EQUIPMENT[i].equals(filter);
        return textOk && filterOk;
    }

    private void showList() {
        list.removeAllViews();

        for (String group : WorkoutOptions.MUSCLE_GROUPS) {
            int count = 0;
            for (int i = 0; i < WorkoutOptions.EXERCISES.length; i++) {
                if (WorkoutOptions.GROUPS[i].equals(group) && matches(i)) {
                    count++;
                }
            }
            if (count == 0) {
                continue;
            }

            View header = LayoutInflater.from(this).inflate(R.layout.item_group_header, list, false);
            WorkoutUi.showMuscle(header.findViewById(R.id.group_tile),
                    (ImageView) header.findViewById(R.id.group_icon), group);
            ((TextView) header.findViewById(R.id.group_name)).setText(group);
            TextView countText = header.findViewById(R.id.group_count);
            countText.setText(count == 1 ? "1 EXERCISE" : count + " EXERCISES");
            countText.setTextColor(WorkoutUi.color(this, WorkoutOptions.getColorForGroup(group)));
            list.addView(header);

            for (int i = 0; i < WorkoutOptions.EXERCISES.length; i++) {
                if (WorkoutOptions.GROUPS[i].equals(group) && matches(i)) {
                    addExerciseRow(WorkoutOptions.EXERCISES[i], WorkoutOptions.EQUIPMENT[i]);
                }
            }
        }

        if (list.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No exercise found");
            empty.setTextColor(WorkoutUi.color(this, R.color.gym_text_secondary));
            empty.setPadding(0, WorkoutUi.dp(this, 24), 0, 0);
            list.addView(empty);
        }
        Effects.cascade(list);
    }

    private void addExerciseRow(String name, String equipment) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_exercise_choice, list, false);
        WorkoutUi.showEquipment(row.findViewById(R.id.equipment_tile),
                (ImageView) row.findViewById(R.id.equipment_icon), equipment);
        ((TextView) row.findViewById(R.id.exercise_name)).setText(name);
        TextView equipmentText = row.findViewById(R.id.exercise_equipment);
        equipmentText.setText(" · " + equipment.toLowerCase());
        equipmentText.setTextColor(WorkoutUi.color(this, WorkoutUi.equipmentColor(equipment)));

        if (planMode) {
            showPicked(row, name + "|" + equipment);
            row.setOnClickListener(v -> {
                String key = name + "|" + equipment;
                if (picked.contains(key)) {
                    picked.remove(key);
                } else {
                    picked.add(key);
                }
                // Numbers of the other rows can change: refresh every row on screen
                for (int i = 0; i < list.getChildCount(); i++) {
                    Object tag = list.getChildAt(i).getTag();
                    if (tag != null) {
                        showPicked(list.getChildAt(i), (String) tag);
                    }
                }
                Effects.bump(row.findViewById(R.id.choice_number));
                Effects.tick(row);
                updateStartButton();
            });
        } else {
            row.setOnClickListener(v -> {
                if (WorkoutUi.tooFast()) {
                    return;
                }
                WorkoutSession.addExercise(name, equipment, 1);
                Toast.makeText(this, name + " added", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
        list.addView(row);
    }

    // Picked row: lime card with its number (1, 2, 3...). Not picked: normal card with a +
    private void showPicked(View row, String key) {
        row.setTag(key);
        int index = picked.indexOf(key);
        TextView number = row.findViewById(R.id.choice_number);
        View plus = row.findViewById(R.id.choice_icon);
        if (index >= 0) {
            row.setBackgroundResource(R.drawable.bg_set_done);
            number.setText(String.valueOf(index + 1));
            number.setVisibility(View.VISIBLE);
            plus.setVisibility(View.GONE);
        } else {
            row.setBackgroundResource(R.drawable.bg_card_default);
            number.setVisibility(View.GONE);
            plus.setVisibility(View.VISIBLE);
        }
    }

    private void updateStartButton() {
        int count = picked.size();
        startButton.setEnabled(count > 0);
        startButton.setText(count == 0 ? "Pick your exercises"
                : "Start workout · " + count + (count == 1 ? " exercise" : " exercises"));
    }

    // The workout (and its timer) starts only now, with the exercises already in it
    private void startPlannedWorkout() {
        if (picked.isEmpty() || WorkoutUi.tooFast()) {
            return;
        }
        if (!WorkoutSession.active) {
            WorkoutSession.start("Workout");
        }
        for (String key : picked) {
            String[] parts = key.split("\\|");
            WorkoutSession.addExercise(parts[0], parts[1], SETS_PER_EXERCISE);
        }
        startActivity(new Intent(this, WorkoutActivity.class));
        finish();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("picked", picked);
    }
}
