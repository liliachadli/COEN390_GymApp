package com.example.smartgymtracker;

// All exercises the user can add (APP-1.4 / APP-1.5).
// Each exercise exists in a cable and/or machine version.
public class WorkoutOptions {

    public static final String CABLE = "Cable";
    public static final String MACHINE = "Machine";

    // Muscle groups, in the order they are shown
    public static final String[] MUSCLE_GROUPS = {
            "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs"
    };

    // The 3 arrays below are in the same order: one line = one choice
    public static final String[] EXERCISES = {
            "Chest Press", "Chest Press", "Pec Deck Fly", "Cable Crossover",
            "Lat Pulldown", "Lat Pulldown", "Seated Row", "Seated Row",
            "Shoulder Press", "Lateral Raise", "Lateral Raise",
            "Cable Biceps Curl", "Preacher Curl",
            "Triceps Pushdown", "Overhead Cable Extension",
            "Leg Press", "Leg Extension", "Leg Curl"
    };

    public static final String[] EQUIPMENT = {
            CABLE, MACHINE, MACHINE, CABLE,
            CABLE, MACHINE, CABLE, MACHINE,
            MACHINE, CABLE, MACHINE,
            CABLE, MACHINE,
            CABLE, CABLE,
            MACHINE, MACHINE, MACHINE
    };

    public static final String[] GROUPS = {
            "Chest", "Chest", "Chest", "Chest",
            "Back", "Back", "Back", "Back",
            "Shoulders", "Shoulders", "Shoulders",
            "Biceps", "Biceps",
            "Triceps", "Triceps",
            "Legs", "Legs", "Legs"
    };

    public static String getGroupFor(String exercise) {
        for (int i = 0; i < EXERCISES.length; i++) {
            if (EXERCISES[i].equals(exercise)) {
                return GROUPS[i];
            }
        }
        return "Chest";
    }

    // Body drawing with the muscle highlighted (style C5)
    public static int getIconForGroup(String group) {
        switch (group) {
            case "Chest":
                return R.drawable.ic_muscle_chest;
            case "Back":
                return R.drawable.ic_muscle_back;
            case "Shoulders":
                return R.drawable.ic_muscle_shoulders;
            case "Biceps":
                return R.drawable.ic_muscle_biceps;
            case "Triceps":
                return R.drawable.ic_muscle_triceps;
            default:
                return R.drawable.ic_muscle_legs;
        }
    }

    // Main color of each muscle group
    public static int getColorForGroup(String group) {
        switch (group) {
            case "Chest":
                return R.color.gym_coral;
            case "Back":
                return R.color.gym_cyan;
            case "Shoulders":
                return R.color.gym_amber;
            case "Biceps":
                return R.color.gym_accent;
            case "Triceps":
                return R.color.gym_violet;
            default:
                return R.color.gym_pink;
        }
    }

    // Dark version of the color, used behind the drawing
    public static int getTintForGroup(String group) {
        switch (group) {
            case "Chest":
                return R.color.gym_coral_tint;
            case "Back":
                return R.color.gym_cyan_tint;
            case "Shoulders":
                return R.color.gym_amber_tint;
            case "Biceps":
                return R.color.gym_accent_tint;
            case "Triceps":
                return R.color.gym_violet_tint;
            default:
                return R.color.gym_pink_tint;
        }
    }
}
