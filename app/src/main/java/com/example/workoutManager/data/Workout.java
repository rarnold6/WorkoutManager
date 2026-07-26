package com.example.workoutManager.data;

import java.io.Serializable;
import java.util.LinkedList;

public class Workout implements Serializable {
    public int getExerciseDuration() {
        return exerciseDuration;
    }

    public int getRecoveryTime() {
        return recoveryTime;
    }

    public int getBreakTime() {
        return breakTime;
    }

    public int getNumberOfSets() {
        return numberOfSets;
    }

    public int getNumberOfExercisesPerSet() {
        return numberOfExercisesPerSet;
    }

    public int getCurrentNumberOfExercise() {
        return currentNumberOfExercise;
    }

    public int getCurrentNumberOfSet() {
        return currentNumberOfSet;
    }

    public WorkoutPhase getCurrentPhase() {
        return currentPhase;
    }


    private String title;
    private int exerciseDuration; // in seconds
    private int recoveryTime; // in seconds
    private int breakTime; // in seconds

    private int numberOfSets;

    private int numberOfExercisesPerSet;

    private int currentNumberOfExercise;
    private int currentNumberOfSet;

    private long startTimestamp;

    private long endTimestamp;

    private LinkedList<Exercise> exercises;

    public void setStartTimestamp(long timestamp){
        this.startTimestamp = timestamp;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public enum WorkoutPhase {
        WARMUP, EXERCISE, RECOVERY, BREAK, FINISHED

    }

    private WorkoutPhase currentPhase;

    public Workout(int exerciseDuration, int recoveryTime, int breakTime, int numberOfSets, int numberOfExercisesPerSet, long startTimestamp) {
        this.exerciseDuration = exerciseDuration;
        this.recoveryTime = recoveryTime;
        this.breakTime = breakTime;
        this.numberOfSets = numberOfSets;
        this.numberOfExercisesPerSet = numberOfExercisesPerSet;
        this.currentNumberOfExercise = 0;
        this.currentNumberOfSet = 1;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = -1;
        // Set the default phase
        currentPhase = WorkoutPhase.WARMUP;
    }

    // to init a predefined workout
    public Workout(String title, int exerciseDuration, int recoveryTime, int breakTime, int numberOfSets, int numberOfExercisesPerSet, LinkedList<Exercise> exercises) {
        this.title = title;
        this.exerciseDuration = exerciseDuration;
        this.recoveryTime = recoveryTime;
        this.breakTime = breakTime;
        this.numberOfSets = numberOfSets;
        this.numberOfExercisesPerSet = numberOfExercisesPerSet;
        this.currentNumberOfExercise = 0;
        this.currentNumberOfSet = 1;
        this.startTimestamp = -1;
        this.endTimestamp = -1;
        this.exercises = exercises;
        // Set the default phase
        currentPhase = WorkoutPhase.WARMUP;
    }

    public void nextPhase(){
        switch (this.currentPhase){
            case WARMUP:
                currentPhase = WorkoutPhase.EXERCISE;
                this.currentNumberOfExercise++;
                break;
            case EXERCISE:
                if(this.currentNumberOfExercise == this.numberOfExercisesPerSet && this.currentNumberOfSet == this.numberOfSets){
                    currentPhase = WorkoutPhase.FINISHED;
                } else if (this.currentNumberOfExercise == this.numberOfExercisesPerSet) {
                    currentPhase = WorkoutPhase.BREAK;
                    this.currentNumberOfExercise = 1;
                    this.currentNumberOfSet++;
                } else {
                    this.currentNumberOfExercise++;
                    currentPhase = WorkoutPhase.RECOVERY;
                }

                break;
            case RECOVERY:
            case BREAK:
                this.currentPhase = WorkoutPhase.EXERCISE;
                break;
            case FINISHED:
                break;
        }
    }


}
