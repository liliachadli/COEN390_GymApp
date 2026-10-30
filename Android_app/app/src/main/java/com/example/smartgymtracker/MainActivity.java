package com.example.smartgymtracker;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    ActivityResultLauncher<Intent> bluetoothEnableResult;
    BluetoothHelper bleHelp;

    protected Button logoutButton;
    TextView tv_xaccel, tv_yaccel, tv_zaccel, tv_xgyro, tv_ygyro, tv_zgyro, tv_redcount, tv_ircount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_xaccel = findViewById(R.id.text_xaccel);
        tv_yaccel = findViewById(R.id.text_yaccel);
        tv_zaccel = findViewById(R.id.text_zaccel);
        tv_xgyro = findViewById(R.id.text_xgyro);
        tv_ygyro = findViewById(R.id.text_ygyro);
        tv_zgyro = findViewById(R.id.text_zgyro);
        tv_ircount = findViewById(R.id.text_ircount);
        tv_redcount = findViewById(R.id.text_redcount);

        logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();   // onDestroy() disconnects from the ESP32
        });

        bluetoothEnableResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK){
                        Toast.makeText(this, "Please enable bluetooth to use this application", Toast.LENGTH_LONG).show();
                    } else {
                        bleHelp.scanForESP32();   // Bluetooth just turned on
                    }
                });

        bleHelp = new BluetoothHelper(getApplicationContext(), this);

        boolean perms = getBluetoothPermissions();

        if(!bleHelp.isBluetoothSupported()){
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
            return;   // e.g. emulator: show the screen but don't touch Bluetooth
        }

        if(!bleHelp.isBluetoothEnabled()){
            if(perms) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                bluetoothEnableResult.launch(enableBtIntent);
            }
            else
                Toast.makeText(this, "Please enable bluetooth on this device", Toast.LENGTH_LONG).show();
        }
        else if(perms) {
            bleHelp.scanForESP32();
        }
        // Otherwise the scan starts from onRequestPermissionsResult / bluetoothEnableResult
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bleHelp != null) {
            bleHelp.disconnect();   // frees the ESP32 so it advertises again
        }
    }

    protected boolean getBluetoothPermissions() {
        boolean perms = true;
        List<String> permissionsToRequest = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        } else {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            perms = false;
            ActivityCompat.requestPermissions(MainActivity.this, permissionsToRequest.toArray(new String[0]), 1);
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
                if (bleHelp.isBluetoothEnabled()) {
                    bleHelp.scanForESP32();
                } else {
                    bluetoothEnableResult.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                }
            }
            else {
                Log.e("Permissions","Permission Denied");
                Toast.makeText(this, "Bluetooth permission is needed to connect to the tracker", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Called by BluetoothHelper to show what the connection is doing
    public void showStatus(String text) {
        Log.i("MainActivity", "BLE status: " + text);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void displayReceivedData(byte[] value) {
        // Packet is 40 bytes; a shorter one (e.g. MTU not raised) would crash getFloat()
        if (value == null || value.length < 40) {
            Log.e("MainActivity", "Short packet: " + (value == null ? 0 : value.length) + " bytes");
            bleHelp.setReadyToDisplay();
            return;
        }
        ByteBuffer wrapped = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);

        int protocolVer = wrapped.get();
        int type = wrapped.get();
        int sequence = wrapped.getShort();
        int timeMs = wrapped.getInt();
        float xAccel = wrapped.getFloat();
        float yAccel = wrapped.getFloat();
        float zAccel = wrapped.getFloat();
        float xGyro = wrapped.getFloat();
        float yGyro = wrapped.getFloat();
        float zGyro = wrapped.getFloat();
        int ir = wrapped.getInt();
        int red = wrapped.getInt();

        tv_xaccel.setText(String.format("%.4f", xAccel));
        tv_yaccel.setText(String.format("%.4f", yAccel));
        tv_zaccel.setText(String.format("%.4f", zAccel));
        tv_xgyro.setText(String.format("%.4f", xGyro));
        tv_ygyro.setText(String.format("%.4f", yGyro));
        tv_zgyro.setText(String.format("%.4f", zGyro));
        tv_ircount.setText(Integer.toString(ir));
        tv_redcount.setText(Integer.toString(red));

        // MainActivity is ready to display next value
        bleHelp.setReadyToDisplay();
    }
}