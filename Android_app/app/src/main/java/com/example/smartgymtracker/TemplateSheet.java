package com.example.smartgymtracker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialog;

// Template sheet: shows the exercises of a template and starts it
public class TemplateSheet {

    public static void show(Activity activity, Template template) {
        BottomSheetDialog sheet = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.sheet_template, null);
        sheet.setContentView(view);
        ((View) view.getParent()).setBackgroundColor(Color.TRANSPARENT);

        WorkoutUi.tint(view.findViewById(R.id.template_color), template.getColorRes());
        ((TextView) view.findViewById(R.id.template_name)).setText(template.name);
        int count = template.exercises.size();
        ((TextView) view.findViewById(R.id.template_info)).setText(
                count + (count == 1 ? " exercise · " : " exercises · ") + template.getMuscles());

        LinearLayout list = view.findViewById(R.id.template_list);
        for (int i = 0; i < count; i++) {
            View row = LayoutInflater.from(activity).inflate(R.layout.item_template_exercise, list, false);
            WorkoutUi.showEquipment(row.findViewById(R.id.equipment_tile),
                    (ImageView) row.findViewById(R.id.equipment_icon), template.equipment.get(i));
            ((TextView) row.findViewById(R.id.exercise_name)).setText(template.exercises.get(i));
            int sets = template.sets.get(i);
            ((TextView) row.findViewById(R.id.exercise_sets)).setText(sets + (sets == 1 ? " set" : " sets"));
            list.addView(row);
        }

        view.findViewById(R.id.template_edit).setOnClickListener(v -> {
            if (WorkoutUi.tooFast()) {
                return;
            }
            sheet.dismiss();
            Intent intent = new Intent(activity, TemplateEditorActivity.class);
            intent.putExtra(TemplateEditorActivity.EXTRA_TEMPLATE_ID, template.id);
            activity.startActivity(intent);
        });

        view.findViewById(R.id.template_start).setOnClickListener(v -> {
            if (WorkoutUi.tooFast()) {
                return;
            }
            if (WorkoutSession.active) {
                // Don't erase the workout in progress without asking
                new AlertDialog.Builder(activity)
                        .setTitle("A workout is already running")
                        .setMessage("Starting " + template.name + " will delete the current workout.")
                        .setPositiveButton("Start new", (dialog, which) -> startTemplate(activity, template, sheet))
                        .setNegativeButton("Keep current", null)
                        .show();
                return;
            }
            startTemplate(activity, template, sheet);
        });
        sheet.show();
    }

    private static void startTemplate(Activity activity, Template template, BottomSheetDialog sheet) {
        WorkoutSession.start(template.name);
        for (int i = 0; i < template.exercises.size(); i++) {
            WorkoutSession.addExercise(template.exercises.get(i), template.equipment.get(i), template.sets.get(i));
        }
        sheet.dismiss();
        activity.startActivity(new Intent(activity, WorkoutActivity.class));
    }
}
