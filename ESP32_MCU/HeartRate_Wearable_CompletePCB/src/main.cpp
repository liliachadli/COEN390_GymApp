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
//         PWR,state   (on change: on battery / charging / charged)

const uint32_t HR_PRINT_INTERVAL_MS = 1000;

// BQ24073 status pins, both active low, 10k pull-ups on the PCB
const uint8_t CHG_PIN   = 3;   // low = charging
const uint8_t PGOOD_PIN = 4;   // low = USB power present

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
int      lastPowerState = -1;   // -1 = print on boot

void printPowerIfChanged() {
  bool usb = digitalRead(PGOOD_PIN) == LOW;
  bool charging = digitalRead(CHG_PIN) == LOW;
  int state = !usb ? 0 : (charging ? 1 : 2);   // CHG means nothing without USB power
  if (state == lastPowerState) return;
  lastPowerState = state;

  const char *names[] = {"on battery", "charging", "charged"};
  Serial.printf("PWR,%s\n", names[state]);
}

// debug: charger status every few seconds, with the raw pin levels
const uint32_t PWR_STATUS_INTERVAL_MS = 5000;
uint32_t nextPwrStatusMs = 0;

void printPowerStatus() {
  if (millis() < nextPwrStatusMs) return;
  nextPwrStatusMs = millis() + PWR_STATUS_INTERVAL_MS;

  int pgood = digitalRead(PGOOD_PIN);
  int chg = digitalRead(CHG_PIN);
  const char *msg;
  if (pgood == HIGH)    msg = "no USB, running on battery";
  else if (chg == LOW)  msg = "USB in, battery CHARGING";
  else                  msg = "USB in, battery FULL (or no battery)";

  Serial.printf("PWR_STATUS: %s  [PGOOD=%d CHG=%d]\n", msg, pgood, chg);
}

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

  pinMode(CHG_PIN, INPUT);
  pinMode(PGOOD_PIN, INPUT);
  printPowerIfChanged();

  Wire.begin(MAX30102_SDA_PIN, MAX30102_SCL_PIN, MAX30102_I2C_CLOCK);

  if (!max30102_checkI2C()) {
    Serial.println("MAX30102 not found at 0x57, check wiring");
    while (true) { printPowerStatus(); delay(100); }   // still report charging
  }
  if (!max30102_init()) {
    Serial.println("MAX30102 init failed");
    while (true) { printPowerStatus(); delay(100); }
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

  printPowerIfChanged();
  printPowerStatus();

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
