package com.dtcteam.pypos.ui.sales;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivitySalesBinding;
import com.dtcteam.pypos.model.Sale;
import com.dtcteam.pypos.ui.common.SkeletonAdapter;
import java.util.ArrayList;
import java.util.List;

public class SalesActivity extends AppCompatActivity {

    private ActivitySalesBinding binding;
    private final ApiService api = ApiService.getInstance();
    private ArrayList<Sale> sales = new ArrayList<>();
    private SalesAdapter adapter;
    private SkeletonAdapter skeletonAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySalesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        setupListeners();
        loadSales();
    }

    private void setupRecyclerView() {
        skeletonAdapter = new SkeletonAdapter();
        skeletonAdapter.setLayoutResId(R.layout.item_row_skeleton);
        skeletonAdapter.setItemCount(5);
        
        adapter = new SalesAdapter();
        binding.salesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.salesRecyclerView.setAdapter(adapter);
    }

    private void showSkeleton(boolean show) {
        if (show) {
            binding.salesRecyclerView.setAdapter(skeletonAdapter);
        } else {
            binding.salesRecyclerView.setAdapter(adapter);
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void loadSales() {
        showSkeleton(true);
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.getSales(new ApiService.Callback<List<Sale>>() {
            @Override
            public void onSuccess(List<Sale> result) {
                binding.loadingIndicator.setVisibility(View.GONE);
                showSkeleton(false);
                sales.clear();
                if (result != null) {
                    sales.addAll(result);
                }
                adapter.setSales(sales);
            }

            @Override
            public void onError(String error) {
                binding.loadingIndicator.setVisibility(View.GONE);
                showSkeleton(false);
                Toast.makeText(SalesActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
