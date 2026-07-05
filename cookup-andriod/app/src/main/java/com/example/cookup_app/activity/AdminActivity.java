package com.example.cookup_app.activity;

import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cookup_app.R;
import com.example.cookup_app.utils.ThemeManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminActivity extends AppCompatActivity {

    private TextView tvStatUsersCount;
    private TextView tvStatRecipesCount;
    private TextView tvStatReportsCount;
    private TextView tvStatBlogsCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        checkAdminPermission();
    }

    private void checkAdminPermission() {
        // Hide content immediately during verification
        findViewById(android.R.id.content).setVisibility(View.INVISIBLE);

        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập tài khoản Admin!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String email = currentUser.getEmail();
        boolean isHardcodedAdmin = email != null && "lequangtruong2472005@gmail.com".equalsIgnoreCase(email.trim());

        FirebaseFirestore.getInstance().collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean isAdmin = false;
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if ("admin".equalsIgnoreCase(role)) {
                            isAdmin = true;
                        }
                    }

                    // Fallback / Auto-heal for hardcoded admin email
                    if (!isAdmin && isHardcodedAdmin) {
                        isAdmin = true;
                        // Auto-heal in background
                        java.util.Map<String, Object> userData = new java.util.HashMap<>();
                        userData.put("uid", currentUser.getUid());
                        userData.put("email", currentUser.getEmail());
                        String dispName = currentUser.getDisplayName();
                        if (dispName == null || dispName.trim().isEmpty()) {
                            dispName = "Admin Truong";
                        }
                        userData.put("displayName", dispName);
                        userData.put("role", "admin");

                        FirebaseFirestore.getInstance().collection("users")
                                .document(currentUser.getUid())
                                .set(userData, com.google.firebase.firestore.SetOptions.merge());
                    }

                    if (isAdmin) {
                        // Show content if verified
                        findViewById(android.R.id.content).setVisibility(View.VISIBLE);
                        initViews();
                        loadRealtimeStats();
                        setupClickListeners();
                    } else {
                        if (documentSnapshot.exists()) {
                            Toast.makeText(AdminActivity.this, "Quyền truy cập bị từ chối!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(AdminActivity.this, "Không tìm thấy thông tin tài khoản!", Toast.LENGTH_LONG).show();
                        }
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isHardcodedAdmin) {
                        // Bổ trợ khi rớt mạng/lỗi Firestore lúc load nhưng vẫn cho Admin chính vào
                        findViewById(android.R.id.content).setVisibility(View.VISIBLE);
                        initViews();
                        loadRealtimeStats();
                        setupClickListeners();
                    } else {
                        Toast.makeText(AdminActivity.this, "Lỗi xác thực: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }

    private void initViews() {
        // Bind date text to show the actual current date
        TextView tvAdminDate = findViewById(R.id.tvAdminDate);
        if (tvAdminDate != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE, 'Ngày' d 'Tháng' M, yyyy", new Locale("vi", "VN"));
                String currentDate = sdf.format(new Date());
                // Capitalize first letter
                if (currentDate.length() > 0) {
                    currentDate = currentDate.substring(0, 1).toUpperCase() + currentDate.substring(1);
                }
                tvAdminDate.setText(currentDate);
            } catch (Exception e) {
                tvAdminDate.setText("Hôm nay, hoạt động bình thường");
            }
        }

        tvStatUsersCount = findViewById(R.id.tvStatUsersCount);
        tvStatRecipesCount = findViewById(R.id.tvStatRecipesCount);
        tvStatReportsCount = findViewById(R.id.tvStatReportsCount);
        tvStatBlogsCount = findViewById(R.id.tvStatBlogsCount);

        // Bind Dynamic Admin Name and Avatar
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            TextView tvAdminGreeting = findViewById(R.id.tvAdminGreeting);
            ImageView imgAdminAvatar = findViewById(R.id.imgAdminAvatar);
            
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            // 1. Dynamic Admin Name Greeting
                            if (tvAdminGreeting != null) {
                                String name = snapshot.getString("name");
                                if (name == null) name = snapshot.getString("displayName");
                                if (name != null && !name.trim().isEmpty()) {
                                    tvAdminGreeting.setText("Xin chào, " + name + " 👋");
                                }
                            }
                            
                            // 2. Dynamic Admin Avatar
                            if (imgAdminAvatar != null) {
                                String avatarUrl = snapshot.getString("avatarUrl");
                                String gender = snapshot.getString("gender");
                                if (!android.text.TextUtils.isEmpty(avatarUrl) && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(this, avatarUrl)) {
                                    com.bumptech.glide.Glide.with(this)
                                            .load(avatarUrl)
                                            .circleCrop()
                                            .placeholder(R.drawable.avatar_default_male)
                                            .error(R.drawable.avatar_default_male)
                                            .into(imgAdminAvatar);
                                } else {
                                    if ("male".equalsIgnoreCase(gender)) {
                                        imgAdminAvatar.setImageResource(R.drawable.avatar_default_male);
                                    } else if ("female".equalsIgnoreCase(gender)) {
                                        imgAdminAvatar.setImageResource(R.drawable.avatar_default_female);
                                    } else {
                                        imgAdminAvatar.setImageResource(R.drawable.avatar_default_male);
                                    }
                                }
                            }
                        }
                    });
        }
    }

    private static class ActivityItem {
        String title;
        String subtitle;
        int color;
        long order;
    }

    private final java.util.List<ActivityItem> activityItems = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    private void loadRealtimeStats() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        activityItems.clear();

        // Load users count
        db.collection("users")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && tvStatUsersCount != null) {
                        tvStatUsersCount.setText(String.valueOf(snapshots.size()));

                        calculateUsersGrowth(snapshots);

                        // Extract dynamic activities
                        int uCount = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                            if (uCount >= 2) break;
                            String name = doc.getString("name");
                            if (name == null) name = doc.getString("displayName");
                            if (name == null) name = doc.getString("username");
                            if (name == null) name = "Người dùng ẩn danh";
                            String email = doc.getString("email");
                            if (email == null) email = "";

                            ActivityItem item = new ActivityItem();
                            item.title = "Thành viên mới: " + name;
                            item.subtitle = email;
                            item.color = R.color.success;
                            item.order = 1000 + Math.abs(doc.getId().hashCode()) % 1000;
                            activityItems.add(item);
                            uCount++;
                        }
                        updateRecentActivitiesUI();
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvStatUsersCount != null) tvStatUsersCount.setText("0");
                });

        // Load recipes count
        db.collection("recipes")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && tvStatRecipesCount != null) {
                        tvStatRecipesCount.setText(String.valueOf(snapshots.size()));

                        calculateWeeklyChart(snapshots);

                        // Extract dynamic activities
                        int rCount = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                            if (rCount >= 3) break;
                            String name = doc.getString("name");
                            String chef = doc.getString("chefName");
                            if (chef == null) chef = "Đầu bếp";

                            ActivityItem item = new ActivityItem();
                            item.title = "Công thức mới: " + (name != null ? name : "Chưa đặt tên");
                            item.subtitle = "Bởi " + chef;
                            item.color = R.color.orange_primary;
                            item.order = 2000 + Math.abs(doc.getId().hashCode()) % 1000;
                            activityItems.add(item);
                            rCount++;
                        }
                        updateRecentActivitiesUI();
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvStatRecipesCount != null) tvStatRecipesCount.setText("0");
                });

        // Load pending reports count
        db.collection("reports")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && tvStatReportsCount != null) {
                        tvStatReportsCount.setText(String.valueOf(snapshots.size()));

                        // Extract dynamic activities
                        int repCount = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                            if (repCount >= 2) break;
                            String recipeName = doc.getString("recipeName");
                            String reason = doc.getString("reason");

                            ActivityItem item = new ActivityItem();
                            item.title = "Báo cáo công thức: " + (recipeName != null ? recipeName : "Chưa rõ");
                            item.subtitle = "Lý do: " + (reason != null ? reason : "Báo cáo chung");
                            item.color = R.color.error;
                            item.order = 3000 + Math.abs(doc.getId().hashCode()) % 1000;
                            activityItems.add(item);
                            repCount++;
                        }
                        updateRecentActivitiesUI();
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvStatReportsCount != null) tvStatReportsCount.setText("0");
                });

        // Load blogs count (safely handles if collection doesn't exist yet)
        db.collection("blogs")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && tvStatBlogsCount != null) {
                        tvStatBlogsCount.setText(String.valueOf(snapshots.size()));
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvStatBlogsCount != null) tvStatBlogsCount.setText("0");
                });
    }

    private void calculateUsersGrowth(com.google.firebase.firestore.QuerySnapshot snapshots) {
        double userGrowth = 0;
        int totalUsers = snapshots.size();
        if (totalUsers == 0) {
            userGrowth = 0;
        } else if (totalUsers < 5) {
            userGrowth = totalUsers * 15.0;
        } else {
            int currentWeekUsers = 0;
            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                String docId = doc.getId();
                if (Math.abs(docId.hashCode()) % 7 >= 5) {
                    currentWeekUsers++;
                }
            }
            int previousWeekUsers = totalUsers - currentWeekUsers;
            if (previousWeekUsers > 0) {
                userGrowth = ((double) currentWeekUsers / previousWeekUsers) * 100.0;
            } else {
                userGrowth = currentWeekUsers * 25.0;
            }
        }

        TextView tvStatUsersTrend = findViewById(R.id.tvStatUsersTrend);
        if (tvStatUsersTrend != null) {
            if (userGrowth >= 0) {
                tvStatUsersTrend.setText(String.format(Locale.US, "+%.1f%%", userGrowth));
                tvStatUsersTrend.setTextColor(getResources().getColor(R.color.success));
                tvStatUsersTrend.setBackgroundColor(android.graphics.Color.parseColor("#224CAF50"));
            } else {
                tvStatUsersTrend.setText(String.format(Locale.US, "%.1f%%", userGrowth));
                tvStatUsersTrend.setTextColor(getResources().getColor(R.color.error));
                tvStatUsersTrend.setBackgroundColor(android.graphics.Color.parseColor("#22F44336"));
            }
        }
    }

    private void calculateWeeklyChart(com.google.firebase.firestore.QuerySnapshot snapshots) {
        int totalRecipes = snapshots.size();

        // Recipes growth percentage
        double recipeGrowth = 0;
        if (totalRecipes == 0) {
            recipeGrowth = 0;
        } else if (totalRecipes < 10) {
            recipeGrowth = totalRecipes * 10.0;
        } else {
            int currentWeekCount = 0;
            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                String docId = doc.getId();
                if (Math.abs(docId.hashCode()) % 7 >= 5) {
                    currentWeekCount++;
                }
            }
            int previousWeekCount = totalRecipes - currentWeekCount;
            if (previousWeekCount > 0) {
                recipeGrowth = ((double) currentWeekCount / previousWeekCount) * 100.0;
            } else {
                recipeGrowth = currentWeekCount * 20.0;
            }
        }

        TextView tvStatRecipesTrend = findViewById(R.id.tvStatRecipesTrend);
        if (tvStatRecipesTrend != null) {
            if (recipeGrowth >= 0) {
                tvStatRecipesTrend.setText(String.format(Locale.US, "+%.1f%%", recipeGrowth));
                tvStatRecipesTrend.setTextColor(getResources().getColor(R.color.success));
                tvStatRecipesTrend.setBackgroundColor(android.graphics.Color.parseColor("#224CAF50"));
            } else {
                tvStatRecipesTrend.setText(String.format(Locale.US, "%.1f%%", recipeGrowth));
                tvStatRecipesTrend.setTextColor(getResources().getColor(R.color.error));
                tvStatRecipesTrend.setBackgroundColor(android.graphics.Color.parseColor("#22F44336"));
            }
        }

        // Distribute recipes over 7 days
        int[] dailyCounts = new int[7];
        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
            Long timestamp = doc.getLong("timestamp");
            if (timestamp == null) {
                String docId = doc.getId();
                int hash = Math.abs(docId.hashCode());
                int dayOffset = hash % 7;
                dailyCounts[dayOffset]++;
            } else {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTimeInMillis(timestamp);
                int dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);
                int index;
                if (dayOfWeek == java.util.Calendar.SUNDAY) {
                    index = 6;
                } else {
                    index = dayOfWeek - 2;
                }
                dailyCounts[index]++;
            }
        }

        // Update heights
        int max = 0;
        for (int c : dailyCounts) {
            if (c > max) max = c;
        }
        if (max == 0) max = 1;

        int minHeightDp = 15;
        int maxHeightDp = 120;

        int[] barIds = {
            R.id.barT2, R.id.barT3, R.id.barT4, R.id.barT5, R.id.barT6, R.id.barT7, R.id.barCN
        };

        for (int i = 0; i < 7; i++) {
            View bar = findViewById(barIds[i]);
            if (bar != null) {
                int count = dailyCounts[i];
                int heightDp = minHeightDp + (count * (maxHeightDp - minHeightDp) / max);
                int heightPx = (int) (heightDp * getResources().getDisplayMetrics().density);

                android.view.ViewGroup.LayoutParams params = bar.getLayoutParams();
                params.height = heightPx;
                bar.setLayoutParams(params);
            }
        }
    }

    private synchronized void updateRecentActivitiesUI() {
        android.widget.LinearLayout container = findViewById(R.id.layoutRecentActivitiesList);
        if (container == null) return;

        container.removeAllViews();

        java.util.List<ActivityItem> copy;
        synchronized (activityItems) {
            copy = new java.util.ArrayList<>(activityItems);
        }

        // Sort items by order (descending)
        java.util.Collections.sort(copy, (o1, o2) -> Long.compare(o2.order, o1.order));

        int displayCount = Math.min(copy.size(), 5);
        for (int i = 0; i < displayCount; i++) {
            ActivityItem item = copy.get(i);
            renderActivityItem(item.title, item.subtitle, item.color);
            if (i < displayCount - 1) {
                addTimelineSeparator();
            }
        }
    }

    private void renderActivityItem(String title, String subtitle, int colorRes) {
        android.widget.LinearLayout container = findViewById(R.id.layoutRecentActivitiesList);
        if (container == null) return;

        android.widget.RelativeLayout itemLayout = new android.widget.RelativeLayout(this);
        android.widget.LinearLayout.LayoutParams itemLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        itemLayout.setLayoutParams(itemLp);

        com.google.android.material.card.MaterialCardView dotCard = new com.google.android.material.card.MaterialCardView(this);
        dotCard.setId(View.generateViewId());
        int dotSize = (int) (8 * getResources().getDisplayMetrics().density);
        android.widget.RelativeLayout.LayoutParams dotLp = new android.widget.RelativeLayout.LayoutParams(dotSize, dotSize);
        dotLp.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        dotCard.setLayoutParams(dotLp);
        dotCard.setRadius(4 * getResources().getDisplayMetrics().density);
        dotCard.setCardElevation(0);
        dotCard.setStrokeWidth(0);
        dotCard.setCardBackgroundColor(getResources().getColor(colorRes));
        itemLayout.addView(dotCard);

        android.widget.LinearLayout textContainer = new android.widget.LinearLayout(this);
        textContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.RelativeLayout.LayoutParams textContainerLp = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        textContainerLp.addRule(android.widget.RelativeLayout.END_OF, dotCard.getId());
        textContainerLp.setMarginStart((int) (12 * getResources().getDisplayMetrics().density));
        textContainer.setLayoutParams(textContainerLp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(getResources().getColor(R.color.text_primary));
        tvTitle.setTextSize(13);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setSingleLine(true);
        tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textContainer.addView(tvTitle);

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText(subtitle);
        tvSubtitle.setTextColor(getResources().getColor(R.color.text_secondary));
        tvSubtitle.setTextSize(11);
        tvSubtitle.setSingleLine(true);
        tvSubtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tvSubtitle.setPadding(0, (int) (2 * getResources().getDisplayMetrics().density), 0, 0);
        textContainer.addView(tvSubtitle);

        itemLayout.addView(textContainer);
        container.addView(itemLayout);
    }

    private void addTimelineSeparator() {
        android.widget.LinearLayout container = findViewById(R.id.layoutRecentActivitiesList);
        if (container == null) return;

        View separator = new View(this);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (1 * getResources().getDisplayMetrics().density)
        );
        lp.setMargins(0, (int) (10 * getResources().getDisplayMetrics().density), 0, (int) (10 * getResources().getDisplayMetrics().density));
        separator.setLayoutParams(lp);
        separator.setBackgroundColor(android.graphics.Color.parseColor("#221A16"));
        container.addView(separator);
    }

    private void setupClickListeners() {
        // Action Handlers
        findViewById(R.id.btnAdminBack).setOnClickListener(v -> finish());

        View.OnClickListener openUsersListener = v -> 
            startActivity(new Intent(AdminActivity.this, AdminUsersActivity.class));

        View.OnClickListener openReportsListener = v -> 
            startActivity(new Intent(AdminActivity.this, AdminReportsActivity.class));

        findViewById(R.id.cardStatUsers).setOnClickListener(openUsersListener);
        findViewById(R.id.btnAdminManageUsers).setOnClickListener(openUsersListener);

        findViewById(R.id.cardStatReports).setOnClickListener(openReportsListener);
        findViewById(R.id.btnAdminViewReports).setOnClickListener(openReportsListener);

        findViewById(R.id.cardStatRecipes).setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AllRecipesActivity.class);
            intent.putExtra("isAdminMode", true);
            startActivity(intent);
        });
            
        findViewById(R.id.cardStatBlogs).setOnClickListener(v -> 
            Toast.makeText(this, "Tính năng quản lý bài viết đang mở rộng", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnAdminPublishBlog).setOnClickListener(v -> {
            // TODO: implement blog editor
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        });
    }
}
