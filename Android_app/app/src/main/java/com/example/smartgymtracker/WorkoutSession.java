package com.example.smartgymtracker;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

// The workout happening right now. Every workout screen reads and changes this.
public class WorkoutSession {

    public static boolean active = false;
    public static String name = "Workout";
    public static long startTime;     // System.currentTimeMillis() when it started
    public static long startElapsed;  // SystemClock.elapsedRealtime(), used by the timer
    public static List<ExerciseEntry> exercises = new ArrayList<>();

    // Rest timer: when the last set ended (0 = no rest running) and which set it was
    public static long restStartElapsed = 0;
    public static SetEntry restAfterSet = null;

    public static void start(String workoutName) {
        active = true;
        name = workoutName;
        startTime = System.currentTimeMillis();
        startElapsed = SystemClock.elapsedRealtime();
        exercises = new ArrayList<>();
        restStartElapsed = 0;
        restAfterSet = null;
    }

    public static ExerciseEntry addExercise(String exerciseName, String equipment, int setCount) {
        String group = WorkoutOptions.getGroupFor(exerciseName);
        ExerciseEntry exercise = new ExerciseEntry(exerciseName, equipment, group);

        // Start from the weight used last time, if there is one
        int weight = WorkoutHistory.getLastWeight(exerciseName, equipment);
        for (int i = 0; i < setCount; i++) {
            exercise.sets.add(new SetEntry(weight));
        }
        exercises.add(exercise);
        return exercise;
    }

    // A set just ended: the rest starts
    public static void startRest(SetEntry set) {
        restStartElapsed = SystemClock.elapsedRealtime();
        restAfterSet = set;
    }

    // The next set started moving: the rest ends and is saved on the previous set
    public static void stopRest() {
        if (restAfterSet != null && restStartElapsed > 0) {
            restAfterSet.restSec = (SystemClock.elapsedRealtime() - restStartElapsed) / 1000;
        }
        restStartElapsed = 0;
        restAfterSet = null;
    }

    public static void reset() {
        active = false;
        exercises = new ArrayList<>();
        restStartElapsed = 0;
        restAfterSet = null;
    }
}
