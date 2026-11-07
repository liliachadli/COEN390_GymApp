package com.example.smartgymtracker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

// Small helpers shared by the screens, so each screen stays short
public class WorkoutUi {

    public static int color(Context context, int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    // Paints a view that uses bg_tile / bg_pill / bg_dot / bg_bar_fill
    public static void tint(View view, int colorRes) {
        view.setBackgroundTintList(ColorStateList.valueOf(color(view.getContext(), colorRes)));
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    // Cable logo on a cyan tile, or machine logo on an amber tile
    public static void showEquipment(View tile, ImageView icon, String equipment) {
        boolean cable = equipment.equals(WorkoutOptions.CABLE);
        icon.setImageResource(cable ? R.drawable.ic_cable : R.drawable.ic_machine);
        icon.setImageTintList(ColorStateList.valueOf(
                color(icon.getContext(), cable ? R.color.gym_cyan : R.color.gym_amber)));
        tint(tile, cable ? R.color.gym_cyan_tint : R.color.gym_amber_tint);
    }

    public static int equipmentColor(String equipment) {
        return equipment.equals(WorkoutOptions.CABLE) ? R.color.gym_cyan : R.color.gym_amber;
    }

    // Body drawing of the muscle group on its tinted tile
    public static void showMuscle(View tile, ImageView icon, String group) {
        icon.setImageResource(WorkoutOptions.getIconForGroup(group));
        tint(tile, WorkoutOptions.getTintForGroup(group));
    }

    // Green "Motion" / "Heart" chips: colored when connected, grey when not
    public static void showDeviceChips(TextView motionChip, TextView heartChip) {
        showChip(motionChip, DeviceState.motionConnected, R.color.gym_accent, R.color.gym_accent_tint);
        showChip(heartChip, DeviceState.heartConnected, R.color.gym_coral, R.color.gym_coral_tint);
    }

    private static void showChip(TextView chip, boolean on, int colorRes, int tintRes) {
        Context context = chip.getContext();
        chip.setTextColor(color(context, on ? colorRes : R.color.gym_text_secondary));
        tint(chip, on ? tintRes : R.color.gym_surface_high);
    }

    // 75 -> "1:15"
    public static String formatTime(long seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    // 3100 -> "52 min" or "1 h 05"
    public static String formatDuration(long seconds) {
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + " min";
        }
        return (minutes / 60) + " h " + String.format("%02d", minutes % 60);
    }

    // 1250 -> "1.3 s"
    public static String formatTut(long ms) {
        return String.format("%.1f s", ms / 1000.0);
    }

    // Adds one row to a rep table (# · ROM bar · TUT · speed)
    public static void addRepRow(LinearLayout list, int number, RepData rep, int barColorRes) {
        View row = LayoutInflater.from(list.getContext()).inflate(R.layout.item_rep_row, list, false);
        ((TextView) row.findViewById(R.id.rep_number)).setText(String.valueOf(number));
        ((TextView) row.findViewById(R.id.rep_rom)).setText(rep.rom + "%");
        ((TextView) row.findViewById(R.id.rep_tut)).setText(formatTut(rep.getTutMs()));
        ((TextView) row.findViewById(R.id.rep_speed)).setText(String.format("%.2f", rep.getSpeed()));

        int rom = Math.max(0, Math.min(100, rep.rom));
        View bar = row.findViewById(R.id.rep_bar);
        bar.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, rom));
        row.findViewById(R.id.rep_bar_empty).setLayoutParams(
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 100 - rom));
        tint(bar, barColorRes);
        list.addView(row);
    }
}
