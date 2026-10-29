#pragma once
#include <Arduino.h>

struct MotionSample {
  float accel[3];   // g
  float gyro[3];    // deg/s
};

void writeReg(uint8_t reg, uint8_t val);    
bool readRegs(uint8_t reg, uint8_t *buf, uint8_t n);       
bool mpu6050_init();   
void mpu6050_read(MotionSample &m);