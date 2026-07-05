package com.example.cookup_app.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RecipeCollection implements Serializable {
    private String id;
    private String name;
    private boolean isPublic;
    private String creatorUid;
    private List<String> recipeIds = new ArrayList<>();
    private long timestamp;

    public RecipeCollection() {
    }

    public RecipeCollection(String id, String name, boolean isPublic, String creatorUid) {
        this.id = id;
        this.name = name;
        this.isPublic = isPublic;
        this.creatorUid = creatorUid;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getCreatorUid() {
        return creatorUid;
    }

    public void setCreatorUid(String creatorUid) {
        this.creatorUid = creatorUid;
    }

    public List<String> getRecipeIds() {
        return recipeIds;
    }

    public void setRecipeIds(List<String> recipeIds) {
        this.recipeIds = recipeIds;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
