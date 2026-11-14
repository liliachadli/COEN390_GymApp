#pragma once

// HDW-1.1 / HDW-1.2 - MAX30102 wiring on CompletePCB V1.3 (ESP32-C3-MINI-1)
// J5 pin 1  VIN  ->  3V3
// J5 pin 2  SDA  ->  GPIO0 (shared with the MPU6050 on J4)
// J5 pin 3  SCL  ->  GPIO1 (shared with the MPU6050 on J4)
// J5 pin 4  GND  ->  GND
// INT not routed on the PCB, we use polling
// SDA/SCL are swapped compared to the SuperMini breadboard build

#define MAX30102_SDA_PIN   0
#define MAX30102_SCL_PIN   1
#define MAX30102_I2C_ADDR  0x57
#define MAX30102_I2C_CLOCK 100000

// HDW-1.3 - sensor settings
#define MAX30102_LED_BRIGHTNESS 0x1F
#define MAX30102_SAMPLE_AVERAGE 1
#define MAX30102_LED_MODE       2     // 2 = Red + IR
#define MAX30102_SAMPLE_RATE    100   // Hz
#define MAX30102_PULSE_WIDTH    411   // us
#define MAX30102_ADC_RANGE      4096  // nA