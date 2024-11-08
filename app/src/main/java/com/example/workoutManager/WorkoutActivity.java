package com.example.workoutManager;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.workoutManager.data.Workout;
import com.example.workoutManager.stravaConnection.StravaUpload;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkoutActivity extends AppCompatActivity {

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
                        break;
                    case EXERCISE:
                        if(workout.getCurrentNumberOfExercise() == workout.getNumberOfExercisesPerSet() && workout.getCurrentNumberOfSet() == workout.getNumberOfSets()){
                            mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.exercise_finished);
                            mediaPlayer.start();
                            new AlertDialog.Builder(WorkoutActivity.this)
                                    .setTitle("Workout Complete")
                                    .setMessage("Congratulations, you've finished your workout!")
                                    .setCancelable(false) // Prevents dialog from being dismissed by tapping outside
                                    .setPositiveButton("Upload to Strava", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            stravaUpload = new StravaUpload(getApplicationContext(), workout);
                                            ExecutorService executor = Executors.newSingleThreadExecutor();
                                            executor.submit(() -> {
                                                try {
                                                    stravaUpload.getCountDownLatch().await();
                                                    runOnUiThread(() -> {
                                                        Toast.makeText(getApplicationContext(), "Activity uploaded!",Toast.LENGTH_SHORT).show();
                                                        Intent intent = new Intent(WorkoutActivity.this, MainActivity.class);
                                                        startActivity(intent);
                                                        finish();});
                                                } catch (InterruptedException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            });
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
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        stravaUpload.handleRedirect(intent);

    }



}