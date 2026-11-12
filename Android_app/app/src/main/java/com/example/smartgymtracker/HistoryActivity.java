package com.example.smartgymtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// 2 · History: list of past workouts, newest first (APP-1.10)
public class HistoryActivity extends AppCompatActivity {

    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        list = findViewById(R.id.history_list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavBar.setup(this, R.id.nav_history);
        showWorkouts();

        WorkoutHistory.loadWorkouts(()->{
            showWorkouts();
        });
    }

    private void showWorkouts() {
        list.removeAllViews();
        findViewById(R.id.empty_text).setVisibility(
                WorkoutHistory.workouts.isEmpty() ? View.VISIBLE : View.GONE);
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.ENGLISH);

        for (int i = 0; i < WorkoutHistory.workouts.size(); i++) {
            PastWorkout workout = WorkoutHistory.workouts.get(i);
            View card = LayoutInflater.from(this).inflate(R.layout.item_history_card, list, false);

            ((TextView) card.findViewById(R.id.history_name)).setText(workout.name);
            ((TextView) card.findViewById(R.id.history_date)).setText(
                    dateFormat.format(new Date(workout.startTime)) + " · " + WorkoutUi.formatDuration(workout.durationSec));
            card.findViewById(R.id.history_pr).setVisibility(workout.prText.isEmpty() ? View.GONE : View.VISIBLE);

            // Up to 3 exercises: "Lat Pulldown      3 × 10"
            LinearLayout lines = card.findViewById(R.id.history_exercises);
            for (int e = 0; e < Math.min(3, workout.exercises.size()); e++) {
                ExerciseEntry exercise = workout.exercises.get(e);
                View line = LayoutInflater.from(this).inflate(R.layout.item_history_line, lines, false);
                ((TextView) line.findViewById(R.id.line_name)).setText(exercise.name);
                if(!exercise.sets.isEmpty()){
                    ((TextView) line.findViewById(R.id.line_sets)).setText(
                            exercise.sets.size() + " × " + exercise.sets.get(0).reps);
                } else{
                    ((TextView) line.findViewById(R.id.line_sets)).setText("0 × 0");
                }
                lines.addView(line);
            }

            String footer = "Volume " + String.format(Locale.ENGLISH, "%,d", workout.getTotalVolume()) + " lbs";
            if (workout.getMaxBpm() > 0) {
                footer += "   ·   Max " + workout.getMaxBpm() + " BPM";
            }
            ((TextView) card.findViewById(R.id.history_footer)).setText(footer);


            card.setOnClickListener(v -> {
                int index = WorkoutHistory.workouts.indexOf(workout);
                if (index == -1) {
                    Toast.makeText(this, "Workout not found", Toast.LENGTH_LONG).show();
                    return;
                }

                Intent intent = new Intent(this, PastWorkoutActivity.class);
                intent.putExtra(PastWorkoutActivity.EXTRA_WORKOUT_INDEX, index);
                startActivity(intent);
            });
            list.addView(card);
        }
    }
}
