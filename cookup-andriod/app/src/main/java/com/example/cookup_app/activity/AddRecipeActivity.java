package com.example.cookup_app.activity;

import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
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
    private EditText edtCalories;

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
    private TextView chipCountryOther;
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
    private TextView tvHeaderTitle;

    // Image Pickers & Launchers
    private ActivityResultLauncher<Intent> mainImagePickerLauncher;
    private ActivityResultLauncher<Intent> stepImagePickerLauncher;
    private FrameLayout activeStepImageBox = null;
    private ImageView activeStepImagePreview = null;
    private ImageView activeStepCameraIcon = null;

    // Edit Mode State
    private Recipe editingRecipe = null;
    private boolean isEditMode = false;

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
                            try {
                                getContentResolver().takePersistableUriPermission(
                                        selectedImageUri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            String localPath = com.example.cookup_app.utils.RecipeDataHelper.copyUriToInternalFile(AddRecipeActivity.this, selectedImageUri, "recipe_main");
                            mainImageUrl = (localPath != null) ? localPath : selectedImageUri.toString();
                            imgRecipePreview.setVisibility(View.VISIBLE);
                            layoutUploadPrompt.setVisibility(View.GONE);
                            Glide.with(AddRecipeActivity.this)
                                    .load(mainImageUrl)
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
                            try {
                                getContentResolver().takePersistableUriPermission(
                                        selectedImageUri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            String localPath = com.example.cookup_app.utils.RecipeDataHelper.copyUriToInternalFile(AddRecipeActivity.this, selectedImageUri, "recipe_step");
                            String finalPath = (localPath != null) ? localPath : selectedImageUri.toString();
                            activeStepImageBox.setTag(finalPath);
                            if (activeStepImagePreview != null) {
                                activeStepImagePreview.setVisibility(View.VISIBLE);
                                Glide.with(AddRecipeActivity.this)
                                        .load(finalPath)
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
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle); // Note: Should add ID to XML or find by position
        edtRecipeName = findViewById(R.id.edtRecipeName);
        edtDescription = findViewById(R.id.edtDescription);
        edtCookTime = findViewById(R.id.edtCookTime);
        edtServings = findViewById(R.id.edtServings);
        edtCalories = findViewById(R.id.edtCalories);

        btnDifficultyEasy = findViewById(R.id.btnDifficultyEasy);
        btnDifficultyMedium = findViewById(R.id.btnDifficultyMedium);
        btnDifficultyHard = findViewById(R.id.btnDifficultyHard);

        chipCountryVN = findViewById(R.id.chipCountryVN);
        chipCountryKR = findViewById(R.id.chipCountryKR);
        chipCountryJP = findViewById(R.id.chipCountryJP);
        chipCountryEU = findViewById(R.id.chipCountryEU);
        chipCountryOther = findViewById(R.id.chipCountryOther);

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

        // Check for Edit Mode
        editingRecipe = (Recipe) getIntent().getSerializableExtra("edit_recipe");
        if (editingRecipe != null) {
            isEditMode = true;
            fillFieldsForEditing();
        } else {
            // Initialize with at least one empty ingredient row and one step row by default
            addIngredientRow("", "");
            addStepRow("", 0);
        }

        // Navigation
        btnBackFrame.setOnClickListener(v -> finish());
        btnMore.setOnClickListener(v -> {
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, btnMore);
            popup.getMenu().add("Tự động điền mẫu");
            popup.getMenu().add("Xóa toàn bộ");
            popup.setOnMenuItemClickListener(item -> {
                if ("Tự động điền mẫu".equals(item.getTitle())) {
                    prefillSampleData();
                } else if ("Xóa toàn bộ".equals(item.getTitle())) {
                    clearAllFields();
                }
                return true;
            });
            popup.show();
        });
        btnSaveRecipe.setOnClickListener(v -> saveRecipeToFirestore());
    }

    private void fillFieldsForEditing() {
        if (editingRecipe == null) return;

        // Header and Button text
        if (tvHeaderTitle != null) tvHeaderTitle.setText("Chỉnh sửa công thức");
        btnSaveRecipe.setText("Cập nhật công thức");

        // Basic Info
        edtRecipeName.setText(editingRecipe.getName());
        edtDescription.setText(editingRecipe.getDescription());
        edtCookTime.setText(String.valueOf(editingRecipe.getCookTimeMinutes()));
        edtServings.setText(String.valueOf(editingRecipe.getServings()));
        edtCalories.setText(String.valueOf(editingRecipe.getCalories()));

        // Difficulty
        selectedDifficulty = editingRecipe.getDifficulty();
        updateDifficultyUI();

        // Country
        selectedCountry = editingRecipe.getCountry();
        updateCountryUI();

        // Tags
        if (editingRecipe.getTags() != null) {
            for (String tag : editingRecipe.getTags()) {
                addTagToLayout(tag);
            }
        }

        // Ingredients
        containerIngredients.removeAllViews();
        if (editingRecipe.getIngredients() != null) {
            for (Ingredient ing : editingRecipe.getIngredients()) {
                addIngredientRow(ing.getQty(), ing.getName());
            }
        }
        if (containerIngredients.getChildCount() == 0) addIngredientRow("", "");

        // Steps
        containerSteps.removeAllViews();
        if (editingRecipe.getSteps() != null) {
            for (RecipeStep step : editingRecipe.getSteps()) {
                addStepRow(step.getInstructions(), step.getDurationSeconds() / 60, step.getImageUrl());
            }
        }
        if (containerSteps.getChildCount() == 0) addStepRow("", 0);

        // Main Image
        mainImageUrl = editingRecipe.getImageUrl();
        if (!TextUtils.isEmpty(mainImageUrl)) {
            imgRecipePreview.setVisibility(View.VISIBLE);
            layoutUploadPrompt.setVisibility(View.GONE);
            Glide.with(this)
                    .load(mainImageUrl)
                    .placeholder(R.drawable.character_chef_1)
                    .into(imgRecipePreview);
        }
    }

    private void updateDifficultyUI() {
        // Clear styles
        btnDifficultyEasy.setBackgroundColor(Color.TRANSPARENT);
        btnDifficultyEasy.setTextColor(Color.parseColor("#8F756C"));
        btnDifficultyMedium.setBackgroundColor(Color.TRANSPARENT);
        btnDifficultyMedium.setTextColor(Color.parseColor("#8F756C"));
        btnDifficultyHard.setBackgroundColor(Color.TRANSPARENT);
        btnDifficultyHard.setTextColor(Color.parseColor("#8F756C"));

        if ("Dễ".equalsIgnoreCase(selectedDifficulty)) {
            btnDifficultyEasy.setBackgroundResource(R.drawable.bg_segment_active);
            btnDifficultyEasy.setTextColor(Color.parseColor("#FFF1ED"));
        } else if ("Trung bình".equalsIgnoreCase(selectedDifficulty)) {
            btnDifficultyMedium.setBackgroundResource(R.drawable.bg_segment_active);
            btnDifficultyMedium.setTextColor(Color.parseColor("#FFF1ED"));
        } else if ("Khó".equalsIgnoreCase(selectedDifficulty)) {
            btnDifficultyHard.setBackgroundResource(R.drawable.bg_segment_active);
            btnDifficultyHard.setTextColor(Color.parseColor("#FFF1ED"));
        }
    }

    private void updateCountryUI() {
        chipCountryVN.setBackgroundResource(R.drawable.bg_chip_unselected);
        chipCountryVN.setTextColor(Color.parseColor("#FFF1ED"));
        chipCountryKR.setBackgroundResource(R.drawable.bg_chip_unselected);
        chipCountryKR.setTextColor(Color.parseColor("#FFF1ED"));
        chipCountryJP.setBackgroundResource(R.drawable.bg_chip_unselected);
        chipCountryJP.setTextColor(Color.parseColor("#FFF1ED"));
        chipCountryEU.setBackgroundResource(R.drawable.bg_chip_unselected);
        chipCountryEU.setTextColor(Color.parseColor("#FFF1ED"));
        chipCountryOther.setBackgroundResource(R.drawable.bg_chip_unselected);
        chipCountryOther.setTextColor(Color.parseColor("#FFF1ED"));

        if ("Việt Nam".equals(selectedCountry)) {
            chipCountryVN.setBackgroundResource(R.drawable.bg_chip_selected);
            chipCountryVN.setTextColor(Color.parseColor("#FF7238"));
        } else if ("Hàn Quốc".equals(selectedCountry)) {
            chipCountryKR.setBackgroundResource(R.drawable.bg_chip_selected);
            chipCountryKR.setTextColor(Color.parseColor("#FF7238"));
        } else if ("Nhật Bản".equals(selectedCountry)) {
            chipCountryJP.setBackgroundResource(R.drawable.bg_chip_selected);
            chipCountryJP.setTextColor(Color.parseColor("#FF7238"));
        } else if ("Âu".equals(selectedCountry)) {
            chipCountryEU.setBackgroundResource(R.drawable.bg_chip_selected);
            chipCountryEU.setTextColor(Color.parseColor("#FF7238"));
        } else {
            chipCountryOther.setText(selectedCountry);
            chipCountryOther.setBackgroundResource(R.drawable.bg_chip_selected);
            chipCountryOther.setTextColor(Color.parseColor("#FF7238"));
        }
    }

    private void setupDifficultySelectors() {
        View.OnClickListener clickListener = v -> {
            int id = v.getId();
            if (id == R.id.btnDifficultyEasy) selectedDifficulty = "Dễ";
            else if (id == R.id.btnDifficultyMedium) selectedDifficulty = "Trung bình";
            else if (id == R.id.btnDifficultyHard) selectedDifficulty = "Khó";
            updateDifficultyUI();
        };

        btnDifficultyEasy.setOnClickListener(clickListener);
        btnDifficultyMedium.setOnClickListener(clickListener);
        btnDifficultyHard.setOnClickListener(clickListener);
    }

    private void setupCountrySelectors() {
        View.OnClickListener clickListener = v -> {
            TextView clickedChip = (TextView) v;
            if (clickedChip == chipCountryOther) {
                showCustomCountryDialog();
            } else {
                selectedCountry = clickedChip.getText().toString();
                updateCountryUI();
            }
        };

        chipCountryVN.setOnClickListener(clickListener);
        chipCountryKR.setOnClickListener(clickListener);
        chipCountryJP.setOnClickListener(clickListener);
        chipCountryEU.setOnClickListener(clickListener);
        chipCountryOther.setOnClickListener(clickListener);
    }

    private void showCustomCountryDialog() {
        String[] countries = {"Trung Quốc", "Thái Lan", "Mỹ", "Pháp", "Ý", "Singapore", "Đài Loan", "Ấn Độ", "Khác (Nhập tay)..."};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn quốc gia 🌍");
        builder.setItems(countries, (dialog, which) -> {
            if (which == countries.length - 1) {
                showManualCountryInputDialog();
            } else {
                selectedCountry = countries[which];
                updateCountryUI();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showManualCountryInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nhập tên quốc gia");

        final EditText input = new EditText(this);
        input.setHint("Ví dụ: Singapore, Tây Ban Nha...");
        builder.setView(input);

        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            String enteredCountry = input.getText().toString().trim();
            if (!TextUtils.isEmpty(enteredCountry)) {
                selectedCountry = enteredCountry;
                updateCountryUI();
            } else {
                Toast.makeText(this, "Tên quốc gia không được trống", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
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
        btnAddStep.setOnClickListener(v -> addStepRow("", 0));
    }

    private void addStepRow(String instructions, int durationMinutes) {
        addStepRow(instructions, durationMinutes, "");
    }

    private void addStepRow(String instructions, int durationMinutes, String existingImgUrl) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View row = inflater.inflate(R.layout.item_add_step, containerSteps, false);

        TextView tvStepNum = row.findViewById(R.id.tvStepNumber);
        EditText edtInst = row.findViewById(R.id.edtStepInstructions);
        EditText edtDuration = row.findViewById(R.id.edtStepDuration);
        FrameLayout boxStepImage = row.findViewById(R.id.boxStepImage);
        ImageView imgStepPreview = row.findViewById(R.id.imgStepPreview);
        ImageView imgStepCameraIcon = row.findViewById(R.id.imgStepCameraIcon);
        ImageView btnDelete = row.findViewById(R.id.btnDeleteStep);

        edtInst.setText(instructions);
        if (durationMinutes > 0 && edtDuration != null) {
            edtDuration.setText(String.valueOf(durationMinutes));
        }

        boxStepImage.setTag(existingImgUrl);
        if (!TextUtils.isEmpty(existingImgUrl)) {
            imgStepPreview.setVisibility(View.VISIBLE);
            imgStepCameraIcon.setVisibility(View.GONE);
            Glide.with(this)
                    .load(existingImgUrl)
                    .placeholder(R.drawable.character_chef_1)
                    .into(imgStepPreview);
        }

        boxStepImage.setOnClickListener(v -> {
            activeStepImageBox = boxStepImage;
            activeStepImagePreview = imgStepPreview;
            activeStepCameraIcon = imgStepCameraIcon;

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            stepImagePickerLauncher.launch(intent);
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
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            mainImagePickerLauncher.launch(intent);
        });
    }

    private void prefillSampleData() {
        containerIngredients.removeAllViews();
        containerSteps.removeAllViews();

        edtRecipeName.setText("Phở Bò Nam Định");
        edtDescription.setText("Món phở truyền thống đậm đà bản sắc Việt với nước dùng từ xương bò ninh nhừ thơm lừng quế hồi.");
        edtCookTime.setText("45");
        edtServings.setText("4");
        if (edtCalories != null) edtCalories.setText("420");

        addTagToLayout("Món mặn");
        addTagToLayout("Ăn tối");

        addIngredientRow("500g", "Thịt bò thăn");
        addIngredientRow("1 củ", "Hành tây");

        addStepRow("Thái thịt bò mỏng, ướp với chút muối, tiêu và tỏi băm trong 15 phút.", 15);
        addStepRow("Phi thơm tỏi, cho thịt bò vào xào nhanh trên lửa lớn cho tái rồi trút ra.", 5);

        mainImageUrl = "https://images.unsplash.com/photo-1583085204876-0f86a9f829ec?q=80&w=1000&auto=format&fit=crop";
        imgRecipePreview.setVisibility(View.VISIBLE);
        layoutUploadPrompt.setVisibility(View.GONE);
        Glide.with(this)
                .load(mainImageUrl)
                .placeholder(R.drawable.ic_logo_cookup)
                .into(imgRecipePreview);
    }

    private void clearAllFields() {
        edtRecipeName.setText("");
        edtDescription.setText("");
        edtCookTime.setText("");
        edtServings.setText("");
        if (edtCalories != null) edtCalories.setText("");
        containerIngredients.removeAllViews();
        containerSteps.removeAllViews();
        addIngredientRow("", "");
        addStepRow("", 0);
        mainImageUrl = "";
        imgRecipePreview.setVisibility(View.GONE);
        layoutUploadPrompt.setVisibility(View.VISIBLE);
    }

    private void saveRecipeToFirestore() {
        String name = edtRecipeName.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String cookTimeStr = edtCookTime.getText().toString().trim();
        String servingsStr = edtServings.getText().toString().trim();
        String caloriesStr = edtCalories != null ? edtCalories.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            edtRecipeName.setError("Vui lòng nhập tên món ăn");
            edtRecipeName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            edtDescription.setError("Vui lòng nhập mô tả chi tiết");
            edtDescription.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(cookTimeStr)) {
            edtCookTime.setError("Vui lòng nhập thời gian nấu");
            edtCookTime.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(servingsStr)) {
            edtServings.setError("Vui lòng nhập số phần ăn");
            edtServings.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(mainImageUrl)) {
            Toast.makeText(this, "Vui lòng chọn ảnh cho món ăn!", Toast.LENGTH_SHORT).show();
            return;
        }

        int id = isEditMode ? editingRecipe.getId() : Math.abs(UUID.randomUUID().hashCode());
        Recipe recipe = new Recipe(id, name, Integer.parseInt(cookTimeStr), 
                isEditMode ? editingRecipe.getRating() : 0.0, 
                TextUtils.isEmpty(caloriesStr) ? 350 : Integer.parseInt(caloriesStr), 
                mainImageUrl);
        
        recipe.setDescription(description);
        recipe.setServings(Integer.parseInt(servingsStr));
        recipe.setDifficulty(selectedDifficulty);
        recipe.setCountry(selectedCountry);
        recipe.setTags(selectedTags);

        List<Ingredient> ingredientsList = new ArrayList<>();
        for (int i = 0; i < containerIngredients.getChildCount(); i++) {
            View child = containerIngredients.getChildAt(i);
            EditText eq = child.findViewById(R.id.edtIngredientQty);
            EditText en = child.findViewById(R.id.edtIngredientName);
            if (en != null && !TextUtils.isEmpty(en.getText().toString().trim())) {
                ingredientsList.add(new Ingredient(eq.getText().toString().trim(), en.getText().toString().trim()));
            }
        }
        recipe.setIngredients(ingredientsList);

        List<RecipeStep> stepsList = new ArrayList<>();
        for (int i = 0; i < containerSteps.getChildCount(); i++) {
            View child = containerSteps.getChildAt(i);
            EditText ei = child.findViewById(R.id.edtStepInstructions);
            EditText ed = child.findViewById(R.id.edtStepDuration);
            FrameLayout box = child.findViewById(R.id.boxStepImage);
            if (ei != null && !TextUtils.isEmpty(ei.getText().toString().trim())) {
                int dur = 0;
                try { dur = Integer.parseInt(ed.getText().toString().trim()) * 60; } catch (Exception ignored) {}
                stepsList.add(new RecipeStep(i + 1, ei.getText().toString().trim(), (String) box.getTag(), dur));
            }
        }
        recipe.setSteps(stepsList);

        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (isEditMode) {
            recipe.setCreatorUid(editingRecipe.getCreatorUid());
            recipe.setChefName(editingRecipe.getChefName());
        } else {
            recipe.setCreatorUid(user != null ? user.getUid() : "anonymous");
            SharedPreferences prefs = getSharedPreferences("cookup_prefs", Context.MODE_PRIVATE);
            recipe.setChefName(prefs.getString("custom_user_name", "Đầu bếp CookUp"));
        }

        btnSaveRecipe.setEnabled(false);
        btnSaveRecipe.setText("Đang xử lý...");
        uploadImagesAndSaveRecipe(recipe);
    }

    private void uploadImagesAndSaveRecipe(Recipe recipe) {
        if (mainImageUrl.startsWith("content://") || mainImageUrl.startsWith("file://")) {
            btnSaveRecipe.setText("Đang tải ảnh chính...");
            uploadSingleImage(Uri.parse(mainImageUrl), "recipe_main", new UploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    recipe.setImageUrl(downloadUrl);
                    uploadStepImages(recipe, 0);
                }
                @Override
                public void onFailure(Exception e) {
                    btnSaveRecipe.setEnabled(true);
                    Toast.makeText(AddRecipeActivity.this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            uploadStepImages(recipe, 0);
        }
    }

    private void uploadStepImages(Recipe recipe, int index) {
        List<RecipeStep> steps = recipe.getSteps();
        if (steps == null || index >= steps.size()) {
            saveRecipeObjectToFirestore(recipe);
            return;
        }

        RecipeStep step = steps.get(index);
        String url = step.getImageUrl();
        if (url != null && (url.startsWith("content://") || url.startsWith("file://"))) {
            btnSaveRecipe.setText("Tải ảnh bước " + (index + 1) + "...");
            uploadSingleImage(Uri.parse(url), "recipe_step", new UploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    step.setImageUrl(downloadUrl);
                    uploadStepImages(recipe, index + 1);
                }
                @Override
                public void onFailure(Exception e) {
                    uploadStepImages(recipe, index + 1); // Skip failed image and continue
                }
            });
        } else {
            uploadStepImages(recipe, index + 1);
        }
    }

    private interface UploadCallback {
        void onSuccess(String url);
        void onFailure(Exception e);
    }

    private void uploadSingleImage(Uri uri, String prefix, UploadCallback callback) {
        com.example.cookup_app.utils.CloudinaryUploader.uploadImage(this, uri, new com.example.cookup_app.utils.CloudinaryUploader.UploadCallback() {
            @Override public void onSuccess(String url) { callback.onSuccess(url); }
            @Override public void onFailure(Exception e) { callback.onFailure(e); }
        });
    }

    private void saveRecipeObjectToFirestore(Recipe recipe) {
        btnSaveRecipe.setText("Đang lưu...");
        FirebaseFirestore.getInstance().collection("recipes")
                .document(String.valueOf(recipe.getId()))
                .set(recipe)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, isEditMode ? "Cập nhật thành công!" : "Đăng công thức thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveRecipe.setEnabled(true);
                    Toast.makeText(this, "Lỗi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
