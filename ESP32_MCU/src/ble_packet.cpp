#include <Arduino.h>
#include "ble_packet.h"

// BLE-1.4 - put the timestamp and sensor values into one packet
RawPacket ble_buildPacket(uint16_t sequence, uint32_t timeMs, const MotionSample &motion, const OpticalSample &optical) {
  RawPacket p;
  p.version = BLE_PROTOCOL_VERSION;
  p.type = BLE_PKT_RAW_SAMPLE;
  p.sequence = sequence;
  p.timeMs = timeMs;

  for (int i = 0; i < 3; i++) {
    p.accel[i] = motion.accel[i];
    p.gyro[i] = motion.gyro[i];
  }

  p.ir = optical.ir;
  p.red = optical.red;
  return p;
}

// BLE-1.6 - print the packet values and raw bytes, so they can be
// compared with what the phone receives
void ble_printPacket(const RawPacket &p) {
  Serial.printf("TX seq=%u t=%lu A=%d,%d,%d G=%d,%d,%d IR=%lu RED=%lu\n",
                p.sequence, (unsigned long)p.timeMs,
                p.accel[0], p.accel[1], p.accel[2],
                p.gyro[0], p.gyro[1], p.gyro[2],
                (unsigned long)p.ir, (unsigned long)p.red);

  const uint8_t *bytes = (const uint8_t *)&p;
  Serial.print("   hex:");
  for (size_t i = 0; i < sizeof(p); i++) {
    Serial.printf(" %02X", bytes[i]);
  }
  Serial.println();
}