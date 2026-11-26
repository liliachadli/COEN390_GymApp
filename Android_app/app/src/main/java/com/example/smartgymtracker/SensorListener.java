package com.example.smartgymtracker;

// What the live set screen needs from the trackers.
public interface SensorListener {

    int SET_WAITING = 0;
    int SET_MOVING = 1;
    int SET_DONE = 2;         // 5 s without moving: the set is over
    int SET_CANCELLED = 3;    // the movement was not a real set
    int SET_HR = 4; // Only heart rate is on

    // States sent by the weight-stack tracker in its STATE packet
    int TRACKER_IDLE = 0;
    int TRACKER_START = 1;
    int TRACKER_LIFT = 2;
    int TRACKER_LOWER = 3;

    void onState(int state);

    void onRep(RepData rep);

    void onHeartRate(int bpm);
}
