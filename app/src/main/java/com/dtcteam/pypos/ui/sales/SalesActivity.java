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
        
        Calendar cal = Calendar.getInstance();
        
        // Today
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long todayStart = cal.getTimeInMillis();
        
        // This week (start of week - Monday)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long weekStart = cal.getTimeInMillis();
        
        // This month
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long monthStart = cal.getTimeInMillis();
        
        // This year
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long yearStart = cal.getTimeInMillis();
        
        double todayTotal = 0, weekTotal = 0, monthTotal = 0, yearTotal = 0;
        int todayCount = 0, weekCount = 0, monthCount = 0, yearCount = 0;
        
        for (Sale sale : sales) {
            long saleTime = 0;
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                if (sale.getCreatedAt() != null) {
                    java.util.Date date = sdf.parse(sale.getCreatedAt());
                    if (date != null) saleTime = date.getTime();
                }
            } catch (Exception e) {
                // Skip invalid dates
            }
            
            if (saleTime >= todayStart) {
                todayTotal += sale.getFinalAmount();
                todayCount++;
            }
            if (saleTime >= weekStart) {
                weekTotal += sale.getFinalAmount();
                weekCount++;
            }
            if (saleTime >= monthStart) {
                monthTotal += sale.getFinalAmount();
                monthCount++;
            }
            if (saleTime >= yearStart) {
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
