package com.example.cookup_app.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cookup_app.R;
import com.example.cookup_app.activity.ProfileActivity;
import com.example.cookup_app.adapter.FeaturedRecipeAdapter;
import com.example.cookup_app.adapter.NewestRecipeAdapter;
import com.example.cookup_app.model.Recipe;
import com.example.cookup_app.model.Ingredient;
import com.example.cookup_app.model.RecipeStep;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String PREFS_NAME = "cookup_prefs";
    private static final String KEY_GENDER = "gender";

    private ImageView imgAvatar;
    private ImageView imgHero;
    private TextView tvHeroName;
    private TextView tvHeroCookTime;
    private TextView tvHeroRating;
    private TextView tvHeroCalories;
    private RecyclerView rvFeatured;
    private RecyclerView rvNewest;

    private android.view.View scrollHomeContent;
    private android.view.View layoutEmptyHomeState;
    private android.view.View btnEmptyStateUpload;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        imgAvatar = view.findViewById(R.id.imgAvatar);
        imgHero = view.findViewById(R.id.imgHero);
        tvHeroName = view.findViewById(R.id.tvHeroName);
        tvHeroCookTime = view.findViewById(R.id.tvHeroCookTime);
        tvHeroRating = view.findViewById(R.id.tvHeroRating);
        tvHeroCalories = view.findViewById(R.id.tvHeroCalories);
        rvFeatured = view.findViewById(R.id.rvFeatured);
        rvNewest = view.findViewById(R.id.rvNewest);

        rvFeatured.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvNewest.setLayoutManager(new LinearLayoutManager(requireContext()));

        scrollHomeContent = view.findViewById(R.id.scrollHomeContent);
        layoutEmptyHomeState = view.findViewById(R.id.layoutEmptyHomeState);
        btnEmptyStateUpload = view.findViewById(R.id.btnEmptyStateUpload);

        if (btnEmptyStateUpload != null) {
            btnEmptyStateUpload.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), com.example.cookup_app.activity.AddRecipeActivity.class);
                startActivity(intent);
            });
        }

        applyAvatar(imgAvatar);

        imgAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), ProfileActivity.class);
            startActivity(intent);
        });

        View cardNotification = view.findViewById(R.id.cardNotification);
        if (cardNotification != null) {
            cardNotification.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), com.example.cookup_app.activity.NotificationActivity.class);
                startActivity(intent);
            });
        }

        View btnViewAll = view.findViewById(R.id.btnViewAll);
        if (btnViewAll != null) {
            btnViewAll.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), com.example.cookup_app.activity.AllRecipesActivity.class);
                startActivity(intent);
            });
        }

        View btnViewAllNewest = view.findViewById(R.id.btnViewAllNewest);
        if (btnViewAllNewest != null) {
            btnViewAllNewest.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), com.example.cookup_app.activity.AllRecipesActivity.class);
                startActivity(intent);
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecipes();
    }

    private void loadRecipes() {
        // Query real recipes from Firebase Firestore
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("recipes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        List<Recipe> items = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Recipe recipe = doc.toObject(Recipe.class);
                            if (recipe != null) {
                                recipe = com.example.cookup_app.utils.RecipeDataHelper.sanitizeAndHealRecipeId(doc, recipe);
                                boolean isFake = false;
                                try {
                                    int docIdVal = Integer.parseInt(doc.getId());
                                    if (docIdVal < 10000) {
                                        isFake = true; // Legacy integer doc ID is mock data
                                    }
                                } catch (NumberFormatException e) {
                                    // Not integer doc ID
                                }

                                if (!isFake) {
                                    items.add(recipe);
                                }
                            }
                        }
                        if (!items.isEmpty()) {
                            if (scrollHomeContent != null) scrollHomeContent.setVisibility(android.view.View.VISIBLE);
                            if (layoutEmptyHomeState != null) layoutEmptyHomeState.setVisibility(android.view.View.GONE);
                            bindRecipes(items);
                        } else {
                            // Automatically seed recipes if database is empty for this user account
                            com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                            if (currentUser != null) {
                                String uid = currentUser.getUid();
                                String email = currentUser.getEmail();
                                String chefName = (email != null && email.contains("@")) ? email.split("@")[0] : "Đầu bếp";
                                autoSeedRecipes(uid, chefName);
                            } else {
                                showEmptyState();
                            }
                        }
                    } else {
                        // Automatically seed recipes if database is empty for this user account
                        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            String uid = currentUser.getUid();
                            String email = currentUser.getEmail();
                            String chefName = (email != null && email.contains("@")) ? email.split("@")[0] : "Đầu bếp";
                            autoSeedRecipes(uid, chefName);
                        } else {
                            showEmptyState();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showEmptyState();
                });
    }

    private void autoSeedRecipes(String uid, String chefName) {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        // 1. Phở Bò Gia Truyền
        Recipe r1 = new Recipe(1001, "Phở Bò Gia Truyền", 120, 0.0, 450, R.drawable.character_chef_1);
        r1.setCreatorUid(uid);
        r1.setChefName(chefName);
        r1.setDifficulty("Khó");
        r1.setCountry("Việt Nam");
        r1.setDescription("Món phở bò truyền thống Việt Nam chuẩn vị thơm ngon, đậm đà nước hầm từ xương ống bò hòa quyện thảo mộc.");
        r1.setServings(4);
        List<Ingredient> ings1 = new ArrayList<>();
        ings1.add(new Ingredient("500g", "Thịt bò thăn"));
        ings1.add(new Ingredient("1kg", "Xương ống bò"));
        ings1.add(new Ingredient("1 củ", "Hành tây"));
        ings1.add(new Ingredient("1 củ", "Hành tím"));
        ings1.add(new Ingredient("1 nhánh", "Gừng tươi"));
        ings1.add(new Ingredient("Vừa đủ", "Quế, hồi, thảo quả"));
        ings1.add(new Ingredient("Vừa đủ", "Bánh phở, hành lá"));
        r1.setIngredients(ings1);
        List<RecipeStep> steps1 = new ArrayList<>();
        steps1.add(new RecipeStep(1, "Nướng chín hành tây, gừng, hành tím và rửa sạch xương ống bò.", "", 180));
        steps1.add(new RecipeStep(2, "Hầm xương ống bò với gừng hành nướng trong 2-3 tiếng.", "", 300));
        steps1.add(new RecipeStep(3, "Thêm quế, hồi, thảo quả vào nồi nước dùng và nêm nếm vừa vị.", "", 120));
        steps1.add(new RecipeStep(4, "Trần bánh phở, xếp thịt bò thái mỏng lên trên, rắc hành lá rồi chan nước dùng.", "", 60));
        r1.setSteps(steps1);

        // 2. Bánh Mì Thịt Nướng
        Recipe r2 = new Recipe(1002, "Bánh Mì Thịt Nướng", 30, 0.0, 320, R.drawable.character_chef_2);
        r2.setCreatorUid(uid);
        r2.setChefName(chefName);
        r2.setDifficulty("Trung bình");
        r2.setCountry("Việt Nam");
        r2.setDescription("Món bánh mì kẹp thịt heo nướng sả băm, phủ dưa chua dòn ngọt nêm nếm cay nồng hấp dẫn.");
        r2.setServings(2);
        List<Ingredient> ings2 = new ArrayList<>();
        ings2.add(new Ingredient("400g", "Thịt nạc vai heo"));
        ings2.add(new Ingredient("2 muỗng", "Sả băm"));
        ings2.add(new Ingredient("1 muỗng", "Mật ong"));
        ings2.add(new Ingredient("2 ổ", "Bánh mì giòn"));
        ings2.add(new Ingredient("Vừa đủ", "Dưa leo, ngò rí, đồ chua"));
        r2.setIngredients(ings2);
        List<RecipeStep> steps2 = new ArrayList<>();
        steps2.add(new RecipeStep(1, "Thái mỏng thịt heo và ướp với sả băm, mật ong, nước mắm trong 20 phút.", "", 180));
        steps2.add(new RecipeStep(2, "Nướng chín thịt heo trên than hoa hoặc nồi chiên không dầu.", "", 240));
        steps2.add(new RecipeStep(3, "Xẻ đôi bánh mì, thêm dưa leo dưa chua đồ ngọt và xếp thịt nướng nóng hổi.", "", 120));
        steps2.add(new RecipeStep(4, "Rưới nước sốt, ngò rí và thưởng thức bánh mì giòn rụm.", "", 60));
        r2.setSteps(steps2);

        // 3. Bún Bò Huế
        Recipe r3 = new Recipe(1003, "Bún bò Huế", 90, 0.0, 550, R.drawable.character_chef_1);
        r3.setCreatorUid(uid);
        r3.setChefName(chefName);
        r3.setDifficulty("Khó");
        r3.setCountry("Việt Nam");
        r3.setDescription("Đặc sản xứ Huế với nước dùng ngọt lịm đậm vị mắm ruốc thơm nồng của sả băm kết hợp giò heo, bắp bò.");
        r3.setServings(4);
        List<Ingredient> ings3 = new ArrayList<>();
        ings3.add(new Ingredient("500g", "Bắp bò"));
        ings3.add(new Ingredient("1kg", "Giò heo"));
        ings3.add(new Ingredient("300g", "Chả cua"));
        ings3.add(new Ingredient("3 cây", "Sả"));
        ings3.add(new Ingredient("1 củ", "Hành tây"));
        ings3.add(new Ingredient("2 trái", "Chanh"));
        ings3.add(new Ingredient("2 muỗng", "Mắm ruốc Huế"));
        ings3.add(new Ingredient("Vừa đủ", "Rau thơm, giá đỗ"));
        ings3.add(new Ingredient("Vừa đủ", "Bún tươi, rau muống chẻ"));
        r3.setIngredients(ings3);
        List<RecipeStep> steps3 = new ArrayList<>();
        steps3.add(new RecipeStep(1, "Luộc sơ xương ống, giò heo, bắp bò rồi xả sạch bọt dơ.", "", 180));
        steps3.add(new RecipeStep(2, "Hầm xương và giò heo cùng sả cây đập dập.", "", 300));
        steps3.add(new RecipeStep(3, "Hòa tan mắm ruốc lọc nước trong cho vào nồi nước dùng hầm sôi.", "", 120));
        steps3.add(new RecipeStep(4, "Xếp bún ra tô, thêm giò heo, bắp bò thái mỏng, chả cua chín rồi chan nước bún nóng hổi.", "", 60));
        r3.setSteps(steps3);

        // 4. Gỏi Cuốn Tôm Thịt
        Recipe r4 = new Recipe(1004, "Gỏi Cuốn Tôm Thịt", 15, 0.0, 250, R.drawable.character_chef_2);
        r4.setCreatorUid(uid);
        r4.setChefName(chefName);
        r4.setDifficulty("Dễ");
        r4.setCountry("Việt Nam");
        r4.setDescription("Món gỏi cuốn thanh nhẹ, tôm thịt tươi ngon cuốn bánh tráng rau sống, chấm xốt tương bơ phộng bùi béo dòn ngọt.");
        r4.setServings(2);
        List<Ingredient> ings4 = new ArrayList<>();
        ings4.add(new Ingredient("300g", "Tôm tươi"));
        ings4.add(new Ingredient("300g", "Thịt ba chỉ heo"));
        ings4.add(new Ingredient("1 xấp", "Bánh tráng"));
        ings4.add(new Ingredient("200g", "Bún tươi"));
        ings4.add(new Ingredient("Vừa đủ", "Rau xà lách, hẹ, ngò rí"));
        r4.setIngredients(ings4);
        List<RecipeStep> steps4 = new ArrayList<>();
        steps4.add(new RecipeStep(1, "Luộc chín thịt ba chỉ heo và tôm tươi rồi bóc vỏ tôm xẻ đôi, thái mỏng thịt.", "", 180));
        steps4.add(new RecipeStep(2, "Thấm ướt nhẹ bánh tráng mỏng dẻo dai.", "", 120));
        steps4.add(new RecipeStep(3, "Xếp xà lách, hẹ, ngò rí, bún tươi rồi đặt tôm thịt lên cuốn chặt tay.", "", 180));
        steps4.add(new RecipeStep(4, "Pha xốt tương đen ngậy bùi với bơ đậu phộng rồi dùng gỏi cuốn chấm ăn.", "", 60));
        r4.setSteps(steps4);

        // Save to Firestore
        db.collection("recipes").document().set(r1);
        db.collection("recipes").document().set(r2);
        db.collection("recipes").document().set(r3);
        db.collection("recipes").document().set(r4)
            .addOnSuccessListener(aVoid -> {
                if (isAdded()) {
                    loadRecipes(); // Refresh home UI immediately
                }
            });
    }

    private void showEmptyState() {
        if (!isAdded()) return;
        if (scrollHomeContent != null) scrollHomeContent.setVisibility(android.view.View.GONE);
        if (layoutEmptyHomeState != null) layoutEmptyHomeState.setVisibility(android.view.View.VISIBLE);
    }

    private void bindRecipes(List<Recipe> list) {
        if (list == null || list.isEmpty() || !isAdded()) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        java.util.HashSet<String> followedChefs = new java.util.HashSet<>(prefs.getStringSet("followed_chefs", new java.util.HashSet<>()));

        // Use smart recommendation algorithm to sort featured list
        List<Recipe> recommendedList = com.example.cookup_app.utils.RecipeRecommendationAlgorithm.recommendFeatured(list, followedChefs);

        // 1. Bind Hero Card (Find top recommended or top rating recipe)
        Recipe heroRecipe = recommendedList.get(0);

        tvHeroName.setText(heroRecipe.getName());
        tvHeroCookTime.setText(heroRecipe.getCookTimeMinutes() >= 60 
            ? (heroRecipe.getCookTimeMinutes() / 60) + " Giờ" 
            : heroRecipe.getCookTimeMinutes() + " Phút");
        tvHeroRating.setText(heroRecipe.getRating() + " (Thịnh hành)");
        tvHeroCalories.setText(heroRecipe.getCalories() + " kcal");

        if (heroRecipe.getImageUrl() != null && !heroRecipe.getImageUrl().trim().isEmpty() && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(requireActivity(), heroRecipe.getImageUrl())) {
            Glide.with(this)
                    .load(heroRecipe.getImageUrl())
                    .placeholder(R.drawable.character_chef_1)
                    .error(R.drawable.character_chef_1)
                    .into(imgHero);
        } else if (com.example.cookup_app.utils.RecipeDataHelper.isValidDrawable(requireActivity(), heroRecipe.getImageResId())) {
            imgHero.setImageResource(heroRecipe.getImageResId());
        } else {
            imgHero.setImageResource(R.drawable.character_chef_1);
        }

        final Recipe finalHeroRecipe = heroRecipe;
        imgHero.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), com.example.cookup_app.activity.RecipeDetailActivity.class);
            intent.putExtra("recipe", finalHeroRecipe);
            startActivity(intent);
        });

        // 2. Bind Featured Recipes (Next items in recommended list)
        List<Recipe> featuredList = new ArrayList<>();
        for (int i = 1; i < recommendedList.size(); i++) {
            featuredList.add(recommendedList.get(i));
        }
        if (featuredList.isEmpty()) {
            featuredList.addAll(recommendedList);
        }
        rvFeatured.setAdapter(new FeaturedRecipeAdapter(featuredList));

        // 3. Bind Newest Recipes (Original list elements representing new arrivals)
        List<Recipe> newestList = new ArrayList<>(list);
        rvNewest.setAdapter(new NewestRecipeAdapter(newestList));
    }

    private void applyAvatar(ImageView imgAvatar) {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : "";
        
        String keyCustomAvatar = "custom_avatar_url" + (!uid.isEmpty() ? "_" + uid : "");
        String keyGender = KEY_GENDER + (!uid.isEmpty() ? "_" + uid : "");

        String customAvatarUrl = prefs.getString(keyCustomAvatar, "");
        if (!android.text.TextUtils.isEmpty(customAvatarUrl) && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(requireActivity(), customAvatarUrl)) {
            Glide.with(this)
                    .load(customAvatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.avatar_default_unknown)
                    .error(R.drawable.avatar_default_unknown)
                    .into(imgAvatar);
        } else {
            String gender = prefs.getString(keyGender, "unknown");
            if ("male".equalsIgnoreCase(gender)) {
                imgAvatar.setImageResource(R.drawable.avatar_default_male);
            } else if ("female".equalsIgnoreCase(gender)) {
                imgAvatar.setImageResource(R.drawable.avatar_default_female);
            } else {
                imgAvatar.setImageResource(R.drawable.avatar_default_unknown);
            }
        }
    }


}
