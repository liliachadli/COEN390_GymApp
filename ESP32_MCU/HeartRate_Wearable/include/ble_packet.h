#pragma once
#include "ble_protocol.h"

HeartRatePacket ble_buildPacket(uint16_t sequence, uint32_t timeMs,
                                int bpm, bool goodQuality);

void ble_printPacket(const HeartRatePacket &p);   // debug output
