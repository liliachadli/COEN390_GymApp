package com.example.smartgymtracker;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

// Devices sheet: connect the 2 sensors of the bracelet (APP-1.6)
public class DeviceSheet {

    public static void show(Activity activity, Runnable onClose, BottomSheetDialog sheet, View view, BluetoothHelper bleHelp) {
        sheet.setContentView(view);
        ((View) view.getParent()).setBackgroundColor(Color.TRANSPARENT);

        View motion = view.findViewById(R.id.device_motion);
        View heart = view.findViewById(R.id.device_heart);
        setupCard(motion, true, bleHelp, activity);
        setupCard(heart, false, bleHelp, activity);

        view.findViewById(R.id.raw_data_link).setOnClickListener(v -> {
            sheet.dismiss();
            activity.startActivity(new Intent(activity, MainActivity.class));
        });
        view.findViewById(R.id.sheet_done).setOnClickListener(v -> sheet.dismiss());
        sheet.setOnDismissListener(d -> onClose.run());
        sheet.show();
    }

    private static void setupCard(View card, boolean isMotion, BluetoothHelper bleHelp, Activity activity) {
        TextView name = card.findViewById(R.id.device_name);
        TextView detail = card.findViewById(R.id.device_detail);
        ImageView icon = card.findViewById(R.id.device_icon);

        if (isMotion) {
            name.setText("Motion sensor");
            detail.setText("MPU6050 · reps, ROM, speed");
            icon.setImageResource(R.drawable.ic_pulse);
            WorkoutUi.tint(card.findViewById(R.id.device_tile), R.color.gym_accent_tint);
        } else {
            name.setText("Heart-rate sensor");
            detail.setText("MAX30102 · BPM");
            icon.setImageResource(R.drawable.ic_heart);
            icon.setImageTintList(ColorStateList.valueOf(WorkoutUi.color(card.getContext(), R.color.gym_coral)));
            WorkoutUi.tint(card.findViewById(R.id.device_tile), R.color.gym_coral_tint);
        }

        MaterialButton button = card.findViewById(R.id.device_button);
        button.setOnClickListener(v -> {
                if (isMotion) {
                    if (!DeviceState.motionConnected) {
                        if (((BluetoothActivity) activity).checkBluetooth(1)) {
                            bleHelp.connectToESP32(1);
                        }
                    }
                    else
                        bleHelp.disconnect(1);
                } else {
                    if (!DeviceState.heartConnected) {
                        if (((BluetoothActivity) activity).checkBluetooth(0)) {
                            bleHelp.connectToESP32(0);
                        }
                    }
                    else
                        bleHelp.disconnect(0);
                }
                showStatus(card, isMotion);
        });
        showStatus(card, isMotion);
    }

    private static void showStatus(View card, boolean isMotion) {
        boolean connected = isMotion ? DeviceState.motionConnected : DeviceState.heartConnected;
        TextView status = card.findViewById(R.id.device_status);
        MaterialButton button = card.findViewById(R.id.device_button);

        if (connected) {
            status.setText("Connected");
            status.setTextColor(WorkoutUi.color(card.getContext(), R.color.gym_accent));
            WorkoutUi.tint(status, R.color.gym_accent_tint);
            button.setText("Disconnect");
            button.setBackgroundTintList(ColorStateList.valueOf(WorkoutUi.color(card.getContext(), R.color.gym_surface_high)));
            button.setTextColor(WorkoutUi.color(card.getContext(), R.color.gym_text));
        } else {
            status.setText("Not connected");
            status.setTextColor(WorkoutUi.color(card.getContext(), R.color.gym_text_secondary));
            WorkoutUi.tint(status, R.color.gym_surface_high);
            button.setText("Connect");
            button.setBackgroundTintList(ColorStateList.valueOf(WorkoutUi.color(card.getContext(), R.color.gym_accent)));
            button.setTextColor(WorkoutUi.color(card.getContext(), R.color.gym_on_accent));
        }
    }

    public static void updateStatus(View view){
        View motion = view.findViewById(R.id.device_motion);
        View heart = view.findViewById(R.id.device_heart);
        showStatus(motion, true);
        showStatus(heart, false);
    }
}
