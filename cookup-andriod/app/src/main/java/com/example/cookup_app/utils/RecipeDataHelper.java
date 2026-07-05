package com.example.cookup_app.utils;

import com.example.cookup_app.model.Ingredient;
import com.example.cookup_app.model.Recipe;
import com.example.cookup_app.model.RecipeStep;

import java.util.ArrayList;
import java.util.List;

public class RecipeDataHelper {

    public static Recipe populateDetails(Recipe r) {
        if (r == null) return null;

        // Đảm bảo rằng mô tả, quốc gia và độ khó luôn được thiết lập giá trị mặc định nếu bị thiếu
        if (r.getCountry() == null || r.getCountry().isEmpty()) {
            r.setCountry("Việt Nam");
        }
        if (r.getDifficulty() == null || r.getDifficulty().isEmpty()) {
            r.setDifficulty("Dễ");
        }
        if (r.getServings() <= 0) {
            r.setServings(4); // Số lượng khẩu phần mặc định
        }
        if (r.getDescription() == null || r.getDescription().isEmpty()) {
            r.setDescription("Công thức món ngon đặc sắc, dễ thực hiện tại nhà cho gia đình.");
        }

        // Chỉ thêm nguyên liệu dự phòng nếu danh sách rỗng
        if (r.getIngredients() == null || r.getIngredients().isEmpty()) {
            List<Ingredient> ingredients = new ArrayList<>();
            ingredients.add(new Ingredient("Vừa đủ", "Nguyên liệu chính"));
            ingredients.add(new Ingredient("Gia vị", "Muối, đường, tiêu, hạt nêm"));
            r.setIngredients(ingredients);
        }

        if (r.getSteps() == null || r.getSteps().isEmpty()) {
            List<RecipeStep> steps = new ArrayList<>();
            steps.add(new RecipeStep(1, "Sơ chế sạch các nguyên liệu xắt khúc mỏng vừa ăn.", "", 180));
            steps.add(new RecipeStep(2, "Ướp gia vị cho ngấm đều trong 15 phút.", "", 240));
            steps.add(new RecipeStep(3, "Chế biến nấu chín vàng thơm nức.", "", 300));
            steps.add(new RecipeStep(4, "Thưởng thức món ăn khi còn nóng ấm.", "", 60));
            r.setSteps(steps);
        }

        return r;
    }

    public static Recipe sanitizeAndHealRecipeId(com.google.firebase.firestore.DocumentSnapshot doc, Recipe recipe) {
        if (recipe == null || doc == null) return recipe;
        if (recipe.getId() <= 0 || recipe.getId() < 10000) {
            int rawHash = doc.getId().hashCode();
            int newId = rawHash == Integer.MIN_VALUE ? Integer.MAX_VALUE : Math.abs(rawHash);
            if (newId < 10000) {
                newId += 10000; // Force it above 10000 to avoid fake filter
            }
            recipe.setId(newId);
            android.util.Log.d("CollectionDiagnostic", "Healed recipe ID from " + recipe.getId() + " to " + newId + " for recipe: " + recipe.getName() + " (docId: " + doc.getId() + ")");
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("recipes")
                        .document(doc.getId())
                        .update("id", newId)
                        .addOnSuccessListener(aVoid -> android.util.Log.d("CollectionDiagnostic", "Successfully updated recipe ID field in Firestore for doc: " + doc.getId()))
                        .addOnFailureListener(e -> android.util.Log.e("CollectionDiagnostic", "Failed to update recipe ID field in Firestore for doc: " + doc.getId() + ", " + e.getMessage()));
            } catch (Exception e) {
                // Ignore silent errors
            }
        }
        return recipe;
    }

    public static String copyUriToInternalFile(android.content.Context context, android.net.Uri uri, String prefix) {
        if (context == null || uri == null) return null;
        try {
            java.io.InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            
            // Tạo tên file duy nhất trong thư mục lưu trữ nội bộ của ứng dụng
            String filename = prefix + "_" + System.currentTimeMillis() + ".jpg";
            java.io.File file = new java.io.File(context.getFilesDir(), filename);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(file);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();
            
            return android.net.Uri.fromFile(file).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isUriReadable(android.content.Context context, String uriStr) {
        if (uriStr == null || !uriStr.startsWith("content://")) {
            return true;
        }
        try {
            android.net.Uri uri = android.net.Uri.parse(uriStr);
            java.io.InputStream is = context.getContentResolver().openInputStream(uri);
            if (is != null) {
                is.close();
                return true;
            }
        } catch (Exception e) {
            // Không có quyền đọc Uri hoặc Uri không tồn tại
        }
        return false;
    }

    public static boolean isValidDrawable(android.content.Context context, int resId) {
        if (context == null || resId == 0) return false;
        try {
            String type = context.getResources().getResourceTypeName(resId);
            return "drawable".equalsIgnoreCase(type);
        } catch (android.content.res.Resources.NotFoundException e) {
            return false;
        }
    }
}
