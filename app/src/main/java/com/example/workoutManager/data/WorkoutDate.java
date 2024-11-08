package com.example.workoutManager.data;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.workoutManager.Utils;
import com.example.workoutManager.database.WorkoutContract;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.TimeZone;

public class WorkoutDate {


    public int getNotificationId() {
        return notificationId;
    }

    public enum Weekday {
        SUNDAY(1, "Sunday"),
        MONDAY(2, "Monday"),
        TUESDAY(3, "Tuesday"),
        WEDNESDAY(4, "Wednesday"),
        THURSDAY(5, "Thursday"),
        FRIDAY(6, "Friday"),
        SATURDAY(7, "Saturday");

        // Field to store the numeric value
        private final int value;

        // Field to store the string representation of the day
        private final String dayName;

        // Constructor to initialize both value and name
        Weekday(int value, String dayName) {
            this.value = value;
            this.dayName = dayName;
        }

        // Getter for the numeric value
        public int getValue() {
            return value;
        }

        // Getter for the string representation
        public String getDayName() {
            return dayName;
        }

        // Static method to get the enum constant by numeric value
        public static Weekday fromValue(int value) {
            for (Weekday weekday : values()) {
                if (weekday.getValue() == value) {
                    return weekday;
                }
            }
            throw new IllegalArgumentException("Invalid value for weekday: " + value);
        }

        // Static method to get the enum constant from a string representation (case insensitive)
        public static Weekday fromString(String dayName) {
            for (Weekday weekday : values()) {
                if (weekday.getDayName().equalsIgnoreCase(dayName)) {
                    return weekday;
                }
            }
            throw new IllegalArgumentException("Invalid weekday name: " + dayName);
        }

        // Override toString to return the string representation (can be customized as needed)
        @Override
        public String toString() {
            return this.dayName;  // Returns the string representation of the day (e.g., "Monday")
        }
    }

    private Weekday weekday;
    private LocalTime localTime;

    private int notificationId;



    @RequiresApi(api = Build.VERSION_CODES.O)
    public WorkoutDate(String dayOfWeek, String localTime){
        this.weekday = Weekday.fromString(dayOfWeek);
        try {
            this.localTime = LocalTime.parse(localTime);
            System.out.println("Parsed LocalTime: " + this.localTime); // Outputs: Parsed LocalTime: 14:30
        } catch (DateTimeParseException e) {
            System.out.println("Invalid time format: " + e.getMessage());
        }

        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.set(Calendar.DAY_OF_WEEK, this.weekday.getValue()); // 1=Sunday, so start from Monday=2
        calendar.set(Calendar.HOUR_OF_DAY, this.localTime.getHour());
        calendar.set(Calendar.MINUTE, this.localTime.getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.add(Calendar.MINUTE, -10);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1); // Schedule for next week if time passed
        }

        String uniqueKey = String.valueOf(calendar.getTimeInMillis());
        this.notificationId = uniqueKey.hashCode() & Integer.MAX_VALUE;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public WorkoutDate(String dayOfWeek, String localTime, int notification_id){
        this.weekday = Weekday.fromString(dayOfWeek);
        try {
            this.localTime = LocalTime.parse(localTime);
            System.out.println("Parsed LocalTime: " + this.localTime); // Outputs: Parsed LocalTime: 14:30
        } catch (DateTimeParseException e) {
            System.out.println("Invalid time format: " + e.getMessage());
        }

        this.notificationId = notification_id;
    }


    public String getDayOfWeek() {
        return weekday.toString();
    }

    public String getLocalTime() {
        return localTime.toString();
    }

    public boolean isBefore(WorkoutDate workoutDate) {
        int thisWeekdayValue = this.weekday.getValue() == 1 ? 8 : this.weekday.getValue();
        int objectWeekdayValue = workoutDate.weekday.getValue() == 1 ? 8 : workoutDate.weekday.getValue();
        if(thisWeekdayValue == objectWeekdayValue){
            LocalTime thisLocalTime = Utils.getTimeInMilliesFromHHmm(this.getLocalTime());
            LocalTime objectLocalTime = Utils.getTimeInMilliesFromHHmm(workoutDate.getLocalTime());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return thisLocalTime.isBefore(objectLocalTime);
            }
            return false;

        } else if (thisWeekdayValue < objectWeekdayValue) {
            return true;
        } else {
            return false;
        }
    }


    public boolean equals(WorkoutDate workoutDate){
        return this.getDayOfWeek().equals(workoutDate.getDayOfWeek()) && this.getLocalTime().equals(workoutDate.getLocalTime());
    }
}
