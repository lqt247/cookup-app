package com.example.cookup_app.activity;

import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.cookup_app.R;
import com.example.cookup_app.model.Ingredient;
import com.example.cookup_app.model.Recipe;
import com.example.cookup_app.model.RecipeStep;
import com.example.cookup_app.utils.ThemeManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddRecipeActivity extends AppCompatActivity {

    private EditText edtRecipeName;
    private EditText edtDescription;
    private EditText edtCookTime;
    private EditText edtServings;
    
    // Difficulty
    private TextView btnDifficultyEasy;
    private TextView btnDifficultyMedium;
    private TextView btnDifficultyHard;
    private String selectedDifficulty = "Dễ";

    // Country
    private TextView chipCountryVN;
    private TextView chipCountryKR;
    private TextView chipCountryJP;
    private TextView chipCountryEU;
    private String selectedCountry = "Việt Nam";

    // Dynamic Tags
    private LinearLayout layoutTags;
    private View btnAddTag;
    private List<String> selectedTags = new ArrayList<>();

    // Dynamic Lists
    private LinearLayout containerIngredients;
    private TextView btnAddIngredient;
    
    private LinearLayout containerSteps;
    private TextView btnAddStep;

    // Main Cover Photo
    private LinearLayout boxAddImage;
    private ImageView imgRecipePreview;
    private LinearLayout layoutUploadPrompt;
    private String mainImageUrl = "";

    // Header actions & Submit
    private FrameLayout btnBackFrame;
    private ImageView btnMore;
    private MaterialButton btnSaveRecipe;

    // Image Pickers & Launchers
    private ActivityResultLauncher<Intent> mainImagePickerLauncher;
    private ActivityResultLauncher<Intent> stepImagePickerLauncher;
    private FrameLayout activeStepImageBox = null;
    private ImageView activeStepImagePreview = null;
    private ImageView activeStepCameraIcon = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        // Register Image Picker Launchers
        mainImagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        mainImageUrl = selectedImageUri.toString();
                        imgRecipePreview.setVisibility(View.VISIBLE);
                        layoutUploadPrompt.setVisibility(View.GONE);
                        Glide.with(AddRecipeActivity.this)
                                .load(selectedImageUri)
                                .placeholder(R.drawable.ic_logo_cookup)
                                .into(imgRecipePreview);
                    }
                }
            }
        );

        stepImagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null && activeStepImageBox != null) {
                        activeStepImageBox.setTag(selectedImageUri.toString());
                        if (activeStepImagePreview != null) {
                            activeStepImagePreview.setVisibility(View.VISIBLE);
                            Glide.with(AddRecipeActivity.this)
                                    .load(selectedImageUri)
                                    .placeholder(R.drawable.character_chef_1)
                                    .into(activeStepImagePreview);
                        }
                        if (activeStepCameraIcon != null) {
                            activeStepCameraIcon.setVisibility(View.GONE);
                        }
                    }
                }
            }
        );

        // Bind Views
        edtRecipeName = findViewById(R.id.edtRecipeName);
        edtDescription = findViewById(R.id.edtDescription);
        edtCookTime = findViewById(R.id.edtCookTime);
        edtServings = findViewById(R.id.edtServings);

        btnDifficultyEasy = findViewById(R.id.btnDifficultyEasy);
        btnDifficultyMedium = findViewById(R.id.btnDifficultyMedium);
        btnDifficultyHard = findViewById(R.id.btnDifficultyHard);

        chipCountryVN = findViewById(R.id.chipCountryVN);
        chipCountryKR = findViewById(R.id.chipCountryKR);
        chipCountryJP = findViewById(R.id.chipCountryJP);
        chipCountryEU = findViewById(R.id.chipCountryEU);

        layoutTags = findViewById(R.id.layoutTags);
        btnAddTag = findViewById(R.id.btnAddTag);

        containerIngredients = findViewById(R.id.containerIngredients);
        btnAddIngredient = findViewById(R.id.btnAddIngredient);

        containerSteps = findViewById(R.id.containerSteps);
        btnAddStep = findViewById(R.id.btnAddStep);

        boxAddImage = findViewById(R.id.boxAddImage);
        imgRecipePreview = findViewById(R.id.imgRecipePreview);
        layoutUploadPrompt = findViewById(R.id.layoutUploadPrompt);

        btnBackFrame = findViewById(R.id.btnBackFrame);
        btnMore = findViewById(R.id.btnMore);
        btnSaveRecipe = findViewById(R.id.btnSaveRecipe);

        // Initial Setup
        setupDifficultySelectors();
        setupCountrySelectors();
        setupTagAdder();
        setupIngredientsManager();
        setupStepsManager();
        setupMainImagePicker();

        // Prefill default sample data as requested (similar to screenshot)
        prefillSampleData();

        // Navigation
        btnBackFrame.setOnClickListener(v -> finish());
        btnMore.setOnClickListener(v -> Toast.makeText(this, "Tùy chọn bổ sung đang được phát triển!", Toast.LENGTH_SHORT).show());
        btnSaveRecipe.setOnClickListener(v -> saveRecipeToFirestore());
    }

    private void setupDifficultySelectors() {
        View.OnClickListener clickListener = v -> {
            // Reset styles
            btnDifficultyEasy.setBackgroundColor(Color.TRANSPARENT);
            btnDifficultyEasy.setTextColor(Color.parseColor("#8F756C"));
            btnDifficultyMedium.setBackgroundColor(Color.TRANSPARENT);
            btnDifficultyMedium.setTextColor(Color.parseColor("#8F756C"));
            btnDifficultyHard.setBackgroundColor(Color.TRANSPARENT);
            btnDifficultyHard.setTextColor(Color.parseColor("#8F756C"));

            int id = v.getId();
            if (id == R.id.btnDifficultyEasy) {
                selectedDifficulty = "Dễ";
                btnDifficultyEasy.setBackgroundResource(R.drawable.bg_segment_active);
                btnDifficultyEasy.setTextColor(Color.parseColor("#FFF1ED"));
            } else if (id == R.id.btnDifficultyMedium) {
                selectedDifficulty = "Trung bình";
                btnDifficultyMedium.setBackgroundResource(R.drawable.bg_segment_active);
                btnDifficultyMedium.setTextColor(Color.parseColor("#FFF1ED"));
            } else if (id == R.id.btnDifficultyHard) {
                selectedDifficulty = "Khó";
                btnDifficultyHard.setBackgroundResource(R.drawable.bg_segment_active);
                btnDifficultyHard.setTextColor(Color.parseColor("#FFF1ED"));
            }
        };

        btnDifficultyEasy.setOnClickListener(clickListener);
        btnDifficultyMedium.setOnClickListener(clickListener);
        btnDifficultyHard.setOnClickListener(clickListener);
    }

    private void setupCountrySelectors() {
        View.OnClickListener clickListener = v -> {
            // Reset all country chips to unselected
            chipCountryVN.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipCountryVN.setTextColor(Color.parseColor("#FFF1ED"));
            chipCountryKR.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipCountryKR.setTextColor(Color.parseColor("#FFF1ED"));
            chipCountryJP.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipCountryJP.setTextColor(Color.parseColor("#FFF1ED"));
            chipCountryEU.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipCountryEU.setTextColor(Color.parseColor("#FFF1ED"));

            TextView clickedChip = (TextView) v;
            selectedCountry = clickedChip.getText().toString();
            clickedChip.setBackgroundResource(R.drawable.bg_chip_selected);
            clickedChip.setTextColor(Color.parseColor("#FF7238"));
        };

        chipCountryVN.setOnClickListener(clickListener);
        chipCountryKR.setOnClickListener(clickListener);
        chipCountryJP.setOnClickListener(clickListener);
        chipCountryEU.setOnClickListener(clickListener);
    }

    private void setupTagAdder() {
        btnAddTag.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Thêm thẻ mới");

            final EditText input = new EditText(this);
            input.setHint("Nhập nhãn (ví dụ: Ăn chay)");
            builder.setView(input);

            builder.setPositiveButton("Thêm", (dialog, which) -> {
                String tag = input.getText().toString().trim();
                if (!TextUtils.isEmpty(tag) && !selectedTags.contains(tag)) {
                    addTagToLayout(tag);
                }
            });
            builder.setNegativeButton("Hủy", (dialog, id) -> dialog.cancel());
            builder.show();
        });
    }

    private void addTagToLayout(String tag) {
        selectedTags.add(tag);

        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout tagView = (LinearLayout) inflater.inflate(R.layout.item_add_tag, layoutTags, false);

        TextView tvTagName = tagView.findViewById(R.id.tvTagName);
        ImageView btnCloseTag = tagView.findViewById(R.id.btnCloseTag);

        tvTagName.setText(tag);
        btnCloseTag.setOnClickListener(v -> {
            layoutTags.removeView(tagView);
            selectedTags.remove(tag);
        });

        // Add dynamically before the "Add Tag" button
        int count = layoutTags.getChildCount();
        layoutTags.addView(tagView, count > 0 ? count - 1 : 0);
    }

    private void setupIngredientsManager() {
        btnAddIngredient.setOnClickListener(v -> addIngredientRow("", ""));
    }

    private void addIngredientRow(String qty, String name) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View row = inflater.inflate(R.layout.item_add_ingredient, containerIngredients, false);

        EditText edtQty = row.findViewById(R.id.edtIngredientQty);
        EditText edtName = row.findViewById(R.id.edtIngredientName);
        ImageView btnDelete = row.findViewById(R.id.btnDeleteIngredient);

        edtQty.setText(qty);
        edtName.setText(name);

        btnDelete.setOnClickListener(v -> containerIngredients.removeView(row));

        containerIngredients.addView(row);
    }

    private void setupStepsManager() {
        btnAddStep.setOnClickListener(v -> addStepRow(""));
    }

    private void addStepRow(String instructions) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View row = inflater.inflate(R.layout.item_add_step, containerSteps, false);

        TextView tvStepNum = row.findViewById(R.id.tvStepNumber);
        EditText edtInst = row.findViewById(R.id.edtStepInstructions);
        FrameLayout boxStepImage = row.findViewById(R.id.boxStepImage);
        ImageView imgStepPreview = row.findViewById(R.id.imgStepPreview);
        ImageView imgStepCameraIcon = row.findViewById(R.id.imgStepCameraIcon);
        ImageView btnDelete = row.findViewById(R.id.btnDeleteStep);

        edtInst.setText(instructions);

        // Manage step images
        boxStepImage.setTag(""); // Stores step image URL/Uri
        boxStepImage.setOnClickListener(v -> {
            activeStepImageBox = boxStepImage;
            activeStepImagePreview = imgStepPreview;
            activeStepCameraIcon = imgStepCameraIcon;

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            stepImagePickerLauncher.launch(Intent.createChooser(intent, "Chọn ảnh bước nấu"));
        });

        btnDelete.setOnClickListener(v -> {
            containerSteps.removeView(row);
            updateStepSequenceNumbers();
        });

        containerSteps.addView(row);
        updateStepSequenceNumbers();
    }

    private void updateStepSequenceNumbers() {
        for (int i = 0; i < containerSteps.getChildCount(); i++) {
            View child = containerSteps.getChildAt(i);
            TextView tvStepNum = child.findViewById(R.id.tvStepNumber);
            if (tvStepNum != null) {
                tvStepNum.setText(String.valueOf(i + 1));
            }
        }
    }

    private void setupMainImagePicker() {
        boxAddImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            mainImagePickerLauncher.launch(Intent.createChooser(intent, "Chọn ảnh món ăn"));
        });
    }

    private void prefillSampleData() {
        // Form Title
        edtRecipeName.setText("Phở Bò Nam Định");
        edtDescription.setText("Món phở truyền thống đậm đà bản sắc Việt với nước dùng từ xương bò ninh nhừ thơm lừng quế hồi.");
        edtCookTime.setText("45");
        edtServings.setText("4");

        // Prefill default sample tags
        addTagToLayout("Món mặn");
        addTagToLayout("Ăn tối");

        // Prefill ingredients matching screenshot
        addIngredientRow("500g", "Thịt bò thăn");
        addIngredientRow("1 củ", "Hành tây");
        addIngredientRow("Định lượng", "Tên nguyên liệu");

        // Prefill cooking steps
        addStepRow("Thái thịt bò mỏng, ướp với chút muối, tiêu và tỏi băm trong 15 phút.");
        addStepRow("Phi thơm tỏi, cho thịt bò vào xào nhanh trên lửa lớn cho tái rồi trút ra.");

        // Set a beautiful food cover photo as default preview
        mainImageUrl = "https://images.unsplash.com/photo-1583085204876-0f86a9f829ec?q=80&w=1000&auto=format&fit=crop";
        imgRecipePreview.setVisibility(View.VISIBLE);
        layoutUploadPrompt.setVisibility(View.GONE);
        Glide.with(this)
                .load(mainImageUrl)
                .placeholder(R.drawable.ic_logo_cookup)
                .into(imgRecipePreview);
    }

    private void saveRecipeToFirestore() {
        String name = edtRecipeName.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String cookTimeStr = edtCookTime.getText().toString().trim();
        String servingsStr = edtServings.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            edtRecipeName.setError("Vui lòng nhập tên món ăn");
            edtRecipeName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(cookTimeStr)) {
            edtCookTime.setError("Vui lòng nhập thời gian nấu");
            edtCookTime.requestFocus();
            return;
        }

        int cookTime = 0;
        try {
            cookTime = Integer.parseInt(cookTimeStr);
        } catch (NumberFormatException e) {
            // Ignored
        }

        int servings = 2;
        if (!TextUtils.isEmpty(servingsStr)) {
            try {
                servings = Integer.parseInt(servingsStr);
            } catch (NumberFormatException e) {
                // Keep 2
            }
        }

        // Fetch Dynamic Ingredients List
        List<Ingredient> ingredientsList = new ArrayList<>();
        for (int i = 0; i < containerIngredients.getChildCount(); i++) {
            View child = containerIngredients.getChildAt(i);
            EditText edtQty = child.findViewById(R.id.edtIngredientQty);
            EditText edtIngName = child.findViewById(R.id.edtIngredientName);
            if (edtQty != null && edtIngName != null) {
                String qty = edtQty.getText().toString().trim();
                String ingName = edtIngName.getText().toString().trim();
                if (!TextUtils.isEmpty(ingName)) {
                    ingredientsList.add(new Ingredient(qty, ingName));
                }
            }
        }

        // Fetch Dynamic Steps List
        List<RecipeStep> stepsList = new ArrayList<>();
        for (int i = 0; i < containerSteps.getChildCount(); i++) {
            View child = containerSteps.getChildAt(i);
            EditText edtInst = child.findViewById(R.id.edtStepInstructions);
            FrameLayout boxStepImg = child.findViewById(R.id.boxStepImage);
            if (edtInst != null && boxStepImg != null) {
                String instructions = edtInst.getText().toString().trim();
                String stepImgUrl = (String) boxStepImg.getTag();
                if (!TextUtils.isEmpty(instructions)) {
                    stepsList.add(new RecipeStep(i + 1, instructions, stepImgUrl));
                }
            }
        }

        // Create detailed recipe
        int id = Math.abs(UUID.randomUUID().hashCode());
        Recipe recipe = new Recipe(id, name, cookTime, 5.0, 320, mainImageUrl);
        recipe.setDescription(description);
        recipe.setServings(servings);
        recipe.setDifficulty(selectedDifficulty);
        recipe.setCountry(selectedCountry);
        recipe.setTags(selectedTags);
        recipe.setIngredients(ingredientsList);
        recipe.setSteps(stepsList);

        // De-activate save button during upload
        btnSaveRecipe.setEnabled(false);
        btnSaveRecipe.setText("Đang gửi...");

        FirebaseFirestore.getInstance().collection("recipes")
                .document(String.valueOf(id))
                .set(recipe)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddRecipeActivity.this, "Đăng công thức nấu ăn thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveRecipe.setEnabled(true);
                    btnSaveRecipe.setText("Gửi công thức");
                    Toast.makeText(AddRecipeActivity.this, "Lỗi khi lưu lên Cloud: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
