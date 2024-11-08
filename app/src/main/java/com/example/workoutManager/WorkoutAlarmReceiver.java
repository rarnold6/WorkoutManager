package com.example.workoutManager;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class WorkoutAlarmReceiver extends BroadcastReceiver {

    public static String NOTIFICATION_ID = "notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlarmReceiver", "Alarm received!");
        showNotification(context, intent);
    }

    private void showNotification(Context context, Intent prevIntend) {
        String uniqueKey = String.valueOf(prevIntend.getLongExtra(NOTIFICATION_ID,-1));
        int notificationId = uniqueKey.hashCode() & Integer.MAX_VALUE;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "workout_channel")
                .setSmallIcon(R.mipmap.app_icon)
                .setContentTitle("Time to workout!")
                .setContentText("No pain, no gain!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, builder.build());
    }
}
