#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include "ble_tracker.h"

static BLECharacteristic *dataChar = nullptr;
static BLE2902 *dataCccd = nullptr;
static bool connected = false;
static uint32_t disconnects = 0;
static char deviceName[12];

// Called by the BLE stack when a phone connects or disconnects
class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer *server) override {
    connected = true;
    Serial.println("BLE: phone connected");
  }

  void onDisconnect(BLEServer *server) override {
    connected = false;
    disconnects++;
    Serial.println("BLE: phone disconnected, advertising again");
    BLEDevice::startAdvertising();
  }
};

void ble_begin() {
  // BLE-1.3 - tracker name "SGT-XXXX", last 2 bytes of this chip's MAC,
  // so two trackers in the same room have different names
  uint64_t mac = ESP.getEfuseMac();
  snprintf(deviceName, sizeof(deviceName), "SGT-%02X%02X",
           (uint8_t)(mac >> 32), (uint8_t)(mac >> 40));

  BLEDevice::init(deviceName);
  BLEDevice::setMTU(185);  // packet is 40 bytes, default BLE limit is 20

  // BLE-1.2 - GATT service with a data and an info characteristic
  BLEServer *server = BLEDevice::createServer();
  server->setCallbacks(new ServerCallbacks());

  BLEService *service = server->createService(BLE_SERVICE_UUID);

  dataChar = service->createCharacteristic(
      BLE_DATA_UUID,
      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
  dataCccd = new BLE2902();   // lets the phone turn notifications on/off
  dataChar->addDescriptor(dataCccd);

  BLECharacteristic *infoChar = service->createCharacteristic(
      BLE_INFO_UUID, BLECharacteristic::PROPERTY_READ);
  infoChar->setValue("SmartGymTracker;proto=1;pkt=40");

  service->start();

  // BLE-1.3 - advertising: service UUID in the advertisement so the app
  // can filter for it, name in the scan response (not enough room for both)
  BLEAdvertising *adv = BLEDevice::getAdvertising();

  BLEAdvertisementData advData;
  advData.setFlags(0x06);  // general discoverable, BLE only
  advData.setCompleteServices(BLEUUID(BLE_SERVICE_UUID));
  adv->setAdvertisementData(advData);

  BLEAdvertisementData scanData;
  scanData.setName(deviceName);
  adv->setScanResponseData(scanData);

  BLEDevice::startAdvertising();

  Serial.print("BLE: advertising as ");
  Serial.print(deviceName);
  Serial.print("  address ");
  Serial.println(BLEDevice::getAddress().toString().c_str());
}

bool ble_isConnected() {
  return connected;
}

bool ble_isSubscribed() {
  return connected && dataCccd != nullptr && dataCccd->getNotifications();
}

// BLE-1.5 - send one packet as a notification
bool ble_send(const RawPacket &p) {
  if (!ble_isSubscribed()) return false;
  dataChar->setValue((uint8_t *)&p, sizeof(p));
  dataChar->notify();
  return true;
}

uint32_t ble_disconnectCount() {
  return disconnects;
}