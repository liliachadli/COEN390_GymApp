package com.example.smartgymtracker;

// One rep sent by the bracelet (REP packet)
public class RepData {

    public int rom;      // range of motion, in %
    public long concMs;  // time going up, in ms
    public long eccMs;   // time going down, in ms

    public RepData(int rom, long concMs, long eccMs) {
        this.rom = rom;
        this.concMs = concMs;
        this.eccMs = eccMs;
    }

    // Time under tension of this rep
    public long getTutMs() {
        return concMs + eccMs;
    }

    // How fast the rep was: ROM covered per second
    public double getSpeed() {
        if (getTutMs() == 0) {
            return 0;
        }
        return (rom / 100.0) / (getTutMs() / 1000.0);
    }
}
