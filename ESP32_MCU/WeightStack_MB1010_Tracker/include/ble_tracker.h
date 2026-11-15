#pragma once
#include <stdint.h>
#include "ble_protocol.h"

void ble_begin();
bool ble_isConnected();
bool ble_isSubscribed();
bool ble_send(const RepPacket &p);
bool ble_send(const StatePacket &p);
uint32_t ble_disconnectCount();
