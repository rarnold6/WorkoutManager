package com.example.workoutManager.database;

import android.content.ContentResolver;
import android.net.Uri;

public class WorkoutContract {
    private WorkoutContract(){
    }

    public static final String CONTENT_AUTHORITY = "com.example.workoutManager";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_WORKOUT_SCHEDULE = "workout_schedule";
    public static final String PATH_PREDEFINED_WORKOUT = "predefined_workout";
    public static final String PATH_EXERCISES = "exercises";
    public static final String PATH_WORKOUT_EXERCISES = "workout_exercises";


    //table for the workout schedule
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

    // table for the predefinedWorkouts
    public static final class PredefinedWorkoutEntry{
        public static final String TABLE_PREDEFINED_WORKOUT = "predefined_workout";
        public static final String COLUMN_PREDEFINED_WORKOUT_ID = "id";
        public static final String COLUMN_WORKOUT_TITLE = "title";

        public static final String CONTENT_LIST_TYPE_PREDEFINED_WORKOUT =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PREDEFINED_WORKOUT;
        public static final String CONTENT_ITEM_TYPE_PREDEFINED_WORKOUT =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PREDEFINED_WORKOUT;

        public static final Uri CONTENT_URI_PREDEFINED_WORKOUT = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PREDEFINED_WORKOUT);

    }

    // table for the exercises
    public static final class ExerciseEntry{
        public static final String TABLE_EXERCISE = "exercises";
        public static final String COLUMN_EXERCISE_ID = "id";
        public static final String COLUMN_EXERCISE_TITLE = "title";
        public static final String COLUMN_EXERCISE_DESCRIPTION = "description";

        public static final String CONTENT_LIST_TYPE_EXERCISE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_EXERCISES;
        public static final String CONTENT_ITEM_TYPE_EXERCISE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_EXERCISES;

        public static final Uri CONTENT_URI_EXERCISE = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_EXERCISES);
    }

    // table for the workout exercises relationship
    public static final class WorkoutExercisesEntry{
        public static final String TABLE_WORKOUT_EXERCISE = "workout_exercises";

        public static final String COLUMN_WORKOUT_EXERCISE_WORKOUT_ID = "workout_id";

        public static final String COLUMN_WORKOUT_EXERCISE_EXERCISE_ID = "exercises_id";

        public static final String CONTENT_LIST_TYPE_WORKOUT_EXERCISE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_WORKOUT_EXERCISES;
        public static final String CONTENT_ITEM_TYPE_WORKOUT_EXERCISE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_WORKOUT_EXERCISES;

        public static final Uri CONTENT_URI_WORKOUT_EXERCISE = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_WORKOUT_EXERCISES);

    }


}
