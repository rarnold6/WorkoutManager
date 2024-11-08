package com.example.workoutManager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.workoutManager.data.WorkoutDate;
import com.example.workoutManager.database.WorkoutContract;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.TimeZone;

public class ScheduleBaseAdapter extends BaseAdapter {
    private Context context;
    private LinkedList<WorkoutDate> workoutDates;
    public ScheduleBaseAdapter(Context context, LinkedList<WorkoutDate> workoutDates) {
        this.context = context;
        this.workoutDates = workoutDates;

        Collections.sort(workoutDates, new Comparator<WorkoutDate>() {
            @Override
            public int compare(WorkoutDate workoutDate1, WorkoutDate workoutDate2) {
                // Assuming you want to sort by weekday and time
                // For example, comparing weekdays alphabetically

                if(workoutDate1.isBefore(workoutDate2)){
                    return -1;
                } else if(workoutDate1.equals(workoutDate2)){
                     return 0;
                } else {
                    return 1;
                }

            }
        });
    }

    @Override
    public int getCount() {
        return workoutDates.size();
    }

    @Override
    public Object getItem(int i) {
        return workoutDates.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.weekday_element, viewGroup, false);
        }

        WorkoutDate workoutDate = (WorkoutDate) getItem(i);

        TextView tvWeekday = (TextView) view.findViewById(R.id.tvWeekday);
        TextView tvTime = (TextView) view.findViewById(R.id.tvTime);
        ImageButton btnDeleteDate = (ImageButton) view.findViewById(R.id.btnDeleteDate);

        tvWeekday.setText(workoutDate.getDayOfWeek());
        tvTime.setText(workoutDate.getLocalTime());
        btnDeleteDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String weekday = tvWeekday.getText().toString();
                String time = tvTime.getText().toString();


                Uri deleteUri = Uri.withAppendedPath(WorkoutContract.WorkoutEntry.CONTENT_URI_WORKOUT, weekday + "/" +time);
                /*String selection = WorkoutContract.WorkoutEntry.WORKOUT_WEEKDAY + "=? AND " +
                        WorkoutContract.WorkoutEntry.WORKOUT_TIME + "=?";
                String[] selectionArgs = new String[]{ weekday, time };*/
                int rowsDeleted = context.getContentResolver().delete(deleteUri,null,null);

                if (rowsDeleted > 0) {
                    // Remove the item from the list and update the view
                    workoutDates.remove(i);
                    notifyDataSetChanged();
                    Toast.makeText(context, "Date deleted successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Error deleting date!", Toast.LENGTH_SHORT).show();
                }


                DateTimeFormatter formatter = null;
                LocalTime localTime = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    formatter = DateTimeFormatter.ofPattern("HH:mm");
                    localTime = LocalTime.parse(time, formatter);
                }




                Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
                calendar.set(Calendar.DAY_OF_WEEK, WorkoutDate.Weekday.fromString(weekday).getValue()); // 1=Sunday, so start from Monday=2
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    calendar.set(Calendar.HOUR_OF_DAY, localTime.getHour());
                    calendar.set(Calendar.MINUTE, localTime.getMinute());
                }
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                calendar.add(Calendar.MINUTE, -10);

                if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 1); // Schedule for next week if time passed
                }
                cancelAlarm(workoutDate);
                //notifyDataSetChanged
                Toast.makeText(context, "Date deleted successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void cancelAlarm(WorkoutDate workoutDate) {
        Intent intent = new Intent(context, WorkoutAlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                workoutDate.getNotificationId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Cancel the alarm by passing the same PendingIntent
        alarmManager.cancel(pendingIntent);
    }

    public void updateData(LinkedList<WorkoutDate> newWorkoutDates) {
        this.workoutDates = newWorkoutDates;
        Collections.sort(workoutDates, new Comparator<WorkoutDate>() {
            @Override
            public int compare(WorkoutDate workoutDate1, WorkoutDate workoutDate2) {
                // Assuming you want to sort by weekday and time
                // For example, comparing weekdays alphabetically

                if(workoutDate1.isBefore(workoutDate2)){
                    return -1;
                } else if(workoutDate1.equals(workoutDate2)){
                    return 0;
                } else {
                    return 1;
                }

            }
        });
        notifyDataSetChanged(); // Notify the adapter that the data has changed
    }
}
