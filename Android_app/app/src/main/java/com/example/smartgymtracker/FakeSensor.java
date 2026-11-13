package com.example.smartgymtracker;

import android.os.Handler;
import android.os.Looper;

import java.util.Random;

// Sends made-up packets so the live set screen can be tested without the bracelet.
public class FakeSensor {

    private static final int REP_COUNT = 8;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final SensorListener listener;
    private int repsSent = 0;
    private int bpm = 112;

    public FakeSensor(SensorListener listener) {
        this.listener = listener;
    }

    public void start() {
        handler.postDelayed(() -> listener.onState(SensorListener.STATE_MOVING), 800);
        handler.postDelayed(this::sendRep, 2000);
        handler.post(this::sendHeartRate);
    }

    public void stop() {
        handler.removeCallbacksAndMessages(null);
    }

    private void sendRep() {
        repsSent++;
        int rom = 96 - repsSent * 2 + random.nextInt(4);
        long up = 550 + repsSent * 30 + random.nextInt(80);
        long down = 650 + repsSent * 40 + random.nextInt(80);
        listener.onRep(new RepData(rom, up, down));

        if (repsSent < REP_COUNT) {
            handler.postDelayed(this::sendRep, 1600);
        } else {
            // 5 seconds without moving: the set is over
            handler.postDelayed(() -> listener.onState(SensorListener.STATE_STOPPED), 5000);
        }
    }

    private void sendHeartRate() {
        bpm = Math.min(150, bpm + random.nextInt(5) - 1);
        listener.onHeartRate(bpm);
        handler.postDelayed(this::sendHeartRate, 1000);
    }
}
