package com.example.smartgymtracker;

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

import androidx.appcompat.app.AppCompatActivity;

// 1.1.3 · Choose exercise, grouped by muscle (APP-1.4 / APP-1.5)
public class ChooseExerciseActivity extends AppCompatActivity {

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
    }

    private void addExerciseRow(String name, String equipment) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_exercise_choice, list, false);
        WorkoutUi.showEquipment(row.findViewById(R.id.equipment_tile),
                (ImageView) row.findViewById(R.id.equipment_icon), equipment);
        ((TextView) row.findViewById(R.id.exercise_name)).setText(name);
        TextView equipmentText = row.findViewById(R.id.exercise_equipment);
        equipmentText.setText(" · " + equipment.toLowerCase());
        equipmentText.setTextColor(WorkoutUi.color(this, WorkoutUi.equipmentColor(equipment)));

        row.setOnClickListener(v -> {
            WorkoutSession.addExercise(name, equipment, 3);
            Toast.makeText(this, name + " added", Toast.LENGTH_SHORT).show();
            finish();
        });
        list.addView(row);
    }
}
