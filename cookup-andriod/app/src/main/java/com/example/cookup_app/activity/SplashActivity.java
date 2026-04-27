package com.example.cookup_app.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.cookup_app.MainActivity;
import com.example.cookup_app.R;
import com.example.cookup_app.utils.ThemeManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Ánh xạ view — tìm view trong XML bằng id
        ImageView imgLogo = findViewById(R.id.imgLogo);
        TextView tvAppName = findViewById(R.id.tvAppName);
        TextView tvTagline = findViewById(R.id.tvTagline);

        /*
         * ANIMATION — làm logo + text xuất hiện đẹp hơn
         * ObjectAnimator: thay đổi property của View theo thời gian
         * alpha: độ trong suốt (0 = ẩn hoàn toàn, 1 = hiện hoàn toàn)
         * translationY: di chuyển theo trục Y (dương = xuống, âm = lên)
         */

        // Ban đầu ẩn hết
        imgLogo.setAlpha(0f);
        tvAppName.setAlpha(0f);
        tvTagline.setAlpha(0f);
        imgLogo.setTranslationY(50f);

        // Animation logo: fade in + bay lên
        ObjectAnimator logoFade = ObjectAnimator.ofFloat(imgLogo, "alpha", 0f, 1f);
        ObjectAnimator logoSlide = ObjectAnimator.ofFloat(imgLogo, "translationY", 50f, 0f);
        logoFade.setDuration(600);
        logoSlide.setDuration(600);

        // Animation tên app: fade in sau logo 200ms
        ObjectAnimator nameFade = ObjectAnimator.ofFloat(tvAppName, "alpha", 0f, 1f);
        nameFade.setDuration(500);
        nameFade.setStartDelay(300);

        // Animation tagline: fade in sau tên 200ms
        ObjectAnimator taglineFade = ObjectAnimator.ofFloat(tvTagline, "alpha", 0f, 1f);
        taglineFade.setDuration(500);
        taglineFade.setStartDelay(500);

        // Chạy tất cả animation cùng lúc
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(logoFade, logoSlide, nameFade, taglineFade);
        animatorSet.start();

        // Sau SPLASH_DURATION ms → chuyển màn hình
        new Handler(Looper.getMainLooper()).postDelayed(
                this::checkLoginAndNavigate,
                SPLASH_DURATION
        );
    }

    private void checkLoginAndNavigate() {
        SharedPreferences prefs = getSharedPreferences("cookup_prefs", MODE_PRIVATE);
        String token = prefs.getString("token", null);

        Intent intent;
        if (token != null) {
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);

        // Animation chuyển màn hình — fade out
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        finish();
    }
}