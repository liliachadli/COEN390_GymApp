package com.example.smartgymtracker;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class BluetoothActivity extends AppCompatActivity {
    BluetoothHelper bleHelp;
    ActivityResultLauncher<Intent> bluetoothEnableResult;
    int deviceToConnect = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bleHelp = BluetoothHelper.getInstance(getApplicationContext(), this, "BluetoothActivity");

        bluetoothEnableResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK){
                        Toast.makeText(this, "Please enable bluetooth to use this application", Toast.LENGTH_LONG).show();
                    } else {
                        if (deviceToConnect >= 0 && deviceToConnect < 2){
                            bleHelp.connectToESP32(deviceToConnect);   // Bluetooth just turned on
                        }
                        else if (deviceToConnect == 2){
                            bleHelp.connectToESP32(0);
                            bleHelp.connectToESP32(1);
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bleHelp.changeActivity(this, "BluetoothActivity");
    }

    public void onNotifiedHeartRate(byte[] value){

    }
    public void onNotifiedReps(byte[] value){

    }

    public void showStatus(String text){

    }

    public void showConnection(int deviceNum, boolean connected){
        if(deviceNum == 0) {
            DeviceState.heartConnected = connected;
        }
        else if(deviceNum == 1){
            DeviceState.motionConnected = connected;
        }
    }

    public boolean checkBluetooth(int deviceNum){
        deviceToConnect = deviceNum;
        boolean perms = getBluetoothPermissions();

        if(!bleHelp.isBluetoothSupported()){
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
            return false;   // e.g. emulator: show the screen but don't touch Bluetooth
        }

        if(!bleHelp.isBluetoothEnabled()){
            if(perms) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                bluetoothEnableResult.launch(enableBtIntent);
            }
            else
                Toast.makeText(this, "Please enable bluetooth to connect to the trackers", Toast.LENGTH_LONG).show();
        }

        return perms && bleHelp.isBluetoothEnabled();
    }

    protected boolean checkBluetoothPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    protected boolean getBluetoothPermissions() {
        boolean perms = true;
        List<String> permissionsToRequest = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            perms = false;
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), 1);
        }
        return perms;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1){
            boolean allGranted = grantResults.length > 0;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) allGranted = false;
            }
            if (allGranted){
                Log.e("Permissions", "Permission Granted");
                if (!bleHelp.isBluetoothEnabled()) {
                    bluetoothEnableResult.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                }
                else if (deviceToConnect >= 0 && deviceToConnect < 2){
                    bleHelp.connectToESP32(deviceToConnect);   // Bluetooth just turned on
                }
                else if (deviceToConnect == 2){
                    bleHelp.connectToESP32(0);
                    bleHelp.connectToESP32(1);
                }
            }
            else {
                Log.e("Permissions","Permission Denied");
                Toast.makeText(this, "Please enable Bluetooth permissions to connect to the tracker.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
