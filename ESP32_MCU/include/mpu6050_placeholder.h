#pragma once
#include <stdint.h>

struct MotionSample {
  int16_t accel[3];
  int16_t gyro[3];
};

// HDW-2 placeholder until Lilia's MPU6050 code is merged.
// Fixed values, all different and some negative, so field order
// and sign decoding can be checked on the phone.
inline void mpu_readPlaceholder(MotionSample &m) {
  m.accel[0] = 1000;
  m.accel[1] = -2000;
  m.accel[2] = 16384;
  m.gyro[0] = 100;
  m.gyro[1] = -200;
  m.gyro[2] = 300;
}