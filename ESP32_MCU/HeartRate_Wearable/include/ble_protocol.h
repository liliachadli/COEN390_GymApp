#pragma once
#include <stdint.h>

// Heart rate wearable BLE protocol

#define BLE_SERVICE_UUID  "6e7a1001-5c3b-4f1e-9a2d-3b8c1f0e7a10"
#define BLE_DATA_UUID     "6e7a1002-5c3b-4f1e-9a2d-3b8c1f0e7a10"  // read + notify
#define BLE_INFO_UUID     "6e7a1003-5c3b-4f1e-9a2d-3b8c1f0e7a10"  // read, text

#define BLE_DEVICE_PREFIX "SGT-HR"

#define BLE_PROTOCOL_VERSION 2

// Packet types
#define BLE_PKT_HEART_RATE 0x02

#define BLE_SEND_INTERVAL_MS 1000

// 10 bytes, little-endian
struct __attribute__((packed)) HeartRatePacket {
  uint8_t  version;      // byte 0      protocol version (2)
  uint8_t  type;         // byte 1      0x02 = heart rate
  uint16_t sequence;     // bytes 2-3   +1 every packet
  uint32_t timeMs;       // bytes 4-7   ms since subscribe
  uint8_t  bpm;          // byte 8      0 = no reading yet
  uint8_t  quality;      // byte 9      1 = good, 0 = bad
};

static_assert(sizeof(HeartRatePacket) == 10, "HeartRatePacket must be 10 bytes");
