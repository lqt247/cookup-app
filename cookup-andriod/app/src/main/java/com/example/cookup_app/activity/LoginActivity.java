package com.example.cookup_app.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.example.cookup_app.R;
import com.example.cookup_app.utils.ThemeManager;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }
}