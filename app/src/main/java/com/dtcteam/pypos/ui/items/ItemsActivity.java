package com.dtcteam.pypos.ui.items;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivityItemsBinding;
import com.dtcteam.pypos.databinding.DialogItemFormBinding;
import com.dtcteam.pypos.model.Category;
import com.dtcteam.pypos.model.Item;
import com.dtcteam.pypos.ui.common.SkeletonAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class ItemsActivity extends AppCompatActivity {

    private ActivityItemsBinding binding;
    private final ApiService api = ApiService.getInstance();
    private ArrayList<Item> items = new ArrayList<>();
    private ItemAdapter adapter;
    private SkeletonAdapter skeletonAdapter;
    private ArrayList<Category> categories = new ArrayList<>();
    private Item editingItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityItemsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        setupListeners();
        loadCategories();
        loadItems();
    }

    private void setupRecyclerView() {
        skeletonAdapter = new SkeletonAdapter();
        skeletonAdapter.setLayoutResId(R.layout.item_row_skeleton);
        skeletonAdapter.setItemCount(5);
        
        adapter = new ItemAdapter();
        adapter.setOnItemClickListener(new ItemAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(Item item) {
                showItemDialog(item);
            }

            @Override
            public void onDeleteClick(Item item) {
                deleteItem(item);
            }
        });
        binding.itemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.itemsRecyclerView.setAdapter(adapter);
    }

    private void showSkeleton(boolean show) {
        if (show) {
            binding.itemsRecyclerView.setAdapter(skeletonAdapter);
        } else {
            binding.itemsRecyclerView.setAdapter(adapter);
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.fabAdd.setOnClickListener(v -> showItemDialog(null));
        
        binding.btnImport.setOnClickListener(v -> {
            Toast.makeText(this, "Import feature - Coming soon", Toast.LENGTH_SHORT).show();
        });
        
        binding.btnExport.setOnClickListener(v -> {
            exportItems();
        });
        
        binding.btnAdd.setOnClickListener(v -> showItemDialog(null));
        
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                filterItems(s.toString());
            }
        });
    }
    
    private void filterItems(String query) {
        if (query.isEmpty()) {
            adapter.setItems(items);
            return;
        }
        
        String search = query.toLowerCase();
        ArrayList<Item> filtered = new ArrayList<>();
        for (Item item : items) {
            if (item.getName().toLowerCase().contains(search) || 
                item.getSku().toLowerCase().contains(search) ||
                (item.getCategoryName() != null && item.getCategoryName().toLowerCase().contains(search))) {
                filtered.add(item);
            }
        }
        adapter.setItems(filtered);
    }

    private void loadItemsFromApi() {
        showSkeleton(true);
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.getItems(null, null, new ApiService.Callback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> result) {
                binding.loadingIndicator.setVisibility(View.GONE);
                showSkeleton(false);
                items.clear();
                if (result != null) {
                    for (Item item : result) {
                        if (!item.isService()) {
                            items.add(item);
                        }
                    }
                }
                String search = binding.etSearch.getText() != null ? binding.etSearch.getText().toString() : "";
                if (!search.isEmpty()) {
                    filterItems(search);
                } else {
                    adapter.setItems(items);
                }
            }

            @Override
            public void onError(String error) {
                binding.loadingIndicator.setVisibility(View.GONE);
                showSkeleton(false);
                Toast.makeText(ItemsActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadCategories() {
        api.getCategories(new ApiService.Callback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> result) {
                categories.clear();
                if (result != null) {
                    categories.addAll(result);
                }
            }

            @Override
            public void onError(String error) {
                // Ignore silently
            }
        });
    }

    private void loadItems() {
        loadItemsFromApi();
    }

    private void showItemDialog(Item item) {
        editingItem = item;
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogItemFormBinding dialogBinding = DialogItemFormBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        TextInputEditText etName = dialogBinding.etName;
        TextInputEditText etSku = dialogBinding.etSku;
        AutoCompleteTextView actvCategory = dialogBinding.actvCategory;
        TextInputEditText etPrice = dialogBinding.etPrice;
        TextInputEditText etCost = dialogBinding.etCost;
        TextInputEditText etQuantity = dialogBinding.etQuantity;
        TextInputEditText etMinStock = dialogBinding.etMinStock;
        MaterialButton btnCancel = dialogBinding.btnCancel;
        MaterialButton btnSave = dialogBinding.btnSave;

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<String>());
        for (Category cat : categories) {
            categoryAdapter.add(cat.getName());
        }
        actvCategory.setAdapter(categoryAdapter);

        if (item != null) {
            dialogBinding.tvTitle.setText("Edit Item");
            etName.setText(item.getName());
            etSku.setText(item.getSku());
            etPrice.setText(String.valueOf(item.getUnitPrice()));
            etCost.setText(String.valueOf(item.getCost()));
            etQuantity.setText(String.valueOf(item.getQuantity()));
            etMinStock.setText(String.valueOf(item.getMinStockLevel()));
            if (item.getCategoryName() != null) {
                actvCategory.setText(item.getCategoryName(), false);
            }
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String sku = etSku.getText() != null ? etSku.getText().toString().trim() : "";
            String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
            String costStr = etCost.getText() != null ? etCost.getText().toString().trim() : "";
            String quantityStr = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";
            String minStockStr = etMinStock.getText() != null ? etMinStock.getText().toString().trim() : "";

            if (name.isEmpty()) {
                Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = 0;
            double cost = 0;
            int quantity = 0;
            int minStock = 5;
            try {
                if (!priceStr.isEmpty()) price = Double.parseDouble(priceStr);
                if (!costStr.isEmpty()) cost = Double.parseDouble(costStr);
                if (!quantityStr.isEmpty()) quantity = Integer.parseInt(quantityStr);
                if (!minStockStr.isEmpty()) minStock = Integer.parseInt(minStockStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
                return;
            }

            Integer categoryId = null;
            String selectedCategory = actvCategory.getText().toString();
            for (Category cat : categories) {
                if (cat.getName().equals(selectedCategory)) {
                    categoryId = cat.getId();
                    break;
                }
            }

            Item newItem = new Item();
            newItem.setName(name);
            newItem.setSku(sku);
            newItem.setUnitPrice(price);
            newItem.setCost(cost);
            newItem.setQuantity(quantity);
            newItem.setMinStockLevel(minStock);
            newItem.setCategoryId(categoryId);
            newItem.setActive(true);
            newItem.setService(false);

            if (item != null) {
                newItem.setId(item.getId());
                api.updateItem(newItem, new ApiService.Callback<Item>() {
                    @Override
                    public void onSuccess(Item result) {
                        dialog.dismiss();
                        loadItems();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(ItemsActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                api.createItem(newItem, new ApiService.Callback<Item>() {
                    @Override
                    public void onSuccess(Item result) {
                        dialog.dismiss();
                        loadItems();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(ItemsActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    private void deleteItem(Item item) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete " + item.getName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                api.deleteItem(item.getId(), new ApiService.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        loadItems();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(ItemsActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void exportItems() {
        if (items.isEmpty()) {
            Toast.makeText(this, "No items to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        StringBuilder csv = new StringBuilder();
        csv.append("SKU,Name,Category,Unit Price,Cost,Stock,Min Stock,Active,Service\n");
        
        for (Item item : items) {
            csv.append("\"").append(item.getSku()).append("\",");
            csv.append("\"").append(item.getName()).append("\",");
            csv.append("\"").append(item.getCategoryName()).append("\",");
            csv.append(item.getUnitPrice()).append(",");
            csv.append(item.getCost()).append(",");
            csv.append(item.getQuantity()).append(",");
            csv.append(item.getMinStockLevel()).append(",");
            csv.append(item.isActive()).append(",");
            csv.append(item.isService()).append("\n");
        }
        
        String fileName = "items_export_" + System.currentTimeMillis() + ".csv";
        
        try {
            java.io.FileOutputStream fos = openFileOutput(fileName, MODE_PRIVATE);
            fos.write(csv.toString().getBytes());
            fos.close();
            Toast.makeText(this, "Exported to " + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadTemplate() {
        String template = "SKU,Name,Category,Unit Price,Cost,Stock,Min Stock,Active,Service\n";
        template += "SKU-001,Sample Item,Category Name,100,50,10,5,true,false\n";
        
        String fileName = "items_template.csv";
        
        try {
            java.io.FileOutputStream fos = openFileOutput(fileName, MODE_PRIVATE);
            fos.write(template.getBytes());
            fos.close();
            Toast.makeText(this, "Template saved to " + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
