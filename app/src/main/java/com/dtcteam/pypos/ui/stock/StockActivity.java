package com.dtcteam.pypos.ui.stock;

import android.os.Bundle;
import android.view.View;
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
        loadLowStockItems();
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
    }

    private void loadLowStockItems() {
        showSkeleton(true);
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.getLowStockItems(new ApiService.Callback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> result) {
                binding.loadingIndicator.setVisibility(View.GONE);
                showSkeleton(false);
                items.clear();
                if (result != null) {
                    items.addAll(result);
                }
                adapter.setItems(items);
                binding.tvLowStockCount.setText(items.size() + " items");
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
