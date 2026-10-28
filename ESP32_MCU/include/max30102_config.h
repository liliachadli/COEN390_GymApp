#pragma once

// HDW-1.1 / HDW-1.2 - MAX30102 wiring
// MAX30102  ->  ESP32-C3 SuperMini
// VIN       ->  3V3 (measured 3.29 V)
// GND       ->  GND
// SDA       ->  GPIO4
// SCL       ->  GPIO5
// INT, RD, IRD -> not connected (INT gave false edges, we use polling)

#define MAX30102_SDA_PIN   4
#define MAX30102_SCL_PIN   5
#define MAX30102_I2C_ADDR  0x57
#define MAX30102_I2C_CLOCK 100000

// HDW-1.3 - sensor settings
#define MAX30102_LED_BRIGHTNESS 0x1F
#define MAX30102_SAMPLE_AVERAGE 1
#define MAX30102_LED_MODE       2     // 2 = Red + IR
#define MAX30102_SAMPLE_RATE    100   // Hz
#define MAX30102_PULSE_WIDTH    411   // us
#define MAX30102_ADC_RANGE      4096  // nA