package com.dtcteam.pypos.ui.sales;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SalesActivity extends AppCompatActivity {

    private ActivitySalesBinding binding;
    private final ApiService api = ApiService.getInstance();
    private ArrayList<Sale> sales = new ArrayList<>();
    private SalesAdapter adapter;
    private SkeletonAdapter skeletonAdapter;
    private Locale usLocale = new Locale("en", "US");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySalesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        setupListeners();
        loadSales();
        
        // Auto-refresh every 10 seconds for real-time like feel
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadSales(false);
                handler.postDelayed(this, 10000);
            }
        };
        handler.postDelayed(refreshRunnable, 10000);
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
        binding.btnRefresh.setOnClickListener(v -> loadSales());
        
        binding.cardToday.setOnClickListener(v -> openSalesDetail("today"));
        binding.cardWeek.setOnClickListener(v -> openSalesDetail("week"));
        binding.cardMonth.setOnClickListener(v -> openSalesDetail("month"));
        binding.cardYear.setOnClickListener(v -> openSalesDetail("year"));
    }

    private void openSalesDetail(String period) {
        Intent intent = new Intent(this, SalesDetailActivity.class);
        intent.putExtra("period", period);
        startActivity(intent);
    }

    private void loadSales() {
        loadSales(false);
    }
    
    private void loadSales(boolean showSkeleton) {
        if (showSkeleton) showSkeleton(true);
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
                updateSummaryCards();
            }

            @Override
            public void onError(String error) {
                binding.loadingIndicator.setVisibility(View.GONE);
                showSkeleton(false);
                Toast.makeText(SalesActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSummaryCards() {
        NumberFormat nf = NumberFormat.getNumberInstance(usLocale);
        
        // Use local date (same as web) to match user's local day
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String todayKey = sdf.format(new java.util.Date()) + "T";
        
        sdf.applyPattern("yyyy-MM");
        String thisMonth = sdf.format(new java.util.Date());
        
        sdf.applyPattern("yyyy");
        String thisYear = sdf.format(new java.util.Date());
        
        // Also use timestamps for week calculation
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long weekStart = cal.getTimeInMillis();
        
        double todayTotal = 0, weekTotal = 0, monthTotal = 0, yearTotal = 0;
        int todayCount = 0, weekCount = 0, monthCount = 0, yearCount = 0;
        
        for (Sale sale : sales) {
            String createdAt = sale.getCreatedAt();
            if (createdAt == null) continue;
            
            // Today: starts with today's date (same as web)
            if (createdAt.startsWith(todayKey)) {
                todayTotal += sale.getFinalAmount();
                todayCount++;
            }
            
            // Week/Month/Year: use timestamp comparison
            long saleTime = 0;
            try {
                java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                java.util.Date date = sdf2.parse(createdAt);
                if (date != null) saleTime = date.getTime();
            } catch (Exception e) {
                // skip
            }
            
            if (saleTime >= weekStart) {
                weekTotal += sale.getFinalAmount();
                weekCount++;
            }
            if (createdAt.startsWith(thisMonth)) {
                monthTotal += sale.getFinalAmount();
                monthCount++;
            }
            if (createdAt.startsWith(thisYear)) {
                yearTotal += sale.getFinalAmount();
                yearCount++;
            }
        }
        
        binding.tvTodayAmount.setText("TSH " + nf.format((long) todayTotal));
        binding.tvTodayCount.setText(todayCount + " transactions");
        
        binding.tvWeekAmount.setText("TSH " + nf.format((long) weekTotal));
        binding.tvWeekCount.setText(weekCount + " transactions");
        
        binding.tvMonthAmount.setText("TSH " + nf.format((long) monthTotal));
        binding.tvMonthCount.setText(monthCount + " transactions");
        
        binding.tvYearAmount.setText("TSH " + nf.format((long) yearTotal));
        binding.tvYearCount.setText(yearCount + " transactions");
    }
}
