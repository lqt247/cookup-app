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

public class SearchRecipeAdapter extends RecyclerView.Adapter<SearchRecipeAdapter.SearchViewHolder> {
    private final List<Recipe> items;

    public SearchRecipeAdapter(List<Recipe> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_grid, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        Recipe item = items.get(position);
        holder.tvName.setText(item.getName());
        holder.tvTime.setText(item.getCookTimeMinutes() + " phút");

        String country = item.getCountry() != null ? item.getCountry() : "Món Việt";
        holder.tvCategory.setText(country);

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

    static class SearchViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgRecipe;
        final TextView tvName;
        final TextView tvCategory;
        final TextView tvTime;
        final View btnBookmark;

        SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            imgRecipe = itemView.findViewById(R.id.imgRecipe);
            tvName = itemView.findViewById(R.id.tvRecipeName);
            tvCategory = itemView.findViewById(R.id.tvRecipeCategory);
            tvTime = itemView.findViewById(R.id.tvRecipeTime);
            btnBookmark = itemView.findViewById(R.id.btnBookmark);
        }
    }
}
