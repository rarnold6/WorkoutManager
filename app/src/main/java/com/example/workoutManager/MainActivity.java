package com.example.workoutManager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.workoutManager.data.Workout;
import com.example.workoutManager.heartFrequencyDevice.BLEScannerActivity;
import com.example.workoutManager.heartFrequencyDevice.HeartRateService;
import com.example.workoutManager.stravaConnection.SecureStorageHelper;

import java.sql.Time;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private TextView tvBluetoothMain;

    private ImageView ivBluetoothMain;

    private LinearLayout llBluetoothMain;

    private BroadcastReceiver heartRateReceiver;

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

    private boolean bluetoothConnectionExists = false;

    private boolean currentlyDisconnecting = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        registerButton();
        registerBluetooth();
    }

    private void registerBluetooth() {
        llBluetoothMain = (LinearLayout) findViewById(R.id.llBluetoothMain);
        ivBluetoothMain = (ImageView) findViewById(R.id.ivBluetoothMain);
        tvBluetoothMain = (TextView) findViewById(R.id.tvBluetoothMain);

        heartRateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.BLE_HEART_RATE_UPDATE".equals(intent.getAction())) {
                    if(!bluetoothConnectionExists && !currentlyDisconnecting){
                        ivBluetoothMain.setImageResource(R.drawable.bluetooth_connected);
                        bluetoothConnectionExists = true;
                    }

                    int heartRate = intent.getIntExtra("HEART_RATE", -1);
                    //System.out.println("Received Heart Rate: " + heartRate);
                    // Update UI here
                    if(!currentlyDisconnecting) {
                        tvBluetoothMain.setText("" + heartRate);
                    }
                }
            }
        };

        llBluetoothMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SecureStorageHelper secureStorageHelper = new SecureStorageHelper(getApplicationContext());
                if(secureStorageHelper.getHeartRateSensorAddress().isEmpty()){
                    Intent intent = new Intent(MainActivity.this, BLEScannerActivity.class);
                    startActivity(intent);
                } else if(!bluetoothConnectionExists){
                    // if there is no bluetooth connection yet
                    currentlyDisconnecting = false;
                    BluetoothDevice bleDevice = getSavedDevice(secureStorageHelper.getHeartRateSensorAddress());
                    Intent serviceIntent = new Intent(MainActivity.this, HeartRateService.class);
                    serviceIntent.putExtra("DEVICE", bleDevice);
                    startService(serviceIntent);
                } else {
                    if (isBound && heartRateService != null) {
                        bluetoothConnectionExists = false;
                        heartRateService.disconnectDevice();
                        ivBluetoothMain.setImageResource(android.R.drawable.stat_sys_data_bluetooth);
                        tvBluetoothMain.setText("HF");
                        currentlyDisconnecting = true;
                        Toast.makeText(getApplicationContext(), "Disconnected from device", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    public BluetoothDevice getSavedDevice(String deviceAddress) {
        if (deviceAddress != null) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            System.out.println("Retrieved device: " + deviceAddress);
            return device;
        } else {
            System.out.println("No saved device found.");
            return null;
        }
    }


    private void registerButton(){
        Button btnStartWorkout = (Button) findViewById(R.id.btnStartWorkout);
        ImageButton btnSettings = (ImageButton) findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Settings.class);
                startActivity(intent);
            }
        });

        EditText etDurationExercise = (EditText) findViewById(R.id.etDurationExercise);
        EditText etBreakDuration = (EditText) findViewById(R.id.etBreakDuration);
        EditText etRecoveryTime = (EditText) findViewById(R.id.etRecoveryTime);
        EditText etNumberOfExercisesPerSet = (EditText) findViewById(R.id.etNumberOfExercisesPerSet);
        EditText etNumberOfSets = (EditText) findViewById(R.id.etNumberOfSets);
        CheckBox cbStandardSettings = (CheckBox) findViewById(R.id.cbStandardSettings);

        SecureStorageHelper secureStorageHelper = new SecureStorageHelper(getApplicationContext());

        etDurationExercise.setText(String.valueOf(secureStorageHelper.getDurationExercise()));
        etBreakDuration.setText(String.valueOf(secureStorageHelper.getBreakDuration()));
        etRecoveryTime.setText(String.valueOf(secureStorageHelper.getRecoveryTime()));
        etNumberOfExercisesPerSet.setText(String.valueOf(secureStorageHelper.getNumberOfExercisesPerSet()));
        etNumberOfSets.setText(String.valueOf(secureStorageHelper.getNumberOfSets()));


        btnStartWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(etDurationExercise.getText().toString().isEmpty() || etBreakDuration.getText().toString().isEmpty() || etRecoveryTime.getText().toString().isEmpty()){
                    showAlertDialog("Missing values","Check your input for the duration times.");
                } else if (Integer.parseInt(etDurationExercise.getText().toString()) < 10) {
                    showAlertDialog("Exercise duration too low","You should do at least 10 seconds per exercise.");
                } else if (Integer.parseInt(etDurationExercise.getText().toString()) > 120) {
                    showAlertDialog("Exercise duration too high","You should not do more than 120 seconds per exercise.");
                } else if (Integer.parseInt(etRecoveryTime.getText().toString()) > Integer.parseInt(etDurationExercise.getText().toString())) {
                    showAlertDialog("Recovery time too high","Your recovery time should at most meet the duration of the exercise.");
                } else if (Integer.parseInt(etRecoveryTime.getText().toString()) < Integer.parseInt(etDurationExercise.getText().toString())*0.2) {
                    showAlertDialog("Recovery time too low","Your recovery time should be higher than 20 percent of the duration of the exercise. In this case at least "
                            + (int) Integer.parseInt(etDurationExercise.getText().toString()) * 0.2);
                } else if (Integer.parseInt(etBreakDuration.getText().toString()) < 60 || Integer.parseInt(etBreakDuration.getText().toString()) > 240) {
                    showAlertDialog("Break duration","Your break should be between 60 seconds and 240 seconds.");
                } else if (Integer.parseInt(etNumberOfExercisesPerSet.getText().toString()) < 3 || Integer.parseInt(etNumberOfExercisesPerSet.getText().toString()) > 15) {
                    showAlertDialog("Number of exercises error","You should do between (inclusive) 3 and 15 exercises per set.");
                } else if (Integer.parseInt(etNumberOfSets.getText().toString()) < 1 || Integer.parseInt(etNumberOfSets.getText().toString()) > 6) {
                    showAlertDialog("Number of sets error","You should do between (inclusive) 1 and 6 rounds.");
                } else {
                    int durationExercise = Integer.parseInt(etDurationExercise.getText().toString());
                    int recoveryTime = Integer.parseInt(etRecoveryTime.getText().toString());
                    int breakDuration = Integer.parseInt(etBreakDuration.getText().toString());
                    int numberOfSets = Integer.parseInt(etNumberOfSets.getText().toString());
                    int numberOfExercisesPerSet = Integer.parseInt(etNumberOfExercisesPerSet.getText().toString());
                    if(cbStandardSettings.isChecked()){
                        secureStorageHelper.saveStandardSettings(durationExercise,recoveryTime,breakDuration,numberOfSets, numberOfExercisesPerSet);
                    }

                    Workout workout = new Workout(durationExercise,recoveryTime,breakDuration,numberOfSets, numberOfExercisesPerSet, new Date().getTime());
                    startWorkout(workout);
                }
            }
        });
    }

    private void startWorkout(Workout workout) {
        Intent intent = new Intent(MainActivity.this, WorkoutActivity.class);
        intent.putExtra("WORKOUT_OBJECT", workout);
        startActivity(intent); // Start the new Activity
    }


    private void showAlertDialog(String title, String message){
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(heartRateReceiver);
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