package com.example.cookup_app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cookup_app.MainActivity;
import com.example.cookup_app.R;
import com.example.cookup_app.utils.ThemeManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends AppCompatActivity {

    private static final String TAG = "VERIFY_EMAIL";

    private FirebaseAuth mAuth;

    private TextView tvEmail;
    private TextView tvBackToLogin;

    private MaterialButton btnVerified;
    private MaterialButton btnResend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeManager.applyTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {

        tvEmail = findViewById(R.id.tvEmail);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnVerified = findViewById(R.id.btnVerified);
        btnResend = findViewById(R.id.btnResend);

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {

            Toast.makeText(this,
                    "Không tìm thấy tài khoản đăng nhập",
                    Toast.LENGTH_LONG).show();

            goToLogin();
            return;
        }

        tvEmail.setText(user.getEmail());
    }

    private void setupListeners() {

        btnVerified.setOnClickListener(v ->
                checkVerification());

        btnResend.setOnClickListener(v ->
                resendVerificationEmail());

        tvBackToLogin.setOnClickListener(v -> {

            mAuth.signOut();
            goToLogin();
        });
    }

    private void resendVerificationEmail() {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Không tìm thấy tài khoản",
                    Toast.LENGTH_LONG
            ).show();

            goToLogin();
            return;
        }

        btnResend.setEnabled(false);
        btnResend.setText("Đang gửi...");

        user.sendEmailVerification()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                this,
                                "Đã gửi email xác thực.\nHãy kiểm tra Gmail hoặc Spam.",
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        Exception e = task.getException();

                        // Firebase anti-spam block
                        if (e instanceof com.google.firebase.FirebaseTooManyRequestsException) {

                            Toast.makeText(
                                    this,
                                    "Bạn thao tác quá nhiều.\nVui lòng thử lại sau 30-60 phút.",
                                    Toast.LENGTH_LONG
                            ).show();

                        } else {

                            Toast.makeText(
                                    this,
                                    e != null
                                            ? e.getMessage()
                                            : "Không thể gửi email",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    // cooldown 2 phút
                    new Handler(Looper.getMainLooper())
                            .postDelayed(() -> {

                                btnResend.setEnabled(true);
                                btnResend.setText("Gửi lại email");

                            }, 120000);
                });
    }

    private void checkVerification() {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {

            goToLogin();
            return;
        }

        btnVerified.setEnabled(false);
        btnVerified.setText("Đang kiểm tra...");

        user.reload()
                .addOnCompleteListener(task -> {

                    btnVerified.setEnabled(true);
                    btnVerified.setText("Tôi đã xác thực Email");

                    if (task.isSuccessful()) {

                        FirebaseUser refreshedUser =
                                FirebaseAuth.getInstance()
                                        .getCurrentUser();

                        if (refreshedUser != null
                                && refreshedUser.isEmailVerified()) {

                            Toast.makeText(
                                    this,
                                    "Xác thực thành công!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            Intent intent =
                                    new Intent(
                                            this,
                                            MainActivity.class
                                    );

                            intent.setFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            );

                            startActivity(intent);
                            finish();

                        } else {

                            Toast.makeText(
                                    this,
                                    "Bạn chưa xác thực email.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                    } else {

                        Exception e = task.getException();

                        Log.e(TAG,
                                "Reload failed",
                                e);

                        Toast.makeText(
                                this,
                                "Không thể kiểm tra trạng thái xác thực.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    @Override
    protected void onResume() {

        super.onResume();

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {

            user.reload().addOnCompleteListener(task -> {

                FirebaseUser refreshedUser =
                        FirebaseAuth.getInstance()
                                .getCurrentUser();

                if (refreshedUser != null
                        && refreshedUser.isEmailVerified()) {

                    Intent intent =
                            new Intent(
                                    this,
                                    MainActivity.class
                            );

                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);
                    finish();
                }
            });
        }
    }

    private void goToLogin() {

        Intent intent =
                new Intent(
                        this,
                        LoginActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}