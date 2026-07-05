package com.example.cookup_app.activity;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.example.cookup_app.R;
import com.example.cookup_app.model.Ingredient;
import com.example.cookup_app.model.Recipe;
import com.example.cookup_app.model.RecipeStep;
import com.example.cookup_app.utils.ThemeManager;

import java.util.List;

public class CookingModeActivity extends AppCompatActivity {

    private Recipe recipe;
    private List<RecipeStep> steps;
    private int currentStepIndex = 0;

    // Timer state
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private int totalSeconds = 120;
    private int remainingSeconds = 120;

    // Wake Lock state
    private boolean isKeepScreenOn = false;

    // UI elements
    private TextView tvCookingStepProgress;
    private LinearProgressIndicator cookingProgressBar;
    private TextView tvCookingInstructions;

    private View layoutTimerCircle;
    private CircularProgressIndicator timerProgressIndicator;
    private TextView tvCookingTimer;
    private TextView tvTimerState;

    private LinearLayout containerStepIngredients;
    private View btnCookingPrev;
    private View btnCookingNext;

    private View btnCookingAwake;
    private ImageView imgAwakeIcon;
    private TextView tvAwakeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cooking_mode);

        // Retrieve recipe
        recipe = (Recipe) getIntent().getSerializableExtra("recipe");
        if (recipe == null || recipe.getSteps() == null || recipe.getSteps().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy dữ liệu bước nấu!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        steps = recipe.getSteps();

        // Bind UI Elements
        tvCookingStepProgress = findViewById(R.id.tvCookingStepProgress);
        cookingProgressBar = findViewById(R.id.cookingProgressBar);
        tvCookingInstructions = findViewById(R.id.tvCookingInstructions);

        layoutTimerCircle = findViewById(R.id.layoutTimerCircle);
        timerProgressIndicator = findViewById(R.id.timerProgressIndicator);
        tvCookingTimer = findViewById(R.id.tvCookingTimer);
        tvTimerState = findViewById(R.id.tvTimerState);

        containerStepIngredients = findViewById(R.id.containerStepIngredients);
        btnCookingPrev = findViewById(R.id.btnCookingPrev);
        btnCookingNext = findViewById(R.id.btnCookingNext);

        btnCookingAwake = findViewById(R.id.btnCookingAwake);
        imgAwakeIcon = findViewById(R.id.imgAwakeIcon);
        tvAwakeText = findViewById(R.id.tvAwakeText);

        // Header and back/close
        findViewById(R.id.btnCookingClose).setOnClickListener(v -> finish());

        // Wake Lock Toggle
        btnCookingAwake.setOnClickListener(v -> toggleKeepScreenOn());

        // Timer action triggers
        layoutTimerCircle.setOnClickListener(v -> {
            if (isTimerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        // Steps navigation click listeners
        btnCookingPrev.setOnClickListener(v -> {
            if (currentStepIndex > 0) {
                currentStepIndex--;
                loadStep(currentStepIndex);
            }
        });

        btnCookingNext.setOnClickListener(v -> {
            if (currentStepIndex < steps.size() - 1) {
                currentStepIndex++;
                loadStep(currentStepIndex);
            } else {
                // Last step, finish! Show congratulations
                showSuccessDialog();
            }
        });

        // Load the very first step
        loadStep(0);
    }

    private void loadStep(int index) {
        // Stop any active countdown timer
        pauseTimer();

        RecipeStep step = steps.get(index);

        // Progress indicators
        int stepNum = index + 1;
        tvCookingStepProgress.setText("Bước " + stepNum + " / " + steps.size());
        
        int percentProgress = (int) (((double) stepNum / steps.size()) * 100);
        cookingProgressBar.setProgress(percentProgress);

        // Instruction Text
        tvCookingInstructions.setText(step.getInstructions());

        // Prepare timer duration
        totalSeconds = step.getDurationSeconds() > 0 ? step.getDurationSeconds() : 120; // Default 2 minutes
        remainingSeconds = totalSeconds;
        updateTimerText();

        // Setup Step Ingredients chips
        containerStepIngredients.removeAllViews();
        List<Ingredient> stepIngs = step.getStepIngredients();
        if (stepIngs != null && !stepIngs.isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(this);
            for (Ingredient ing : stepIngs) {
                View chip = inflater.inflate(R.layout.chip_cooking_ingredient, containerStepIngredients, false);
                TextView tvChipText = chip.findViewById(R.id.tvChipIngredientText);
                tvChipText.setText(ing.getName() + " (" + ing.getQty() + ")");
                containerStepIngredients.addView(chip);
            }
        } else {
            // Fallback: If step ingredients is empty, show default notification chip
            LayoutInflater inflater = LayoutInflater.from(this);
            View chip = inflater.inflate(R.layout.chip_cooking_ingredient, containerStepIngredients, false);
            TextView tvChipText = chip.findViewById(R.id.tvChipIngredientText);
            tvChipText.setText("Không có nguyên liệu phụ trợ riêng lẻ");
            containerStepIngredients.addView(chip);
        }

        // Adjust navigation button styling
        if (index == 0) {
            btnCookingPrev.setVisibility(View.INVISIBLE);
        } else {
            btnCookingPrev.setVisibility(View.VISIBLE);
        }

        if (index == steps.size() - 1) {
            TextView btnText = (TextView) btnCookingNext;
            btnText.setText("Hoàn thành ✓");
        } else {
            TextView btnText = (TextView) btnCookingNext;
            btnText.setText("Tiếp theo ➔");
        }
    }

    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        timerProgressIndicator.setMax(totalSeconds);

        countDownTimer = new CountDownTimer(remainingSeconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingSeconds = (int) (millisUntilFinished / 1000);
                updateTimerText();
            }

            @Override
            public void onFinish() {
                remainingSeconds = 0;
                updateTimerText();
                isTimerRunning = false;
                tvTimerState.setText("Đã hoàn thành!");
                Toast.makeText(CookingModeActivity.this, "Hết giờ! Chuyển sang bước tiếp theo nào!", Toast.LENGTH_LONG).show();
            }
        }.start();

        isTimerRunning = true;
        tvTimerState.setText("Chạm để tạm dừng");
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        tvTimerState.setText("Chạm để tiếp tục");
    }

    private void updateTimerText() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        String timeFormatted = String.format("%02d:%02d", minutes, seconds);
        tvCookingTimer.setText(timeFormatted);
        timerProgressIndicator.setProgress(remainingSeconds);
    }

    private void toggleKeepScreenOn() {
        isKeepScreenOn = !isKeepScreenOn;
        if (isKeepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            btnCookingAwake.setBackgroundColor(getResources().getColor(R.color.orange_primary));
            imgAwakeIcon.setColorFilter(Color.WHITE);
            tvAwakeText.setTextColor(Color.WHITE);
            Toast.makeText(this, "Màn hình luôn sáng đã được kích hoạt!", Toast.LENGTH_SHORT).show();
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            btnCookingAwake.setBackgroundColor(Color.parseColor("#CC1E1410"));
            imgAwakeIcon.setColorFilter(getResources().getColor(R.color.text_secondary));
            tvAwakeText.setTextColor(getResources().getColor(R.color.text_secondary));
            Toast.makeText(this, "Đã tắt chế độ giữ sáng màn hình.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSuccessDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_cooking_success);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogSuccessTitle);
        TextView tvMsg = dialog.findViewById(R.id.tvDialogSuccessMessage);
        View btnClose = dialog.findViewById(R.id.btnDialogSuccessClose);

        tvTitle.setText("Chúc mừng Đầu Bếp! 🎉");
        tvMsg.setText("Bạn đã hoàn thành xuất sắc món ăn \"" + recipe.getName() + "\". Hãy tận hưởng thành quả ngọt ngào cùng những người thân yêu nhé!");

        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            finish(); // return to details page
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
