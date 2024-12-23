package com.example.workoutManager;

import static com.example.workoutManager.stravaConnection.FitFile.generateFitFile;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.example.workoutManager.heartFrequencyDevice.BLEPermissionUtils;
import com.example.workoutManager.heartFrequencyDevice.HeartRateService;
import com.example.workoutManager.stravaConnection.SecureStorageHelper;
import com.example.workoutManager.stravaConnection.StravaUpload;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkoutActivity extends AppCompatActivity {

    private TextView tvBluetoothWorkout;

    private ImageView ivBluetoothWorkout;

    private ProgressBar pbBluetoothWorkout;

    private LinearLayout llBluetoothWorkout;

    private BroadcastReceiver heartRateReceiver;

    private boolean isBound = false;

    private HeartRateService heartRateService;

    private LinkedHashMap<Long,Integer> heartFrequencyHashMap = new LinkedHashMap<>();

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

    private Workout workout;
    private TextView tvSecondsLeft;
    private TextView tvWorkoutPhase;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 10000; // Initial timer value in milliseconds (10 seconds)
    private long timeLeftOnPause = 10000;

    private boolean timerRunning = true;
    private MediaPlayer mediaPlayer; // MediaPlayer for playing the sound

    private StravaUpload stravaUpload;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workout);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.workoutActivity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        registerBluetooth();


        workout = (Workout) getIntent().getSerializableExtra("WORKOUT_OBJECT");

        tvSecondsLeft = (TextView) findViewById(R.id.tvSecondsLeft);
        tvWorkoutPhase = (TextView) findViewById(R.id.tvWorkoutPhase);

        ImageButton btnPausePlay = (android.widget.ImageButton) findViewById(R.id.btnPausePlay);
        btnPausePlay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(!timerRunning){
                    timerRunning = true;
                    timeLeftInMillis = timeLeftOnPause;
                    startTimer();
                    btnPausePlay.setImageResource(android.R.drawable.ic_media_pause);
                } else {
                    pauseTimer();
                    btnPausePlay.setImageResource(android.R.drawable.ic_media_play);
                }
            }
        });

        Button btnExtendTimer = (Button) findViewById(R.id.btnExtendTimer);
        btnExtendTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Extend the timer by 5 seconds (5000 milliseconds)
                if(timeLeftInMillis > 100000){
                    return;
                }
                timeLeftInMillis += 5000;
                startTimer();




            }
        });

        Button btnAbortWorkout = (Button) findViewById(R.id.btnAbortWorkout);
        btnAbortWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseTimer();
                new AlertDialog.Builder(WorkoutActivity.this)
                        .setTitle("Workout abortion")
                        .setMessage("Are you sure you want to abort the workout?\nRemember: Ohne Fleiß, kein Preis!")
                        .setCancelable(false) // Prevents dialog from being dismissed by tapping outside
                        .setPositiveButton("Abort", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Go back to MainActivity
                                Intent intent = new Intent(WorkoutActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish(); // Optional: Finish WorkoutActivity so the user can't go back
                            }
                        })
                        .setNegativeButton("Resume", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                timerRunning = true;
                                timeLeftInMillis = timeLeftOnPause;
                                startTimer();
                                btnPausePlay.setImageResource(android.R.drawable.ic_media_pause);
                            }
                        })
                        .show();
            }
        });

        if (workout != null) {
            startTimer();
        }
    }



    private void startTimer(){
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Create a new countdown timer
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Update the time left
                timeLeftInMillis = millisUntilFinished;
                updateTimer();
            }

            @Override
            public void onFinish() {
                // Timer finished, you can perform an action here

                switch (workout.getCurrentPhase()){
                    case WARMUP:
                    case RECOVERY:
                    case BREAK:
                        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.interface_welcome_131917);
                        mediaPlayer.start();
                        timeLeftInMillis = workout.getExerciseDuration() * 1000L;
                        startTimer();
                        workout.nextPhase();
                        tvWorkoutPhase.setText("Set No. " + workout.getCurrentNumberOfSet() + " / " + workout.getNumberOfSets() +
                                "\nExercise No. " + workout.getCurrentNumberOfExercise() + " / " + workout.getNumberOfExercisesPerSet());
                        if (workout.getCurrentNumberOfExercise() == 1 && workout.getCurrentNumberOfSet() == 1){
                            workout.setStartTimestamp(new Date().getTime());
                            System.out.println("Time stamp started set: " + workout.getStartTimestamp());
                        }
                        break;
                    case EXERCISE:
                        System.out.println("Current number of exercise: " + workout.getCurrentNumberOfExercise());
                        if(workout.getCurrentNumberOfExercise() == workout.getNumberOfExercisesPerSet() && workout.getCurrentNumberOfSet() == workout.getNumberOfSets()){
                            // when the last exercise was done
                            mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.exercise_finished);
                            mediaPlayer.start();
                            workout.setEndTimestamp(new Date().getTime());
                            System.out.println("Time stamp ended set: " + workout.getEndTimestamp());
                            new AlertDialog.Builder(WorkoutActivity.this)
                                    .setTitle("Workout Complete")
                                    .setMessage("Congratulations, you've finished your workout!")
                                    .setCancelable(false) // Prevents dialog from being dismissed by tapping outside
                                    .setPositiveButton("Upload to Strava", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            unregisterReceiver(heartRateReceiver);

                                            double duration = (double) workout.getNumberOfSets() *
                                                    (workout.getNumberOfExercisesPerSet() * workout.getExerciseDuration() + (workout.getNumberOfExercisesPerSet() - 1) * workout.getRecoveryTime()) +
                                                    (workout.getNumberOfSets() - 1) * workout.getBreakTime();
                                            if (heartFrequencyHashMap.size() > (int) duration * 0.5) {
                                                generateFitFile(getApplicationContext(), workout, heartFrequencyHashMap);
                                            }
                                            SecureStorageHelper storageHelper = new SecureStorageHelper(getApplicationContext());
                                            if (storageHelper.getAccessToken().isEmpty()) {
                                                Intent intent = new Intent(WorkoutActivity.this, StravaConnectActivity.class);
                                                intent.putExtra("workout", workout);
                                                intent.putExtra("heartRateData", heartFrequencyHashMap);

                                                startActivity(intent);
                                                finish();
                                            } else {
                                                System.out.println("HEART RATE HASH MAP ITEMS: " + heartFrequencyHashMap.size());
                                                stravaUpload = new StravaUpload(getApplicationContext(), workout, heartFrequencyHashMap);
                                                ExecutorService executor = Executors.newSingleThreadExecutor();
                                                executor.submit(() -> {
                                                    try {
                                                        stravaUpload.getCountDownLatch().await();
                                                        runOnUiThread(() -> {
                                                            Toast.makeText(getApplicationContext(), "Activity uploaded!", Toast.LENGTH_SHORT).show();
                                                            Intent intent = new Intent(WorkoutActivity.this, MainActivity.class);
                                                            startActivity(intent);
                                                            finish();
                                                        });
                                                    } catch (InterruptedException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                });
                                            }
                                        }
                                    })
                                    .setNeutralButton("Back to Main Menu", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Intent intent = new Intent(WorkoutActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    })
                                    .show();
                        } else if (workout.getCurrentNumberOfExercise() == workout.getNumberOfExercisesPerSet()) {
                            mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.game_bonus_144751);
                            mediaPlayer.start();
                            timeLeftInMillis = workout.getBreakTime() * 1000L;
                            startTimer();
                            tvWorkoutPhase.setText("Break No. " + workout.getCurrentNumberOfSet() + " / " + (workout.getNumberOfSets()-1) );
                        } else {
                            mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.old_style_door_bell_101191);
                            mediaPlayer.start();
                            timeLeftInMillis = workout.getRecoveryTime() * 1000L;
                            startTimer();
                            tvWorkoutPhase.setText("Get ready for the next exercise!");
                        }
                        workout.nextPhase();
                        break;
                }
            }
        }.start();
    }

    private void updateTimer() {
        // Convert milliseconds to seconds and update the TextView
        int secondsLeft = (int) (timeLeftInMillis / 1000);
        tvSecondsLeft.setText(String.valueOf(secondsLeft));
    }

    private void pauseTimer(){
        countDownTimer.cancel();
        timeLeftOnPause = timeLeftInMillis;
        timerRunning = false;
    }

    @SuppressLint("NewApi")
    private void registerBluetooth() {
        llBluetoothWorkout = (LinearLayout) findViewById(R.id.llBluetoothWorkout);
        ivBluetoothWorkout = (ImageView) findViewById(R.id.ivBluetoothWorkout);
        tvBluetoothWorkout = (TextView) findViewById(R.id.tvBluetoothWorkout);
        pbBluetoothWorkout = (ProgressBar) findViewById(R.id.pbBluetoothWorkout);

        llBluetoothWorkout.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        heartRateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.BLE_HEART_RATE_UPDATE".equals(intent.getAction())) {
                    if(!bluetoothConnectionExists && !currentlyDisconnecting){
                        pbBluetoothWorkout.setVisibility(View.GONE);
                        ivBluetoothWorkout.setImageResource(R.drawable.bluetooth_connected);
                        bluetoothConnectionExists = true;
                        ivBluetoothWorkout.setVisibility(View.VISIBLE);
                        tvBluetoothWorkout.setVisibility(View.VISIBLE);
                    }
                    int heartRate = intent.getIntExtra("HEART_RATE", -1);
                    switch(workout.getCurrentPhase()){
                        case RECOVERY:
                        case BREAK:
                        case EXERCISE:
                            long currentDate = new Date().getTime();
                            heartFrequencyHashMap.put(currentDate,heartRate);
                            System.out.println("HashMap size: " + heartFrequencyHashMap.size());
                            break;
                        default:
                            break;
                    }

                    //System.out.println("Received Heart Rate: " + heartRate);
                    // Update UI here
                    if(!currentlyDisconnecting) {
                        tvBluetoothWorkout.setText("" + heartRate);
                    }
                }else if ("com.example.BLE_CONNECTED_UPDATE".equals(intent.getAction())) {
                    System.out.println("RECEIVING BLE CONNECTED");
                    boolean bleConnectedUpdate = intent.getBooleanExtra("CONNECTED", false);
                    System.out.println("Received BLE_CONNECTED_UPADTE: " + bleConnectedUpdate);
                    // Update UI here
                    if(!bleConnectedUpdate){
                        pbBluetoothWorkout.setVisibility(View.GONE);
                        ivBluetoothWorkout.setImageResource(android.R.drawable.stat_sys_data_bluetooth);
                        tvBluetoothWorkout.setText("HF");
                        ivBluetoothWorkout.setVisibility(View.VISIBLE);
                        tvBluetoothWorkout.setVisibility(View.VISIBLE);
                        bluetoothConnectionExists = false;
                        Toast.makeText(context,"Connection failed. See more in the settings!",Toast.LENGTH_LONG).show();
                    }

                }
            }
        };

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.BLE_HEART_RATE_UPDATE");
        filter.addAction("com.example.BLE_CONNECTED_UPDATE");
        registerReceiver(heartRateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        llBluetoothWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Animation shake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button_animator);
                llBluetoothWorkout.startAnimation(shake);

                if (!BLEPermissionUtils.hasPermissions(getApplicationContext())) {
                    BLEPermissionUtils.requestPermissions(WorkoutActivity.this);
                    return; // Exit and wait for the user response
                }
                clickBluetooth();

            }
        });
    }

    private void clickBluetooth() {
        SecureStorageHelper secureStorageHelper = new SecureStorageHelper(getApplicationContext());
        if(secureStorageHelper.getHeartRateSensorAddress().isEmpty()){
            Toast.makeText(getApplicationContext(), "Configuration not during workout possible!", Toast.LENGTH_LONG).show();
        } else if(!bluetoothConnectionExists){



            // if there is no bluetooth connection yet
            ivBluetoothWorkout.setVisibility(View.GONE);
            tvBluetoothWorkout.setVisibility(View.GONE);
            pbBluetoothWorkout.setVisibility(View.VISIBLE);

            currentlyDisconnecting = false;
            BluetoothDevice bleDevice = getSavedDevice(secureStorageHelper.getHeartRateSensorAddress());
            Intent serviceIntent = new Intent(WorkoutActivity.this, HeartRateService.class);
            serviceIntent.putExtra("DEVICE", bleDevice);
            startService(serviceIntent);
        } else {
            if (isBound && heartRateService != null) {
                bluetoothConnectionExists = false;
                heartRateService.disconnectDevice();
                pbBluetoothWorkout.setVisibility(View.GONE);
                ivBluetoothWorkout.setImageResource(android.R.drawable.stat_sys_data_bluetooth);
                tvBluetoothWorkout.setText("HF");
                ivBluetoothWorkout.setVisibility(View.VISIBLE);
                tvBluetoothWorkout.setVisibility(View.VISIBLE);
                currentlyDisconnecting = true;
                Toast.makeText(getApplicationContext(), "Disconnected from device", Toast.LENGTH_SHORT).show();
            }
        }
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
        //registerReceiver(heartRateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(heartRateReceiver);
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
        // Cancel the timer to prevent memory leaks
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (isBound && heartRateService != null) {
            heartRateService.disconnectDevice();
        }
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
                clickBluetooth();
            } else {
                System.out.println("Permissions denied. Cannot scan for BLE devices.");
            }
        }
    }




}