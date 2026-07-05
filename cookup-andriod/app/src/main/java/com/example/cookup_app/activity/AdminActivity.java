package com.example.cookup_app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cookup_app.R;
import com.example.cookup_app.utils.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Bind date text to show the actual current date
        TextView tvAdminDate = findViewById(R.id.tvAdminDate);
        if (tvAdminDate != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE, 'Ngày' d 'Tháng' M, yyyy", new Locale("vi", "VN"));
                String currentDate = sdf.format(new Date());
                // Capitalize first letter
                if (currentDate.length() > 0) {
                    currentDate = currentDate.substring(0, 1).toUpperCase() + currentDate.substring(1);
                }
                tvAdminDate.setText(currentDate);
            } catch (Exception e) {
                tvAdminDate.setText("Hôm nay, hoạt động bình thường");
            }
        }

        // Action Handlers
        findViewById(R.id.btnAdminBack).setOnClickListener(v -> finish());

        findViewById(R.id.cardStatUsers).setOnClickListener(v -> 
            Toast.makeText(this, "Tính năng quản lý thành viên đang mở rộng", Toast.LENGTH_SHORT).show());
            
        findViewById(R.id.cardStatRecipes).setOnClickListener(v -> 
            Toast.makeText(this, "Tính năng phê duyệt công thức đang mở rộng", Toast.LENGTH_SHORT).show());
            
        findViewById(R.id.cardStatReports).setOnClickListener(v -> 
            startActivity(new Intent(this, AdminReportsActivity.class)));
            
        findViewById(R.id.cardStatBlogs).setOnClickListener(v -> 
            Toast.makeText(this, "Tính năng quản lý bài viết đang mở rộng", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnAdminViewReports).setOnClickListener(v -> 
            startActivity(new Intent(this, AdminReportsActivity.class)));

        findViewById(R.id.btnAdminManageUsers).setOnClickListener(v -> 
            Toast.makeText(this, "Mở trang danh sách thành viên", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnAdminPublishBlog).setOnClickListener(v -> 
            Toast.makeText(this, "Mở trình soạn thảo bài viết Blog mới", Toast.LENGTH_SHORT).show());
    }
}
