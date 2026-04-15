package com.dtcteam.pypos.ui.categories;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivityCategoriesBinding;
import com.dtcteam.pypos.databinding.DialogCategoryFormBinding;
import com.dtcteam.pypos.model.Category;
import com.dtcteam.pypos.ui.common.SkeletonAdapter;
import com.dtcteam.pypos.util.PdfExportUtil;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoriesActivity extends AppCompatActivity {

    private ActivityCategoriesBinding binding;
    private final ApiService api = ApiService.getInstance();
    private ArrayList<Category> categories = new ArrayList<>();
    private CategoryAdapter adapter;
    private SkeletonAdapter skeletonAdapter;
    private Category editingCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoriesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        setupListeners();
        loadCategories();
    }

    private void setupRecyclerView() {
        skeletonAdapter = new SkeletonAdapter();
        skeletonAdapter.setLayoutResId(R.layout.item_category_skeleton);
        skeletonAdapter.setItemCount(5);
        
        adapter = new CategoryAdapter();
        adapter.setOnCategoryClickListener(new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onEditClick(Category category) {
                showCategoryDialog(category);
            }

            @Override
            public void onDeleteClick(Category category) {
                deleteCategory(category);
            }
        });
        binding.categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.categoriesRecyclerView.setAdapter(adapter);
    }

    private void showSkeleton(boolean show) {
        if (show) {
            binding.categoriesRecyclerView.setAdapter(skeletonAdapter);
        } else {
            binding.categoriesRecyclerView.setAdapter(adapter);
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnAdd.setOnClickListener(v -> showCategoryDialog(null));
        
        binding.btnImport.setOnClickListener(v -> {
            Toast.makeText(this, "Import feature - Coming soon", Toast.LENGTH_SHORT).show();
        });
        
        binding.btnExport.setOnClickListener(v -> {
            exportCategories();
        });
        
        binding.btnExportPdf.setOnClickListener(v -> {
            exportCategoriesPdf();
        });
        
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                filterCategories(s.toString());
            }
        });
        
        binding.swipeRefresh.setOnRefreshListener(() -> {
            loadCategories();
        });
    }
    
    private void filterCategories(String query) {
        if (query.isEmpty()) {
            adapter.setCategories(categories);
            return;
        }
        
        String search = query.toLowerCase();
        ArrayList<Category> filtered = new ArrayList<>();
        for (Category category : categories) {
            if (category.getName().toLowerCase().contains(search) || 
                (category.getDescription() != null && category.getDescription().toLowerCase().contains(search))) {
                filtered.add(category);
            }
        }
        adapter.setCategories(filtered);
    }

    private void exportCategories() {
        if (categories.isEmpty()) {
            Toast.makeText(this, "No categories to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        StringBuilder csv = new StringBuilder();
        csv.append("Name,Description\n");
        
        for (Category category : categories) {
            csv.append("\"").append(category.getName()).append("\",");
            csv.append("\"").append(category.getDescription() != null ? category.getDescription() : "").append("\"\n");
        }
        
        String fileName = "categories_export_" + System.currentTimeMillis() + ".csv";
        
        try {
            java.io.FileOutputStream fos = openFileOutput(fileName, MODE_PRIVATE);
            fos.write(csv.toString().getBytes());
            fos.close();
            Toast.makeText(this, "Exported to " + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportCategoriesPdf() {
        if (categories.isEmpty()) {
            Toast.makeText(this, "No categories to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        java.util.List<Map<String, Object>> exportData = new ArrayList<>();
        for (Category category : categories) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", category.getName());
            row.put("description", category.getDescription());
            exportData.add(row);
        }
        
        PdfExportUtil.exportCategories(this, exportData);
    }

    private void loadCategories() {
        showSkeleton(true);
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.getCategories(new ApiService.Callback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> result) {
                binding.loadingIndicator.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                showSkeleton(false);
                categories.clear();
                if (result != null) {
                    categories.addAll(result);
                }
                String search = binding.etSearch.getText() != null ? binding.etSearch.getText().toString() : "";
                if (!search.isEmpty()) {
                    filterCategories(search);
                } else {
                    adapter.setCategories(categories);
                }
            }

            @Override
            public void onError(String error) {
                binding.loadingIndicator.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                showSkeleton(false);
                Toast.makeText(CategoriesActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCategoryDialog(Category category) {
        editingCategory = category;
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogCategoryFormBinding dialogBinding = DialogCategoryFormBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        TextInputEditText etName = dialogBinding.etName;
        TextInputEditText etDescription = dialogBinding.etDescription;
        MaterialButton btnCancel = dialogBinding.btnCancel;
        MaterialButton btnSave = dialogBinding.btnSave;

        if (category != null) {
            dialogBinding.tvTitle.setText("Edit Category");
            etName.setText(category.getName());
            etDescription.setText(category.getDescription());
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

            if (name.isEmpty()) {
                Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            Category newCategory = new Category();
            newCategory.setName(name);
            newCategory.setDescription(description.isEmpty() ? null : description);

            if (category != null) {
                newCategory.setId(category.getId());
                api.updateCategory(newCategory, new ApiService.Callback<Category>() {
                    @Override
                    public void onSuccess(Category result) {
                        dialog.dismiss();
                        loadCategories();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(CategoriesActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                api.createCategory(newCategory, new ApiService.Callback<Category>() {
                    @Override
                    public void onSuccess(Category result) {
                        dialog.dismiss();
                        loadCategories();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(CategoriesActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    private void deleteCategory(Category category) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete " + category.getName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                api.deleteCategory(category.getId(), new ApiService.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        loadCategories();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(CategoriesActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
