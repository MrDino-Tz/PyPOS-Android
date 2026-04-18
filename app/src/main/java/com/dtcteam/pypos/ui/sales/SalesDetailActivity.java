package com.dtcteam.pypos.ui.sales;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivitySalesDetailBinding;
import com.dtcteam.pypos.model.Sale;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SalesDetailActivity extends AppCompatActivity {

    private ActivitySalesDetailBinding binding;
    private final ApiService api = ApiService.getInstance();
    private int saleId;
    private String period;
    private Sale sale;
    private ArrayList<Sale> salesList = new ArrayList<>();
    private SalesAdapter adapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("sw", "TZ"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySalesDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        saleId = getIntent().getIntExtra("sale_id", 0);
        period = getIntent().getStringExtra("period");

        if (saleId == 0 && period == null) {
            finish();
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());
        
        if (period != null) {
            setupPeriodView();
        } else {
            setupSaleDetailView();
        }
    }

    private void setupPeriodView() {
        binding.itemsSection.setVisibility(View.GONE);
        binding.statsSection.setVisibility(View.VISIBLE);
        binding.statsHeader.setVisibility(View.VISIBLE);
        binding.salesListRecycler.setVisibility(View.VISIBLE);
        
        String title;
        switch (period) {
            case "today": title = "Today's Sales"; break;
            case "week": title = "This Week's Sales"; break;
            case "month": title = "This Month's Sales"; break;
            case "year": title = "This Year's Sales"; break;
            default: title = "All Sales"; break;
        }
        binding.tvReceiptNumber.setText(title);
        binding.tvDate.setVisibility(View.GONE);

        adapter = new SalesAdapter(salesList);
        binding.salesListRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.salesListRecycler.setAdapter(adapter);

        loadSalesByPeriod();
    }

    private void setupSaleDetailView() {
        binding.itemsSection.setVisibility(View.GONE);
        binding.statsSection.setVisibility(View.GONE);
        binding.statsHeader.setVisibility(View.GONE);
        binding.salesListRecycler.setVisibility(View.GONE);
        
        binding.btnPrint.setOnClickListener(v -> printReceipt());
        
        loadSaleDetail();
    }

    private void loadSalesByPeriod() {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.getSales(new ApiService.Callback<List<Sale>>() {
            @Override
            public void onSuccess(List<Sale> result) {
                runOnUiThread(() -> {
                    binding.loadingIndicator.setVisibility(View.GONE);
                    
                    if (result != null) {
                        List<Sale> filtered = filterSalesByPeriod(result);
                        salesList.clear();
                        salesList.addAll(filtered);
                        adapter.notifyDataSetChanged();
                        
                        double total = 0;
                        for (Sale s : filtered) {
                            total += s.getFinalAmount();
                        }
                        binding.tvTotal.setText(currencyFormat.format(total));
                        binding.tvDate.setText(filtered.size() + " transactions");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(SalesDetailActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private List<Sale> filterSalesByPeriod(List<Sale> allSales) {
        List<Sale> filtered = new ArrayList<>();
        String todayPrefix = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()) + "T";
        
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date weekStart = cal.getTime();
        cal.add(Calendar.DATE, -cal.get(Calendar.DAY_OF_WEEK) + 7);
        
        cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date monthStart = cal.getTime();
        
        cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date yearStart = cal.getTime();

        for (Sale sale : allSales) {
            if (sale.getCreatedAt() == null) continue;
            
            try {
                Date saleDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(sale.getCreatedAt());
                if (saleDate == null) continue;
                
                boolean matches = false;
                switch (period) {
                    case "today":
                        matches = sale.getCreatedAt().startsWith(todayPrefix);
                        break;
                    case "week":
                        matches = saleDate.after(weekStart);
                        break;
                    case "month":
                        matches = saleDate.after(monthStart);
                        break;
                    case "year":
                        matches = saleDate.after(yearStart);
                        break;
                    default:
                        matches = true;
                }
                if (matches) filtered.add(sale);
            } catch (Exception e) {
                // skip invalid dates
            }
        }
        return filtered;
    }

    private void loadSaleDetail() {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.getSales(new ApiService.Callback<List<Sale>>() {
            @Override
            public void onSuccess(List<Sale> result) {
                if (result != null) {
                    for (Sale s : result) {
                        if (s.getId() == saleId) {
                            sale = s;
                            break;
                        }
                    }
                }
                runOnUiThread(() -> {
                    binding.loadingIndicator.setVisibility(View.GONE);
                    displaySale();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(SalesDetailActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void displaySale() {
        if (sale == null) {
            Toast.makeText(this, "Sale not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.tvReceiptNumber.setText("Receipt #" + String.format("%05d", sale.getId()));
        binding.tvDate.setText(sale.getCreatedAt() != null ? sale.getCreatedAt() : "N/A");
        binding.tvTotalAmount.setText("TSH " + currencyFormat.format(sale.getFinalAmount()));
        
        binding.tvPaid.setText("TSH " + currencyFormat.format(sale.getFinalAmount()));
        binding.tvChange.setText("TSH 0");
        binding.tvPaymentMethod.setText("Cash");
        binding.tvCashier.setText("System");
        binding.tvSubtotal.setText("TSH " + currencyFormat.format(sale.getFinalAmount()));
        binding.tvTax.setText("TSH 0");
        binding.tvDiscount.setText("TSH 0");
    }

    private void printReceipt() {
        Toast.makeText(this, "Printing receipt #" + saleId, Toast.LENGTH_SHORT).show();
    }
}