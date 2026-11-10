package com.example.smartgymtracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

// 1.1.1 · Exercise details: the sets of one exercise (APP-1.7)
public class ExerciseDetailsActivity extends AppCompatActivity {

    private ExerciseEntry exercise;
    private int exerciseIndex;
    private LinearLayout setsList;
    private MaterialSwitch manualSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_details);

        exerciseIndex = getIntent().getIntExtra(WorkoutActivity.EXTRA_EXERCISE_INDEX, 0);
        if (exerciseIndex >= WorkoutSession.exercises.size()) {
            finish();
            return;
        }
        exercise = WorkoutSession.exercises.get(exerciseIndex);

        WorkoutUi.showEquipment(findViewById(R.id.equipment_tile),
                (ImageView) findViewById(R.id.equipment_icon), exercise.equipment);
        ((TextView) findViewById(R.id.exercise_name)).setText(exercise.name);
        TextView equipmentText = findViewById(R.id.exercise_equipment);
        equipmentText.setText(exercise.equipment);
        equipmentText.setTextColor(WorkoutUi.color(this, WorkoutUi.equipmentColor(exercise.equipment)));

        setsList = findViewById(R.id.sets_list);
        manualSwitch = findViewById(R.id.manual_switch);
        manualSwitch.setChecked(!DeviceState.motionConnected);
        manualSwitch.setOnCheckedChangeListener((button, checked) -> showSets());

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        findViewById(R.id.add_set_button).setOnClickListener(v -> addSet());
        findViewById(R.id.delete_set_button).setOnClickListener(v -> deleteSet());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (exercise != null) {
            showSets();
        }
    }

    private void showSets() {
        setsList.removeAllViews();
        int current = exercise.getCurrentSetIndex();

        for (int i = 0; i < exercise.sets.size(); i++) {
            SetEntry set = exercise.sets.get(i);
            View row = LayoutInflater.from(this).inflate(R.layout.item_set_row, setsList, false);
            TextView number = row.findViewById(R.id.set_number);
            TextView previous = row.findViewById(R.id.set_previous);
            EditText weight = row.findViewById(R.id.set_weight);
            EditText reps = row.findViewById(R.id.set_reps);
            MaterialButton action = row.findViewById(R.id.set_action);

            number.setText(String.valueOf(i + 1));

            SetEntry last = WorkoutHistory.getPreviousSet(exercise.name, exercise.equipment, i);
            previous.setText(last == null ? "—" : last.weight + " × " + last.reps);

            if (set.weight > 0) {
                weight.setText(String.valueOf(set.weight));
            }
            weight.setEnabled(!set.done);
            weight.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                public void onTextChanged(CharSequence s, int start, int before, int count) { }

                public void afterTextChanged(Editable s) {
                    set.weight = toNumber(s.toString());
                }
            });

            if (set.done) {
                // Finished set: lime row, "Done"
                row.setBackgroundResource(R.drawable.bg_set_done);
                number.setTextColor(WorkoutUi.color(this, R.color.gym_accent));
                reps.setText(String.valueOf(set.reps));
                reps.setEnabled(false);
                action.setText("Done");
                action.setIconResource(R.drawable.ic_check);
                action.setClickable(false);
            } else if (i == current) {
                // The next set to do
                reps.setEnabled(manualSwitch.isChecked());
                if (manualSwitch.isChecked()) {
                    action.setText("Complete");
                    action.setIconResource(R.drawable.ic_check);
                    action.setOnClickListener(v -> completeByHand(set, reps));
                } else {
                    action.setText("Start");
                    action.setIconResource(R.drawable.ic_play);
                    int setIndex = i;
                    action.setOnClickListener(v -> startSet(set, setIndex));
                }
            } else {
                // Later sets unlock one after the other
                number.setTextColor(WorkoutUi.color(this, R.color.gym_text_secondary));
                reps.setEnabled(false);
                action.setText("Locked");
                action.setIconResource(R.drawable.ic_lock);
                action.setEnabled(false);
            }
            setsList.addView(row);

            if (set.done) {
                addRestPill(set);
            }
        }

        findViewById(R.id.delete_set_button).setEnabled(current != -1);
    }

    // Rest after a set: live timer if it is still running, otherwise the time it took
    private void addRestPill(SetEntry set) {
        boolean running = set == WorkoutSession.restAfterSet && WorkoutSession.restStartElapsed > 0;
        if (!running && set.restSec < 0) {
            return;
        }
        View pill = LayoutInflater.from(this).inflate(R.layout.item_rest_pill, setsList, false);
        Chronometer time = pill.findViewById(R.id.rest_time);
        if (running) {
            time.setBase(WorkoutSession.restStartElapsed);
            time.start();
        } else {
            time.setText(WorkoutUi.formatTime(set.restSec));
        }
        setsList.addView(pill);
    }

    private void startSet(SetEntry set, int setIndex) {
        if (set.weight <= 0) {
            Toast.makeText(this, "Enter the weight first", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, SetDetailsActivity.class);
        intent.putExtra(WorkoutActivity.EXTRA_EXERCISE_INDEX, exerciseIndex);
        intent.putExtra(WorkoutActivity.EXTRA_SET_INDEX, setIndex);
        startActivity(intent);
    }

    private void completeByHand(SetEntry set, EditText repsInput) {
        int reps = toNumber(repsInput.getText().toString());
        if (set.weight <= 0 || reps <= 0) {
            Toast.makeText(this, "Enter the weight and the reps", Toast.LENGTH_SHORT).show();
            return;
        }
        set.reps = reps;
        set.done = true;
        WorkoutSession.stopRest();
        WorkoutSession.startRest(set);
        showSets();
    }

    private void addSet() {
        int weight = 0;
        if (!exercise.sets.isEmpty()) {
            weight = exercise.sets.get(exercise.sets.size() - 1).weight;
        }
        exercise.sets.add(new SetEntry(weight));
        showSets();
    }

    // Removes the last set that is not done yet
    private void deleteSet() {
        for (int i = exercise.sets.size() - 1; i >= 0; i--) {
            if (!exercise.sets.get(i).done) {
                exercise.sets.remove(i);
                showSets();
                return;
            }
        }
    }

    private int toNumber(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
