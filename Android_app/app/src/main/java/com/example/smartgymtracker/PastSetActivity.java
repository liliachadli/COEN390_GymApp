package com.example.smartgymtracker;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

// 2.1.3 · Past set details: every rep of one set (APP-1.11)
public class PastSetActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_set);

        int workoutIndex = getIntent().getIntExtra(PastWorkoutActivity.EXTRA_WORKOUT_INDEX, 0);
        int exerciseIndex = getIntent().getIntExtra(WorkoutActivity.EXTRA_EXERCISE_INDEX, 0);
        int setIndex = getIntent().getIntExtra(WorkoutActivity.EXTRA_SET_INDEX, 0);

        // Check if the workout index is valid
        if (workoutIndex < 0 || workoutIndex >= WorkoutHistory.workouts.size()) {
            finish();
            return;
        }

        // Get the workout index
        PastWorkout workout = WorkoutHistory.workouts.get(workoutIndex);

        // Check if the exercise index is valid
        if (exerciseIndex < 0 || exerciseIndex >= workout.exercises.size()) {
            finish();
            return;
        }

        // Get the exercise index
        ExerciseEntry exercise = workout.exercises.get(exerciseIndex);

        // Check if the set index is valid
        if (setIndex < 0 || setIndex >= exercise.sets.size()) {
            finish();
            return;
        }

        // Get the set index
        SetEntry set = exercise.sets.get(setIndex);

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.page_title)).setText(exercise.name + " · Set " + (setIndex + 1));
        ((TextView) findViewById(R.id.page_subtitle)).setText(Units.format(set.weight) + " · " + set.reps + " reps");

        LinearLayout repList = findViewById(R.id.rep_list);
        for (int i = 0; i < set.repList.size(); i++) {
            WorkoutUi.addRepRow(repList, i + 1, set.repList.get(i), R.color.gym_violet);
        }
        if (set.repList.isEmpty()) {
            findViewById(R.id.no_sensor_text).setVisibility(View.VISIBLE);
        }
        ((TextView) findViewById(R.id.total_tut)).setText(WorkoutUi.formatTut(set.getTotalTutMs()));

        // Heart-rate card: graph and average of this set
        View heartCard = findViewById(R.id.heart_card);
        if (set.bpmList.isEmpty()) {
            heartCard.setVisibility(View.GONE);
        } else {
            HeartRateGraphView graph = heartCard.findViewById(R.id.hr_graph);
            graph.setValues(set.bpmList);
            ((TextView) heartCard.findViewById(R.id.avg_bpm)).setText("Avg " + set.getAverageBpm());
            ((TextView) heartCard.findViewById(R.id.current_bpm)).setText("Max " + set.getMaxBpm() + " BPM");
        }
    }
}
