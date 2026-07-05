package com.example.cookup_app.activity;

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

public class NotificationActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "cookup_prefs";
    private static final String KEY_GENDER = "gender";

    private ImageView imgNotificationHeaderAvatar;
    private TextView tabAllNotifications;
    private TextView tabUnreadNotifications;

    private View notiCard1;
    private View notiCard2;
    private View notiCard3;
    private View notiCard4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Bind Views
        imgNotificationHeaderAvatar = findViewById(R.id.imgNotificationHeaderAvatar);
        tabAllNotifications = findViewById(R.id.tabAllNotifications);
        tabUnreadNotifications = findViewById(R.id.tabUnreadNotifications);

        notiCard1 = findViewById(R.id.notiCard1);
        notiCard2 = findViewById(R.id.notiCard2);
        notiCard3 = findViewById(R.id.notiCard3);
        notiCard4 = findViewById(R.id.notiCard4);

        View btnBack = findViewById(R.id.btnNotificationBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Apply dynamic gender avatar
        loadHeaderAvatar();

        // Register tab selection listeners
        tabAllNotifications.setOnClickListener(v -> selectTab(true));
        tabUnreadNotifications.setOnClickListener(v -> selectTab(false));
    }

    private void loadHeaderAvatar() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String gender = prefs.getString(KEY_GENDER, "unknown");

        if ("male".equalsIgnoreCase(gender)) {
            imgNotificationHeaderAvatar.setImageResource(R.drawable.avatar_default_male);
        } else if ("female".equalsIgnoreCase(gender)) {
            imgNotificationHeaderAvatar.setImageResource(R.drawable.avatar_default_female);
        } else {
            imgNotificationHeaderAvatar.setImageResource(R.drawable.avatar_default_unknown);
        }
    }

    private void selectTab(boolean showAll) {
        if (showAll) {
            tabAllNotifications.setBackgroundResource(R.drawable.bg_segment_active);
            tabAllNotifications.setTextColor(getResources().getColor(R.color.text_primary));

            tabUnreadNotifications.setBackgroundResource(android.R.color.transparent);
            tabUnreadNotifications.setTextColor(getResources().getColor(R.color.text_secondary));

            // Show all notifications
            notiCard1.setVisibility(View.VISIBLE);
            notiCard2.setVisibility(View.VISIBLE);
            notiCard3.setVisibility(View.VISIBLE);
            notiCard4.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Hiển thị tất cả thông báo", Toast.LENGTH_SHORT).show();
        } else {
            tabUnreadNotifications.setBackgroundResource(R.drawable.bg_segment_active);
            tabUnreadNotifications.setTextColor(getResources().getColor(R.color.text_primary));

            tabAllNotifications.setBackgroundResource(android.R.color.transparent);
            tabAllNotifications.setTextColor(getResources().getColor(R.color.text_secondary));

            // Only show unread notifications (e.g. Card 2 has unread orange dot indicator)
            notiCard1.setVisibility(View.GONE);
            notiCard2.setVisibility(View.VISIBLE);
            notiCard3.setVisibility(View.GONE);
            notiCard4.setVisibility(View.GONE);
            Toast.makeText(this, "Hiển thị thông báo chưa đọc", Toast.LENGTH_SHORT).show();
        }
    }
}
