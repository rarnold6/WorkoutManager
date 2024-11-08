package com.example.workoutManager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;

import com.example.workoutManager.data.WorkoutDate;
import com.example.workoutManager.database.WorkoutContract;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;

public class BootCompleteReceiver extends BroadcastReceiver {


    @SuppressLint("Range")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            // Set the alarm here.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try(Cursor cursor = context.getContentResolver().query(WorkoutContract.WorkoutEntry.CONTENT_URI_WORKOUT,
                        new String[]{"*"},null,null)){
                    while(cursor.moveToNext()){
                        String weekday = cursor.getString(cursor.getColumnIndex(WorkoutContract.WorkoutEntry.WORKOUT_WEEKDAY));
                        String time = cursor.getString(cursor.getColumnIndex(WorkoutContract.WorkoutEntry.WORKOUT_TIME));
                        int notificationId = cursor.getInt(cursor.getColumnIndex(WorkoutContract.WorkoutEntry.WORKOUT_NOTIFICATION_ID));

                        DateTimeFormatter formatter = null;
                        LocalTime localTime = null;
                        formatter = DateTimeFormatter.ofPattern("HH:mm");
                        localTime = LocalTime.parse(time, formatter);


                        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
                        calendar.set(Calendar.DAY_OF_WEEK, WorkoutDate.Weekday.fromString(weekday).getValue()); // 1=Sunday, so start from Monday=2
                        calendar.set(Calendar.HOUR_OF_DAY, localTime.getHour());
                        calendar.set(Calendar.MINUTE, localTime.getMinute());
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);

                        calendar.add(Calendar.MINUTE, -10);

                        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                            calendar.add(Calendar.WEEK_OF_YEAR, 1); // Schedule for next week if time passed
                        }

                        Utils.setAlarm(context, calendar, notificationId);
                    }
                }
            }
        }
    }
}
