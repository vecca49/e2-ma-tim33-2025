package com.example.bossapp.business;

import com.example.bossapp.data.model.Category;
import com.example.bossapp.data.repository.CategoryRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryManager {

    private final CategoryRepository repository;
    private final Set<String> usedColors = new HashSet<>();

    public CategoryManager() {
        this.repository = new CategoryRepository();
    }

    public interface OnCategoryActionListener {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnCategoriesLoadListener {
        void onSuccess(List<Category> categories);
        void onError(Exception e);
    }

    public void addCategory(Category category, CategoryRepository.OnCategoryActionListener listener) {
        if (usedColors.contains(category.getColorHex())) {
            listener.onError(new Exception("Boja je već zauzeta!"));
            return;
        }
        repository.addCategory(category, listener);
        usedColors.add(category.getColorHex());
    }

    public void changeCategoryColor(Category category, String newColorHex, CategoryRepository.OnCategoryActionListener listener) {
        if (usedColors.contains(newColorHex)) {
            listener.onError(new Exception("Boja je već zauzeta!"));
            return;
        }
        repository.updateCategoryColor(category.getId(), newColorHex, new CategoryRepository.OnCategoryActionListener() {
            @Override
            public void onSuccess() {
                usedColors.remove(category.getColorHex());
                category.setColorHex(newColorHex);
                usedColors.add(newColorHex);
                listener.onSuccess();
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e);
            }
        });
    }

    public void loadUserCategories(String userId, OnCategoriesLoadListener listener) {
        repository.getCategoriesForUser(userId, new CategoryRepository.OnGetCategoriesListener() {
            @Override
            public void onSuccess(List<Category> categories) {
                listener.onSuccess(categories);
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e);
            }
        });
    }



}
