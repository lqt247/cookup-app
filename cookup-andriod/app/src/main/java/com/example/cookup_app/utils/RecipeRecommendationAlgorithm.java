package com.example.cookup_app.utils;

import com.example.cookup_app.model.Recipe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class RecipeRecommendationAlgorithm {

    /**
     * Thuật toán gợi ý món ăn nổi bật cho người dùng dựa trên điểm đánh giá (rating)
     * và mức độ yêu thích đối với đầu bếp (followedChefs).
     * Điểm số càng cao, món ăn càng được ưu tiên hiển thị ở mục nổi bật.
     */
    public static List<Recipe> recommendFeatured(List<Recipe> allRecipes, Set<String> followedChefs) {
        if (allRecipes == null) return new ArrayList<>();
        List<Recipe> recommended = new ArrayList<>(allRecipes);

        // Hàm tính điểm (Scoring):
        // Điểm = (Đánh giá * 10) + (Có theo dõi đầu bếp đó không ? 15 : 0) + (Đánh giá cực cao >= 4.8 ? 5 : 0)
        Collections.sort(recommended, (r1, r2) -> {
            double score1 = r1.getRating() * 10.0;
            if (followedChefs != null && r1.getChefName() != null && followedChefs.contains(r1.getChefName())) {
                score1 += 15.0;
            }
            if (r1.getRating() >= 4.8) {
                score1 += 5.0;
            }

            double score2 = r2.getRating() * 10.0;
            if (followedChefs != null && r2.getChefName() != null && followedChefs.contains(r2.getChefName())) {
                score2 += 15.0;
            }
            if (r2.getRating() >= 4.8) {
                score2 += 5.0;
            }

            return Double.compare(score2, score1); // Sắp xếp giảm dần theo điểm số gợi ý
        });

        return recommended;
    }

    /**
     * Thuật toán tìm kiếm và xếp hạng độ liên quan của công thức nấu ăn.
     * Ưu tiên cao nhất là khớp tiêu đề món ăn, sau đó đến đầu bếp, quốc gia và giới thiệu món ăn.
     */
    public static List<Recipe> searchAndRank(List<Recipe> allRecipes, String query) {
        if (allRecipes == null) return new ArrayList<>();
        if (query == null || query.trim().isEmpty()) return allRecipes;

        String lowerQuery = query.toLowerCase().trim();
        List<RecipeScoreWrapper> matched = new ArrayList<>();

        for (Recipe r : allRecipes) {
            double relevanceScore = 0;

            if (r.getName() != null && r.getName().toLowerCase().contains(lowerQuery)) {
                relevanceScore += 100; // Khớp tên món ăn (Mức ưu tiên cao nhất)
            }
            if (r.getChefName() != null && r.getChefName().toLowerCase().contains(lowerQuery)) {
                relevanceScore += 50;  // Khớp tên đầu bếp
            }
            if (r.getCountry() != null && r.getCountry().toLowerCase().contains(lowerQuery)) {
                relevanceScore += 40;  // Khớp quốc gia của món ăn
            }
            if (r.getDescription() != null && r.getDescription().toLowerCase().contains(lowerQuery)) {
                relevanceScore += 20;  // Khớp từ khóa trong phần mô tả
            }

            if (relevanceScore > 0) {
                matched.add(new RecipeScoreWrapper(r, relevanceScore));
            }
        }

        // Sắp xếp danh sách kết quả tìm kiếm theo điểm liên quan giảm dần, nếu bằng điểm thì ưu tiên rating cao hơn
        Collections.sort(matched, (w1, w2) -> {
            if (Double.compare(w2.score, w1.score) != 0) {
                return Double.compare(w2.score, w1.score);
            }
            return Double.compare(w2.recipe.getRating(), w1.recipe.getRating());
        });

        List<Recipe> result = new ArrayList<>();
        for (RecipeScoreWrapper w : matched) {
            result.add(w.recipe);
        }
        return result;
    }

    private static class RecipeScoreWrapper {
        Recipe recipe;
        double score;

        RecipeScoreWrapper(Recipe recipe, double score) {
            this.recipe = recipe;
            this.score = score;
        }
    }
}
