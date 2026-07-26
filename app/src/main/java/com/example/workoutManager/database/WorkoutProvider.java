package com.example.workoutManager.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public class WorkoutProvider extends ContentProvider {


    private DBHelper dbHelper;

    private static final int URI_WORKOUT_DATES = 100;
    private static final int URI_WORKOUT_DATE_TIME = 101;

    private static final int URI_PREDEFINED_WORKOUT = 102;

    private static final int URI_PREDEFINED_WORKOUT_ITEM = 103;

    private static final int URI_EXERCISES = 104;

    private static final int URI_EXERCISES_ITEM = 105;

    private static final int URI_WORKOUT_EXERCISES = 106;

    private static final int URI_WORKOUT_EXERCISES_ITEM = 107;


    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(WorkoutContract.CONTENT_AUTHORITY, WorkoutContract.PATH_WORKOUT_SCHEDULE, URI_WORKOUT_DATES);
        uriMatcher.addURI(WorkoutContract.CONTENT_AUTHORITY, "workout_schedule/*/*", URI_WORKOUT_DATE_TIME);
        uriMatcher.addURI(WorkoutContract.CONTENT_AUTHORITY, WorkoutContract.PATH_PREDEFINED_WORKOUT, URI_PREDEFINED_WORKOUT);
        uriMatcher.addURI(WorkoutContract.CONTENT_AUTHORITY, "predefined_workout/*/*", URI_PREDEFINED_WORKOUT_ITEM);
        uriMatcher.addURI(WorkoutContract.CONTENT_AUTHORITY, WorkoutContract.PATH_EXERCISES, URI_EXERCISES);
        uriMatcher.addURI(WorkoutContract.CONTENT_AUTHORITY, "exercises/*/*", URI_EXERCISES_ITEM);
        uriMatcher.addURI(WorkoutContract.CONTENT_AUTHORITY, WorkoutContract.PATH_EXERCISES, URI_WORKOUT_EXERCISES);
        uriMatcher.addURI(WorkoutContract.CONTENT_AUTHORITY, "exercises/*/*", URI_WORKOUT_EXERCISES_ITEM);
    }


        @Override
    public boolean onCreate() {
        this.dbHelper = new DBHelper(this.getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = this.dbHelper.getReadableDatabase();

        Cursor cursor = null;

        int match = uriMatcher.match(uri);
        if (match == URI_WORKOUT_DATES) {
            cursor = database.query(WorkoutContract.WorkoutEntry.TABLE_WORKOUT,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder);
        } else if(match == URI_PREDEFINED_WORKOUT){
            SQLiteQueryBuilder queryBuilder = getSQLiteQueryBuilderForPredefinedWorkouts();

            // Execute the query
            cursor = queryBuilder.query(
                    database,                 // Database instance
                    projection,         // Columns to return
                    selection,          // WHERE clause
                    selectionArgs,      // WHERE arguments
                    null,               // GROUP BY
                    null,               // HAVING
                    sortOrder           // ORDER BY
            );
        } else if(match == URI_EXERCISES){
            cursor = database.query(WorkoutContract.ExerciseEntry.TABLE_EXERCISE,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder);
        }

        if(cursor != null) {
            cursor.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), uri);
        }
        return cursor;
    }

    @NonNull
    private static SQLiteQueryBuilder getSQLiteQueryBuilderForPredefinedWorkouts() {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(WorkoutContract.PredefinedWorkoutEntry.TABLE_PREDEFINED_WORKOUT + " " +
                "INNER JOIN " + WorkoutContract.WorkoutExercisesEntry.TABLE_WORKOUT_EXERCISE + " ON " + WorkoutContract.PredefinedWorkoutEntry.TABLE_PREDEFINED_WORKOUT + "." + WorkoutContract.PredefinedWorkoutEntry.COLUMN_PREDEFINED_WORKOUT_ID + " = " + WorkoutContract.WorkoutExercisesEntry.TABLE_WORKOUT_EXERCISE + "." + WorkoutContract.WorkoutExercisesEntry.COLUMN_WORKOUT_EXERCISE_WORKOUT_ID + " " +
                "INNER JOIN " + WorkoutContract.ExerciseEntry.TABLE_EXERCISE + " ON " + WorkoutContract.WorkoutExercisesEntry.TABLE_WORKOUT_EXERCISE + "." + WorkoutContract.WorkoutExercisesEntry.COLUMN_WORKOUT_EXERCISE_EXERCISE_ID + " = " + WorkoutContract.ExerciseEntry.TABLE_EXERCISE + "." + WorkoutContract.ExerciseEntry.COLUMN_EXERCISE_ID);
        return queryBuilder;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = uriMatcher.match(uri);
        switch (match){
            case URI_WORKOUT_DATES:
                return WorkoutContract.WorkoutEntry.CONTENT_LIST_TYPE_WORKOUT;
            case URI_WORKOUT_DATE_TIME:
                // URI for a single workout entry identified by both weekday and time
                return WorkoutContract.WorkoutEntry.CONTENT_ITEM_TYPE_WORKOUT;
            default:
                throw new IllegalArgumentException("Unknown URI "+ uri + " with match " + match);

        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        final int match = uriMatcher.match(uri);
        long id = 0;
        if (match == URI_WORKOUT_DATES) {
            SQLiteDatabase database = this.dbHelper.getWritableDatabase();
            id = database.insert(WorkoutContract.WorkoutEntry.TABLE_WORKOUT,null,contentValues);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri,id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase database = this.dbHelper.getWritableDatabase();
        final int match = uriMatcher.match(uri);

        // Match the URI for deleting a workout entry based on weekday and time
        if (match == URI_WORKOUT_DATE_TIME) {
            // Extract weekday and time from the URI (if present)
            String weekday = uri.getPathSegments().get(1);  // Assumes format /workouts/{weekday}/{time}
            String time = uri.getPathSegments().get(2);     // Assumes format /workouts/{weekday}/{time}

            // Construct the selection string based on the extracted values
            selection = WorkoutContract.WorkoutEntry.WORKOUT_WEEKDAY + "=? AND " +
                    WorkoutContract.WorkoutEntry.WORKOUT_TIME + "=?";
            selectionArgs = new String[]{weekday, time};

            // Perform the deletion
            int rowsDeleted = database.delete(WorkoutContract.WorkoutEntry.TABLE_WORKOUT, selection, selectionArgs);

            // Notify content resolver if deletion was successful
            if (rowsDeleted > 0) {
                getContext().getContentResolver().notifyChange(uri, null);
            }

            return rowsDeleted;
        }

        return -1;
    }


    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }
}
