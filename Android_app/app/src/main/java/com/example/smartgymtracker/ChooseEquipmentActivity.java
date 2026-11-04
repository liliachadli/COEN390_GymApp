package com.example.smartgymtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

// APP-1.5: the user picks the machine and sees the bracelet that will be used
public class ChooseEquipmentActivity extends AppCompatActivity {

    private final List<View> cards = new ArrayList<>();
    private String exercise;
    private String selectedEquipment = null;
    private MaterialButton continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_equipment);

        // Exercise chosen on the previous screen
        exercise = getIntent().getStringExtra(WorkoutOptions.EXTRA_EXERCISE);
        if (exercise == null) {
            finish();
            return;
        }

        WorkoutUi.setupHeader(this, 2, "Choose your machine",
                "Machines you can use for " + exercise + ".");

        continueButton = findViewById(R.id.continue_button);
        continueButton.setOnClickListener(v -> goToStart());

        // One card per machine that works for this exercise
        LinearLayout list = findViewById(R.id.option_list);
        String[] machines = WorkoutOptions.getEquipmentFor(exercise);
        for (String machine : machines) {
            View card = WorkoutUi.addOptionCard(this, list, machine,
                    WorkoutOptions.getEquipmentDescription(machine), R.drawable.ic_fitness);
            card.setOnClickListener(v -> selectEquipment(card, machine));
            cards.add(card);
        }

        // Only one machine possible: select it for the user
        if (cards.size() == 1) {
            selectEquipment(cards.get(0), machines[0]);
        }

        // The bracelet used during the workout (real connection comes in APP-1.6)
        WorkoutUi.setupDeviceRow(this, R.id.device_tracker, R.drawable.ic_bluetooth,
                "Smart bracelet", "Motion + heart rate, on your wrist", "Not connected");
    }

    private void selectEquipment(View chosenCard, String machine) {
        selectedEquipment = machine;
        for (View card : cards) {
            WorkoutUi.setCardSelected(card, card == chosenCard);
        }
        continueButton.setEnabled(true);
    }

    private void goToStart() {
        Intent intent = new Intent(this, StartWorkoutActivity.class);
        intent.putExtra(WorkoutOptions.EXTRA_EXERCISE, exercise);
        intent.putExtra(WorkoutOptions.EXTRA_EQUIPMENT, selectedEquipment);
        startActivity(intent);
    }
}
