package com.example.cookup_app.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cookup_app.R;
import com.example.cookup_app.utils.ThemeManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "cookup_prefs";
    private static final String KEY_GENDER = "gender";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ImageView imgAvatar = findViewById(R.id.imgProfileAvatar);
        ImageView btnBack = findViewById(R.id.btnProfileBack);
        ImageView btnSettings = findViewById(R.id.btnProfileSettings);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);

        applyAvatar(imgAvatar);

        btnBack.setOnClickListener(v -> finish());
        btnSettings.setOnClickListener(v ->
                Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
        );

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().remove("token").apply();

            Intent intent = new Intent(this, SplashActivity.class);
            startActivity(intent);
            finishAffinity();
        });
    }

    private void applyAvatar(ImageView imgAvatar) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String gender = prefs.getString(KEY_GENDER, "unknown");

        if ("male".equalsIgnoreCase(gender)) {
            imgAvatar.setImageResource(R.drawable.avatar_default_male);
        } else if ("female".equalsIgnoreCase(gender)) {
            imgAvatar.setImageResource(R.drawable.avatar_default_female);
        } else {
            imgAvatar.setImageResource(R.drawable.avatar_default_unknown);
        }
    }
}
