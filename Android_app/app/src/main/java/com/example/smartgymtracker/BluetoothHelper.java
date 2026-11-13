package com.example.smartgymtracker;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.os.HandlerCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class BluetoothHelper {
    private static BluetoothHelper bleHelp = null;
    private final BluetoothAdapter bluetoothAdapter;
    private final ScanSettings scanSettings;
    private BluetoothLeScanner scanner;
    private final boolean[] scanning = new boolean[2];
    private final Context appContext;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WeakReference<BluetoothActivity> activityRef;
    private String activityClass;

    // Bluetooth objects for devices
    private final ScanCallback[] scanCallbacks = new ScanCallback[2];
    private final BluetoothGatt[] btGatts = new BluetoothGatt[2];
    private final BluetoothGattCallback[] gattCallbacks = new BluetoothGattCallback[2];
    private final BluetoothGattCharacteristic[] characteristics = new BluetoothGattCharacteristic[2];
    private final String[] service_UUIDs = new String[2];
    private final String[] char_UUIDs = new String[2];

    // UUIDs
    private final String CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    private final String SERVICE_UUID_HR = "6e7a1001-5c3b-4f1e-9a2d-3b8c1f0e7a10";
    private final String SERVICE_UUID_REPS = "6e7a2001-5c3b-4f1e-9a2d-3b8c1f0e7a10";
    private final String DATA_CHARACTERISTIC_UUID_HR = "6e7a1002-5c3b-4f1e-9a2d-3b8c1f0e7a10";
    private final String DATA_CHARACTERISTIC_UUID_REPS = "6e7a2002-5c3b-4f1e-9a2d-3b8c1f0e7a10";

    public static BluetoothHelper getInstance(Context context, BluetoothActivity activity, String actClass){
        if(bleHelp == null)
            bleHelp = new BluetoothHelper(context);

        bleHelp.changeActivity(activity, actClass);
        return bleHelp;
    }
    private BluetoothHelper(Context context){
        appContext = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        service_UUIDs[0] = SERVICE_UUID_HR;
        service_UUIDs[1] = SERVICE_UUID_REPS;
        char_UUIDs[0] = DATA_CHARACTERISTIC_UUID_HR;
        char_UUIDs[1] = DATA_CHARACTERISTIC_UUID_REPS;

        // Null if the phone has no Bluetooth (emulator) or Bluetooth is off
        scanner = (bluetoothAdapter != null) ? bluetoothAdapter.getBluetoothLeScanner() : null;
        scanning[0] = false;
        scanning[1] = false;

        scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                // FIRST_MATCH needs hardware support many phones don't have (scan fails silently)
                .setReportDelay(0L)
                .build();

        scanCallbacks[0] = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                    return;
                }
                scanner.stopScan(scanCallbacks[0]);
                scanning[0] = false;
                handler.removeCallbacksAndMessages(service_UUIDs[0]);

                if (btGatts[0] != null) return;   // already connecting/connected
                BluetoothDevice esp32 = result.getDevice();
                Log.i("BluetoothHelper", "Found " + esp32.getAddress() + ", connecting");
                sendStatus("Heart Rate Tracker found, connecting...");
                btGatts[0] = esp32.connectGatt(appContext, false, gattCallbacks[0], BluetoothDevice.TRANSPORT_LE);
            }
            @Override
            public void onScanFailed(int errorCode) {
                Log.e("BluetoothHelper", "Scan failed, error " + errorCode);
                sendStatus("Bluetooth scan failed (error " + errorCode + ")");
                scanning[0] = false;
            }
        };

        scanCallbacks[1] = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                    return;
                }
                scanner.stopScan(scanCallbacks[1]);
                scanning[1] = false;
                handler.removeCallbacksAndMessages(service_UUIDs[1]);

                if (btGatts[1] != null) return;   // already connecting/connected
                BluetoothDevice esp32 = result.getDevice();
                Log.i("BluetoothHelper", "Found " + esp32.getAddress() + ", connecting");
                sendStatus("Rep Tracker found, connecting...");
                btGatts[1] = esp32.connectGatt(appContext, false, gattCallbacks[1], BluetoothDevice.TRANSPORT_LE);
            }
            @Override
            public void onScanFailed(int errorCode) {
                Log.e("BluetoothHelper", "Scan failed, error " + errorCode);
                sendStatus("Bluetooth scan failed (error " + errorCode + ")");
                scanning[1] = false;
            }
        };

        gattCallbacks[0] = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.i("BluetoothHelper", "Connection state " + newState + ", status " + status);
                if (status == GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                            return;
                        }
                        sendStatus("Connected to Heart Rate Tracker");
                        sendConnection(0,true);
                        gatt.discoverServices();

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        gatt.close();
                        btGatts[0] = null;
                        sendConnection(0,false);
                        sendStatus("Heart Rate Tracker disconnected");
                    }
                } else {
                    // status 133 etc. is common on the first try: close and scan again
                    Log.e("BluetoothHelper", "Error connecting to the HR board, status " + status);
                    gatt.close();
                    btGatts[0] = null;
                    sendConnection(0,false);
                    sendStatus("Heart Rate Tracker has been disconnected. Trying to reconnect...");
                    handler.postDelayed(() -> connectToESP32(0), 1000);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == GATT_SUCCESS) {
                    List<BluetoothGattService> services = gatt.getServices();
                    BluetoothGattCharacteristic characteristic = null;
                    for(int i = 0; i<services.size(); i++){
                        if (services.get(i).getUuid().equals(UUID.fromString(service_UUIDs[0]))){
                            characteristic = services.get(i).getCharacteristic(UUID.fromString(char_UUIDs[0]));
                        }
                    }
                    if (characteristic!=null)
                        characteristics[0] = characteristic;
                    else
                        sendStatus("Heart Rate Tracker service not found");

                    if(activityClass.equals("MainActivity"))
                        turnOnNotifications(0);

                } else {
                    Log.e("BluetoothHelper", "Service discovery failed");
                    if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                        return;
                    }
                    gatt.disconnect();
                }
            }
            // Android 12 and lower only call this older version
            @Override
            @SuppressWarnings("deprecation")
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                sendHeartRateToActivity(characteristic.getValue());
            }

            @Override
            public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
                sendHeartRateToActivity(value);
            }
        };


        gattCallbacks[1] = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.i("BluetoothHelper", "Connection state " + newState + ", status " + status);
                if (status == GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                            return;
                        }
                        sendStatus("Connected to Rep Tracker");
                        sendConnection(1,true);
                        // 40-byte packet needs a bigger MTU; services are discovered in onMtuChanged
                        if (!gatt.requestMtu(64)) {
                            gatt.discoverServices();
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        gatt.close();
                        btGatts[1] = null;
                        sendConnection(1,false);
                        sendStatus("Rep Tracker disconnected");
                    }
                } else {
                    // status 133 etc. is common on the first try: close and scan again
                    Log.e("BluetoothHelper", "Error connecting to the rep tracker board, status " + status);
                    gatt.close();
                    btGatts[1] = null;
                    sendConnection(1,false);
                    sendStatus("Rep Tracker has been disconnected. Trying to reconnect...");
                    handler.postDelayed(() -> connectToESP32(1), 1000);
                }
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                Log.i("BluetoothHelper", "MTU " + mtu + ", status " + status);
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    return;
                }
                gatt.discoverServices();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == GATT_SUCCESS) {
                    List<BluetoothGattService> services = gatt.getServices();
                    BluetoothGattCharacteristic characteristic = null;
                    for(int i = 0; i<services.size(); i++){
                        if (services.get(i).getUuid().equals(UUID.fromString(service_UUIDs[1]))){
                            characteristic = services.get(i).getCharacteristic(UUID.fromString(char_UUIDs[1]));
                        }
                    }
                    if (characteristic!=null)
                        characteristics[1] = characteristic;
                    else
                        sendStatus("Rep Tracker service not found");

                    if(activityClass.equals("MainActivity"))
                        turnOnNotifications(1);

                } else {
                    Log.e("BluetoothHelper", "Service discovery failed");
                    if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                        return;
                    }
                    gatt.disconnect();
                }
            }
            // Android 12 and lower only call this older version
            @Override
            @SuppressWarnings("deprecation")
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                sendRepsToActivity(characteristic.getValue());
            }

            @Override
            public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
                sendRepsToActivity(value);
            }
        };
    }
    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void connectToESP32(int deviceNum) {    // deviceNum: 0 for HR, 1 for Rep counter
        List<ScanFilter> filters = new ArrayList<>();
        String service_UUID = service_UUIDs[deviceNum];

        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(UUID.fromString(service_UUID))).build());
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            Log.e("BluetoothHelper", "BLUETOOTH_SCAN permission missing");
            sendStatus("Bluetooth permission missing");
            return;
        }
        if (btGatts[deviceNum] != null) return;   // already connected
        if (scanner == null && bluetoothAdapter != null) {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        if (scanner == null) {
            Log.e("BluetoothHelper", "Bluetooth is off, cannot scan");
            return;
        }

        if (!scanning[deviceNum]) {
            String trackerType;
            if(deviceNum == 0)
                trackerType = "Heart Rate";
            else
                trackerType = "Rep";

            // Stops scanning after 10 seconds
            HandlerCompat.postDelayed(handler, new Runnable() {
                @Override
                public void run() {
                    scanning[deviceNum] = false;
                    if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                        Log.e("BluetoothHelper", "BLUETOOTH_SCAN permission missing");
                        return;
                    }
                    scanner.stopScan(scanCallbacks[deviceNum]);
                    sendStatus(trackerType + " Tracker not found. Is the device turned on?");
                }
            }, service_UUID,10000);

            scanning[deviceNum] = true;
            sendStatus("Searching for " + trackerType + " Tracker...");
            scanner.startScan(filters, scanSettings, scanCallbacks[deviceNum]);
        } else {
            scanning[deviceNum] = false;
            scanner.stopScan(scanCallbacks[deviceNum]);
        }
    }

    private void setCharacteristicNotification(int deviceNum, BluetoothGattCharacteristic characteristic, boolean enable) {
        if (btGatts[deviceNum] == null) {
            Log.w("BluetoothHelper", "BluetoothGatt not initialized");
            return;
        }
        if(characteristic == null) {
            Log.e("BluetoothHelper", "Characteristic is null");
            return;
        }
        if ((characteristic.getProperties() & PROPERTY_NOTIFY) == 0) {
            Log.e("BluetoothHelper", "Characteristic doesn't support notifications");
            return;
        }

        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return;
        }

        byte[] desc_value;

        if(enable)
            desc_value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
        else {
            desc_value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
        }

        btGatts[deviceNum].setCharacteristicNotification(characteristic, enable);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CCC_DESCRIPTOR_UUID));
        descriptor.setValue(desc_value);
        btGatts[deviceNum].writeDescriptor(descriptor);
    }

    // Stop scanning and drop the connection (call from MainActivity.onDestroy)
    public void disconnect(int deviceNum) { // deviceNum 0 for hr, 1 for repcount
        handler.removeCallbacksAndMessages(service_UUIDs[deviceNum]);
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
                || !hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return;
        }
        if (scanning[deviceNum] && scanner != null) {
            scanner.stopScan(scanCallbacks[deviceNum]);
            scanning[deviceNum] = false;
        }
        if (btGatts[deviceNum] != null) {
            btGatts[deviceNum].disconnect();
            btGatts[deviceNum].close();
            btGatts[deviceNum] = null;
        }

        sendConnection(deviceNum,false);
    }

    // BLUETOOTH_SCAN / BLUETOOTH_CONNECT only exist on Android 12+; older phones use location instead
    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                && (permission.equals(Manifest.permission.BLUETOOTH_SCAN) || permission.equals(Manifest.permission.BLUETOOTH_CONNECT))) {
            return ActivityCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return ActivityCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED;
    }

    // Shows a short status message on the screen
    private void sendStatus(String text) {
        final BluetoothActivity activity = activityRef.get();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            activity.runOnUiThread(() -> activity.showStatus(text));
        }
    }

    private void sendConnection(int deviceNum, boolean connected){
        final BluetoothActivity activity = activityRef.get();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            activity.runOnUiThread(() -> activity.showConnection(deviceNum, connected));
        }
    }

    private void sendRepsToActivity(byte[] value) {
        final BluetoothActivity activity = activityRef.get();

        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {

            // Critical: Move from the background BLE thread to the UI Main Thread
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // CALL YOUR MAIN ACTIVITY FUNCTION HERE
                    activity.onNotifiedReps(value);
                }
            });
        }
    }

    private void sendHeartRateToActivity(byte[] value) {
        final BluetoothActivity activity = activityRef.get();

        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {

            // Critical: Move from the background BLE thread to the UI Main Thread
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // CALL YOUR MAIN ACTIVITY FUNCTION HERE
                    activity.onNotifiedHeartRate(value);
                }
            });
        }
    }

    public void turnOnNotifications(int deviceNum){
        setCharacteristicNotification(deviceNum, characteristics[deviceNum], true);
    }
    public void turnOffNotifications(int deviceNum){
        setCharacteristicNotification(deviceNum, characteristics[deviceNum], false);
    }

    public void changeActivity(BluetoothActivity activity, String actClass){
        activityRef = new WeakReference<>(activity);
        activityClass = actClass;
    }
}