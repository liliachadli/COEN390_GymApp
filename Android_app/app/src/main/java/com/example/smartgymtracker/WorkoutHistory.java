package com.example.smartgymtracker;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

// Finished workouts, newest first.

public class WorkoutHistory {

    public static List<PastWorkout> workouts = new ArrayList<>();
    private static final WorkoutRepository repo = new WorkoutRepository();

    private static int loadGen = 0;
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
                    SetEntry setCopy = new SetEntry(set.weight);

                    setCopy.reps = set.reps;
                    setCopy.done = set.done;
                    setCopy.restSec = set.restSec;
                    setCopy.savedAverageBpm = set.savedAverageBpm;
                    setCopy.savedMaxBpm = set.savedMaxBpm;

                    setCopy.repList = new ArrayList<>();

                    for(RepData rep : set.repList){
                        RepData repCopy = new RepData(rep.rom, rep.concMs, rep.eccMs);
                        setCopy.repList.add(repCopy);
                    }

                    setCopy.bpmList = new ArrayList<>(set.bpmList);

                    copy.sets.add(setCopy);
                }
            }
            if (!copy.sets.isEmpty()) {
                past.exercises.add(copy);
                checkForRecord(past, copy);
            }
        }

        int recoveryBPM = 0;
        for(Integer bpm:WorkoutSession.recoveryBPMs){
            recoveryBPM+=bpm;
        }
        recoveryBPM/=WorkoutSession.recoveryBPMs.size();
        past.hrRecovery = recoveryBPM;

        loadGen++;
        workouts.add(0, past);
        return past;
    }

    public static void loadWorkouts(Runnable onComplete) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return;
        }

        String requestedUid = user.getUid();
        int reqGen = ++loadGen;

        repo.getWorkouts(new WorkoutRepository.getWorkoutsCallback() {
            @Override
            public void onSuccess(List<PastWorkout> savedWorkouts) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                if(currentUser == null || !currentUser.getUid().equals(requestedUid) || loadGen != reqGen){
                    return;
                }
                workouts.clear();
                workouts.addAll(savedWorkouts);

                if(onComplete != null){
                    onComplete.run();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("WorkoutHistory", "Failed to load workouts", e);

                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if(currentUser == null || !currentUser.getUid().equals(requestedUid) || loadGen != reqGen){
                    return;
                }
                if(onComplete != null){
                    onComplete.run();
                }

            }
        });
    }

    // Clears the workout history
    public static void clearHistory(){
        loadGen++;
        workouts.clear();
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
                    if (setIndex >= 0 && setIndex < entry.sets.size()) {
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

}
