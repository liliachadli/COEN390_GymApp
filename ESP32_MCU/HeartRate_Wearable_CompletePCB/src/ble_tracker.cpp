#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include <BLESecurity.h>
#include "ble_tracker.h"

static BLECharacteristic *dataChar = nullptr;
static BLE2902 *dataCccd = nullptr;
static bool connected = false;
static uint32_t disconnects = 0;
static char deviceName[16];

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
  uint64_t mac = ESP.getEfuseMac();
  snprintf(deviceName, sizeof(deviceName), BLE_DEVICE_PREFIX "-%02X%02X",
           (uint8_t)(mac >> 32), (uint8_t)(mac >> 40));

  BLEDevice::init(deviceName);
  BLEDevice::setMTU(185);
  BLESecurity *sec = new BLESecurity();
  sec->setAuthenticationMode(ESP_LE_AUTH_REQ_SC_BOND);
  sec->setCapability(ESP_IO_CAP_NONE);
  // BLE keys only, the C3 can't make a classic BT key
  sec->setInitEncryptionKey(ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK);
  sec->setRespEncryptionKey(ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK);

  BLEServer *server = BLEDevice::createServer();
  server->setCallbacks(new ServerCallbacks());

  BLEService *service = server->createService(BLE_SERVICE_UUID);

  dataChar = service->createCharacteristic(
      BLE_DATA_UUID,
      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
  dataCccd = new BLE2902();
  dataChar->addDescriptor(dataCccd);
  dataChar->setAccessPermissions(ESP_GATT_PERM_READ_ENCRYPTED);
  dataCccd->setAccessPermissions(ESP_GATT_PERM_READ_ENCRYPTED | ESP_GATT_PERM_WRITE_ENCRYPTED);

  BLECharacteristic *infoChar = service->createCharacteristic(
      BLE_INFO_UUID, BLECharacteristic::PROPERTY_READ);
  infoChar->setValue("SmartGymTracker-HR;proto=2;pkt=10");

  service->start();

  // UUID in advertisement, name in scan response
  BLEAdvertising *adv = BLEDevice::getAdvertising();

  BLEAdvertisementData advData;
  advData.setFlags(0x06);
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

bool ble_send(const HeartRatePacket &p) {
  if (!ble_isSubscribed()) return false;
  dataChar->setValue((uint8_t *)&p, sizeof(p));
  dataChar->notify();
  return true;
}

bool ble_sendText(const char *line) {
  if (!ble_isSubscribed()) return false;
  dataChar->setValue((uint8_t *)line, strlen(line));
  dataChar->notify();
  return true;
}

uint32_t ble_disconnectCount() {
  return disconnects;
}