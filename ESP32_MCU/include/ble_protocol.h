#pragma once
#include <stdint.h>

// BLE-1.1 - Sprint 1 BLE data structure

// Service and characteristic UUIDs
#define BLE_SERVICE_UUID  "6e7a0001-5c3b-4f1e-9a2d-3b8c1f0e7a10"
#define BLE_DATA_UUID     "6e7a0002-5c3b-4f1e-9a2d-3b8c1f0e7a10"  // read + notify
#define BLE_INFO_UUID     "6e7a0003-5c3b-4f1e-9a2d-3b8c1f0e7a10"  // read, text

#define BLE_PROTOCOL_VERSION 1

// Packet types. Later sprints add new types (rep count, ROM, TUT,
// rest time, BPM) without changing the Sprint 1 raw packet.
#define BLE_PKT_RAW_SAMPLE 0x01

// One packet every 20 ms = 50 packets per second
#define BLE_SEND_INTERVAL_MS 20

// Sprint 1 raw packet, 40 bytes, little-endian
struct __attribute__((packed)) RawPacket {
  uint8_t  version;      // byte 0      protocol version (1)
  uint8_t  type;         // byte 1      0x01 = raw sample
  uint16_t sequence;     // bytes 2-3   +1 every packet, used to find dropped packets
  uint32_t timeMs;       // bytes 4-7   ms since ESP32 boot
  float    accel[3];     // bytes 8-19  accel X, Y, Z (g)
  float    gyro[3];      // bytes 20-31 gyro X, Y, Z (deg/s)
  uint32_t ir;           // bytes 32-35 MAX30102 IR count
  uint32_t red;          // bytes 36-39 MAX30102 Red count
};

static_assert(sizeof(RawPacket) == 40, "RawPacket must be 40 bytes");