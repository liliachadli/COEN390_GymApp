#include <Arduino.h>
#include "ble_packet.h"

static uint16_t toU16(int v) {
  return (uint16_t)constrain(v, 0, 65535);
}

RepPacket ble_buildRepPacket(uint16_t sequence, uint32_t timeMs,
                             int repCount, bool validRep, uint8_t state,
                             float romCm, int liftMs, int lowerMs) {
  RepPacket p;
  p.version = BLE_PROTOCOL_VERSION;
  p.type = BLE_PKT_REP;
  p.sequence = sequence;
  p.timeMs = timeMs;
  p.repCount = toU16(repCount);
  p.validRep = validRep ? 1 : 0;
  p.state = state;
  p.romCm = romCm;
  p.liftMs = toU16(liftMs);
  p.lowerMs = toU16(lowerMs);
  p.reserved = 0;
  return p;
}

StatePacket ble_buildStatePacket(uint16_t sequence, uint32_t timeMs, uint8_t state) {
  StatePacket p;
  p.version = BLE_PROTOCOL_VERSION;
  p.type = BLE_PKT_STATE;
  p.sequence = sequence;
  p.timeMs = timeMs;
  p.state = state;
  return p;
}

static void printHex(const uint8_t *bytes, size_t len) {
  Serial.print("   hex:");
  for (size_t i = 0; i < len; i++) {
    Serial.printf(" %02X", bytes[i]);
  }
  Serial.println();
}

// debug: values + raw bytes
void ble_printPacket(const RepPacket &p) {
  Serial.printf("TX REP seq=%u t=%lu rep=%u valid=%u state=%u rom=%.1f lift=%u lower=%u\n",
                p.sequence, (unsigned long)p.timeMs, p.repCount, p.validRep,
                p.state, p.romCm, p.liftMs, p.lowerMs);
  printHex((const uint8_t *)&p, sizeof(p));
}

void ble_printPacket(const StatePacket &p) {
  Serial.printf("TX STATE seq=%u t=%lu state=%u\n",
                p.sequence, (unsigned long)p.timeMs, p.state);
  printHex((const uint8_t *)&p, sizeof(p));
}
