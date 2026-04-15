package com.dtcteam.pypos.ui.items;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivityItemsBinding;
import com.dtcteam.pypos.databinding.DialogItemFormBinding;
import com.dtcteam.pypos.model.Category;
import com.dtcteam.pypos.model.Item;
import com.dtcteam.pypos.ui.common.SkeletonAdapter;
import com.dtcteam.pypos.util.PdfExportUtil;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemsActivity extends AppCompatActivity {

    private ActivityItemsBinding binding;
    private final ApiService api = ApiService.getInstance();
    private ArrayList<Item> items = new ArrayList<>();
    private ItemAdapter adapter;
    private SkeletonAdapter skeletonAdapter;
    private ArrayList<Category> categories = new ArrayList<>();
    private Item editingItem;
    private byte[] selectedImageBytes;
    private String selectedImageUrl;
    private DialogItemFormBinding currentDialogBinding;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                if (imageUri != null && currentDialogBinding != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        byte[] data = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(data)) != -1) {
                            buffer.write(data, 0, bytesRead);
                        }
                        selectedImageBytes = buffer.toByteArray();
                        currentDialogBinding.ivItemImage.setImageURI(imageUri);
                        currentDialogBinding.ivItemImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    );

    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestPermission(),
        isGranted -> {
            if (isGranted) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission required to select images", Toast.LENGTH_SHORT).show();
            }
        }
    );

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
        binding.fabAdd.setVisibility(View.GONE);
        
        binding.btnImport.setOnClickListener(v -> {
            importItems();
        });
        
        binding.btnExport.setOnClickListener(v -> {
            exportItems();
        });
        
        binding.btnExportPdf.setOnClickListener(v -> {
            exportItemsPdf();
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
        
        binding.swipeRefresh.setOnRefreshListener(() -> {
            loadItems();
        });
    }
    
    private void filterItems(String query) {
        filterItems(query, binding.actvCategory.getText().toString());
    }
    
    private void filterItems(String query, String category) {
        ArrayList<Item> filtered = new ArrayList<>();
        
        for (Item item : items) {
            boolean matchesSearch = query.isEmpty() || 
                item.getName().toLowerCase().contains(query.toLowerCase()) || 
                item.getSku().toLowerCase().contains(query.toLowerCase()) ||
                (item.getCategoryName() != null && item.getCategoryName().toLowerCase().contains(query.toLowerCase()));
            
            boolean matchesCategory = category.isEmpty() || category.equals("All") ||
                (item.getCategoryName() != null && item.getCategoryName().equals(category));
            
            if (matchesSearch && matchesCategory) {
                filtered.add(item);
            }
        }
        adapter.setItems(filtered);
    }

    private void setupCategoryFilter() {
        ArrayList<String> categoryNames = new ArrayList<>();
        categoryNames.add("All");
        for (Category cat : categories) {
            categoryNames.add(cat.getName());
        }
        
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, categoryNames);
        binding.actvCategory.setAdapter(categoryAdapter);
        
        binding.actvCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            filterItems(binding.etSearch.getText().toString(), selected);
        });
        
        binding.actvCategory.setText("All", false);
    }

    private void loadItemsFromApi() {
        showSkeleton(true);
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.getItems(null, null, new ApiService.Callback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> result) {
                binding.loadingIndicator.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                showSkeleton(false);
                items.clear();
                if (result != null) {
                    for (Item item : result) {
                        // Exclude services from items list
                        if (!item.isService()) {
                            items.add(item);
                        }
                    }
                }
                String search = binding.etSearch.getText() != null ? binding.etSearch.getText().toString() : "";
                String category = binding.actvCategory.getText() != null ? binding.actvCategory.getText().toString() : "";
                if (!search.isEmpty() || !category.isEmpty()) {
                    filterItems(search, category);
                } else {
                    adapter.setItems(items);
                }
            }

            @Override
            public void onError(String error) {
                binding.loadingIndicator.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
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
                setupCategoryFilter();
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
        selectedImageBytes = null;
        selectedImageUrl = item != null ? item.getImageUrl() : null;
        
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogItemFormBinding dialogBinding = DialogItemFormBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());
        currentDialogBinding = dialogBinding;

        TextInputEditText etName = dialogBinding.etName;
        TextInputEditText etSku = dialogBinding.etSku;
        AutoCompleteTextView actvCategory = dialogBinding.actvCategory;
        TextInputEditText etPrice = dialogBinding.etPrice;
        TextInputEditText etCost = dialogBinding.etCost;
        TextInputEditText etQuantity = dialogBinding.etQuantity;
        TextInputEditText etMinStock = dialogBinding.etMinStock;
        CheckBox cbIsService = dialogBinding.cbIsService;
        TextInputLayout tilQuantity = dialogBinding.tilQuantity;
        TextInputLayout tilMinStock = dialogBinding.tilMinStock;
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
            cbIsService.setChecked(item.isService());
            if (item.getCategoryName() != null) {
                actvCategory.setText(item.getCategoryName(), false);
            }
            // Show/hide stock fields based on service status
            tilQuantity.setVisibility(item.isService() ? View.GONE : View.VISIBLE);
            tilMinStock.setVisibility(item.isService() ? View.GONE : View.VISIBLE);
        }

        // Show/hide stock fields when checkbox changes
        cbIsService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tilQuantity.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            tilMinStock.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });

        // Image selection
        dialogBinding.btnSelectImage.setOnClickListener(v -> checkPermissionAndPickImage());
        
        // Load existing image if editing
        if (item != null && item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            selectedImageUrl = item.getImageUrl();
            new Thread(() -> {
                try {
                    okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                    okhttp3.Request request = new okhttp3.Request.Builder().url(item.getImageUrl()).build();
                    okhttp3.Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        byte[] imageData = response.body().bytes();
                        runOnUiThread(() -> {
                            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                            dialogBinding.ivItemImage.setImageBitmap(bitmap);
                        });
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }).start();
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
            // For services, quantity and min stock should be 0
            newItem.setQuantity(cbIsService.isChecked() ? 0 : quantity);
            newItem.setMinStockLevel(cbIsService.isChecked() ? 0 : minStock);
            newItem.setCategoryId(categoryId);
            newItem.setActive(true);
            newItem.setService(cbIsService.isChecked());
            newItem.setImageUrl(selectedImageUrl);

            if (selectedImageBytes != null) {
                String fileName = "item_" + System.currentTimeMillis() + ".jpg";
                btnSave.setEnabled(false);
                btnSave.setText("Uploading...");
                
                api.uploadImage(selectedImageBytes, fileName, new ApiService.Callback<String>() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        newItem.setImageUrl(imageUrl);
                        saveItem(newItem, item, dialog);
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            btnSave.setEnabled(true);
                            btnSave.setText("Save");
                            Toast.makeText(ItemsActivity.this, "Image upload failed, saving without image", Toast.LENGTH_SHORT).show();
                        });
                        saveItem(newItem, item, dialog);
                    }
                });
            } else {
                saveItem(newItem, item, dialog);
            }
        });
    }

    private void saveItem(Item newItem, Item originalItem, BottomSheetDialog dialog) {
        if (originalItem != null) {
            newItem.setId(originalItem.getId());
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
            java.io.File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            java.io.File pyposDir = new java.io.File(downloadsDir, "PyPOS");
            if (!pyposDir.exists()) {
                pyposDir.mkdirs();
            }
            java.io.File file = new java.io.File(pyposDir, fileName);
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(csv.toString());
            writer.close();
            
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(android.net.Uri.fromFile(file));
            sendBroadcast(intent);
            
            Toast.makeText(this, "Saved to Downloads/PyPOS/" + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportItemsPdf() {
        if (items.isEmpty()) {
            Toast.makeText(this, "No items to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        java.util.List<Map<String, Object>> exportData = new ArrayList<>();
        for (Item item : items) {
            Map<String, Object> row = new HashMap<>();
            row.put("sku", item.getSku());
            row.put("name", item.getName());
            row.put("category", item.getCategoryName());
            row.put("unit_price", item.getUnitPrice());
            row.put("cost_price", item.getCost());
            row.put("quantity", item.getQuantity());
            row.put("min_stock_level", item.getMinStockLevel());
            exportData.add(row);
        }
        
        PdfExportUtil.exportItems(this, exportData);
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
    
    private void importItems() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Import Items")
            .setMessage("To import items, place a CSV file named 'import_items.csv' in the app's internal storage.\n\nCSV format:\nSKU,Name,Category,Unit Price,Cost,Stock,Min Stock,Active,Service")
            .setPositiveButton("Import from File", (dialog, which) -> {
                try {
                    java.io.FileInputStream fis = openFileInput("import_items.csv");
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
                    String line;
                    int imported = 0;
                    int failed = 0;
                    
                    boolean isFirstLine = true;
                    while ((line = reader.readLine()) != null) {
                        if (isFirstLine) {
                            isFirstLine = false;
                            continue;
                        }
                        
                        String[] parts = line.split(",");
                        if (parts.length >= 5) {
                            Item item = new Item();
                            try {
                                if (parts.length > 0) item.setSku(parts[0].replace("\"", ""));
                                if (parts.length > 1) item.setName(parts[1].replace("\"", ""));
                                if (parts.length > 2) {
                                    String catName = parts[2].replace("\"", "");
                                    for (Category cat : categories) {
                                        if (cat.getName().equals(catName)) {
                                            item.setCategoryId(cat.getId());
                                            break;
                                        }
                                    }
                                }
                                if (parts.length > 3) item.setUnitPrice(Double.parseDouble(parts[3]));
                                if (parts.length > 4) item.setCost(Double.parseDouble(parts[4]));
                                if (parts.length > 5) item.setQuantity(Integer.parseInt(parts[5]));
                                if (parts.length > 6) item.setMinStockLevel(Integer.parseInt(parts[6]));
                                item.setActive(true);
                                item.setService(false);
                                
                                api.createItem(item, new ApiService.Callback<Item>() {
                                    @Override
                                    public void onSuccess(Item result) {}

                                    @Override
                                    public void onError(String error) {}
                                });
                                imported++;
                            } catch (Exception e) {
                                failed++;
                            }
                        }
                    }
                    reader.close();
                    fis.close();
                    
                    Toast.makeText(this, "Imported " + imported + " items" + (failed > 0 ? ", " + failed + " failed" : ""), Toast.LENGTH_LONG).show();
                    loadItems();
                } catch (Exception e) {
                    Toast.makeText(this, "No import file found or error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }
}
