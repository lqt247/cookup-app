package com.example.cookup_app.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookup_app.R;
import com.example.cookup_app.adapter.SearchRecipeAdapter;
import com.example.cookup_app.model.Recipe;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.card.MaterialCardView;
import android.widget.ImageView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {
    private EditText etSearch;
    private ChipGroup cgCountry;
    private ChipGroup cgType;
    private ChipGroup cgTaste;
    private RecyclerView rvSearchResults;
    private MaterialCardView btnFilter;
    private ImageView imgFilterIcon;
    private android.widget.TextView tvNoResults;
    private android.widget.LinearLayout layoutHistory;
    private ChipGroup cgHistory;

    private String selectedTimeFilter = "Tất cả";
    private String selectedCalorieFilter = "Tất cả";

    private List<Recipe> allRecipes = new ArrayList<>();
    private List<Recipe> filteredRecipes = new ArrayList<>();

    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable = null;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        etSearch = view.findViewById(R.id.etSearch);
        cgCountry = view.findViewById(R.id.cgCountry);
        cgType = view.findViewById(R.id.cgType);
        cgTaste = view.findViewById(R.id.cgTaste);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        btnFilter = view.findViewById(R.id.btnFilter);
        imgFilterIcon = view.findViewById(R.id.imgFilterIcon);
        tvNoResults = view.findViewById(R.id.tvNoResults);
        layoutHistory = view.findViewById(R.id.layoutHistory);
        cgHistory = view.findViewById(R.id.cgHistory);

        rvSearchResults.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterBottomSheet());
        }

        if (etSearch != null) {
            etSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    String query = etSearch.getText().toString().trim();
                    if (!query.isEmpty()) {
                        saveSearchQuery(query);
                        updateSearchHistoryChips();
                    }
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                    }
                    return true;
                }
                return false;
            });
        }

        loadAllRecipes();
        setupFilters();
        updateSearchHistoryChips();

        return view;
    }

    private void loadAllRecipes() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("recipes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        List<Recipe> items = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Recipe recipe = doc.toObject(Recipe.class);
                            if (recipe != null) {
                                recipe = com.example.cookup_app.utils.RecipeDataHelper.sanitizeAndHealRecipeId(doc, recipe);
                                boolean isFake = false;
                                try {
                                    int docIdVal = Integer.parseInt(doc.getId());
                                    if (docIdVal < 10000) {
                                        isFake = true;
                                    }
                                } catch (NumberFormatException e) {
                                    // Not integer doc ID
                                }

                                if (!isFake) {
                                    items.add(recipe);
                                }
                            }
                        }
                        if (!items.isEmpty()) {
                            allRecipes = items;
                            updateResults();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Fail silently
                });
    }

    private void setupFilters() {
        // Text Search Watcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSearchHistoryChips();
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> updateResults();
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Set custom styling update on checks
        setupChipGroupStyling(cgCountry);
        setupChipGroupStyling(cgType);
        setupChipGroupStyling(cgTaste);

        cgCountry.setOnCheckedStateChangeListener((group, checkedIds) -> updateResults());
        cgType.setOnCheckedStateChangeListener((group, checkedIds) -> updateResults());
        cgTaste.setOnCheckedStateChangeListener((group, checkedIds) -> updateResults());
    }

    private void setupChipGroupStyling(ChipGroup chipGroup) {
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        chip.setChipBackgroundColorResource(R.color.orange_primary);
                        chip.setTextColor(getResources().getColor(R.color.text_primary));
                    } else {
                        chip.setChipBackgroundColorResource(R.color.bg_card);
                        chip.setTextColor(getResources().getColor(R.color.text_secondary));
                    }
                });
            }
        }
    }

    private void updateResults() {
        if (!isAdded()) return;

        String query = etSearch.getText().toString().trim();
        
        // Find checked chips
        String countryFilter = getSelectedChipText(cgCountry);
        String typeFilter = getSelectedChipText(cgType);
        String tasteFilter = getSelectedChipText(cgTaste);

        filteredRecipes.clear();

        for (Recipe r : allRecipes) {
            // Country Filter
            boolean matchesCountry = countryFilter.equals("Tất cả") || 
                    (r.getCountry() != null && r.getCountry().equalsIgnoreCase(countryFilter));

            // Type Filter
            boolean matchesType = typeFilter.equals("Tất cả") || 
                    (r.getTags() != null && r.getTags().contains(typeFilter)) ||
                    r.getName().toLowerCase().contains(typeFilter.toLowerCase()) ||
                    (r.getDescription() != null && r.getDescription().toLowerCase().contains(typeFilter.toLowerCase()));

            // Taste Filter
            boolean matchesTaste = tasteFilter.equals("Tất cả") || 
                    (r.getTags() != null && r.getTags().contains(tasteFilter)) ||
                    r.getName().toLowerCase().contains(tasteFilter.toLowerCase());

            // Cook Time Filter
            boolean matchesTime = true;
            if (!selectedTimeFilter.equals("Tất cả")) {
                if (selectedTimeFilter.contains("Dưới 30")) {
                    matchesTime = r.getCookTimeMinutes() < 30;
                } else if (selectedTimeFilter.contains("30 - 60")) {
                    matchesTime = r.getCookTimeMinutes() >= 30 && r.getCookTimeMinutes() <= 60;
                } else if (selectedTimeFilter.contains("Trên 60")) {
                    matchesTime = r.getCookTimeMinutes() > 60;
                }
            }

            // Calories Filter
            boolean matchesCalories = true;
            if (!selectedCalorieFilter.equals("Tất cả")) {
                if (selectedCalorieFilter.contains("Dưới 300")) {
                    matchesCalories = r.getCalories() < 300;
                } else if (selectedCalorieFilter.contains("300 - 500")) {
                    matchesCalories = r.getCalories() >= 300 && r.getCalories() <= 500;
                } else if (selectedCalorieFilter.contains("Trên 500")) {
                    matchesCalories = r.getCalories() > 500;
                }
            }

            if (matchesCountry && matchesType && matchesTaste && matchesTime && matchesCalories) {
                filteredRecipes.add(r);
            }
        }

        // Apply smart search relevance scoring and ranking!
        List<Recipe> rankedResults = com.example.cookup_app.utils.RecipeRecommendationAlgorithm.searchAndRank(filteredRecipes, query);

        if (rankedResults.isEmpty()) {
            if (tvNoResults != null) {
                tvNoResults.setVisibility(View.VISIBLE);
            }
            rvSearchResults.setVisibility(View.GONE);
        } else {
            if (tvNoResults != null) {
                tvNoResults.setVisibility(View.GONE);
            }
            rvSearchResults.setVisibility(View.VISIBLE);
        }

        rvSearchResults.setAdapter(new SearchRecipeAdapter(rankedResults));
    }

    private String getSelectedChipText(ChipGroup chipGroup) {
        int checkedId = chipGroup.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            Chip chip = chipGroup.findViewById(checkedId);
            if (chip != null) {
                return chip.getText().toString();
            }
        }
        return "Tất cả";
    }

    private void showFilterBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetTheme);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_search_filter, null);
        dialog.setContentView(view);

        ChipGroup cgFilterTime = view.findViewById(R.id.cgFilterTime);
        ChipGroup cgFilterCalories = view.findViewById(R.id.cgFilterCalories);
        View btnReset = view.findViewById(R.id.btnResetFilters);
        View btnApply = view.findViewById(R.id.btnApplyFilters);

        if (cgFilterTime != null) {
            setupFilterSheetChipStyling(cgFilterTime, selectedTimeFilter);
        }
        if (cgFilterCalories != null) {
            setupFilterSheetChipStyling(cgFilterCalories, selectedCalorieFilter);
        }

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                selectedTimeFilter = "Tất cả";
                selectedCalorieFilter = "Tất cả";
                dialog.dismiss();
                updateFilterButtonUI();
                updateResults();
            });
        }

        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                if (cgFilterTime != null) {
                    selectedTimeFilter = getSelectedChipText(cgFilterTime);
                }
                if (cgFilterCalories != null) {
                    selectedCalorieFilter = getSelectedChipText(cgFilterCalories);
                }
                dialog.dismiss();
                updateFilterButtonUI();
                updateResults();
            });
        }

        dialog.show();
    }

    private void setupFilterSheetChipStyling(ChipGroup chipGroup, String currentSelectedValue) {
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                boolean isSelected = chip.getText().toString().equals(currentSelectedValue);
                chip.setChecked(isSelected);
                if (isSelected) {
                    chip.setChipBackgroundColorResource(R.color.orange_primary);
                    chip.setTextColor(android.graphics.Color.WHITE);
                } else {
                    chip.setChipBackgroundColorResource(R.color.bg_input);
                    chip.setTextColor(getResources().getColor(R.color.text_secondary));
                }

                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        chip.setChipBackgroundColorResource(R.color.orange_primary);
                        chip.setTextColor(android.graphics.Color.WHITE);
                    } else {
                        chip.setChipBackgroundColorResource(R.color.bg_input);
                        chip.setTextColor(getResources().getColor(R.color.text_secondary));
                    }
                });
            }
        }
    }

    private void updateFilterButtonUI() {
        if (btnFilter == null || imgFilterIcon == null) return;

        boolean isFilterActive = !selectedTimeFilter.equals("Tất cả") || !selectedCalorieFilter.equals("Tất cả");
        if (isFilterActive) {
            btnFilter.setCardBackgroundColor(getResources().getColor(R.color.orange_primary));
            btnFilter.setStrokeColor(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.orange_primary)));
            imgFilterIcon.setColorFilter(android.graphics.Color.WHITE);
        } else {
            btnFilter.setCardBackgroundColor(getResources().getColor(R.color.bg_card));
            btnFilter.setStrokeColor(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.border)));
            imgFilterIcon.setColorFilter(getResources().getColor(R.color.orange_primary));
        }
    }

    private List<String> getSearchHistory() {
        if (!isAdded()) return new ArrayList<>();
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("cookup_search_history", android.content.Context.MODE_PRIVATE);
        String raw = prefs.getString("queries", "");
        List<String> list = new ArrayList<>();
        if (!raw.isEmpty()) {
            for (String s : raw.split("\\|\\|\\|")) {
                if (!s.trim().isEmpty()) {
                    list.add(s.trim());
                }
            }
        }
        return list;
    }

    private void saveSearchQuery(String query) {
        if (query == null || query.trim().isEmpty() || !isAdded()) return;
        query = query.trim();
        List<String> history = getSearchHistory();
        history.remove(query); // Remove duplicates
        history.add(0, query); // Prepend to beginning
        if (history.size() > 8) {
            history = history.subList(0, 8); // Keep up to 8 items
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            sb.append(history.get(i));
            if (i < history.size() - 1) {
                sb.append("|||");
            }
        }
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("cookup_search_history", android.content.Context.MODE_PRIVATE);
        prefs.edit().putString("queries", sb.toString()).apply();
    }

    private void updateSearchHistoryChips() {
        if (cgHistory == null || layoutHistory == null || !isAdded()) return;
        cgHistory.removeAllViews();
        List<String> history = getSearchHistory();
        String currentQuery = etSearch.getText().toString().trim();
        if (history.isEmpty() || !currentQuery.isEmpty()) {
            layoutHistory.setVisibility(View.GONE);
            return;
        }
        layoutHistory.setVisibility(View.VISIBLE);
        for (String q : history) {
            Chip chip = new Chip(requireContext());
            chip.setText(q);
            chip.setChipBackgroundColorResource(R.color.bg_card);
            chip.setTextColor(getResources().getColor(R.color.text_primary));
            chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.border)));
            chip.setChipStrokeWidth(1.0f);
            chip.setOnClickListener(v -> {
                etSearch.setText(q);
                etSearch.setSelection(q.length());
                // Save and update results
                saveSearchQuery(q);
                updateSearchHistoryChips();
                updateResults();
            });
            cgHistory.addView(chip);
        }
    }


}
