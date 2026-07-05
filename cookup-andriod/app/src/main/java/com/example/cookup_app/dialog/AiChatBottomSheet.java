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
import androidx.core.widget.NestedScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cookup_app.BuildConfig;
import com.example.cookup_app.R;
import com.example.cookup_app.activity.RecipeDetailActivity;
import com.example.cookup_app.model.Ingredient;
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

    private NestedScrollView scrollAiChatMessages;
    private LinearLayout containerAiChatMessages;
    private LinearLayout containerAiIngredientChips;
    private EditText edtAiChatInput;
    private View btnAiChatSend;
    private View btnAiAddIngredientDirect;

    private final List<String> currentIngredients = new ArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Danh sách công thức THẬT được tải từ Firebase Firestore, dùng để gợi ý cho người dùng
    private final List<Recipe> firestoreRecipes = new ArrayList<>();
    private boolean firestoreRecipesLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_ai_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Liên kết các thành phần giao diện (Views)
        scrollAiChatMessages = view.findViewById(R.id.scrollAiChatMessages);
        containerAiChatMessages = view.findViewById(R.id.containerAiChatMessages);
        containerAiIngredientChips = view.findViewById(R.id.containerAiIngredientChips);
        edtAiChatInput = view.findViewById(R.id.edtAiChatInput);
        btnAiChatSend = view.findViewById(R.id.btnAiChatSend);
        btnAiAddIngredientDirect = view.findViewById(R.id.btnAiAddIngredientDirect);

        // Sự kiện nút đóng
        view.findViewById(R.id.btnAiChatClose).setOnClickListener(v -> dismiss());

        // Thiết lập các thẻ gợi ý nguyên liệu nhanh
        setupSuggestedChips();

        // Tải danh sách công thức thật từ Firebase Firestore để dùng làm gợi ý cho AI
        loadRecipesFromFirestore();

        // Tin nhắn chào mừng kèm hướng dẫn API Key thông minh nếu chưa cấu hình
        String welcomeMsg = "Xin chào! Tôi là Trợ lý nấu ăn CookUp. Bạn đang có những nguyên liệu gì trong tủ lạnh thế? Hãy chọn nhanh bên dưới hoặc nhập nguyên liệu nhé! 🍳";
        String currentKey = BuildConfig.GEMINI_API_KEY;
        if (TextUtils.isEmpty(currentKey) || "MY_GEMINI_API_KEY".equals(currentKey)) {
            welcomeMsg += "\n\n*(Lưu ý: Trợ lý đang chạy mô phỏng ngoại tuyến. Bạn hãy nhập GEMINI_API_KEY vào tab Secrets của ứng dụng để kích hoạt Trí tuệ nhân tạo AI thật sự nhé!)*";
        }
        addAiMessageBubble(welcomeMsg, null);

        // Xử lý các sự kiện nhập liệu
        edtAiChatInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                handleSendAction();
                return true;
            }
            return false;
        });

        btnAiChatSend.setOnClickListener(v -> handleSendAction());

        // Thêm trực tiếp nguyên liệu khi nhấn vào nút cộng
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
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheetInternal);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                ViewGroup.LayoutParams layoutParams = bottomSheetInternal.getLayoutParams();
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheetInternal.setLayoutParams(layoutParams);
            }
        });
        return dialog;
    }

    private void loadRecipesFromFirestore() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("recipes")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    firestoreRecipes.clear();
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Recipe recipe = doc.toObject(Recipe.class);
                            if (recipe != null) {
                                recipe = com.example.cookup_app.utils.RecipeDataHelper.sanitizeAndHealRecipeId(doc, recipe);
                                firestoreRecipes.add(recipe);
                            }
                        }
                    }
                    firestoreRecipesLoaded = true;
                })
                .addOnFailureListener(e -> {
                    // Nếu tải thất bại, giữ danh sách rỗng - hàm gợi ý sẽ tự xử lý an toàn
                    firestoreRecipesLoaded = true;
                    android.util.Log.e("AiChatBottomSheet", "Lỗi tải công thức từ Firestore: " + e.getMessage());
                });
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

        // Xây dựng tin nhắn câu hỏi đầy đủ
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

        // 1. Hiển thị tin nhắn của Người dùng
        addUserMessageBubble(queryText.toString());

        // 2. Thêm bong bóng tin nhắn chờ của AI (đang suy nghĩ...)
        final View aiBubbleView = addAiMessageBubble("Đang suy nghĩ... 🍳", null);

        // 3. Kích hoạt gọi API
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

                if (com.example.cookup_app.utils.RecipeDataHelper.isValidDrawable(getContext(), r.getImageResId())) {
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

                if (com.example.cookup_app.utils.RecipeDataHelper.isValidDrawable(getContext(), r.getImageResId())) {
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

        // Kiểm tra xem có cấu hình API Key thật hay không
        if (TextUtils.isEmpty(apiKey) || "MY_GEMINI_API_KEY".equals(apiKey)) {
            // Chuyển sang sử dụng bộ máy gợi ý cục bộ có sẵn nếu chưa có API Key
            mainHandler.postDelayed(() -> {
                LocalRecommendation result = runLocalRecommendationEngine(userQuery);
                updateAiMessageBubble(aiBubbleView, result.message, result.recipes);
            }, 1200);
            return;
        }

        executorService.execute(() -> {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, 'ngày' dd/MM/yyyy, 'giờ' HH:mm", new java.util.Locale("vi", "VN"));
                String currentDateTime = sdf.format(new java.util.Date());

                String systemPrompt = "Bạn là Trợ lý nấu ăn CookUp thông minh, thân thiện và vạn năng. " +
                        "Nhiệm vụ chính của bạn là gợi ý món ăn ngon dựa trên nguyên liệu của người dùng, " +
                        "nhưng bạn sẵn sàng trả lời BẤT KỲ CÂU HỎI NÀO khác mà người dùng đưa ra (như khoa học, đời sống, thời tiết, toán học, hỏi thăm, trò chuyện thông thường). " +
                        "Hãy luôn trả lời một cách tự nhiên, nhiệt tình và súc tích bằng Tiếng Việt.\n\n" +
                        "THỜI GIAN HIỆN TẠI CỦA HỆ THỐNG: " + currentDateTime + ".\n" +
                        "Khi người dùng đặt câu hỏi liên quan đến thời gian như 'hôm nay', 'ngày mai', 'hôm qua', 'tuần này', 'bây giờ', hoặc ngày tháng năm cụ thể, hãy áp dụng tư duy logic thời gian chuẩn xác nhất dựa trên mốc thời gian hệ thống được cung cấp ở trên để trả lời chính xác, đầy đủ và thông thái.";

                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=" + apiKey);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                // Đặt thời gian chờ hợp lý để tránh treo lâu khi mạng yếu hoặc API phản hồi chậm
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(15000);

                // Xây dựng nội dung yêu cầu (Request Body) chuẩn API của Gemini
                JSONObject requestBody = new JSONObject();

                // 1. Cấu hình nội dung hội thoại (contents)
                JSONArray contentsArray = new JSONArray();
                JSONObject contentObj = new JSONObject();
                JSONArray partsArray = new JSONArray();
                JSONObject userPart = new JSONObject();
                userPart.put("text", userQuery);
                partsArray.put(userPart);
                contentObj.put("parts", partsArray);
                contentsArray.put(contentObj);
                requestBody.put("contents", contentsArray);

                // 2. Cấu hình chỉ dẫn hệ thống của AI (systemInstruction)
                JSONObject systemInstructionObj = new JSONObject();
                JSONArray systemPartsArray = new JSONArray();
                JSONObject systemPartObj = new JSONObject();
                systemPartObj.put("text", systemPrompt);
                systemPartsArray.put(systemPartObj);
                systemInstructionObj.put("parts", systemPartsArray);
                requestBody.put("systemInstruction", systemInstructionObj);

                // Gửi yêu cầu qua kết nối mạng
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

                    // Phân tích cú pháp phản hồi JSON
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray candidates = jsonResponse.getJSONArray("candidates");
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    String aiText = parts.getJSONObject(0).getString("text");

                    // Tìm kiếm các công thức nấu ăn khớp với câu hỏi
                    List<Recipe> matchingRecipes = matchSampleRecipesForQuery(userQuery);

                    mainHandler.post(() -> {
                        updateAiMessageBubble(aiBubbleView, aiText, matchingRecipes);
                    });

                } else {
                    // Chuyển sang câu trả lời hữu ích dự phòng (fallback) khi gặp lỗi kết nối HTTP
                    String fallbackMsg = generateFallbackMessage();
                    List<Recipe> matchingRecipes = matchSampleRecipesForQuery(userQuery);
                    mainHandler.post(() -> {
                        updateAiMessageBubble(aiBubbleView, fallbackMsg, matchingRecipes);
                    });
                }

            } catch (Exception e) {
                // Chuyển sang câu trả lời hữu ích dự phòng (fallback) khi xảy ra ngoại lệ (Exception)
                String fallbackMsg = generateFallbackMessage();
                List<Recipe> matchingRecipes = matchSampleRecipesForQuery(userQuery);
                mainHandler.post(() -> {
                    updateAiMessageBubble(aiBubbleView, fallbackMsg, matchingRecipes);
                });
            }
        });
    }

    private String generateFallbackMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Xin lỗi bạn, kết nối của mình đang bận hoặc có lỗi xảy ra 😢.\n\nTuy nhiên, dựa trên kinh nghiệm của mình");
        if (!currentIngredients.isEmpty()) {
            sb.append(", bạn có thể thử kết hợp các nguyên liệu của bạn (");
            for (int i = 0; i < currentIngredients.size(); i++) {
                sb.append(currentIngredients.get(i));
                if (i < currentIngredients.size() - 1) sb.append(", ");
            }
            sb.append(") để làm món xào tỏi thơm lừng hoặc nấu canh súp thanh mát nhé! 🍲");
        } else {
            sb.append(", bạn có thể thử chế biến các nguyên liệu sẵn có trong tủ lạnh như thịt, cà chua, trứng để nấu những món ăn gia đình đơn giản, ấm cúng nhé! 🍲");
        }
        return sb.toString();
    }

    /**
     * Tìm các công thức THẬT trong Firestore khớp với câu hỏi/nguyên liệu của người dùng.
     * Không còn tạo dữ liệu giả (Recipe ảo không tồn tại trong Firebase) như trước đây.
     */
    private List<Recipe> matchSampleRecipesForQuery(String query) {
        String lowerQuery = query.toLowerCase();
        List<Recipe> matched = new ArrayList<>();

        // Nếu Firestore chưa tải xong hoặc không có công thức nào, không gợi ý bừa
        if (firestoreRecipes.isEmpty()) {
            return matched;
        }

        // Gộp thêm nguyên liệu người dùng đã chọn vào từ khóa tìm kiếm
        List<String> keywords = new ArrayList<>();
        for (String word : lowerQuery.split("[\\s,.!?]+")) {
            if (word.length() >= 2) keywords.add(word);
        }
        for (String ing : currentIngredients) {
            keywords.add(ing.toLowerCase());
        }

        for (Recipe r : firestoreRecipes) {
            if (recipeMatchesKeywords(r, keywords)) {
                matched.add(r);
            }
        }

        // Sắp xếp theo đánh giá cao nhất trước
        java.util.Collections.sort(matched, (a, b) -> Double.compare(b.getRating(), a.getRating()));

        // Nếu không khớp món nào cụ thể, gợi ý các công thức có đánh giá cao nhất (vẫn là dữ liệu thật)
        if (matched.isEmpty()) {
            List<Recipe> fallback = new ArrayList<>(firestoreRecipes);
            java.util.Collections.sort(fallback, (a, b) -> Double.compare(b.getRating(), a.getRating()));
            matched = fallback;
        }

        // Giới hạn tối đa 3 gợi ý
        if (matched.size() > 3) {
            matched = matched.subList(0, 3);
        }
        return matched;
    }

    private boolean recipeMatchesKeywords(Recipe r, List<String> keywords) {
        if (r == null || keywords.isEmpty()) return false;
        StringBuilder haystack = new StringBuilder();
        if (r.getName() != null) haystack.append(r.getName().toLowerCase()).append(" ");
        if (r.getTags() != null) {
            for (String tag : r.getTags()) {
                if (tag != null) haystack.append(tag.toLowerCase()).append(" ");
            }
        }
        if (r.getIngredients() != null) {
            for (Ingredient ing : r.getIngredients()) {
                if (ing != null && ing.getName() != null) {
                    haystack.append(ing.getName().toLowerCase()).append(" ");
                }
            }
        }
        String text = haystack.toString();
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private LocalRecommendation runLocalRecommendationEngine(String query) {
        String lowerQuery = query.toLowerCase();
        String message;
        boolean wantsRecipeSuggestions = true;

        if (lowerQuery.contains("ngày") || lowerQuery.contains("ngày mấy") || lowerQuery.contains("thứ mấy") || lowerQuery.contains("giờ") || lowerQuery.contains("mấy giờ") || lowerQuery.contains("thời gian")) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, 'ngày' dd/MM/yyyy, 'bây giờ là' HH:mm", new java.util.Locale("vi", "VN"));
            String currentDateTime = sdf.format(new java.util.Date());
            message = "Chào bạn! Hôm nay là " + currentDateTime + ". Hôm nay bạn muốn nấu món gì? Hãy chia sẻ nguyên liệu để tôi gợi ý món ăn phù hợp nhé!";
        } else if (lowerQuery.contains("chào") || lowerQuery.contains("hello") || lowerQuery.contains("hi") || lowerQuery.contains("chao")) {
            message = "Chào bạn thân yêu! Tôi là CookUp AI. Rất vui được trò chuyện cùng bạn hôm nay. Bạn muốn tìm kiếm công thức nấu ăn hay hỏi tôi bất cứ điều gì nào?";
        } else if (lowerQuery.contains("bạn là ai") || lowerQuery.contains("tên gì") || lowerQuery.contains("ten gi") || lowerQuery.contains("là gì")) {
            message = "Tôi là Trợ lý Ảo vạn năng CookUp! Nhiệm vụ của tôi là giúp bạn nấu ăn ngon mỗi ngày và đồng hành giải đáp mọi câu hỏi đời sống, thời gian, công việc của bạn. Hãy cứ tự nhiên hỏi nhé!";
        } else if (lowerQuery.contains("làm gì") || lowerQuery.contains("lam gi") || lowerQuery.contains("giúp") || lowerQuery.contains("hướng dẫn") || lowerQuery.contains("tính năng")) {
            message = "Tôi có thể giúp bạn tìm kiếm mọi công thức ẩm thực truyền thống Việt Nam, tính toán thời gian, tư vấn thực đơn và giải đáp các thắc mắc kiến thức thú vị khác. Bạn cần tôi trợ giúp gì nào?";
        } else if (lowerQuery.contains("cà chua") || lowerQuery.contains("trứng") || lowerQuery.contains("heo") || lowerQuery.contains("thịt")) {
            message = "Tuyệt vời! Dựa trên nguyên liệu bạn có, đây là những công thức phù hợp nhất mình tìm được cho bạn:";
        } else if (lowerQuery.contains("bò") || lowerQuery.contains("bún") || lowerQuery.contains("phở")) {
            message = "Thật tuyệt hảo! Đây là những công thức liên quan đến thịt bò mà mình tìm thấy cho bạn:";
        } else if (lowerQuery.contains("tôm") || lowerQuery.contains("bột")) {
            message = "Ý tưởng tuyệt vời! Đây là những công thức phù hợp với nguyên liệu của bạn:";
        } else {
            message = "Thật là một câu hỏi thú vị! Là trợ lý vạn năng CookUp, tôi luôn sẵn lòng giải đáp mọi thắc mắc của bạn từ cuộc sống đến bếp núc. Đây là các gợi ý công thức món ăn hôm nay:";
        }

        // Luôn lấy gợi ý công thức THẬT từ Firebase Firestore, không tạo dữ liệu giả
        List<Recipe> suggestions = wantsRecipeSuggestions ? matchSampleRecipesForQuery(query) : new ArrayList<>();
        if (suggestions.isEmpty() && !firestoreRecipesLoaded) {
            message += "\n\n(Đang tải danh sách công thức, vui lòng thử lại sau giây lát nếu chưa thấy gợi ý.)";
        }

        return new LocalRecommendation(message, suggestions);
    }

    private void scrollToBottom() {
        mainHandler.postDelayed(() -> {
            if (scrollAiChatMessages != null) {
                scrollAiChatMessages.fullScroll(View.FOCUS_DOWN);
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