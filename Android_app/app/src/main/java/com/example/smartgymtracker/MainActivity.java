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


public class MainActivity extends BluetoothActivity {

    protected Button logoutButton;
    protected Button startButton;
    protected Button stopButton;
    TrackingSession trackingSession = new TrackingSession();
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

        // APP-0.4: Start/Stop Tracking buttons
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);

        startButton.setOnClickListener(v -> {
            trackingSession.start();
            Toast.makeText(this, "Tracking started", Toast.LENGTH_SHORT).show();
        });

        stopButton.setOnClickListener(v -> {
            trackingSession.stop();
            Toast.makeText(this, "Tracking stopped", Toast.LENGTH_SHORT).show();
        });

        bleHelp = BluetoothHelper.getInstance(getApplicationContext(), this, "MainActivity");

        if(checkBluetooth(2)) {
            bleHelp.connectToESP32(0);   // Bluetooth just turned on
            bleHelp.connectToESP32(1);
        }
        // Otherwise the scan starts from onRequestPermissionsResult / bluetoothEnableResult
    }

    @Override
    protected void onResume(){
        super.onResume();
        bleHelp.changeActivity(this, "MainActivity");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bleHelp != null) {
            bleHelp.disconnect(0);   // frees the ESP32 so it advertises again
            bleHelp.disconnect(1);
        }
    }

    // Called by BluetoothHelper to show what the connection is doing
    @Override
    public void showStatus(String text) {
        Log.i("MainActivity", "BLE status: " + text);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNotifiedHeartRate(byte[] value){
        displayReceivedData(value);
    }

    @Override
    public void onNotifiedReps(byte[] value){
        displayReceivedData(value);
    }

    public void displayReceivedData(byte[] value) {
        // APP-0.4: only update the screen while tracking
        if (!trackingSession.isTracking()) {
            return;
        }

        // Packet is 40 bytes; a shorter one (e.g. MTU not raised) would crash getFloat()
        if (value == null || value.length < 40) {
            Log.e("MainActivity", "Short packet: " + (value == null ? 0 : value.length) + " bytes");
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
    }
}