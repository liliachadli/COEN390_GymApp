package com.example.smartgymtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

// 1.2 · Create a template, or edit one (opened from the dashboard or the template sheet)
public class TemplateEditorActivity extends AppCompatActivity {

    public static final String EXTRA_TEMPLATE_ID = "templateId";   // missing = new template
    private static final int MIN_SETS = 1;
    private static final int MAX_SETS = 10;

    private Template template;      // working copy, saved only when the user taps Save
    private boolean isNew;
    private String startState;      // to know if something changed
    private EditText nameInput;
    private LinearLayout exerciseList;
    private LinearLayout colorRow;

    // Exercise picker (ChooseExerciseActivity in pick mode) gives back "name|equipment" keys
    private final ActivityResultLauncher<Intent> pickExercises = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                ArrayList<String> picked = result.getData().getStringArrayListExtra(ChooseExerciseActivity.RESULT_PICKED);
                if (picked == null) {
                    return;
                }
                for (String key : picked) {
                    String[] parts = key.split("\\|");
                    template.exercises.add(parts[0]);
                    template.equipment.add(parts[1]);
                    template.sets.add(3);
                }
                showExercises();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template_editor);

        nameInput = findViewById(R.id.template_name_input);
        exerciseList = findViewById(R.id.template_exercise_list);
        colorRow = findViewById(R.id.color_row);

        String id = getIntent().getStringExtra(EXTRA_TEMPLATE_ID);
        Template existing = id == null ? null : TemplateStore.find(id);
        isNew = existing == null;
        template = isNew ? new Template() : existing.copy();

        if (savedInstanceState != null) {
            // Page rebuilt (light/dark mode): put back the unsaved changes
            template.color = savedInstanceState.getString("color", template.color);
            restoreExercises(savedInstanceState);
        }

        ((TextView) findViewById(R.id.page_title)).setText(isNew ? "New template" : "Edit template");
        if (savedInstanceState == null) {
            nameInput.setText(isNew ? "" : template.name);
        }
        startState = isNew ? stateOf(new Template()) : stateOf(existing);

        findViewById(R.id.back_button).setOnClickListener(v -> leave());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                leave();
            }
        });

        findViewById(R.id.add_exercises_button).setOnClickListener(v -> {
            if (WorkoutUi.tooFast()) {
                return;
            }
            Intent intent = new Intent(this, ChooseExerciseActivity.class);
            intent.putExtra(ChooseExerciseActivity.EXTRA_PICK, true);
            pickExercises.launch(intent);
        });

        findViewById(R.id.save_template_button).setOnClickListener(v -> save());

        View delete = findViewById(R.id.delete_template_button);
        delete.setVisibility(isNew ? View.GONE : View.VISIBLE);
        delete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete " + template.name + "?")
                .setMessage("The template will be removed. Your past workouts stay in History.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    TemplateStore.delete(this, template.id);
                    Toast.makeText(this, "Template deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show());

        showColors();
        showExercises();
    }

    // ---------- colors ----------

    private void showColors() {
        colorRow.removeAllViews();
        for (String color : Template.COLORS) {
            FrameLayout dot = new FrameLayout(this);
            dot.setBackgroundResource(R.drawable.bg_dot);
            WorkoutUi.tint(dot, Template.colorRes(color));
            int size = WorkoutUi.dp(this, 38);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginEnd(WorkoutUi.dp(this, 12));

            if (color.equals(template.color)) {
                ImageView check = new ImageView(this);
                check.setImageResource(R.drawable.ic_check);
                check.setColorFilter(WorkoutUi.color(this, R.color.gym_bg));
                int pad = WorkoutUi.dp(this, 9);
                check.setPadding(pad, pad, pad, pad);
                dot.addView(check, new FrameLayout.LayoutParams(size, size));
                dot.setScaleX(1.12f);
                dot.setScaleY(1.12f);
            }
            dot.setOnClickListener(v -> {
                template.color = color;
                showColors();
                Effects.tick(v);
            });
            colorRow.addView(dot, params);
        }
    }

    // ---------- exercises ----------

    private void showExercises() {
        exerciseList.removeAllViews();
        findViewById(R.id.empty_text).setVisibility(template.exercises.isEmpty() ? View.VISIBLE : View.GONE);

        int totalSets = 0;
        for (int i = 0; i < template.exercises.size(); i++) {
            final int index = i;
            String name = template.exercises.get(i);
            String equipment = template.equipment.get(i);
            String group = WorkoutOptions.getGroupFor(name);
            totalSets += template.sets.get(i);

            View row = LayoutInflater.from(this).inflate(R.layout.item_template_edit_row, exerciseList, false);
            ((TextView) row.findViewById(R.id.row_number)).setText(String.valueOf(i + 1));
            WorkoutUi.showEquipment(row.findViewById(R.id.equipment_tile),
                    (ImageView) row.findViewById(R.id.equipment_icon), equipment);
            ((TextView) row.findViewById(R.id.exercise_name)).setText(name);
            TextView groupText = row.findViewById(R.id.exercise_group);
            groupText.setText(group + " · " + equipment.toLowerCase());
            groupText.setTextColor(WorkoutUi.color(this, WorkoutOptions.getColorForGroup(group)));

            TextView count = row.findViewById(R.id.sets_count);
            count.setText(setsText(template.sets.get(i)));
            row.findViewById(R.id.sets_minus).setOnClickListener(v -> changeSets(index, -1, count));
            row.findViewById(R.id.sets_plus).setOnClickListener(v -> changeSets(index, +1, count));

            row.findViewById(R.id.remove_button).setOnClickListener(v -> {
                if (WorkoutUi.tooFast()) {
                    return;
                }
                template.exercises.remove(index);
                template.equipment.remove(index);
                template.sets.remove(index);
                showExercises();
            });
            exerciseList.addView(row);
        }

        int n = template.exercises.size();
        ((TextView) findViewById(R.id.template_summary)).setText(
                n == 0 ? "" : n + (n == 1 ? " exercise · " : " exercises · ") + totalSets + " sets");
    }

    private void changeSets(int index, int delta, TextView count) {
        int sets = Math.max(MIN_SETS, Math.min(MAX_SETS, template.sets.get(index) + delta));
        template.sets.set(index, sets);
        count.setText(setsText(sets));
        Effects.bump(count);
        int total = 0;
        for (int s : template.sets) {
            total += s;
        }
        int n = template.exercises.size();
        ((TextView) findViewById(R.id.template_summary)).setText(
                n + (n == 1 ? " exercise · " : " exercises · ") + total + " sets");
    }

    private String setsText(int sets) {
        return sets + (sets == 1 ? " set" : " sets");
    }

    // ---------- save / leave ----------

    private void save() {
        if (WorkoutUi.tooFast()) {
            return;
        }
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            nameInput.setError("Give your template a name");
            nameInput.requestFocus();
            return;
        }
        if (template.exercises.isEmpty()) {
            Toast.makeText(this, "Add at least one exercise", Toast.LENGTH_SHORT).show();
            return;
        }
        template.name = name;
        TemplateStore.save(this, template);
        Toast.makeText(this, isNew ? "Template created" : "Template saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    // Back: ask before losing changes
    private void leave() {
        template.name = nameInput.getText().toString().trim();
        if (stateOf(template).equals(startState)) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Save your changes?")
                .setPositiveButton("Save", (dialog, which) -> save())
                .setNegativeButton("Discard", (dialog, which) -> finish())
                .setNeutralButton("Cancel", null)
                .show();
    }

    private static String stateOf(Template t) {
        return (t.name == null ? "" : t.name) + "|" + t.color + "|" + t.exercises + "|" + t.equipment + "|" + t.sets;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("color", template.color);
        outState.putStringArrayList("exercises", new ArrayList<>(template.exercises));
        outState.putStringArrayList("equipment", new ArrayList<>(template.equipment));
        outState.putIntegerArrayList("sets", new ArrayList<>(template.sets));
    }

    private void restoreExercises(Bundle state) {
        ArrayList<String> exercises = state.getStringArrayList("exercises");
        ArrayList<String> equipment = state.getStringArrayList("equipment");
        ArrayList<Integer> sets = state.getIntegerArrayList("sets");
        if (exercises != null && equipment != null && sets != null) {
            template.exercises = exercises;
            template.equipment = equipment;
            template.sets = sets;
        }
    }
}
