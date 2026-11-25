package com.example.smartgymtracker;

import android.app.Application;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// Runs once when the app starts, before any screen
public class GymApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeSetting.apply(ThemeSetting.get(this));

        // A workout was running when the app was closed: bring it back
        WorkoutStore.init(this);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            WorkoutStore.restore(user.getUid());
        }
        WorkoutService.createChannel(this);
    }
}
