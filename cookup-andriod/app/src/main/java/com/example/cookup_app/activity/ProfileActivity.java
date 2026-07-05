package com.example.cookup_app.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.cookup_app.R;
import com.example.cookup_app.model.Recipe;
import com.example.cookup_app.utils.ThemeManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.cookup_app.model.RecipeCollection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "cookup_prefs";
    private static final String KEY_GENDER = "gender";
    private static final String KEY_USER_NAME = "custom_user_name";
    private static final String KEY_USER_BIO = "custom_user_bio";
    private static final String KEY_CUSTOM_AVATAR = "custom_avatar_url";

    private ImageView imgProfileAvatar;
    private TextView tvProfileName;
    private TextView tvProfileBio;
    private TextView tvStatRecipesCount;
    private TextView tvStatFollowers;
    private TextView tvStatFavoritesCount;

    private TextView tabMyRecipes;
    private TextView tabLikedRecipes;
    private View btnEditProfile;
    private MaterialButton btnLogout;
    private GridLayout gridRecentlySaved;
    private android.widget.LinearLayout containerProfileCollections;

    private boolean currentTabIsMyRecipes = true;
    private ActivityResultLauncher<Intent> avatarPickerLauncher;
    private String targetUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Bind views
        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileBio = findViewById(R.id.tvProfileBio);
        tvStatRecipesCount = findViewById(R.id.tvStatRecipesCount);
        tvStatFollowers = findViewById(R.id.tvStatFollowers);
        tvStatFavoritesCount = findViewById(R.id.tvStatFavoritesCount);

        tabMyRecipes = findViewById(R.id.tabMyRecipes);
        tabLikedRecipes = findViewById(R.id.tabLikedRecipes);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);
        gridRecentlySaved = findViewById(R.id.gridRecentlySaved);
        containerProfileCollections = findViewById(R.id.containerProfileCollections);

        View btnBack = findViewById(R.id.btnProfileBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Setup Avatar picker launcher
        avatarPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        android.net.Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            try {
                                getContentResolver().takePersistableUriPermission(
                                        selectedImageUri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            // Load Avatar instantly from local for immediate feedback
                            Glide.with(ProfileActivity.this)
                                    .load(selectedImageUri)
                                    .circleCrop()
                                    .placeholder(R.drawable.avatar_default_unknown)
                                    .error(R.drawable.avatar_default_unknown)
                                    .into(imgProfileAvatar);

                            // Upload to Cloudinary (same pipeline used for recipe photos)
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();
                                com.example.cookup_app.utils.CloudinaryUploader.uploadImage(
                                        ProfileActivity.this,
                                        selectedImageUri,
                                        new com.example.cookup_app.utils.CloudinaryUploader.UploadCallback() {
                                            @Override
                                            public void onSuccess(String secureUrl) {
                                                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                                                prefs.edit().putString(KEY_CUSTOM_AVATAR + "_" + uid, secureUrl).apply();

                                                // Save to Firestore so it's visible everywhere (reviews, followers, etc.)
                                                saveUserProfileToFirestore(
                                                        prefs.getString(KEY_USER_NAME + "_" + uid, "Minh Anh"),
                                                        prefs.getString(KEY_USER_BIO + "_" + uid, ""),
                                                        secureUrl
                                                );

                                                // Refresh with the final remote URL (in case local preview differs)
                                                Glide.with(ProfileActivity.this)
                                                        .load(secureUrl)
                                                        .circleCrop()
                                                        .placeholder(R.drawable.avatar_default_unknown)
                                                        .error(R.drawable.avatar_default_unknown)
                                                        .into(imgProfileAvatar);

                                                Toast.makeText(ProfileActivity.this, "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                                            }

                                            @Override
                                            public void onFailure(Exception e) {
                                                Toast.makeText(ProfileActivity.this, "Lỗi tải ảnh đại diện lên đám mây: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                );
                            }
                        }
                    }
                }
        );

        // Determine if targetUserId is self or another user
        targetUserId = getIntent().getStringExtra("userId");
        if (targetUserId == null) {
            targetUserId = getIntent().getStringExtra("creatorUid");
        }
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isSelf = true;
        if (targetUserId == null && currentUser != null) {
            targetUserId = currentUser.getUid();
        } else if (targetUserId != null && currentUser != null && !targetUserId.equals(currentUser.getUid())) {
            isSelf = false;
        }

        final boolean finalIsSelf = isSelf;

        if (!isSelf) {
            // Hide edit profile, logout, settings, and admin dashboard
            if (btnEditProfile != null) btnEditProfile.setVisibility(View.GONE);
            if (btnLogout != null) btnLogout.setVisibility(View.GONE);

            View btnSettings = findViewById(R.id.btnProfileSettings);
            if (btnSettings != null) btnSettings.setVisibility(View.GONE);

            View btnAdminDashboard = findViewById(R.id.btnAdminDashboard);
            if (btnAdminDashboard != null) btnAdminDashboard.setVisibility(View.GONE);

            // Disable clicking on avatar to change it
            imgProfileAvatar.setOnClickListener(null);

            // Hide Liked Recipes tab since it is private
            if (tabLikedRecipes != null) tabLikedRecipes.setVisibility(View.GONE);
            if (tabMyRecipes != null) {
                tabMyRecipes.setText("Công thức");
                tabMyRecipes.setClickable(false);
                tabMyRecipes.setFocusable(false);
            }

            // Hide the Create Collection button
            View btnCreateCollection = findViewById(R.id.btnCreateCollection);
            if (btnCreateCollection != null) btnCreateCollection.setVisibility(View.GONE);

            // Show and init Profile Follow button
            MaterialButton btnProfileFollow = findViewById(R.id.btnProfileFollow);
            if (btnProfileFollow != null && currentUser != null && targetUserId != null) {
                btnProfileFollow.setVisibility(View.VISIBLE);
                checkFollowStatus(btnProfileFollow, currentUser.getUid(), targetUserId);
            }
        } else {
            // Tap on avatar directly to change it (only if self)
            imgProfileAvatar.setOnClickListener(v -> selectAvatarImage());

            // Register edit profile listener
            btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

            // Settings click
            View btnSettings = findViewById(R.id.btnProfileSettings);
            if (btnSettings != null) {
                btnSettings.setOnClickListener(v -> showSettingsDialog());
            }

            // Admin Dashboard Click & Role Checking
            View btnAdminDashboard = findViewById(R.id.btnAdminDashboard);
            if (btnAdminDashboard != null) {
                btnAdminDashboard.setVisibility(View.GONE); // Default hide

                if (currentUser != null) {
                    // Auto-unlock for the user's email for seamless testing
                    if ("lequangtruong2472005@gmail.com".equalsIgnoreCase(currentUser.getEmail())) {
                        btnAdminDashboard.setVisibility(View.VISIBLE);
                    } else {
                        // Check role in Firestore users collection
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(currentUser.getUid())
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        String role = documentSnapshot.getString("role");
                                        if ("admin".equalsIgnoreCase(role)) {
                                            btnAdminDashboard.setVisibility(View.VISIBLE);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    // Ignored silently
                                });
                    }
                }

                btnAdminDashboard.setOnClickListener(v -> {
                    Intent intent = new Intent(this, AdminActivity.class);
                    startActivity(intent);
                });
            }

            // Create collection
            View btnCreateCollection = findViewById(R.id.btnCreateCollection);
            if (btnCreateCollection != null) {
                btnCreateCollection.setOnClickListener(v -> showCreateCollectionDialog());
            }

            // Logout
            btnLogout.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit()
                        .remove("token")
                        .remove(KEY_USER_NAME)
                        .remove(KEY_USER_BIO)
                        .remove(KEY_CUSTOM_AVATAR)
                        .remove(KEY_GENDER)
                        .remove("bookmarked_recipes")
                        .apply();

                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(this, SplashActivity.class);
                startActivity(intent);
                finishAffinity();
            });
        }

        // Apply profile content
        loadProfileData();

        // Followers stats click (Any profile: clicking shows who follows this chef!)
        View btnStatFollowers = findViewById(R.id.btnStatFollowers);
        if (btnStatFollowers != null) {
            btnStatFollowers.setOnClickListener(v -> showFollowersListDialog());
        }

        // Segmented Tabs switching (only active for self)
        if (isSelf) {
            tabMyRecipes.setOnClickListener(v -> selectTab(true));
            tabLikedRecipes.setOnClickListener(v -> selectTab(false));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileData();
        loadRecipesToGrid(currentTabIsMyRecipes);
        loadProfileCollections();
    }

    private void selectAvatarImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        avatarPickerLauncher.launch(intent);
    }

    private void loadProfileData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isSelf = true;
        if (targetUserId == null && currentUser != null) {
            targetUserId = currentUser.getUid();
        } else if (targetUserId != null && currentUser != null && !targetUserId.equals(currentUser.getUid())) {
            isSelf = false;
        }

        if (!isSelf) {
            // Load other user's public info from Firestore
            FirebaseFirestore.getInstance().collection("users")
                    .document(targetUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String realName = documentSnapshot.getString("displayName");
                            if (TextUtils.isEmpty(realName)) {
                                realName = documentSnapshot.getString("name");
                            }
                            String realBio = documentSnapshot.getString("bio");
                            String realAvatarUrl = documentSnapshot.getString("avatarUrl");

                            if (!TextUtils.isEmpty(realName)) {
                                tvProfileName.setText(realName);
                            } else {
                                tvProfileName.setText("Người dùng CookUp");
                            }

                            if (!TextUtils.isEmpty(realBio)) {
                                tvProfileBio.setText(realBio);
                            } else {
                                tvProfileBio.setText("Đầu bếp này chưa cập nhật giới thiệu.");
                            }

                            if (!TextUtils.isEmpty(realAvatarUrl) && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(ProfileActivity.this, realAvatarUrl)) {
                                Glide.with(ProfileActivity.this)
                                        .load(realAvatarUrl)
                                        .circleCrop()
                                        .placeholder(R.drawable.avatar_default_unknown)
                                        .error(R.drawable.avatar_default_unknown)
                                        .into(imgProfileAvatar);
                            } else {
                                imgProfileAvatar.setImageResource(R.drawable.avatar_default_unknown);
                            }
                        } else {
                            tvProfileName.setText("Đầu bếp ẩn danh");
                            tvProfileBio.setText("Đầu bếp này chưa cập nhật thông tin.");
                            imgProfileAvatar.setImageResource(R.drawable.avatar_default_unknown);
                        }
                    });

            // Followers Count: Query users collection
            FirebaseFirestore.getInstance().collection("users")
                    .document(targetUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && tvStatFollowers != null) {
                            Long count = documentSnapshot.getLong("followersCount");
                            if (count != null) {
                                tvStatFollowers.setText(String.valueOf(count));
                            } else {
                                tvStatFollowers.setText("0");
                            }
                        } else if (tvStatFollowers != null) {
                            tvStatFollowers.setText("0");
                        }
                    });

            // Recipes Count: Query Firestore recipes collection
            FirebaseFirestore.getInstance().collection("recipes")
                    .whereEqualTo("creatorUid", targetUserId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots != null && tvStatRecipesCount != null) {
                            tvStatRecipesCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                        }
                    });

            // Favorites count is private for others, set to 0 or hide
            if (tvStatFavoritesCount != null) {
                tvStatFavoritesCount.setText("0");
            }
        } else {
            // Self: Load with custom Namespaced SharedPreferences and Firestore Priority
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            if (currentUser != null) {
                String uid = currentUser.getUid();

                // 1. Get cached Namespaced info
                String savedName = prefs.getString(KEY_USER_NAME + "_" + uid, "");
                String savedBio = prefs.getString(KEY_USER_BIO + "_" + uid, "");

                if (TextUtils.isEmpty(savedName)) {
                    savedName = currentUser.getDisplayName();
                }
                if (TextUtils.isEmpty(savedName)) {
                    savedName = currentUser.getEmail();
                    if (savedName != null && savedName.contains("@")) {
                        savedName = savedName.split("@")[0];
                    }
                }
                if (TextUtils.isEmpty(savedName)) {
                    savedName = "Minh Anh";
                }
                if (TextUtils.isEmpty(savedBio)) {
                    savedBio = "Yêu thích ẩm thực truyền thống Việt Nam. Thích chia sẻ công thức dễ làm tại nhà.";
                }

                tvProfileName.setText(savedName);
                tvProfileBio.setText(savedBio);

                // 2. Load Custom Avatar or Default Gender Avatar from Namespaced
                String customAvatarUrl = prefs.getString(KEY_CUSTOM_AVATAR + "_" + uid, "");
                if (!TextUtils.isEmpty(customAvatarUrl) && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(this, customAvatarUrl)) {
                    Glide.with(this)
                            .load(customAvatarUrl)
                            .circleCrop()
                            .placeholder(R.drawable.avatar_default_unknown)
                            .error(R.drawable.avatar_default_unknown)
                            .into(imgProfileAvatar);
                } else {
                    String gender = prefs.getString(KEY_GENDER + "_" + uid, "unknown");
                    if ("male".equalsIgnoreCase(gender)) {
                        imgProfileAvatar.setImageResource(R.drawable.avatar_default_male);
                    } else if ("female".equalsIgnoreCase(gender)) {
                        imgProfileAvatar.setImageResource(R.drawable.avatar_default_female);
                    } else {
                        imgProfileAvatar.setImageResource(R.drawable.avatar_default_unknown);
                    }
                }

                // 3. Query Firestore for Realtime / Online profile updates (Priority source of truth)
                FirebaseFirestore.getInstance().collection("users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String name = documentSnapshot.getString("name");
                                if (TextUtils.isEmpty(name)) {
                                    name = documentSnapshot.getString("displayName");
                                }
                                String bio = documentSnapshot.getString("bio");
                                String avatarUrl = documentSnapshot.getString("avatarUrl");

                                if (!TextUtils.isEmpty(name)) {
                                    tvProfileName.setText(name);
                                    prefs.edit().putString(KEY_USER_NAME + "_" + uid, name).apply();
                                }
                                if (bio != null) {
                                    tvProfileBio.setText(bio);
                                    prefs.edit().putString(KEY_USER_BIO + "_" + uid, bio).apply();
                                }
                                if (!TextUtils.isEmpty(avatarUrl)) {
                                    prefs.edit().putString(KEY_CUSTOM_AVATAR + "_" + uid, avatarUrl).apply();
                                    Glide.with(ProfileActivity.this)
                                            .load(avatarUrl)
                                            .circleCrop()
                                            .placeholder(R.drawable.avatar_default_unknown)
                                            .error(R.drawable.avatar_default_unknown)
                                            .into(imgProfileAvatar);
                                }
                            }
                        });

                // 4. Load dynamic statistics counts
                // Favorites Count: Read from Namespaced SharedPreferences Set
                HashSet<String> bookmarks = new HashSet<>(prefs.getStringSet("bookmarked_recipes_" + uid, new HashSet<>()));
                tvStatFavoritesCount.setText(String.valueOf(bookmarks.size()));

                // Recipes Count: Query Firestore recipes collection
                FirebaseFirestore.getInstance().collection("recipes")
                        .whereEqualTo("creatorUid", uid)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (queryDocumentSnapshots != null && tvStatRecipesCount != null) {
                                tvStatRecipesCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                            }
                        });

                // Followers Count: Query users collection
                FirebaseFirestore.getInstance().collection("users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists() && tvStatFollowers != null) {
                                Long count = documentSnapshot.getLong("followersCount");
                                if (count != null) {
                                    tvStatFollowers.setText(String.valueOf(count));
                                } else {
                                    tvStatFollowers.setText("0");
                                }
                            } else if (tvStatFollowers != null) {
                                tvStatFollowers.setText("0");
                            }
                        });
            }
        }
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder customBuilder = new AlertDialog.Builder(this);
        customBuilder.setTitle("Chỉnh sửa trang cá nhân");

        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        final EditText inputName = dialogView.findViewById(R.id.edtEditProfileName);
        final EditText inputBio = dialogView.findViewById(R.id.edtEditProfileBio);

        if (inputName != null) {
            inputName.setText(tvProfileName.getText());
        }
        if (inputBio != null) {
            inputBio.setText(tvProfileBio.getText());
        }

        customBuilder.setView(dialogView);

        customBuilder.setPositiveButton("Lưu", (dialog, which) -> {
            String newName = (inputName != null) ? inputName.getText().toString().trim() : "";
            String newBio = (inputBio != null) ? inputBio.getText().toString().trim() : "";

            if (!TextUtils.isEmpty(newName)) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String uid = user != null ? user.getUid() : "";
                prefs.edit()
                        .putString(KEY_USER_NAME + "_" + uid, newName)
                        .putString(KEY_USER_BIO + "_" + uid, newBio)
                        .apply();

                tvProfileName.setText(newName);
                tvProfileBio.setText(newBio);

                // Save user profile info to Firestore for social interaction
                String customAvatar = prefs.getString(KEY_CUSTOM_AVATAR + "_" + uid, "");
                saveUserProfileToFirestore(newName, newBio, customAvatar);

                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
            }
        });
        customBuilder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        customBuilder.show();
    }

    private void saveUserProfileToFirestore(String name, String bio, String avatarUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        java.util.Map<String, Object> userData = new java.util.HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("name", name);
        userData.put("bio", bio);
        userData.put("avatarUrl", avatarUrl);
        userData.put("email", user.getEmail());

        FirebaseFirestore.getInstance().collection("users")
                .document(user.getUid())
                .set(userData, com.google.firebase.firestore.SetOptions.merge());
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cài đặt ứng dụng ⚙");

        String[] options = {
                "Giao diện (Sáng / Tối)",
                "Đổi giới tính (Cập nhật avatar mặc định)",
                "Xóa lịch sử nấu ăn",
                "Xem điều khoản dịch vụ"
        };

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                ThemeManager.toggleTheme(this);
            } else if (which == 1) {
                showGenderSelectionDialog();
            } else if (which == 2) {
                Toast.makeText(this, "Đã dọn dẹp lịch sử nấu ăn!", Toast.LENGTH_SHORT).show();
            } else if (which == 3) {
                showTermsDialog();
            }
        });
        builder.setNegativeButton("Đóng", null);
        builder.show();
    }

    private void showGenderSelectionDialog() {
        String[] genders = {"Nam", "Nữ", "Khác (Mặc định)"};
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "";
        String currentGender = prefs.getString(KEY_GENDER + "_" + uid, "unknown");
        int checkedItem = 2;
        if ("male".equalsIgnoreCase(currentGender)) checkedItem = 0;
        else if ("female".equalsIgnoreCase(currentGender)) checkedItem = 1;

        new AlertDialog.Builder(this)
                .setTitle("Chọn giới tính")
                .setSingleChoiceItems(genders, checkedItem, (dialog, which) -> {
                    String newGender = "unknown";
                    if (which == 0) newGender = "male";
                    else if (which == 1) newGender = "female";

                    prefs.edit()
                            .putString(KEY_GENDER + "_" + uid, newGender)
                            .remove(KEY_CUSTOM_AVATAR + "_" + uid) // Remove custom photo to use fallback gender avatar
                            .apply();

                    if (!uid.isEmpty()) {
                        java.util.Map<String, Object> updates = new java.util.HashMap<>();
                        updates.put("gender", newGender);
                        updates.put("avatarUrl", ""); // Clear online custom avatarUrl
                        FirebaseFirestore.getInstance().collection("users")
                                .document(uid)
                                .set(updates, com.google.firebase.firestore.SetOptions.merge());
                    }

                    loadProfileData();
                    dialog.dismiss();
                    Toast.makeText(this, "Đã cập nhật giới tính & avatar mặc định!", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showTermsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Điều khoản dịch vụ")
                .setMessage("Chào mừng đến với CookUp! Bằng cách sử dụng ứng dụng mạng xã hội ẩm thực này, bạn đồng ý không đăng tải các nội dung độc hại, xúc phạm hoặc không phù hợp với chuẩn mực nấu ăn lành mạnh. Mọi công thức của bạn sẽ được hiển thị công khai tới mọi thành viên của cộng đồng.")
                .setPositiveButton("Tôi đồng ý", null)
                .show();
    }

    private void selectTab(boolean isMyRecipes) {
        currentTabIsMyRecipes = isMyRecipes;
        if (isMyRecipes) {
            tabMyRecipes.setBackgroundResource(R.drawable.bg_segment_active);
            tabMyRecipes.setTextColor(getResources().getColor(R.color.text_primary));

            tabLikedRecipes.setBackgroundResource(android.R.color.transparent);
            tabLikedRecipes.setTextColor(getResources().getColor(R.color.text_secondary));
        } else {
            tabLikedRecipes.setBackgroundResource(R.drawable.bg_segment_active);
            tabLikedRecipes.setTextColor(getResources().getColor(R.color.text_primary));

            tabMyRecipes.setBackgroundResource(android.R.color.transparent);
            tabMyRecipes.setTextColor(getResources().getColor(R.color.text_secondary));
        }
        loadRecipesToGrid(isMyRecipes);
    }

    private void loadRecipesToGrid(boolean isMyRecipes) {
        if (gridRecentlySaved == null) return;

        gridRecentlySaved.removeAllViews();

        FirebaseFirestore.getInstance().collection("recipes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Recipe> allRecipes = new ArrayList<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Recipe r = doc.toObject(Recipe.class);
                            if (r != null) {
                                r = com.example.cookup_app.utils.RecipeDataHelper.sanitizeAndHealRecipeId(doc, r);
                                boolean isFake = false;
                                try {
                                    int docIdVal = Integer.parseInt(doc.getId());
                                    if (docIdVal < 10000) {
                                        isFake = true;
                                    }
                                } catch (NumberFormatException e) {
                                    // Not integer doc ID
                                }

                                if (!isFake) {
                                    allRecipes.add(r);
                                }
                            }
                        }
                    }

                    if (isMyRecipes) {
                        List<Recipe> filteredRecipes = new ArrayList<>();
                        String queryUid = targetUserId != null ? targetUserId : "";
                        for (Recipe r : allRecipes) {
                            if (queryUid.equals(r.getCreatorUid())) {
                                filteredRecipes.add(r);
                            }
                        }
                        displayFilteredRecipes(filteredRecipes, isMyRecipes);
                    } else {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        if (user != null) {
                            String uid = user.getUid();
                            FirebaseFirestore.getInstance().collection("collections")
                                    .whereEqualTo("creatorUid", uid)
                                    .get()
                                    .addOnSuccessListener(colSnap -> {
                                        HashSet<String> bookmarks = new HashSet<>(prefs.getStringSet("bookmarked_recipes_" + uid, new HashSet<>()));
                                        if (colSnap != null) {
                                            for (DocumentSnapshot docCol : colSnap.getDocuments()) {
                                                RecipeCollection col = docCol.toObject(RecipeCollection.class);
                                                if (col != null && col.getRecipeIds() != null) {
                                                    bookmarks.addAll(col.getRecipeIds());
                                                }
                                            }
                                        }
                                        List<Recipe> filteredRecipes = new ArrayList<>();
                                        for (Recipe r : allRecipes) {
                                            if (bookmarks.contains(String.valueOf(r.getId()))) {
                                                filteredRecipes.add(r);
                                            }
                                        }
                                        displayFilteredRecipes(filteredRecipes, isMyRecipes);
                                    })
                                    .addOnFailureListener(e -> {
                                        HashSet<String> bookmarks = new HashSet<>(prefs.getStringSet("bookmarked_recipes_" + uid, new HashSet<>()));
                                        List<Recipe> filteredRecipes = new ArrayList<>();
                                        for (Recipe r : allRecipes) {
                                            if (bookmarks.contains(String.valueOf(r.getId()))) {
                                                filteredRecipes.add(r);
                                            }
                                        }
                                        displayFilteredRecipes(filteredRecipes, isMyRecipes);
                                    });
                        } else {
                            HashSet<String> bookmarks = new HashSet<>(prefs.getStringSet("bookmarked_recipes", new HashSet<>()));
                            List<Recipe> filteredRecipes = new ArrayList<>();
                            for (Recipe r : allRecipes) {
                                if (bookmarks.contains(String.valueOf(r.getId()))) {
                                    filteredRecipes.add(r);
                                }
                            }
                            displayFilteredRecipes(filteredRecipes, isMyRecipes);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải danh sách: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayFilteredRecipes(List<Recipe> filteredRecipes, boolean isMyRecipes) {
        if (gridRecentlySaved == null) return;
        gridRecentlySaved.removeAllViews();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Update count stats in UI
        if (isMyRecipes) {
            tvStatRecipesCount.setText(String.valueOf(filteredRecipes.size()));
        } else {
            tvStatFavoritesCount.setText(String.valueOf(filteredRecipes.size()));
        }

        if (filteredRecipes.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText(isMyRecipes
                    ? "Bạn chưa có công thức nấu ăn nào tự đăng. Hãy ấn nút '+' để đóng góp cho cộng đồng!"
                    : "Danh sách yêu thích đang trống.");
            tvEmpty.setTextColor(getResources().getColor(R.color.text_secondary));
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            tvEmpty.setPadding(48, 48, 48, 48);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.columnSpec = GridLayout.spec(0, 2, 1f); // Span 2 columns
            tvEmpty.setLayoutParams(params);
            gridRecentlySaved.addView(tvEmpty);
        } else {
            for (Recipe r : filteredRecipes) {
                View itemCard = LayoutInflater.from(this).inflate(R.layout.item_search_grid, gridRecentlySaved, false);

                ImageView imgRecipe = itemCard.findViewById(R.id.imgRecipe);
                TextView tvRecipeName = itemCard.findViewById(R.id.tvRecipeName);
                TextView tvRecipeCategory = itemCard.findViewById(R.id.tvRecipeCategory);
                TextView tvRecipeTime = itemCard.findViewById(R.id.tvRecipeTime);
                View btnBookmark = itemCard.findViewById(R.id.btnBookmark);

                tvRecipeName.setText(r.getName());
                tvRecipeCategory.setText(r.getCountry() != null ? r.getCountry() : "Việt Nam");
                tvRecipeTime.setText(r.getCookTimeMinutes() + " phút");

                if (r.getImageUrl() != null && !r.getImageUrl().trim().isEmpty() && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(this, r.getImageUrl())) {
                    Glide.with(this)
                            .load(r.getImageUrl())
                            .placeholder(R.drawable.character_chef_1)
                            .error(R.drawable.character_chef_1)
                            .into(imgRecipe);
                } else if (com.example.cookup_app.utils.RecipeDataHelper.isValidDrawable(this, r.getImageResId())) {
                    imgRecipe.setImageResource(r.getImageResId());
                } else {
                    imgRecipe.setImageResource(R.drawable.character_chef_1);
                }

                itemCard.setOnClickListener(v -> {
                    Intent intent = new Intent(this, RecipeDetailActivity.class);
                    intent.putExtra("recipe", r);
                    startActivity(intent);
                });

                if (btnBookmark != null) {
                    if (isMyRecipes) {
                        btnBookmark.setVisibility(View.GONE);
                    } else {
                        btnBookmark.setVisibility(View.VISIBLE);
                        ImageView imgBookmark = itemCard.findViewById(R.id.imgBookmark);
                        if (imgBookmark != null) {
                            imgBookmark.setColorFilter(getResources().getColor(R.color.orange_primary));
                        }
                        // Make bookmark click unsave/remove favorite
                        btnBookmark.setOnClickListener(v -> {
                            com.example.cookup_app.utils.CollectionHelper.removeRecipeFromAllFavoritesAndCollections(this, r.getId(), () -> {
                                Toast.makeText(this, "Đã xóa khỏi danh sách yêu thích!", Toast.LENGTH_SHORT).show();
                                loadRecipesToGrid(isMyRecipes); // Reload
                            });
                        });
                    }
                }

                gridRecentlySaved.addView(itemCard);
            }
        }
    }

    private void showFollowersListDialog() {
        if (isFinishing()) return;

        final String queryUid = targetUserId != null ? targetUserId : (FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null);
        if (queryUid == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Người theo dõi");

        FirebaseFirestore.getInstance().collection("follows")
                .whereEqualTo("followedUid", queryUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (isFinishing()) return;

                    List<java.util.Map<String, String>> followers = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String fUid = doc.getString("followerUid");
                            String fName = doc.getString("followerName");
                            if (!android.text.TextUtils.isEmpty(fUid)) {
                                java.util.Map<String, String> fMap = new java.util.HashMap<>();
                                fMap.put("uid", fUid);
                                fMap.put("name", android.text.TextUtils.isEmpty(fName) ? "Người dùng CookUp" : fName);
                                followers.add(fMap);
                            }
                        }
                    }

                    if (followers.isEmpty()) {
                        builder.setMessage("Chưa có người theo dõi nào. 🍳");
                        builder.setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss());
                        builder.show();
                    } else {
                        // Let's build a scroll view containing followers list
                        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
                        android.widget.LinearLayout listContainer = new android.widget.LinearLayout(this);
                        listContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
                        listContainer.setPadding(32, 24, 32, 24);

                        final AlertDialog dialog = builder.setView(scrollView).create();
                        scrollView.addView(listContainer);

                        for (java.util.Map<String, String> follower : followers) {
                            String fUid = follower.get("uid");
                            String fName = follower.get("name");

                            // Create an item row
                            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
                            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                            row.setPadding(0, 16, 0, 16);
                            row.setClickable(true);
                            row.setFocusable(true);
                            row.setBackgroundResource(android.R.drawable.list_selector_background);

                            // Circular ImageView for Avatar
                            ImageView avatarView = new ImageView(this);
                            android.widget.LinearLayout.LayoutParams imgParams = new android.widget.LinearLayout.LayoutParams(96, 96);
                            imgParams.setMarginEnd(32);
                            avatarView.setLayoutParams(imgParams);
                            avatarView.setImageResource(R.drawable.avatar_default_unknown);

                            // TextView for Name
                            TextView nameView = new TextView(this);
                            nameView.setText(fName);
                            nameView.setTextColor(getResources().getColor(R.color.text_primary));
                            nameView.setTextSize(15);
                            nameView.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
                            nameView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

                            row.addView(avatarView);
                            row.addView(nameView);

                            // Load user avatar dynamically from Firestore
                            FirebaseFirestore.getInstance().collection("users")
                                    .document(fUid)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists() && !isFinishing()) {
                                            String url = userDoc.getString("avatarUrl");
                                            String name = userDoc.getString("displayName");
                                            if (android.text.TextUtils.isEmpty(name)) {
                                                name = userDoc.getString("name");
                                            }
                                            if (!android.text.TextUtils.isEmpty(name)) {
                                                nameView.setText(name);
                                            }
                                            if (!android.text.TextUtils.isEmpty(url) && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(this, url)) {
                                                Glide.with(this)
                                                        .load(url)
                                                        .circleCrop()
                                                        .placeholder(R.drawable.avatar_default_unknown)
                                                        .error(R.drawable.avatar_default_unknown)
                                                        .into(avatarView);
                                            }
                                        }
                                    });

                            // Row click listener to open their public profile
                            row.setOnClickListener(v -> {
                                dialog.dismiss();
                                Intent intent = new Intent(this, ProfileActivity.class);
                                intent.putExtra("userId", fUid);
                                startActivity(intent);
                            });

                            listContainer.addView(row);
                        }

                        dialog.show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing()) {
                        Toast.makeText(this, "Lỗi tải người theo dõi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkFollowStatus(MaterialButton btnProfileFollow, String currentUid, String targetUid) {
        if (btnProfileFollow == null || currentUid == null || targetUid == null) return;

        String followDocId = currentUid + "_" + targetUid;
        FirebaseFirestore.getInstance().collection("follows").document(followDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    final boolean isFollowing = documentSnapshot.exists();
                    updateProfileFollowButtonUI(btnProfileFollow, isFollowing);

                    btnProfileFollow.setOnClickListener(v -> {
                        btnProfileFollow.setEnabled(false);
                        if (isFollowing) {
                            // Unfollow
                            FirebaseFirestore.getInstance().collection("follows").document(followDocId).delete()
                                    .addOnSuccessListener(aVoid -> {
                                        FirebaseFirestore.getInstance().collection("users").document(targetUid)
                                                .update("followersCount", com.google.firebase.firestore.FieldValue.increment(-1));

                                        Toast.makeText(ProfileActivity.this, "Đã bỏ theo dõi đầu bếp", Toast.LENGTH_SHORT).show();
                                        btnProfileFollow.setEnabled(true);
                                        checkFollowStatus(btnProfileFollow, currentUid, targetUid);
                                        loadProfileData(); // Reload stats count
                                    })
                                    .addOnFailureListener(e -> btnProfileFollow.setEnabled(true));
                        } else {
                            // Follow
                            java.util.Map<String, Object> followData = new java.util.HashMap<>();
                            followData.put("followerUid", currentUid);
                            followData.put("followedUid", targetUid);

                            String tempFollowerName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                            if (android.text.TextUtils.isEmpty(tempFollowerName)) {
                                tempFollowerName = FirebaseAuth.getInstance().getCurrentUser().getEmail() != null ? FirebaseAuth.getInstance().getCurrentUser().getEmail().split("@")[0] : "Một đầu bếp";
                            }
                            final String followerName = tempFollowerName;
                            followData.put("followerName", followerName);

                            String chefName = tvProfileName.getText().toString();
                            followData.put("followedName", chefName);
                            followData.put("timestamp", System.currentTimeMillis());

                            FirebaseFirestore.getInstance().collection("follows").document(followDocId).set(followData)
                                    .addOnSuccessListener(aVoid -> {
                                        FirebaseFirestore.getInstance().collection("users").document(targetUid)
                                                .update("followersCount", com.google.firebase.firestore.FieldValue.increment(1))
                                                .addOnFailureListener(e -> {
                                                    java.util.Map<String, Object> initialData = new java.util.HashMap<>();
                                                    initialData.put("followersCount", 1);
                                                    initialData.put("name", chefName);
                                                    FirebaseFirestore.getInstance().collection("users").document(targetUid).set(initialData, com.google.firebase.firestore.SetOptions.merge());
                                                });

                                        // Send real notification
                                        java.util.Map<String, Object> notificationData = new java.util.HashMap<>();
                                        notificationData.put("title", "Người theo dõi mới");
                                        notificationData.put("body", followerName + " đã bắt đầu theo dõi gian bếp của bạn. 👥");
                                        notificationData.put("type", "follow");
                                        notificationData.put("timestamp", System.currentTimeMillis());
                                        notificationData.put("read", false);
                                        notificationData.put("recipientUid", targetUid);
                                        notificationData.put("senderUid", currentUid);

                                        FirebaseFirestore.getInstance().collection("notifications").add(notificationData);

                                        Toast.makeText(ProfileActivity.this, "Đã theo dõi " + chefName + "!", Toast.LENGTH_SHORT).show();
                                        btnProfileFollow.setEnabled(true);
                                        checkFollowStatus(btnProfileFollow, currentUid, targetUid);
                                        loadProfileData(); // Reload stats count
                                    })
                                    .addOnFailureListener(e -> btnProfileFollow.setEnabled(true));
                        }
                    });
                });
    }

    private void updateProfileFollowButtonUI(MaterialButton button, boolean isFollowing) {
        if (button == null) return;
        if (isFollowing) {
            button.setText("Đang theo dõi");
            button.setIcon(null);
            button.setTextColor(android.graphics.Color.parseColor("#FF7238"));
            button.setBackgroundColor(android.graphics.Color.parseColor("#160E0C"));
            button.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF7238")));
            button.setStrokeWidth(2);
        } else {
            button.setText("Theo dõi");
            button.setIconResource(R.drawable.ic_add);
            button.setTextColor(android.graphics.Color.WHITE);
            button.setBackgroundColor(android.graphics.Color.parseColor("#FF7238"));
            button.setStrokeWidth(0);
        }
    }

    private void loadProfileCollections() {
        if (containerProfileCollections == null) return;

        // Keep only the "Create Collection" card (the first child)
        int childCount = containerProfileCollections.getChildCount();
        if (childCount > 1) {
            containerProfileCollections.removeViews(1, childCount - 1);
        }

        final String queryUid = targetUserId != null ? targetUserId : (FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null);
        if (queryUid == null) return;

        FirebaseFirestore.getInstance().collection("collections")
                .whereEqualTo("creatorUid", queryUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (isFinishing()) return;
                    List<com.example.cookup_app.model.RecipeCollection> collectionsList = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            com.example.cookup_app.model.RecipeCollection col = doc.toObject(com.example.cookup_app.model.RecipeCollection.class);
                            if (col != null) {
                                if (col.getId() == null || col.getId().trim().isEmpty()) {
                                    col.setId(doc.getId());
                                }

                                // Only show collections that are public unless we are viewing our own profile!
                                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                boolean isSelf = (currentUser != null && currentUser.getUid().equals(queryUid));
                                if (isSelf || col.isPublic()) {
                                    collectionsList.add(col);
                                }
                            }
                        }
                    }

                    for (com.example.cookup_app.model.RecipeCollection col : collectionsList) {
                        addCollectionCardToUI(col);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing()) {
                        Toast.makeText(ProfileActivity.this, "Lỗi tải bộ sưu tập: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addCollectionCardToUI(com.example.cookup_app.model.RecipeCollection col) {
        if (containerProfileCollections == null) return;

        Context context = ProfileActivity.this;
        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(context);

        // Layout params for Card: 120dp x 120dp, marginEnd 12dp
        int sizePx = (int) (120 * context.getResources().getDisplayMetrics().density);
        int marginPx = (int) (12 * context.getResources().getDisplayMetrics().density);

        android.widget.LinearLayout.LayoutParams cardParams = new android.widget.LinearLayout.LayoutParams(sizePx, sizePx);
        cardParams.setMarginEnd(marginPx);
        card.setLayoutParams(cardParams);
        card.setRadius(16 * context.getResources().getDisplayMetrics().density);
        card.setCardElevation(0);
        card.setStrokeWidth(0);
        card.setClickable(true);
        card.setFocusable(true);

        android.widget.FrameLayout frame = new android.widget.FrameLayout(context);
        frame.setLayoutParams(new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView img = new ImageView(context);
        img.setLayoutParams(new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Pick a default drawable based on collection name hash
        int hash = Math.abs(col.getName().hashCode());
        int drawableRes = (hash % 2 == 0) ? R.drawable.character_chef_1 : R.drawable.character_chef_2;
        img.setImageResource(drawableRes);
        frame.addView(img);

        View overlay = new View(context);
        overlay.setLayoutParams(new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(android.graphics.Color.parseColor("#80000000")); // semi-transparent black overlay
        frame.addView(overlay);

        // Star/Pin icon top-left for decoration
        ImageView pinIcon = new ImageView(context);
        int iconSize = (int) (18 * context.getResources().getDisplayMetrics().density);
        int iconMargin = (int) (10 * context.getResources().getDisplayMetrics().density);
        android.widget.FrameLayout.LayoutParams pinParams = new android.widget.FrameLayout.LayoutParams(iconSize, iconSize);
        pinParams.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
        pinParams.setMargins(iconMargin, iconMargin, iconMargin, iconMargin);
        pinIcon.setLayoutParams(pinParams);
        pinIcon.setImageResource(R.drawable.ic_bookmark);
        pinIcon.setColorFilter(android.graphics.Color.WHITE);
        frame.addView(pinIcon);

        // Bottom text block
        android.widget.LinearLayout textContainer = new android.widget.LinearLayout(context);
        textContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.FrameLayout.LayoutParams textParams = new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.gravity = android.view.Gravity.BOTTOM;
        int paddingPx = (int) (10 * context.getResources().getDisplayMetrics().density);
        textContainer.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        textContainer.setLayoutParams(textParams);

        TextView tvName = new TextView(context);
        tvName.setText(col.getName());
        tvName.setTextColor(android.graphics.Color.WHITE);
        tvName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tvName.setSingleLine(true);
        textContainer.addView(tvName);

        TextView tvCount = new TextView(context);
        int count = col.getRecipeIds() != null ? col.getRecipeIds().size() : 0;
        tvCount.setText(count + " công thức");
        tvCount.setTextColor(android.graphics.Color.parseColor("#CCCCCC"));
        tvCount.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 9);
        textContainer.addView(tvCount);

        frame.addView(textContainer);
        card.addView(frame);

        card.setOnClickListener(v -> showCollectionRecipesDialog(col));

        containerProfileCollections.addView(card);
    }

    private void showCreateCollectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setTitle("Tạo bộ sưu tập mới");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(ProfileActivity.this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        final EditText edtName = new EditText(ProfileActivity.this);
        edtName.setHint("Tên bộ sưu tập (vd: Món chay, Ăn sáng...)");
        layout.addView(edtName);

        final android.widget.CheckBox cbPublic = new android.widget.CheckBox(ProfileActivity.this);
        cbPublic.setText("Công khai bộ sưu tập");
        cbPublic.setChecked(true);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) (10 * getResources().getDisplayMetrics().density);
        cbPublic.setLayoutParams(params);
        layout.addView(cbPublic);

        builder.setView(layout);

        builder.setPositiveButton("Tạo", (dialog, which) -> {
            String name = edtName.getText().toString().trim();
            if (android.text.TextUtils.isEmpty(name)) {
                Toast.makeText(ProfileActivity.this, "Vui lòng nhập tên bộ sưu tập!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) return;

            String docId = FirebaseFirestore.getInstance().collection("collections").document().getId();
            com.example.cookup_app.model.RecipeCollection newCol = new com.example.cookup_app.model.RecipeCollection(
                    docId, name, cbPublic.isChecked(), currentUser.getUid()
            );

            FirebaseFirestore.getInstance().collection("collections").document(docId).set(newCol)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ProfileActivity.this, "Đã tạo bộ sưu tập thành công!", Toast.LENGTH_SHORT).show();
                        loadProfileCollections();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProfileActivity.this, "Lỗi khi tạo bộ sưu tập: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showCollectionRecipesDialog(com.example.cookup_app.model.RecipeCollection col) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setTitle("Bộ sưu tập: " + col.getName());

        List<String> recipeIds = col.getRecipeIds();
        if (recipeIds == null || recipeIds.isEmpty()) {
            builder.setMessage("Chưa có công thức nào trong bộ sưu tập này. Hãy nhấn biểu tượng Bookmark trong chi tiết công thức để thêm vào nhé!");
            builder.setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss());
            builder.show();
            return;
        }

        // Fetch recipes from Firestore belonging to this collection
        FirebaseFirestore.getInstance().collection("recipes")
                .whereIn("id", getIntegerList(recipeIds))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (isFinishing()) return;
                    List<com.example.cookup_app.model.Recipe> recipes = queryDocumentSnapshots.toObjects(com.example.cookup_app.model.Recipe.class);

                    if (recipes.isEmpty()) {
                        Toast.makeText(ProfileActivity.this, "Không tìm thấy chi tiết món ăn nào!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] items = new String[recipes.size()];
                    for (int i = 0; i < recipes.size(); i++) {
                        items[i] = recipes.get(i).getName();
                    }

                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    boolean isSelf = (currentUser != null && currentUser.getUid().equals(col.getCreatorUid()));

                    builder.setItems(items, (dialog, which) -> {
                        com.example.cookup_app.model.Recipe selectedRecipe = recipes.get(which);

                        if (isSelf) {
                            // Action choices dialog for own profile
                            AlertDialog.Builder actionBuilder = new AlertDialog.Builder(ProfileActivity.this);
                            actionBuilder.setTitle(selectedRecipe.getName());

                            String[] options = {"Xem chi tiết công thức", "Xóa khỏi bộ sưu tập", "Hủy"};
                            actionBuilder.setItems(options, (actionDialog, actionWhich) -> {
                                if (actionWhich == 0) {
                                    // View details
                                    Intent intent = new Intent(ProfileActivity.this, com.example.cookup_app.activity.RecipeDetailActivity.class);
                                    intent.putExtra("recipe", selectedRecipe);
                                    startActivity(intent);
                                } else if (actionWhich == 1) {
                                    // Delete from collection confirmation
                                    AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(ProfileActivity.this);
                                    confirmBuilder.setTitle("Xác nhận xóa");
                                    confirmBuilder.setMessage("Bạn có chắc chắn muốn xóa \"" + selectedRecipe.getName() + "\" khỏi bộ sưu tập \"" + col.getName() + "\"?");
                                    confirmBuilder.setNegativeButton("Hủy", (d, w) -> d.dismiss());
                                    confirmBuilder.setPositiveButton("Xóa", (d, w) -> {
                                        List<String> rIds = col.getRecipeIds();
                                        if (rIds != null) {
                                            rIds.remove(String.valueOf(selectedRecipe.getId()));
                                            col.setRecipeIds(rIds);

                                            FirebaseFirestore.getInstance().collection("collections")
                                                    .document(col.getId())
                                                    .update("recipeIds", com.google.firebase.firestore.FieldValue.arrayRemove(String.valueOf(selectedRecipe.getId())))
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(ProfileActivity.this, "Đã xóa \"" + selectedRecipe.getName() + "\" khỏi bộ sưu tập!", Toast.LENGTH_SHORT).show();
                                                        loadProfileCollections(); // Reload list to update count
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(ProfileActivity.this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });
                                        }
                                    });
                                    confirmBuilder.show();
                                }
                            });
                            actionBuilder.show();
                        } else {
                            // Directly view details for other user's collection
                            Intent intent = new Intent(ProfileActivity.this, com.example.cookup_app.activity.RecipeDetailActivity.class);
                            intent.putExtra("recipe", selectedRecipe);
                            startActivity(intent);
                        }
                    });

                    builder.setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss());
                    builder.show();
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing()) {
                        Toast.makeText(ProfileActivity.this, "Lỗi tải công thức: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private List<Integer> getIntegerList(List<String> stringList) {
        List<Integer> list = new ArrayList<>();
        for (String s : stringList) {
            try {
                list.add(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        if (list.isEmpty()) {
            list.add(-1); // dummy to prevent empty whereIn query
        }
        return list;
    }
}
