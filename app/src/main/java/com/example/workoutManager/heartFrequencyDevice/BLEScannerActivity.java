package com.example.workoutManager.heartFrequencyDevice;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.workoutManager.R;
import com.example.workoutManager.Settings;
import com.example.workoutManager.stravaConnection.SecureStorageHelper;

import org.w3c.dom.Text;

import java.util.LinkedList;

public class BLEScannerActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;

    private boolean scanning = false;

    LinkedList<BluetoothDevice> bluetoothDevices = new LinkedList<>();

    private BLEDevicesAdapter bleDevicesAdapter;

    private TextView tvEmptyViewScanDevices;

    private ListView lvAvailableDevices;

    private LinearLayout llLoading;

    private TextView tvConnectingToDevice;

    private LinearLayout llHeartFrequency;

    private TextView tvHeartFrequency;

    private TextView tvAvailableDevices;

    private BroadcastReceiver heartRateReceiver;

    private BroadcastReceiver bleConnectedReceiver;

    private boolean connected;

    private BluetoothDevice bluetoothDeviceConnecting;

    private TextView tvConntectedToBLE;

    private Button btnScanForDevices;

    private boolean isBound = false;

    private HeartRateService heartRateService;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            HeartRateService.LocalBinder binder = (HeartRateService.LocalBinder) service;
            heartRateService = binder.getService();
            isBound = true;
            System.out.println("Service connected.");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            System.out.println("Service disconnected.");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_ble_device);

        ImageButton btnBackBLE = (ImageButton) findViewById(R.id.btnBackBLE);
        btnBackBLE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(BLEScannerActivity.this, Settings.class);
                startActivity(intent);
            }
        });


        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();


        bleDevicesAdapter = new BLEDevicesAdapter(this,bluetoothDevices);
        lvAvailableDevices = (ListView) findViewById(R.id.lvAvailableDevices);
        LinearLayout llEmptyViewScanDevices = (LinearLayout) findViewById(R.id.llEmptyViewScanDevices);
        tvEmptyViewScanDevices = (TextView) findViewById(R.id.tvEmptyViewScanDevices);
        lvAvailableDevices.setEmptyView(llEmptyViewScanDevices);
        lvAvailableDevices.setAdapter(bleDevicesAdapter);

        tvAvailableDevices = (TextView) findViewById(R.id.tvAvailableDevices);

        llLoading = (LinearLayout) findViewById(R.id.llLoading);
        tvConnectingToDevice = (TextView) findViewById(R.id.tvConnectingToDevice);

        llHeartFrequency = (LinearLayout) findViewById(R.id.llHeartFrequency);
        tvHeartFrequency = (TextView) findViewById(R.id.tvHeartFrequency);
        tvConntectedToBLE = (TextView) findViewById(R.id.tvConntectedToBLE);

        heartRateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.BLE_HEART_RATE_UPDATE".equals(intent.getAction())) {
                    if(!connected) {
                        connectedUI();
                        connected = true;
                    }
                    int heartRate = intent.getIntExtra("HEART_RATE", -1);
                    //System.out.println("Received Heart Rate: " + heartRate);
                    // Update UI here
                    tvHeartFrequency.setText("Heart Rate: " + heartRate + " bpm");
                }
            }
        };

        bleConnectedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.BLE_CONNECTED_UPDATE".equals(intent.getAction())) {
                    System.out.println("RECEIVING BLE CONNECTED");
                    connected = intent.getBooleanExtra("CONNECTED", false);
                    System.out.println("Received BLE_CONNECTED_UPADTE: " + connected);
                    // Update UI here
                    connectedUI();
                }
            }
        };




        btnScanForDevices = (Button) findViewById(R.id.btnScanForDevices);
        btnScanForDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scanning && !connected) {
                    scanning = false;
                    // stop scanning for BLE devices
                    stopScan();
                    tvEmptyViewScanDevices.setText("Scan for devices!");
                    btnScanForDevices.setText("Scan for your device");
                } else if(connected) {
                    connected = false;
                    scanningUI();
                    scanning = false;
                    btnScanForDevices.setText("Scan for devices!");

                    //disconnect the Bluetooth
                    if (isBound && heartRateService != null) {
                        heartRateService.disconnectDevice();
                        Toast.makeText(getApplicationContext(), "Disconnected from device", Toast.LENGTH_SHORT).show();
                    }
                } else {
                        scanning = true;
                        // start scanning for BLE devices
                        startScan();
                        tvEmptyViewScanDevices.setText("Scanning for devices...");
                        btnScanForDevices.setText("Stop scanning");
                    }
                }

        });
    }

    @SuppressLint("MissingPermission")
    void scanningUI(){
        lvAvailableDevices.setVisibility(View.VISIBLE);
        tvAvailableDevices.setText("Available devices:");

        llLoading.setVisibility(View.GONE);

        llHeartFrequency.setVisibility(View.GONE);
    }

    @SuppressLint("MissingPermission")
    void connectingUI(BluetoothDevice bleDevice){
        lvAvailableDevices.setVisibility(View.GONE);
        tvAvailableDevices.setText("Available devices:");

        tvConnectingToDevice.setText("Connecting to device: " + bleDevice.getName());
        llLoading.setVisibility(View.VISIBLE);

        llHeartFrequency.setVisibility(View.GONE);
        this.bluetoothDeviceConnecting = bleDevice;
    }

    @SuppressLint("MissingPermission")
    void connectedUI(){
        lvAvailableDevices.setVisibility(View.GONE);

        llLoading.setVisibility(View.GONE);

        tvAvailableDevices.setText("Connected device:");
        tvConntectedToBLE.setText(bluetoothDeviceConnecting.getName());
        llHeartFrequency.setVisibility(View.VISIBLE);
        btnScanForDevices.setText("Disconnect device");

        SecureStorageHelper secureStorageHelper = new SecureStorageHelper(getApplicationContext());
        secureStorageHelper.saveSensor(bluetoothDeviceConnecting.getName(),bluetoothDeviceConnecting.getAddress());

    }

    // Start scanning
    @SuppressLint("MissingPermission")
    public void startScan() {
        // Check if permissions are granted
        if (!BLEPermissionUtils.hasPermissions(this.getApplicationContext())) {
            BLEPermissionUtils.requestPermissions(this);
            return; // Exit and wait for the user response
        }
        bluetoothLeScanner.startScan(scanCallback);
    }

    // Stop scanning
    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (!BLEPermissionUtils.hasPermissions(this.getApplicationContext())) {
            BLEPermissionUtils.requestPermissions(this);
            return; // Exit and wait for the user response
        }
        bluetoothLeScanner.stopScan(scanCallback);
    }

    // Callback to handle scanned devices
    private ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (!BLEPermissionUtils.hasPermissions(BLEScannerActivity.this)) {
                BLEPermissionUtils.requestPermissions(BLEScannerActivity.this);
                return; // Exit and wait for the user response
            }
            if(result.getDevice().getName() != null && !result.getDevice().getName().isEmpty()) {
                bleDevicesAdapter.addDevice(result.getDevice());
                bleDevicesAdapter.notifyDataSetChanged();
            }



//            BluetoothDevice device = result.getDevice();
//            if (!BLEPermissionUtils.hasPermissions(BLEScannerActivity.this)) {
//                BLEPermissionUtils.requestPermissions(BLEScannerActivity.this);
//                return; // Exit and wait for the user response
//            }
//            @SuppressLint("MissingPermission") String deviceName = device.getName();
//            String deviceAddress = device.getAddress();
//
//            if (deviceName != null && deviceName.contains("Heart")) {
//                // Found a heart rate device
//                System.out.println("Found device: " + deviceName + " Address: " + deviceAddress);
//                stopScan(); // Stop scanning when device is found
//                connectToDevice(device);
//            }
        }
    };

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        if (!BLEPermissionUtils.hasPermissions(this.getApplicationContext())) {
            BLEPermissionUtils.requestPermissions(this);
            return; // Exit and wait for the user response
        }
        System.out.println("Connecting to: " + device.getName());
        // Proceed to Step 3: Connect and read heart rate data


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            boolean allGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                System.out.println("Permissions granted! Starting BLE scan...");
                startScan(); // Restart the scan or required process
            } else {
                System.out.println("Permissions denied. Cannot scan for BLE devices.");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, HeartRateService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("com.example.BLE_HEART_RATE_UPDATE");
        registerReceiver(heartRateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        registerReceiver(bleConnectedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(heartRateReceiver);
        unregisterReceiver(bleConnectedReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound && heartRateService != null) {
            heartRateService.disconnectDevice();
        }
    }


}
