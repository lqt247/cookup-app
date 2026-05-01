package com.example.cookup_app.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.cookup_app.R;
import com.example.cookup_app.utils.ThemeManager;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private MaterialButton btnSendOtp;
    private TextView tvBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
    }

    private void setupClickListeners() {

        // Back → về Login
        btnBack.setOnClickListener(v -> finish());

        // Gửi OTP
        btnSendOtp.setOnClickListener(v -> {
            if (validateEmail()) {
                // TODO: Gọi API Spring Boot sau
                String email = etEmail.getText().toString().trim();
                Toast.makeText(this,
                        "Đã gửi mã OTP đến " + email,
                        Toast.LENGTH_SHORT).show();

                // Chuyển sang màn nhập OTP
                // TODO: startActivity(new Intent(this, OtpActivity.class));
            }
        });

        // Quay lại đăng nhập
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private boolean validateEmail() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            tilEmail.setError("Vui lòng nhập email");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            return false;
        }

        tilEmail.setError(null);
        return true;
    }
}