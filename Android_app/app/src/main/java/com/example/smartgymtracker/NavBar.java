package com.example.smartgymtracker;

import android.app.Activity;
import android.content.Intent;

import com.google.android.material.bottomnavigation.BottomNavigationView;

// The bottom bar (History | Workout | Profile) used on the 3 main screens
public class NavBar {

    public static void setup(Activity activity, int selectedId) {
        BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(null);
        nav.setSelectedItemId(selectedId);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedId) {
                return true;
            }
            Class<?> screen = DashboardActivity.class;
            if (id == R.id.nav_history) {
                screen = HistoryActivity.class;
            } else if (id == R.id.nav_profile) {
                screen = ProfileActivity.class;
            }
            Intent intent = new Intent(activity, screen);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
            return true;
        });
    }
}
