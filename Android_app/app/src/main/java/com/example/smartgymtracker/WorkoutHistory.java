package com.example.smartgymtracker;

import java.util.ArrayList;
import java.util.List;

// Finished workouts, newest first.
// TODO (Person 5): load and save these with Firestore. For now they stay in memory,
// with 2 example workouts so the History screens are not empty.
public class WorkoutHistory {

    public static List<PastWorkout> workouts = new ArrayList<>();

    static {
        long day = 24L * 60 * 60 * 1000;
        workouts.add(makeExample("Pull day", System.currentTimeMillis() - 2 * day, 47 * 60,
                new String[]{"Lat Pulldown", "Seated Row", "Cable Biceps Curl"},
                new String[]{WorkoutOptions.CABLE, WorkoutOptions.MACHINE, WorkoutOptions.CABLE},
                new int[]{172, 140, 40}, new int[]{10, 10, 12}));
        workouts.add(makeExample("Leg day", System.currentTimeMillis() - 4 * day, 55 * 60,
                new String[]{"Leg Press", "Leg Extension", "Leg Curl"},
                new String[]{WorkoutOptions.MACHINE, WorkoutOptions.MACHINE, WorkoutOptions.MACHINE},
                new int[]{320, 110, 95}, new int[]{10, 12, 12}));
    }

    // Turns the current workout into a PastWorkout and puts it at the top of the list
    public static PastWorkout saveCurrentWorkout() {
        PastWorkout past = new PastWorkout();
        past.name = WorkoutSession.name;
        past.startTime = WorkoutSession.startTime;
        past.durationSec = (System.currentTimeMillis() - WorkoutSession.startTime) / 1000;

        // Keep only the exercises with at least one finished set
        for (ExerciseEntry exercise : WorkoutSession.exercises) {
            ExerciseEntry copy = new ExerciseEntry(exercise.name, exercise.equipment, exercise.group);
            for (SetEntry set : exercise.sets) {
                if (set.done) {
                    copy.sets.add(set);
                }
            }
            if (!copy.sets.isEmpty()) {
                past.exercises.add(copy);
                checkForRecord(past, copy);
            }
        }

        // TODO (Person 4): real value from the heart-rate sensor after the workout
        past.hrRecovery = past.getMaxBpm() > 0 ? 30 : 0;

        workouts.add(0, past);
        return past;
    }

    // A PR is a weight higher than any weight used before for the same exercise
    private static void checkForRecord(PastWorkout past, ExerciseEntry exercise) {
        int best = 0;
        for (SetEntry set : exercise.sets) {
            best = Math.max(best, set.weight);
        }
        int previousBest = 0;
        for (PastWorkout old : workouts) {
            for (ExerciseEntry oldExercise : old.exercises) {
                if (oldExercise.name.equals(exercise.name)) {
                    for (SetEntry set : oldExercise.sets) {
                        previousBest = Math.max(previousBest, set.weight);
                    }
                }
            }
        }
        if (best > previousBest && past.prText.isEmpty()) {
            past.prText = best + " lbs " + exercise.name;
        }
    }

    // Weight used the last time for this exercise (0 if never done)
    public static int getLastWeight(String exercise, String equipment) {
        SetEntry set = getPreviousSet(exercise, equipment, 0);
        return set == null ? 0 : set.weight;
    }

    // Same set number in the last workout that had this exercise, or null
    public static SetEntry getPreviousSet(String exercise, String equipment, int setIndex) {
        for (PastWorkout workout : workouts) {
            for (ExerciseEntry entry : workout.exercises) {
                if (entry.name.equals(exercise) && entry.equipment.equals(equipment)) {
                    if (setIndex < entry.sets.size()) {
                        return entry.sets.get(setIndex);
                    }
                    return null;
                }
            }
        }
        return null;
    }

    public static int countThisWeek() {
        long weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        int count = 0;
        for (PastWorkout workout : workouts) {
            if (workout.startTime > weekAgo) {
                count++;
            }
        }
        return count;
    }

    // Example data: 3 sets per exercise with made-up reps and heart rate
    private static PastWorkout makeExample(String name, long startTime, long durationSec,
                                           String[] exercises, String[] equipment, int[] weights, int[] reps) {
        PastWorkout past = new PastWorkout();
        past.name = name;
        past.startTime = startTime;
        past.durationSec = durationSec;
        past.hrRecovery = 28;
        for (int e = 0; e < exercises.length; e++) {
            ExerciseEntry exercise = new ExerciseEntry(exercises[e], equipment[e],
                    WorkoutOptions.getGroupFor(exercises[e]));
            for (int s = 0; s < 3; s++) {
                SetEntry set = new SetEntry(weights[e]);
                set.done = true;
                set.reps = reps[e];
                set.restSec = (s < 2) ? 120 : -1;
                for (int r = 0; r < reps[e]; r++) {
                    int rom = 96 - r * 2 - s * 2;
                    set.repList.add(new RepData(rom, 600 + r * 40, 700 + r * 50));
                    set.bpmList.add(118 + r * 2 + s * 4);
                }
                exercise.sets.add(set);
            }
            past.exercises.add(exercise);
        }
        return past;
    }
}
