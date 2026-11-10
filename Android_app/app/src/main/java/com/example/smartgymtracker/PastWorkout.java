package com.example.smartgymtracker;

import java.util.ArrayList;
import java.util.List;

// A finished workout, shown in History
public class PastWorkout {

    public String name;
    public long startTime;
    public long durationSec;
    public List<ExerciseEntry> exercises = new ArrayList<>();
    public int hrRecovery;    // BPM lost in the first minute after the workout
    public String prText = ""; // e.g. "210 lbs Chest Press", empty if no PR

    public int getTotalVolume() {
        int volume = 0;
        for (ExerciseEntry exercise : exercises) {
            for (SetEntry set : exercise.sets) {
                if (set.done) {
                    volume += set.weight * set.reps;
                }
            }
        }
        return volume;
    }

    public int getMaxBpm() {
        int max = 0;
        for (ExerciseEntry exercise : exercises) {
            for (SetEntry set : exercise.sets) {
                max = Math.max(max, set.getMaxBpm());
            }
        }
        return max;
    }
}
