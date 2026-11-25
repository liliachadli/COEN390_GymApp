package com.example.smartgymtracker;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

// Keeps every page away from the status bar, the navigation bar and the keyboard,
// WITHOUT losing the page's own padding.
// (android:fitsSystemWindows replaced the padding written in the layout, so some pages
// had their text touching the screen edges.)
public class ScreenInsets {

    private static final String DONE = "insets-done";

    // Called once per page by GymApp, before the page is drawn
    public static void apply(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) {
            return;
        }
        View root = content.getChildAt(0);
        if (DONE.equals(root.getTag())) {
            return;
        }
        root.setTag(DONE);

        // We place the page ourselves, under the bars
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        root.setFitsSystemWindows(false);

        // Padding written in the layout (24dp sides, etc.)
        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(left + bars.left, top + bars.top, right + bars.right,
                    bottom + Math.max(bars.bottom, keyboard.bottom));
            return WindowInsetsCompat.CONSUMED;   // children (bottom bar) don't add them again
        });
        ViewCompat.requestApplyInsets(root);
    }
}
