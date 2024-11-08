package com.example.workoutManager;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.workoutManager.data.Workout;
import com.example.workoutManager.stravaConnection.SecureStorageHelper;

public class MainActivity extends AppCompatActivity {

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
    }

    private void registerButton(){
        Button btnStartWorkout = (Button) findViewById(R.id.btnStartWorkout);
        ImageButton btnCalendar = (ImageButton) findViewById(R.id.btnCalendar);
        btnCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, WorkoutScheduleActivity.class);
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
                    Workout workout = new Workout(durationExercise,recoveryTime,breakDuration,numberOfSets, numberOfExercisesPerSet);
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
}