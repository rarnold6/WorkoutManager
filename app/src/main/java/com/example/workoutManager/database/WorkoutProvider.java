package com.example.workoutManager.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public class WorkoutProvider extends ContentProvider {


    private DBHelper dbHelper;

    private static final int URI_WORKOUT_DATES = 100;
    private static final int URI_WORKOUT_DATE_TIME = 101;
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(WorkoutContract.CONTENT_AUTHORITY, WorkoutContract.PATH_WORKOUT_SCHEDULE, URI_WORKOUT_DATES);
        uriMatcher.addURI(WorkoutContract.CONTENT_AUTHORITY, "workout_schedule/*/*", URI_WORKOUT_DATE_TIME);
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
        }

        if(cursor != null) {
            cursor.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), uri);
        }
        return cursor;
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
