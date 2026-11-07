package com.example.smartgymtracker;

import java.util.ArrayList;
import java.util.List;

// One exercise inside a workout, with its sets
public class ExerciseEntry {

    public String name;
    public String equipment;  // "Cable" or "Machine"
    public String group;      // "Chest", "Back", ...
    public List<SetEntry> sets = new ArrayList<>();

    public ExerciseEntry(String name, String equipment, String group) {
        this.name = name;
        this.equipment = equipment;
        this.group = group;
    }

    public int getDoneCount() {
        int count = 0;
        for (SetEntry set : sets) {
            if (set.done) {
                count++;
            }
        }
        return count;
    }

    // Index of the first set that is not done yet (-1 if all are done)
    public int getCurrentSetIndex() {
        for (int i = 0; i < sets.size(); i++) {
            if (!sets.get(i).done) {
                return i;
            }
        }
        return -1;
    }
}
