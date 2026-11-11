#include <Arduino.h>
#include "ble_packet.h"

HeartRatePacket ble_buildPacket(uint16_t sequence, uint32_t timeMs, int bpm, bool goodQuality) {
  HeartRatePacket p;
  p.version = BLE_PROTOCOL_VERSION;
  p.type = BLE_PKT_HEART_RATE;
  p.sequence = sequence;
  p.timeMs = timeMs;
  p.bpm = (uint8_t)constrain(bpm, 0, 255);
  p.quality = goodQuality ? 1 : 0;
  return p;
}

// debug: values + raw bytes
void ble_printPacket(const HeartRatePacket &p) {
  Serial.printf("TX seq=%u t=%lu bpm=%u quality=%u\n",
                p.sequence, (unsigned long)p.timeMs, p.bpm, p.quality);

  const uint8_t *bytes = (const uint8_t *)&p;
  Serial.print("   hex:");
  for (size_t i = 0; i < sizeof(p); i++) {
    Serial.printf(" %02X", bytes[i]);
  }
  Serial.println();
}
