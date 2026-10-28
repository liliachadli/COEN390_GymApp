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
  Serial.begin(115200);
  delay(1500);
  Wire.begin(0, 1);
  Wire.setClock(400000); 

  writeReg(0x6B, 0x00);  // PWR_MGMT_1: wake up
  writeReg(0x19, 9);     // SMPLRT_DIV: 1000 / (1+9) = 100 Hz
  writeReg(0x1A, 0x03);  // CONFIG: gyro DLPF ~41 Hz
  writeReg(0x1B, 0x08);  // GYRO_CONFIG: +-500 dps
  writeReg(0x1C, 0x10);  // ACCEL_CONFIG: +-8 g
  writeReg(0x1D, 0x03);  // ACCEL_CONFIG2 (MPU-6500): accel DLPF ~44.8 Hz
  
  return true;
}

void mpu6050_read(MotionSample &ms) {
  uint8_t st;
  if (readRegs(0x3A, &st, 1) && (st & 0x01)) {      // INT_STATUS: data ready
    uint8_t b[14];
    uint32_t t = micros();                          // timestamp at read time
    if (readRegs(0x3B, b, 14)) {
      ms.accel[0] = (int16_t)((b[0]  << 8) | b[1]) / 4096; //ax
      ms.accel[1] = (int16_t)((b[2]  << 8) | b[3]) / 4096; //ay
      ms.accel[2] = (int16_t)((b[4]  << 8) | b[5]) / 4096; //az
      ms.gyro[0] = (int16_t)((b[8]  << 8) | b[9]) / 65.5; //gx
      ms.gyro[1] = (int16_t)((b[10] << 8) | b[11]) / 65.5; //gy
      ms.gyro[2] = (int16_t)((b[12] << 8) | b[13])/ 65.5; //gz
    }
  }
}