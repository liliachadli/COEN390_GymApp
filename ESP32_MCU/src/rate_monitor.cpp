#include <Arduino.h>
#include "rate_monitor.h"

// HDW-1.5 - measure the real sample rate (sketch 04)
// Vincent measured ~100-101 Hz (301-302 samples per 3 s)

static uint32_t samplesThisWindow = 0;
static uint32_t lastReportMs = 0;

void rate_addSample() {
  samplesThisWindow++;
}

void rate_update() {
  uint32_t now = millis();
  uint32_t elapsed = now - lastReportMs;

  if (elapsed >= 1000) {
    Serial.print("Sample rate: ");
    Serial.print(samplesThisWindow * 1000.0f / elapsed, 1);
    Serial.println(" Hz");

    samplesThisWindow = 0;
    lastReportMs = now;
  }
}