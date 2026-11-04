package com.example.smartgymtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

// APP-1.4: the user picks the exercise for this workout
public class ChooseExerciseActivity extends AppCompatActivity {

    private final List<View> cards = new ArrayList<>();
    private String selectedExercise = null;
    private MaterialButton continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_exercise);

        WorkoutUi.setupHeader(this, 1, "Choose your exercise",
                "Pick a muscle group, then the exercise you want to track.");

        // Exercises sorted by muscle group: a title with a body drawing, then its exercises
        LinearLayout list = findViewById(R.id.option_list);
        for (String group : WorkoutOptions.MUSCLE_GROUPS) {
            WorkoutUi.addSectionHeader(this, list, group,
                    WorkoutOptions.countInGroup(group), WorkoutOptions.getIconForGroup(group));

            for (int i = 0; i < WorkoutOptions.EXERCISES.length; i++) {
                if (!WorkoutOptions.EXERCISE_GROUPS[i].equals(group)) {
                    continue;
                }
                String exercise = WorkoutOptions.EXERCISES[i];
                String muscles = WorkoutOptions.MUSCLES[i];
                View card = WorkoutUi.addOptionCard(this, list, exercise, muscles, R.drawable.ic_fitness);
                card.setOnClickListener(v -> selectExercise(card, exercise));
                cards.add(card);
            }
        }

        // Continue stays disabled until an exercise is chosen
        continueButton = findViewById(R.id.continue_button);
        continueButton.setOnClickListener(v -> goToEquipment());
    }

    private void selectExercise(View chosenCard, String exercise) {
        selectedExercise = exercise;
        for (View card : cards) {
            WorkoutUi.setCardSelected(card, card == chosenCard);
        }
        continueButton.setEnabled(true);
    }

    private void goToEquipment() {
        Intent intent = new Intent(this, ChooseEquipmentActivity.class);
        intent.putExtra(WorkoutOptions.EXTRA_EXERCISE, selectedExercise);
        startActivity(intent);
    }
}
