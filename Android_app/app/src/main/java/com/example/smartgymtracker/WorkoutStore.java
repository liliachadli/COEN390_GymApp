package com.example.smartgymtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

// Saves the workout in progress on the phone, so it comes back if the app is closed,
// killed by Android or crashes in the middle of a workout.
public class WorkoutStore {

    private static final String PREFS = "workout_in_progress";
    private static final String KEY = "workout";

    private static Context appContext;

    // Called once in GymApp
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static void save() {
        if (appContext == null || !WorkoutSession.active) {
            return;
        }
        try {
            prefs().edit().putString(KEY, toJson().toString()).apply();
        } catch (Exception e) {
            Log.e("WorkoutStore", "Could not save the workout", e);
        }
    }

    public static void clear() {
        if (appContext != null) {
            prefs().edit().remove(KEY).apply();
        }
    }

    // Puts the saved workout back in WorkoutSession (only if it belongs to this user)
    public static void restore(String userId) {
        if (appContext == null || WorkoutSession.active || userId == null) {
            return;
        }
        String text = prefs().getString(KEY, null);
        if (text == null) {
            return;
        }
        try {
            JSONObject json = new JSONObject(text);
            if (!userId.equals(json.optString("user"))) {
                return;   // someone else's workout
            }
            fromJson(json);
            save();
        } catch (Exception e) {
            // Broken data: forget it rather than crash
            Log.e("WorkoutStore", "Could not restore the workout", e);
            WorkoutSession.reset();
        }
    }

    // Who owns the saved workout (null if none)
    public static String savedUser() {
        if (appContext == null) {
            return null;
        }
        String text = prefs().getString(KEY, null);
        if (text == null) {
            return null;
        }
        try {
            return new JSONObject(text).optString("user", null);
        } catch (JSONException e) {
            return null;
        }
    }

    private static JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("user", WorkoutSession.userId);
        json.put("name", WorkoutSession.name);
        json.put("startTime", WorkoutSession.startTime);

        // Times are saved as clock time because elapsedRealtime is not saved anywhere
        long now = System.currentTimeMillis();
        long elapsedNow = SystemClock.elapsedRealtime();
        if (WorkoutSession.restStartElapsed > 0) {
            json.put("restStart", now - (elapsedNow - WorkoutSession.restStartElapsed));
        }

        JSONArray exercises = new JSONArray();
        for (ExerciseEntry exercise : WorkoutSession.exercises) {
            JSONObject e = new JSONObject();
            e.put("name", exercise.name);
            e.put("equipment", exercise.equipment);
            e.put("group", exercise.group);
            JSONArray sets = new JSONArray();
            for (SetEntry set : exercise.sets) {
                JSONObject s = new JSONObject();
                s.put("weight", set.weight);
                s.put("reps", set.reps);
                s.put("done", set.done);
                s.put("restSec", set.restSec);
                s.put("restRunning", set == WorkoutSession.restAfterSet);
                JSONArray reps = new JSONArray();
                for (RepData rep : set.repList) {
                    reps.put(new JSONArray().put(rep.rom).put(rep.concMs).put(rep.eccMs));
                }
                s.put("repList", reps);
                JSONArray bpms = new JSONArray();
                for (int bpm : set.bpmList) {
                    bpms.put(bpm);
                }
                s.put("bpmList", bpms);
                sets.put(s);
            }
            e.put("sets", sets);
            exercises.put(e);
        }
        json.put("exercises", exercises);

        JSONArray recovery = new JSONArray();
        if (WorkoutSession.recoveryBPMs != null) {
            for (int bpm : WorkoutSession.recoveryBPMs) {
                recovery.put(bpm);
            }
        }
        json.put("recovery", recovery);
        return json;
    }

    private static void fromJson(JSONObject json) throws JSONException {
        WorkoutSession.start(json.optString("name", "Workout"));
        WorkoutSession.userId = json.optString("user");

        long now = System.currentTimeMillis();
        long elapsedNow = SystemClock.elapsedRealtime();
        WorkoutSession.startTime = json.getLong("startTime");
        WorkoutSession.startElapsed = elapsedNow - (now - WorkoutSession.startTime);

        JSONArray exercises = json.getJSONArray("exercises");
        for (int i = 0; i < exercises.length(); i++) {
            JSONObject e = exercises.getJSONObject(i);
            ExerciseEntry exercise = new ExerciseEntry(e.getString("name"), e.getString("equipment"), e.optString("group"));
            JSONArray sets = e.getJSONArray("sets");
            for (int j = 0; j < sets.length(); j++) {
                JSONObject s = sets.getJSONObject(j);
                SetEntry set = new SetEntry(s.getInt("weight"));
                set.reps = s.optInt("reps");
                set.done = s.optBoolean("done");
                set.restSec = s.optLong("restSec", -1);
                JSONArray reps = s.optJSONArray("repList");
                for (int r = 0; reps != null && r < reps.length(); r++) {
                    JSONArray rep = reps.getJSONArray(r);
                    set.repList.add(new RepData(rep.getInt(0), rep.getLong(1), rep.getLong(2)));
                }
                JSONArray bpms = s.optJSONArray("bpmList");
                for (int b = 0; bpms != null && b < bpms.length(); b++) {
                    set.bpmList.add(bpms.getInt(b));
                }
                if (s.optBoolean("restRunning") && json.has("restStart")) {
                    WorkoutSession.restAfterSet = set;
                    WorkoutSession.restStartElapsed = elapsedNow - (now - json.getLong("restStart"));
                }
                exercise.sets.add(set);
            }
            WorkoutSession.exercises.add(exercise);
        }

        WorkoutSession.recoveryBPMs = new ArrayList<>();
        JSONArray recovery = json.optJSONArray("recovery");
        for (int i = 0; recovery != null && i < recovery.length(); i++) {
            WorkoutSession.recoveryBPMs.add(recovery.getInt(i));
        }
    }

    private static SharedPreferences prefs() {
        return appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
