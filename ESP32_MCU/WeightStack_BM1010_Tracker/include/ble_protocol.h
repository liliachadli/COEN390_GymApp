#pragma once
#include <stdint.h>

// Weight stack tracker BLE protocol

#define BLE_SERVICE_UUID  "6e7a2001-5c3b-4f1e-9a2d-3b8c1f0e7a10"
#define BLE_DATA_UUID     "6e7a2002-5c3b-4f1e-9a2d-3b8c1f0e7a10"  // read + notify
#define BLE_INFO_UUID     "6e7a2003-5c3b-4f1e-9a2d-3b8c1f0e7a10"  // read, text

#define BLE_DEVICE_PREFIX "SGT-WS"

#define BLE_PROTOCOL_VERSION 2

// Packet types
#define BLE_PKT_REP   0x03
#define BLE_PKT_STATE 0x04

// Sent per rep, 22 bytes, little-endian
struct __attribute__((packed)) RepPacket {
  uint8_t  version;      // byte 0      protocol version (2)
  uint8_t  type;         // byte 1      0x03 = rep
  uint16_t sequence;     // bytes 2-3   +1 every packet
  uint32_t timeMs;       // bytes 4-7   ms since subscribe
  uint16_t repCount;     // bytes 8-9
  uint8_t  validRep;     // byte 10     1 = counted
  uint8_t  state;        // byte 11     0 IDLE, 1 START, 2 LIFT, 3 LOWER
  float    romCm;        // bytes 12-15 top - bottom
  uint16_t liftMs;       // bytes 16-17
  uint16_t lowerMs;      // bytes 18-19
  uint16_t reserved;     // bytes 20-21 0
};

// Sent on state change, 9 bytes, little-endian
struct __attribute__((packed)) StatePacket {
  uint8_t  version;      // byte 0      protocol version (2)
  uint8_t  type;         // byte 1      0x04 = state
  uint16_t sequence;     // bytes 2-3
  uint32_t timeMs;       // bytes 4-7
  uint8_t  state;        // byte 8      0 IDLE, 1 START, 2 LIFT, 3 LOWER
};

static_assert(sizeof(RepPacket) == 22, "RepPacket must be 22 bytes");
static_assert(sizeof(StatePacket) == 9, "StatePacket must be 9 bytes");
