package com.example.cookup_app.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.cookup_app.R;
import com.example.cookup_app.utils.ThemeManager;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword;
    private ProgressBar progressPasswordStrength;
    private TextView tvPasswordStrength, tvTerms, tvLogin;
    private CheckBox cbTerms;
    private MaterialButton btnRegister, btnGoogle, btnFacebook;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

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
        progressPasswordStrength = findViewById(R.id.progressPasswordStrength);
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);
        tvTerms = findViewById(R.id.tvTerms);
        tvLogin = findViewById(R.id.tvLogin);
        cbTerms = findViewById(R.id.cbTerms);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupClickListeners() {
        // Back → về Login
        btnBack.setOnClickListener(v -> {
            finish(); // finish() = đóng màn hiện tại = quay lại màn trước
        });

        // Đăng ký
        btnRegister.setOnClickListener(v -> {
            if (validateForm()) {
                // TODO: Gọi API Spring Boot sau
                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                // Chuyển về Login sau khi đăng ký
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        // Google (làm sau)
        btnGoogle.setOnClickListener(v ->
                Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
        );

        // Facebook (làm sau)
        btnFacebook.setOnClickListener(v ->
                Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
        );
    }

    // Theo dõi độ mạnh mật khẩu realtime
    private void setupPasswordStrength() {
        etPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void updatePasswordStrength(String password) {
        int strength = 0;
        if (password.length() >= 8) strength++;
        if (password.matches(".*[A-Z].*")) strength++; // có chữ hoa
        if (password.matches(".*[0-9].*")) strength++; // có số
        if (password.matches(".*[!@#$%^&*].*")) strength++; // có ký tự đặc biệt

        switch (strength) {
            case 0:
            case 1:
                progressPasswordStrength.setProgress(25);
                progressPasswordStrength.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(
                                getColor(R.color.error)));
                tvPasswordStrength.setText("Mức độ: Yếu");
                tvPasswordStrength.setTextColor(getColor(R.color.error));
                break;
            case 2:
                progressPasswordStrength.setProgress(50);
                progressPasswordStrength.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(
                                getColor(R.color.warning)));
                tvPasswordStrength.setText("Mức độ: Trung bình");
                tvPasswordStrength.setTextColor(getColor(R.color.warning));
                break;
            case 3:
                progressPasswordStrength.setProgress(75);
                progressPasswordStrength.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(
                                getColor(R.color.orange_primary)));
                tvPasswordStrength.setText("Mức độ: Mạnh");
                tvPasswordStrength.setTextColor(getColor(R.color.orange_primary));
                break;
            case 4:
                progressPasswordStrength.setProgress(100);
                progressPasswordStrength.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(
                                getColor(R.color.success)));
                tvPasswordStrength.setText("Mức độ: Rất mạnh");
                tvPasswordStrength.setTextColor(getColor(R.color.success));
                break;
        }
    }

    // "Tôi đồng ý với Điều khoản sử dụng"
    // "Điều khoản sử dụng" màu cam + bấm được
    private void setupTermsText() {
        String fullText = "Tôi đồng ý với Điều khoản sử dụng";
        SpannableString spannable = new SpannableString(fullText);

        int start = fullText.indexOf("Điều khoản sử dụng");
        int end = fullText.length();

        // Màu cam
        spannable.setSpan(
                new ForegroundColorSpan(getColor(R.color.orange_primary)),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Bấm được → mở BottomSheet điều khoản
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                showTermsBottomSheet();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(getColor(R.color.orange_primary));
                ds.setUnderlineText(false); // không gạch chân
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvTerms.setText(spannable);
        tvTerms.setMovementMethod(LinkMovementMethod.getInstance());
        tvTerms.setHighlightColor(android.graphics.Color.TRANSPARENT);
    }

    // "Đã có tài khoản? Đăng nhập"
    private void setupLoginText() {
        String fullText = "Đã có tài khoản? Đăng nhập";
        SpannableString spannable = new SpannableString(fullText);

        int start = fullText.indexOf("Đăng nhập");
        int end = fullText.length();

        spannable.setSpan(
                new ForegroundColorSpan(getColor(R.color.orange_primary)),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                finish(); // quay lại Login
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(getColor(R.color.orange_primary));
                ds.setUnderlineText(false);
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvLogin.setText(spannable);
        tvLogin.setMovementMethod(LinkMovementMethod.getInstance());
        tvLogin.setHighlightColor(android.graphics.Color.TRANSPARENT);
    }

    // BottomSheet hiện điều khoản sử dụng
    private void showTermsBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_terms, null);
        dialog.setContentView(view);

        // Nút đồng ý trong bottom sheet
        MaterialButton btnAgree = view.findViewById(R.id.btnAgree);
        btnAgree.setOnClickListener(v -> {
            cbTerms.setChecked(true); // tự tick checkbox
            dialog.dismiss();
        });

        // Nút từ chối
        TextView tvDecline = view.findViewById(R.id.tvDecline);
        tvDecline.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private boolean validateForm() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        boolean isValid = true;

        if (name.isEmpty()) {
            tilName.setError("Vui lòng nhập họ và tên");
            isValid = false;
        } else {
            tilName.setError(null);
        }

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

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            tilConfirmPassword.setError("Mật khẩu không khớp");
            isValid = false;
        } else {
            tilConfirmPassword.setError(null);
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this,
                    "Vui lòng đồng ý với Điều khoản sử dụng",
                    Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }
}