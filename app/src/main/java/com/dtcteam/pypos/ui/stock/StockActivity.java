package com.dtcteam.pypos.ui.stock;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivityStockBinding;
import com.dtcteam.pypos.model.Item;
import com.dtcteam.pypos.ui.common.SkeletonAdapter;
import com.dtcteam.pypos.ui.items.ItemAdapter;
import java.util.ArrayList;
import java.util.List;

public class StockActivity extends AppCompatActivity {

    private ActivityStockBinding binding;
    private final ApiService api = ApiService.getInstance();
    private ArrayList<Item> items = new ArrayList<>();
    private ItemAdapter adapter;
    private SkeletonAdapter skeletonAdapter;

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
                Toast.makeText(StockActivity.this, "Edit from Items screen", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClick(Item item) {
                Toast.makeText(StockActivity.this, "Delete from Items screen", Toast.LENGTH_SHORT).show();
            }
        });
        binding.stockRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.stockRecyclerView.setAdapter(adapter);
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
            Toast.makeText(this, "Stock In feature - Coming soon", Toast.LENGTH_SHORT).show();
        });
        
        binding.btnStockOut.setOnClickListener(v -> {
            Toast.makeText(this, "Stock Out feature - Coming soon", Toast.LENGTH_SHORT).show();
        });
        
        binding.btnAdjust.setOnClickListener(v -> {
            Toast.makeText(this, "Adjust feature - Coming soon", Toast.LENGTH_SHORT).show();
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
                showSkeleton(false);
                Toast.makeText(StockActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
