#pragma once
#include "ble_protocol.h"
#include "max30102.h"
#include "mpu6050_placeholder.h"

RawPacket ble_buildPacket(uint16_t sequence, uint32_t timeMs,
                          const MotionSample &motion,
                          const OpticalSample &optical);

void ble_printPacket(const RawPacket &p);   // BLE-1.6 debug output