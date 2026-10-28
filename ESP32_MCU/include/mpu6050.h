#pragma once
#include <Arduino.h>

struct MotionSample {
  int16_t accel[3];
  int16_t gyro[3];
};

void writeReg(uint8_t reg, uint8_t val);    
bool readRegs(uint8_t reg, uint8_t *buf, uint8_t n);       
bool mpu6050_init();   
void mpu6050_read(MotionSample &m);