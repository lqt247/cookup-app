package com.example.cookup_app.model;

public class Recipe {
    private final int id;
    private final String name;
    private final int cookTimeMinutes;
    private final double rating;
    private final int calories;
    private final int imageResId;

    public Recipe(int id, String name, int cookTimeMinutes, double rating, int calories, int imageResId) {
        this.id = id;
        this.name = name;
        this.cookTimeMinutes = cookTimeMinutes;
        this.rating = rating;
        this.calories = calories;
        this.imageResId = imageResId;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCookTimeMinutes() {
        return cookTimeMinutes;
    }

    public double getRating() {
        return rating;
    }

    public int getCalories() {
        return calories;
    }

    public int getImageResId() {
        return imageResId;
    }
}
