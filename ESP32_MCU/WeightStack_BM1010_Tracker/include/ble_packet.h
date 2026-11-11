#pragma once
#include "ble_protocol.h"

RepPacket ble_buildRepPacket(uint16_t sequence, uint32_t timeMs,
                             int repCount, bool validRep, uint8_t state,
                             float romCm, int liftMs, int lowerMs);

StatePacket ble_buildStatePacket(uint16_t sequence, uint32_t timeMs, uint8_t state);

void ble_printPacket(const RepPacket &p);
void ble_printPacket(const StatePacket &p);
