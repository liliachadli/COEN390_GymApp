#include <Wire.h>
#include "MAX30105.h"   // SparkFun library, works for MAX30102 too
#include "max30102.h"
#include "max30102_config.h"

static MAX30105 sensor;

// HDW-1.2 - check the sensor answers at 0x57 (sketch 01)
bool max30102_checkI2C() {
  Wire.beginTransmission(MAX30102_I2C_ADDR);
  return Wire.endTransmission() == 0;
}

// HDW-1.3 - start the library and configure Red + IR (sketch 02)
bool max30102_init() {
  if (!sensor.begin(Wire, I2C_SPEED_STANDARD)) {
    return false;
  }

  Serial.print("Part ID: 0x");
  Serial.println(sensor.readPartID(), HEX);

  // setup() also soft-resets the chip, so it wakes up from shutdown
  sensor.setup(MAX30102_LED_BRIGHTNESS, MAX30102_SAMPLE_AVERAGE,
               MAX30102_LED_MODE, MAX30102_SAMPLE_RATE,
               MAX30102_PULSE_WIDTH, MAX30102_ADC_RANGE);
  return true;
}

// HDW-1.4 - read one Red/IR sample from the FIFO (sketch 03)
// HDW-1.5 - attach a timestamp to it (sketch 04)
// Returns false when there is no new sample.
bool max30102_read(OpticalSample &s) {
  if (!sensor.available()) {
    sensor.check();
    if (!sensor.available()) return false;
  }

  s.red = sensor.getFIFORed();
  s.ir = sensor.getFIFOIR();
  s.timeUs = micros();

  sensor.nextSample();
  return true;
}