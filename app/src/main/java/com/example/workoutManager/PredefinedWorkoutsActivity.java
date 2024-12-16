package com.example.workoutManager;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.workoutManager.data.Workout;

import java.util.LinkedHashMap;
import java.util.LinkedList;

public class PredefinedWorkoutsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinkedList<Workout> predefinedWorkouts = retrieveDatabaseInformation();
        inflateListView(predefinedWorkouts);
    }

    private LinkedList<Workout> retrieveDatabaseInformation() {
        return null;

    }

    private void inflateListView(LinkedList<Workout> predefinedWorkouts) {
    }
}
