package com.example.bossapp.presentation.category;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.business.CategoryManager;
import com.example.bossapp.data.model.Category;
import com.example.bossapp.data.repository.CategoryRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class CreateCategoryFragment extends Fragment {

    private CategoryManager categoryManager;


    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private final List<Category> categories = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_category, container, false);

        categoryManager = new CategoryManager();

        recyclerView = view.findViewById(R.id.rv_categories);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CategoryAdapter(categories, requireContext(), (category, currentColor) -> {
            showChangeColorDialog(category);
        });
        recyclerView.setAdapter(adapter);


        view.findViewById(R.id.fab_add_category).setOnClickListener(v -> showAddCategoryDialog());

        loadCategories();

        return view;
    }

    private void loadCategories() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("categories")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        e.printStackTrace();
                        return;
                    }

                    if (querySnapshot != null) {
                        categories.clear();
                        for (var doc : querySnapshot.getDocuments()) {
                            Category category = doc.toObject(Category.class);
                            if (category != null) {
                                category.setId(doc.getId()); // postavi ID dokumenta
                                categories.add(category);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }



    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        EditText etName = dialogView.findViewById(R.id.et_category_name);
        Button btnPickColor = dialogView.findViewById(R.id.btn_pick_color);
        View colorPreview = dialogView.findViewById(R.id.view_color_preview);

        final int[] selectedColor = {Color.RED};

        btnPickColor.setOnClickListener(v -> {
            View colorDialog = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_color_picker, null);
            View preview = colorDialog.findViewById(R.id.color_preview);
            SeekBar seekR = colorDialog.findViewById(R.id.seek_red);
            SeekBar seekG = colorDialog.findViewById(R.id.seek_green);
            SeekBar seekB = colorDialog.findViewById(R.id.seek_blue);

            int red = Color.red(selectedColor[0]);
            int green = Color.green(selectedColor[0]);
            int blue = Color.blue(selectedColor[0]);

            seekR.setProgress(red);
            seekG.setProgress(green);
            seekB.setProgress(blue);
            preview.setBackgroundColor(selectedColor[0]);

            SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int color = Color.rgb(seekR.getProgress(), seekG.getProgress(), seekB.getProgress());
                    preview.setBackgroundColor(color);
                }

                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            };

            seekR.setOnSeekBarChangeListener(listener);
            seekG.setOnSeekBarChangeListener(listener);
            seekB.setOnSeekBarChangeListener(listener);

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Izaberi boju")
                    .setView(colorDialog)
                    .setPositiveButton("OK", (d, w) -> {
                        selectedColor[0] = Color.rgb(seekR.getProgress(), seekG.getProgress(), seekB.getProgress());
                        colorPreview.setBackgroundColor(selectedColor[0]);
                    })
                    .setNegativeButton("Otkaži", null)
                    .show();
        });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Nova kategorija")
                .setView(dialogView)
                .setPositiveButton("Sačuvaj", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Unesite naziv kategorije!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (Category c : categories) {
                        if (c.getName().equalsIgnoreCase(name)) {
                            Toast.makeText(requireContext(), "Kategorija sa tim imenom već postoji!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }


                    String colorHex = String.format("#%06X", (0xFFFFFF & selectedColor[0]));
                    String ownerId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

                    categoryManager.addCategory(
                            new Category(name, colorHex, ownerId),
                            new CategoryRepository.OnCategoryActionListener() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(requireContext(), "Kategorija dodata!", Toast.LENGTH_SHORT).show();

                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    private void showChangeColorDialog(Category category) {
        View colorDialog = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_color_picker, null);
        View preview = colorDialog.findViewById(R.id.color_preview);
        SeekBar seekR = colorDialog.findViewById(R.id.seek_red);
        SeekBar seekG = colorDialog.findViewById(R.id.seek_green);
        SeekBar seekB = colorDialog.findViewById(R.id.seek_blue);

        int color = Color.parseColor(category.getColorHex());
        seekR.setProgress(Color.red(color));
        seekG.setProgress(Color.green(color));
        seekB.setProgress(Color.blue(color));
        preview.setBackgroundColor(color);

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newColor = Color.rgb(seekR.getProgress(), seekG.getProgress(), seekB.getProgress());
                preview.setBackgroundColor(newColor);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        seekR.setOnSeekBarChangeListener(listener);
        seekG.setOnSeekBarChangeListener(listener);
        seekB.setOnSeekBarChangeListener(listener);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Promeni boju kategorije")
                .setView(colorDialog)
                .setPositiveButton("Sačuvaj", (dialog, which) -> {
                    int newColor = Color.rgb(seekR.getProgress(), seekG.getProgress(), seekB.getProgress());
                    String newColorHex = String.format("#%06X", (0xFFFFFF & newColor));

                    for (Category c : categories) {
                        if (c.getColorHex().equalsIgnoreCase(newColorHex)) {
                            Toast.makeText(requireContext(), "Boja je već zauzeta!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    categoryManager.changeCategoryColor(category, newColorHex, new CategoryRepository.OnCategoryActionListener() {
                        @Override
                        public void onSuccess() {
                            adapter.notifyDataSetChanged();
                            Toast.makeText(requireContext(), "Boja uspešno promenjena!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Otkaži", null)
                .show();
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
