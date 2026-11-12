package com.example.smartgymtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

// 1 · Dashboard: start a workout, templates, devices (APP-1.3)
public class DashboardActivity extends AppCompatActivity {

    private TextView greeting;
    private TextView motionChip;
    private TextView heartChip;
    private TextView startTitle;
    private TextView startSubtitle;
    private TextView weekCount;
    private TextView lastPr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        greeting = findViewById(R.id.dash_greeting);
        motionChip = findViewById(R.id.motion_chip);
        heartChip = findViewById(R.id.heart_chip);
        startTitle = findViewById(R.id.start_title);
        startSubtitle = findViewById(R.id.start_subtitle);
        weekCount = findViewById(R.id.week_count);
        lastPr = findViewById(R.id.last_pr);

        findViewById(R.id.start_card).setOnClickListener(v -> startWorkout());
        findViewById(R.id.devices_button).setOnClickListener(v -> DeviceSheet.show(this, this::refresh));
        findViewById(R.id.new_template_button).setOnClickListener(v ->
                Toast.makeText(this, "Creating templates comes later", Toast.LENGTH_SHORT).show());

        addTemplateCards();
        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavBar.setup(this, R.id.nav_workout);
        refresh();

        // Load workouts from the database
        WorkoutHistory.loadWorkouts(() ->{
            refresh();
        });
    }

    private void refresh() {
        WorkoutUi.showDeviceChips(motionChip, heartChip);

        if (WorkoutSession.active) {
            startTitle.setText("Resume workout");
            startSubtitle.setText(WorkoutSession.name + " is still running");
        } else {
            startTitle.setText("Start empty workout");
            startSubtitle.setText("Add exercises as you go");
        }

        int count = WorkoutHistory.countThisWeek();
        weekCount.setText(count == 1 ? "1 workout" : count + " workouts");

        lastPr.setText("—");
        for (PastWorkout workout : WorkoutHistory.workouts) {
            if (!workout.prText.isEmpty()) {
                lastPr.setText(workout.prText);
                break;
            }
        }
    }

    private void startWorkout() {
        if (!WorkoutSession.active) {
            WorkoutSession.start("Workout");
        }
        startActivity(new Intent(this, WorkoutActivity.class));
    }

    // Template cards, 2 per row
    private void addTemplateCards() {
        LinearLayout grid = findViewById(R.id.templates_grid);
        LinearLayout row = null;

        for (int i = 0; i < Template.ALL.length; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.bottomMargin = WorkoutUi.dp(this, 12);
                row.setLayoutParams(params);
                grid.addView(row);
            } else {
                row.addView(new Space(this), WorkoutUi.dp(this, 12), 1);
            }

            Template template = Template.ALL[i];
            View card = LayoutInflater.from(this).inflate(R.layout.item_template_card, row, false);
            TextView name = card.findViewById(R.id.template_name);
            TextView muscles = card.findViewById(R.id.template_muscles);
            TextView count = card.findViewById(R.id.template_count);
            name.setText(template.name);
            muscles.setText(template.muscles);
            count.setText(template.exercises.length + " exercises");
            count.setTextColor(WorkoutUi.color(this, template.colorRes));
            WorkoutUi.tint(card.findViewById(R.id.template_color), template.colorRes);
            card.setOnClickListener(v -> TemplateSheet.show(this, template));
            row.addView(card);
        }

        // Odd number of templates: keep the last card half width
        if (Template.ALL.length % 2 == 1 && row != null) {
            row.addView(new Space(this), WorkoutUi.dp(this, 12), 1);
            View empty = new View(this);
            row.addView(empty, new LinearLayout.LayoutParams(0, 1, 1));
        }
    }

    // Shows the first name, and opens the profile screen if it was never filled (APP-1.3)
    private void loadProfile() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String hello = hour < 12 ? "Good morning" : (hour < 18 ? "Good afternoon" : "Good evening");
        greeting.setText(hello);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String firstName = doc.getString("firstName");
                    if (firstName != null && !firstName.isEmpty()) {
                        greeting.setText(hello + ", " + firstName);
                    }
                    if (!Boolean.TRUE.equals(doc.getBoolean("profileDone"))) {
                        Intent intent = new Intent(this, ProfileActivity.class);
                        intent.putExtra(ProfileActivity.EXTRA_FIRST_TIME, true);
                        startActivity(intent);
                    }
                });
    }
}
