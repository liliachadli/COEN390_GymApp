package com.example.smartgymtracker;

// A saved list of exercises the user can start in one tap
public class Template {

    public String name;
    public String muscles;
    public int colorRes;
    public String[] exercises;
    public String[] equipment;
    public int[] sets;

    public Template(String name, String muscles, int colorRes, String[] exercises, String[] equipment, int[] sets) {
        this.name = name;
        this.muscles = muscles;
        this.colorRes = colorRes;
        this.exercises = exercises;
        this.equipment = equipment;
        this.sets = sets;
    }

    // The templates shown on the dashboard (fixed for now)
    public static final Template[] ALL = {
            new Template("Push day", "Chest · Shoulders · Triceps", R.color.gym_coral,
                    new String[]{"Chest Press", "Cable Crossover", "Shoulder Press", "Triceps Pushdown"},
                    new String[]{"Machine", "Cable", "Machine", "Cable"},
                    new int[]{3, 3, 3, 4}),
            new Template("Pull day", "Back · Biceps", R.color.gym_cyan,
                    new String[]{"Lat Pulldown", "Seated Row", "Cable Biceps Curl", "Preacher Curl"},
                    new String[]{"Cable", "Machine", "Cable", "Machine"},
                    new int[]{3, 3, 3, 3}),
            new Template("Leg day", "Quads · Hamstrings", R.color.gym_violet,
                    new String[]{"Leg Press", "Leg Extension", "Leg Curl"},
                    new String[]{"Machine", "Machine", "Machine"},
                    new int[]{4, 3, 3})
    };
}
