package com.example.cookup_app;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.example.cookup_app.utils.ThemeManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this); // áp dụng theme trước
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}