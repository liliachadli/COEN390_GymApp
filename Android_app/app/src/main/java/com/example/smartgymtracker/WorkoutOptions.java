package com.example.smartgymtracker;

// Exercises and machines the user can choose (APP-1.4 / APP-1.5).
// Machine and cable exercises; the bracelet on the wrist tracks the reps.
public class WorkoutOptions {

    // Keys used to pass the choices from one screen to the next
    public static final String EXTRA_EXERCISE = "exercise";
    public static final String EXTRA_EQUIPMENT = "equipment";

    // Muscle groups, in the order they are shown
    public static final String[] MUSCLE_GROUPS = {
            "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs"
    };

    // All exercises. The 4 arrays below are in the same order.
    public static final String[] EXERCISES = {
            "Chest Press", "Pec Deck Fly", "Cable Crossover",
            "Lat Pulldown", "Seated Row",
            "Shoulder Press", "Lateral Raise",
            "Cable Biceps Curl", "Preacher Curl",
            "Triceps Pushdown", "Overhead Cable Extension",
            "Leg Press", "Leg Extension", "Leg Curl"
    };

    // Muscle group of each exercise
    public static final String[] EXERCISE_GROUPS = {
            "Chest", "Chest", "Chest",
            "Back", "Back",
            "Shoulders", "Shoulders",
            "Biceps", "Biceps",
            "Triceps", "Triceps",
            "Legs", "Legs", "Legs"
    };

    // Muscles worked, shown under the exercise name
    public static final String[] MUSCLES = {
            "Chest · Triceps", "Chest", "Chest · Shoulders",
            "Lats · Biceps", "Mid back · Biceps",
            "Shoulders · Triceps", "Side shoulders",
            "Biceps · Forearms", "Biceps",
            "Triceps", "Triceps",
            "Quads · Glutes", "Quads", "Hamstrings"
    };

    // Machines that can be used for each exercise
    public static final String[][] EQUIPMENT = {
            {"Chest Press Machine", "Cable Station"}, {"Pec Deck Machine"}, {"Cable Station"},
            {"Lat Pulldown Machine", "Cable Station"}, {"Seated Row Machine", "Cable Station"},
            {"Shoulder Press Machine"}, {"Lateral Raise Machine", "Cable Station"},
            {"Cable Station"}, {"Preacher Curl Machine"},
            {"Cable Station"}, {"Cable Station"},
            {"Leg Press Machine"}, {"Leg Extension Machine"}, {"Leg Curl Machine"}
    };

    // Position of an exercise in the arrays above (-1 if not found)
    private static int indexOf(String exercise) {
        for (int i = 0; i < EXERCISES.length; i++) {
            if (EXERCISES[i].equals(exercise)) {
                return i;
            }
        }
        return -1;
    }

    public static String[] getEquipmentFor(String exercise) {
        int i = indexOf(exercise);
        if (i == -1) {
            return new String[0];
        }
        return EQUIPMENT[i];
    }

    public static String getMusclesFor(String exercise) {
        int i = indexOf(exercise);
        if (i == -1) {
            return "";
        }
        return MUSCLES[i];
    }

    public static String getGroupFor(String exercise) {
        int i = indexOf(exercise);
        if (i == -1) {
            return "";
        }
        return EXERCISE_GROUPS[i];
    }

    // Number of exercises in a muscle group
    public static int countInGroup(String group) {
        int count = 0;
        for (String g : EXERCISE_GROUPS) {
            if (g.equals(group)) {
                count++;
            }
        }
        return count;
    }

    // Small body drawing with the muscle group highlighted
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

    public static String getEquipmentDescription(String equipment) {
        if (equipment.equals("Cable Station")) {
            return "Adjustable cable pulley";
        }
        return "Weight-stack machine";
    }
}
