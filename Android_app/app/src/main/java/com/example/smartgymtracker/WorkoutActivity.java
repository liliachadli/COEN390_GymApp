package com.example.smartgymtracker;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// 1.1 · Workout page: timer and the list of exercises (APP-1.7)
public class WorkoutActivity extends BluetoothActivity {

    public static final String EXTRA_EXERCISE_INDEX = "exerciseIndex";
    public static final String EXTRA_SET_INDEX = "setIndex";

    private TextView nameText;
    private LinearLayout exerciseList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        if (!WorkoutSession.active) {
            WorkoutSession.start("Workout");
        }

        bleHelp = BluetoothHelper.getInstance(getApplicationContext(), this, "WorkoutActivity");

        nameText = findViewById(R.id.workout_name);
        exerciseList = findViewById(R.id.exercise_list);
        nameText.setText(WorkoutSession.name);

        TextView date = findViewById(R.id.workout_date);
        date.setText(new SimpleDateFormat("EEEE, MMM d", Locale.ENGLISH).format(new Date(WorkoutSession.startTime)));

        // The timer counts from the moment the workout started
        Chronometer timer = findViewById(R.id.workout_timer);
        timer.setBase(WorkoutSession.startElapsed);
        timer.start();

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        findViewById(R.id.edit_name_button).setOnClickListener(v -> editName());
        findViewById(R.id.finish_button).setOnClickListener(v -> askToFinish());
        findViewById(R.id.add_exercise_button).setOnClickListener(v ->
                startActivity(new Intent(this, ChooseExerciseActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        bleHelp.changeActivity(this, "WorkoutActivity");
        WorkoutUi.showDeviceChips(findViewById(R.id.motion_chip), findViewById(R.id.heart_chip));
        showExercises();
    }

    private void showExercises() {
        exerciseList.removeAllViews();
        findViewById(R.id.empty_text).setVisibility(
                WorkoutSession.exercises.isEmpty() ? View.VISIBLE : View.GONE);

        for (int i = 0; i < WorkoutSession.exercises.size(); i++) {
            ExerciseEntry exercise = WorkoutSession.exercises.get(i);
            View card = LayoutInflater.from(this).inflate(R.layout.item_workout_exercise, exerciseList, false);

            WorkoutUi.showEquipment(card.findViewById(R.id.equipment_tile),
                    (ImageView) card.findViewById(R.id.equipment_icon), exercise.equipment);
            ((TextView) card.findViewById(R.id.exercise_name)).setText(exercise.name);

            int weight = exercise.sets.isEmpty() ? 0 : exercise.sets.get(0).weight;
            String weightText = weight > 0 ? weight + " lbs" : "no weight yet";
            ((TextView) card.findViewById(R.id.exercise_info)).setText(exercise.equipment + " · " + weightText);

            // One small bar per set
            LinearLayout bars = card.findViewById(R.id.progress_bars);
            for (int s = 0; s < exercise.sets.size(); s++) {
                View bar = new View(this);
                bar.setBackgroundResource(R.drawable.bg_pill);
                WorkoutUi.tint(bar, exercise.sets.get(s).done ? R.color.gym_accent : R.color.gym_stroke);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
                if (s > 0) {
                    params.setMarginStart(WorkoutUi.dp(this, 6));
                }
                bars.addView(bar, params);
            }
            ((TextView) card.findViewById(R.id.progress_text)).setText(
                    exercise.getDoneCount() + " / " + exercise.sets.size() + " sets");

            // Only the exercise in progress has a lime button
            MaterialButton select = card.findViewById(R.id.select_button);
            boolean inProgress = exercise.getDoneCount() > 0 && exercise.getCurrentSetIndex() != -1;
            if (!inProgress) {
                select.setBackgroundTintList(ColorStateList.valueOf(
                        WorkoutUi.color(this, R.color.gym_surface_high)));
                select.setTextColor(WorkoutUi.color(this, R.color.gym_text));
            }
            int index = i;
            select.setOnClickListener(v -> openExercise(index));
            card.setOnClickListener(v -> openExercise(index));
            exerciseList.addView(card);
        }
    }

    private void openExercise(int index) {
        Intent intent = new Intent(this, ExerciseDetailsActivity.class);
        intent.putExtra(EXTRA_EXERCISE_INDEX, index);
        startActivity(intent);
    }

    private void editName() {
        EditText input = new EditText(this);
        input.setText(WorkoutSession.name);
        input.setSelection(input.getText().length());
        new AlertDialog.Builder(this)
                .setTitle("Workout name")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        WorkoutSession.name = name;
                        nameText.setText(name);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void askToFinish() {
        new AlertDialog.Builder(this)
                .setTitle("Finish workout?")
                .setMessage("Sets that are not done will not be saved.")
                .setPositiveButton("Finish", (dialog, which) -> finishWorkout())
                .setNegativeButton("Keep going", null)
                .show();
    }

    // Saves the workout and opens its summary (APP-1.9)
    private void finishWorkout() {
        WorkoutHistory.saveCurrentWorkout();
        WorkoutSession.reset();

        Intent intent = new Intent(this, PastWorkoutActivity.class);
        intent.putExtra(PastWorkoutActivity.EXTRA_WORKOUT_INDEX, 0);
        startActivity(intent);
        finish();
    }

    @Override
    public void showConnection(int deviceNum, boolean connected){
        super.showConnection(deviceNum, connected);
        WorkoutUi.showDeviceChips(findViewById(R.id.motion_chip), findViewById(R.id.heart_chip));
    }

    @Override
    public void showStatus(String text) {
        Log.i("WorkoutActivity", "BLE status: " + text);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
