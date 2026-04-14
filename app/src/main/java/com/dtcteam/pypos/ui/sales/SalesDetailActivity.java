package com.dtcteam.pypos.ui.sales;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivitySalesDetailBinding;
import com.dtcteam.pypos.model.Sale;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SalesDetailActivity extends AppCompatActivity {

    private ActivitySalesDetailBinding binding;
    private final ApiService api = ApiService.getInstance();
    private int saleId;
    private Sale sale;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySalesDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        saleId = getIntent().getIntExtra("sale_id", 0);
        if (saleId == 0) {
            finish();
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnPrint.setOnClickListener(v -> printReceipt());
        binding.itemsSection.setVisibility(View.GONE);
        
        loadSaleDetail();
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
        binding.tvTotal.setText("TSH " + currencyFormat.format(sale.getFinalAmount()));
        
        // Calculate payments from sale totals
        double paid = sale.getFinalAmount();
        binding.tvPaid.setText("TSH " + currencyFormat.format(paid));
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