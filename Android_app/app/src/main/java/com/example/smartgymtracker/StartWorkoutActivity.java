package com.example.smartgymtracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

// APP-1.7: the user checks the setup and starts the workout
public class StartWorkoutActivity extends AppCompatActivity {

    private String exercise;
    private String equipment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_workout);

        exercise = getIntent().getStringExtra(WorkoutOptions.EXTRA_EXERCISE);
        equipment = getIntent().getStringExtra(WorkoutOptions.EXTRA_EQUIPMENT);
        if (exercise == null || equipment == null) {
            finish();
            return;
        }

        WorkoutUi.setupHeader(this, 3, "Ready to train",
                "Check your setup, then start your workout.");

        // Summary of the choices
        TextView exerciseText = findViewById(R.id.summary_exercise);
        TextView musclesText = findViewById(R.id.summary_muscles);
        TextView equipmentText = findViewById(R.id.summary_equipment);
        exerciseText.setText(exercise);
        musclesText.setText(WorkoutOptions.getMusclesFor(exercise));
        equipmentText.setText(equipment);

        // Body drawing of the muscle group
        ImageView muscleIcon = findViewById(R.id.summary_icon);
        muscleIcon.setImageResource(WorkoutOptions.getIconForGroup(WorkoutOptions.getGroupFor(exercise)));

        WorkoutUi.setupDeviceRow(this, R.id.device_tracker, R.drawable.ic_bluetooth,
                "Smart bracelet", "Motion + heart rate, on your wrist", "Connects at start");

        MaterialButton startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(v -> startWorkout());
    }

    private void startWorkout() {
        // The live workout screen comes in APP-1.8.
        // For now, open the Sprint 1 live data screen (it connects to the bracelet by itself).
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(WorkoutOptions.EXTRA_EXERCISE, exercise);
        intent.putExtra(WorkoutOptions.EXTRA_EQUIPMENT, equipment);
        // Clear the setup screens so Back and Logout work normally from the live screen
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
