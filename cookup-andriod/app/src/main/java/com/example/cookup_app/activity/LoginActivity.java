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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.cookup_app.R;
import com.example.cookup_app.utils.ThemeManager;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogle, btnFacebook;
    private TextView tvForgotPassword, tvRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // Kiểm tra đã đăng nhập chưa
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMain();
            return;
        }

        initViews();
        setupClickListeners();
        setupRegisterText();
    }

    private void initViews() {
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
        btnLogin.setOnClickListener(v -> {
            if (validateForm()) {
                loginWithFirebase();
            }
        });

        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class))
        );

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );

        btnGoogle.setOnClickListener(v ->
                Toast.makeText(this, "Google Login đang phát triển",
                        Toast.LENGTH_SHORT).show()
        );

        btnFacebook.setOnClickListener(v ->
                Toast.makeText(this, "Facebook Login đang phát triển",
                        Toast.LENGTH_SHORT).show()
        );
    }

    private void loginWithFirebase() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Đăng nhập");

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                String name = user.getDisplayName() != null ? user.getDisplayName() : "bạn";
                                Toast.makeText(this, "Xin chào " + name + "!", Toast.LENGTH_SHORT).show();
                                goToMain();
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "Tài khoản chưa được kích hoạt email!", Toast.LENGTH_LONG).show();
                                // Luôn chuyển thẳng sang màn hình verify để xử lý tiếp
                                startActivity(new Intent(LoginActivity.this, VerifyEmailActivity.class));
                            }
                        }
                    } else {
                        String errorMsg = "Đăng nhập thất bại";
                        if (task.getException() != null) {
                            String error = task.getException().getMessage();
                            if (error != null) {
                                if (error.contains("no user record") || error.contains("user-not-found")) {
                                    errorMsg = "Email chưa được đăng ký";
                                    tilEmail.setError(errorMsg);
                                } else if (error.contains("password is invalid") || error.contains("wrong-password")) {
                                    errorMsg = "Mật khẩu không đúng";
                                    tilPassword.setError(errorMsg);
                                } else if (error.contains("too-many-requests")) {
                                    errorMsg = "Quá nhiều yêu cầu. Vui lòng thử lại sau ít phút";
                                }
                            }
                        }
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void goToMain() {
        startActivity(new Intent(this, com.example.cookup_app.MainActivity.class));
        finishAffinity();
    }

    private boolean validateForm() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean isValid = true;

        if (email.isEmpty()) {
            tilEmail.setError("Vui lòng nhập email");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            isValid = false;
        } else {
            tilEmail.setError(null);
        }

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

    private void setupRegisterText() {
        String fullText = "Chưa có tài khoản? Đăng ký ngay";
        SpannableString spannable = new SpannableString(fullText);
        int start = fullText.indexOf("Đăng ký ngay");
        int end = fullText.length();

        spannable.setSpan(
                new ForegroundColorSpan(getColor(R.color.orange_primary)),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(getColor(R.color.orange_primary));
                ds.setUnderlineText(false);
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvRegister.setText(spannable);
        tvRegister.setMovementMethod(LinkMovementMethod.getInstance());
        tvRegister.setHighlightColor(Color.TRANSPARENT);
    }
}