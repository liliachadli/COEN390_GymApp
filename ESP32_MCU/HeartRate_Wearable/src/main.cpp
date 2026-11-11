#include <Arduino.h>
#include <Wire.h>
#include <math.h>
#include "max30102.h"
#include "max30102_config.h"
#include "mpu6050.h"
#include "heart_rate.h"
#include "ble_packet.h"
#include "ble_tracker.h"

// serial: HR,seq,timeMs,bpm,quality

const uint32_t HR_PRINT_INTERVAL_MS = 1000;

// only big movements count, normal small hand motion is ignored
const float    MOTION_ACCEL_G    = 1.0f;    // how far |accel| is from 1 g
const float    MOTION_GYRO_DPS   = 300.0f;
const uint32_t MOTION_HOLD_MS    = 2000;    // quality stays 0 this long after the last big movement
const float    MOTION_MATCH      = 0.15f;   // while moving, still good if within 15% of the BPM before moving

uint32_t startMs = 0;
uint32_t nextPrintTimeMs = 0;
uint32_t hrSeq = 0;
uint32_t lastMotionMs = 0;
bool     motionSeen = false;
float    lastGoodBpm = 0;   // last good BPM while not moving
bool     wasSubscribed = false;

void resetTimer() {
  startMs = millis();
  nextPrintTimeMs = HR_PRINT_INTERVAL_MS;
  hrSeq = 0;
}

bool bigMotion(const MotionSample &m) {
  float a = sqrtf(m.accel[0] * m.accel[0] + m.accel[1] * m.accel[1] + m.accel[2] * m.accel[2]);
  float g = sqrtf(m.gyro[0] * m.gyro[0] + m.gyro[1] * m.gyro[1] + m.gyro[2] * m.gyro[2]);
  return fabsf(a - 1.0f) > MOTION_ACCEL_G || g > MOTION_GYRO_DPS;
}

bool moving() {
  return motionSeen && millis() - lastMotionMs < MOTION_HOLD_MS;
}

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
  mpu6050_init();
  Serial.println("MAX30102 ready, place your finger on the sensor");

  ble_begin();

  resetTimer();
}

void loop() {
  // reset timer and sequence on subscribe
  bool subscribed = ble_isSubscribed();
  if (subscribed && !wasSubscribed) {
    resetTimer();
    Serial.println("BLE: phone subscribed, timer reset");
  }
  wasSubscribed = subscribed;

  MotionSample m;
  if (mpu6050_read(m) && bigMotion(m)) {
    motionSeen = true;
    lastMotionMs = millis();
  }

  OpticalSample s;
  while (max30102_read(s)) {
    hr_addSample(s.ir);
  }

  if (millis() - startMs >= nextPrintTimeMs) {
    float bpmNow = hr_bpm();
    bool good = hr_goodQuality();

    if (!moving()) {
      if (good) lastGoodBpm = bpmNow;
    } else {
      good = good && lastGoodBpm > 0 &&
             fabsf(bpmNow - lastGoodBpm) <= MOTION_MATCH * lastGoodBpm;
    }

    int bpm = (int)(bpmNow + 0.5f);
    int quality = good ? 1 : 0;

    Serial.printf("HR,%lu,%lu,%d,%d\n",
                  (unsigned long)hrSeq, (unsigned long)nextPrintTimeMs, bpm, quality);

    HeartRatePacket p = ble_buildPacket(hrSeq, nextPrintTimeMs, bpm, good);
    if (ble_send(p)) ble_printPacket(p);

    hrSeq++;
    nextPrintTimeMs += HR_PRINT_INTERVAL_MS;
  }

  delay(1);
}
