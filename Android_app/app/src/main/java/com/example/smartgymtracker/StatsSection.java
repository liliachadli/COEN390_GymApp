package com.example.smartgymtracker;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// Stats tab of History: month summary, training calendar, weekly volume, heart rate, muscles
public class StatsSection {

    private static final int WEEKS = 8;
    private static final int BPM_WORKOUTS = 10;

    public static void show(LinearLayout container) {
        Context context = container.getContext();
        container.removeAllViews();
        List<PastWorkout> workouts = WorkoutHistory.workouts;

        String monthName = new java.text.SimpleDateFormat("MMMM", Locale.ENGLISH).format(Calendar.getInstance().getTime());
        container.addView(label(context, "THIS MONTH · " + monthName.toUpperCase()));

        // 1. Month summary, compared with last month
        WorkoutStats.Month now = WorkoutStats.month(workouts, 0);
        WorkoutStats.Month before = WorkoutStats.month(workouts, 1);
        LinearLayout row1 = row(context);
        row1.addView(summaryCard(context, now.workouts, "", "Workouts",
                WorkoutStats.change(now.workouts, before.workouts), R.color.gym_accent), weight());
        row1.addView(gap(context));
        row1.addView(summaryCard(context, (int) (now.seconds / 60), " min", "Training time",
                WorkoutStats.change(now.seconds, before.seconds), R.color.gym_cyan), weight());
        container.addView(row1);

        LinearLayout row2 = row(context);
        ((LinearLayout.LayoutParams) row2.getLayoutParams()).topMargin = WorkoutUi.dp(context, 10);
        row2.addView(summaryCard(context, (int) displayWeight(now.volume), " " + Units.label(), "Volume lifted",
                WorkoutStats.change(now.volume, before.volume), R.color.gym_violet), weight());
        row2.addView(gap(context));
        row2.addView(summaryCard(context, now.avgBpm, " bpm", "Avg heart rate",
                "", R.color.gym_coral), weight());
        container.addView(row2);

        // 2. Training calendar
        LinearLayout calendar = card(context, "Training calendar", "Brighter = more volume that day");
        calendar.addView(new MonthCalendarView(context, WorkoutStats.volumePerDay(workouts)));
        container.addView(calendar);

        // 3. Volume per week
        LinearLayout weekly = card(context, "Weekly volume", Units.label() + " lifted per week");
        long[] weeks = WorkoutStats.volumePerWeek(workouts, WEEKS);
        for (int i = 0; i < weeks.length; i++) {
            weeks[i] = displayWeight(weeks[i]);
        }
        weekly.addView(new BarChartView(context, weeks, WorkoutStats.weekLabels(WEEKS), R.color.gym_accent),
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, WorkoutUi.dp(context, 170)));
        container.addView(weekly);

        // 4. Heart rate trend
        LinearLayout heart = card(context, "Heart rate", "Last " + BPM_WORKOUTS + " workouts with the wearable");
        List<int[]> bpm = WorkoutStats.heartRates(workouts, BPM_WORKOUTS);
        if (bpm.size() < 2) {
            heart.addView(note(context, "Wear the heart rate tracker in 2 workouts to see your trend here."));
        } else {
            LinearLayout legend = row(context);
            legend.addView(legendItem(context, "Average", R.color.gym_coral));
            legend.addView(legendItem(context, "Max", R.color.gym_amber));
            heart.addView(legend);
            heart.addView(new BpmChartView(context, bpm),
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, WorkoutUi.dp(context, 150)));
        }
        container.addView(heart);

        // 5. Sets per muscle group this month
        LinearLayout muscles = card(context, "Muscles this month", "Finished sets per muscle group");
        int[] sets = WorkoutStats.setsPerGroup(workouts);
        int maxSets = 1;
        for (int s : sets) {
            maxSets = Math.max(maxSets, s);
        }
        for (int g = 0; g < sets.length; g++) {
            muscles.addView(muscleRow(context, WorkoutOptions.MUSCLE_GROUPS[g], sets[g], maxSets, g));
        }
        container.addView(muscles);

        Effects.cascade(container);
    }

    // ---------- pieces ----------

    private static View summaryCard(Context context, int value, String unit, String name, String change, int colorRes) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_default);
        int pad = WorkoutUi.dp(context, 14);
        card.setPadding(pad, pad, pad, pad);

        View dot = new View(context);
        dot.setBackgroundResource(R.drawable.bg_pill);
        WorkoutUi.tint(dot, colorRes);
        card.addView(dot, new LinearLayout.LayoutParams(WorkoutUi.dp(context, 22), WorkoutUi.dp(context, 4)));

        TextView number = new TextView(context);
        number.setTextColor(WorkoutUi.color(context, R.color.gym_text));
        number.setTextSize(24);
        number.setTypeface(android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.NORMAL));
        number.setPadding(0, WorkoutUi.dp(context, 8), 0, 0);
        number.setMaxLines(1);
        card.addView(number);
        countUp(number, value, unit);

        TextView title = new TextView(context);
        title.setText(name);
        title.setTextColor(WorkoutUi.color(context, R.color.gym_text_secondary));
        title.setTextSize(12);
        card.addView(title);

        if (!change.isEmpty()) {
            TextView chip = new TextView(context);
            chip.setText(change + " vs last month");
            boolean down = change.startsWith("↓");
            chip.setTextColor(WorkoutUi.color(context, down ? R.color.gym_coral : R.color.gym_accent));
            chip.setTextSize(11);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.setPadding(0, WorkoutUi.dp(context, 6), 0, 0);
            card.addView(chip);
        }
        return card;
    }

    // White card with a title and a small subtitle
    private static LinearLayout card(Context context, String title, String subtitle) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_default);
        int pad = WorkoutUi.dp(context, 16);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = WorkoutUi.dp(context, 14);
        card.setLayoutParams(lp);

        TextView titleText = new TextView(context);
        titleText.setText(title);
        titleText.setTextColor(WorkoutUi.color(context, R.color.gym_text));
        titleText.setTextSize(17);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(titleText);

        TextView subtitleText = new TextView(context);
        subtitleText.setText(subtitle);
        subtitleText.setTextColor(WorkoutUi.color(context, R.color.gym_text_secondary));
        subtitleText.setTextSize(12);
        subtitleText.setPadding(0, 0, 0, WorkoutUi.dp(context, 12));
        card.addView(subtitleText);
        return card;
    }

    private static View muscleRow(Context context, String group, int sets, int maxSets, int index) {
        LinearLayout row = row(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, WorkoutUi.dp(context, 5), 0, WorkoutUi.dp(context, 5));

        TextView name = new TextView(context);
        name.setText(group);
        name.setTextColor(WorkoutUi.color(context, R.color.gym_text));
        name.setTextSize(13);
        row.addView(name, new LinearLayout.LayoutParams(WorkoutUi.dp(context, 78), LinearLayout.LayoutParams.WRAP_CONTENT));

        // Track + colored bar that grows from the left
        LinearLayout track = new LinearLayout(context);
        track.setBackgroundResource(R.drawable.bg_pill);
        WorkoutUi.tint(track, R.color.gym_surface_high);
        View bar = new View(context);
        bar.setBackgroundResource(R.drawable.bg_pill);
        WorkoutUi.tint(bar, WorkoutOptions.getColorForGroup(group));
        float share = sets == 0 ? 0f : Math.max(0.04f, (float) sets / maxSets);
        track.addView(bar, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, share));
        track.addView(new Space(context), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f - share));
        row.addView(track, new LinearLayout.LayoutParams(0, WorkoutUi.dp(context, 10), 1f));
        bar.setPivotX(0f);
        bar.setScaleX(0f);
        bar.animate().scaleX(1f).setStartDelay(300 + 80L * index).setDuration(700)
                .setInterpolator(new DecelerateInterpolator(2f)).start();

        TextView count = new TextView(context);
        count.setText(String.valueOf(sets));
        count.setTextColor(WorkoutUi.color(context, R.color.gym_text_secondary));
        count.setTextSize(13);
        count.setGravity(Gravity.END);
        row.addView(count, new LinearLayout.LayoutParams(WorkoutUi.dp(context, 36), LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private static View legendItem(Context context, String text, int colorRes) {
        TextView item = new TextView(context);
        item.setText("●  " + text + "     ");
        item.setTextColor(WorkoutUi.color(context, colorRes));
        item.setTextSize(12);
        item.setTypeface(null, android.graphics.Typeface.BOLD);
        return item;
    }

    private static TextView label(Context context, String text) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextColor(WorkoutUi.color(context, R.color.gym_text_secondary));
        label.setTextSize(12);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setLetterSpacing(0.12f);
        label.setPadding(0, WorkoutUi.dp(context, 4), 0, WorkoutUi.dp(context, 10));
        return label;
    }

    private static TextView note(Context context, String text) {
        TextView note = new TextView(context);
        note.setText(text);
        note.setTextColor(WorkoutUi.color(context, R.color.gym_text_muted));
        note.setTextSize(13);
        note.setPadding(0, WorkoutUi.dp(context, 8), 0, WorkoutUi.dp(context, 8));
        return note;
    }

    private static LinearLayout row(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private static LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private static View gap(Context context) {
        View space = new View(context);
        space.setLayoutParams(new LinearLayout.LayoutParams(WorkoutUi.dp(context, 10), 1));
        return space;
    }

    // Number counts up from 0 when the card appears
    private static void countUp(TextView text, int value, String unit) {
        if (value <= 0) {
            text.setText("0" + unit);
            return;
        }
        ValueAnimator animator = ValueAnimator.ofInt(0, value);
        animator.setDuration(1000);
        animator.setInterpolator(new DecelerateInterpolator(2f));
        animator.addUpdateListener(a -> text.setText(String.format(Locale.ENGLISH, "%,d", (int) a.getAnimatedValue()) + unit));
        animator.start();
    }

    // lbs -> the user's unit
    private static long displayWeight(long lbs) {
        return Units.useKg ? Math.round(lbs * 0.45359237) : lbs;
    }

    // 12400 -> "12.4k"
    public static String compact(long value) {
        if (value >= 1000) {
            return String.format(Locale.ENGLISH, "%.1fk", value / 1000.0);
        }
        return String.valueOf(value);
    }

    public static float sp(Context context, float value) {
        return value * context.getResources().getDisplayMetrics().scaledDensity;
    }
}
