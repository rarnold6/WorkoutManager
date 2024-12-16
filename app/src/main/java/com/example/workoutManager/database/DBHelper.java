package com.example.workoutManager.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "workout_manager.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_WORKOUT = "workout_schedule";
    public static final String COLUMN_WEEKDAY = "weekday";
    public static final String COLUMN_TIME = "time";

    public static final String NOTIFICATION_ID = "notification_id";

    public static final String TABLE_PREDEFINED_WORKOUT = "predefined_workout";
    public static final String COLUMN_PREDEFINED_WORKOUT_ID = "id";
    public static final String COLUMN_WORKOUT_TITLE = "title";

    public static final String TABLE_EXERCISE = "exercises";
    public static final String COLUMN_EXERCISE_ID = "id";
    public static final String COLUMN_EXERCISE_TITLE = "title";
    public static final String COLUMN_EXERCISE_DESCRIPTION = "description";

    public static final String TABLE_WORKOUT_EXERCISE = "workout_exercises";
    public static final String COLUMN_WORKOUT_EXERCISE_WORKOUT_ID = "workout_id";
    public static final String COLUMN_WORKOUT_EXERCISE_EXERCISE_ID = "exercises_id";


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }



    @Override
    public void onCreate(SQLiteDatabase database) {
        String sqlWorkoutDates = "CREATE TABLE " + TABLE_WORKOUT + " ("
                + COLUMN_WEEKDAY + " TEXT, "
                + COLUMN_TIME + " TEXT, "
                + NOTIFICATION_ID + " INTEGER, "
                + "PRIMARY KEY(" + COLUMN_WEEKDAY + ", " + COLUMN_TIME + ")"
                + ");";
        database.execSQL(sqlWorkoutDates);

        String sqlPredefinedWorkouts = "CREATE TABLE " + TABLE_PREDEFINED_WORKOUT + " ("
                + COLUMN_PREDEFINED_WORKOUT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_WORKOUT_TITLE + " TEXT NOT NULL"
                + ");";
        database.execSQL(sqlPredefinedWorkouts);

        String sqlExercises = "CREATE TABLE " + TABLE_EXERCISE + " ("
                + COLUMN_EXERCISE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_EXERCISE_TITLE + " TEXT NOT NULL, "
                + COLUMN_EXERCISE_DESCRIPTION + " TEXT"
                + ");";
        database.execSQL(sqlExercises);

        String sqlWorkoutExercises = "CREATE TABLE " + TABLE_WORKOUT_EXERCISE + " ("
                + COLUMN_WORKOUT_EXERCISE_WORKOUT_ID + " INTEGER NOT NULL, "
                + COLUMN_WORKOUT_EXERCISE_EXERCISE_ID + " INTEGER NOT NULL, "
                + "PRIMARY KEY(" + COLUMN_WORKOUT_EXERCISE_WORKOUT_ID + ", " + COLUMN_WORKOUT_EXERCISE_EXERCISE_ID + "), "
                + "FOREIGN KEY(" + COLUMN_WORKOUT_EXERCISE_WORKOUT_ID + ") REFERENCES " + TABLE_PREDEFINED_WORKOUT + "(" + COLUMN_PREDEFINED_WORKOUT_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY(" + COLUMN_WORKOUT_EXERCISE_EXERCISE_ID + ") REFERENCES " + TABLE_EXERCISE + "(" + COLUMN_EXERCISE_ID + ") ON DELETE CASCADE"
                + ");";
        database.execSQL(sqlWorkoutExercises);



        Log.i("DATABASE CREATED", "TABLE WORKOUT");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if(oldVersion < 3){
            String sqlPredefinedWorkouts = "CREATE TABLE " + TABLE_PREDEFINED_WORKOUT + " ("
                    + COLUMN_PREDEFINED_WORKOUT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_WORKOUT_TITLE + " TEXT NOT NULL"
                    + ");";
            database.execSQL(sqlPredefinedWorkouts);

            String sqlExercises = "CREATE TABLE " + TABLE_EXERCISE + " ("
                    + COLUMN_EXERCISE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_EXERCISE_TITLE + " TEXT NOT NULL, "
                    + COLUMN_EXERCISE_DESCRIPTION + " TEXT"
                    + ");";
            database.execSQL(sqlExercises);

            String sqlWorkoutExercises = "CREATE TABLE " + TABLE_WORKOUT_EXERCISE + " ("
                    + COLUMN_WORKOUT_EXERCISE_WORKOUT_ID + " INTEGER NOT NULL, "
                    + COLUMN_WORKOUT_EXERCISE_EXERCISE_ID + " INTEGER NOT NULL, "
                    + "PRIMARY KEY(" + COLUMN_WORKOUT_EXERCISE_WORKOUT_ID + ", " + COLUMN_WORKOUT_EXERCISE_EXERCISE_ID + "), "
                    + "FOREIGN KEY(" + COLUMN_WORKOUT_EXERCISE_WORKOUT_ID + ") REFERENCES " + TABLE_PREDEFINED_WORKOUT + "(" + COLUMN_PREDEFINED_WORKOUT_ID + ") ON DELETE CASCADE, "
                    + "FOREIGN KEY(" + COLUMN_WORKOUT_EXERCISE_EXERCISE_ID + ") REFERENCES " + TABLE_EXERCISE + "(" + COLUMN_EXERCISE_ID + ") ON DELETE CASCADE"
                    + ");";
            database.execSQL(sqlWorkoutExercises);
        }
    }
}
