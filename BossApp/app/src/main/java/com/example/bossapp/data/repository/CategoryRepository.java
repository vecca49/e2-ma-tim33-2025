package com.example.bossapp.data.repository;

import com.example.bossapp.data.model.Category;
import com.google.firebase.firestore.FirebaseFirestore;

public class CategoryRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String COLLECTION_NAME = "categories";

    public interface OnCategoryActionListener {
        void onSuccess();
        void onError(Exception e);
    }

    public void addCategory(Category category, OnCategoryActionListener listener) {
        db.collection(COLLECTION_NAME)
                .add(category)
                .addOnSuccessListener(documentReference -> {
                    category.setId(documentReference.getId()); // VAŽNO: Postavi ID
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onError);
    }

    public void updateCategoryColor(String categoryId, String newColorHex, OnCategoryActionListener listener) {
        db.collection(COLLECTION_NAME)
                .document(categoryId)
                .update("colorHex", newColorHex)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

}
