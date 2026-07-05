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

public class FeaturedRecipeAdapter extends RecyclerView.Adapter<FeaturedRecipeAdapter.FeaturedViewHolder> {
    private final List<Recipe> items;

    public FeaturedRecipeAdapter(List<Recipe> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public FeaturedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_featured_recipe, parent, false);
        return new FeaturedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeaturedViewHolder holder, int position) {
        Recipe item = items.get(position);
        holder.tvName.setText(item.getName());
        holder.tvRating.setText(String.valueOf(item.getRating()));
        holder.tvCalories.setText(item.getCalories() + " kcal");

        // Thiết lập thẻ nhãn động dựa trên dữ liệu công thức
        String tag = "Món Ngon";
        if (item.getTags() != null && !item.getTags().isEmpty()) {
            tag = item.getTags().get(0);
        } else {
            tag = item.getCookTimeMinutes() < 30 ? "Dưới 30 phút" : "Healthy";
        }
        holder.tvRecipeTag.setText(tag);

        if (item.getImageUrl() != null && !item.getImageUrl().trim().isEmpty() && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(holder.itemView.getContext(), item.getImageUrl())) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.character_chef_2)
                    .error(R.drawable.character_chef_2)
                    .into(holder.imgRecipe);
        } else if (com.example.cookup_app.utils.RecipeDataHelper.isValidDrawable(holder.itemView.getContext(), item.getImageResId())) {
            holder.imgRecipe.setImageResource(item.getImageResId());
        } else {
            holder.imgRecipe.setImageResource(R.drawable.character_chef_2);
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

    static class FeaturedViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgRecipe;
        final TextView tvName;
        final TextView tvRating;
        final TextView tvCalories;
        final TextView tvRecipeTag;
        final View btnBookmark;
        final ImageView imgBookmark;

        FeaturedViewHolder(@NonNull View itemView) {
            super(itemView);
            imgRecipe = itemView.findViewById(R.id.imgRecipe);
            tvName = itemView.findViewById(R.id.tvRecipeName);
            tvRating = itemView.findViewById(R.id.tvRecipeRating);
            tvCalories = itemView.findViewById(R.id.tvRecipeCalories);
            tvRecipeTag = itemView.findViewById(R.id.tvRecipeTag);
            btnBookmark = itemView.findViewById(R.id.btnBookmark);
            imgBookmark = itemView.findViewById(R.id.imgBookmark);
        }
    }
}
