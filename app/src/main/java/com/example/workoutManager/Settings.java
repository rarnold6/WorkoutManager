package com.example.workoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.workoutManager.data.WorkoutDate;
import com.example.workoutManager.database.WorkoutContract;
import com.example.workoutManager.heartFrequencyDevice.BLEScannerActivity;
import com.example.workoutManager.stravaConnection.SecureStorageHelper;

import java.util.LinkedList;

public class Settings extends AppCompatActivity {

    LinkedList<WorkoutDate> workoutDates = new LinkedList<>();
    private ScheduleBaseAdapter scheduleBaseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Get existing padding values from the view
            int existingPaddingLeft = v.getPaddingLeft();
            int existingPaddingTop = v.getPaddingTop();
            int existingPaddingRight = v.getPaddingRight();
            int existingPaddingBottom = v.getPaddingBottom();

            // Add the system bars insets to the existing padding
            v.setPadding(
                    existingPaddingLeft + systemBars.left,
                    existingPaddingTop + systemBars.top,
                    existingPaddingRight + systemBars.right,
                    existingPaddingBottom + systemBars.bottom
            );

            return insets;
        });

        ImageButton btnBackSettings = (ImageButton) findViewById(R.id.btnBackSettings);
        btnBackSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.this, MainActivity.class);
                startActivity(intent);
            }
        });

        LinearLayout llStravaRef = (LinearLayout) findViewById(R.id.llStravaRef);
        llStravaRef.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.this, StravaConnectActivity.class);
                startActivity(intent);
            }
        });

        LinearLayout llHeartRateSensor = (LinearLayout) findViewById(R.id.llHeartRateSensor);
        llHeartRateSensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.this, BLEScannerActivity.class);
                startActivity(intent);
            }
        });

        ImageView ivAddDate = (ImageView) findViewById(R.id.ivAddDate);
        ivAddDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.this, WorkoutScheduleActivity.class);
                startActivity(intent);
            }
        });

        SecureStorageHelper secureStorageHelper = new SecureStorageHelper(getApplicationContext());
        String username = secureStorageHelper.getUsername();
        String heartRateSensorName = secureStorageHelper.getHeartRateSensor();

        TextView tvStravaUsername = (TextView) findViewById(R.id.tvStravaUsername);

        if(username.isEmpty()){
            tvStravaUsername.setText("Not connected");
        } else {
            tvStravaUsername.setText(username);
        }

        TextView tvHeartRateSensor = (TextView) findViewById(R.id.tvHeartRateSensor);

        if(heartRateSensorName.isEmpty()){
            tvHeartRateSensor.setText("no standard device");
        } else {
            tvHeartRateSensor.setText(heartRateSensorName);
        }

        retrieveSchedule();


        LinearLayout emptyView = (LinearLayout) findViewById(R.id.emptyView);
        ListView lvWorkoutSchedule = (ListView) findViewById(R.id.lvWorkoutSchedule);
        lvWorkoutSchedule.setEmptyView(emptyView);
        this.scheduleBaseAdapter = new ScheduleBaseAdapter(getApplicationContext(),workoutDates);
        lvWorkoutSchedule.setAdapter(this.scheduleBaseAdapter);


        ImageView ibAddDate = (ImageView) findViewById(R.id.ibAddDate);
        ibAddDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.this, WorkoutScheduleActivity.class);
                startActivity(intent);
            }
        });

    }

    private void retrieveSchedule() {
        try (Cursor cursorWorkoutDates = getContentResolver().query(WorkoutContract.WorkoutEntry.CONTENT_URI_WORKOUT,
                new String[]{"*"},
                null,
                null,
                null)) {
            while (cursorWorkoutDates.moveToNext()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    @SuppressLint("Range") WorkoutDate workoutDate = new WorkoutDate(cursorWorkoutDates.getString(cursorWorkoutDates.getColumnIndex(WorkoutContract.WorkoutEntry.WORKOUT_WEEKDAY)),
                            cursorWorkoutDates.getString(cursorWorkoutDates.getColumnIndex(WorkoutContract.WorkoutEntry.WORKOUT_TIME)),
                            cursorWorkoutDates.getInt(cursorWorkoutDates.getColumnIndex(WorkoutContract.WorkoutEntry.WORKOUT_NOTIFICATION_ID)));
                    workoutDates.add(workoutDate);
                }
            }
        }
    }
}
