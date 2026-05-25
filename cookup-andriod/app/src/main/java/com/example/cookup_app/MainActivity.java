package com.example.cookup_app;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cookup_app.R;
import com.example.cookup_app.activity.SplashActivity;
import com.example.cookup_app.utils.ThemeManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private TextView tvUserEmail;
    private MaterialButton btnLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Áp dụng Theme đồng bộ
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ các thành phần giao diện
        tvUserEmail = findViewById(R.id.tvUserEmail);
        btnLogout = findViewById(R.id.btnLogout);

        // Hiển thị email của tài khoản đang đăng nhập hiện tại
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            tvUserEmail.setText(currentUser.getEmail());
        } else {
            tvUserEmail.setText("Tài khoản chưa định danh");
        }

        // Xử lý sự kiện click nút Đăng xuất để test Splash
        btnLogout.setOnClickListener(v -> {

            // 1. Đăng xuất Firebase Session
            if (mAuth.getCurrentUser() != null) {
                mAuth.signOut();
            }

            // 2. Xóa Token khỏi SharedPreferences (Giúp Splash không bị check trúng token nữa)
            SharedPreferences prefs = getSharedPreferences("cookup_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("token");
            editor.apply();

            Toast.makeText(MainActivity.this, "Đăng xuất thành công! Đang khởi động lại màn hình Splash...", Toast.LENGTH_SHORT).show();

            // 3. Chuyển hướng quay về màn hình SplashActivity để test
            Intent intent = new Intent(MainActivity.this, SplashActivity.class);
            startActivity(intent);

            // Xoá sạch bộ nhớ stack các Activity cũ để không thể bấm phím Back quay lại màn Main được nữa
            finishAffinity();
        });
    }
}