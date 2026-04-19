package com.dtcteam.pypos.ui.dashboard;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.FragmentDashboardBinding;
import com.dtcteam.pypos.model.DashboardStats;
import com.dtcteam.pypos.model.Item;
import com.dtcteam.pypos.model.Sale;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private final ApiService api = ApiService.getInstance();
    private RecentSalesAdapter salesAdapter;
    private LowStockAdapter lowStockAdapter;
    private ArrayList<Sale> recentSales = new ArrayList<>();
    private ArrayList<Item> lowStockItems = new ArrayList<>();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable poller;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerViews();
        setupClickListeners();
        loadDashboard();
        startPolling();
    }

    private void startPolling() {
        poller = new Runnable() {
            @Override
            public void run() {
                loadDashboard();
                handler.postDelayed(this, 10000);
            }
        };
        handler.post(poller);
    }

    private void setupRecyclerViews() {
        salesAdapter = new RecentSalesAdapter();
        binding.recentSalesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recentSalesRecycler.setAdapter(salesAdapter);

        lowStockAdapter = new LowStockAdapter();
        binding.lowStockRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.lowStockRecycler.setAdapter(lowStockAdapter);
    }

    private void setupClickListeners() {
        binding.btnRefresh.setOnClickListener(v -> loadDashboard());
        
        binding.btnNewSale.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Go to POS", Toast.LENGTH_SHORT).show();
        });

        binding.btnAddStock.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Go to Stock", Toast.LENGTH_SHORT).show();
        });

        binding.btnReports.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Go to Reports", Toast.LENGTH_SHORT).show();
        });

        binding.tvViewAllSales.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "View All Sales", Toast.LENGTH_SHORT).show();
        });

        binding.tvViewAllLowStock.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "View All Low Stock", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadDashboard() {
        if (binding == null) return;
        
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.getDashboardStats(new ApiService.Callback<DashboardStats>() {
            @Override
            public void onSuccess(DashboardStats stats) {
                if (binding == null) return;
                
                NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
                
                binding.tvTodaySales.setText("TSH " + nf.format((long) stats.getTodaySales()));
                binding.tvTransactions.setText(String.valueOf(stats.getTodayTransactions()));
                binding.tvTotalItems.setText(String.valueOf(stats.getTotalItems()));
                binding.tvLowStock.setText(String.valueOf(stats.getLowStockItems()));
                
                binding.loadingIndicator.setVisibility(View.GONE);
            }

            @Override
            public void onError(String error) {
                if (binding == null) return;
                binding.loadingIndicator.setVisibility(View.GONE);
            }
        });

        api.getSales(new ApiService.Callback<List<Sale>>() {
            @Override
            public void onSuccess(List<Sale> result) {
                if (binding == null) return;
                recentSales.clear();
                if (result != null && !result.isEmpty()) {
                    int count = Math.min(5, result.size());
                    for (int i = 0; i < count; i++) {
                        recentSales.add(result.get(i));
                    }
                }
                salesAdapter.setSales(recentSales);
            }

            @Override
            public void onError(String error) {}
        });

        api.getItems(null, null, new ApiService.Callback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> result) {
                if (binding == null) return;
                lowStockItems.clear();
                if (result != null) {
                    for (Item item : result) {
                        if (!item.isService() && item.getQuantity() <= item.getMinStockLevel()) {
                            lowStockItems.add(item);
                            if (lowStockItems.size() >= 5) break;
                        }
                    }
                }
                lowStockAdapter.setItems(lowStockItems);
            }

            @Override
            public void onError(String error) {}
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (poller != null) handler.removeCallbacks(poller);
        binding = null;
    }
}