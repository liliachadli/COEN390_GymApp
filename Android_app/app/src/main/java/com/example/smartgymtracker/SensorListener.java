package com.example.smartgymtracker;

// What the live set screen needs from the bracelet.
// BLE code (Person 4 / Nam) calls these when a packet arrives.
public interface SensorListener {

    int STATE_IDLE = 0;
    int STATE_MOVING = 1;
    int STATE_STOPPED = 2;    // 5 s without moving: the set is over
    int STATE_CANCELLED = 3;  // the movement was not a real set

    void onState(int state);

    void onRep(RepData rep);

    void onHeartRate(int bpm);
}
