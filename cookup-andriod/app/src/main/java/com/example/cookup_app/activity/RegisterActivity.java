package com.example.cookup_app.activity;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cookup_app.R;
import com.example.cookup_app.utils.ThemeManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilEmail, tilPassword, tilConfirmPassword;

    private TextInputEditText etName,
            etEmail,
            etPassword,
            etConfirmPassword;

    private ProgressBar progressPasswordStrength;

    private TextView tvPasswordStrength,
            tvTerms,
            tvLogin;

    private CheckBox cbTerms;

    private MaterialButton btnRegister,
            btnGoogle,
            btnFacebook;

    private ImageButton btnBack;

    // Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeManager.applyTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Firebase init
        mAuth = FirebaseAuth.getInstance();

        initViews();

        setupPasswordStrength();

        setupTermsText();

        setupLoginText();

        setupClickListeners();
    }

    private void initViews() {

        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        progressPasswordStrength =
                findViewById(R.id.progressPasswordStrength);

        tvPasswordStrength =
                findViewById(R.id.tvPasswordStrength);

        tvTerms = findViewById(R.id.tvTerms);

        tvLogin = findViewById(R.id.tvLogin);

        cbTerms = findViewById(R.id.cbTerms);

        btnRegister = findViewById(R.id.btnRegister);

        btnGoogle = findViewById(R.id.btnGoogle);

        btnFacebook = findViewById(R.id.btnFacebook);

        btnBack = findViewById(R.id.btnBack);
    }

    private void setupClickListeners() {

        // Back
        btnBack.setOnClickListener(v -> finish());

        // Register
        btnRegister.setOnClickListener(v -> {

            if (validateForm()) {

                registerWithFirebase();
            }
        });
        // Google
        btnGoogle.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Google Login đang phát triển",
                        Toast.LENGTH_SHORT
                ).show()
        );
        // Facebook
        btnFacebook.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Facebook Login đang phát triển",
                        Toast.LENGTH_SHORT
                ).show()
        );
    }
    private void registerWithFirebase() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String fullName = etName.getText().toString().trim();

        btnRegister.setEnabled(false);
        btnRegister.setText("Đang đăng ký...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // 1. Cập nhật Display Name
                            UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();

                            user.updateProfile(profileUpdate).addOnCompleteListener(profileTask -> {
                                // Save profile and role to Firestore
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("uid", user.getUid());
                                userData.put("name", fullName);
                                userData.put("displayName", fullName);
                                userData.put("email", email);
                                if ("lequangtruong2472005@gmail.com".equalsIgnoreCase(email)) {
                                    userData.put("role", "admin");
                                } else {
                                    userData.put("role", "user");
                                }

                                FirebaseFirestore.getInstance().collection("users")
                                        .document(user.getUid())
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "User profile and role saved!"))
                                        .addOnFailureListener(e -> Log.e("Firestore", "Failed to save profile!", e));

                                // 2. Tiến hành gửi mail xác thực
                                user.sendEmailVerification()
                                        .addOnCompleteListener(verifyTask -> {
                                            if (verifyTask.isSuccessful()) {
                                                Toast.makeText(RegisterActivity.this,
                                                        "Đăng ký thành công!\nKiểm tra Gmail để xác thực tài khoản.",
                                                        Toast.LENGTH_LONG).show();
                                            } else {
                                                // Nếu không gửi được mail (Do bị block IP/Spam), vẫn báo cho user biết
                                                String detailError = verifyTask.getException() != null ?
                                                        verifyTask.getException().getMessage() : "Lỗi hệ thống mail";
                                                Log.e("Firebase_Register", "Gửi mail thất bại: " + detailError);
                                                Toast.makeText(RegisterActivity.this,
                                                        "Tài khoản đã tạo nhưng chưa thể gửi email kích hoạt do giới hạn hệ thống. Vui lòng bấm gửi lại ở màn hình kế tiếp!",
                                                        Toast.LENGTH_LONG).show();
                                            }

                                            // Bất luận gửi mail thành công hay không, vẫn sang màn hình verify để cứu session
                                            Intent intent = new Intent(RegisterActivity.this, VerifyEmailActivity.class);
                                            startActivity(intent);
                                            finish();
                                        });
                            });
                        }
                    } else {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Đăng ký");

                        String errorMessage = "Đăng ký thất bại";
                        if (task.getException() != null) {
                            String error = task.getException().getMessage();
                            if (error != null) {
                                if (error.contains("email address is already in use")) {
                                    errorMessage = "Email này đã được sử dụng";
                                    tilEmail.setError(errorMessage);
                                } else if (error.contains("badly formatted")) {
                                    errorMessage = "Email không hợp lệ";
                                    tilEmail.setError(errorMessage);
                                } else if (error.contains("weak-password")) {
                                    errorMessage = "Mật khẩu quá yếu, cần ít nhất 6 ký tự";
                                    tilPassword.setError(errorMessage);
                                }
                            }
                        }
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                    }
                });
    }
    // Password strength
    private void setupPasswordStrength() {
        etPassword.addTextChangedListener(
                new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }
                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {
                        updatePasswordStrength(s.toString());
                    }
                    @Override
                    public void afterTextChanged(
                            android.text.Editable s
                    ) {
                    }
                });
    }
    private void updatePasswordStrength(String password) {
        int strength = 0;
        if (password.length() >= 8) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[0-9].*")) strength++;
        if (password.matches(".*[!@#$%^&*].*")) strength++;
        switch (strength) {
            case 0:
            case 1:
                progressPasswordStrength.setProgress(25);
                progressPasswordStrength.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(
                                getColor(R.color.error)
                        )
                );
                tvPasswordStrength.setText("Mức độ: Yếu");
                tvPasswordStrength.setTextColor(
                        getColor(R.color.error)
                );
                break;
            case 2:
                progressPasswordStrength.setProgress(50);
                progressPasswordStrength.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(
                                getColor(R.color.warning)
                        )
                );
                tvPasswordStrength.setText("Mức độ: Trung bình");
                tvPasswordStrength.setTextColor(
                        getColor(R.color.warning)
                );

                break;
            case 3:
                progressPasswordStrength.setProgress(75);
                progressPasswordStrength.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(
                                getColor(R.color.orange_primary)
                        )
                );
                tvPasswordStrength.setText("Mức độ: Mạnh");
                tvPasswordStrength.setTextColor(
                        getColor(R.color.orange_primary)
                );
                break;
            case 4:
                progressPasswordStrength.setProgress(100);
                progressPasswordStrength.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(
                                getColor(R.color.success)
                        )
                );
                tvPasswordStrength.setText("Mức độ: Rất mạnh");
                tvPasswordStrength.setTextColor(
                        getColor(R.color.success)
                );
                break;
        }
    }
    private void setupTermsText() {
        String fullText = "Tôi đồng ý với Điều khoản sử dụng";
        SpannableString spannable = new SpannableString(fullText);
        int start = fullText.indexOf("Điều khoản sử dụng");
        int end = fullText.length();
        spannable.setSpan(
                new ForegroundColorSpan(
                        getColor(R.color.orange_primary)
                ),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                showTermsBottomSheet();
            }
            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(
                        getColor(R.color.orange_primary)
                );
                ds.setUnderlineText(false);
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvTerms.setText(spannable);
        tvTerms.setMovementMethod(
                LinkMovementMethod.getInstance()
        );
        tvTerms.setHighlightColor(
                android.graphics.Color.TRANSPARENT
        );
    }
    private void setupLoginText() {
        String fullText =
                "Đã có tài khoản? Đăng nhập";
        SpannableString spannable =
                new SpannableString(fullText);
        int start = fullText.indexOf("Đăng nhập");
        int end = fullText.length();
        spannable.setSpan(
                new ForegroundColorSpan(
                        getColor(R.color.orange_primary)
                ),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                finish();
            }
            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(
                        getColor(R.color.orange_primary)
                );
                ds.setUnderlineText(false);
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvLogin.setText(spannable);
        tvLogin.setMovementMethod(
                LinkMovementMethod.getInstance()
        );
        tvLogin.setHighlightColor(
                android.graphics.Color.TRANSPARENT
        );
    }
    private void showTermsBottomSheet() {
        BottomSheetDialog dialog =
                new BottomSheetDialog(
                        this,
                        R.style.BottomSheetTheme
                );
        View view =
                getLayoutInflater().inflate(
                        R.layout.bottom_sheet_terms,
                        null
                );
        dialog.setContentView(view);
        MaterialButton btnAgree =
                view.findViewById(R.id.btnAgree);
        btnAgree.setOnClickListener(v -> {
            cbTerms.setChecked(true);
            dialog.dismiss();
        });
        TextView tvDecline =
                view.findViewById(R.id.tvDecline);
        tvDecline.setOnClickListener(v ->
                dialog.dismiss()
        );
        dialog.show();
    }

    private boolean validateForm() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        boolean isValid = true;
        // Name
        if (name.isEmpty()) {
            tilName.setError(
                    "Vui lòng nhập họ và tên"
            );
            isValid = false;
        } else {
            tilName.setError(null);
        }
        // Email
        if (email.isEmpty()) {
            tilEmail.setError(
                    "Vui lòng nhập email"
            );
            isValid = false;
        } else if (
                !android.util.Patterns.EMAIL_ADDRESS
                        .matcher(email)
                        .matches()
        ) {
            tilEmail.setError(
                    "Email không hợp lệ"
            );
            isValid = false;
        } else {
            tilEmail.setError(null);
        }
        // Password
        if (password.isEmpty()) {
            tilPassword.setError(
                    "Vui lòng nhập mật khẩu"
            );
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError(
                    "Mật khẩu tối thiểu 6 ký tự"
            );
            isValid = false;
        } else {
            tilPassword.setError(null);
        }
        // Confirm password
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError(
                    "Vui lòng xác nhận mật khẩu"
            );
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            tilConfirmPassword.setError(
                    "Mật khẩu không khớp"
            );
            isValid = false;
        } else {
            tilConfirmPassword.setError(null);
        }
        // Terms
        if (!cbTerms.isChecked()) {
            Toast.makeText(
                    this,
                    "Vui lòng đồng ý điều khoản",
                    Toast.LENGTH_SHORT
            ).show();
            isValid = false;
        }
        return isValid;
    }

}