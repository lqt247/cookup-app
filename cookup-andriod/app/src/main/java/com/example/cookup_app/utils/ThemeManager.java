package com.example.cookup_app.utils;

import android.app.Activity;
import android.content.Context;

import com.example.cookup_app.R;

// utils/ThemeManager.java
public class ThemeManager {

    private static final String PREF_NAME = "cookup_prefs";
    private static final String KEY_THEME = "is_dark_theme";

    // Lưu lựa chọn theme
    public static void saveTheme(Context context, boolean isDark) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_THEME, isDark)
                .apply();
    }

    // Lấy lựa chọn đã lưu (mặc định là Dark)
    public static boolean isDarkTheme(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_THEME, true); // true = dark mặc định
    }

    // Áp dụng theme — gọi cái này trong onCreate() của MỌI Activity
    public static void applyTheme(Activity activity) {
        if (isDarkTheme(activity)) {
            activity.setTheme(R.style.Theme_Cookupapp_Dark);
        } else {
            activity.setTheme(R.style.Theme_Cookupapp_Light);
        }
    }

    // Toggle dark/light — gọi khi user bấm nút đổi theme
    public static void toggleTheme(Activity activity) {
        boolean currentlyDark = isDarkTheme(activity);
        saveTheme(activity, !currentlyDark);
        // Restart activity để áp dụng theme mới
        activity.recreate();
    }
}