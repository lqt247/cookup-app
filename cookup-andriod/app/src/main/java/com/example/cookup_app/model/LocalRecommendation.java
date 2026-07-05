package com.example.cookup_app.model;

import java.util.List;

public class LocalRecommendation {
    public String message;
    public List<Recipe> recipes;

    public LocalRecommendation(String message, List<Recipe> recipes) {
        this.message = message;
        this.recipes = recipes;
    }
}
