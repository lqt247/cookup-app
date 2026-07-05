package com.example.cookup_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cookup_app.R;
import com.example.cookup_app.model.Recipe;

import java.util.List;

public class NewestRecipeAdapter extends RecyclerView.Adapter<NewestRecipeAdapter.NewestViewHolder> {
    private final List<Recipe> items;

    public NewestRecipeAdapter(List<Recipe> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public NewestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_newest_recipe, parent, false);
        return new NewestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewestViewHolder holder, int position) {
        Recipe item = items.get(position);
        holder.tvName.setText(item.getName());

        String country = item.getCountry() != null ? item.getCountry() : "Món Việt";
        // Add country flag emoji if not exists
        if ("Việt Nam".equalsIgnoreCase(country) && !country.contains("🇻🇳")) {
            country = "🇻🇳 " + country;
        } else if ("Nhật Bản".equalsIgnoreCase(country) && !country.contains("🇯🇵")) {
            country = "🇯🇵 " + country;
        } else if ("Hàn Quốc".equalsIgnoreCase(country) && !country.contains("🇰🇷")) {
            country = "🇰🇷 " + country;
        }
        
        holder.tvCategoryAndTime.setText(country + " • " + item.getCookTimeMinutes() + " Phút");
        holder.tvStats.setText("⭐ " + item.getRating() + "  •  " + item.getCalories() + " kcal");

        if (item.getImageUrl() != null && !item.getImageUrl().trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.character_chef_1)
                    .error(R.drawable.character_chef_1)
                    .into(holder.imgRecipe);
        } else if (item.getImageResId() != 0) {
            holder.imgRecipe.setImageResource(item.getImageResId());
        } else {
            holder.imgRecipe.setImageResource(R.drawable.character_chef_1);
        }

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(holder.itemView.getContext(), com.example.cookup_app.activity.RecipeDetailActivity.class);
            intent.putExtra("recipe", item);
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class NewestViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgRecipe;
        final TextView tvName;
        final TextView tvCategoryAndTime;
        final TextView tvStats;
        final View btnBookmark;

        NewestViewHolder(@NonNull View itemView) {
            super(itemView);
            imgRecipe = itemView.findViewById(R.id.imgRecipe);
            tvName = itemView.findViewById(R.id.tvRecipeName);
            tvCategoryAndTime = itemView.findViewById(R.id.tvRecipeCategoryAndTime);
            tvStats = itemView.findViewById(R.id.tvRecipeStats);
            btnBookmark = itemView.findViewById(R.id.btnBookmark);
        }
    }
}
