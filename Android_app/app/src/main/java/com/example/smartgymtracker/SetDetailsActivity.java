package com.example.smartgymtracker;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

// 1.1.2 · Set details: live reps, ROM, TUT and heart rate during one set (APP-1.8)
public class SetDetailsActivity extends AppCompatActivity implements SensorListener {

    private SetEntry set;
    private FakeSensor sensor;
    private final List<RepData> reps = new ArrayList<>();
    private final List<Integer> bpms = new ArrayList<>();

    // Rest of the previous set, kept in case this set is cancelled
    private boolean restStopped = false;
    private long savedRestStart;
    private SetEntry savedRestSet;

    private TextView stateChip;
    private TextView repsCount;
    private TextView tutValue;
    private TextView romValue;
    private TextView finishHint;
    private TextView avgBpm;
    private TextView currentBpm;
    private LinearLayout repList;
    private ScrollView repScroll;
    private HeartRateGraphView graph;
    private MaterialButton finishButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_details);

        int exerciseIndex = getIntent().getIntExtra(WorkoutActivity.EXTRA_EXERCISE_INDEX, 0);
        int setIndex = getIntent().getIntExtra(WorkoutActivity.EXTRA_SET_INDEX, 0);
        ExerciseEntry exercise = WorkoutSession.exercises.get(exerciseIndex);
        set = exercise.sets.get(setIndex);

        ((TextView) findViewById(R.id.set_title)).setText(exercise.name);
        ((TextView) findViewById(R.id.set_subtitle)).setText("Set " + (setIndex + 1) + " · " + set.weight + " lbs");

        stateChip = findViewById(R.id.state_chip);
        repsCount = findViewById(R.id.reps_count);
        tutValue = findViewById(R.id.tut_value);
        romValue = findViewById(R.id.rom_value);
        finishHint = findViewById(R.id.finish_hint);
        avgBpm = findViewById(R.id.avg_bpm);
        currentBpm = findViewById(R.id.current_bpm);
        repList = findViewById(R.id.rep_list);
        repScroll = findViewById(R.id.rep_scroll);
        graph = findViewById(R.id.hr_graph);
        finishButton = findViewById(R.id.finish_button);

        findViewById(R.id.cancel_button).setOnClickListener(v -> cancelSet());
        finishButton.setOnClickListener(v -> finishSet());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancelSet();
            }
        });

        showState(STATE_IDLE);

        // TODO: use the real bracelet when Person 4's BLE code sends these packets
        sensor = new FakeSensor(this);
        sensor.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensor != null) {
            sensor.stop();
        }
    }

    @Override
    public void onState(int state) {
        showState(state);

        if (state == STATE_MOVING && !restStopped) {
            // The set started: stop the rest timer of the previous set
            savedRestStart = WorkoutSession.restStartElapsed;
            savedRestSet = WorkoutSession.restAfterSet;
            WorkoutSession.stopRest();
            restStopped = true;
        } else if (state == STATE_STOPPED) {
            finishButton.setEnabled(true);
            finishHint.setText("Set over. Tap Finish to save it.");
        } else if (state == STATE_CANCELLED) {
            reps.clear();
            repList.removeAllViews();
            updateStats();
            restoreRest();
            finishHint.setText("Not a real set. Start moving again when you are ready.");
        }
    }

    @Override
    public void onRep(RepData rep) {
        reps.add(rep);
        WorkoutUi.addRepRow(repList, reps.size(), rep, R.color.gym_violet);
        repScroll.post(() -> repScroll.fullScroll(ScrollView.FOCUS_DOWN));
        updateStats();
    }

    @Override
    public void onHeartRate(int bpm) {
        bpms.add(bpm);
        graph.setValues(bpms);
        currentBpm.setText(bpm + " BPM");

        int total = 0;
        for (int value : bpms) {
            total += value;
        }
        avgBpm.setText("Avg " + (total / bpms.size()));
    }

    private void updateStats() {
        long tut = 0;
        int rom = 0;
        for (RepData rep : reps) {
            tut += rep.getTutMs();
            rom += rep.rom;
        }
        repsCount.setText(String.valueOf(reps.size()));
        tutValue.setText(WorkoutUi.formatTut(tut));
        romValue.setText(reps.isEmpty() ? "—" : (rom / reps.size()) + "%");
    }

    private void showState(int state) {
        switch (state) {
            case STATE_MOVING:
                setChip("● MOVING", R.color.gym_accent, R.color.gym_accent_tint);
                break;
            case STATE_STOPPED:
                setChip("● SET DONE", R.color.gym_cyan, R.color.gym_cyan_tint);
                break;
            case STATE_CANCELLED:
                setChip("● CANCELLED", R.color.gym_coral, R.color.gym_coral_tint);
                break;
            default:
                setChip("● WAITING", R.color.gym_text_secondary, R.color.gym_surface_high);
        }
    }

    private void setChip(String text, int colorRes, int tintRes) {
        stateChip.setText(text);
        stateChip.setTextColor(WorkoutUi.color(this, colorRes));
        WorkoutUi.tint(stateChip, tintRes);
    }

    // Put the rest timer back like before this set
    private void restoreRest() {
        if (restStopped && savedRestSet != null) {
            savedRestSet.restSec = -1;
            WorkoutSession.restStartElapsed = savedRestStart;
            WorkoutSession.restAfterSet = savedRestSet;
        }
        restStopped = false;
    }

    private void finishSet() {
        if (reps.isEmpty()) {
            return;
        }
        sensor.stop();
        set.repList = new ArrayList<>(reps);
        set.bpmList = new ArrayList<>(bpms);
        set.reps = reps.size();
        set.done = true;
        WorkoutSession.startRest(set);
        finish();
    }

    private void cancelSet() {
        sensor.stop();
        restoreRest();
        finish();
    }
}
