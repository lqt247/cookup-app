package com.example.cookup_app.utils;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cookup_app.R;
import com.example.cookup_app.model.Recipe;
import com.example.cookup_app.model.RecipeCollection;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CollectionHelper {

    public interface OnBookmarkStatusListener {
        void onStatusChecked(boolean isBookmarked);
    }

    public static void checkIsBookmarked(Context context, int recipeId, OnBookmarkStatusListener listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            listener.onStatusChecked(false);
            return;
        }

        String recipeIdStr = String.valueOf(recipeId);
        android.util.Log.d("CollectionDiagnostic", "checkIsBookmarked [START]: Checking if recipeId=" + recipeId + " is bookmarked for user=" + currentUser.getUid());

        FirebaseFirestore.getInstance()
                .collection("collections")
                .whereEqualTo("creatorUid", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isSaved = false;
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            RecipeCollection col = doc.toObject(RecipeCollection.class);
                            if (col != null) {
                                String colId = col.getId();
                                if (colId == null || colId.trim().isEmpty()) {
                                    colId = doc.getId();
                                    col.setId(colId);
                                }
                                boolean containsRecipe = col.getRecipeIds() != null && col.getRecipeIds().contains(recipeIdStr);
                                android.util.Log.d("CollectionDiagnostic", "checkIsBookmarked [FETCHED]: colId=" + colId + ", name=\"" + col.getName() + "\", recipeIds=" + col.getRecipeIds() + ", containsTarget=" + containsRecipe);
                                if (containsRecipe) {
                                    isSaved = true;
                                    break;
                                }
                            }
                        }
                    }
                    android.util.Log.d("CollectionDiagnostic", "checkIsBookmarked [RESULT]: recipeId=" + recipeId + " isSaved=" + isSaved);
                    listener.onStatusChecked(isSaved);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CollectionDiagnostic", "checkIsBookmarked [FAILURE]: Error checking bookmark for recipeId=" + recipeId, e);
                    listener.onStatusChecked(false);
                });
    }

    public static void showSaveToCollectionDialog(Context context, Recipe recipe, Runnable onComplete) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(context, "Vui lòng đăng nhập để lưu công thức!", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("CollectionDiagnostic", "showSaveToCollectionDialog [START]: recipeId=" + recipe.getId() + ", recipeName=\"" + recipe.getName() + "\"");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("collections")
                .whereEqualTo("creatorUid", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<RecipeCollection> userCollections = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            RecipeCollection col = doc.toObject(RecipeCollection.class);
                            if (col != null) {
                                String colId = col.getId();
                                if (colId == null || colId.trim().isEmpty()) {
                                    colId = doc.getId();
                                    col.setId(colId);
                                }
                                android.util.Log.d("CollectionDiagnostic", "showSaveToCollectionDialog [FETCHED]: colId=" + colId + ", name=\"" + col.getName() + "\", recipeIds=" + col.getRecipeIds());
                                userCollections.add(col);
                            }
                        }
                    }
                    showCollectionListSelectionDialog(context, recipe, userCollections, onComplete);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CollectionDiagnostic", "showSaveToCollectionDialog [FAILURE]: Failed to fetch collections", e);
                    Toast.makeText(context, "Không thể tải danh sách bộ sưu tập: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private static void showCollectionListSelectionDialog(Context context, Recipe recipe, List<RecipeCollection> collections, Runnable onComplete) {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.BottomSheetTheme);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_save_to_collection, null);
        dialog.setContentView(view);

        LinearLayout layoutCollectionsList = view.findViewById(R.id.layoutCollectionsList);
        View layoutEmptyCollections = view.findViewById(R.id.layoutEmptyCollections);
        View btnCreateNewCollection = view.findViewById(R.id.btnCreateNewCollection);
        View btnFinishSave = view.findViewById(R.id.btnFinishSave);

        String recipeIdStr = String.valueOf(recipe.getId());
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (collections.isEmpty()) {
            if (layoutEmptyCollections != null) {
                layoutEmptyCollections.setVisibility(View.VISIBLE);
            }
        } else {
            if (layoutEmptyCollections != null) {
                layoutEmptyCollections.setVisibility(View.GONE);
            }

            for (RecipeCollection col : collections) {
                View itemView = LayoutInflater.from(context).inflate(R.layout.item_collection_select, layoutCollectionsList, false);
                
                TextView tvCollectionName = itemView.findViewById(R.id.tvCollectionName);
                TextView tvCollectionInfo = itemView.findViewById(R.id.tvCollectionInfo);
                MaterialCheckBox cbCollectionSelect = itemView.findViewById(R.id.cbCollectionSelect);
                View cardCollectionItem = itemView.findViewById(R.id.cardCollectionItem);

                if (tvCollectionName != null) {
                    tvCollectionName.setText(col.getName());
                }

                int count = col.getRecipeIds() != null ? col.getRecipeIds().size() : 0;
                String infoStr = (col.isPublic() ? "Công khai" : "Riêng tư") + " • " + count + " món ăn";
                if (tvCollectionInfo != null) {
                    tvCollectionInfo.setText(infoStr);
                }

                boolean isChecked = col.getRecipeIds() != null && col.getRecipeIds().contains(recipeIdStr);
                if (cbCollectionSelect != null) {
                    cbCollectionSelect.setChecked(isChecked);
                }

                if (cardCollectionItem != null) {
                    cardCollectionItem.setOnClickListener(v -> {
                        if (cbCollectionSelect != null) {
                            boolean newCheckedState = !cbCollectionSelect.isChecked();
                            cbCollectionSelect.setChecked(newCheckedState);

                            List<String> rIds = col.getRecipeIds();
                            if (rIds == null) rIds = new ArrayList<>();

                            String colIdRaw = col.getId();
                            final String colId = (colIdRaw == null || colIdRaw.trim().isEmpty()) ? "UNKNOWN_ID" : colIdRaw;

                            if (newCheckedState) {
                                if (!rIds.contains(recipeIdStr)) {
                                    rIds.add(recipeIdStr);
                                }
                                col.setRecipeIds(rIds);
                                android.util.Log.d("CollectionDiagnostic", "showCollectionListSelectionDialog [UPDATE - ADD]: Adding recipeId=" + recipeIdStr + " to collection colId=" + colId);
                                db.collection("collections").document(colId)
                                        .update("recipeIds", com.google.firebase.firestore.FieldValue.arrayUnion(recipeIdStr))
                                        .addOnSuccessListener(aVoid -> android.util.Log.d("CollectionDiagnostic", "showCollectionListSelectionDialog [UPDATE - ADD - SUCCESS]: colId=" + colId))
                                        .addOnFailureListener(e -> android.util.Log.e("CollectionDiagnostic", "showCollectionListSelectionDialog [UPDATE - ADD - FAILURE]: colId=" + colId, e));
                            } else {
                                rIds.remove(recipeIdStr);
                                col.setRecipeIds(rIds);
                                android.util.Log.d("CollectionDiagnostic", "showCollectionListSelectionDialog [UPDATE - REMOVE]: Removing recipeId=" + recipeIdStr + " from collection colId=" + colId);
                                db.collection("collections").document(colId)
                                        .update("recipeIds", com.google.firebase.firestore.FieldValue.arrayRemove(recipeIdStr))
                                        .addOnSuccessListener(aVoid -> android.util.Log.d("CollectionDiagnostic", "showCollectionListSelectionDialog [UPDATE - REMOVE - SUCCESS]: colId=" + colId))
                                        .addOnFailureListener(e -> android.util.Log.e("CollectionDiagnostic", "showCollectionListSelectionDialog [UPDATE - REMOVE - FAILURE]: colId=" + colId, e));
                            }
                            
                            // Live update the subtitle item count
                            int updatedCount = col.getRecipeIds().size();
                            if (tvCollectionInfo != null) {
                                tvCollectionInfo.setText((col.isPublic() ? "Công khai" : "Riêng tư") + " • " + updatedCount + " món ăn");
                            }
                        }
                    });
                }

                layoutCollectionsList.addView(itemView);
            }
        }

        if (btnCreateNewCollection != null) {
            btnCreateNewCollection.setOnClickListener(v -> {
                dialog.dismiss();
                showCreateCollectionDialog(context, recipe, onComplete);
            });
        }

        if (btnFinishSave != null) {
            btnFinishSave.setOnClickListener(v -> {
                dialog.dismiss();
                if (onComplete != null) {
                    onComplete.run();
                }
            });
        }

        dialog.show();
    }

    private static void showCreateCollectionDialog(Context context, Recipe recipe, Runnable onComplete) {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.BottomSheetTheme);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_create_collection, null);
        dialog.setContentView(view);

        TextInputLayout tilCollectionName = view.findViewById(R.id.tilCollectionName);
        EditText etCollectionName = view.findViewById(R.id.etCollectionName);
        SwitchMaterial swCollectionPrivacy = view.findViewById(R.id.swCollectionPrivacy);
        View btnCancelCreateCollection = view.findViewById(R.id.btnCancelCreateCollection);
        View btnSubmitCreateCollection = view.findViewById(R.id.btnSubmitCreateCollection);

        if (btnCancelCreateCollection != null) {
            btnCancelCreateCollection.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSubmitCreateCollection != null) {
            btnSubmitCreateCollection.setOnClickListener(v -> {
                String name = etCollectionName != null ? etCollectionName.getText().toString().trim() : "";
                if (TextUtils.isEmpty(name)) {
                    if (tilCollectionName != null) {
                        tilCollectionName.setError("Tên bộ sưu tập không được để trống!");
                    } else if (etCollectionName != null) {
                        etCollectionName.setError("Tên bộ sưu tập không được để trống!");
                    }
                    return;
                }

                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    String colId = UUID.randomUUID().toString();
                    boolean isPublic = swCollectionPrivacy == null || swCollectionPrivacy.isChecked();
                    
                    RecipeCollection newCol = new RecipeCollection(
                            colId,
                            name,
                            isPublic,
                            currentUser.getUid()
                    );

                    newCol.getRecipeIds().add(String.valueOf(recipe.getId()));

                    FirebaseFirestore.getInstance()
                            .collection("collections")
                            .document(colId)
                            .set(newCol)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Đã tạo bộ sưu tập \"" + name + "\"!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                // Reopen the selection dialog to show the new collection with this recipe checked
                                showSaveToCollectionDialog(context, recipe, onComplete);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Không thể tạo bộ sưu tập: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            });
        }

        dialog.show();
    }

    public static void removeRecipeFromAllFavoritesAndCollections(Context context, int recipeId, Runnable onComplete) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        String recipeIdStr = String.valueOf(recipeId);
        android.util.Log.d("CollectionDiagnostic", "removeRecipeFromAllFavoritesAndCollections [START]: recipeId=" + recipeId);

        // 1. Remove from SharedPreferences bookmarked_recipes
        android.content.SharedPreferences prefs = context.getSharedPreferences("cookup_prefs", Context.MODE_PRIVATE);
        String uid = currentUser.getUid();
        java.util.HashSet<String> bookmarks = new java.util.HashSet<>(prefs.getStringSet("bookmarked_recipes_" + uid, new java.util.HashSet<>()));
        if (bookmarks.contains(recipeIdStr)) {
            bookmarks.remove(recipeIdStr);
            prefs.edit().putStringSet("bookmarked_recipes_" + uid, bookmarks).apply();
            android.util.Log.d("CollectionDiagnostic", "removeRecipeFromAllFavoritesAndCollections [SHARED_PREFS]: Removed recipeId=" + recipeId + " from SharedPreferences");
        }

        // 2. Remove from all Firestore collections
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("collections")
                .whereEqualTo("creatorUid", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        com.google.firebase.firestore.WriteBatch batch = db.batch();
                        boolean needsCommit = false;
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            RecipeCollection col = doc.toObject(RecipeCollection.class);
                            if (col != null) {
                                String colId = col.getId();
                                if (colId == null || colId.trim().isEmpty()) {
                                    colId = doc.getId();
                                    col.setId(colId);
                                }
                                boolean containsRecipe = col.getRecipeIds() != null && col.getRecipeIds().contains(recipeIdStr);
                                android.util.Log.d("CollectionDiagnostic", "removeRecipeFromAllFavoritesAndCollections [CHECKING]: colId=" + colId + ", name=\"" + col.getName() + "\", containsRecipe=" + containsRecipe);
                                if (containsRecipe) {
                                    col.getRecipeIds().remove(recipeIdStr);
                                    android.util.Log.d("CollectionDiagnostic", "removeRecipeFromAllFavoritesAndCollections [BATCH_REMOVE]: Queueing removal of recipeId=" + recipeIdStr + " from colId=" + colId);
                                    batch.update(db.collection("collections").document(colId), "recipeIds", com.google.firebase.firestore.FieldValue.arrayRemove(recipeIdStr));
                                    needsCommit = true;
                                }
                            }
                        }
                        if (needsCommit) {
                            android.util.Log.d("CollectionDiagnostic", "removeRecipeFromAllFavoritesAndCollections [BATCH_COMMIT]: Committing batch update to Firestore...");
                            batch.commit()
                                    .addOnSuccessListener(aVoid -> {
                                        android.util.Log.d("CollectionDiagnostic", "removeRecipeFromAllFavoritesAndCollections [BATCH_COMMIT - SUCCESS]");
                                        if (onComplete != null) onComplete.run();
                                    })
                                    .addOnFailureListener(e -> {
                                        android.util.Log.e("CollectionDiagnostic", "removeRecipeFromAllFavoritesAndCollections [BATCH_COMMIT - FAILURE]", e);
                                        if (onComplete != null) onComplete.run();
                                    });
                        } else {
                            android.util.Log.d("CollectionDiagnostic", "removeRecipeFromAllFavoritesAndCollections [NO_CHANGE]: Recipe was not present in any collections");
                            if (onComplete != null) onComplete.run();
                        }
                    } else {
                        android.util.Log.d("CollectionDiagnostic", "removeRecipeFromAllFavoritesAndCollections [NO_COLLECTIONS]: User has no collections");
                        if (onComplete != null) onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CollectionDiagnostic", "removeRecipeFromAllFavoritesAndCollections [FAILURE]: Firestore retrieval failed", e);
                    if (onComplete != null) onComplete.run();
                });
    }
}
