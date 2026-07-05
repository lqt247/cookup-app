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

/**
 * HomeRecipeAdapter là cầu nối (Adapter) giữa nguồn dữ liệu (Danh sách Recipe) 
 * và giao diện hiển thị (RecyclerView) ở màn hình chính.
 */
public class HomeRecipeAdapter extends RecyclerView.Adapter<HomeRecipeAdapter.RecipeViewHolder> {
    
    // Nguồn dữ liệu truyền vào adapter
    private final List<Recipe> items;

    // Hàm khởi tạo để gán danh sách dữ liệu
    public HomeRecipeAdapter(List<Recipe> items) {
        this.items = items;
    }

    /**
     * Bước 1: Tạo ViewHolder. Hàm này được gọi khi RecyclerView cần tạo một ô hiển thị mới.
     * Nó sẽ nạp layout XML (item_home_recipe) thành đối tượng View trong Java/Kotlin.
     */
    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp tệp layout XML giao diện cho mỗi ô phần tử
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    /**
     * Bước 2: Liên kết dữ liệu (Bind). Hàm này được gọi để hiển thị dữ liệu của một phần tử 
     * tại vị trí (position) cụ thể lên các thành phần giao diện của ViewHolder tương ứng.
     */
    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        // Lấy đối tượng dữ liệu tại vị trí hiện tại
        Recipe item = items.get(position);
        
        // Gán tên món ăn lên TextView
        holder.tvName.setText(item.getName());
        
        // Gán các thông tin phụ như thời gian, đánh giá, năng lượng calo
        holder.tvMeta.setText(item.getCookTimeMinutes() + " phut" + " | "
                + item.getRating() + " | "
                + item.getCalories() + " kcal");
        
        // Xử lý nạp hình ảnh bằng thư viện Glide (nếu là đường dẫn URI hợp lệ) hoặc lấy từ tài nguyên drawable có sẵn
        if (item.getImageUrl() != null && !item.getImageUrl().trim().isEmpty() && com.example.cookup_app.utils.RecipeDataHelper.isUriReadable(holder.itemView.getContext(), item.getImageUrl())) {
            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.character_chef_1)
                    .error(R.drawable.character_chef_1)
                    .into(holder.imgRecipe);
        } else if (com.example.cookup_app.utils.RecipeDataHelper.isValidDrawable(holder.itemView.getContext(), item.getImageResId())) {
            holder.imgRecipe.setImageResource(item.getImageResId());
        } else {
            holder.imgRecipe.setImageResource(R.drawable.character_chef_1);
        }

        // Đăng ký sự kiện nhấn vào ô phần tử để mở màn hình chi tiết món ăn (RecipeDetailActivity)
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(holder.itemView.getContext(), com.example.cookup_app.activity.RecipeDetailActivity.class);
            intent.putExtra("recipe", item); // Truyền đối tượng món ăn qua Intent (đã implements Serializable)
            holder.itemView.getContext().startActivity(intent);
        });
    }

    /**
     * Trả về tổng số phần tử có trong danh sách dữ liệu để RecyclerView biết cần hiển thị bao nhiêu dòng/cột.
     */
    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    /**
     * ViewHolder là lớp đại diện cho giao diện của từng ô phần tử đơn lẻ.
     * Nó giúp lưu trữ (cache) các tham chiếu đến các view con (ImageView, TextView) 
     * để tránh việc gọi hàm findViewById nhiều lần gây giật lag (tối ưu hiệu năng).
     */
    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgRecipe;
        final TextView tvName;
        final TextView tvMeta;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ các view con từ layout XML item_home_recipe
            imgRecipe = itemView.findViewById(R.id.imgRecipe);
            tvName = itemView.findViewById(R.id.tvRecipeName);
            tvMeta = itemView.findViewById(R.id.tvRecipeMeta);
        }
    }
}
