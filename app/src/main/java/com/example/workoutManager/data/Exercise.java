package com.example.workoutManager.data;

public class Exercise {

    private String name;
    private String description;

    private int difficulty;
    private String category;


    public Exercise(String name, String description, int difficulty, String category) {
        this.name = name;
        this.description = description;
        this.difficulty = difficulty;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
