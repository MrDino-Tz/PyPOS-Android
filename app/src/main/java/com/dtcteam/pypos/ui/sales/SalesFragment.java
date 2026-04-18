package com.dtcteam.pypos.ui.sales;

import android.content.Intent;
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
    }

    private void openSalesDetail(String period) {
        Intent intent = new Intent(requireContext(), SalesDetailActivity.class);
        intent.putExtra("period", period);
        startActivity(intent);
    }

    private void calculateStats(List<Sale> allSales) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date()) + "T";
        
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        cal.add(Calendar.DATE, -(dayOfWeek - 1));
        java.util.Date weekStart = cal.getTime();
        
        cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        java.util.Date monthStart = cal.getTime();
        
        cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        java.util.Date yearStart = cal.getTime();
        
        double todayTotal = 0;
        int todayCount = 0;
        double weekTotal = 0;
        int weekCount = 0;
        double monthTotal = 0;
        int monthCount = 0;
        double yearTotal = 0;
        int yearCount = 0;
        
        for (Sale sale : allSales) {
            if (sale.getCreatedAt() != null) {
                if (sale.getCreatedAt().startsWith(today)) {
                    todayTotal += sale.getFinalAmount();
                    todayCount++;
                }
                java.util.Date saleDate = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(sale.getCreatedAt());
                if (saleDate != null) {
                    if (saleDate.after(weekStart)) {
                        weekTotal += sale.getFinalAmount();
                        weekCount++;
                    }
                    if (saleDate.after(monthStart)) {
                        monthTotal += sale.getFinalAmount();
                        monthCount++;
                    }
                    if (saleDate.after(yearStart)) {
                        yearTotal += sale.getFinalAmount();
                        yearCount++;
                    }
                }
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
                calculateStats(sales);
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