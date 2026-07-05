package com.example.cookup_app.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.cookup_app.R;
import com.example.cookup_app.model.Ingredient;
import com.example.cookup_app.model.Recipe;
import com.example.cookup_app.model.RecipeStep;
import com.example.cookup_app.utils.RecipeDataHelper;
import com.example.cookup_app.utils.ThemeManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecipeDetailActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "cookup_prefs";
    private static final String BOOKMARKS_KEY = "bookmarked_recipes";
    private static final String PLAN_PREFS_NAME = "cookup_plan_prefs";
    private static final String KEY_CURRENT_RECIPE_NAME = "current_recipe_name";
    private static final String KEY_CHECKED_INGREDIENTS = "checked_ingredients";

    private Recipe recipe;
    private int defaultServings = 4;
    private int currentServings = 4;

    private ImageView imgRecipeBanner;
    private TextView tvDetailTitle;
    private TextView tvChefName;
    private TextView tvDetailDescription;
    private TextView tvDetailRating;
    private TextView tvDetailTime;
    private TextView tvDetailCalories;

    private TextView tabDetailIngredients;
    private TextView tabDetailSteps;
    private TextView tabDetailReviews;

    private View layoutTabIngredients;
    private View layoutTabSteps;
    private View layoutTabReviews;

    private LinearLayout containerDetailIngredients;
    private LinearLayout containerDetailSteps;

    private TextView tvPortionCount;
    private View btnPortionMinus;
    private View btnPortionPlus;

    private View btnAddToCart;
    private View btnStartCooking;
    private ImageView imgDetailBookmark;
    private View btnDetailBookmark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        // Retrieve Recipe
        recipe = (Recipe) getIntent().getSerializableExtra("recipe");
        if (recipe == null) {
            Toast.makeText(this, "Không tìm thấy công thức!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fill empty details if necessary
        recipe = RecipeDataHelper.populateDetails(recipe);
        defaultServings = recipe.getServings() > 0 ? recipe.getServings() : 4;
        currentServings = defaultServings;

        // Bind views
        imgRecipeBanner = findViewById(R.id.imgRecipeBanner);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvChefName = findViewById(R.id.tvChefName);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailRating = findViewById(R.id.tvDetailRating);
        tvDetailTime = findViewById(R.id.tvDetailTime);
        tvDetailCalories = findViewById(R.id.tvDetailCalories);

        tabDetailIngredients = findViewById(R.id.tabDetailIngredients);
        tabDetailSteps = findViewById(R.id.tabDetailSteps);
        tabDetailReviews = findViewById(R.id.tabDetailReviews);

        layoutTabIngredients = findViewById(R.id.layoutTabIngredients);
        layoutTabSteps = findViewById(R.id.layoutTabSteps);
        layoutTabReviews = findViewById(R.id.layoutTabReviews);

        containerDetailIngredients = findViewById(R.id.containerDetailIngredients);
        containerDetailSteps = findViewById(R.id.containerDetailSteps);

        tvPortionCount = findViewById(R.id.tvPortionCount);
        btnPortionMinus = findViewById(R.id.btnPortionMinus);
        btnPortionPlus = findViewById(R.id.btnPortionPlus);

        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnStartCooking = findViewById(R.id.btnStartCooking);
        imgDetailBookmark = findViewById(R.id.imgDetailBookmark);
        btnDetailBookmark = findViewById(R.id.btnDetailBookmark);

        findViewById(R.id.btnDetailBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDetailShare).setOnClickListener(v -> shareRecipe());
        findViewById(R.id.btnDetailReport).setOnClickListener(v -> showReportBottomSheet());

        // Setup base fields
        tvDetailTitle.setText(recipe.getName());
        tvDetailDescription.setText(recipe.getDescription() != null ? recipe.getDescription() : "Khám phá công thức nấu ăn chuẩn vị truyền thống.");
        tvDetailRating.setText(String.valueOf(recipe.getRating()));
        tvDetailTime.setText(recipe.getCookTimeMinutes() + " Phút");
        tvDetailCalories.setText(recipe.getCalories() + " kcal");
        tvPortionCount.setText(String.valueOf(currentServings));

        // Display images safely
        if (recipe.getImageUrl() != null && !recipe.getImageUrl().trim().isEmpty()) {
            Glide.with(this)
                    .load(recipe.getImageUrl())
                    .placeholder(R.drawable.character_chef_1)
                    .error(R.drawable.character_chef_1)
                    .into(imgRecipeBanner);
        } else if (recipe.getImageResId() != 0) {
            imgRecipeBanner.setImageResource(recipe.getImageResId());
        } else {
            imgRecipeBanner.setImageResource(R.drawable.character_chef_1);
        }

        // Set custom chef name
        if (recipe.getName().toLowerCase().contains("phở")) {
            tvChefName.setText("Bởi Chef Hoàng Hải");
            ImageView imgChef = findViewById(R.id.imgChefAvatar);
            imgChef.setImageResource(R.drawable.avatar_default_male);
        } else if (recipe.getName().toLowerCase().contains("bún bò")) {
            tvChefName.setText("Bởi Chef Minh Tuấn");
            ImageView imgChef = findViewById(R.id.imgChefAvatar);
            imgChef.setImageResource(R.drawable.avatar_default_male);
        } else {
            tvChefName.setText("Bởi Chef Thu Hà");
            ImageView imgChef = findViewById(R.id.imgChefAvatar);
            imgChef.setImageResource(R.drawable.avatar_default_female);
        }

        // Setup tabs listeners
        tabDetailIngredients.setOnClickListener(v -> selectTab(0));
        tabDetailSteps.setOnClickListener(v -> selectTab(1));
        tabDetailReviews.setOnClickListener(v -> selectTab(2));

        // Portion buttons
        btnPortionMinus.setOnClickListener(v -> {
            if (currentServings > 1) {
                currentServings--;
                tvPortionCount.setText(String.valueOf(currentServings));
                renderIngredients();
            }
        });

        btnPortionPlus.setOnClickListener(v -> {
            if (currentServings < 20) {
                currentServings++;
                tvPortionCount.setText(String.valueOf(currentServings));
                renderIngredients();
            }
        });

        // Add to shopping cart (Plan tab synchronization)
        btnAddToCart.setOnClickListener(v -> {
            SharedPreferences planPrefs = getSharedPreferences(PLAN_PREFS_NAME, Context.MODE_PRIVATE);
            planPrefs.edit()
                    .putString(KEY_CURRENT_RECIPE_NAME, recipe.getName())
                    .remove(KEY_CHECKED_INGREDIENTS) // Reset checkbox on adding new recipe
                    .apply();

            Toast.makeText(this, "Đã cập nhật nguyên liệu cho \"" + recipe.getName() + "\" vào Danh sách đi chợ!", Toast.LENGTH_LONG).show();
        });

        // Bookmark listener
        updateBookmarkUI();
        btnDetailBookmark.setOnClickListener(v -> toggleBookmark());

        // Start Cooking
        btnStartCooking.setOnClickListener(v -> {
            if (recipe.getSteps() == null || recipe.getSteps().isEmpty()) {
                Toast.makeText(this, "Không tìm thấy các bước nấu cho món này!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, CookingModeActivity.class);
            intent.putExtra("recipe", recipe);
            startActivity(intent);
        });

        // Initialize Lists
        renderIngredients();
        renderSteps();
    }

    private void selectTab(int index) {
        // Tab background and text coloring
        tabDetailIngredients.setBackgroundResource(index == 0 ? R.drawable.bg_segment_active : android.R.color.transparent);
        tabDetailIngredients.setTextColor(getResources().getColor(index == 0 ? R.color.text_primary : R.color.text_secondary));

        tabDetailSteps.setBackgroundResource(index == 1 ? R.drawable.bg_segment_active : android.R.color.transparent);
        tabDetailSteps.setTextColor(getResources().getColor(index == 1 ? R.color.text_primary : R.color.text_secondary));

        tabDetailReviews.setBackgroundResource(index == 2 ? R.drawable.bg_segment_active : android.R.color.transparent);
        tabDetailReviews.setTextColor(getResources().getColor(index == 2 ? R.color.text_primary : R.color.text_secondary));

        // Tab visibility
        layoutTabIngredients.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        layoutTabSteps.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        layoutTabReviews.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
    }

    private void renderIngredients() {
        containerDetailIngredients.removeAllViews();
        List<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients == null || ingredients.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Ingredient ing : ingredients) {
            View view = inflater.inflate(R.layout.item_detail_ingredient, containerDetailIngredients, false);
            TextView tvName = view.findViewById(R.id.tvDetailIngName);
            TextView tvQty = view.findViewById(R.id.tvDetailIngQty);

            tvName.setText(ing.getName());
            tvQty.setText(scaleQuantity(ing.getQty(), defaultServings, currentServings));

            containerDetailIngredients.addView(view);
        }
    }

    private void renderSteps() {
        containerDetailSteps.removeAllViews();
        List<RecipeStep> steps = recipe.getSteps();
        if (steps == null || steps.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        for (RecipeStep step : steps) {
            View view = inflater.inflate(R.layout.item_detail_step, containerDetailSteps, false);
            TextView tvNum = view.findViewById(R.id.tvDetailStepNum);
            TextView tvDesc = view.findViewById(R.id.tvDetailStepDesc);

            tvNum.setText(String.valueOf(step.getStepNumber()));
            tvDesc.setText(step.getInstructions());

            containerDetailSteps.addView(view);
        }
    }

    private void toggleBookmark() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        HashSet<String> bookmarks = new HashSet<>(prefs.getStringSet(BOOKMARKS_KEY, new HashSet<>()));

        String recipeIdStr = String.valueOf(recipe.getId());
        if (bookmarks.contains(recipeIdStr)) {
            bookmarks.remove(recipeIdStr);
            Toast.makeText(this, "Đã bỏ yêu thích món ăn!", Toast.LENGTH_SHORT).show();
        } else {
            bookmarks.add(recipeIdStr);
            Toast.makeText(this, "Đã thêm vào danh sách yêu thích!", Toast.LENGTH_SHORT).show();
        }
        prefs.edit().putStringSet(BOOKMARKS_KEY, bookmarks).apply();
        updateBookmarkUI();
    }

    private void updateBookmarkUI() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        HashSet<String> bookmarks = new HashSet<>(prefs.getStringSet(BOOKMARKS_KEY, new HashSet<>()));
        boolean isBookmarked = bookmarks.contains(String.valueOf(recipe.getId()));

        if (isBookmarked) {
            imgDetailBookmark.setImageResource(R.drawable.ic_bookmark);
            imgDetailBookmark.setColorFilter(getResources().getColor(R.color.orange_primary));
        } else {
            imgDetailBookmark.setImageResource(R.drawable.ic_bookmark);
            imgDetailBookmark.setColorFilter(getResources().getColor(R.color.text_primary));
        }
    }

    private void shareRecipe() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Hãy xem thử công thức cực ngon \"" + recipe.getName() + "\" trên CookUp nhé!");
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Chia sẻ công thức"));
    }

    // Dynamic ingredient scaling helper
    private String scaleQuantity(String qtyStr, int originalPortion, int targetPortion) {
        if (qtyStr == null || qtyStr.trim().isEmpty() || qtyStr.equalsIgnoreCase("vừa đủ") || qtyStr.equalsIgnoreCase("tùy chọn")) {
            return qtyStr;
        }

        double ratio = (double) targetPortion / originalPortion;

        // Try parsing numbers (e.g. "500g", "1.5 muỗng", "2 cây", "1kg")
        Pattern pattern = Pattern.compile("([\\d.]+)\\s*([\\w\\s\\p{L}]+)");
        Matcher matcher = pattern.matcher(qtyStr.trim());
        if (matcher.find()) {
            try {
                double numericVal = Double.parseDouble(matcher.group(1));
                double scaled = numericVal * ratio;
                String unit = matcher.group(2);

                // Format decimal beautifully
                String formattedVal;
                if (scaled == (long) scaled) {
                    formattedVal = String.format("%d", (long) scaled);
                } else {
                    formattedVal = String.format("%.1f", scaled);
                }

                // If unit is kg and less than 1, convert to g
                if ("kg".equalsIgnoreCase(unit.trim()) && scaled < 1.0) {
                    scaled = scaled * 1000;
                    unit = "g";
                    if (scaled == (long) scaled) {
                        formattedVal = String.format("%d", (long) scaled);
                    } else {
                        formattedVal = String.format("%.1f", scaled);
                    }
                }

                return formattedVal + " " + unit;
            } catch (Exception e) {
                return qtyStr; // Fallback
            }
        }

        return qtyStr;
    }

    private void showReportBottomSheet() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để báo cáo công thức!", Toast.LENGTH_LONG).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_report, null);
        dialog.setContentView(view);

        RadioGroup rgReasons = view.findViewById(R.id.rgReportReasons);
        TextInputEditText etDetails = view.findViewById(R.id.etReportDetails);
        View btnCancel = view.findViewById(R.id.btnCancelReport);
        View btnSubmit = view.findViewById(R.id.btnSubmitReport);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            int checkedId = rgReasons.getCheckedRadioButtonId();
            String reason = "Spam hoặc quảng cáo";
            if (checkedId == R.id.rbInappropriate) {
                reason = "Hình ảnh không phù hợp / phản cảm";
            } else if (checkedId == R.id.rbIncorrect) {
                reason = "Công thức sai lệch / thông tin nguy hại";
            } else if (checkedId == R.id.rbOther) {
                reason = "Lý do khác";
            }

            String details = etDetails.getText() != null ? etDetails.getText().toString().trim() : "";
            submitReportToFirestore(reason, details, dialog);
        });

        dialog.show();
    }

    private void submitReportToFirestore(String reason, String details, BottomSheetDialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String reportId = UUID.randomUUID().toString();
        Map<String, Object> report = new HashMap<>();
        report.put("reportId", reportId);
        report.put("recipeId", recipe.getId());
        report.put("recipeName", recipe.getName());
        report.put("recipeImageUrl", recipe.getImageUrl() != null ? recipe.getImageUrl() : "");
        report.put("recipeImageResId", recipe.getImageResId());
        
        // Author logic matching UI
        String author = "Chef Thu Hà";
        if (recipe.getName().toLowerCase().contains("phở")) {
            author = "Chef Hoàng Hải";
        } else if (recipe.getName().toLowerCase().contains("bún bò")) {
            author = "Chef Minh Tuấn";
        }
        report.put("recipeAuthor", author);
        
        report.put("reporterUid", user.getUid());
        report.put("reporterEmail", user.getEmail() != null ? user.getEmail() : "Ẩn danh");
        report.put("reason", reason);
        report.put("details", details);
        report.put("timestamp", System.currentTimeMillis());
        report.put("status", "pending");

        FirebaseFirestore.getInstance().collection("reports")
                .document(reportId)
                .set(report)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RecipeDetailActivity.this, "Cảm ơn bạn! Báo cáo của bạn đã được gửi đến ban quản trị.", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RecipeDetailActivity.this, "Gửi báo cáo thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
