package com.dtcteam.pypos.ui.reports;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivityReportsBinding;
import com.dtcteam.pypos.model.Sale;
import com.dtcteam.pypos.ui.common.SkeletonAdapter;
import com.dtcteam.pypos.ui.sales.SalesAdapter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportsActivity extends AppCompatActivity {

    private ActivityReportsBinding binding;
    private final ApiService api = ApiService.getInstance();
    private ArrayList<Sale> sales = new ArrayList<>();
    private SalesAdapter adapter;
    private SkeletonAdapter skeletonAdapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("sw", "TZ"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportsBinding.inflate(getLayoutInflater());
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
        binding.reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.reportsRecyclerView.setAdapter(adapter);
    }

    private void showSkeleton(boolean show) {
        if (show) {
            binding.reportsRecyclerView.setAdapter(skeletonAdapter);
        } else {
            binding.reportsRecyclerView.setAdapter(adapter);
        }
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
                double todayTotal = 0;
                double totalSales = 0;
                
                if (result != null) {
                    sales.addAll(result);
                    String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
                    
                    for (Sale sale : result) {
                        totalSales += sale.getFinalAmount();
                        if (sale.getCreatedAt() != null && sale.getCreatedAt().startsWith(today)) {
                            todayTotal += sale.getFinalAmount();
                        }
                    }
                }
                
                adapter.setSales(sales);
                binding.tvTodaySales.setText(currencyFormat.format(todayTotal));
                binding.tvTotalSales.setText(currencyFormat.format(totalSales));
            }

            @Override
            public void onError(String error) {
                binding.loadingIndicator.setVisibility(View.GONE);
                showSkeleton(false);
                Toast.makeText(ReportsActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
    }
}
