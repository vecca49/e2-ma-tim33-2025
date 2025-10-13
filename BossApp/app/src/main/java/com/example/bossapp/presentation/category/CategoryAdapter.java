package com.example.bossapp.presentation.category;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.data.model.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface OnColorChangeListener {
        void onColorChange(Category category, String newColor);
    }

    private List<Category> categories;
    private final Context context;
    private final OnColorChangeListener listener;

    public CategoryAdapter(List<Category> categories, Context context, OnColorChangeListener listener) {
        this.categories = categories;
        this.context = context;
        this.listener = listener;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category c = categories.get(position);
        holder.name.setText(c.getName());
        holder.color.setBackgroundColor(Color.parseColor(c.getColorHex()));

        holder.color.setOnClickListener(v -> {
            if (listener != null) {
                listener.onColorChange(c, c.getColorHex());
            }
        });

        holder.btnChangeColor.setOnClickListener(v -> {
            if (listener != null) {
                listener.onColorChange(c, c.getColorHex());
            }
        });
    }



    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        View color;
        View btnChangeColor;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_category_name);
            color = itemView.findViewById(R.id.view_color);
            btnChangeColor = itemView.findViewById(R.id.btn_change_color);
        }
    }

}
