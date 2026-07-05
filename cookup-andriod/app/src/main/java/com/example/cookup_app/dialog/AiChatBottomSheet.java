package com.example.cookup_app.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cookup_app.BuildConfig;
import com.example.cookup_app.R;
import com.example.cookup_app.activity.RecipeDetailActivity;
import com.example.cookup_app.model.Recipe;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiChatBottomSheet extends BottomSheetDialogFragment {

    private ScrollView scrollAiChatMessages;
    private LinearLayout containerAiChatMessages;
    private LinearLayout containerAiIngredientChips;
    private EditText edtAiChatInput;
    private View btnAiChatSend;
    private View btnAiAddIngredientDirect;

    private final List<String> currentIngredients = new ArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_ai_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind Views
        scrollAiChatMessages = view.findViewById(R.id.scrollAiChatMessages);
        containerAiChatMessages = view.findViewById(R.id.containerAiChatMessages);
        containerAiIngredientChips = view.findViewById(R.id.containerAiIngredientChips);
        edtAiChatInput = view.findViewById(R.id.edtAiChatInput);
        btnAiChatSend = view.findViewById(R.id.btnAiChatSend);
        btnAiAddIngredientDirect = view.findViewById(R.id.btnAiAddIngredientDirect);

        // Close action
        view.findViewById(R.id.btnAiChatClose).setOnClickListener(v -> dismiss());

        // Quick Input suggestion tags setup
        setupSuggestedChips();

        // Welcome message
        addAiMessageBubble("Xin chào! Tôi là Trợ lý nấu ăn CookUp. Bạn đang có những nguyên liệu gì trong tủ lạnh thế? Hãy chọn nhanh bên dưới hoặc nhập nguyên liệu nhé! 🍳", null);

        // Input Actions
        edtAiChatInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                handleSendAction();
                return true;
            }
            return false;
        });

        btnAiChatSend.setOnClickListener(v -> handleSendAction());

        // Add ingredient directly on plus icon click
        btnAiAddIngredientDirect.setOnClickListener(v -> {
            String input = edtAiChatInput.getText().toString().trim();
            if (!TextUtils.isEmpty(input)) {
                addIngredientTag(input);
                edtAiChatInput.setText("");
            } else {
                Toast.makeText(getContext(), "Vui lòng nhập tên nguyên liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        // Force wrap_content height behavior but let it slide correctly
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                ViewGroup.LayoutParams layoutParams = bottomSheetInternal.getLayoutParams();
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheetInternal.setLayoutParams(layoutParams);
            }
        });
        return dialog;
    }

    private void setupSuggestedChips() {
        String[] suggestions = {"Thịt heo 🥩", "Cà chua 🍅", "Trứng 🥚", "Bánh mì 🥖", "Tôm 🍤"};
        for (String item : suggestions) {
            addSuggestionChipToContainer(item);
        }
    }

    private void addSuggestionChipToContainer(String name) {
        if (getContext() == null) return;
        TextView tvChip = new TextView(getContext());
        tvChip.setText(name);
        tvChip.setTextColor(getResources().getColor(R.color.text_primary));
        tvChip.setTextSize(13);
        tvChip.setBackgroundResource(R.drawable.bg_chip_unselected);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 16, 0);
        tvChip.setLayoutParams(params);
        
        int py = (int) (6 * getResources().getDisplayMetrics().density);
        int px = (int) (12 * getResources().getDisplayMetrics().density);
        tvChip.setPadding(px, py, px, py);
        
        tvChip.setClickable(true);
        tvChip.setFocusable(true);
        tvChip.setOnClickListener(v -> {
            containerAiIngredientChips.removeView(tvChip);
            addIngredientTag(name);
        });

        containerAiIngredientChips.addView(tvChip);
    }

    private void addIngredientTag(String rawName) {
        if (getContext() == null) return;
        String name = rawName.trim();
        if (currentIngredients.contains(name)) {
            return;
        }
        currentIngredients.add(name);

        TextView tvChip = new TextView(getContext());
        tvChip.setText(name + "  ✕");
        tvChip.setTextColor(getResources().getColor(R.color.text_primary));
        tvChip.setTextSize(13);
        tvChip.setBackgroundResource(R.drawable.bg_chip_selected);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 16, 0);
        tvChip.setLayoutParams(params);
        
        int py = (int) (6 * getResources().getDisplayMetrics().density);
        int px = (int) (12 * getResources().getDisplayMetrics().density);
        tvChip.setPadding(px, py, px, py);

        tvChip.setClickable(true);
        tvChip.setFocusable(true);
        tvChip.setOnClickListener(v -> {
            containerAiIngredientChips.removeView(tvChip);
            currentIngredients.remove(name);
            addSuggestionChipToContainer(name);
        });

        containerAiIngredientChips.addView(tvChip, 0);
    }

    private void handleSendAction() {
        String userQuery = edtAiChatInput.getText().toString().trim();
        if (TextUtils.isEmpty(userQuery) && currentIngredients.isEmpty()) {
            Toast.makeText(getContext(), "Hãy nhập nguyên liệu hoặc nội dung câu hỏi!", Toast.LENGTH_SHORT).show();
            return;
        }

        edtAiChatInput.setText("");

        // Build elegant query message
        StringBuilder queryText = new StringBuilder();
        if (!currentIngredients.isEmpty()) {
            queryText.append("Tôi đang có: ");
            for (int i = 0; i < currentIngredients.size(); i++) {
                queryText.append(currentIngredients.get(i));
                if (i < currentIngredients.size() - 1) queryText.append(", ");
            }
            queryText.append(". ");
        }
        if (!TextUtils.isEmpty(userQuery)) {
            queryText.append(userQuery);
        } else {
            queryText.append("Gợi ý cho tôi món ăn ngon và phù hợp nhé!");
        }

        // 1. Display User Message
        addUserMessageBubble(queryText.toString());

        // 2. Add AI Typing Bubble
        final View aiBubbleView = addAiMessageBubble("Đang suy nghĩ... 🍳", null);

        // 3. Trigger API Call
        executeGeminiQuery(queryText.toString(), aiBubbleView);
    }

    private void addUserMessageBubble(String text) {
        View bubble = LayoutInflater.from(getContext()).inflate(R.layout.item_ai_chat_bubble_user, containerAiChatMessages, false);
        TextView tvUserMsg = bubble.findViewById(R.id.tvUserMsg);
        tvUserMsg.setText(text);
        containerAiChatMessages.addView(bubble);
        scrollToBottom();
    }

    private View addAiMessageBubble(String text, @Nullable List<Recipe> suggestions) {
        View bubble = LayoutInflater.from(getContext()).inflate(R.layout.item_ai_chat_bubble_ai, containerAiChatMessages, false);
        TextView tvAiMsg = bubble.findViewById(R.id.tvAiMsg);
        tvAiMsg.setText(text);

        if (suggestions != null && !suggestions.isEmpty()) {
            View scrollSuggestions = bubble.findViewById(R.id.scrollAiSuggestions);
            LinearLayout containerSuggestions = bubble.findViewById(R.id.containerAiSuggestions);
            scrollSuggestions.setVisibility(View.VISIBLE);

            for (Recipe r : suggestions) {
                View card = LayoutInflater.from(getContext()).inflate(R.layout.item_ai_recipe_suggestion_card, containerSuggestions, false);
                ImageView img = card.findViewById(R.id.imgAiRecipeSuggestion);
                TextView tvMatch = card.findViewById(R.id.tvAiRecipeSuggestionMatch);
                TextView tvName = card.findViewById(R.id.tvAiRecipeSuggestionName);
                TextView tvInfo = card.findViewById(R.id.tvAiRecipeSuggestionInfo);
                View btnView = card.findViewById(R.id.btnAiRecipeSuggestionView);

                tvName.setText(r.getName());
                tvInfo.setText(r.getDifficulty() + " • " + r.getCookTimeMinutes() + " phút");
                tvMatch.setText("Phù hợp 95%");

                if (r.getImageResId() != 0) {
                    img.setImageResource(r.getImageResId());
                } else {
                    img.setImageResource(R.drawable.character_chef_1);
                }

                btnView.setOnClickListener(v -> {
                    dismiss();
                    Intent intent = new Intent(getActivity(), RecipeDetailActivity.class);
                    intent.putExtra("recipe", r);
                    startActivity(intent);
                });

                containerSuggestions.addView(card);
            }
        }

        containerAiChatMessages.addView(bubble);
        scrollToBottom();
        return bubble;
    }

    private void updateAiMessageBubble(View bubbleView, String text, @Nullable List<Recipe> suggestions) {
        TextView tvAiMsg = bubbleView.findViewById(R.id.tvAiMsg);
        tvAiMsg.setText(text);

        if (suggestions != null && !suggestions.isEmpty()) {
            View scrollSuggestions = bubbleView.findViewById(R.id.scrollAiSuggestions);
            LinearLayout containerSuggestions = bubbleView.findViewById(R.id.containerAiSuggestions);
            containerSuggestions.removeAllViews();
            scrollSuggestions.setVisibility(View.VISIBLE);

            for (Recipe r : suggestions) {
                View card = LayoutInflater.from(getContext()).inflate(R.layout.item_ai_recipe_suggestion_card, containerSuggestions, false);
                ImageView img = card.findViewById(R.id.imgAiRecipeSuggestion);
                TextView tvMatch = card.findViewById(R.id.tvAiRecipeSuggestionMatch);
                TextView tvName = card.findViewById(R.id.tvAiRecipeSuggestionName);
                TextView tvInfo = card.findViewById(R.id.tvAiRecipeSuggestionInfo);
                View btnView = card.findViewById(R.id.btnAiRecipeSuggestionView);

                tvName.setText(r.getName());
                tvInfo.setText(r.getDifficulty() + " • " + r.getCookTimeMinutes() + " phút");
                tvMatch.setText("Phù hợp 95%");

                if (r.getImageResId() != 0) {
                    img.setImageResource(r.getImageResId());
                } else {
                    img.setImageResource(R.drawable.character_chef_2);
                }

                btnView.setOnClickListener(v -> {
                    dismiss();
                    Intent intent = new Intent(getActivity(), RecipeDetailActivity.class);
                    intent.putExtra("recipe", r);
                    startActivity(intent);
                });

                containerSuggestions.addView(card);
            }
        }
        scrollToBottom();
    }

    private void executeGeminiQuery(String userQuery, View aiBubbleView) {
        String apiKey = BuildConfig.GEMINI_API_KEY;

        // Check if there is a real API key configured
        if (TextUtils.isEmpty(apiKey) || "MY_GEMINI_API_KEY".equals(apiKey)) {
            // Fall back to rule-based mock engine with delay
            mainHandler.postDelayed(() -> {
                LocalRecommendation result = runLocalRecommendationEngine(userQuery);
                updateAiMessageBubble(aiBubbleView, result.message, result.recipes);
            }, 1200);
            return;
        }

        executorService.execute(() -> {
            try {
                String systemPrompt = "Bạn là Trợ lý nấu ăn CookUp thông minh và thân thiện. " +
                        "Nhiệm vụ của bạn là đưa ra các gợi ý món ăn ngon dựa trên nguyên liệu của người dùng, " +
                        "và tư vấn ẩm thực súc tích, ngắn gọn bằng Tiếng Việt. " +
                        "Hãy tư vấn một cách nhiệt tình.";

                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Build Request Body
                JSONObject requestBody = new JSONObject();
                JSONArray contentsArray = new JSONArray();
                JSONObject contentObj = new JSONObject();
                JSONArray partsArray = new JSONArray();

                // System Instruction block
                JSONObject systemPart = new JSONObject();
                systemPart.put("text", systemPrompt + "\n\nUser query: " + userQuery);
                partsArray.put(systemPart);

                contentObj.put("parts", partsArray);
                contentsArray.put(contentObj);
                requestBody.put("contents", contentsArray);

                // Send request
                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes("utf-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line.trim());
                    }
                    br.close();

                    // Parse JSON response
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray candidates = jsonResponse.getJSONArray("candidates");
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    String aiText = parts.getJSONObject(0).getString("text");

                    // Map matching recipes
                    List<Recipe> matchingRecipes = matchSampleRecipesForQuery(userQuery);

                    mainHandler.post(() -> {
                        updateAiMessageBubble(aiBubbleView, aiText, matchingRecipes);
                    });

                } else {
                    // Fall back to rule-based engine on HTTP failure
                    mainHandler.post(() -> {
                        LocalRecommendation result = runLocalRecommendationEngine(userQuery);
                        updateAiMessageBubble(aiBubbleView, result.message, result.recipes);
                    });
                }

            } catch (Exception e) {
                // Fall back to rule-based engine on Exception
                mainHandler.post(() -> {
                    LocalRecommendation result = runLocalRecommendationEngine(userQuery);
                    updateAiMessageBubble(aiBubbleView, result.message, result.recipes);
                });
            }
        });
    }

    private List<Recipe> matchSampleRecipesForQuery(String query) {
        String lowerQuery = query.toLowerCase();
        List<Recipe> suggestions = new ArrayList<>();

        if (lowerQuery.contains("phở") || lowerQuery.contains("bo") || lowerQuery.contains("bò")) {
            suggestions.add(new Recipe(101, "Phở Bò Gia Truyền", 120, 4.9, 450, R.drawable.character_chef_1));
        }
        if (lowerQuery.contains("bánh mì") || lowerQuery.contains("thịt") || lowerQuery.contains("heo")) {
            suggestions.add(new Recipe(102, "Bánh Mì Thịt Nướng", 30, 4.8, 320, R.drawable.character_chef_2));
        }
        if (lowerQuery.contains("bánh xèo") || lowerQuery.contains("xèo") || lowerQuery.contains("tôm")) {
            suggestions.add(new Recipe(103, "Bánh Xèo Miền Tây", 45, 4.6, 380, R.drawable.character_chef_1));
        }
        if (lowerQuery.contains("gỏi cuốn") || lowerQuery.contains("cuốn") || lowerQuery.contains("tôm") || lowerQuery.contains("heo")) {
            suggestions.add(new Recipe(104, "Gỏi Cuốn Tôm Thịt", 15, 4.7, 250, R.drawable.character_chef_2));
        }
        if (lowerQuery.contains("kho tàu") || lowerQuery.contains("thịt kho") || lowerQuery.contains("trứng")) {
            suggestions.add(new Recipe(105, "Thịt Kho Tàu Truyền Thống", 60, 4.9, 520, R.drawable.character_chef_1));
        }

        // If nothing matches, provide two delicious fallbacks
        if (suggestions.isEmpty()) {
            suggestions.add(new Recipe(102, "Bánh Mì Thịt Nướng", 30, 4.8, 320, R.drawable.character_chef_2));
            suggestions.add(new Recipe(104, "Gỏi Cuốn Tôm Thịt", 15, 4.7, 250, R.drawable.character_chef_2));
        }

        return suggestions;
    }

    private LocalRecommendation runLocalRecommendationEngine(String query) {
        String lowerQuery = query.toLowerCase();
        List<Recipe> suggestions = new ArrayList<>();
        String message;

        if (lowerQuery.contains("cà chua") || lowerQuery.contains("trứng") || lowerQuery.contains("heo") || lowerQuery.contains("thịt")) {
            message = "Tuyệt vời! Với nguyên liệu thịt heo, cà chua hay trứng, tôi gợi ý cho bạn món Bánh Mì Thịt Nướng nóng hổi đậm đà, hoặc Gỏi Cuốn Tôm Thịt thanh mát siêu dễ làm tại nhà nhé! Bạn có thể xem chi tiết công thức ngay dưới đây:";
            suggestions.add(new Recipe(102, "Bánh Mì Thịt Nướng", 30, 4.8, 320, R.drawable.character_chef_2));
            suggestions.add(new Recipe(104, "Gỏi Cuốn Tôm Thịt", 15, 4.7, 250, R.drawable.character_chef_2));
        } else if (lowerQuery.contains("bò") || lowerQuery.contains("bún") || lowerQuery.contains("phở")) {
            message = "Thật tuyệt hảo! Với nguyên liệu thịt bò, không gì sánh bằng món Phở Bò Gia Truyền nóng hổi nức tiếng Hà Nội, nước dùng ngọt lịm từ xương ống hầm sâu. Xem công thức chuẩn vị ở đây nhé:";
            suggestions.add(new Recipe(101, "Phở Bò Gia Truyền", 120, 4.9, 450, R.drawable.character_chef_1));
        } else if (lowerQuery.contains("tôm") || lowerQuery.contains("bột")) {
            message = "Ý tưởng tuyệt vời! Tôm tươi mọng nước cuốn với bánh tráng dẻo dai thành món Gỏi Cuốn tôm thịt, chấm xốt tương bơ đậu phộng ngậy bùi, hoặc đổ Bánh Xèo Miền Tây vàng giòn rụm đều ngon mê ly!";
            suggestions.add(new Recipe(103, "Bánh Xèo Miền Tây", 45, 4.6, 380, R.drawable.character_chef_1));
            suggestions.add(new Recipe(104, "Gỏi Cuốn Tôm Thịt", 15, 4.7, 250, R.drawable.character_chef_2));
        } else {
            message = "Chào bạn! Để tận dụng tối đa nguyên liệu của bạn, đây là các gợi ý công thức món ăn truyền thống Việt Nam cực ngon, chuẩn vị và dễ chế biến nhất:";
            suggestions.add(new Recipe(102, "Bánh Mì Thịt Nướng", 30, 4.8, 320, R.drawable.character_chef_2));
            suggestions.add(new Recipe(104, "Gỏi Cuốn Tôm Thịt", 15, 4.7, 250, R.drawable.character_chef_2));
        }

        return new LocalRecommendation(message, suggestions);
    }

    private void scrollToBottom() {
        mainHandler.postDelayed(() -> {
            if (scrollAiChatMessages != null) {
                scrollAiChatMessages.fullScroll(ScrollView.FOCUS_DOWN);
            }
        }, 100);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executorService.shutdown();
    }

    private static class LocalRecommendation {
        String message;
        List<Recipe> recipes;

        LocalRecommendation(String message, List<Recipe> recipes) {
            this.message = message;
            this.recipes = recipes;
        }
    }
}
