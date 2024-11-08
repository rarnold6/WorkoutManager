package com.example.workoutManager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

public class Utils {

    protected static void setAlarm(Context context, Calendar calendar) {
        Intent notificationIntent = new Intent(context, WorkoutAlarmReceiver.class);
        notificationIntent.putExtra(WorkoutAlarmReceiver.NOTIFICATION_ID,calendar.getTimeInMillis());

        String uniqueKey = String.valueOf(calendar.getTimeInMillis());
        int notificationId = uniqueKey.hashCode() & Integer.MAX_VALUE;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 7, pendingIntent);
    }

    protected static void setAlarm(Context context, Calendar calendar, int notificationId) {
        Intent notificationIntent = new Intent(context, WorkoutAlarmReceiver.class);
        notificationIntent.putExtra(WorkoutAlarmReceiver.NOTIFICATION_ID,calendar.getTimeInMillis());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 7, pendingIntent);
    }

    public static LocalTime getTimeInMilliesFromHHmm(String formattedTimeString){
        DateTimeFormatter formatter = null;
        LocalTime localTime = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            formatter = DateTimeFormatter.ofPattern("HH:mm");
            localTime = LocalTime.parse(formattedTimeString, formatter);
            return localTime;
        }
        return null;
    }
}
