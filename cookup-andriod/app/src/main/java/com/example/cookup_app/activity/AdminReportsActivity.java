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

        initViews();
        setupRecyclerView();
        fetchPendingReports();
    }

    private void initViews() {
        findViewById(R.id.btnReportsBack).setOnClickListener(v -> finish());
        tvRecipeBadgeCount = findViewById(R.id.tvRecipeBadgeCount);
        pbReportsProgress = findViewById(R.id.pbReportsProgress);
        layoutReportsEmpty = findViewById(R.id.layoutReportsEmpty);
        rvAdminReports = findViewById(R.id.rvAdminReports);
    }

    private void setupRecyclerView() {
        rvAdminReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportsAdapter();
        rvAdminReports.setAdapter(adapter);
    }

    private void fetchPendingReports() {
        pbReportsProgress.setVisibility(View.VISIBLE);
        rvAdminReports.setVisibility(View.GONE);
        layoutReportsEmpty.setVisibility(View.GONE);

        FirebaseFirestore.getInstance().collection("reports")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pbReportsProgress.setVisibility(View.GONE);
                    reportsList.clear();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        reportsList.addAll(queryDocumentSnapshots.getDocuments());
                    }

                    int count = reportsList.size();
                    tvRecipeBadgeCount.setText(String.valueOf(count));

                    if (count == 0) {
                        layoutReportsEmpty.setVisibility(View.VISIBLE);
                        rvAdminReports.setVisibility(View.GONE);
                    } else {
                        layoutReportsEmpty.setVisibility(View.GONE);
                        rvAdminReports.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
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
            if (recipeImageUrl != null && !recipeImageUrl.trim().isEmpty()) {
                Glide.with(AdminReportsActivity.this)
                        .load(recipeImageUrl)
                        .placeholder(R.drawable.character_chef_1)
                        .error(R.drawable.character_chef_1)
                        .into(holder.imgRecipe);
            } else if (recipeImageResId != 0) {
                holder.imgRecipe.setImageResource(recipeImageResId);
            } else {
                holder.imgRecipe.setImageResource(R.drawable.character_chef_1);
            }

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
        }

        @Override
        public int getItemCount() {
            return reportsList.size();
        }

        class ReportViewHolder extends RecyclerView.ViewHolder {
            ImageView imgRecipe;
            TextView tvRecipeName;
            TextView tvRecipeAuthor;
            Chip chipReason;
            View layoutDetails;
            TextView tvReporterEmail;
            TextView tvDetailComment;
            View btnDeleteRecipe;
            View btnDismissReport;

            public ReportViewHolder(@NonNull View itemView) {
                super(itemView);
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
}
