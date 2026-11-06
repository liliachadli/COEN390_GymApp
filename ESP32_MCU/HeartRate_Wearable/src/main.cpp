#include <Arduino.h>
#include <Wire.h>
#include "max30102.h"
#include "max30102_config.h"
#include "heart_rate.h"

// output: HR,seq,timeMs,bpm,quality

const uint32_t HR_PRINT_INTERVAL_MS = 1000;

uint32_t startMs = 0;
uint32_t nextPrintTimeMs = 0;
uint32_t hrSeq = 0;

void setup() {
  Serial.begin(115200);
  delay(2000);

  Wire.begin(MAX30102_SDA_PIN, MAX30102_SCL_PIN, MAX30102_I2C_CLOCK);

  if (!max30102_checkI2C()) {
    Serial.println("MAX30102 not found at 0x57, check wiring");
    while (true) delay(1000);
  }
  if (!max30102_init()) {
    Serial.println("MAX30102 init failed");
    while (true) delay(1000);
  }
  Serial.println("MAX30102 ready, place your finger on the sensor");

  startMs = millis();
  nextPrintTimeMs = HR_PRINT_INTERVAL_MS;
}

void loop() {
  OpticalSample s;
  while (max30102_read(s)) {
    hr_addSample(s.ir);
  }

  if (millis() - startMs >= nextPrintTimeMs) {
    int bpm = (int)(hr_bpm() + 0.5f);
    int quality = hr_goodQuality() ? 1 : 0;

    Serial.printf("HR,%lu,%lu,%d,%d\n",
                  (unsigned long)hrSeq, (unsigned long)nextPrintTimeMs, bpm, quality);

    hrSeq++;
    nextPrintTimeMs += HR_PRINT_INTERVAL_MS;
  }

  delay(1);
}
