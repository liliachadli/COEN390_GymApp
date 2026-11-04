package com.example.smartgymtracker;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

// Small helpers shared by the workout setup screens, so each screen stays short.
public class WorkoutUi {

    // Fills the header at the top of the screen (step, progress bar, title, back button)
    public static void setupHeader(Activity activity, int step, String title, String subtitle) {
        TextView stepText = activity.findViewById(R.id.header_step);
        TextView titleText = activity.findViewById(R.id.header_title);
        TextView subtitleText = activity.findViewById(R.id.header_subtitle);
        stepText.setText("STEP " + step + " OF 3");
        titleText.setText(title);
        subtitleText.setText(subtitle);

        // Colour the progress bar: one part per step done
        int[] bars = {R.id.header_bar_1, R.id.header_bar_2, R.id.header_bar_3};
        for (int i = 0; i < bars.length; i++) {
            View bar = activity.findViewById(bars[i]);
            if (i < step) {
                bar.setBackgroundResource(R.drawable.bg_progress_on);
            } else {
                bar.setBackgroundResource(R.drawable.bg_progress_off);
            }
        }

        // Back arrow goes to the previous screen (hidden on the first step)
        ImageButton back = activity.findViewById(R.id.header_back);
        if (step == 1) {
            back.setVisibility(View.INVISIBLE);
        } else {
            back.setOnClickListener(v -> activity.finish());
        }
    }

    // Adds a muscle group title (with its body drawing) to the list
    public static void addSectionHeader(Activity activity, LinearLayout list, String title, int count, int iconRes) {
        View header = LayoutInflater.from(activity).inflate(R.layout.item_section_header, list, false);
        TextView titleText = header.findViewById(R.id.section_title);
        TextView countText = header.findViewById(R.id.section_count);
        ImageView icon = header.findViewById(R.id.section_icon);
        titleText.setText(title);
        if (count == 1) {
            countText.setText("1 EXERCISE");
        } else {
            countText.setText(count + " EXERCISES");
        }
        icon.setImageResource(iconRes);
        list.addView(header);
    }

    // Creates one choice card and adds it to the list
    public static View addOptionCard(Activity activity, LinearLayout list, String title, String subtitle, int iconRes) {
        View card = LayoutInflater.from(activity).inflate(R.layout.item_option_card, list, false);
        TextView titleText = card.findViewById(R.id.option_title);
        TextView subtitleText = card.findViewById(R.id.option_subtitle);
        ImageView icon = card.findViewById(R.id.option_icon);
        titleText.setText(title);
        subtitleText.setText(subtitle);
        icon.setImageResource(iconRes);
        list.addView(card);
        return card;
    }

    // Shows a card as selected (lime border + check mark) or not
    public static void setCardSelected(View card, boolean selected) {
        View check = card.findViewById(R.id.option_check);
        if (selected) {
            card.setBackgroundResource(R.drawable.bg_card_selected);
            check.setVisibility(View.VISIBLE);
        } else {
            card.setBackgroundResource(R.drawable.bg_card_default);
            check.setVisibility(View.INVISIBLE);
        }
    }

    // Fills one tracker row (icon, name, description, status)
    public static void setupDeviceRow(Activity activity, int rowId, int iconRes, String name, String description, String status) {
        View row = activity.findViewById(rowId);
        ImageView icon = row.findViewById(R.id.device_icon);
        TextView nameText = row.findViewById(R.id.device_name);
        TextView descriptionText = row.findViewById(R.id.device_description);
        TextView statusText = row.findViewById(R.id.device_status);
        icon.setImageResource(iconRes);
        nameText.setText(name);
        descriptionText.setText(description);
        statusText.setText(status);
    }
}
