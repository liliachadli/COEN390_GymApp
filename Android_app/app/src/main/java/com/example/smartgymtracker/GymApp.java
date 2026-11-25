package com.example.smartgymtracker;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;

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

        // Every page opens with its blocks coming up one by one (see Effects.enterPage)
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
                if (savedInstanceState == null) {
                    // Runs once the page is built, just before it is drawn
                    activity.getWindow().getDecorView().post(() -> Effects.enterPage(activity));
                }
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                // Margins + room for the status bar, navigation bar and keyboard (once per page)
                ScreenInsets.apply(activity);
            }
            @Override public void onActivityResumed(@NonNull Activity activity) { }
            @Override public void onActivityPaused(@NonNull Activity activity) { }
            @Override public void onActivityStopped(@NonNull Activity activity) { }
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }
            @Override public void onActivityDestroyed(@NonNull Activity activity) { }
        });
    }
}
