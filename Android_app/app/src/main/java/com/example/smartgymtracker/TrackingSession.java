package com.example.smartgymtracker;

// Start/Stop tracking state (APP-0.4)
public class TrackingSession {

    private boolean tracking = false;
    private long startTimeMs = 0;

    public void start() {
        if (tracking) {
            return;
        }
        tracking = true;
        startTimeMs = System.currentTimeMillis();
    }

    public void stop() {
        tracking = false;
        startTimeMs = 0;
    }

    public boolean isTracking() {
        return tracking;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }
}