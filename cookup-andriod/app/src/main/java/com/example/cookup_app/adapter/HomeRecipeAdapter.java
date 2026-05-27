package com.example.cookup_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookup_app.R;
import com.example.cookup_app.model.Recipe;

import java.util.List;

public class HomeRecipeAdapter extends RecyclerView.Adapter<HomeRecipeAdapter.RecipeViewHolder> {
    private final List<Recipe> items;

    public HomeRecipeAdapter(List<Recipe> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe item = items.get(position);
        holder.tvName.setText(item.getName());
        holder.tvMeta.setText(item.getCookTimeMinutes() + " phut" + " | "
                + item.getRating() + " | "
                + item.getCalories() + " kcal");
        holder.imgRecipe.setImageResource(item.getImageResId());
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgRecipe;
        final TextView tvName;
        final TextView tvMeta;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            imgRecipe = itemView.findViewById(R.id.imgRecipe);
            tvName = itemView.findViewById(R.id.tvRecipeName);
            tvMeta = itemView.findViewById(R.id.tvRecipeMeta);
        }
    }
}
