package com.example.smartgymtracker;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

// Light / dark mode chosen in Profile. Saved on the phone, not in the database.
public class ThemeSetting {

    public static final int SYSTEM = 0;   // follow the phone setting
    public static final int LIGHT = 1;
    public static final int DARK = 2;

    private static final String PREFS = "settings";
    private static final String KEY = "theme";

    public static int get(Context context) {
        return prefs(context).getInt(KEY, SYSTEM);
    }

    public static void set(Context context, int theme) {
        prefs(context).edit().putInt(KEY, theme).apply();
        apply(theme);
    }

    // Called when the app opens, and again each time the user changes it
    public static void apply(int theme) {
        if (theme == LIGHT) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (theme == DARK) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
