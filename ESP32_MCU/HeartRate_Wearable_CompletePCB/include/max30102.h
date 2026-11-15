#pragma once
#include <Arduino.h>

struct OpticalSample {
  uint32_t red;
  uint32_t ir;
  uint32_t timeUs;  // time the ESP32 read it, not the sensor's ADC time
};

bool max30102_checkI2C();               // HDW-1.2
bool max30102_init();                   // HDW-1.3
bool max30102_read(OpticalSample &s);   // HDW-1.4 + HDW-1.5