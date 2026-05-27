package com.example.cookup_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

import com.example.cookup_app.fragment.FavoriteFragment;
import com.example.cookup_app.fragment.HomeFragment;
import com.example.cookup_app.fragment.PlanFragment;
import com.example.cookup_app.fragment.SearchFragment;
import com.example.cookup_app.utils.ThemeManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.content.res.ColorStateList;
import android.graphics.Color;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Áp dụng Theme đồng bộ
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNav);
        fabUpload = findViewById(R.id.fabUpload);

        bottomNavigationView.setItemActiveIndicatorColor(ColorStateList.valueOf(Color.TRANSPARENT));

        if (savedInstanceState == null) {
            switchFragment(new HomeFragment());
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                switchFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_search) {
                switchFragment(new SearchFragment());
                return true;
            } else if (id == R.id.nav_favorite) {
                switchFragment(new FavoriteFragment());
                return true;
            } else if (id == R.id.nav_plan) {
                switchFragment(new PlanFragment());
                return true;
            }
            return false;
        });

        fabUpload.setOnClickListener(v ->
                android.widget.Toast.makeText(this,
                        "Tinh nang dang phat trien",
                        android.widget.Toast.LENGTH_SHORT).show()
        );
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}