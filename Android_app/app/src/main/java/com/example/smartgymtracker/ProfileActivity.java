package com.example.smartgymtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

// 3 · Profile. Also opens by itself the first time the user logs in (APP-1.3).
public class ProfileActivity extends AppCompatActivity implements NavBar.LeaveGuard {

    public static final String EXTRA_FIRST_TIME = "firstTime";

    private boolean firstTime;
    private FirebaseUser user;
    private EditText firstName;
    private EditText lastName;
    private EditText birthDate;
    private EditText weight;
    private EditText height;
    private MaterialButtonToggleGroup sexGroup;
    private MaterialButtonToggleGroup unitsGroup;
    private TextView initials;

    private boolean kgSelected = false;   // unit chosen on this screen (saved only with Save)
    private String savedState = "";       // what the fields looked like after the last load or save

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firstTime = getIntent().getBooleanExtra(EXTRA_FIRST_TIME, false);
        user = FirebaseAuth.getInstance().getCurrentUser();

        firstName = findViewById(R.id.first_name_input);
        lastName = findViewById(R.id.last_name_input);
        birthDate = findViewById(R.id.birth_date_input);
        weight = findViewById(R.id.weight_input);
        height = findViewById(R.id.height_input);
        sexGroup = findViewById(R.id.sex_group);
        unitsGroup = findViewById(R.id.units_group);
        initials = findViewById(R.id.profile_initials);
        sexGroup.check(R.id.sex_m);
        unitsGroup.check(R.id.units_lbs);
        weight.setHint("lbs");

        // Switching lbs <-> kg converts the weight already typed
        unitsGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            boolean kg = checkedId == R.id.units_kg;
            if (kg != kgSelected) {
                int lbs = Units.toLbs(weight.getText().toString(), kgSelected);
                kgSelected = kg;
                weight.setText(lbs > 0 ? Units.number(lbs, kgSelected) : "");
            }
            weight.setHint(kgSelected ? "kg" : "lbs");
        });

        if (firstTime) {
            ((TextView) findViewById(R.id.profile_title)).setText("Your profile");
            findViewById(R.id.profile_first_time).setVisibility(View.VISIBLE);
            findViewById(R.id.bottom_nav).setVisibility(View.GONE);
        }

        String email = user != null ? user.getEmail() : "";
        ((TextView) findViewById(R.id.profile_email)).setText(email);
        ((TextView) findViewById(R.id.email_value)).setText(email);

        findViewById(R.id.save_button).setOnClickListener(v -> saveProfile(null));
        findViewById(R.id.logout_button).setOnClickListener(v -> logOut());
        findViewById(R.id.change_password).setOnClickListener(v -> sendPasswordEmail());

        // Back button: ask before leaving if something changed
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (hasUnsavedChanges()) {
                    askToSave(() -> finish());
                } else {
                    finish();
                }
            }
        });

        savedState = currentState();
        loadProfile();
    }

    // All the fields in one string, to see if something changed since the last save
    private String currentState() {
        return firstName.getText() + "|" + lastName.getText() + "|" + birthDate.getText() + "|"
                + Units.toLbs(weight.getText().toString(), kgSelected) + "|" + height.getText() + "|"
                + sexGroup.getCheckedButtonId() + "|" + kgSelected;
    }

    @Override
    public boolean hasUnsavedChanges() {
        return !currentState().equals(savedState);
    }

    @Override
    public void askToSave(Runnable thenLeave) {
        new AlertDialog.Builder(this)
                .setTitle("Save your changes?")
                .setMessage("You changed your profile.")
                .setPositiveButton("Save", (dialog, which) -> saveProfile(thenLeave))
                .setNegativeButton("Don't save", (dialog, which) -> thenLeave.run())
                .setNeutralButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!firstTime) {
            NavBar.setup(this, R.id.nav_profile);
        }
    }

    private void loadProfile() {
        if (user == null) {
            return;
        }
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(this::showProfile);
    }

    private void showProfile(DocumentSnapshot doc) {
        firstName.setText(textOf(doc, "firstName"));
        lastName.setText(textOf(doc, "lastName"));
        birthDate.setText(textOf(doc, "birthDate"));
        height.setText(textOf(doc, "height"));
        sexGroup.check("F".equals(doc.getString("sex")) ? R.id.sex_f : R.id.sex_m);

        // The weight is saved in lbs: show it in the user's unit
        kgSelected = "kg".equals(doc.getString("units"));
        unitsGroup.check(kgSelected ? R.id.units_kg : R.id.units_lbs);
        weight.setHint(kgSelected ? "kg" : "lbs");
        int lbs = Units.toLbs(textOf(doc, "weight"), false);
        weight.setText(lbs > 0 ? Units.number(lbs, kgSelected) : "");

        showInitials();
        savedState = currentState();
    }

    private String textOf(DocumentSnapshot doc, String key) {
        Object value = doc.get(key);
        return value == null ? "" : value.toString();
    }

    private void showInitials() {
        String first = firstName.getText().toString().trim();
        String last = lastName.getText().toString().trim();
        String text = "";
        if (!first.isEmpty()) {
            text += first.charAt(0);
        }
        if (!last.isEmpty()) {
            text += last.charAt(0);
        }
        initials.setText(text.isEmpty() ? "?" : text.toUpperCase());
    }

    // thenLeave: what to do after saving (leave the screen), or null to stay
    private void saveProfile(Runnable thenLeave) {
        if (firstName.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter your first name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (user == null) {
            return;
        }

        // TODO (Person 5): check these field names match the database format
        Map<String, Object> data = new HashMap<>();
        data.put("firstName", firstName.getText().toString().trim());
        data.put("lastName", lastName.getText().toString().trim());
        data.put("birthDate", birthDate.getText().toString().trim());
        int lbs = Units.toLbs(weight.getText().toString(), kgSelected);
        data.put("weight", lbs > 0 ? String.valueOf(lbs) : "");  // always saved in lbs
        data.put("height", height.getText().toString().trim());
        data.put("sex", sexGroup.getCheckedButtonId() == R.id.sex_f ? "F" : "M");
        data.put("units", kgSelected ? "kg" : "lbs");
        data.put("profileDone", true);

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
                    showInitials();
                    Units.useKg = kgSelected;
                    savedState = currentState();
                    if (thenLeave != null) {
                        thenLeave.run();
                    } else if (firstTime) {
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not save the profile", Toast.LENGTH_LONG).show());
    }

    private void sendPasswordEmail() {
        if (user == null || user.getEmail() == null) {
            return;
        }
        FirebaseAuth.getInstance().sendPasswordResetEmail(user.getEmail())
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Email sent to change your password", Toast.LENGTH_LONG).show());
    }

    private void logOut() {
        FirebaseAuth.getInstance().signOut();
        WorkoutSession.reset();
        WorkoutHistory.clearHistory();
        Units.useKg = false;

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
