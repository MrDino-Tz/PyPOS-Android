package com.dtcteam.pypos.ui.reports;

import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivityReportsBinding;
import com.dtcteam.pypos.model.Sale;
import com.dtcteam.pypos.model.SaleItem;
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
        
        binding.btnExportOptions.setOnClickListener(v -> {
            com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
            View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_export, null);
            bottomSheet.setContentView(sheetView);
            
            sheetView.findViewById(R.id.btnExportExcelDaily).setOnClickListener(view -> {
                bottomSheet.dismiss();
                exportDailyPdf(); // Reusing PDF logic for now, or add CSV if available
            });
            
            sheetView.findViewById(R.id.btnExportExcelMonthly).setOnClickListener(view -> {
                bottomSheet.dismiss();
                exportMonthlyPdf();
            });
            
            sheetView.findViewById(R.id.btnExportPdfDaily).setOnClickListener(view -> {
                bottomSheet.dismiss();
                exportDailyPdf();
            });
            
            sheetView.findViewById(R.id.btnExportPdfMonthly).setOnClickListener(view -> {
                bottomSheet.dismiss();
                exportMonthlyPdf();
            });
            
            sheetView.findViewById(R.id.btnExportAll).setOnClickListener(view -> {
                bottomSheet.dismiss();
                exportDailyPdf(); // Or a combined function
            });
            
            bottomSheet.show();
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
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date()) + "T";
        
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
    
    private void exportDailyPdf() {
        if (sales.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date()) + "T";
        double dailyTotal = 0;
        List<Sale> dailySales = new ArrayList<>();
        
        for (Sale sale : sales) {
            if (sale.getCreatedAt() != null && sale.getCreatedAt().startsWith(today)) {
                dailySales.add(sale);
                dailyTotal += sale.getFinalAmount();
            }
        }
        
        if (dailySales.isEmpty()) {
            Toast.makeText(this, "No sales data for today", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Map<String, Object> dailyData = new HashMap<>();
        List<Map<String, Object>> salesList = new ArrayList<>();
        
        for (Sale sale : dailySales) {
            Map<String, Object> saleMap = new HashMap<>();
            saleMap.put("id", sale.getId());
            saleMap.put("date", sale.getCreatedAt() != null ? sale.getCreatedAt().substring(0, 16) : "");
            saleMap.put("amount", sale.getFinalAmount());
            saleMap.put("status", "completed");
            
            List<Map<String, Object>> itemList = new ArrayList<>();
            if (sale.getSaleItems() != null) {
                for (SaleItem si : sale.getSaleItems()) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("name", si.getItemName() != null ? si.getItemName() : "-");
                    itemMap.put("price", si.getUnitPrice());
                    itemMap.put("quantity", si.getQuantity());
                    itemMap.put("subtotal", si.getSubtotal());
                    itemList.add(itemMap);
                }
            }
            saleMap.put("items", itemList);
            salesList.add(saleMap);
        }
        dailyData.put("sales", salesList);
        
        PdfExportUtil.exportReports(this, dailyData, new HashMap<>(), dailyTotal, dailySales.size());
    }

    private void exportMonthlyPdf() {
        if (sales.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String thisMonth = new java.text.SimpleDateFormat("yyyy-MM", Locale.US).format(new java.util.Date());
        double monthlyTotal = 0;
        List<Sale> monthlySales = new ArrayList<>();
        
        for (Sale sale : sales) {
            if (sale.getCreatedAt() != null && sale.getCreatedAt().startsWith(thisMonth)) {
                monthlySales.add(sale);
                monthlyTotal += sale.getFinalAmount();
            }
        }
        
        if (monthlySales.isEmpty()) {
            Toast.makeText(this, "No sales data for this month", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Map<String, Object> monthlyData = new HashMap<>();
        List<Map<String, Object>> salesList = new ArrayList<>();
        
        for (Sale sale : monthlySales) {
            Map<String, Object> saleMap = new HashMap<>();
            saleMap.put("id", sale.getId());
            saleMap.put("date", sale.getCreatedAt() != null ? sale.getCreatedAt().substring(0, 16) : "");
            saleMap.put("amount", sale.getFinalAmount());
            saleMap.put("status", "completed");
            
            List<Map<String, Object>> itemList = new ArrayList<>();
            if (sale.getSaleItems() != null) {
                for (SaleItem si : sale.getSaleItems()) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("name", si.getItemName() != null ? si.getItemName() : "-");
                    itemMap.put("price", si.getUnitPrice());
                    itemMap.put("quantity", si.getQuantity());
                    itemMap.put("subtotal", si.getSubtotal());
                    itemList.add(itemMap);
                }
            }
            saleMap.put("items", itemList);
            salesList.add(saleMap);
        }
        monthlyData.put("sales", salesList);
        
        PdfExportUtil.exportReports(this, new HashMap<>(), monthlyData, monthlyTotal, monthlySales.size());
    }

    private void exportYearlyPdf() {
        if (sales.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String thisYear = new java.text.SimpleDateFormat("yyyy", Locale.US).format(new java.util.Date());
        double yearlyTotal = 0;
        List<Sale> yearlySales = new ArrayList<>();
        
        for (Sale sale : sales) {
            if (sale.getCreatedAt() != null && sale.getCreatedAt().startsWith(thisYear)) {
                yearlySales.add(sale);
                yearlyTotal += sale.getFinalAmount();
            }
        }
        
        if (yearlySales.isEmpty()) {
            Toast.makeText(this, "No sales data for this year", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Map<String, Object> yearlyData = new HashMap<>();
        List<Map<String, Object>> salesList = new ArrayList<>();
        
        for (Sale sale : yearlySales) {
            Map<String, Object> saleMap = new HashMap<>();
            saleMap.put("id", sale.getId());
            saleMap.put("date", sale.getCreatedAt() != null ? sale.getCreatedAt().substring(0, 16) : "");
            saleMap.put("amount", sale.getFinalAmount());
            saleMap.put("status", "completed");
            
            List<Map<String, Object>> itemList = new ArrayList<>();
            if (sale.getSaleItems() != null) {
                for (SaleItem si : sale.getSaleItems()) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("name", si.getItemName() != null ? si.getItemName() : "-");
                    itemMap.put("price", si.getUnitPrice());
                    itemMap.put("quantity", si.getQuantity());
                    itemMap.put("subtotal", si.getSubtotal());
                    itemList.add(itemMap);
                }
            }
            saleMap.put("items", itemList);
            salesList.add(saleMap);
        }
        yearlyData.put("sales", salesList);
        
        PdfExportUtil.exportReports(this, yearlyData, new HashMap<>(), yearlyTotal, yearlySales.size());
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
                    String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date()) + "T";
                    
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
