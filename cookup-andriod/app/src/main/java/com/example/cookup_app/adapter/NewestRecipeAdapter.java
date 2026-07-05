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
        // Thêm biểu tượng cờ quốc gia nếu chưa có
        if ("Việt Nam".equalsIgnoreCase(country) && !country.contains("🇻🇳")) {
            country = "🇻🇳 " + country;
        } else if ("Nhật Bản".equalsIgnoreCase(country) && !country.contains("🇯🇵")) {
            country = "🇯🇵 " + country;
        } else if ("Hàn Quốc".equalsIgnoreCase(country) && !country.contains("🇰🇷")) {
            country = "🇰🇷 " + country;
        }
        
        holder.tvCategoryAndTime.setText(country + " • " + item.getCookTimeMinutes() + " Phút");
        holder.tvStats.setText("⭐ " + item.getRating() + "  •  " + item.getCalories() + " kcal");

        if (item.getImageUrl() != null && !item.getImageUrl().trim().isEmpty() && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(holder.itemView.getContext(), item.getImageUrl())) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.character_chef_1)
                    .error(R.drawable.character_chef_1)
                    .into(holder.imgRecipe);
        } else if (com.example.cookup_app.utils.RecipeDataHelper.isValidDrawable(holder.itemView.getContext(), item.getImageResId())) {
            holder.imgRecipe.setImageResource(item.getImageResId());
        } else {
            holder.imgRecipe.setImageResource(R.drawable.character_chef_1);
        }

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(holder.itemView.getContext(), com.example.cookup_app.activity.RecipeDetailActivity.class);
            intent.putExtra("recipe", item);
            holder.itemView.getContext().startActivity(intent);
        });

        // Bookmark logic using Firestore collections
        android.content.Context ctx = holder.itemView.getContext();
        com.example.cookup_app.utils.CollectionHelper.checkIsBookmarked(ctx, item.getId(), isBookmarked -> {
            if (isBookmarked) {
                holder.imgBookmark.setColorFilter(ctx.getResources().getColor(R.color.orange_primary));
            } else {
                holder.imgBookmark.setColorFilter(ctx.getResources().getColor(R.color.text_primary));
            }
        });
        
        holder.btnBookmark.setOnClickListener(v -> {
            com.example.cookup_app.utils.CollectionHelper.showSaveToCollectionDialog(ctx, item, () -> {
                // Update icon after dialog closed
                com.example.cookup_app.utils.CollectionHelper.checkIsBookmarked(ctx, item.getId(), isBookmarked -> {
                    if (isBookmarked) {
                        holder.imgBookmark.setColorFilter(ctx.getResources().getColor(R.color.orange_primary));
                    } else {
                        holder.imgBookmark.setColorFilter(ctx.getResources().getColor(R.color.text_primary));
                    }
                });
            });
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
        final ImageView imgBookmark;

        NewestViewHolder(@NonNull View itemView) {
            super(itemView);
            imgRecipe = itemView.findViewById(R.id.imgRecipe);
            tvName = itemView.findViewById(R.id.tvRecipeName);
            tvCategoryAndTime = itemView.findViewById(R.id.tvRecipeCategoryAndTime);
            tvStats = itemView.findViewById(R.id.tvRecipeStats);
            btnBookmark = itemView.findViewById(R.id.btnBookmark);
            imgBookmark = itemView.findViewById(R.id.imgBookmark);
        }
    }
}
