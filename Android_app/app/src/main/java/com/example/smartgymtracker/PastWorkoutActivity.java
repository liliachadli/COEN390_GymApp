package com.example.smartgymtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// 2.1 · Past workout details. Also the summary shown after Finish (APP-1.9 / APP-1.11)
public class PastWorkoutActivity extends AppCompatActivity {

    public static final String EXTRA_WORKOUT_INDEX = "workoutIndex";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_workout);

        int workoutIndex = getIntent().getIntExtra(EXTRA_WORKOUT_INDEX, 0);
        if (workoutIndex < 0 || workoutIndex >= WorkoutHistory.workouts.size()) {
            finish();
            return;
        }
        PastWorkout workout = WorkoutHistory.workouts.get(workoutIndex);

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.page_title)).setText(workout.name);
        String date = new SimpleDateFormat("EEE, MMM d · h:mm a", Locale.ENGLISH).format(new Date(workout.startTime));
        ((TextView) findViewById(R.id.page_subtitle)).setText(date + " · " + WorkoutUi.formatDuration(workout.durationSec));

        ((TextView) findViewById(R.id.volume_value)).setText(
                Units.formatTotal(workout.getTotalVolume()));
        ((TextView) findViewById(R.id.pr_value)).setText(workout.prText.isEmpty() ? "—" : Units.convertText(workout.prText));
        ((TextView) findViewById(R.id.max_bpm_value)).setText(
                workout.getMaxBpm() > 0 ? String.valueOf(workout.getMaxBpm()) : "—");
        ((TextView) findViewById(R.id.hr_recovery_value)).setText(
                workout.hrRecovery > 0 ? "-" + workout.hrRecovery + " BPM" : "—");

        LinearLayout list = findViewById(R.id.past_exercise_list);
        for (int e = 0; e < workout.exercises.size(); e++) {
            ExerciseEntry exercise = workout.exercises.get(e);

            View header = LayoutInflater.from(this).inflate(R.layout.item_past_exercise_header, list, false);
            WorkoutUi.showEquipment(header.findViewById(R.id.equipment_tile),
                    (ImageView) header.findViewById(R.id.equipment_icon), exercise.equipment);
            ((TextView) header.findViewById(R.id.exercise_name)).setText(exercise.name);
            TextView equipmentText = header.findViewById(R.id.exercise_equipment);
            equipmentText.setText(" · " + exercise.equipment.toLowerCase());
            equipmentText.setTextColor(WorkoutUi.color(this, WorkoutUi.equipmentColor(exercise.equipment)));
            ((TextView) header.findViewById(R.id.exercise_sets)).setText(exercise.sets.size() + " sets");
            list.addView(header);
            list.addView(LayoutInflater.from(this).inflate(R.layout.item_past_set_header, list, false));

            for (int s = 0; s < exercise.sets.size(); s++) {
                SetEntry set = exercise.sets.get(s);
                View row = LayoutInflater.from(this).inflate(R.layout.item_past_set_row, list, false);
                ((TextView) row.findViewById(R.id.past_set_text)).setText(
                        (s + 1) + ".  " + Units.format(set.weight) + " × " + set.reps);
                ((TextView) row.findViewById(R.id.past_set_tut)).setText(
                        set.repList.isEmpty() ? "—" : WorkoutUi.formatTut(set.getTotalTutMs()));
                ((TextView) row.findViewById(R.id.past_set_rest)).setText(
                        set.restSec >= 0 ? WorkoutUi.formatTime(set.restSec) : "—");

                int exerciseIndex = e;
                int setIndex = s;
                row.setOnClickListener(v -> {
                    int currentIndex = WorkoutHistory.workouts.indexOf(workout);

                    if(currentIndex == -1){
                        Toast.makeText(this, "Workout not found", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Intent intent = new Intent(this, PastSetActivity.class);
                    intent.putExtra(EXTRA_WORKOUT_INDEX, currentIndex);
                    intent.putExtra(WorkoutActivity.EXTRA_EXERCISE_INDEX, exerciseIndex);
                    intent.putExtra(WorkoutActivity.EXTRA_SET_INDEX, setIndex);
                    startActivity(intent);
                });
                list.addView(row);
            }
        }
    }
}
