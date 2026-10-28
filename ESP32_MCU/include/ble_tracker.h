#pragma once
#include <stdint.h>
#include "ble_protocol.h"

void ble_begin();                       // BLE-1.2 + BLE-1.3
bool ble_isConnected();
bool ble_isSubscribed();                // phone turned on notifications
bool ble_send(const RawPacket &p);      // BLE-1.5
uint32_t ble_disconnectCount();         // BLE-1.5 test record