package com.example.cookup_app.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookup_app.R;
import com.example.cookup_app.adapter.SearchRecipeAdapter;
import com.example.cookup_app.model.Recipe;
import com.example.cookup_app.utils.RecipeDataHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllRecipesActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tabNewest;
    private TextView tabOldest;
    private RecyclerView rvAllRecipes;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private List<Recipe> recipeList = new ArrayList<>();
    private SearchRecipeAdapter adapter;
    private boolean isNewestSelected = true;
    private boolean isAdminMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_recipes);

        isAdminMode = getIntent().getBooleanExtra("isAdminMode", false);

        // Bind Views
        btnBack = findViewById(R.id.btnBack);
        tabNewest = findViewById(R.id.tabNewest);
        tabOldest = findViewById(R.id.tabOldest);
        rvAllRecipes = findViewById(R.id.rvAllRecipes);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // Setup RecyclerView with 2-column Grid Layout (to match search grid look)
        rvAllRecipes.setLayoutManager(new GridLayoutManager(this, 2));
        if (isAdminMode) {
            adapter = new SearchRecipeAdapter(recipeList, true, (recipe, position) -> {
                showDeleteConfirmationDialog(recipe, position);
            });
        } else {
            adapter = new SearchRecipeAdapter(recipeList);
        }
        rvAllRecipes.setAdapter(adapter);

        // Listeners
        btnBack.setOnClickListener(v -> finish());

        tabNewest.setOnClickListener(v -> {
            if (!isNewestSelected) {
                isNewestSelected = true;
                updateTabSelection();
                sortRecipes();
            }
        });

        tabOldest.setOnClickListener(v -> {
            if (isNewestSelected) {
                isNewestSelected = false;
                updateTabSelection();
                sortRecipes();
            }
        });

        // Load data
        loadRecipesFromFirestore();
    }

    private void updateTabSelection() {
        if (isNewestSelected) {
            tabNewest.setBackgroundResource(R.drawable.bg_segment_active);
            tabNewest.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            
            tabOldest.setBackgroundResource(android.R.color.transparent);
            tabOldest.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            tabOldest.setBackgroundResource(R.drawable.bg_segment_active);
            tabOldest.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            
            tabNewest.setBackgroundResource(android.R.color.transparent);
            tabNewest.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void loadRecipesFromFirestore() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);

        FirebaseFirestore.getInstance().collection("recipes")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    recipeList.clear();
                    
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Recipe recipe = doc.toObject(Recipe.class);
                            if (recipe != null) {
                                recipe = RecipeDataHelper.sanitizeAndHealRecipeId(doc, recipe);
                                recipeList.add(recipe);
                            }
                        }
                    }

                    if (recipeList.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        sortRecipes();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.VISIBLE);
                    Toast.makeText(AllRecipesActivity.this, "Lỗi tải công thức: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sortRecipes() {
        if (recipeList == null || recipeList.isEmpty()) return;

        Collections.sort(recipeList, (r1, r2) -> {
            // Sort by createdAt timestamp
            long t1 = r1.getCreatedAt();
            long t2 = r2.getCreatedAt();
            
            // If createdAt values are equal or 0, fallback to comparing recipe IDs
            if (t1 == t2) {
                return isNewestSelected ? Integer.compare(r2.getId(), r1.getId()) : Integer.compare(r1.getId(), r2.getId());
            }
            
            return isNewestSelected ? Long.compare(t2, t1) : Long.compare(t1, t2);
        });

        adapter.notifyDataSetChanged();
    }

    private void showDeleteConfirmationDialog(Recipe recipe, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa công thức")
                .setMessage("Bạn có chắc chắn muốn xóa công thức \"" + recipe.getName() + "\" khỏi hệ thống không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteRecipeFromFirestore(recipe, position);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteRecipeFromFirestore(Recipe recipe, int position) {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("recipes")
                .document(String.valueOf(recipe.getId()))
                .delete()
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AllRecipesActivity.this, "Đã xóa công thức thành công!", Toast.LENGTH_SHORT).show();
                    recipeList.remove(position);
                    adapter.notifyItemRemoved(position);
                    if (recipeList.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AllRecipesActivity.this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
