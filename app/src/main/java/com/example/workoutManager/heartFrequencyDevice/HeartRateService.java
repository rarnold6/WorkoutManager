package com.example.workoutManager.heartFrequencyDevice;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.UUID;

public class HeartRateService extends Service {

    private static final UUID HEART_RATE_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID HEART_RATE_MEASUREMENT_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    private BluetoothGatt bluetoothGatt;

    private boolean isConnected = false;

    public class LocalBinder extends Binder {
        public HeartRateService getService() {
            return HeartRateService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                System.out.println("Connected to GATT server.");
                System.out.println("Discovering services...");
                gatt.discoverServices(); // Discover services after connection
                isConnected = true;
                sendConnectedUpdate();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                System.out.println("Disconnected from GATT server.");
                isConnected = false;
                sendConnectedUpdate();
                bluetoothGatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("Services discovered successfully.");
                readHeartRateCharacteristic(gatt);
            } else {
                System.out.println("Service discovery failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // Handle incoming notifications or indications
            if (HEART_RATE_MEASUREMENT_UUID.equals(characteristic.getUuid())) {
                int heartRate = parseHeartRate(characteristic);
                sendHeartRateUpdate(heartRate);
                System.out.println("Heart Rate: " + heartRate + " bpm");

            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // BLE connection logic here
        connectToDevice((BluetoothDevice) intent.getParcelableExtra("DEVICE"));
        return START_STICKY; // Keeps the service running
    }


    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        System.out.println("Connecting to: " + device.getName());

        // Connect to the GATT server on the BLE device
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
        if(bluetoothGatt != null) {
            isConnected = true;
            sendConnectedUpdate();
        }
    }

    @SuppressLint("MissingPermission")
    private void readHeartRateCharacteristic(BluetoothGatt gatt) {
        BluetoothGattService heartRateService = gatt.getService(HEART_RATE_SERVICE_UUID);
        if (heartRateService != null) {
            BluetoothGattCharacteristic heartRateCharacteristic = heartRateService.getCharacteristic(HEART_RATE_MEASUREMENT_UUID);
            if (heartRateCharacteristic != null) {
                System.out.println("Found Heart Rate Characteristic, enabling notifications...");
                gatt.setCharacteristicNotification(heartRateCharacteristic, true);

                // Set up the descriptor to enable notifications
                BluetoothGattDescriptor descriptor = heartRateCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                if (descriptor != null) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }
            } else {
                System.out.println("Heart Rate Characteristic not found.");
            }
        } else {
            System.out.println("Heart Rate Service not found.");
        }
    }

    private int parseHeartRate(BluetoothGattCharacteristic characteristic) {
        int flag = characteristic.getProperties();
        int format = (flag & 0x01) != 0 ? BluetoothGattCharacteristic.FORMAT_UINT16 : BluetoothGattCharacteristic.FORMAT_UINT8;
        System.out.println(characteristic.getIntValue(format, 1));
        return characteristic.getIntValue(format, 1);
    }

    private void sendHeartRateUpdate(int heartRate) {
        Intent intent = new Intent("com.example.BLE_HEART_RATE_UPDATE");
        intent.putExtra("HEART_RATE", heartRate);
        sendBroadcast(intent);
    }

    private void sendConnectedUpdate(){
        System.out.println("SEND CONNECTED:" + isConnected);
        Intent intent = new Intent("com.example.BLE_CONNECTED_UPDATE");
        intent.putExtra("CONNECTED", isConnected);
        sendBroadcast(intent);
    }

    @SuppressLint("MissingPermission")
    public void disconnectDevice() {
        if (bluetoothGatt != null) {
            System.out.println("Disconnecting from GATT server...");
            bluetoothGatt.disconnect(); // Disconnect from the device
            bluetoothGatt.close();      // Close and release GATT resources
            bluetoothGatt = null;
            System.out.println("Disconnected and resources released.");
        } else {
            System.out.println("No GATT connection to disconnect.");
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        if (bluetoothGatt != null) {
            disconnectDevice();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    public boolean isConnected() {
        return isConnected;
    }

}
