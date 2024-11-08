package com.example.workoutManager.database;

import android.content.ContentResolver;
import android.net.Uri;

public class WorkoutContract {
    private WorkoutContract(){
    }

    public static final String CONTENT_AUTHORITY = "com.example.workoutManager";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_WORKOUT_SCHEDULE = "workout_schedule";

    public static final class WorkoutEntry {
        public static final String TABLE_WORKOUT = "workout_schedule";
        public static final String WORKOUT_WEEKDAY = "weekday";
        public static final String WORKOUT_TIME = "time";

        public static final String WORKOUT_NOTIFICATION_ID = "notification_id";
        public static final String CONTENT_LIST_TYPE_WORKOUT =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_WORKOUT_SCHEDULE;
        public static final String CONTENT_ITEM_TYPE_WORKOUT =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_WORKOUT_SCHEDULE;

        public static final Uri CONTENT_URI_WORKOUT = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_WORKOUT_SCHEDULE);



    }

}
