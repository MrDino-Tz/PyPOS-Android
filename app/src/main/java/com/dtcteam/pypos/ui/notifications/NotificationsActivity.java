package com.dtcteam.pypos.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivityNotificationsBinding;
import com.dtcteam.pypos.model.Item;
import com.dtcteam.pypos.model.Sale;
import com.dtcteam.pypos.ui.common.SkeletonAdapter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends AppCompatActivity {

    private ActivityNotificationsBinding binding;
    private final ApiService api = ApiService.getInstance();
    private ArrayList<NotificationItem> notifications = new ArrayList<>();
    private NotificationsAdapter adapter;
    private SkeletonAdapter skeletonAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        skeletonAdapter = new SkeletonAdapter();
        skeletonAdapter.setLayoutResId(R.layout.item_category_skeleton);
        skeletonAdapter.setItemCount(4);

        adapter = new NotificationsAdapter();
        binding.notificationsRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.notificationsRecycler.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());

        binding.swipeRefresh.setOnRefreshListener(() -> loadNotifications());
        
        loadNotifications();
    }

    private void showSkeleton(boolean show) {
        if (show) {
            binding.notificationsRecycler.setAdapter(skeletonAdapter);
        } else {
            binding.notificationsRecycler.setAdapter(adapter);
        }
    }

    private void loadNotifications() {
        binding.swipeRefresh.setRefreshing(true);
        showSkeleton(true);
        
        api.getItems(null, null, new ApiService.Callback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> result) {
                List<Item> items = result != null ? result : new ArrayList<>();
                
                api.getSales(new ApiService.Callback<List<Sale>>() {
                    @Override
                    public void onSuccess(List<Sale> salesResult) {
                        List<Sale> sales = salesResult != null ? salesResult : new ArrayList<>();
                        processNotifications(items, sales);
                    }

                    @Override
                    public void onError(String error) {
                        processNotifications(items, new ArrayList<>());
                    }
                });
            }

            @Override
            public void onError(String error) {
                binding.swipeRefresh.setRefreshing(false);
                showSkeleton(false);
            }
        });
    }

    private void processNotifications(List<Item> items, List<Sale> sales) {
        notifications.clear();
        
        // Low stock alerts
        for (Item item : items) {
            if (!item.isService() && item.getQuantity() <= item.getMinStockLevel() && item.getQuantity() > 0) {
                notifications.add(new NotificationItem("low-stock-" + item.getId(), "warning", 
                    "Low Stock: " + item.getName(), 
                    "Stock: " + item.getQuantity() + ", Min: " + item.getMinStockLevel()));
            }
        }
        
        // Out of stock alerts
        for (Item item : items) {
            if (!item.isService() && item.getQuantity() <= 0) {
                notifications.add(new NotificationItem("out-stock-" + item.getId(), "danger",
                    "Out of Stock: " + item.getName(),
                    "SKU: " + item.getSku()));
            }
        }
        
        // Large transactions
        int receiptNum = 1;
        for (Sale sale : sales) {
            if (sale.getFinalAmount() >= 50000 && receiptNum <= 5) {
                notifications.add(new NotificationItem("sale-" + sale.getId(), "success",
                    "Large Sale #" + String.format("%05d", sale.getId()),
                    "TSH " + NumberFormat.getCurrencyInstance(Locale.US).format(sale.getFinalAmount())));
                receiptNum++;
            }
        }
        
        // Today's summary
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
        int todayCount = 0;
        double todayTotal = 0;
        for (Sale sale : sales) {
            if (sale.getCreatedAt() != null && sale.getCreatedAt().startsWith(today)) {
                todayCount++;
                todayTotal += sale.getFinalAmount();
            }
        }
        if (todayCount > 0) {
            notifications.add(new NotificationItem("today-summary", "info",
                "Today's Sales",
                todayCount + " transactions, TSH " + NumberFormat.getCurrencyInstance(Locale.US).format(todayTotal)));
        }

        runOnUiThread(() -> {
            binding.swipeRefresh.setRefreshing(false);
            showSkeleton(false);
            binding.tvTitle.setText("Notifications (" + notifications.size() + ")");
            adapter.setNotifications(notifications);
        });
    }

    public static class NotificationItem {
        String id, type, title, message;
        public NotificationItem(String id, String type, String title, String message) {
            this.id = id; this.type = type; this.title = title; this.message = message;
        }
    }

    public static class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {
        private ArrayList<NotificationItem> items = new ArrayList<>();
        
        public void setNotifications(ArrayList<NotificationItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationItem item = items.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tvTitle, tvMessage, tvIcon;
            
            ViewHolder(View view) {
                super(view);
                tvTitle = view.findViewById(R.id.tvTitle);
                tvMessage = view.findViewById(R.id.tvMessage);
                tvIcon = view.findViewById(R.id.tvIcon);
            }
            
            void bind(NotificationItem item) {
                tvTitle.setText(item.title);
                tvMessage.setText(item.message);
                
                int color;
                switch (item.type) {
                    case "danger": color = 0xFFFB2C36; break;
                    case "warning": color = 0xFFF0B100; break;
                    case "success": color = 0xFF00C951; break;
                    default: color = 0xFF00B8DB;
                }
                tvIcon.setTextColor(color);
            }
        }
    }
}