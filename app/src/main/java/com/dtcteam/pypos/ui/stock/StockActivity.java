package com.dtcteam.pypos.ui.stock;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivityStockBinding;
import com.dtcteam.pypos.databinding.DialogSelectItemBinding;
import com.dtcteam.pypos.databinding.DialogStockAdjustBinding;
import com.dtcteam.pypos.model.Item;
import com.dtcteam.pypos.model.StockMovement;
import com.dtcteam.pypos.ui.common.SkeletonAdapter;
import com.dtcteam.pypos.ui.items.ItemAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class StockActivity extends AppCompatActivity {

    private ActivityStockBinding binding;
    private final ApiService api = ApiService.getInstance();
    private ArrayList<Item> items = new ArrayList<>();
    private ItemAdapter adapter;
    private SkeletonAdapter skeletonAdapter;
    private StockMovementAdapter movementAdapter;
    private String currentMode = "in";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        setupListeners();
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
                showStockDialog(item);
            }

            @Override
            public void onDeleteClick(Item item) {
                Toast.makeText(StockActivity.this, "Delete from Items screen", Toast.LENGTH_SHORT).show();
            }
        });
        binding.stockRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.stockRecyclerView.setAdapter(adapter);
        
        movementAdapter = new StockMovementAdapter();
        binding.movementsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.movementsRecyclerView.setAdapter(movementAdapter);
    }

    private void showSkeleton(boolean show) {
        if (show) {
            binding.stockRecyclerView.setAdapter(skeletonAdapter);
        } else {
            binding.stockRecyclerView.setAdapter(adapter);
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        
        binding.btnStockIn.setOnClickListener(v -> {
            currentMode = "in";
            showItemSelector();
        });
        
        binding.btnStockOut.setOnClickListener(v -> {
            currentMode = "out";
            showItemSelector();
        });
        
        binding.btnAdjust.setOnClickListener(v -> {
            currentMode = "adjust";
            showItemSelector();
        });
        
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
    
    private void showItemSelector() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogSelectItemBinding dialogBinding = DialogSelectItemBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());
        
        ItemAdapter itemAdapter = new ItemAdapter();
        itemAdapter.setItems(items);
        
        itemAdapter.setOnItemClickListener(new ItemAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(Item item) {
                dialog.dismiss();
                showStockDialog(item);
            }

            @Override
            public void onDeleteClick(Item item) {}
        });
        
        dialogBinding.itemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dialogBinding.itemsRecyclerView.setAdapter(itemAdapter);
        
        dialogBinding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().toLowerCase();
                ArrayList<Item> filtered = new ArrayList<>();
                for (Item item : items) {
                    if (item.getName().toLowerCase().contains(query) || 
                        item.getSku().toLowerCase().contains(query)) {
                        filtered.add(item);
                    }
                }
                itemAdapter.setItems(filtered);
            }
        });
        
        dialogBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void showStockDialog(Item item) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogStockAdjustBinding dialogBinding = DialogStockAdjustBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());
        
        TextView tvTitle = dialogBinding.tvTitle;
        TextView tvItemName = dialogBinding.tvItemName;
        TextView tvCurrentStock = dialogBinding.tvCurrentStock;
        
        if (currentMode.equals("in")) {
            tvTitle.setText("Stock In");
        } else if (currentMode.equals("out")) {
            tvTitle.setText("Stock Out");
        } else {
            tvTitle.setText("Adjust Stock");
        }
        
        if (item != null) {
            tvItemName.setText(item.getName() + " (" + item.getSku() + ")");
            tvCurrentStock.setText("Current Stock: " + item.getQuantity());
        } else {
            tvItemName.setText("All Items");
            tvCurrentStock.setText("Select items to update");
        }
        
        dialogBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.btnSave.setOnClickListener(v -> {
            String qtyStr = dialogBinding.etQuantity.getText() != null ? 
                dialogBinding.etQuantity.getText().toString().trim() : "";
            
            if (qtyStr.isEmpty()) {
                Toast.makeText(this, "Quantity is required", Toast.LENGTH_SHORT).show();
                return;
            }
            
            int qty = 0;
            try {
                qty = Integer.parseInt(qtyStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (item != null) {
                updateStock(item, qty, dialog);
            } else {
                Toast.makeText(this, "Please select an item first", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }
    
    private void updateStock(Item item, int qty, BottomSheetDialog dialog) {
        int newQty = item.getQuantity();
        
        if (currentMode.equals("in")) {
            newQty += qty;
        } else if (currentMode.equals("out")) {
            newQty -= qty;
            if (newQty < 0) newQty = 0;
        } else {
            newQty = qty;
        }
        
        item.setQuantity(newQty);
        api.updateItem(item, new ApiService.Callback<Item>() {
            @Override
            public void onSuccess(Item result) {
                StockMovement movement = new StockMovement();
                movement.setItemId(item.getId());
                movement.setType(currentMode);
                movement.setQuantity(qty);
                
                api.createStockMovement(movement, new ApiService.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {}

                    @Override
                    public void onError(String error) {}
                });
                
                dialog.dismiss();
                Toast.makeText(StockActivity.this, "Stock updated successfully", Toast.LENGTH_SHORT).show();
                loadItems();
                loadMovements();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(StockActivity.this, error, Toast.LENGTH_SHORT).show();
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
                item.getSku().toLowerCase().contains(search)) {
                filtered.add(item);
            }
        }
        adapter.setItems(filtered);
    }

    private void loadItems() {
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
                        if (!item.isService()) {
                            items.add(item);
                        }
                    }
                }
                adapter.setItems(items);
            }

            @Override
            public void onError(String error) {
                binding.loadingIndicator.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                showSkeleton(false);
                Toast.makeText(StockActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
        
        loadMovements();
    }
    
    private void loadMovements() {
        api.getStockMovements(new ApiService.Callback<List<StockMovement>>() {
            @Override
            public void onSuccess(List<StockMovement> result) {
                ArrayList<StockMovement> movements = new ArrayList<>();
                if (result != null) {
                    movements.addAll(result);
                }
                movementAdapter.setMovements(movements);
            }

            @Override
            public void onError(String error) {
                // Silently fail
            }
        });
    }
}
