package com.example.cookup_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

import com.example.cookup_app.fragment.FavoriteFragment;
import com.example.cookup_app.fragment.HomeFragment;
import com.example.cookup_app.fragment.PlanFragment;
import com.example.cookup_app.fragment.SearchFragment;
import com.example.cookup_app.utils.ThemeManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;
import com.example.cookup_app.activity.AddRecipeActivity;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private android.view.View fabUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Áp dụng Theme đồng bộ
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNav);
        fabUpload = findViewById(R.id.fabUpload);

        bottomNavigationView.setItemActiveIndicatorColor(ColorStateList.valueOf(Color.TRANSPARENT));

        if (savedInstanceState == null) {
            switchFragment(new HomeFragment());
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                switchFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_search) {
                switchFragment(new SearchFragment());
                return true;
            } else if (id == R.id.nav_favorite) {
                switchFragment(new FavoriteFragment());
                return true;
            } else if (id == R.id.nav_plan) {
                switchFragment(new PlanFragment());
                return true;
            }
            return false;
        });

        fabUpload.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddRecipeActivity.class);
            startActivity(intent);
        });

        // Đăng ký nút bong bóng Trợ lý AI có thể kéo thả di chuyển khắp màn hình
        FloatingActionButton fabAiChat = findViewById(R.id.fabAiChat);
        if (fabAiChat != null) {
            fabAiChat.setOnClickListener(v -> {
                com.example.cookup_app.dialog.AiChatBottomSheet bottomSheet = new com.example.cookup_app.dialog.AiChatBottomSheet();
                bottomSheet.show(getSupportFragmentManager(), "AiChatBottomSheet");
            });

            fabAiChat.setOnTouchListener(new View.OnTouchListener() {
                private float initialX, initialY;
                private float initialTouchX, initialTouchY;
                private boolean isMoving = false;
                private static final int CLICK_THRESHOLD = 15; // Khoảng cách pixel tối đa để nhận biết click thay vì kéo

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = v.getX();
                            initialY = v.getY();
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            isMoving = false;
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getRawX() - initialTouchX;
                            float dy = event.getRawY() - initialTouchY;

                            if (Math.abs(dx) > CLICK_THRESHOLD || Math.abs(dy) > CLICK_THRESHOLD) {
                                isMoving = true;
                            }

                            if (isMoving) {
                                v.setX(initialX + dx);
                                v.setY(initialY + dy);
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                            if (!isMoving) {
                                v.performClick();
                            }
                            return true;
                    }
                    return false;
                }
            });
        }

        ensureUserProfileAndRole();
        runRecipeMigration();
    }

    private void runRecipeMigration() {
        android.content.SharedPreferences prefs = getSharedPreferences("cookup_prefs", android.content.Context.MODE_PRIVATE);
        if (prefs.getBoolean("recipe_rating_migration_done_v3", false)) {
            return;
        }

        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        db.collection("recipes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        prefs.edit().putBoolean("recipe_rating_migration_done_v3", true).apply();
                        return;
                    }

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String recipeId = doc.getId();

                        db.collection("recipes").document(recipeId).collection("reviews")
                                .get()
                                .addOnSuccessListener(reviewsSnapshot -> {
                                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                                    if (reviewsSnapshot == null || reviewsSnapshot.isEmpty()) {
                                        // Reset to 0.0 if there are no reviews
                                        updates.put("rating", 0.0);
                                        updates.put("averageRating", 0.0);
                                        db.collection("recipes").document(recipeId)
                                                .update(updates)
                                                .addOnSuccessListener(aVoid -> {
                                                    android.util.Log.d("Migration", "Successfully updated rating to 0.0 for recipe " + recipeId);
                                                });
                                    } else {
                                        // Calculate actual average rating from reviews and update
                                        double sum = 0.0;
                                        int count = reviewsSnapshot.size();
                                        for (com.google.firebase.firestore.DocumentSnapshot rDoc : reviewsSnapshot.getDocuments()) {
                                            Double stars = rDoc.getDouble("rating");
                                            if (stars != null) {
                                                sum += stars;
                                            }
                                        }
                                        double avg = sum / count;
                                        avg = Math.round(avg * 10.0) / 10.0;

                                        updates.put("rating", avg);
                                        updates.put("averageRating", avg);
                                        db.collection("recipes").document(recipeId).update(updates);
                                    }
                                });
                    }
                    prefs.edit().putBoolean("recipe_rating_migration_done_v3", true).apply();
                });
    }

    private void ensureUserProfileAndRole() {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        String email = currentUser.getEmail();
        String displayName = currentUser.getDisplayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = email != null && email.contains("@") ? email.split("@")[0] : "Người dùng";
        }

        final String finalDisplayName = displayName;
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean needUpdate = false;
                    java.util.Map<String, Object> userData = new java.util.HashMap<>();

                    if (!documentSnapshot.exists()) {
                        needUpdate = true;
                        userData.put("uid", uid);
                        userData.put("email", email);
                        userData.put("name", finalDisplayName);
                        userData.put("displayName", finalDisplayName);
                        userData.put("role", "user");
                    } else {
                        // Document exists, verify if role or other fields are correct
                        String role = documentSnapshot.getString("role");
                        if (role == null || role.trim().isEmpty()) {
                            needUpdate = true;
                            userData.put("role", "user");
                        }
                    }

                    if (needUpdate) {
                        db.collection("users").document(uid)
                                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener(aVoid -> android.util.Log.d("UserHealer", "User profile/role successfully ensured in Firestore"))
                                .addOnFailureListener(e -> android.util.Log.e("UserHealer", "Failed to write user profile/role in Firestore", e));
                    }
                })
                .addOnFailureListener(e -> android.util.Log.e("UserHealer", "Error fetching user profile", e));
    }

    public void navigateToSearchTab() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_search);
        }
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}