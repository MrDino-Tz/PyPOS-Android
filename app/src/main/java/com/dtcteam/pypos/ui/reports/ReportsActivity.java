package com.dtcteam.pypos.ui.reports;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivityReportsBinding;
import com.dtcteam.pypos.model.Sale;
import com.dtcteam.pypos.ui.sales.SalesAdapter;
import com.dtcteam.pypos.ui.common.SkeletonAdapter;
import com.dtcteam.pypos.util.PdfExportUtil;
import com.google.android.material.tabs.TabLayout;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

        skeletonAdapter = new SkeletonAdapter();
        skeletonAdapter.setLayoutResId(R.layout.item_row_skeleton);
        skeletonAdapter.setItemCount(5);
        
        setupRecyclerView();
        setupListeners();
        showSkeleton(true);
        loadSales();
    }

    private void showSkeleton(boolean show) {
        if (show) {
            binding.monthlyBreakdownRecycler.setAdapter(skeletonAdapter);
            binding.stockArrivalsRecycler.setAdapter(skeletonAdapter);
        } else {
            binding.monthlyBreakdownRecycler.setAdapter(adapter);
            binding.stockArrivalsRecycler.setAdapter(adapter);
        }
    }

    private void setupRecyclerView() {
        adapter = new SalesAdapter();
        binding.monthlyBreakdownRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.monthlyBreakdownRecycler.setAdapter(adapter);
        
        binding.stockArrivalsRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.stockArrivalsRecycler.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        
        binding.btnExportCsv.setOnClickListener(v -> {
            exportAllReports();
        });
        
        binding.btnExportPdf.setOnClickListener(v -> {
            exportReportsPdf();
        });
        
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadTabData(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        binding.swipeRefresh.setOnRefreshListener(() -> {
            loadSales();
        });
    }
    
    private void loadTabData(int position) {
        switch (position) {
            case 0:
                loadDailyData();
                break;
            case 1:
                loadMonthlyData();
                break;
            case 2:
                loadStockData();
                break;
        }
    }
    
    private void loadDailyData() {
        double todayTotal = 0;
        int todayCount = 0;
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
        
        for (Sale sale : sales) {
            if (sale.getCreatedAt() != null && sale.getCreatedAt().startsWith(today)) {
                todayTotal += sale.getFinalAmount();
                todayCount++;
            }
        }
        
        binding.tvTodaySales.setText(currencyFormat.format(todayTotal));
        binding.tvTodayTransactions.setText(todayCount + " transactions");
        binding.tvMonthlyRevenue.setText(currencyFormat.format(todayTotal));
    }
    
    private void loadMonthlyData() {
        double monthlyTotal = 0;
        int monthlyCount = 0;
        String thisMonth = new java.text.SimpleDateFormat("yyyy-MM", Locale.US).format(new java.util.Date());
        
        for (Sale sale : sales) {
            if (sale.getCreatedAt() != null && sale.getCreatedAt().startsWith(thisMonth)) {
                monthlyTotal += sale.getFinalAmount();
                monthlyCount++;
            }
        }
        
        binding.tvTodaySales.setText(currencyFormat.format(monthlyTotal));
        binding.tvTodayTransactions.setText(monthlyCount + " transactions");
        binding.tvMonthlyRevenue.setText(currencyFormat.format(monthlyTotal));
    }
    
    private void loadStockData() {
        binding.tvTodaySales.setText("Stock Overview");
        binding.tvTodayTransactions.setText(sales.size() + " sales records");
        binding.tvMonthlyRevenue.setText("Stock movements");
    }
    
    private void exportAllReports() {
        if (sales.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        StringBuilder csv = new StringBuilder();
        csv.append("Sale ID,Date,Total Amount,Final Amount,Payment Method\n");
        
        for (Sale sale : sales) {
            csv.append(sale.getId()).append(",");
            csv.append("\"").append(sale.getCreatedAt()).append("\",");
            csv.append(sale.getTotalAmount()).append(",");
            csv.append(sale.getFinalAmount()).append(",");
            csv.append("\"").append(sale.getPaymentMethod()).append("\"\n");
        }
        
        String fileName = "reports_export_" + System.currentTimeMillis() + ".csv";
        
        try {
            java.io.FileOutputStream fos = openFileOutput(fileName, MODE_PRIVATE);
            fos.write(csv.toString().getBytes());
            fos.close();
            Toast.makeText(this, "Exported to " + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportReportsPdf() {
        if (sales.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        double totalRevenue = 0;
        for (Sale sale : sales) {
            totalRevenue += sale.getFinalAmount();
        }
        
        Map<String, Object> dailyData = new HashMap<>();
        List<Map<String, Object>> dailySales = new ArrayList<>();
        
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
        for (Sale sale : sales) {
            Map<String, Object> saleMap = new HashMap<>();
            saleMap.put("date", sale.getCreatedAt() != null ? sale.getCreatedAt().substring(0, 10) : "");
            saleMap.put("amount", sale.getFinalAmount());
            saleMap.put("items", 1);
            saleMap.put("status", "completed");
            dailySales.add(saleMap);
        }
        dailyData.put("sales", dailySales);
        
        Map<String, Object> monthlyData = new HashMap<>();
        
        PdfExportUtil.exportReports(this, dailyData, monthlyData, totalRevenue, sales.size());
    }

    private void loadSales() {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        showSkeleton(true);
        
        api.getSales(new ApiService.Callback<List<Sale>>() {
            @Override
            public void onSuccess(List<Sale> result) {
                binding.loadingIndicator.setVisibility(View.GONE);
                showSkeleton(false);
                sales.clear();
                double todayTotal = 0;
                double totalSales = 0;
                int todayCount = 0;
                
                if (result != null) {
                    sales.addAll(result);
                    String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
                    
                    for (Sale sale : result) {
                        totalSales += sale.getFinalAmount();
                        if (sale.getCreatedAt() != null && sale.getCreatedAt().startsWith(today)) {
                            todayTotal += sale.getFinalAmount();
                            todayCount++;
                        }
                    }
                }
                
                adapter.setSales(sales);
                binding.tvTodaySales.setText(currencyFormat.format(todayTotal));
                binding.tvTodayTransactions.setText(todayCount + " transactions");
                binding.tvMonthlyRevenue.setText(currencyFormat.format(totalSales));
                binding.tvTotalTransactions.setText(String.valueOf(sales.size()));
                binding.tvStockArrivals.setText("0");
            }

            @Override
            public void onError(String error) {
                binding.loadingIndicator.setVisibility(View.GONE);
                Toast.makeText(ReportsActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
