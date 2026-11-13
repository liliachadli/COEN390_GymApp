package com.example.smartgymtracker;

import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

// 1.1.2 · Set details: live reps, ROM, TUT and heart rate during one set (APP-1.8)
public class SetDetailsActivity extends BluetoothActivity implements SensorListener {

    private SetEntry set;
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
    private int lastState = STATE_IDLE;
    private float maxROM = 0;
    private int lastGoodBPM = 0;
    private int recoveryBPM = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_details);

        bleHelp = BluetoothHelper.getInstance(getApplicationContext(), this, "SetDetailsActivity");

        int exerciseIndex = getIntent().getIntExtra(WorkoutActivity.EXTRA_EXERCISE_INDEX, 0);
        int setIndex = getIntent().getIntExtra(WorkoutActivity.EXTRA_SET_INDEX, 0);
        ExerciseEntry exercise = WorkoutSession.exercises.get(exerciseIndex);
        set = exercise.sets.get(setIndex);

        ((TextView) findViewById(R.id.set_title)).setText(exercise.name);
        ((TextView) findViewById(R.id.set_subtitle)).setText("Set " + (setIndex + 1) + " · " + Units.format(set.weight));

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
    }

    @Override
    protected void onResume(){
        super.onResume();
        bleHelp.changeActivity(this, "SetDetailsActivity");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(DeviceState.heartConnected)
            bleHelp.turnOffNotifications(0);
        if(DeviceState.motionConnected)
            bleHelp.turnOffNotifications(1);
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
        set.repList = new ArrayList<>(reps);
        set.bpmList = new ArrayList<>(bpms);
        set.reps = reps.size();
        set.done = true;
        if(recoveryBPM > 0)
            WorkoutSession.recoveryBPMs.add(recoveryBPM);
        WorkoutSession.startRest(set);
        finish();
    }

    private void cancelSet() {
        restoreRest();
        finish();
    }

    @Override
    public void onNotifiedHeartRate(byte[] value){
        ByteBuffer wrapped = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
        if(value.length == 10){
            int version = wrapped.get();
            int type = wrapped.get();
            int sequence = wrapped.getShort();
            int timeMs = wrapped.getInt();
            int bpm = wrapped.get();
            int quality = wrapped.get();
            if((quality == 0 && bpm>0) || bpm < 0)
                bpm = lastGoodBPM;
            lastGoodBPM = bpm;
            onHeartRate(bpm);
            if(bpm > 0)
                recoveryBPM = bpm;
        }
        else{
            Log.e("SetDetailsActivity", "Bluetooth Notification Error: Packet is the wrong size.");
        }
    }
    @Override
    public void onNotifiedReps(byte[] value){
        ByteBuffer wrapped = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
        if(value.length == 9){  // State Packet
            int version = wrapped.get();
            int type = wrapped.get();
            int sequence = wrapped.getShort();
            int timeMs = wrapped.getInt();
            int state = wrapped.get();
            if(lastState == STATE_STOPPED){
                return;
            }
            if((state == 0 || state == 1) && lastState == STATE_IDLE) {
                onState(STATE_IDLE);
            }
            else if((state == 0 || state == 1) && lastState == STATE_MOVING && !reps.isEmpty()){
                lastState = STATE_STOPPED;
                onState(STATE_STOPPED);
            }
            else if((state == 0 || state == 1) && lastState == STATE_MOVING){
                lastState = STATE_CANCELLED;
                onState(STATE_CANCELLED);
            }
            else if (state == 2 || state == 3) {
                lastState = STATE_MOVING;
                onState(STATE_MOVING);
            }
        }
        else if(value.length == 22){    // Rep Packet
            int version = wrapped.get();
            int type = wrapped.get();
            int sequence = wrapped.getShort();
            int timeMs = wrapped.getInt();
            int repCount = wrapped.getShort();
            int validRep = wrapped.get();
            int state = wrapped.get();
            float ROM = wrapped.getFloat();
            int liftMs = wrapped.getShort();
            int lowerMs = wrapped.getShort();
            if(lastState == STATE_STOPPED){
                return;
            }
            RepData rep = new RepData(calculateROMPercentage(ROM), liftMs, lowerMs);
            onRep(rep);
        }
        else{
            Log.e("SetDetailsActivity", "Bluetooth Notification Error: Packet is the wrong size.");
        }
    }

    public int calculateROMPercentage(float ROM){
        float lastMaxROM = maxROM;
        if(ROM > maxROM) {
            maxROM = ROM;
            for(RepData rep: reps){
                rep.rom = (int) (lastMaxROM*(float)rep.rom/maxROM);
            }
        }

        return (int)(100*(ROM/maxROM));
    }

    @Override
    public void showStatus(String text){
        Log.i("SetDetailsActivity", "BLE status: " + text);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
