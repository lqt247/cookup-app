package com.example.cookup_app.model;

import java.io.Serializable;

public class Notification implements Serializable {
    private String id;
    private String title;
    private String body;
    private String type; // Các loại thông báo: "like" (lượt thích), "follow" (theo dõi), "system" (hệ thống)
    private long timestamp;
    private boolean read;
    private String userAvatar;
    private String userGender;
    private String recipeImage;
    private String senderUid;
    private String recipeId;

    // Hàm khởi tạo mặc định bắt buộc cho Firebase
    public Notification() {
    }

    public Notification(String id, String title, String body, String type, long timestamp, boolean read) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.type = type;
        this.timestamp = timestamp;
        this.read = read;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public String getUserGender() {
        return userGender;
    }

    public void setUserGender(String userGender) {
        this.userGender = userGender;
    }

    public String getRecipeImage() {
        return recipeImage;
    }

    public void setRecipeImage(String recipeImage) {
        this.recipeImage = recipeImage;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }
}
