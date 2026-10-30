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
import androidx.core.app.ActivityCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothHelper {
    private final BluetoothAdapter bluetoothAdapter;
    private final ScanSettings scanSettings;

    private final ScanCallback scanCallback;

    private BluetoothLeScanner scanner;
    private final Context appContext;
    private boolean scanning;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private BluetoothGatt btGatt = null;
    private final BluetoothGattCallback gattCallback;
    private BluetoothDevice esp32 = null;

    private final String CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    private final String SERVICE_UUID = "6e7a0001-5c3b-4f1e-9a2d-3b8c1f0e7a10";
    private final String DATA_CHARACTERISTIC_UUID = "6e7a0002-5c3b-4f1e-9a2d-3b8c1f0e7a10";
    private final WeakReference<MainActivity> activityRef;
    private boolean readyToDisplay = true;

    BluetoothHelper(Context context, MainActivity activity){
        activityRef = new WeakReference<>(activity);
        appContext = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Null if the phone has no Bluetooth (emulator) or Bluetooth is off
        scanner = (bluetoothAdapter != null) ? bluetoothAdapter.getBluetoothLeScanner() : null;
        scanning = false;

        scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                // FIRST_MATCH needs hardware support many phones don't have (scan fails silently)
                .setReportDelay(0L)
                .build();

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                    return;
                }
                if (btGatt != null) return;   // already connecting/connected
                scanner.stopScan(scanCallback);
                scanning = false;
                handler.removeCallbacksAndMessages(null);
                esp32 = result.getDevice();
                Log.i("BluetoothHelper", "Found " + esp32.getAddress() + ", connecting");
                sendStatus("Tracker found, connecting...");
                btGatt = esp32.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
            }
            @Override
            public void onScanFailed(int errorCode) {
                Log.e("BluetoothHelper", "Scan failed, error " + errorCode);
                sendStatus("Bluetooth scan failed (error " + errorCode + ")");
                scanning = false;
            }
        };

        gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.i("BluetoothHelper", "Connection state " + newState + ", status " + status);
                if (status == GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                            return;
                        }
                        sendStatus("Connected to tracker");
                        // 40-byte packet needs a bigger MTU; services are discovered in onMtuChanged
                        if (!gatt.requestMtu(64)) {
                            gatt.discoverServices();
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        gatt.close();
                        btGatt = null;
                        sendStatus("Tracker disconnected");
                    }
                } else {
                    // status 133 etc. is common on the first try: close and scan again
                    Log.e("BluetoothHelper", "Error connecting to the board, status " + status);
                    gatt.close();
                    btGatt = null;
                    sendStatus("Connection failed (status " + status + "), retrying...");
                    handler.postDelayed(() -> scanForESP32(), 1000);
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
                        if (services.get(i).getUuid().equals(UUID.fromString(SERVICE_UUID))){
                            characteristic = services.get(i).getCharacteristic(UUID.fromString(DATA_CHARACTERISTIC_UUID));
                        }
                    }
                    if (characteristic!=null)
                        setCharacteristicNotification(characteristic);
                    else
                        sendStatus("Tracker service not found");

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
                if(readyToDisplay) {
                    readyToDisplay = false;
                    sendToMain(characteristic.getValue());
                }
            }

            @Override
            public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
                if(readyToDisplay) {
                    readyToDisplay = false;
                    sendToMain(value);
                }
            }
        };
    }
    public void setReadyToDisplay() {
        readyToDisplay = true;
    }
    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void scanForESP32() {
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(UUID.fromString(SERVICE_UUID))).build());
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            Log.e("BluetoothHelper", "BLUETOOTH_SCAN permission missing");
            sendStatus("Bluetooth permission missing");
            return;
        }
        if (btGatt != null) return;   // already connected
        if (scanner == null && bluetoothAdapter != null) {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        if (scanner == null) {
            Log.e("BluetoothHelper", "Bluetooth is off, cannot scan");
            return;
        }

        if (!scanning) {
            // Stops scanning after 10 seconds
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                        Log.e("BluetoothHelper", "BLUETOOTH_SCAN permission missing");
                        return;
                    }
                    scanner.stopScan(scanCallback);
                    sendStatus("Tracker not found. Is the ESP32 on?");
                }
            }, 10000);

            scanning = true;
            sendStatus("Searching for tracker...");
            scanner.startScan(filters, scanSettings, scanCallback);
        } else {
            scanning = false;
            scanner.stopScan(scanCallback);
        }
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic) {
        if (btGatt == null) {
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
        btGatt.setCharacteristicNotification(characteristic, true);
        sendStatus("Receiving data");

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CCC_DESCRIPTOR_UUID));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        btGatt.writeDescriptor(descriptor);
    }

    // Stop scanning and drop the connection (call from MainActivity.onDestroy)
    public void disconnect() {
        handler.removeCallbacksAndMessages(null);
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
                || !hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return;
        }
        if (scanning && scanner != null) {
            scanner.stopScan(scanCallback);
            scanning = false;
        }
        if (btGatt != null) {
            btGatt.disconnect();
            btGatt.close();
            btGatt = null;
        }
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
        final MainActivity activity = activityRef.get();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            activity.runOnUiThread(() -> activity.showStatus(text));
        }
    }

    public void sendToMain(byte[] value) {
        final MainActivity activity = activityRef.get();

        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {

            // Critical: Move from the background BLE thread to the UI Main Thread
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // CALL YOUR MAIN ACTIVITY FUNCTION HERE
                    activity.displayReceivedData(value);
                }
            });
        }
    }
}