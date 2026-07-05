package com.example.cookup_app.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.cookup_app.R;
import com.example.cookup_app.model.Notification;
import com.example.cookup_app.utils.ThemeManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "cookup_prefs";
    private static final String KEY_GENDER = "gender";
    private static final String KEY_CUSTOM_AVATAR = "custom_avatar_url";

    private ImageView imgNotificationHeaderAvatar;
    private TextView tabAllNotifications;
    private TextView tabUnreadNotifications;
    private LinearLayout containerNotifications;

    private boolean currentTabIsAll = true;
    private List<Notification> notificationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Bind Views
        imgNotificationHeaderAvatar = findViewById(R.id.imgNotificationHeaderAvatar);
        tabAllNotifications = findViewById(R.id.tabAllNotifications);
        tabUnreadNotifications = findViewById(R.id.tabUnreadNotifications);
        containerNotifications = findViewById(R.id.containerNotifications);

        View btnBack = findViewById(R.id.btnNotificationBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Apply dynamic gender/custom avatar
        loadHeaderAvatar();

        // Register tab selection listeners
        tabAllNotifications.setOnClickListener(v -> selectTab(true));
        tabUnreadNotifications.setOnClickListener(v -> selectTab(false));

        // Load notifications from Firestore
        loadNotificationsFromFirestore();
    }

    private void loadHeaderAvatar() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : "";
        
        String keyCustomAvatar = KEY_CUSTOM_AVATAR + (!uid.isEmpty() ? "_" + uid : "");
        String keyGender = KEY_GENDER + (!uid.isEmpty() ? "_" + uid : "");

        String customAvatarUrl = prefs.getString(keyCustomAvatar, "");
        if (!TextUtils.isEmpty(customAvatarUrl) && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(this, customAvatarUrl)) {
            Glide.with(this)
                    .load(customAvatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.avatar_default_unknown)
                    .error(R.drawable.avatar_default_unknown)
                    .into(imgNotificationHeaderAvatar);
        } else {
            String gender = prefs.getString(keyGender, "unknown");
            if ("male".equalsIgnoreCase(gender)) {
                imgNotificationHeaderAvatar.setImageResource(R.drawable.avatar_default_male);
            } else if ("female".equalsIgnoreCase(gender)) {
                imgNotificationHeaderAvatar.setImageResource(R.drawable.avatar_default_female);
            } else {
                imgNotificationHeaderAvatar.setImageResource(R.drawable.avatar_default_unknown);
            }
        }
    }

    private void selectTab(boolean showAll) {
        currentTabIsAll = showAll;
        if (showAll) {
            tabAllNotifications.setBackgroundResource(R.drawable.bg_segment_active);
            tabAllNotifications.setTextColor(getResources().getColor(R.color.text_primary));

            tabUnreadNotifications.setBackgroundResource(android.R.color.transparent);
            tabUnreadNotifications.setTextColor(getResources().getColor(R.color.text_secondary));
        } else {
            tabUnreadNotifications.setBackgroundResource(R.drawable.bg_segment_active);
            tabUnreadNotifications.setTextColor(getResources().getColor(R.color.text_primary));

            tabAllNotifications.setBackgroundResource(android.R.color.transparent);
            tabAllNotifications.setTextColor(getResources().getColor(R.color.text_secondary));
        }
        renderNotificationsList();
    }

    private void loadNotificationsFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            renderNotificationsList();
            return;
        }

        String currentUid = user.getUid();
        FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("recipientUid", currentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        boolean hasWelcome = false;
                        List<com.google.firebase.firestore.DocumentSnapshot> duplicatesToDelete = new ArrayList<>();

                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Notification notification = doc.toObject(Notification.class);
                            if (notification != null) {
                                notification.setId(doc.getId());
                                
                                if ("system".equalsIgnoreCase(notification.getType()) &&
                                        notification.getBody() != null &&
                                        notification.getBody().contains("Chào mừng bạn gia nhập mạng xã hội ẩm thực CookUp")) {
                                    if (hasWelcome) {
                                        duplicatesToDelete.add(doc);
                                        continue;
                                    } else {
                                        hasWelcome = true;
                                    }
                                }
                                notificationList.add(notification);
                            }
                        }

                        for (com.google.firebase.firestore.DocumentSnapshot duplicateDoc : duplicatesToDelete) {
                            FirebaseFirestore.getInstance().collection("notifications")
                                    .document(duplicateDoc.getId())
                                    .delete();
                        }

                        java.util.Collections.sort(notificationList, (n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                        renderNotificationsList();
                    } else {
                        prefillWelcomeNotifications(currentUid);
                    }
                })
                .addOnFailureListener(e -> {
                    prefillWelcomeNotificationsLocally();
                });
    }

    private void prefillWelcomeNotificationsLocally() {
        notificationList.clear();
        Notification n4 = new Notification(null, "Chào mừng thành viên mới", "Chào mừng bạn gia nhập mạng xã hội ẩm thực CookUp! Bắt đầu đăng tải công thức ngay nhé! 🎉", "system", System.currentTimeMillis(), false);
        notificationList.add(n4);
        renderNotificationsList();
    }

    private void prefillWelcomeNotifications(String recipientUid) {
        long now = System.currentTimeMillis();
        List<Notification> prefilled = new ArrayList<>();

        Notification n4 = new Notification(null, "Chào mừng thành viên mới", "Chào mừng bạn gia nhập mạng xã hội ẩm thực CookUp! Bắt đầu đăng tải công thức ngay nhé! 🎉", "system", now, false);

        prefilled.add(n4);

        for (Notification n : prefilled) {
            Map<String, Object> data = new HashMap<>();
            data.put("title", n.getTitle());
            data.put("body", n.getBody());
            data.put("type", n.getType());
            data.put("timestamp", n.getTimestamp());
            data.put("read", n.isRead());
            data.put("recipientUid", recipientUid);

            FirebaseFirestore.getInstance().collection("notifications")
                    .add(data)
                    .addOnSuccessListener(documentReference -> {
                        n.setId(documentReference.getId());
                        notificationList.add(n);
                        notificationList.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                        renderNotificationsList();
                    });
        }
    }

    private void renderNotificationsList() {
        if (containerNotifications == null) return;
        containerNotifications.removeAllViews();

        List<Notification> filteredList = new ArrayList<>();
        if (currentTabIsAll) {
            filteredList.addAll(notificationList);
        } else {
            for (Notification n : notificationList) {
                if (!n.isRead()) {
                    filteredList.add(n);
                }
            }
        }

        if (filteredList.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText(currentTabIsAll ? "Không có thông báo nào" : "Không có thông báo chưa đọc");
            tvEmpty.setTextColor(getResources().getColor(R.color.text_secondary));
            tvEmpty.setTextSize(14);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            tvEmpty.setPadding(48, 48, 48, 48);
            containerNotifications.addView(tvEmpty);
            return;
        }

        for (Notification n : filteredList) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_notification_card, containerNotifications, false);

            ImageView imgAvatar = card.findViewById(R.id.imgNotiAvatar);
            TextView tvBody = card.findViewById(R.id.tvNotiBody);
            TextView tvTime = card.findViewById(R.id.tvNotiTime);
            View viewDot = card.findViewById(R.id.viewNotiDot);
            View thumbContainer = card.findViewById(R.id.notiThumbContainer);
            ImageView imgThumb = card.findViewById(R.id.imgNotiThumb);

            tvBody.setText(n.getBody());

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            tvTime.setText(sdf.format(new Date(n.getTimestamp())));

            if (n.isRead()) {
                viewDot.setVisibility(View.GONE);
            } else {
                viewDot.setVisibility(View.VISIBLE);
            }

            if ("system".equalsIgnoreCase(n.getType())) {
                imgAvatar.setImageResource(R.drawable.ic_logo_cookup);
            } else {
                imgAvatar.setImageResource(R.drawable.avatar_default_unknown);
                if (!TextUtils.isEmpty(n.getSenderUid())) {
                    FirebaseFirestore.getInstance().collection("users")
                            .document(n.getSenderUid())
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                if (userDoc.exists() && !isFinishing()) {
                                    String url = userDoc.getString("avatarUrl");
                                    if (!TextUtils.isEmpty(url) && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(this, url)) {
                                        Glide.with(this)
                                                .load(url)
                                                .circleCrop()
                                                .placeholder(R.drawable.avatar_default_unknown)
                                                .error(R.drawable.avatar_default_unknown)
                                                .into(imgAvatar);
                                    } else {
                                        String gender = userDoc.getString("gender");
                                        if ("male".equalsIgnoreCase(gender)) {
                                            imgAvatar.setImageResource(R.drawable.avatar_default_male);
                                        } else if ("female".equalsIgnoreCase(gender)) {
                                            imgAvatar.setImageResource(R.drawable.avatar_default_female);
                                        }
                                    }
                                }
                            });
                } else {
                    if ("follow".equalsIgnoreCase(n.getType())) {
                        imgAvatar.setImageResource(R.drawable.avatar_default_female);
                    } else if ("like".equalsIgnoreCase(n.getType())) {
                        imgAvatar.setImageResource(R.drawable.avatar_default_male);
                    }
                }
            }

            if (!TextUtils.isEmpty(n.getRecipeImage()) && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(this, n.getRecipeImage())) {
                thumbContainer.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(n.getRecipeImage())
                        .placeholder(R.drawable.character_chef_1)
                        .error(R.drawable.character_chef_1)
                        .into(imgThumb);
            } else {
                thumbContainer.setVisibility(View.GONE);
            }

            // Click listener: Mark as read
            card.setOnClickListener(v -> {
                if (!n.isRead()) {
                    n.setRead(true);
                    viewDot.setVisibility(View.GONE);
                    if (n.getId() != null) {
                        FirebaseFirestore.getInstance().collection("notifications")
                                .document(n.getId())
                                .update("read", true);
                    }
                }
                navigateToNotificationContent(n);
            });

            // TÍNH NĂNG XÓA: Nhấn giữ để xóa thông báo
            card.setOnLongClickListener(v -> {
                showDeleteNotificationDialog(n);
                return true;
            });

            containerNotifications.addView(card);
        }
    }

    private void showDeleteNotificationDialog(Notification n) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa thông báo")
                .setMessage("Bạn có chắc chắn muốn xóa thông báo này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (n.getId() != null) {
                        FirebaseFirestore.getInstance().collection("notifications")
                                .document(n.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    notificationList.remove(n);
                                    renderNotificationsList();
                                    Toast.makeText(this, "Đã xóa thông báo", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void navigateToNotificationContent(Notification n) {
        String body = n.getBody() != null ? n.getBody() : "";
        String recipeId = n.getRecipeId();
        String senderUid = n.getSenderUid();

        if (!TextUtils.isEmpty(recipeId)) {
            FirebaseFirestore.getInstance().collection("recipes")
                    .document(recipeId)
                    .get()
                    .addOnSuccessListener(recipeDoc -> {
                        if (recipeDoc.exists() && !isFinishing()) {
                            com.example.cookup_app.model.Recipe recipeObj = recipeDoc.toObject(com.example.cookup_app.model.Recipe.class);
                            if (recipeObj != null) {
                                android.content.Intent intent = new android.content.Intent(this, RecipeDetailActivity.class);
                                intent.putExtra("recipe", recipeObj);
                                startActivity(intent);
                            }
                        } else {
                            navigateToUserByText(body, senderUid);
                        }
                    })
                    .addOnFailureListener(e -> navigateToUserByText(body, senderUid));
        } else {
            navigateToUserByText(body, senderUid);
        }
    }

    private String removeAccents(String src) {
        if (src == null) return "";
        String nfdNormalizedString = java.text.Normalizer.normalize(src, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(nfdNormalizedString).replaceAll("")
                .toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("Đ", "d");
        result = result.replaceAll("\\b(admin|chef|dau bep|dau-bep|daubep|nguoi dung|user)\\b", "")
                       .replaceAll("\\s+", "");
        return result;
    }

    private boolean isNameMatch(String name1, String name2) {
        if (name1 == null || name2 == null) return false;
        String clean1 = removeAccents(name1);
        String clean2 = removeAccents(name2);
        if (clean1.isEmpty() || clean2.isEmpty()) return false;
        return clean1.contains(clean2) || clean2.contains(clean1);
    }

    private void navigateToUserByText(String body, String senderUid) {
        if (!TextUtils.isEmpty(senderUid)) {
            android.content.Intent intent = new android.content.Intent(this, ProfileActivity.class);
            intent.putExtra("userId", senderUid);
            startActivity(intent);
            return;
        }

        String senderName = null;
        if (!TextUtils.isEmpty(body)) {
            if (body.contains(" đã bắt đầu theo dõi")) {
                int endIdx = body.indexOf(" đã bắt đầu theo dõi");
                if (endIdx > 0) senderName = body.substring(0, endIdx).trim();
            } else if (body.contains("chia sẻ bởi ")) {
                int startIdx = body.indexOf("chia sẻ bởi ") + "chia sẻ bởi ".length();
                int endIdx = body.indexOf(".", startIdx);
                senderName = (endIdx != -1) ? body.substring(startIdx, endIdx).trim() : body.substring(startIdx).trim();
            } else if (body.contains("Đầu bếp ")) {
                int startIdx = body.indexOf("Đầu bếp ") + "Đầu bếp ".length();
                int endIdx = body.indexOf(" vừa", startIdx);
                if (endIdx != -1 && endIdx > startIdx) senderName = body.substring(startIdx, endIdx).trim();
            }
        }

        final String finalSenderName = senderName;
        FirebaseFirestore.getInstance().collection("users")
                .get()
                .addOnSuccessListener(queryDocs -> {
                    if (queryDocs != null && !isFinishing()) {
                        String foundUserId = null;
                        if (!TextUtils.isEmpty(finalSenderName)) {
                            for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocs.getDocuments()) {
                                if (isNameMatch(finalSenderName, doc.getString("displayName")) || isNameMatch(finalSenderName, doc.getString("name"))) {
                                    foundUserId = doc.getId();
                                    break;
                                }
                            }
                        }
                        
                        if (foundUserId == null) {
                            for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocs.getDocuments()) {
                                String role = doc.getString("role");
                                String email = doc.getString("email");
                                if ("admin".equalsIgnoreCase(role) || (email != null && "lequangtruong2472005@gmail.com".equalsIgnoreCase(email.trim()))) {
                                    foundUserId = doc.getId();
                                    break;
                                }
                            }
                        }

                        if (foundUserId != null) {
                            android.content.Intent intent = new android.content.Intent(this, ProfileActivity.class);
                            intent.putExtra("userId", foundUserId);
                            startActivity(intent);
                        }
                    }
                });
    }
}
