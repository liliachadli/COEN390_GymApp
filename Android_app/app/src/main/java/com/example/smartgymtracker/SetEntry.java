package com.example.smartgymtracker;

import java.util.ArrayList;
import java.util.List;

// One set of an exercise
public class SetEntry {

    public int weight;     // lbs
    public int reps;
    public boolean done = false;
    public long restSec = -1;  // rest taken after this set (-1 = not known yet)
    public List<RepData> repList = new ArrayList<>();
    public List<Integer> bpmList = new ArrayList<>();

    public SetEntry(int weight) {
        this.weight = weight;
    }

    public long getTotalTutMs() {
        long total = 0;
        for (RepData rep : repList) {
            total += rep.getTutMs();
        }
        return total;
    }

    public int getAverageRom() {
        if (repList.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (RepData rep : repList) {
            total += rep.rom;
        }
        return total / repList.size();
    }

    public int getAverageBpm() {
        if (bpmList.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int bpm : bpmList) {
            total += bpm;
        }
        return total / bpmList.size();
    }

    public int getMaxBpm() {
        int max = 0;
        for (int bpm : bpmList) {
            max = Math.max(max, bpm);
        }
        return max;
    }
}
