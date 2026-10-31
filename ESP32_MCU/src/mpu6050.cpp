#include <Arduino.h>
#include <Wire.h>
#include <mpu6050.h>

const uint8_t ADDR = 0x68; // MPU-6050 I2C address. AD0 pin low

//Writes 1 byte to 1 register
void writeReg(uint8_t reg, uint8_t val) {
  Wire.beginTransmission(ADDR);
  Wire.write(reg);
  Wire.write(val);
  Wire.endTransmission();
}

//Read n bytes starting at a register
bool readRegs(uint8_t reg, uint8_t *buf, uint8_t n) {
  Wire.beginTransmission(ADDR);
  Wire.write(reg);
  if (Wire.endTransmission(false) != 0) return false;
  if (Wire.requestFrom(ADDR, n) != n) return false;
  for (uint8_t i = 0; i < n; i++) buf[i] = Wire.read();
  return true;
}

bool mpu6050_init() {
  // Uses the I2C bus (GPIO4/5) already started in main.cpp, shared with the MAX30102

  writeReg(0x6B, 0x00);  // PWR_MGMT_1: wake up
  writeReg(0x19, 9);     // SMPLRT_DIV: 1000 / (1+9) = 100 Hz
  writeReg(0x1A, 0x03);  // CONFIG: gyro DLPF ~41 Hz
  writeReg(0x1B, 0x00);  // GYRO_CONFIG: +-250 dps
  writeReg(0x1C, 0x00);  // ACCEL_CONFIG: +-2 g
  writeReg(0x1D, 0x03);  // ACCEL_CONFIG2 (MPU-6500): accel DLPF ~44.8 Hz
  
  return true;
}

void mpu6050_read(MotionSample &ms) {
  uint8_t status;
  if (readRegs(0x3A, &status, 1) && (status & 0x01)) {      // INT_STATUS: data ready
    uint8_t buffer[14];
    if (readRegs(0x3B, buffer, 14)) {
      ms.accel[0] = (int16_t)((buffer[0]  << 8) | buffer[1]) / 16384.0f; //ax (g)
      ms.accel[1] = (int16_t)((buffer[2]  << 8) | buffer[3]) / 16384.0f; //ay (g)
      ms.accel[2] = (int16_t)((buffer[4]  << 8) | buffer[5]) / 16384.0f; //az (g)
      ms.gyro[0] = (int16_t)((buffer[8]  << 8) | buffer[9]) / 131.0f; //gx (deg/s)
      ms.gyro[1] = (int16_t)((buffer[10] << 8) | buffer[11]) / 131.0f; //gy (deg/s)
      ms.gyro[2] = (int16_t)((buffer[12] << 8) | buffer[13]) / 131.0f; //gz (deg/s)
    }
  }
}