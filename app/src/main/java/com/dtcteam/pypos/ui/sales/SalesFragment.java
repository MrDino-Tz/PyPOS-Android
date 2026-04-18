package com.dtcteam.pypos.ui.sales;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.FragmentSalesBinding;
import com.dtcteam.pypos.model.Sale;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SalesFragment extends Fragment {

    private FragmentSalesBinding binding;
    private final ApiService api = ApiService.getInstance();
    private ArrayList<Sale> sales = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("sw", "TZ"));

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSalesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        binding.salesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.salesRecyclerView.setAdapter(new SalesAdapter(sales));
        
        binding.cardToday.setOnClickListener(v -> openSalesDetail("today"));
        binding.cardWeek.setOnClickListener(v -> openSalesDetail("week"));
        binding.cardMonth.setOnClickListener(v -> openSalesDetail("month"));
        binding.cardYear.setOnClickListener(v -> openSalesDetail("year"));
        
        loadSales();
        
        // Auto-refresh every 10 seconds for real-time feel
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadSales();
                handler.postDelayed(this, 10000);
            }
        };
        handler.postDelayed(refreshRunnable, 10000);
    }

    private void openSalesDetail(String period) {
        Intent intent = new Intent(requireContext(), SalesDetailActivity.class);
        intent.putExtra("period", period);
        startActivity(intent);
    }

    private void updateSummaryCards() {
        // Use local date (same as web) to match user's local day
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String todayKey = sdf.format(new java.util.Date()) + "T";
        
        sdf.applyPattern("yyyy-MM");
        String thisMonth = sdf.format(new java.util.Date());
        
        sdf.applyPattern("yyyy");
        String thisYear = sdf.format(new java.util.Date());
        
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
            
            if (createdAt.startsWith(todayKey)) {
                todayTotal += sale.getFinalAmount();
                todayCount++;
            }
            
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
        
        binding.tvTodayAmount.setText(currencyFormat.format(todayTotal));
        binding.tvTodayCount.setText(todayCount + " transactions");
        binding.tvWeekAmount.setText(currencyFormat.format(weekTotal));
        binding.tvWeekCount.setText(weekCount + " transactions");
        binding.tvMonthAmount.setText(currencyFormat.format(monthTotal));
        binding.tvMonthCount.setText(monthCount + " transactions");
        binding.tvYearAmount.setText(currencyFormat.format(yearTotal));
        binding.tvYearCount.setText(yearCount + " transactions");
    }

    private void loadSales() {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.getSales(new ApiService.Callback<List<Sale>>() {
            @Override
            public void onSuccess(List<Sale> result) {
                sales = new ArrayList<>(result);
                binding.salesRecyclerView.getAdapter().notifyDataSetChanged();
                updateSummaryCards();
                binding.loadingIndicator.setVisibility(View.GONE);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                binding.loadingIndicator.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}