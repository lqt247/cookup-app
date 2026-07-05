package com.example.cookup_app.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cookup_app.R;
import com.example.cookup_app.utils.ThemeManager;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminReportsActivity extends AppCompatActivity {

    private TextView tvRecipeBadgeCount;
    private ProgressBar pbReportsProgress;
    private View layoutReportsEmpty;
    private RecyclerView rvAdminReports;

    private List<DocumentSnapshot> reportsList = new ArrayList<>();
    private ReportsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reports);

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

                    // Fallback / Auto-heal
                    if (!isAdmin && isHardcodedAdmin) {
                        isAdmin = true;
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
                        setupRecyclerView();
                        fetchPendingReports();
                    } else {
                        if (documentSnapshot.exists()) {
                            Toast.makeText(AdminReportsActivity.this, "Quyền truy cập bị từ chối!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(AdminReportsActivity.this, "Không tìm thấy thông tin tài khoản!", Toast.LENGTH_LONG).show();
                        }
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isHardcodedAdmin) {
                        findViewById(android.R.id.content).setVisibility(View.VISIBLE);
                        initViews();
                        setupRecyclerView();
                        fetchPendingReports();
                    } else {
                        Toast.makeText(AdminReportsActivity.this, "Lỗi xác thực: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }

    private View tabRecipes, tabComments;
    private TextView tvTabRecipesText, tvTabCommentsText;
    private View viewTabRecipesIndicator, viewTabCommentsIndicator;
    private TextView tvCommentBadgeCount;
    private boolean isRecipeTab = true;

    private void initViews() {
        findViewById(R.id.btnReportsBack).setOnClickListener(v -> finish());
        tvRecipeBadgeCount = findViewById(R.id.tvRecipeBadgeCount);
        pbReportsProgress = findViewById(R.id.pbReportsProgress);
        layoutReportsEmpty = findViewById(R.id.layoutReportsEmpty);
        rvAdminReports = findViewById(R.id.rvAdminReports);

        tabRecipes = findViewById(R.id.tabRecipes);
        tabComments = findViewById(R.id.tabComments);
        tvTabRecipesText = findViewById(R.id.tvTabRecipesText);
        tvTabCommentsText = findViewById(R.id.tvTabCommentsText);
        viewTabRecipesIndicator = findViewById(R.id.viewTabRecipesIndicator);
        viewTabCommentsIndicator = findViewById(R.id.viewTabCommentsIndicator);
        tvCommentBadgeCount = findViewById(R.id.tvCommentBadgeCount);

        tabRecipes.setOnClickListener(v -> switchTab(true));
        tabComments.setOnClickListener(v -> switchTab(false));
    }

    private void setupRecyclerView() {
        rvAdminReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportsAdapter();
        rvAdminReports.setAdapter(adapter);
    }

    private void switchTab(boolean isRecipe) {
        if (isRecipeTab == isRecipe) return;
        isRecipeTab = isRecipe;

        // Update Tab UI
        if (isRecipeTab) {
            tvTabRecipesText.setTextColor(getResources().getColor(R.color.text_primary));
            tvTabRecipesText.setTypeface(null, android.graphics.Typeface.BOLD);
            viewTabRecipesIndicator.setBackgroundColor(getResources().getColor(R.color.orange_primary));
            ViewGroup.LayoutParams lp1 = viewTabRecipesIndicator.getLayoutParams();
            lp1.height = (int) (3 * getResources().getDisplayMetrics().density);
            viewTabRecipesIndicator.setLayoutParams(lp1);

            tvTabCommentsText.setTextColor(getResources().getColor(R.color.text_secondary));
            tvTabCommentsText.setTypeface(null, android.graphics.Typeface.NORMAL);
            viewTabCommentsIndicator.setBackgroundColor(getResources().getColor(R.color.bg_elevated));
            ViewGroup.LayoutParams lp2 = viewTabCommentsIndicator.getLayoutParams();
            lp2.height = (int) (1 * getResources().getDisplayMetrics().density);
            viewTabCommentsIndicator.setLayoutParams(lp2);
        } else {
            tvTabCommentsText.setTextColor(getResources().getColor(R.color.text_primary));
            tvTabCommentsText.setTypeface(null, android.graphics.Typeface.BOLD);
            viewTabCommentsIndicator.setBackgroundColor(getResources().getColor(R.color.orange_primary));
            ViewGroup.LayoutParams lp2 = viewTabCommentsIndicator.getLayoutParams();
            lp2.height = (int) (3 * getResources().getDisplayMetrics().density);
            viewTabCommentsIndicator.setLayoutParams(lp2);

            tvTabRecipesText.setTextColor(getResources().getColor(R.color.text_secondary));
            tvTabRecipesText.setTypeface(null, android.graphics.Typeface.NORMAL);
            viewTabRecipesIndicator.setBackgroundColor(getResources().getColor(R.color.bg_elevated));
            ViewGroup.LayoutParams lp1 = viewTabRecipesIndicator.getLayoutParams();
            lp1.height = (int) (1 * getResources().getDisplayMetrics().density);
            viewTabRecipesIndicator.setLayoutParams(lp1);
        }

        fetchPendingReports();
    }

    private void fetchCounts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("reports")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && tvRecipeBadgeCount != null) {
                        tvRecipeBadgeCount.setText(String.valueOf(snapshots.size()));
                    }
                });

        db.collection("commentReports")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && tvCommentBadgeCount != null) {
                        tvCommentBadgeCount.setText(String.valueOf(snapshots.size()));
                    }
                });
    }

    private void fetchPendingReports() {
        pbReportsProgress.setVisibility(View.VISIBLE);
        rvAdminReports.setVisibility(View.GONE);
        layoutReportsEmpty.setVisibility(View.GONE);

        String collectionName = isRecipeTab ? "reports" : "commentReports";

        FirebaseFirestore.getInstance().collection(collectionName)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pbReportsProgress.setVisibility(View.GONE);
                    reportsList.clear();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        reportsList.addAll(queryDocumentSnapshots.getDocuments());
                    }

                    int count = reportsList.size();
                    if (isRecipeTab) {
                        tvRecipeBadgeCount.setText(String.valueOf(count));
                    } else {
                        tvCommentBadgeCount.setText(String.valueOf(count));
                    }

                    if (count == 0) {
                        layoutReportsEmpty.setVisibility(View.VISIBLE);
                        rvAdminReports.setVisibility(View.GONE);
                    } else {
                        layoutReportsEmpty.setVisibility(View.GONE);
                        rvAdminReports.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }

                    fetchCounts(); // update both badge counts
                })
                .addOnFailureListener(e -> {
                    pbReportsProgress.setVisibility(View.GONE);
                    layoutReportsEmpty.setVisibility(View.VISIBLE);
                    rvAdminReports.setVisibility(View.GONE);
                    Toast.makeText(AdminReportsActivity.this, "Lỗi khi lấy báo cáo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ReportViewHolder> {

        @NonNull
        @Override
        public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_report_card, parent, false);
            return new ReportViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
            DocumentSnapshot doc = reportsList.get(position);
            Map<String, Object> report = doc.getData();
            if (report == null) return;

            String reportId = doc.getId();

            if (isRecipeTab) {
                // Recipe report mode
                holder.cardImage.setVisibility(View.VISIBLE);

                Object recipeIdObj = report.get("recipeId");
                String recipeId = recipeIdObj != null ? recipeIdObj.toString() : "";
                String recipeName = (String) report.get("recipeName");
                String recipeImageUrl = (String) report.get("recipeImageUrl");
                Object recipeImageResIdObj = report.get("recipeImageResId");
                int recipeImageResId = recipeImageResIdObj instanceof Number ? ((Number) recipeImageResIdObj).intValue() : 0;
                String recipeAuthor = (String) report.get("recipeAuthor");
                String reporterEmail = (String) report.get("reporterEmail");
                String reason = (String) report.get("reason");
                String details = (String) report.get("details");

                holder.tvRecipeName.setText(recipeName != null ? recipeName : "Công thức không xác định");
                holder.tvRecipeAuthor.setText("Bởi: " + (recipeAuthor != null ? recipeAuthor : "Ẩn danh"));
                holder.chipReason.setText(reason != null ? reason : "Báo cáo chung");

                if (TextUtils.isEmpty(details)) {
                    holder.layoutDetails.setVisibility(View.GONE);
                } else {
                    holder.layoutDetails.setVisibility(View.VISIBLE);
                    holder.tvReporterEmail.setText("Người báo cáo: " + (reporterEmail != null ? reporterEmail : "Ẩn danh"));
                    holder.tvDetailComment.setText("Chi tiết: " + details);
                }

                // Load Recipe Image safely
                if (recipeImageUrl != null && !recipeImageUrl.trim().isEmpty() && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(AdminReportsActivity.this, recipeImageUrl)) {
                    Glide.with(AdminReportsActivity.this)
                            .load(recipeImageUrl)
                            .placeholder(R.drawable.character_chef_1)
                            .error(R.drawable.character_chef_1)
                            .into(holder.imgRecipe);
                } else if (com.example.cookup_app.utils.RecipeDataHelper.isValidDrawable(AdminReportsActivity.this, recipeImageResId)) {
                    holder.imgRecipe.setImageResource(recipeImageResId);
                } else {
                    holder.imgRecipe.setImageResource(R.drawable.character_chef_1);
                }

                holder.btnDeleteRecipe.setText("Xoá công thức");
                // Delete action click listener
                holder.btnDeleteRecipe.setOnClickListener(v -> {
                    new AlertDialog.Builder(AdminReportsActivity.this, R.style.BottomSheetTheme)
                            .setTitle("Xác nhận xoá công thức")
                            .setMessage("Bạn có chắc chắn muốn xoá vĩnh viễn công thức \"" + recipeName + "\" khỏi hệ thống không?")
                            .setPositiveButton("Xoá", (dialog, which) -> {
                                deleteRecipeAndResolveReport(recipeId, reportId);
                            })
                            .setNegativeButton("Huỷ", null)
                            .show();
                });

                // Dismiss action click listener
                holder.btnDismissReport.setOnClickListener(v -> {
                    dismissReport(reportId);
                });
            } else {
                // Comment report mode
                holder.cardImage.setVisibility(View.GONE);

                String commentText = (String) report.get("commentText");
                String commentAuthor = (String) report.get("commentAuthor");
                String commentId = (String) report.get("commentId");
                Object recipeIdObj = report.get("recipeId");
                String recipeId = recipeIdObj != null ? recipeIdObj.toString() : "";
                String recipeName = (String) report.get("recipeName");
                String reporterEmail = (String) report.get("reporterEmail");
                String reason = (String) report.get("reason");
                String details = (String) report.get("details");

                holder.tvRecipeName.setText(commentText != null ? commentText : "Nội dung bình luận trống");
                holder.tvRecipeAuthor.setText("Người viết: " + (commentAuthor != null ? commentAuthor : "Ẩn danh") + " • Bài: " + (recipeName != null ? recipeName : "Chưa rõ"));
                holder.chipReason.setText(reason != null ? reason : "Báo cáo bình luận");

                holder.layoutDetails.setVisibility(View.VISIBLE);
                holder.tvReporterEmail.setText("Người báo cáo: " + (reporterEmail != null ? reporterEmail : "Ẩn danh"));
                holder.tvDetailComment.setText("Chi tiết lý do: " + (TextUtils.isEmpty(details) ? "Không có" : details));

                holder.btnDeleteRecipe.setText("Xoá bình luận");
                holder.btnDeleteRecipe.setOnClickListener(v -> {
                    new AlertDialog.Builder(AdminReportsActivity.this, R.style.BottomSheetTheme)
                            .setTitle("Xác nhận xoá bình luận")
                            .setMessage("Bạn có chắc chắn muốn xoá vĩnh viễn bình luận này khỏi hệ thống không?")
                            .setPositiveButton("Xoá", (dialog, which) -> {
                                deleteCommentAndResolveReport(recipeId, commentId, reportId);
                            })
                            .setNegativeButton("Huỷ", null)
                            .show();
                });

                holder.btnDismissReport.setOnClickListener(v -> {
                    dismissCommentReport(reportId);
                });
            }
        }

        @Override
        public int getItemCount() {
            return reportsList.size();
        }

        class ReportViewHolder extends RecyclerView.ViewHolder {
            View cardImage;
            ImageView imgRecipe;
            TextView tvRecipeName;
            TextView tvRecipeAuthor;
            Chip chipReason;
            View layoutDetails;
            TextView tvReporterEmail;
            TextView tvDetailComment;
            com.google.android.material.button.MaterialButton btnDeleteRecipe;
            com.google.android.material.button.MaterialButton btnDismissReport;

            public ReportViewHolder(@NonNull View itemView) {
                super(itemView);
                cardImage = itemView.findViewById(R.id.cardReportedRecipeImage);
                imgRecipe = itemView.findViewById(R.id.imgReportedRecipe);
                tvRecipeName = itemView.findViewById(R.id.tvReportedRecipeName);
                tvRecipeAuthor = itemView.findViewById(R.id.tvReportedRecipeAuthor);
                chipReason = itemView.findViewById(R.id.chipReason);
                layoutDetails = itemView.findViewById(R.id.layoutReportDetails);
                tvReporterEmail = itemView.findViewById(R.id.tvReporterEmail);
                tvDetailComment = itemView.findViewById(R.id.tvReportDetailComment);
                btnDeleteRecipe = itemView.findViewById(R.id.btnDeleteRecipe);
                btnDismissReport = itemView.findViewById(R.id.btnDismissReport);
            }
        }
    }

    private void deleteRecipeAndResolveReport(String recipeId, String reportId) {
        pbReportsProgress.setVisibility(View.VISIBLE);

        // Delete recipe document from recipes
        FirebaseFirestore.getInstance().collection("recipes")
                .document(recipeId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Update report status to resolved
                    FirebaseFirestore.getInstance().collection("reports")
                            .document(reportId)
                            .update("status", "resolved_deleted")
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(AdminReportsActivity.this, "Đã xoá công thức nấu ăn và giải quyết báo cáo!", Toast.LENGTH_SHORT).show();
                                fetchPendingReports();
                            })
                            .addOnFailureListener(e -> {
                                pbReportsProgress.setVisibility(View.GONE);
                                Toast.makeText(AdminReportsActivity.this, "Công thức đã xoá nhưng lỗi cập nhật báo cáo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    pbReportsProgress.setVisibility(View.GONE);
                    Toast.makeText(AdminReportsActivity.this, "Lỗi khi xoá công thức: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void dismissReport(String reportId) {
        pbReportsProgress.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance().collection("reports")
                .document(reportId)
                .update("status", "dismissed")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdminReportsActivity.this, "Đã bỏ qua báo cáo này!", Toast.LENGTH_SHORT).show();
                    fetchPendingReports();
                })
                .addOnFailureListener(e -> {
                    pbReportsProgress.setVisibility(View.GONE);
                    Toast.makeText(AdminReportsActivity.this, "Lỗi khi bỏ qua báo cáo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteCommentAndResolveReport(String recipeId, String commentId, String reportId) {
        pbReportsProgress.setVisibility(View.VISIBLE);

        if (TextUtils.isEmpty(recipeId) || TextUtils.isEmpty(commentId)) {
            // Just resolve the report if IDs are missing/invalid
            FirebaseFirestore.getInstance().collection("commentReports")
                    .document(reportId)
                    .update("status", "resolved")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AdminReportsActivity.this, "Không tìm thấy ID bình luận. Đã giải quyết báo cáo!", Toast.LENGTH_SHORT).show();
                        fetchPendingReports();
                    })
                    .addOnFailureListener(e -> {
                        pbReportsProgress.setVisibility(View.GONE);
                        Toast.makeText(AdminReportsActivity.this, "Lỗi cập nhật báo cáo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        // Delete from subcollection: recipes/{recipeId}/reviews/{commentId}
        FirebaseFirestore.getInstance().collection("recipes")
                .document(recipeId)
                .collection("reviews")
                .document(commentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    FirebaseFirestore.getInstance().collection("commentReports")
                            .document(reportId)
                            .update("status", "resolved")
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(AdminReportsActivity.this, "Đã xoá bình luận và giải quyết báo cáo!", Toast.LENGTH_SHORT).show();
                                fetchPendingReports();
                            })
                            .addOnFailureListener(e -> {
                                pbReportsProgress.setVisibility(View.GONE);
                                Toast.makeText(AdminReportsActivity.this, "Bình luận đã xoá nhưng lỗi cập nhật báo cáo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    pbReportsProgress.setVisibility(View.GONE);
                    Toast.makeText(AdminReportsActivity.this, "Lỗi khi xoá bình luận: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void dismissCommentReport(String reportId) {
        pbReportsProgress.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance().collection("commentReports")
                .document(reportId)
                .update("status", "resolved")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdminReportsActivity.this, "Đã bỏ qua báo cáo bình luận này!", Toast.LENGTH_SHORT).show();
                    fetchPendingReports();
                })
                .addOnFailureListener(e -> {
                    pbReportsProgress.setVisibility(View.GONE);
                    Toast.makeText(AdminReportsActivity.this, "Lỗi khi bỏ qua báo cáo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
