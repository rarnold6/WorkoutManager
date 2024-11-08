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
        Log.i("DATABASE CREATED", "TABLE WORKOUT");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORKOUT);
        onCreate(db);
    }
}
