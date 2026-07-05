package com.example.cookup_app.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Recipe implements Serializable {
    private int id;
    private String name;
    private int cookTimeMinutes;
    private double rating;
    private int calories;
    private int imageResId;
    private String imageUrl;

    // Các trường dữ liệu chi tiết của công thức món ăn
    private String description;
    private int servings;
    private String difficulty;
    private String country;
    private List<String> tags = new ArrayList<>();
    private List<Ingredient> ingredients = new ArrayList<>();
    private List<RecipeStep> steps = new ArrayList<>();
    private String chefName;
    private String creatorUid;
    private long createdAt = System.currentTimeMillis();

    // Hàm khởi tạo mặc định bắt buộc cho Firebase Firestore
    public Recipe() {
    }

    public Recipe(int id, String name, int cookTimeMinutes, double rating, int calories, int imageResId) {
        this.id = id;
        this.name = name;
        this.cookTimeMinutes = cookTimeMinutes;
        this.rating = rating;
        this.calories = calories;
        this.imageResId = imageResId;
        this.difficulty = "Dễ";
        this.country = "Việt Nam";
        this.createdAt = System.currentTimeMillis();
    }

    public Recipe(int id, String name, int cookTimeMinutes, double rating, int calories, String imageUrl) {
        this.id = id;
        this.name = name;
        this.cookTimeMinutes = cookTimeMinutes;
        this.rating = rating;
        this.calories = calories;
        this.imageUrl = imageUrl;
        this.difficulty = "Dễ";
        this.country = "Việt Nam";
        this.createdAt = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCookTimeMinutes() {
        return cookTimeMinutes;
    }

    public void setCookTimeMinutes(int cookTimeMinutes) {
        this.cookTimeMinutes = cookTimeMinutes;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // Các hàm Getter và Setter bổ sung cho các trường dữ liệu chi tiết
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getServings() {
        return servings;
    }

    public void setServings(int servings) {
        this.servings = servings;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    public List<RecipeStep> getSteps() {
        return steps;
    }

    public void setSteps(List<RecipeStep> steps) {
        this.steps = steps;
    }

    public String getChefName() {
        return chefName;
    }

    public void setChefName(String chefName) {
        this.chefName = chefName;
    }

    public String getCreatorUid() {
        return creatorUid;
    }

    public void setCreatorUid(String creatorUid) {
        this.creatorUid = creatorUid;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
