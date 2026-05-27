package com.example.cookup_app.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookup_app.R;
import com.example.cookup_app.activity.ProfileActivity;
import com.example.cookup_app.adapter.HomeRecipeAdapter;
import com.example.cookup_app.model.Recipe;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String PREFS_NAME = "cookup_prefs";
    private static final String KEY_GENDER = "gender";

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        ImageView imgAvatar = view.findViewById(R.id.imgAvatar);
        RecyclerView rvHome = view.findViewById(R.id.rvHome);

        rvHome.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHome.setAdapter(new HomeRecipeAdapter(getSampleRecipes()));

        applyAvatar(imgAvatar);

        imgAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), ProfileActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void applyAvatar(ImageView imgAvatar) {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String gender = prefs.getString(KEY_GENDER, "unknown");

        if ("male".equalsIgnoreCase(gender)) {
            imgAvatar.setImageResource(R.drawable.avatar_default_male);
        } else if ("female".equalsIgnoreCase(gender)) {
            imgAvatar.setImageResource(R.drawable.avatar_default_female);
        } else {
            imgAvatar.setImageResource(R.drawable.avatar_default_unknown);
        }
    }

    private List<Recipe> getSampleRecipes() {
        List<Recipe> items = new ArrayList<>();
        items.add(new Recipe(101, "Pho Bo Gia Truyen", 120, 4.9, 450, R.drawable.character_chef_1));
        items.add(new Recipe(102, "Banh Mi Thit Nuong", 30, 4.8, 320, R.drawable.character_chef_2));
        items.add(new Recipe(103, "Banh Xeo Mien Tay", 45, 4.6, 380, R.drawable.character_chef_1));
        items.add(new Recipe(104, "Goi Cuon Tom Thit", 15, 4.7, 250, R.drawable.character_chef_2));
        return items;
    }
}