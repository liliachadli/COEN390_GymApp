package com.example.smartgymtracker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

// Numbers for the Stats tab in History, computed from WorkoutHistory.workouts
public class WorkoutStats {

    // One month: how many workouts, time, volume (lbs) and average heart rate
    public static class Month {
        public int workouts;
        public long seconds;
        public long volume;
        public int avgBpm;
    }

    // monthsAgo = 0 for this month, 1 for last month
    public static Month month(List<PastWorkout> workouts, int monthsAgo) {
        Calendar start = startOfMonth(monthsAgo);
        Calendar end = startOfMonth(monthsAgo - 1);
        Month month = new Month();
        int bpmTotal = 0;
        int bpmCount = 0;
        for (PastWorkout workout : workouts) {
            if (workout.startTime >= start.getTimeInMillis() && workout.startTime < end.getTimeInMillis()) {
                month.workouts++;
                month.seconds += workout.durationSec;
                month.volume += workout.getTotalVolume();
                int bpm = averageBpm(workout);
                if (bpm > 0) {
                    bpmTotal += bpm;
                    bpmCount++;
                }
            }
        }
        month.avgBpm = bpmCount == 0 ? 0 : bpmTotal / bpmCount;
        return month;
    }

    // Average of the sets' average BPM (0 if the wearable was not used)
    public static int averageBpm(PastWorkout workout) {
        int total = 0;
        int count = 0;
        for (ExerciseEntry exercise : workout.exercises) {
            for (SetEntry set : exercise.sets) {
                int bpm = set.getAverageBpm();
                if (set.done && bpm > 0) {
                    total += bpm;
                    count++;
                }
            }
        }
        return count == 0 ? 0 : total / count;
    }

    // Volume (lbs) of each day of this month: index 0 = day 1
    public static long[] volumePerDay(List<PastWorkout> workouts) {
        Calendar start = startOfMonth(0);
        long[] days = new long[start.getActualMaximum(Calendar.DAY_OF_MONTH)];
        Calendar day = Calendar.getInstance();
        for (PastWorkout workout : workouts) {
            day.setTimeInMillis(workout.startTime);
            if (day.get(Calendar.YEAR) == start.get(Calendar.YEAR) && day.get(Calendar.MONTH) == start.get(Calendar.MONTH)) {
                // +1 so a workout with no weight still shows as a trained day
                days[day.get(Calendar.DAY_OF_MONTH) - 1] += workout.getTotalVolume() + 1;
            }
        }
        return days;
    }

    // Volume (lbs) of the last `count` weeks, oldest first. Weeks start on Monday.
    public static long[] volumePerWeek(List<PastWorkout> workouts, int count) {
        long[] weeks = new long[count];
        Calendar thisWeek = startOfWeek();
        long weekMs = 7L * 24 * 60 * 60 * 1000;
        for (PastWorkout workout : workouts) {
            long diff = thisWeek.getTimeInMillis() - workout.startTime;
            // 0 = this week, 1 = last week...
            int weeksAgo = diff <= 0 ? 0 : (int) ((diff + weekMs - 1) / weekMs);
            if (weeksAgo < count) {
                weeks[count - 1 - weeksAgo] += workout.getTotalVolume();
            }
        }
        return weeks;
    }

    // "Nov 3" for the Monday of each of those weeks
    public static String[] weekLabels(int count) {
        String[] labels = new String[count];
        Calendar week = startOfWeek();
        week.add(Calendar.WEEK_OF_YEAR, -(count - 1));
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (int i = 0; i < count; i++) {
            labels[i] = i == count - 1 ? "This wk" : months[week.get(Calendar.MONTH)] + " " + week.get(Calendar.DAY_OF_MONTH);
            week.add(Calendar.WEEK_OF_YEAR, 1);
        }
        return labels;
    }

    // Last workouts that used the wearable, oldest first: {avg BPM, max BPM}
    public static List<int[]> heartRates(List<PastWorkout> workouts, int count) {
        List<int[]> result = new ArrayList<>();
        // History is newest first
        for (PastWorkout workout : workouts) {
            int avg = averageBpm(workout);
            if (avg > 0) {
                result.add(0, new int[]{avg, workout.getMaxBpm()});
                if (result.size() == count) {
                    break;
                }
            }
        }
        return result;
    }

    // Finished sets per muscle group this month, in WorkoutOptions.MUSCLE_GROUPS order
    public static int[] setsPerGroup(List<PastWorkout> workouts) {
        int[] sets = new int[WorkoutOptions.MUSCLE_GROUPS.length];
        long start = startOfMonth(0).getTimeInMillis();
        for (PastWorkout workout : workouts) {
            if (workout.startTime < start) {
                continue;
            }
            for (ExerciseEntry exercise : workout.exercises) {
                for (int g = 0; g < sets.length; g++) {
                    if (WorkoutOptions.MUSCLE_GROUPS[g].equals(exercise.group)) {
                        sets[g] += exercise.getDoneCount();
                    }
                }
            }
        }
        return sets;
    }

    // "+20%" / "-5%" / "" compared with last month
    public static String change(long now, long before) {
        if (before == 0) {
            return now > 0 ? "New" : "";
        }
        long percent = Math.round(100.0 * (now - before) / before);
        return (percent >= 0 ? "↑ " : "↓ ") + Math.abs(percent) + "%";
    }

    private static Calendar startOfMonth(int monthsAgo) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.MONTH, -monthsAgo);
        return c;
    }

    private static Calendar startOfWeek() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        int daysSinceMonday = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        c.add(Calendar.DAY_OF_MONTH, -daysSinceMonday);
        return c;
    }
}
