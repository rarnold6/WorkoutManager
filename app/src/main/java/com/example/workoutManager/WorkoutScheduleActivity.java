package com.example.workoutManager;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.workoutManager.data.WorkoutDate;
import com.example.workoutManager.database.WorkoutContract;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.TimeZone;

public class WorkoutScheduleActivity extends AppCompatActivity {

    private CheckBox cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday;
    private TimePicker timePicker;
    private Button btnSaveSchedule;

    private ScheduleBaseAdapter scheduleBaseAdapter;

    LinkedList<WorkoutDate> workoutDates = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workout_schedule2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.workoutSchedule), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        createNotificationChannel();

        ImageButton btnBack = (ImageButton) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WorkoutScheduleActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        cbMonday = findViewById(R.id.cbMonday);
        cbTuesday = findViewById(R.id.cbTuesday);
        cbWednesday = findViewById(R.id.cbWednesday);
        cbThursday = findViewById(R.id.cbThursday);
        cbFriday = findViewById(R.id.cbFriday);
        cbSaturday = findViewById(R.id.cbSaturday);
        cbSunday = findViewById(R.id.cbSunday);

        setSingleChoiceCheckbox(cbMonday);
        setSingleChoiceCheckbox(cbTuesday);
        setSingleChoiceCheckbox(cbWednesday);
        setSingleChoiceCheckbox(cbThursday);
        setSingleChoiceCheckbox(cbFriday);
        setSingleChoiceCheckbox(cbSaturday);
        setSingleChoiceCheckbox(cbSunday);


        timePicker = findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        btnSaveSchedule = findViewById(R.id.btnSaveSchedule);

        btnSaveSchedule.setOnClickListener(view -> setWorkoutAlarmsAndStore());

        retrieveSchedule();

        ListView lvWorkoutSchedule = (ListView) findViewById(R.id.lvWorkoutSchedule);
        this.scheduleBaseAdapter = new ScheduleBaseAdapter(getApplicationContext(),workoutDates);
        lvWorkoutSchedule.setAdapter(this.scheduleBaseAdapter);

    }

    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            String channel_id = "workout_channel";
            CharSequence name = getString(R.string.channel_name);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channel_id, name, importance);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
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

    private void setSingleChoiceCheckbox(CheckBox checkBox) {
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Uncheck all other checkboxes
                uncheckAllExcept(checkBox);
            }
        });
    }

    private void uncheckAllExcept(CheckBox selectedCheckbox) {
        if (selectedCheckbox != cbMonday) cbMonday.setChecked(false);
        if (selectedCheckbox != cbTuesday) cbTuesday.setChecked(false);
        if (selectedCheckbox != cbWednesday) cbWednesday.setChecked(false);
        if (selectedCheckbox != cbThursday) cbThursday.setChecked(false);
        if (selectedCheckbox != cbFriday) cbFriday.setChecked(false);
        if (selectedCheckbox != cbSaturday) cbSaturday.setChecked(false);
        if (selectedCheckbox != cbSunday) cbSunday.setChecked(false);
    }

    private void setWorkoutAlarmsAndStore() {
        boolean[] daysSelected = {
                cbMonday.isChecked(), cbTuesday.isChecked(), cbWednesday.isChecked(),
                cbThursday.isChecked(), cbFriday.isChecked(), cbSaturday.isChecked(), cbSunday.isChecked()
        };

        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        for (int i = 0; i < daysSelected.length; i++) {
            if (daysSelected[i]) {
                Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
                calendar.set(Calendar.DAY_OF_WEEK, i + 2); // 1=Sunday, so start from Monday=2
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                calendar.add(Calendar.MINUTE, -10);

                if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 1); // Schedule for next week if time passed
                }

                WorkoutDate workoutDate = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    workoutDate = new WorkoutDate(WorkoutDate.Weekday.fromValue(i+2 == 8 ? 1 : i+2).getDayName(),String.format("%02d:%02d", hour, minute));
                }

                if(!checkDateAlreadyExists(workoutDate)) {
                    insertWorkoutDate(workoutDate);
                    workoutDates.add(workoutDate);
                    Utils.setAlarm(getApplicationContext(),calendar, workoutDate.getNotificationId());
                    scheduleBaseAdapter.updateData(workoutDates);
                    Toast.makeText(this, "Workout schedule saved!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Date already saved.", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    private boolean checkDateAlreadyExists(WorkoutDate workoutDate) {
        for(WorkoutDate savedWorkoutDate : workoutDates){
            if(workoutDate.equals(savedWorkoutDate)){
                return true;
            }
        }
        return false;
    }

    private void insertWorkoutDate(WorkoutDate workoutDate) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(WorkoutContract.WorkoutEntry.WORKOUT_WEEKDAY,workoutDate.getDayOfWeek());
        contentValues.put(WorkoutContract.WorkoutEntry.WORKOUT_TIME,workoutDate.getLocalTime());
        contentValues.put(WorkoutContract.WorkoutEntry.WORKOUT_NOTIFICATION_ID,workoutDate.getNotificationId());
        getContentResolver().insert(WorkoutContract.WorkoutEntry.CONTENT_URI_WORKOUT,contentValues);
    }
}