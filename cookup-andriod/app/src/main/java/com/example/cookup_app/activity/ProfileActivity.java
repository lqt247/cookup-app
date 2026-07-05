package com.example.cookup_app.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cookup_app.R;
import com.example.cookup_app.utils.ThemeManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "cookup_prefs";
    private static final String KEY_GENDER = "gender";
    private static final String KEY_USER_NAME = "custom_user_name";
    private static final String KEY_USER_BIO = "custom_user_bio";

    private ImageView imgProfileAvatar;
    private TextView tvProfileName;
    private TextView tvProfileBio;
    private TextView tvStatRecipesCount;
    private TextView tvStatFavoritesCount;

    private TextView tabMyRecipes;
    private TextView tabLikedRecipes;
    private View btnEditProfile;
    private MaterialButton btnLogout;

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
        tvStatFavoritesCount = findViewById(R.id.tvStatFavoritesCount);

        tabMyRecipes = findViewById(R.id.tabMyRecipes);
        tabLikedRecipes = findViewById(R.id.tabLikedRecipes);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);

        View btnBack = findViewById(R.id.btnProfileBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Apply profile content
        loadProfileData();

        // Register edit profile listener
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        // Segmented Tabs switching
        tabMyRecipes.setOnClickListener(v -> selectTab(true));
        tabLikedRecipes.setOnClickListener(v -> selectTab(false));

        // Settings click
        View btnSettings = findViewById(R.id.btnProfileSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> Toast.makeText(this, "Tính năng cài đặt tài khoản đang phát triển", Toast.LENGTH_SHORT).show());
        }

        // Admin Dashboard Click & Role Checking
        View btnAdminDashboard = findViewById(R.id.btnAdminDashboard);
        if (btnAdminDashboard != null) {
            btnAdminDashboard.setVisibility(View.GONE); // Default hide

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                // Auto-unlock for the user's email for seamless testing
                if ("lequangtruong2472005@gmail.com".equalsIgnoreCase(currentUser.getEmail())) {
                    btnAdminDashboard.setVisibility(View.VISIBLE);
                } else {
                    // Check role in Firestore users collection
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
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

        // Logout
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().remove("token").apply();

            Intent intent = new Intent(this, SplashActivity.class);
            startActivity(intent);
            finishAffinity();
        });
    }

    private void loadProfileData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 1. Load Name & Bio
        String savedName = prefs.getString(KEY_USER_NAME, "");
        String savedBio = prefs.getString(KEY_USER_BIO, "");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            if (TextUtils.isEmpty(savedName)) {
                savedName = user.getDisplayName();
            }
            if (TextUtils.isEmpty(savedName)) {
                savedName = user.getEmail();
                if (savedName != null && savedName.contains("@")) {
                    savedName = savedName.split("@")[0];
                }
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

        // 2. Load Gender Avatar
        String gender = prefs.getString(KEY_GENDER, "unknown");
        if ("male".equalsIgnoreCase(gender)) {
            imgProfileAvatar.setImageResource(R.drawable.avatar_default_male);
        } else if ("female".equalsIgnoreCase(gender)) {
            imgProfileAvatar.setImageResource(R.drawable.avatar_default_female);
        } else {
            imgProfileAvatar.setImageResource(R.drawable.avatar_default_unknown);
        }
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder customBuilder = new AlertDialog.Builder(this);
        customBuilder.setTitle("Chỉnh sửa trang cá nhân");

        ViewGroup container = new android.widget.LinearLayout(this);
        container.setPadding(48, 32, 48, 32);
        ((android.widget.LinearLayout)container).setOrientation(android.widget.LinearLayout.VERTICAL);

        final EditText inputName = new EditText(this);
        inputName.setHint("Nhập họ và tên");
        inputName.setText(tvProfileName.getText());
        container.addView(inputName);

        final EditText inputBio = new EditText(this);
        inputBio.setHint("Nhập giới thiệu ngắn");
        inputBio.setText(tvProfileBio.getText());
        inputBio.setSingleLine(false);
        inputBio.setMinLines(2);

        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = 24;
        inputBio.setLayoutParams(params);
        container.addView(inputBio);

        customBuilder.setView(container);

        customBuilder.setPositiveButton("Lưu", (dialog, which) -> {
            String newName = inputName.getText().toString().trim();
            String newBio = inputBio.getText().toString().trim();

            if (!TextUtils.isEmpty(newName)) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit()
                     .putString(KEY_USER_NAME, newName)
                     .putString(KEY_USER_BIO, newBio)
                     .apply();

                tvProfileName.setText(newName);
                tvProfileBio.setText(newBio);
                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
            }
        });
        customBuilder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        customBuilder.show();
    }

    private void selectTab(boolean isMyRecipes) {
        if (isMyRecipes) {
            tabMyRecipes.setBackgroundResource(R.drawable.bg_segment_active);
            tabMyRecipes.setTextColor(getResources().getColor(R.color.text_primary));

            tabLikedRecipes.setBackgroundResource(android.R.color.transparent);
            tabLikedRecipes.setTextColor(getResources().getColor(R.color.text_secondary));
            Toast.makeText(this, "Hiển thị công thức của tôi", Toast.LENGTH_SHORT).show();
        } else {
            tabLikedRecipes.setBackgroundResource(R.drawable.bg_segment_active);
            tabLikedRecipes.setTextColor(getResources().getColor(R.color.text_primary));

            tabMyRecipes.setBackgroundResource(android.R.color.transparent);
            tabMyRecipes.setTextColor(getResources().getColor(R.color.text_secondary));
            Toast.makeText(this, "Hiển thị các món yêu thích", Toast.LENGTH_SHORT).show();
        }
    }
}
