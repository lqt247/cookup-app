package com.example.cookup_app.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminUsersActivity extends AppCompatActivity {

    private ProgressBar pbUsersProgress;
    private View layoutUsersEmpty;
    private RecyclerView rvAdminUsers;

    private List<DocumentSnapshot> usersList = new ArrayList<>();
    private UsersAdapter adapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }

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
                        fetchUsers();
                    } else {
                        if (documentSnapshot.exists()) {
                            Toast.makeText(AdminUsersActivity.this, "Quyền truy cập bị từ chối!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(AdminUsersActivity.this, "Không tìm thấy thông tin tài khoản!", Toast.LENGTH_LONG).show();
                        }
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isHardcodedAdmin) {
                        findViewById(android.R.id.content).setVisibility(View.VISIBLE);
                        initViews();
                        setupRecyclerView();
                        fetchUsers();
                    } else {
                        Toast.makeText(AdminUsersActivity.this, "Lỗi xác thực: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }

    private void initViews() {
        findViewById(R.id.btnUsersBack).setOnClickListener(v -> finish());
        pbUsersProgress = findViewById(R.id.pbUsersProgress);
        layoutUsersEmpty = findViewById(R.id.layoutUsersEmpty);
        rvAdminUsers = findViewById(R.id.rvAdminUsers);
    }

    private void setupRecyclerView() {
        rvAdminUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UsersAdapter();
        rvAdminUsers.setAdapter(adapter);
    }

    private void fetchUsers() {
        pbUsersProgress.setVisibility(View.VISIBLE);
        rvAdminUsers.setVisibility(View.GONE);
        layoutUsersEmpty.setVisibility(View.GONE);

        FirebaseFirestore.getInstance().collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pbUsersProgress.setVisibility(View.GONE);
                    usersList.clear();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        usersList.addAll(queryDocumentSnapshots.getDocuments());
                    }

                    if (usersList.isEmpty()) {
                        layoutUsersEmpty.setVisibility(View.VISIBLE);
                        rvAdminUsers.setVisibility(View.GONE);
                    } else {
                        layoutUsersEmpty.setVisibility(View.GONE);
                        rvAdminUsers.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    pbUsersProgress.setVisibility(View.GONE);
                    layoutUsersEmpty.setVisibility(View.VISIBLE);
                    rvAdminUsers.setVisibility(View.GONE);
                    Toast.makeText(AdminUsersActivity.this, "Lỗi khi tải thành viên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_user_card, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            DocumentSnapshot doc = usersList.get(position);
            Map<String, Object> userData = doc.getData();
            if (userData == null) return;

            String userId = doc.getId();
            String username = (String) userData.get("username");
            String email = (String) userData.get("email");
            String role = (String) userData.get("role");
            String avatarUrl = (String) userData.get("avatarUrl");

            if (TextUtils.isEmpty(role)) {
                role = "user";
            }

            holder.tvUsername.setText(TextUtils.isEmpty(username) ? "Người dùng ẩn danh" : username);
            holder.tvUserEmail.setText(TextUtils.isEmpty(email) ? "Chưa cấu hình email" : email);

            // Configure chip role
            boolean isAdmin = "admin".equalsIgnoreCase(role);
            if (isAdmin) {
                holder.chipUserRole.setText("ADMIN");
                holder.chipUserRole.setChipBackgroundColorResource(R.color.orange_primary);
                holder.btnToggleRole.setText("Hạ cấp xuống User");
                holder.btnToggleRole.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.bg_elevated)));
            } else {
                holder.chipUserRole.setText("USER");
                holder.chipUserRole.setChipBackgroundColorResource(R.color.text_secondary);
                holder.btnToggleRole.setText("Thăng chức lên Admin");
                holder.btnToggleRole.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.orange_primary)));
            }

            // Load Avatar safely
            if (!TextUtils.isEmpty(avatarUrl) && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(AdminUsersActivity.this, avatarUrl)) {
                Glide.with(AdminUsersActivity.this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.avatar_default_male)
                        .error(R.drawable.avatar_default_male)
                        .into(holder.imgAvatar);
            } else {
                holder.imgAvatar.setImageResource(R.drawable.avatar_default_male);
            }

            // Safe toggle check: Cannot toggle own role, and cannot change sole admin's role
            boolean isTargetSoleAdmin = email != null && "lequangtruong2472005@gmail.com".equalsIgnoreCase(email.trim());
            if (userId.equals(currentUserId) || isTargetSoleAdmin || !isAdmin) {
                holder.btnToggleRole.setVisibility(View.GONE);
            } else {
                holder.btnToggleRole.setVisibility(View.VISIBLE);
                holder.btnToggleRole.setOnClickListener(v -> {
                    new AlertDialog.Builder(AdminUsersActivity.this, R.style.BottomSheetTheme)
                            .setTitle("Xác nhận thay đổi vai trò")
                            .setMessage("Bạn có chắc muốn hạ cấp thành viên \"" + (username != null ? username : email) + "\" xuống User không?")
                            .setPositiveButton("Xác nhận", (dialog, which) -> {
                                updateRole(userId, "user");
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                });
            }
        }

        @Override
        public int getItemCount() {
            return usersList.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            ShapeableImageView imgAvatar;
            TextView tvUsername;
            TextView tvUserEmail;
            Chip chipUserRole;
            com.google.android.material.button.MaterialButton btnToggleRole;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                imgAvatar = itemView.findViewById(R.id.imgUserAvatar);
                tvUsername = itemView.findViewById(R.id.tvUsername);
                tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
                chipUserRole = itemView.findViewById(R.id.chipUserRole);
                btnToggleRole = itemView.findViewById(R.id.btnToggleRole);
            }
        }
    }

    private void updateRole(String userId, String targetRole) {
        pbUsersProgress.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .update("role", targetRole)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdminUsersActivity.this, "Cập nhật vai trò thành công!", Toast.LENGTH_SHORT).show();
                    fetchUsers();
                })
                .addOnFailureListener(e -> {
                    pbUsersProgress.setVisibility(View.GONE);
                    Toast.makeText(AdminUsersActivity.this, "Lỗi khi cập nhật vai trò: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
