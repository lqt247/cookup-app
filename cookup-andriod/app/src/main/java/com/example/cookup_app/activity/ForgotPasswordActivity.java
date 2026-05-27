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
import com.google.firebase.auth.FirebaseAuth;
import com.example.cookup_app.R;
import com.example.cookup_app.utils.ThemeManager;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private MaterialButton btnSendOtp;
    private TextView tvBackToLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();
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

        btnBack.setOnClickListener(v -> finish());

        btnSendOtp.setOnClickListener(v -> {
            if (validateEmail()) {
                sendPasswordReset();
            }
        });

        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void sendPasswordReset() {
        String email = etEmail.getText().toString().trim();

        // Loading state
        btnSendOtp.setEnabled(false);
        btnSendOtp.setText("Đang gửi...");

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    btnSendOtp.setEnabled(true);
                    btnSendOtp.setText("Gửi email đặt lại mật khẩu");

                    if (task.isSuccessful()) {
                        // Thành công — Firebase gửi email reset
                        Toast.makeText(this,
                                "Đã gửi email đặt lại mật khẩu đến " + email +
                                        "\nKiểm tra hộp thư của bạn!",
                                Toast.LENGTH_LONG).show();

                        // Quay về Login sau 2 giây
                        new android.os.Handler().postDelayed(() -> {
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        }, 2000);

                    } else {
                        // Thất bại
                        String errorMsg = "Gửi email thất bại";
                        if (task.getException() != null) {
                            String error = task.getException().getMessage();
                            if (error != null) {
                                if (error.contains("no user record") ||
                                        error.contains("user-not-found")) {
                                    errorMsg = "Email này chưa được đăng ký";
                                    tilEmail.setError(errorMsg);
                                } else if (error.contains("badly formatted")) {
                                    errorMsg = "Email không hợp lệ";
                                    tilEmail.setError(errorMsg);
                                } else if (error.contains("too-many-requests")) {
                                    errorMsg = "Gửi quá nhiều lần, thử lại sau";
                                }
                            }
                        }
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
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