package com.example.smartgymtracker;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

// A saved list of exercises the user can start in one tap
public class Template {

    // Colors a template can have (saved by name, not by R.color id, because ids change between builds)
    public static final String[] COLORS = {"lime", "coral", "cyan", "amber", "violet", "pink"};

    public String id;          // Firestore document id
    public String name;
    public String color = "lime";
    public List<String> exercises = new ArrayList<>();
    public List<String> equipment = new ArrayList<>();
    public List<Integer> sets = new ArrayList<>();
    public long createdAt;     // keeps the templates in the order they were made

    public Template() {
        id = "t" + System.currentTimeMillis();
        createdAt = System.currentTimeMillis();
    }

    public Template(String id, String name, String color, String[] exercises, String[] equipment, int[] sets) {
        this.id = id;
        this.name = name;
        this.color = color;
        for (int i = 0; i < exercises.length; i++) {
            this.exercises.add(exercises[i]);
            this.equipment.add(equipment[i]);
            this.sets.add(sets[i]);
        }
    }

    public static int colorRes(String color) {
        switch (color) {
            case "coral":
                return R.color.gym_coral;
            case "cyan":
                return R.color.gym_cyan;
            case "amber":
                return R.color.gym_amber;
            case "violet":
                return R.color.gym_violet;
            case "pink":
                return R.color.gym_pink;
            default:
                return R.color.gym_accent;
        }
    }

    public int getColorRes() {
        return colorRes(color);
    }

    // "Chest · Shoulders · Triceps", from the exercises
    public String getMuscles() {
        List<String> groups = new ArrayList<>();
        for (String exercise : exercises) {
            String group = WorkoutOptions.getGroupFor(exercise);
            if (group != null && !groups.contains(group)) {
                groups.add(group);
            }
        }
        return groups.isEmpty() ? "No exercises" : TextUtils.join(" · ", groups);
    }

    public Template copy() {
        Template copy = new Template();
        copy.id = id;
        copy.name = name;
        copy.color = color;
        copy.createdAt = createdAt;
        copy.exercises.addAll(exercises);
        copy.equipment.addAll(equipment);
        copy.sets.addAll(sets);
        return copy;
    }

    // Given to a new account the first time (they can edit or delete them)
    public static List<Template> defaults() {
        List<Template> list = new ArrayList<>();
        list.add(new Template("push", "Push day", "coral",
                new String[]{"Chest Press", "Cable Crossover", "Shoulder Press", "Triceps Pushdown"},
                new String[]{"Machine", "Cable", "Machine", "Cable"},
                new int[]{3, 3, 3, 4}));
        list.add(new Template("pull", "Pull day", "cyan",
                new String[]{"Lat Pulldown", "Seated Row", "Cable Biceps Curl", "Preacher Curl"},
                new String[]{"Cable", "Machine", "Cable", "Machine"},
                new int[]{3, 3, 3, 3}));
        list.add(new Template("legs", "Leg day", "violet",
                new String[]{"Leg Press", "Leg Extension", "Leg Curl"},
                new String[]{"Machine", "Machine", "Machine"},
                new int[]{4, 3, 3}));
        for (int i = 0; i < list.size(); i++) {
            list.get(i).createdAt = i;
        }
        return list;
    }
}
