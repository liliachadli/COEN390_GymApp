#include <Arduino.h>
#include <Wire.h>
#include "max30102.h"
#include "max30102_config.h"
#include "rate_monitor.h"
#include "mpu6050.h"
#include "ble_protocol.h"
#include "ble_packet.h"
#include "ble_tracker.h"

OpticalSample latestOptical = {0, 0, 0};
uint16_t sequence = 0;
uint32_t lastSendMs = 0;

void setup() {
  Serial.begin(115200);
  delay(2000);  // give USB serial time to connect

  Wire.begin(MAX30102_SDA_PIN, MAX30102_SCL_PIN, MAX30102_I2C_CLOCK);

  // HDW-1.2: check wiring
  if (!max30102_checkI2C()) {
    Serial.println("MAX30102 not found at 0x57, check wiring");
    while (true) delay(1000);
  }
  Serial.println("MAX30102 found at 0x57");

  // HDW-1.3: initialize sensor
  if (!max30102_init()) {
    Serial.println("MAX30102 init failed");
    while (true) delay(1000);
  }
  Serial.println("MAX30102 ready: Red + IR, 100 Hz");
  
  // initialize MPU6050
  if (!mpu6050_init()) {
    Serial.println("MPU6050 init failed");
    while (true) delay(1000);
  }
  Serial.println("MPU6050 ready");
  
  // BLE-1.2 / BLE-1.3: start service and advertising
  ble_begin();
}

void loop() {
  // HDW-1.4: read every new optical sample, keep the newest one
  OpticalSample s;
  while (max30102_read(s)) {
    latestOptical = s;
    rate_addSample();
  }

  // BLE-1.4 / BLE-1.5: every 20 ms, build and send one packet
  uint32_t now = millis();
  if (now - lastSendMs >= BLE_SEND_INTERVAL_MS) {
    lastSendMs = now;

    if (ble_isSubscribed()) {
      MotionSample m = {};
      mpu6050_read(m);

      RawPacket p = ble_buildPacket(sequence, now, m, latestOptical);
      ble_send(p);

      // BLE-1.6: print 1 of every 50 packets (once per second)
      if (sequence % 50 == 0) {
        ble_printPacket(p);
      }
      sequence++;
    }
  }

  // HDW-1.5: sample rate once per second
  rate_update();
  delay(1);
}