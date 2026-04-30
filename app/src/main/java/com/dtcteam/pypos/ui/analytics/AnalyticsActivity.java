package com.dtcteam.pypos.ui.analytics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivityAnalyticsBinding;
import com.dtcteam.pypos.model.Category;
import com.dtcteam.pypos.model.Item;
import com.dtcteam.pypos.model.Sale;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsActivity extends AppCompatActivity {

    private ActivityAnalyticsBinding binding;
    private final ApiService api = ApiService.getInstance();
    private final Locale usLocale = new Locale("en", "US");
    private NumberFormat nf = NumberFormat.getNumberInstance(usLocale);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAnalyticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupCharts();
        setupListeners();
        loadData();
    }

    private void setupCharts() {
        int primaryColor = Color.parseColor("#E66239");
        int successColor = Color.parseColor("#00C951");
        int infoColor = Color.parseColor("#00B8DB");
        int warningColor = Color.parseColor("#F0B100");
        int dangerColor = Color.parseColor("#FB2C36");

        // Revenue Line Chart
        LineChart revenueChart = binding.revenueChart;
        revenueChart.getDescription().setEnabled(false);
        revenueChart.setDrawGridBackground(false);
        revenueChart.getAxisRight().setEnabled(false);
        revenueChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        revenueChart.getLegend().setEnabled(false);
        revenueChart.setTouchEnabled(true);
        revenueChart.setDragEnabled(true);

        // Top Items Horizontal Bar Chart
        HorizontalBarChart topItemsChart = binding.topItemsChart;
        topItemsChart.getDescription().setEnabled(false);
        topItemsChart.setDrawGridBackground(false);
        topItemsChart.getAxisRight().setEnabled(false);
        topItemsChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        topItemsChart.getLegend().setEnabled(false);
        topItemsChart.setTouchEnabled(true);

        // Stock Pie Chart
        PieChart stockChart = binding.stockChart;
        stockChart.getDescription().setEnabled(false);
        stockChart.setDrawHoleEnabled(true);
        stockChart.setHoleRadius(50f);
        stockChart.setTransparentCircleRadius(55f);
        stockChart.setHoleColor(Color.WHITE);
        stockChart.setDrawEntryLabels(false);
        stockChart.setRotationEnabled(true);
        stockChart.setHighlightPerTapEnabled(true);
        stockChart.getLegend().setEnabled(true);
        stockChart.getLegend().setTextSize(10f);

        // Category Bar Chart
        BarChart categoryChart = binding.categoryChart;
        categoryChart.getDescription().setEnabled(false);
        categoryChart.setDrawGridBackground(false);
        categoryChart.getAxisRight().setEnabled(false);
        categoryChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        categoryChart.getLegend().setEnabled(false);
        categoryChart.setTouchEnabled(true);
        categoryChart.setFitBars(true);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRefresh.setOnClickListener(v -> loadData());
        binding.swipeRefresh.setOnRefreshListener(this::loadData);
    }

    private void loadData() {
        binding.swipeRefresh.setRefreshing(true);
        binding.loadingIndicator.setVisibility(View.VISIBLE);

        api.getItems(null, null, new ApiService.Callback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> items) {
                api.getCategories(new ApiService.Callback<List<Category>>() {
                    @Override
                    public void onSuccess(List<Category> categories) {
                        api.getSales(new ApiService.Callback<List<Sale>>() {
                            @Override
                            public void onSuccess(List<Sale> sales) {
                                runOnUiThread(() -> {
                                    binding.swipeRefresh.setRefreshing(false);
                                    binding.loadingIndicator.setVisibility(View.GONE);
                                    processData(items, categories, sales);
                                });
                            }

                            @Override
                            public void onError(String error) {
                                runOnUiThread(() -> {
                                    binding.swipeRefresh.setRefreshing(false);
                                    binding.loadingIndicator.setVisibility(View.GONE);
                                    processData(items, categories, new ArrayList<>());
                                });
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            binding.swipeRefresh.setRefreshing(false);
                            binding.loadingIndicator.setVisibility(View.GONE);
                            processData(items, new ArrayList<>(), new ArrayList<>());
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.swipeRefresh.setRefreshing(false);
                    binding.loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(AnalyticsActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void processData(List<Item> items, List<Category> categories, List<Sale> sales) {
        // Process Profit Table
        processProfitTable(sales);

        // Process Revenue Trend (Last 7 days)
        processRevenueChart(sales);

        // Process Top Items
        processTopItemsChart(sales);

        // Process Stock Status
        processStockChart(items);

        // Process Category Data
        processCategoryChart(items, categories);
    }

    private void processProfitTable(List<Sale> sales) {
        TableLayout table = binding.profitTable;
        table.removeAllViews();

        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.parseColor("#F8F9FA"));
        headerRow.setPadding(0, 16, 0, 16);
        
        String[] headers = {"Product/Service", "Qty", "Revenue", "Cost", "Profit", "Margin"};
        for (String h : headers) {
            TextView tv = new TextView(this);
            tv.setText(h);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setPadding(16, 8, 16, 8);
            tv.setTextColor(Color.parseColor("#495057"));
            headerRow.addView(tv);
        }
        table.addView(headerRow);

        Map<String, double[]> itemData = new HashMap<>();
        for (Sale sale : sales) {
            if (sale.getSaleItems() != null) {
                for (com.dtcteam.pypos.model.SaleItem si : sale.getSaleItems()) {
                    String name = si.getItemName() != null ? si.getItemName() : "Item " + si.getItemId();
                    double[] data = itemData.getOrDefault(name, new double[]{0, 0, 0});
                    data[0] += si.getQuantity();
                    data[1] += si.getQuantity() * si.getUnitPrice();
                    data[2] += si.getQuantity() * si.getCostPrice();
                    itemData.put(name, data);
                }
            }
        }

        List<Map.Entry<String, double[]>> sorted = new ArrayList<>(itemData.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue()[1] - b.getValue()[2], a.getValue()[1] - a.getValue()[2]));

        if (sorted.isEmpty()) {
            TableRow emptyRow = new TableRow(this);
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No profitability data available");
            emptyTv.setPadding(16, 32, 16, 32);
            emptyTv.setGravity(Gravity.CENTER);
            TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
            params.span = 6;
            emptyTv.setLayoutParams(params);
            emptyRow.addView(emptyTv);
            table.addView(emptyRow);
            return;
        }

        for (Map.Entry<String, double[]> entry : sorted) {
            TableRow row = new TableRow(this);
            row.setPadding(0, 16, 0, 16);
            
            double qty = entry.getValue()[0];
            double revenue = entry.getValue()[1];
            double cost = entry.getValue()[2];
            double profit = revenue - cost;
            double margin = revenue > 0 ? (profit / revenue) * 100 : 0;

            TextView tvName = new TextView(this);
            tvName.setText(entry.getKey());
            tvName.setPadding(16, 8, 16, 8);
            row.addView(tvName);

            TextView tvQty = new TextView(this);
            tvQty.setText(String.valueOf((int)qty));
            tvQty.setPadding(16, 8, 16, 8);
            row.addView(tvQty);

            TextView tvRev = new TextView(this);
            tvRev.setText(nf.format(revenue));
            tvRev.setTextColor(Color.parseColor("#E66239"));
            tvRev.setPadding(16, 8, 16, 8);
            row.addView(tvRev);

            TextView tvCost = new TextView(this);
            tvCost.setText(nf.format(cost));
            tvCost.setTextColor(Color.parseColor("#FB2C36"));
            tvCost.setPadding(16, 8, 16, 8);
            row.addView(tvCost);

            TextView tvProfit = new TextView(this);
            tvProfit.setText(nf.format(profit));
            tvProfit.setTextColor(Color.parseColor("#00C951"));
            tvProfit.setTypeface(null, Typeface.BOLD);
            tvProfit.setPadding(16, 8, 16, 8);
            row.addView(tvProfit);

            TextView tvMargin = new TextView(this);
            tvMargin.setText(String.format(usLocale, "%.1f%%", margin));
            tvMargin.setPadding(24, 8, 24, 8);
            tvMargin.setTextColor(Color.WHITE);
            tvMargin.setTextSize(12f);
            tvMargin.setGravity(Gravity.CENTER);

            GradientDrawable badge = new GradientDrawable();
            badge.setCornerRadius(16f);
            if (margin >= 50) badge.setColor(Color.parseColor("#00C951"));
            else if (margin >= 20) badge.setColor(Color.parseColor("#F0B100"));
            else badge.setColor(Color.parseColor("#FB2C36"));
            tvMargin.setBackground(badge);

            TableRow.LayoutParams marginParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
            marginParams.setMargins(16, 8, 16, 8);
            tvMargin.setLayoutParams(marginParams);

            row.addView(tvMargin);
            table.addView(row);

            View divider = new View(this);
            divider.setBackgroundColor(Color.parseColor("#E5E5E5"));
            table.addView(divider, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 1));
        }
    }

    private void processRevenueChart(List<Sale> sales) {
        List<Entry> revEntries = new ArrayList<>();
        List<Entry> profitEntries = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        Map<String, Float> dailyRevenue = new java.util.LinkedHashMap<>();
        Map<String, Float> dailyProfit = new java.util.LinkedHashMap<>();

        // Get last 7 days
        for (int i = 6; i >= 0; i--) {
            cal.setTime(new java.util.Date());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            String dateKey = String.format("%02d/%02d", cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
            dailyRevenue.put(dateKey, 0f);
            dailyProfit.put(dateKey, 0f);
        }

        // Sum sales by day
        for (Sale sale : sales) {
            if (sale.getCreatedAt() != null) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", usLocale);
                    java.util.Date date = sdf.parse(sale.getCreatedAt());
                    if (date != null) {
                        cal.setTime(date);
                        String dateKey = String.format("%02d/%02d", cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
                        if (dailyRevenue.containsKey(dateKey)) {
                            dailyRevenue.put(dateKey, dailyRevenue.get(dateKey) + (float) sale.getFinalAmount());
                            float saleProfit = 0;
                            if (sale.getSaleItems() != null) {
                                for (com.dtcteam.pypos.model.SaleItem si : sale.getSaleItems()) {
                                    saleProfit += (si.getUnitPrice() - si.getCostPrice()) * si.getQuantity();
                                }
                            }
                            dailyProfit.put(dateKey, dailyProfit.get(dateKey) + saleProfit);
                        }
                    }
                } catch (Exception e) {}
            }
        }

        int index = 0;
        for (Map.Entry<String, Float> entry : dailyRevenue.entrySet()) {
            revEntries.add(new Entry(index, entry.getValue()));
            profitEntries.add(new Entry(index, dailyProfit.get(entry.getKey())));
            index++;
        }

        LineDataSet revDataSet = new LineDataSet(revEntries, "Revenue");
        revDataSet.setColor(Color.parseColor("#E66239"));
        revDataSet.setCircleColor(Color.parseColor("#E66239"));
        revDataSet.setLineWidth(2f);
        revDataSet.setCircleRadius(4f);
        revDataSet.setDrawFilled(true);
        revDataSet.setFillColor(Color.parseColor("#33E66239"));
        revDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        revDataSet.setDrawValues(false);

        LineDataSet profitDataSet = new LineDataSet(profitEntries, "Profit");
        profitDataSet.setColor(Color.parseColor("#00C951"));
        profitDataSet.setCircleColor(Color.parseColor("#00C951"));
        profitDataSet.setLineWidth(2f);
        profitDataSet.setCircleRadius(4f);
        profitDataSet.setDrawFilled(true);
        profitDataSet.setFillColor(Color.parseColor("#3300C951"));
        profitDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        profitDataSet.setDrawValues(false);

        LineData lineData = new LineData(revDataSet, profitDataSet);
        binding.revenueChart.setData(lineData);
        binding.revenueChart.getLegend().setEnabled(true);
        
        String[] labels = dailyRevenue.keySet().toArray(new String[0]);
        binding.revenueChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.revenueChart.getXAxis().setGranularity(1f);
        binding.revenueChart.invalidate();
    }

    private void processTopItemsChart(List<Sale> sales) {
        Map<String, Float> itemProfits = new HashMap<>();
        
        for (Sale sale : sales) {
            if (sale.getSaleItems() != null) {
                for (com.dtcteam.pypos.model.SaleItem saleItem : sale.getSaleItems()) {
                    String name = saleItem.getItemName() != null ? saleItem.getItemName() : "Item " + saleItem.getItemId();
                    float profit = (float) ((saleItem.getUnitPrice() - saleItem.getCostPrice()) * saleItem.getQuantity());
                    itemProfits.put(name, itemProfits.getOrDefault(name, 0f) + profit);
                }
            }
        }

        List<Map.Entry<String, Float>> sorted = new ArrayList<>(itemProfits.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        sorted = sorted.subList(0, Math.min(5, sorted.size()));

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        for (int i = 0; i < sorted.size(); i++) {
            entries.add(new BarEntry(i, sorted.get(i).getValue()));
            String name = sorted.get(i).getKey();
            labels.add(name.length() > 10 ? name.substring(0, 10) + ".." : name);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Profit");
        dataSet.setColor(Color.parseColor("#00C951"));
        dataSet.setValueTextSize(10f);
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        
        binding.topItemsChart.setData(barData);
        binding.topItemsChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.topItemsChart.getXAxis().setGranularity(1f);
        binding.topItemsChart.getXAxis().setLabelRotationAngle(0);
        binding.topItemsChart.invalidate();
    }

    private void processStockChart(List<Item> items) {
        int out = 0, low = 0, medium = 0, good = 0;
        
        for (Item item : items) {
            if (item.isService()) continue;
            int qty = item.getQuantity();
            int min = item.getMinStockLevel() > 0 ? item.getMinStockLevel() : 5;
            
            if (qty == 0) out++;
            else if (qty <= min) low++;
            else if (qty <= min * 2) medium++;
            else good++;
        }

        List<PieEntry> entries = new ArrayList<>();
        int[] colors = new int[]{Color.parseColor("#FB2C36"), Color.parseColor("#F0B100"), Color.parseColor("#00B8DB"), Color.parseColor("#00C951")};
        
        if (out > 0) entries.add(new PieEntry(out, "Out: " + out));
        if (low > 0) entries.add(new PieEntry(low, "Low: " + low));
        if (medium > 0) entries.add(new PieEntry(medium, "Med: " + medium));
        if (good > 0) entries.add(new PieEntry(good, "Good: " + good));

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "No Data"));
            colors = new int[]{Color.parseColor("#E5E5E5")};
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        
        PieData pieData = new PieData(dataSet);
        binding.stockChart.setData(pieData);
        binding.stockChart.invalidate();
    }

    private void processCategoryChart(List<Item> items, List<Category> categories) {
        Map<String, Float> categoryTotals = new HashMap<>();
        
        for (Item item : items) {
            String catName = item.getCategoryName() != null ? item.getCategoryName() : "Uncategorized";
            categoryTotals.put(catName, categoryTotals.getOrDefault(catName, 0f) + item.getQuantity());
        }

        // Sort and get top 6
        List<Map.Entry<String, Float>> sorted = new ArrayList<>(categoryTotals.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        sorted = sorted.subList(0, Math.min(6, sorted.size()));

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        for (int i = 0; i < sorted.size(); i++) {
            entries.add(new BarEntry(i, sorted.get(i).getValue()));
            labels.add(sorted.get(i).getKey());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Items");
        dataSet.setColor(Color.parseColor("#00C951"));
        dataSet.setValueTextSize(10f);
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        
        binding.categoryChart.setData(barData);
        binding.categoryChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.categoryChart.getXAxis().setGranularity(1f);
        binding.categoryChart.getXAxis().setLabelRotationAngle(-45);
        binding.categoryChart.getXAxis().setTextSize(9f);
        binding.categoryChart.setFitBars(true);
        binding.categoryChart.invalidate();
    }
}