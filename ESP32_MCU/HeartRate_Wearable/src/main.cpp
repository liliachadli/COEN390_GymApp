#include <Arduino.h>
#include <Wire.h>
#include "max30102.h"
#include "max30102_config.h"
#include "mpu6050.h"

// Serial-only debug build: no BLE, just print sensor output.
const uint32_t PRINT_INTERVAL_MS = 20;  // 50 Hz

OpticalSample latestOptical = {0, 0, 0};
uint32_t lastPrintMs = 0;

void setup() {
  Serial.begin(115200);
  delay(2000);  // give USB serial time to connect

  Wire.begin(MAX30102_SDA_PIN, MAX30102_SCL_PIN, MAX30102_I2C_CLOCK);

  if (!max30102_checkI2C()) {
    Serial.println("MAX30102 not found at 0x57, check wiring");
    while (true) delay(1000);
  }
  if (!max30102_init()) {
    Serial.println("MAX30102 init failed");
    while (true) delay(1000);
  }
  Serial.println("MAX30102 ready");

  if (!mpu6050_init()) {
    Serial.println("MPU6050 init failed");
    while (true) delay(1000);
  }
  Serial.println("MPU6050 ready");

  Serial.println("ms,red,ir,ax,ay,az,gx,gy,gz");
}

void loop() {
  // Drain the MAX30102 FIFO, keep the newest sample
  OpticalSample s;
  while (max30102_read(s)) {
    latestOptical = s;
  }

  uint32_t now = millis();
  if (now - lastPrintMs >= PRINT_INTERVAL_MS) {
    lastPrintMs = now;

    MotionSample m = {};
    mpu6050_read(m);

    Serial.printf("%lu,%lu,%lu,%.3f,%.3f,%.3f,%.2f,%.2f,%.2f\n",
                  (unsigned long)now,
                  (unsigned long)latestOptical.red,
                  (unsigned long)latestOptical.ir,
                  m.accel[0], m.accel[1], m.accel[2],
                  m.gyro[0], m.gyro[1], m.gyro[2]);
  }

  delay(1);
}
