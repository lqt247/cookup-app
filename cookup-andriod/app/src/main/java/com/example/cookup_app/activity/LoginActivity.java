package com.example.cookup_app.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.cookup_app.R;
import com.example.cookup_app.utils.ThemeManager;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogle, btnFacebook;
    private TextView tvForgotPassword, tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupClickListeners();
        setupRegisterText();
    }

    private void initViews() {
        // Ánh xạ view — tìm view theo id
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegister);
    }

    private void setupClickListeners() {
        // Đăng nhập
        btnLogin.setOnClickListener(v -> {
            if (validateForm()) {
                // TODO: Gọi API Spring Boot sau
                // Tạm thời chuyển thẳng vào MainActivity
                startActivity(new Intent(this, com.example.cookup_app.MainActivity.class));
                finish();
            }
        });

        // Quên mật khẩu
        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        // Đăng ký
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        // Google
        btnGoogle.setOnClickListener(v -> {

        });

        // Facebook
        btnFacebook.setOnClickListener(v -> {

        });
    }

    private boolean validateForm() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean isValid = true;

        // Kiểm tra email
        if (email.isEmpty()) {
            tilEmail.setError("Vui lòng nhập email");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            isValid = false;
        } else {
            tilEmail.setError(null); // xoá lỗi nếu đúng
        }

        // Kiểm tra mật khẩu
        if (password.isEmpty()) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Mật khẩu tối thiểu 6 ký tự");
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        return isValid;
    }

    // Tạo text "Chưa có tài khoản? Đăng ký ngay"

    private void setupRegisterText() {
        String fullText = "Chưa có tài khoản? Đăng ký ngay";

        SpannableString spannable = new SpannableString(fullText);

        int start = fullText.indexOf("Đăng ký ngay");
        int end = fullText.length();

        // Màu cam
        spannable.setSpan(
                new ForegroundColorSpan(getColor(R.color.orange_primary)),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // Click riêng chữ "Đăng ký ngay"
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false); // đổi true nếu muốn gạch chân
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvRegister.setText(spannable);
        tvRegister.setMovementMethod(LinkMovementMethod.getInstance());
        tvRegister.setHighlightColor(Color.TRANSPARENT); // bỏ background khi click
    }
}