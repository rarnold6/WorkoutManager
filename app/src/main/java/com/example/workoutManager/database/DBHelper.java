package com.example.workoutManager.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.workoutManager.data.Exercise;

import java.util.LinkedList;

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

    public static final String COLUMN_EXERCISE_DURATION = "exercise_duration";

    public static final String COLUMN_RECOVERY_TIME = "recovery_time";

    public static final String COLUMN_BREAK_TIME = "break_time";

    public static final String COLUMN_NUMBER_OF_SETS = "number_of_sets";

    public static final String COLUMN_NUMBER_OF_EXERCISES = "number_of_exercises";


    public static final String TABLE_EXERCISE = "exercises";
    public static final String COLUMN_EXERCISE_ID = "id";
    public static final String COLUMN_EXERCISE_TITLE = "title";
    public static final String COLUMN_EXERCISE_DESCRIPTION = "description";
    public static final String COLUMN_EXERCISE_DIFFICULTY = "difficulty";
    public static final String COLUMN_EXERCISE_CATEGORY = "category";

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

        /*String sqlPredefinedWorkouts = "CREATE TABLE " + TABLE_PREDEFINED_WORKOUT + " ("
                + COLUMN_PREDEFINED_WORKOUT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_WORKOUT_TITLE + " TEXT NOT NULL,"
                + COLUMN_EXERCISE_DURATION + " INTEGER NOT NULL,"
                + COLUMN_RECOVERY_TIME + " INTEGER NOT NULL,"
                + COLUMN_BREAK_TIME + " INTEGER NOT NULL,"
                + COLUMN_NUMBER_OF_SETS + " INTEGER NOT NULL,"
                + COLUMN_NUMBER_OF_EXERCISES + " INTEGER NOT NULL"
                + ");";
        database.execSQL(sqlPredefinedWorkouts);

        String sqlExercises = "CREATE TABLE " + TABLE_EXERCISE + " ("
                + COLUMN_EXERCISE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_EXERCISE_TITLE + " TEXT NOT NULL, "
                + COLUMN_EXERCISE_DESCRIPTION + " TEXT,"
                + COLUMN_EXERCISE_DIFFICULTY + " INTEGER,"
                + COLUMN_EXERCISE_CATEGORY + " TEXT"
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

        insertDefaultDataIntoExercises(database);
        insertDefaultDataIntoPredefinedWorkouts(database);
*/


        Log.i("DATABASE CREATED", "TABLE WORKOUT");
    }

    private void insertDefaultDataIntoExercises(SQLiteDatabase database) {
        LinkedList<Exercise> exercises = new LinkedList<>();
        exercises.add(new Exercise("Planks","Hold your body straight and parallel to the floor while resting on your toes and forearm.", 2,"isometric core exercise"));
        exercises.add(new Exercise("Side Planks (right)","Lie on your side with your knees bent. Prop your upper body up on your elbow, with your elbow under your shoulder. Raise your hips off the floor.",2,"isometric core exercise"));
        exercises.add(new Exercise("Side Planks (left)","Lie on your side with your knees bent. Prop your upper body up on your elbow, with your elbow under your shoulder. Raise your hips off the floor.",2,"isometric core exercise"));
        //exercises.add(new Exercise("Sit ups","Performed from a supine position by raising the torso to a sitting position and returning to the original position without using the arms or lifting the feet.",1,));
        //exercises.add(new Exercise("Imaginary chair","Position yourself against a wall as if seated.",2));
        //exercises.add(new Exercise("Tricep dips","Raise and lower your body on your hands with your arms bent behind you, while sitting in a position with your legs straight out in front of you.",3));
        //exercises.add(new Exercise("Pelvice lift","",1));
        //exercises.add(new Exercise("Push ups","Push-ups are exercises to strengthen your arms and chest muscles. They are done by lying with your face towards the floor and pushing with your hands to raise your body until your arms are straight.",2));


    }

    private void insertDefaultDataIntoPredefinedWorkouts(SQLiteDatabase database) {

    }



    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if(oldVersion != newVersion && oldVersion < 3){
            String sqlPredefinedWorkouts = "CREATE TABLE " + TABLE_PREDEFINED_WORKOUT + " ("
                    + COLUMN_PREDEFINED_WORKOUT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_WORKOUT_TITLE + " TEXT NOT NULL,"
                    + COLUMN_EXERCISE_DURATION + " INTEGER NOT NULL,"
                    + COLUMN_RECOVERY_TIME + " INTEGER NOT NULL,"
                    + COLUMN_BREAK_TIME + " INTEGER NOT NULL,"
                    + COLUMN_NUMBER_OF_SETS + " INTEGER NOT NULL,"
                    + COLUMN_NUMBER_OF_EXERCISES + " INTEGER NOT NULL"
                    + ");";
            database.execSQL(sqlPredefinedWorkouts);

            String sqlExercises = "CREATE TABLE " + TABLE_EXERCISE + " ("
                    + COLUMN_EXERCISE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_EXERCISE_TITLE + " TEXT NOT NULL, "
                    + COLUMN_EXERCISE_DESCRIPTION + " TEXT,"
                    + COLUMN_EXERCISE_DIFFICULTY + " INTEGER,"
                    + COLUMN_EXERCISE_CATEGORY + " TEXT"
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

            insertDefaultDataIntoExercises(database);
            insertDefaultDataIntoPredefinedWorkouts(database);
        }
    }
}
