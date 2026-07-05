package com.example.cookup_app.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RecipeStep implements Serializable {
    private int stepNumber;
    private String instructions;
    private String imageUrl;
    private int durationSeconds;
    private List<Ingredient> stepIngredients = new ArrayList<>();

    // Hàm khởi tạo mặc định bắt buộc cho Firebase Firestore
    public RecipeStep() {
    }

    public RecipeStep(int stepNumber, String instructions, String imageUrl) {
        this.stepNumber = stepNumber;
        this.instructions = instructions;
        this.imageUrl = imageUrl;
        this.durationSeconds = 0;
    }

    public RecipeStep(int stepNumber, String instructions, String imageUrl, int durationSeconds) {
        this.stepNumber = stepNumber;
        this.instructions = instructions;
        this.imageUrl = imageUrl;
        this.durationSeconds = durationSeconds;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public List<Ingredient> getStepIngredients() {
        return stepIngredients;
    }

    public void setStepIngredients(List<Ingredient> stepIngredients) {
        this.stepIngredients = stepIngredients;
    }
}
