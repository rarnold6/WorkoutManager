package com.example.workoutManager.heartFrequencyDevice;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.workoutManager.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BLEDevicesAdapter extends BaseAdapter {

    private Activity activity;
    private LinkedList<BluetoothDevice> bluetoothDevices;
    public BLEDevicesAdapter(Activity activity, LinkedList<BluetoothDevice> bluetoothDevices) {
        this.activity = activity;
        this.bluetoothDevices = bluetoothDevices;

    }

    @Override
    public int getCount() {
        return bluetoothDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return bluetoothDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint("MissingPermission")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(activity.getApplicationContext()).inflate(R.layout.bluetooth_device, viewGroup, false);
        }
        BluetoothDevice bleDevice = (BluetoothDevice) getItem(i);


        LinearLayout llBluetoothDevice = view.findViewById(R.id.llBluetoothDevice);
        TextView tvBLEDeviceDescription = view.findViewById(R.id.tvBLEDeviceDescription);
        tvBLEDeviceDescription.setText(bleDevice.getName());

        llBluetoothDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // connect to this device
                Intent serviceIntent = new Intent(activity, HeartRateService.class);
                serviceIntent.putExtra("DEVICE", bleDevice);
                activity.startService(serviceIntent);

                if (activity instanceof BLEScannerActivity) {
                    ((BLEScannerActivity) activity).connectingUI(bleDevice);
                    ((BLEScannerActivity) activity).stopScan();
                }

            }
        });

        return view;
    }

    void addDevice(BluetoothDevice device){
        for(BluetoothDevice bleDevice : bluetoothDevices){
            if(bleDevice.getAddress().equals(device.getAddress())){
                return;
            }
        }
        this.bluetoothDevices.add(device);
    }
}
