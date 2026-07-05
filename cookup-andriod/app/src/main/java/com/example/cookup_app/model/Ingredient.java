package com.example.cookup_app.model;

import java.io.Serializable;

public class Ingredient implements Serializable {
    private String qty;
    private String name;

    // Hàm khởi tạo mặc định bắt buộc cho Firebase Firestore
    public Ingredient() {
    }

    public Ingredient(String qty, String name) {
        this.qty = qty;
        this.name = name;
    }

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
