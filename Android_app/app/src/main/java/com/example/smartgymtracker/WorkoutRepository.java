package com.example.smartgymtracker;

import android.util.Log;

import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkoutRepository {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    public WorkoutRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void saveWorkout(PastWorkout workout, saveWorkoutCallback callback) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            callback.onFailure(new IllegalStateException("No user is logged in"));
            return;
        }
        String uid = user.getUid();

        DocumentReference workoutReference = db.collection("users").document(uid).collection("workouts").document(String.valueOf(workout.startTime));

        // Create a batch to perform multiple writes
        WriteBatch batch = db.batch();
        int writeCount = 0;

        // Set the workout data in the batch
        Map<String, Object> workoutData = new HashMap<>();
        workoutData.put("name", workout.name);
        workoutData.put("startTime", workout.startTime);
        workoutData.put("durationSec", workout.durationSec);
        workoutData.put("hrRecovery", workout.hrRecovery);
        workoutData.put("prText", workout.prText);
        batch.set(workoutReference, workoutData);
        writeCount++;


        for (int i=0; i < workout.exercises.size(); i++) {
            ExerciseEntry exercise = workout.exercises.get(i);
            if (exercise.getDoneCount() == 0){
                continue;
            }

            // Create a document reference for the exercise in the workout
            DocumentReference exerciseReference = workoutReference.collection("exercises").document(String.valueOf(i + 1));

            // Set the exercise data in the batch
            Map<String, Object> exerciseData = new HashMap<>();
            exerciseData.put("exerciseNumber", i + 1);
            exerciseData.put("name", exercise.name);
            exerciseData.put("equipment", exercise.equipment);
            exerciseData.put("group", exercise.group);
            batch.set(exerciseReference, exerciseData);
            writeCount++;

            for (int j=0; j < exercise.sets.size(); j++) {
                // Skip sets that are not done
                SetEntry set = exercise.sets.get(j);
                if(!set.done) {
                    continue;
                }

                // Create a document reference for the set in the exercise
                DocumentReference setReference = exerciseReference.collection("sets").document(String.valueOf(j + 1));

                // Set the set data in the batch
                Map<String, Object> setData = new HashMap<>();
                setData.put("setNumber", j + 1);
                setData.put("weight", set.weight);
                setData.put("reps", set.reps);
                setData.put("restTime", set.restSec);
                setData.put("totalTUT", set.getTotalTutMs());
                setData.put("averageBpm", set.getAverageBpm());
                setData.put("maxBpm", set.getMaxBpm());
                setData.put("bpmList", set.bpmList);
                batch.set(setReference, setData);
                writeCount++;

                for (int k=0; k < set.repList.size(); k++) {

                    // Create a document reference for the rep in the set
                    DocumentReference repReference = setReference.collection("reps").document(String.valueOf(k+1));

                    // Set the rep data in the batch
                    RepData rep = set.repList.get(k);
                    Map<String, Object> repData = new HashMap<>();
                    repData.put("repNumber", k + 1);
                    repData.put("rom", rep.rom);
                    repData.put("concentricTime", rep.concMs);
                    repData.put("eccentricTime", rep.eccMs);
                    repData.put("tut", rep.getTutMs());
                    repData.put("speed", rep.getSpeed());
                    batch.set(repReference, repData);
                    writeCount++;
                }
            }
        }
        Log.d("WorkoutRepository", "writeCount: " + writeCount);

        if(writeCount > 400){
            callback.onFailure(new IllegalStateException("Workout has too many records to save"));
            return;
        }

        // Commit the batch to save the workout
        batch.commit()
                .addOnSuccessListener(unused -> {
                    Log.d("WorkoutRepository", "Workout saved successfully");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("WorkoutRepository", "Failed to save workout", e);
                    callback.onFailure(e);
                });
    }

    public void getWorkouts(getWorkoutsCallback callback){
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(new IllegalStateException("No user is logged in"));
            return;
        }
        String uid = user.getUid();
        db.collection("users").document(uid).collection("workouts").orderBy("startTime", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(workoutSnapshots -> {
                    List<PastWorkout> workouts = new ArrayList<>();
                    List<Task<?>> workoutTasks = new ArrayList<>();
                    for (DocumentSnapshot workoutDocument : workoutSnapshots){
                        PastWorkout workout = new PastWorkout();

                        // Retrieve name
                        workout.name = workoutDocument.getString("name");

                        // Retrieve start time
                        Long startTime = workoutDocument.getLong("startTime");
                        if(startTime != null){
                            workout.startTime = startTime;
                        }

                        // Retrieve duration
                        Long durationSec = workoutDocument.getLong("durationSec");
                        if(durationSec != null){
                            workout.durationSec = durationSec;
                        }

                        // Retrieve HR recovery
                        Long hrRecovery = workoutDocument.getLong("hrRecovery");
                        if(hrRecovery != null){
                            workout.hrRecovery = hrRecovery.intValue();
                        }

                        // Retrieve PR text
                        workout.prText = workoutDocument.getString("prText");

                        // Track completion of exercise, set and rep retrieval
                        TaskCompletionSource<Void> exerciseCompletion = new TaskCompletionSource<>();
                        workoutTasks.add(exerciseCompletion.getTask());

                        // Retrieve exercises
                        Task<QuerySnapshot> exerciseTask = workoutDocument.getReference().collection("exercises").orderBy("exerciseNumber").get()
                                        .addOnSuccessListener(exerciseSnapshots -> {
                                            List<Task<?>> setTasks = new ArrayList<>();
                                            for(DocumentSnapshot exerciseDocument : exerciseSnapshots){

                                                // Retrieve exercise name
                                                String name = exerciseDocument.getString("name");

                                                // Retrieve equipment
                                                String equipment = exerciseDocument.getString("equipment");

                                                // Retrieve group
                                                String group = exerciseDocument.getString("group");

                                                // Create exercise
                                                ExerciseEntry exercise = new ExerciseEntry(name, equipment, group);


                                                // Add the retrieved exercise to the workout
                                                workout.exercises.add(exercise);

                                                // Track completion of set and rep retrieval
                                                TaskCompletionSource<Void> setCompletion = new TaskCompletionSource<>();
                                                setTasks.add(setCompletion.getTask());

                                                // Retrieve sets
                                                exerciseDocument.getReference().collection("sets").orderBy("setNumber").get()
                                                        .addOnSuccessListener(setSnapshots -> {
                                                            List<Task<?>> repTasks = new ArrayList<>();
                                                            for(DocumentSnapshot setDocument : setSnapshots){

                                                                // Retrieve set weight
                                                                Long weight = setDocument.getLong("weight");
                                                                SetEntry set = new SetEntry(weight != null ? weight.intValue() : 0);

                                                                    // Retrieve set reps
                                                                    Long reps = setDocument.getLong("reps");
                                                                    if(reps != null){
                                                                        set.reps = reps.intValue();
                                                                    }

                                                                    // Retrieve set rest time
                                                                    Long restTime = setDocument.getLong("restTime");
                                                                    if(restTime != null){
                                                                        set.restSec = restTime;
                                                                    }

                                                                    // Retrieve boolean of whether set is done or not
                                                                    set.done = true;

                                                                    // Retrieve avg bpm
                                                                    Long avgBpm = setDocument.getLong("averageBpm");
                                                                    if(avgBpm != null) {
                                                                        set.savedAverageBpm = avgBpm.intValue();
                                                                    }

                                                                    // Retrieve max bpm
                                                                    Long maxBpm = setDocument.getLong("maxBpm");
                                                                    if(maxBpm != null) {
                                                                        set.savedMaxBpm = maxBpm.intValue();
                                                                    }

                                                                    // Retrieve bpm list
                                                                    List<Long> bpmList = (List<Long>) setDocument.get("bpmList");
                                                                    if(bpmList != null) {
                                                                        for(Long bpm : bpmList){
                                                                            if(bpm != null){
                                                                                set.bpmList.add(bpm.intValue());
                                                                            }
                                                                        }
                                                                    }

                                                                    // Add the retrieved set to the exercise
                                                                    exercise.sets.add(set);

                                                                    Task<QuerySnapshot> repTask =  setDocument.getReference().collection("reps").orderBy("repNumber").get();
                                                                            Task<Void> processedRepTask = repTask.continueWithTask(TaskExecutors.MAIN_THREAD, task -> {
                                                                                if (!task.isSuccessful()) {
                                                                                    throw new RuntimeException(task.getException());
                                                                                }
                                                                                for (DocumentSnapshot repDocument : task.getResult()) {
                                                                                    // Retrieve rep rom
                                                                                    Long rom = repDocument.getLong("rom");

                                                                                    // Retrieve rep concentric time
                                                                                    Long concentricTime = repDocument.getLong("concentricTime");

                                                                                    //Retrieve rep eccentric time
                                                                                    Long eccentricTime = repDocument.getLong("eccentricTime");


                                                                                    if (rom != null && concentricTime != null && eccentricTime != null) {
                                                                                        RepData rep = new RepData(rom.intValue(), concentricTime, eccentricTime);

                                                                                        // Add the retrieved rep to the set
                                                                                        set.repList.add(rep);
                                                                                    }
                                                                                }
                                                                                return Tasks.forResult(null);
                                                                            });

                                                                            repTasks.add(processedRepTask);


                                                            }
                                                            // Wait for all rep tasks to complete
                                                            Tasks.whenAll(repTasks)
                                                                    .addOnSuccessListener(unused -> {
                                                                        setCompletion.setResult(null);
                                                                    })
                                                                    .addOnFailureListener(e ->{
                                                                        setCompletion.setException(e);
                                                                    });
                                                        })
                                                        .addOnFailureListener(e ->{
                                                            setCompletion.setException(e);

                                                        });

                                            }
                                            // Wait for all set tasks to complete
                                            Tasks.whenAll(setTasks)
                                                    .addOnSuccessListener(unused -> {
                                                        exerciseCompletion.setResult(null);
                                                    })
                                                    .addOnFailureListener(e ->{
                                                        exerciseCompletion.setException(e);
                                                    });
                                        })

                                        .addOnFailureListener(e ->{
                                            exerciseCompletion.setException(e);

                                        });


                        workouts.add(workout);
                    }
                    // Wait for all workout tasks to complete
                    Tasks.whenAll(workoutTasks)
                            .addOnSuccessListener(unused -> {
                                Log.d("WorkoutRepository", "Retrieved workouts: " + workouts.size());
                                callback.onSuccess(workouts);
                            })
                            .addOnFailureListener(e -> {
                                callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e ->{
                    callback.onFailure(e);
                });
    }

    public interface saveWorkoutCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface getWorkoutsCallback {
        void onSuccess(List<PastWorkout> workouts);
        void onFailure(Exception e);
    }

}
