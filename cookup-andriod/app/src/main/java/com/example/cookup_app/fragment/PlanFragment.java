package com.example.cookup_app.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.cookup_app.R;
import com.example.cookup_app.model.Ingredient;
import com.example.cookup_app.model.Recipe;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlanFragment extends Fragment {
    private static final String PREFS_NAME = "cookup_plan_prefs";
    private static final String KEY_CURRENT_RECIPE_NAME = "current_recipe_name";
    private static final String KEY_CHECKED_INGREDIENTS = "checked_ingredients";

    private TextView tvRecipeSource;
    private View layoutRecipeSource;
    private TextView tvEmptyMessage;

    private LinearLayout sectionVegetables;
    private LinearLayout sectionMeats;
    private LinearLayout sectionSpices;

    private LinearLayout containerVegetables;
    private LinearLayout containerMeats;
    private LinearLayout containerSpices;

    private View btnPlanAddRecipe;
    private View btnPlanMore;

    private List<Recipe> allAvailableRecipes = new ArrayList<>();
    private Set<String> checkedIngredientsSet = new HashSet<>();

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plan, container, false);

        // Bind Views
        tvRecipeSource = view.findViewById(R.id.tvRecipeSource);
        layoutRecipeSource = view.findViewById(R.id.layoutRecipeSource);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);

        sectionVegetables = view.findViewById(R.id.sectionVegetables);
        sectionMeats = view.findViewById(R.id.sectionMeats);
        sectionSpices = view.findViewById(R.id.sectionSpices);

        containerVegetables = view.findViewById(R.id.containerVegetables);
        containerMeats = view.findViewById(R.id.containerMeats);
        containerSpices = view.findViewById(R.id.containerSpices);

        btnPlanAddRecipe = view.findViewById(R.id.btnPlanAddRecipe);
        btnPlanMore = view.findViewById(R.id.btnPlanMore);

        // Load Persisted Data
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        checkedIngredientsSet = prefs.getStringSet(KEY_CHECKED_INGREDIENTS, new HashSet<>());

        // Load dynamic recipes database
        fetchRecipesList();

        // Register button actions
        btnPlanAddRecipe.setOnClickListener(v -> showRecipeSelectorDialog());
        btnPlanMore.setOnClickListener(v -> showMoreOptionsDialog());

        return view;
    }

    private void fetchRecipesList() {
        List<Recipe> allFetched = new ArrayList<>();
        allFetched.add(getBunBoHueRecipe());
        allFetched.add(getPhoBoRecipe());
        allFetched.add(getBanhMiRecipe());
        allFetched.add(getGoiCuonRecipe());

        // Load current recipe state
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedRecipeName = prefs.getString(KEY_CURRENT_RECIPE_NAME, "");

        // Fetch additional custom recipes from Firestore
        FirebaseFirestore.getInstance().collection("recipes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Recipe recipe = doc.toObject(Recipe.class);
                            if (recipe != null) {
                                recipe = com.example.cookup_app.utils.RecipeDataHelper.sanitizeAndHealRecipeId(doc, recipe);
                                // Prevent duplicates
                                boolean exists = false;
                                for (Recipe r : allFetched) {
                                    if (r.getName().equalsIgnoreCase(recipe.getName())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    allFetched.add(recipe);
                                }
                            }
                        }
                    }
                    filterRecipesBySavedAndCurrent(allFetched, savedRecipeName);
                })
                .addOnFailureListener(e -> {
                    filterRecipesBySavedAndCurrent(allFetched, savedRecipeName);
                });
    }

    private void filterRecipesBySavedAndCurrent(List<Recipe> allFetched, String savedRecipeName) {
        if (!isAdded()) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "";

        SharedPreferences mainPrefs = requireContext().getSharedPreferences("cookup_prefs", Context.MODE_PRIVATE);
        Set<String> bookmarkedIds = new HashSet<>();
        if (!uid.isEmpty()) {
            bookmarkedIds.addAll(mainPrefs.getStringSet("bookmarked_recipes_" + uid, new HashSet<>()));
        } else {
            bookmarkedIds.addAll(mainPrefs.getStringSet("bookmarked_recipes", new HashSet<>()));
        }

        if (!uid.isEmpty()) {
            FirebaseFirestore.getInstance().collection("collections")
                    .whereEqualTo("creatorUid", uid)
                    .get()
                    .addOnSuccessListener(colSnap -> {
                        if (colSnap != null) {
                            for (DocumentSnapshot docCol : colSnap.getDocuments()) {
                                com.example.cookup_app.model.RecipeCollection col = docCol.toObject(com.example.cookup_app.model.RecipeCollection.class);
                                if (col != null && col.getRecipeIds() != null) {
                                    bookmarkedIds.addAll(col.getRecipeIds());
                                }
                            }
                        }
                        applyFilter(allFetched, bookmarkedIds, savedRecipeName);
                    })
                    .addOnFailureListener(e -> {
                        applyFilter(allFetched, bookmarkedIds, savedRecipeName);
                    });
        } else {
            applyFilter(allFetched, bookmarkedIds, savedRecipeName);
        }
    }

    private void applyFilter(List<Recipe> allFetched, Set<String> bookmarkedIds, String savedRecipeName) {
        if (!isAdded()) return;

        allAvailableRecipes.clear();
        for (Recipe r : allFetched) {
            String rIdStr = String.valueOf(r.getId());
            boolean isBookmarked = bookmarkedIds.contains(rIdStr);
            boolean isCurrentActive = !TextUtils.isEmpty(savedRecipeName) && r.getName().equalsIgnoreCase(savedRecipeName);

            if (isBookmarked || isCurrentActive) {
                // Prevent duplicates
                boolean exists = false;
                for (Recipe existing : allAvailableRecipes) {
                    if (existing.getName().equalsIgnoreCase(r.getName())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    allAvailableRecipes.add(r);
                }
            }
        }

        displayRecipeIngredients(savedRecipeName);
    }

    private void displayRecipeIngredients(String recipeName) {
        if (!isAdded()) return;

        Recipe targetRecipe = null;
        if (!TextUtils.isEmpty(recipeName)) {
            for (Recipe r : allAvailableRecipes) {
                if (r.getName().equalsIgnoreCase(recipeName)) {
                    targetRecipe = r;
                    break;
                }
            }
        }

        if (targetRecipe == null) {
            tvEmptyMessage.setVisibility(View.VISIBLE);
            layoutRecipeSource.setVisibility(View.GONE);
            sectionVegetables.setVisibility(View.GONE);
            sectionMeats.setVisibility(View.GONE);
            sectionSpices.setVisibility(View.GONE);
            return;
        }

        layoutRecipeSource.setVisibility(View.VISIBLE);
        tvRecipeSource.setText("Từ: " + targetRecipe.getName());

        // Clear previous rows
        containerVegetables.removeAllViews();
        containerMeats.removeAllViews();
        containerSpices.removeAllViews();

        List<Ingredient> ingredients = targetRecipe.getIngredients();
        if (ingredients == null || ingredients.isEmpty()) {
            tvEmptyMessage.setVisibility(View.VISIBLE);
            sectionVegetables.setVisibility(View.GONE);
            sectionMeats.setVisibility(View.GONE);
            sectionSpices.setVisibility(View.GONE);
            return;
        }

        tvEmptyMessage.setVisibility(View.GONE);
        sectionVegetables.setVisibility(View.VISIBLE);
        sectionMeats.setVisibility(View.VISIBLE);
        sectionSpices.setVisibility(View.VISIBLE);

        boolean hasVeg = false;
        boolean hasMeat = false;
        boolean hasSpice = false;

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (Ingredient ing : ingredients) {
            View row = inflater.inflate(R.layout.item_shopping_ingredient, null);
            CheckBox cbBuy = row.findViewById(R.id.cbBuy);
            TextView tvName = row.findViewById(R.id.tvIngredientName);
            TextView tvQty = row.findViewById(R.id.tvIngredientQty);

            tvName.setText(ing.getName());
            tvQty.setText(ing.getQty());

            // Check if already bought
            boolean isChecked = checkedIngredientsSet.contains(ing.getName().toLowerCase());
            cbBuy.setChecked(isChecked);
            if (isChecked) {
                tvName.setPaintFlags(tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvName.setTextColor(getResources().getColor(R.color.text_secondary));
            } else {
                tvName.setPaintFlags(tvName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvName.setTextColor(getResources().getColor(R.color.text_primary));
            }

            // Register checkbox listener
            cbBuy.setOnCheckedChangeListener((buttonView, isCheckedNow) -> {
                SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                checkedIngredientsSet = new HashSet<>(prefs.getStringSet(KEY_CHECKED_INGREDIENTS, new HashSet<>()));

                if (isCheckedNow) {
                    checkedIngredientsSet.add(ing.getName().toLowerCase());
                    tvName.setPaintFlags(tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    tvName.setTextColor(getResources().getColor(R.color.text_secondary));
                } else {
                    checkedIngredientsSet.remove(ing.getName().toLowerCase());
                    tvName.setPaintFlags(tvName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    tvName.setTextColor(getResources().getColor(R.color.text_primary));
                }

                prefs.edit().putStringSet(KEY_CHECKED_INGREDIENTS, checkedIngredientsSet).apply();
            });

            // Categorize
            String nameLower = ing.getName().toLowerCase();
            if (isVegetable(nameLower)) {
                containerVegetables.addView(row);
                hasVeg = true;
            } else if (isMeat(nameLower)) {
                containerMeats.addView(row);
                hasMeat = true;
            } else {
                containerSpices.addView(row);
                hasSpice = true;
            }
        }

        // Adjust visibility of sections
        sectionVegetables.setVisibility(hasVeg ? View.VISIBLE : View.GONE);
        sectionMeats.setVisibility(hasMeat ? View.VISIBLE : View.GONE);
        sectionSpices.setVisibility(hasSpice ? View.VISIBLE : View.GONE);
    }

    private boolean isVegetable(String name) {
        return name.contains("hành") || name.contains("sả") || name.contains("chanh") ||
                name.contains("rau") || name.contains("ớt") || name.contains("tỏi") ||
                name.contains("giá") || name.contains("ngò") || name.contains("gừng") ||
                name.contains("chuối") || name.contains("muống") || name.contains("thảo") ||
                name.contains("nấm") || name.contains("cà chua") || name.contains("dưa") ||
                name.contains("khế") || name.contains("thơm") || name.contains("dứa");
    }

    private boolean isMeat(String name) {
        return name.contains("bò") || name.contains("heo") || name.contains("thịt") ||
                name.contains("giò") || name.contains("sườn") || name.contains("gà") ||
                name.contains("chả") || name.contains("tôm") || name.contains("cá") ||
                name.contains("mực") || name.contains("ốc") || name.contains("cua") ||
                name.contains("hải sản") || name.contains("xương");
    }

    private void showRecipeSelectorDialog() {
        if (allAvailableRecipes.isEmpty()) {
            Toast.makeText(getContext(), "Chưa có công thức đã lưu hoặc được thêm vào giỏ! Hãy lưu công thức yêu thích trước nhé ❤️", Toast.LENGTH_LONG).show();
            return;
        }

        String[] recipeNames = new String[allAvailableRecipes.size()];
        for (int i = 0; i < allAvailableRecipes.size(); i++) {
            recipeNames[i] = allAvailableRecipes.get(i).getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Chọn món để lên danh sách đi chợ");
        builder.setItems(recipeNames, (dialog, which) -> {
            String selectedName = recipeNames[which];
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                 .putString(KEY_CURRENT_RECIPE_NAME, selectedName)
                 .remove(KEY_CHECKED_INGREDIENTS) // Reset checkbox on new recipe select
                 .apply();

            checkedIngredientsSet.clear();
            displayRecipeIngredients(selectedName);
            Toast.makeText(getContext(), "Đã cập nhật danh sách đi chợ cho " + selectedName, Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private void showMoreOptionsDialog() {
        String[] options = {"Đánh dấu đã mua hết", "Bỏ đánh dấu toàn bộ", "Xóa danh sách"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Tùy chọn");
        builder.setItems(options, (dialog, which) -> {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            if (which == 0) { // Mark all
                checkedIngredientsSet.clear();
                String savedRecipeName = prefs.getString(KEY_CURRENT_RECIPE_NAME, "Bún bò Huế");
                Recipe active = null;
                for (Recipe r : allAvailableRecipes) {
                    if (r.getName().equalsIgnoreCase(savedRecipeName)) active = r;
                }
                if (active != null) {
                    for (Ingredient ing : active.getIngredients()) {
                        checkedIngredientsSet.add(ing.getName().toLowerCase());
                    }
                }
                prefs.edit().putStringSet(KEY_CHECKED_INGREDIENTS, checkedIngredientsSet).apply();
                displayRecipeIngredients(savedRecipeName);
            } else if (which == 1) { // Unmark all
                checkedIngredientsSet.clear();
                prefs.edit().putStringSet(KEY_CHECKED_INGREDIENTS, checkedIngredientsSet).apply();
                String savedRecipeName = prefs.getString(KEY_CURRENT_RECIPE_NAME, "Bún bò Huế");
                displayRecipeIngredients(savedRecipeName);
            } else { // Clear completely
                prefs.edit()
                     .putString(KEY_CURRENT_RECIPE_NAME, "")
                     .remove(KEY_CHECKED_INGREDIENTS)
                     .apply();
                checkedIngredientsSet.clear();
                displayRecipeIngredients("");
            }
        });
        builder.show();
    }

    // High fidelity presets matching screenshots and expectations
    private Recipe getBunBoHueRecipe() {
        Recipe r = new Recipe(201, "Bún bò Huế", 90, 4.9, 550, R.drawable.character_chef_1);
        List<Ingredient> ings = new ArrayList<>();
        ings.add(new Ingredient("500g", "Bắp bò"));
        ings.add(new Ingredient("1kg", "Giò heo"));
        ings.add(new Ingredient("300g", "Chả cua"));
        ings.add(new Ingredient("3 cây", "Sả"));
        ings.add(new Ingredient("1 củ", "Hành tây"));
        ings.add(new Ingredient("2 trái", "Chanh"));
        ings.add(new Ingredient("2 muỗng", "Mắm ruốc Huế"));
        ings.add(new Ingredient("Vừa đủ", "Rau thơm, giá đỗ"));
        r.setIngredients(ings);
        return r;
    }

    private Recipe getPhoBoRecipe() {
        Recipe r = new Recipe(101, "Phở Bò Gia Truyền", 120, 4.9, 450, R.drawable.character_chef_1);
        List<Ingredient> ings = new ArrayList<>();
        ings.add(new Ingredient("500g", "Thịt bò thăn"));
        ings.add(new Ingredient("1kg", "Xương ống bò"));
        ings.add(new Ingredient("1 củ", "Hành tây"));
        ings.add(new Ingredient("1 củ", "Hành tím"));
        ings.add(new Ingredient("1 nhánh", "Gừng tươi"));
        ings.add(new Ingredient("Vừa đủ", "Quế, hồi, thảo quả"));
        ings.add(new Ingredient("Vừa đủ", "Bánh phở, hành lá"));
        r.setIngredients(ings);
        return r;
    }

    private Recipe getBanhMiRecipe() {
        Recipe r = new Recipe(102, "Bánh Mì Thịt Nướng", 30, 4.8, 320, R.drawable.character_chef_2);
        List<Ingredient> ings = new ArrayList<>();
        ings.add(new Ingredient("400g", "Thịt nạc vai heo"));
        ings.add(new Ingredient("2 muỗng", "Sả băm"));
        ings.add(new Ingredient("1 muỗng", "Mật ong"));
        ings.add(new Ingredient("2 ổ", "Bánh mì giòn"));
        ings.add(new Ingredient("Vừa đủ", "Dưa leo, ngò rí, đồ chua"));
        r.setIngredients(ings);
        return r;
    }

    private Recipe getGoiCuonRecipe() {
        Recipe r = new Recipe(104, "Gỏi Cuốn Tôm Thịt chuẩn vị", 15, 4.7, 250, R.drawable.character_chef_2);
        List<Ingredient> ings = new ArrayList<>();
        ings.add(new Ingredient("300g", "Tôm tươi"));
        ings.add(new Ingredient("300g", "Thịt ba chỉ heo"));
        ings.add(new Ingredient("1 xấp", "Bánh tráng"));
        ings.add(new Ingredient("200g", "Bún tươi"));
        ings.add(new Ingredient("Vừa đủ", "Rau xà lách, hẹ, ngò rí"));
        ings.add(new Ingredient("Vừa đủ", "Tương hột, đậu phộng"));
        r.setIngredients(ings);
        return r;
    }
}
