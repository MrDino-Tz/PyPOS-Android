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
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("sw", "TZ"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
        loadSales();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        
        binding.btnExportAll.setOnClickListener(v -> {
            exportAllReports();
        });
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

    private void loadSales() {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.getSales(new ApiService.Callback<List<Sale>>() {
            @Override
            public void onSuccess(List<Sale> result) {
                binding.loadingIndicator.setVisibility(View.GONE);
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
