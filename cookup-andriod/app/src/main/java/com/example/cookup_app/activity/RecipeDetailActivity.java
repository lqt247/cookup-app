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
import androidx.appcompat.app.AlertDialog;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import android.graphics.Color;
import android.content.res.ColorStateList;

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
    private TextView tvDetailReviewsCount;
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
    private View btnDetailEdit;

    // Review fields
    private LinearLayout containerReviews;
    private int selectedReviewRating = 5;
    private ImageView[] imgStars = new ImageView[5];
    private EditText edtReviewComment;
    private com.google.android.material.button.MaterialButton btnSubmitReview;

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
        tvDetailReviewsCount = findViewById(R.id.tvDetailReviewsCount);
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
        btnDetailEdit = findViewById(R.id.btnDetailEdit);

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
        if (recipe.getImageUrl() != null && !recipe.getImageUrl().trim().isEmpty() && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(this, recipe.getImageUrl())) {
            Glide.with(this)
                    .load(recipe.getImageUrl())
                    .placeholder(R.drawable.character_chef_1)
                    .error(R.drawable.character_chef_1)
                    .into(imgRecipeBanner);
        } else if (com.example.cookup_app.utils.RecipeDataHelper.isValidDrawable(this, recipe.getImageResId())) {
            imgRecipeBanner.setImageResource(recipe.getImageResId());
        } else {
            imgRecipeBanner.setImageResource(R.drawable.character_chef_1);
        }

        // Check ownership to show Edit button
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && recipe.getCreatorUid() != null && currentUser.getUid().equals(recipe.getCreatorUid())) {
            if (btnDetailEdit != null) {
                btnDetailEdit.setVisibility(View.VISIBLE);
                btnDetailEdit.setOnClickListener(v -> {
                    Intent intent = new Intent(this, AddRecipeActivity.class);
                    intent.putExtra("edit_recipe", recipe);
                    startActivity(intent);
                });
            }
        }

        // Set custom chef name (dynamically pulled from database if creatorUid is present)
        final String[] finalChefNameHolder = {recipe.getChefName()};
        if (finalChefNameHolder[0] == null || finalChefNameHolder[0].trim().isEmpty()) {
            if (recipe.getName().toLowerCase().contains("phở")) {
                finalChefNameHolder[0] = "Chef Hoàng Hải";
            } else if (recipe.getName().toLowerCase().contains("bún bò")) {
                finalChefNameHolder[0] = "Chef Minh Tuấn";
            } else {
                finalChefNameHolder[0] = "Chef Thu Hà";
            }
        }
        tvChefName.setText("Bởi " + finalChefNameHolder[0]);

        ImageView imgChef = findViewById(R.id.imgChefAvatar);
        if (imgChef != null) {
            imgChef.setImageResource(R.drawable.avatar_default_unknown);
        }

        View.OnClickListener chefProfileClickListener = v -> {
            if (recipe.getCreatorUid() != null && !recipe.getCreatorUid().isEmpty()) {
                Intent profileIntent = new Intent(RecipeDetailActivity.this, ProfileActivity.class);
                profileIntent.putExtra("userId", recipe.getCreatorUid());
                startActivity(profileIntent);
            }
        };
        tvChefName.setOnClickListener(chefProfileClickListener);
        if (imgChef != null) {
            imgChef.setOnClickListener(chefProfileClickListener);
        }

        if (recipe.getCreatorUid() != null && !recipe.getCreatorUid().isEmpty()) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(recipe.getCreatorUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String realName = documentSnapshot.getString("displayName");
                            if (realName == null || realName.trim().isEmpty()) {
                                realName = documentSnapshot.getString("name");
                            }
                            String realAvatarUrl = documentSnapshot.getString("avatarUrl");
                            if (realName != null && !realName.trim().isEmpty()) {
                                finalChefNameHolder[0] = realName;
                                tvChefName.setText("Bởi " + realName);
                            }
                            if (imgChef != null) {
                                if (realAvatarUrl != null && !realAvatarUrl.trim().isEmpty() && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(this, realAvatarUrl)) {
                                    Glide.with(RecipeDetailActivity.this)
                                            .load(realAvatarUrl)
                                            .circleCrop()
                                            .placeholder(R.drawable.avatar_default_unknown)
                                            .error(R.drawable.avatar_default_unknown)
                                            .into(imgChef);
                                }
                            }
                        }
                    });
        }

        // Setup Follow Chef button logic with Firebase "follows" collection (Preventing self-following)
        MaterialButton btnFollowChef = findViewById(R.id.btnFollowChef);
        String creatorUid = recipe.getCreatorUid();

        if (btnFollowChef != null) {
            if (currentUser != null && creatorUid != null && currentUser.getUid().equals(creatorUid)) {
                btnFollowChef.setVisibility(View.GONE); // Self cannot follow self
            } else {
                btnFollowChef.setVisibility(View.VISIBLE);
                if (currentUser != null && creatorUid != null) {
                    String followDocId = currentUser.getUid() + "_" + creatorUid;
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("follows")
                            .document(followDocId)
                            .get()
                            .addOnSuccessListener(doc -> {
                                boolean isFollowing = doc.exists();
                                updateFollowButtonUI(btnFollowChef, isFollowing);
                                btnFollowChef.setOnClickListener(v -> toggleFollowChef(btnFollowChef, isFollowing, creatorUid, finalChefNameHolder[0]));
                            });
                } else {
                    btnFollowChef.setOnClickListener(v -> {
                        Toast.makeText(this, "Vui lòng đăng nhập để theo dõi đầu bếp!", Toast.LENGTH_SHORT).show();
                    });
                }
            }
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

        // Dynamic star rating & reviews submission setup
        containerReviews = findViewById(R.id.containerReviews);
        edtReviewComment = findViewById(R.id.edtReviewComment);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        imgStars[0] = findViewById(R.id.imgStar1);
        imgStars[1] = findViewById(R.id.imgStar2);
        imgStars[2] = findViewById(R.id.imgStar3);
        imgStars[3] = findViewById(R.id.imgStar4);
        imgStars[4] = findViewById(R.id.imgStar5);

        android.view.View cardWriteReview = findViewById(R.id.cardWriteReview);
        if (cardWriteReview != null && currentUser != null && creatorUid != null && currentUser.getUid().equals(creatorUid)) {
            cardWriteReview.setVisibility(View.GONE);
        }

        setupStarRatingSelector();
        setupReviewSubmission();
        loadRecipeReviews();

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

    private void updateFollowButtonUI(MaterialButton button, boolean isFollowing) {
        if (isFollowing) {
            button.setText("Đang theo dõi");
            button.setTextColor(Color.parseColor("#FFF1ED"));
            button.setBackgroundColor(Color.parseColor("#FF7238"));
            button.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FF7238")));
        } else {
            button.setText("Theo dõi");
            button.setTextColor(Color.parseColor("#FF7238"));
            button.setBackgroundColor(Color.parseColor("#160E0C"));
            button.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FF7238")));
        }
    }

    private void toggleFollowChef(MaterialButton btnFollowChef, boolean currentIsFollowing, String creatorUid, String chefName) {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || creatorUid == null) return;

        String followDocId = currentUser.getUid() + "_" + creatorUid;
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        btnFollowChef.setEnabled(false); // Disable during network call

        if (currentIsFollowing) {
            // Unfollow
            db.collection("follows").document(followDocId).delete()
                    .addOnSuccessListener(aVoid -> {
                        db.collection("users").document(creatorUid)
                                .update("followersCount", com.google.firebase.firestore.FieldValue.increment(-1));

                        Toast.makeText(this, "Đã bỏ theo dõi " + chefName, Toast.LENGTH_SHORT).show();
                        btnFollowChef.setEnabled(true);
                        updateFollowButtonUI(btnFollowChef, false);
                        // Rebind click listener with updated state
                        btnFollowChef.setOnClickListener(v -> toggleFollowChef(btnFollowChef, false, creatorUid, chefName));
                    })
                    .addOnFailureListener(e -> btnFollowChef.setEnabled(true));
        } else {
            // Follow
            java.util.Map<String, Object> followData = new java.util.HashMap<>();
            followData.put("followerUid", currentUser.getUid());
            followData.put("followedUid", creatorUid);

            String tempFollowerName = currentUser.getDisplayName();
            if (android.text.TextUtils.isEmpty(tempFollowerName)) {
                tempFollowerName = currentUser.getEmail() != null ? currentUser.getEmail().split("@")[0] : "Một đầu bếp";
            }
            final String followerName = tempFollowerName;
            followData.put("followerName", followerName);
            followData.put("followedName", chefName);
            followData.put("timestamp", System.currentTimeMillis());

            db.collection("follows").document(followDocId).set(followData)
                    .addOnSuccessListener(aVoid -> {
                        db.collection("users").document(creatorUid)
                                .update("followersCount", com.google.firebase.firestore.FieldValue.increment(1))
                                .addOnFailureListener(e -> {
                                    java.util.Map<String, Object> initialData = new java.util.HashMap<>();
                                    initialData.put("followersCount", 1);
                                    initialData.put("name", chefName);
                                    db.collection("users").document(creatorUid).set(initialData, com.google.firebase.firestore.SetOptions.merge());
                                });

                        // Send real notification
                        java.util.Map<String, Object> notificationData = new java.util.HashMap<>();
                        notificationData.put("title", "Người theo dõi mới");
                        notificationData.put("body", followerName + " đã bắt đầu theo dõi gian bếp của bạn. 👥");
                        notificationData.put("type", "follow");
                        notificationData.put("timestamp", System.currentTimeMillis());
                        notificationData.put("read", false);
                        notificationData.put("recipientUid", creatorUid);
                        notificationData.put("senderUid", currentUser.getUid());

                        db.collection("notifications").add(notificationData);

                        Toast.makeText(this, "Đã theo dõi " + chefName + "!", Toast.LENGTH_SHORT).show();
                        btnFollowChef.setEnabled(true);
                        updateFollowButtonUI(btnFollowChef, true);
                        // Rebind click listener with updated state
                        btnFollowChef.setOnClickListener(v -> toggleFollowChef(btnFollowChef, true, creatorUid, chefName));
                    })
                    .addOnFailureListener(e -> btnFollowChef.setEnabled(true));
        }
    }

    private void toggleBookmark() {
        com.example.cookup_app.utils.CollectionHelper.showSaveToCollectionDialog(this, recipe, () -> updateBookmarkUI());
    }

    private void updateBookmarkUI() {
        com.example.cookup_app.utils.CollectionHelper.checkIsBookmarked(this, recipe.getId(), isBookmarked -> {
            if (isBookmarked) {
                imgDetailBookmark.setColorFilter(getResources().getColor(R.color.orange_primary));
            } else {
                imgDetailBookmark.setColorFilter(getResources().getColor(R.color.text_primary));
            }
        });
    }

    private void setupStarRatingSelector() {
        for (int i = 0; i < 5; i++) {
            final int index = i;
            imgStars[i].setOnClickListener(v -> {
                selectedReviewRating = index + 1;
                updateStarRatingUI();
            });
        }
        updateStarRatingUI();
    }

    private void updateStarRatingUI() {
        for (int i = 0; i < 5; i++) {
            if (i < selectedReviewRating) {
                imgStars[i].setColorFilter(getResources().getColor(R.color.orange_primary));
            } else {
                imgStars[i].setColorFilter(getResources().getColor(R.color.text_secondary));
            }
        }
    }

    private void setupReviewSubmission() {
        btnSubmitReview.setOnClickListener(v -> {
            com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để gửi đánh giá!", Toast.LENGTH_SHORT).show();
                return;
            }

            String creatorUid = recipe.getCreatorUid();
            if (creatorUid != null && creatorUid.equals(currentUser.getUid())) {
                Toast.makeText(this, "Bạn không thể tự đánh giá công thức nấu ăn của chính mình! ❤️", Toast.LENGTH_LONG).show();
                return;
            }

            String comment = edtReviewComment.getText().toString().trim();
            if (comment.isEmpty()) {
                edtReviewComment.setError("Vui lòng nhập nội dung đánh giá");
                edtReviewComment.requestFocus();
                return;
            }

            btnSubmitReview.setEnabled(false);

            String reviewId = java.util.UUID.randomUUID().toString();
            String recipeIdStr = String.valueOf(recipe.getId());

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String customUserName = prefs.getString("custom_user_name", "");
            String customAvatarUrl = prefs.getString("custom_avatar_url", "");

            String userName = customUserName;
            if (android.text.TextUtils.isEmpty(userName)) {
                userName = currentUser.getDisplayName();
            }
            if (android.text.TextUtils.isEmpty(userName)) {
                userName = currentUser.getEmail() != null ? currentUser.getEmail().split("@")[0] : "Một đầu bếp";
            }

            com.example.cookup_app.model.RecipeReview review = new com.example.cookup_app.model.RecipeReview(
                    reviewId,
                    recipeIdStr,
                    currentUser.getUid(),
                    userName,
                    customAvatarUrl,
                    selectedReviewRating,
                    comment
            );

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("reviews")
                    .document(reviewId)
                    .set(review)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Gửi đánh giá thành công! Cảm ơn bạn ❤️", Toast.LENGTH_SHORT).show();
                        edtReviewComment.setText("");
                        selectedReviewRating = 5;
                        updateStarRatingUI();
                        btnSubmitReview.setEnabled(true);

                        updateRecipeAverageRating(recipeIdStr);
                        loadRecipeReviews();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi khi gửi đánh giá: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSubmitReview.setEnabled(true);
                    });
        });
    }

    private void updateRecipeAverageRating(String recipeIdStr) {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        db.collection("reviews")
                .whereEqualTo("recipeId", recipeIdStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = 0;
                    double avgRating = 0.0;
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        double totalRating = 0;
                        count = queryDocumentSnapshots.size();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            com.example.cookup_app.model.RecipeReview r = doc.toObject(com.example.cookup_app.model.RecipeReview.class);
                            if (r != null) {
                                totalRating += r.getRating();
                            }
                        }
                        avgRating = totalRating / count;
                        avgRating = Math.round(avgRating * 10.0) / 10.0;
                    }

                    db.collection("recipes")
                            .document(recipeIdStr)
                            .update("rating", avgRating);

                    tvDetailRating.setText(String.valueOf(avgRating));
                    if (tvDetailReviewsCount != null) {
                        tvDetailReviewsCount.setText(count + " Đánh giá");
                    }
                    recipe.setRating(avgRating);
                });
    }

    private void loadRecipeReviews() {
        if (containerReviews == null) return;
        containerReviews.removeAllViews();

        String recipeIdStr = String.valueOf(recipe.getId());
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("reviews")
                .whereEqualTo("recipeId", recipeIdStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = 0;
                    double avgRating = 0.0;
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        count = queryDocumentSnapshots.size();
                        double totalRating = 0;
                        LayoutInflater inflater = LayoutInflater.from(this);
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            com.example.cookup_app.model.RecipeReview review = doc.toObject(com.example.cookup_app.model.RecipeReview.class);
                            if (review != null) {
                                totalRating += review.getRating();
                                View view = inflater.inflate(R.layout.item_detail_review, containerReviews, false);

                                ImageView imgAvatar = view.findViewById(R.id.imgReviewAvatar);
                                TextView tvName = view.findViewById(R.id.tvReviewUserName);
                                TextView tvStars = view.findViewById(R.id.tvReviewStars);
                                TextView tvComment = view.findViewById(R.id.tvReviewComment);

                                // Show the snapshot first for instant feedback, then override with
                                // the reviewer's LIVE profile data below (name/avatar may have changed since).
                                tvName.setText(review.getUserName());
                                tvComment.setText(review.getComment());

                                StringBuilder starsText = new StringBuilder();
                                for (int i = 0; i < 5; i++) {
                                    if (i < (int)review.getRating()) {
                                        starsText.append("⭐");
                                    } else {
                                        starsText.append("☆");
                                    }
                                }
                                starsText.append(" ").append(review.getRating());
                                tvStars.setText(starsText.toString());

                                if (review.getUserAvatar() != null && !review.getUserAvatar().trim().isEmpty() && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(this, review.getUserAvatar())) {
                                    Glide.with(RecipeDetailActivity.this)
                                            .load(review.getUserAvatar())
                                            .circleCrop()
                                            .placeholder(R.drawable.avatar_default_unknown)
                                            .error(R.drawable.avatar_default_unknown)
                                            .into(imgAvatar);
                                } else {
                                    imgAvatar.setImageResource(R.drawable.avatar_default_unknown);
                                }

                                // Tap a review to open that reviewer's public profile (same as the follow list).
                                final String reviewerUid = review.getUserId();
                                if (!android.text.TextUtils.isEmpty(reviewerUid)) {
                                    view.setClickable(true);
                                    view.setFocusable(true);
                                    view.setOnClickListener(v -> {
                                        Intent profileIntent = new Intent(RecipeDetailActivity.this, ProfileActivity.class);
                                        profileIntent.putExtra("userId", reviewerUid);
                                        startActivity(profileIntent);
                                    });

                                    // Fetch the reviewer's current profile so name/avatar always match
                                    // what they've set on their profile, regardless of when they reviewed.
                                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(reviewerUid)
                                            .get()
                                            .addOnSuccessListener(userDoc -> {
                                                if (userDoc == null || !userDoc.exists() || isFinishing()) return;

                                                String liveName = userDoc.getString("displayName");
                                                if (android.text.TextUtils.isEmpty(liveName)) {
                                                    liveName = userDoc.getString("name");
                                                }
                                                if (!android.text.TextUtils.isEmpty(liveName)) {
                                                    tvName.setText(liveName);
                                                }

                                                String liveAvatar = userDoc.getString("avatarUrl");
                                                if (!android.text.TextUtils.isEmpty(liveAvatar) && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(this, liveAvatar)) {
                                                    Glide.with(RecipeDetailActivity.this)
                                                            .load(liveAvatar)
                                                            .circleCrop()
                                                            .placeholder(R.drawable.avatar_default_unknown)
                                                            .error(R.drawable.avatar_default_unknown)
                                                            .into(imgAvatar);
                                                } else if (android.text.TextUtils.isEmpty(review.getUserAvatar())) {
                                                    imgAvatar.setImageResource(R.drawable.avatar_default_unknown);
                                                }
                                            });
                                }

                                containerReviews.addView(view);
                            }
                        }
                        avgRating = totalRating / count;
                        avgRating = Math.round(avgRating * 10.0) / 10.0;
                        tvDetailRating.setText(String.valueOf(avgRating));
                        if (tvDetailReviewsCount != null) {
                            tvDetailReviewsCount.setText(count + " Đánh giá");
                        }
                        recipe.setRating(avgRating);
                    } else {
                        tvDetailRating.setText(String.valueOf(recipe.getRating() == 0.0 ? "0.0" : recipe.getRating()));
                        if (tvDetailReviewsCount != null) {
                            tvDetailReviewsCount.setText("0 Đánh giá");
                        }
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("Chưa có đánh giá nào cho công thức này. Hãy là người đầu tiên chia sẻ cảm nhận!");
                        tvEmpty.setTextColor(getResources().getColor(R.color.text_secondary));
                        tvEmpty.setGravity(android.view.Gravity.CENTER);
                        tvEmpty.setPadding(32, 32, 32, 32);
                        containerReviews.addView(tvEmpty);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải đánh giá: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
