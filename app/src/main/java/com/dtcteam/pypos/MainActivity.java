package com.dtcteam.pypos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivityMainBinding;
import com.dtcteam.pypos.model.Item;
import com.dtcteam.pypos.model.Sale;
import com.dtcteam.pypos.model.User;
import com.dtcteam.pypos.ui.account.AccountFragment;
import com.dtcteam.pypos.ui.categories.CategoriesFragment;
import com.dtcteam.pypos.ui.dashboard.DashboardFragment;
import com.dtcteam.pypos.ui.items.ItemsActivity;
import com.dtcteam.pypos.ui.categories.CategoriesActivity;
import com.dtcteam.pypos.ui.sales.SalesActivity;
import com.dtcteam.pypos.ui.stock.StockActivity;
import com.dtcteam.pypos.ui.reports.ReportsActivity;
import com.dtcteam.pypos.ui.users.UsersActivity;
import com.dtcteam.pypos.ui.pos.PosFragment;
import com.dtcteam.pypos.ui.settings.ServicesFragment;
import com.dtcteam.pypos.ui.settings.SettingsFragment;
import com.dtcteam.pypos.ui.login.LoginActivity;
import com.dtcteam.pypos.ui.notifications.NotificationsActivity;
import com.google.android.material.navigation.NavigationView;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;
    private final ApiService api = ApiService.getInstance();
    private SharedPreferences prefs;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable poller;
    private TextView badgeCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = getSharedPreferences("pypos_prefs", MODE_PRIVATE);
        
        badgeCount = binding.badgeCount;
        startPolling();
        checkAuth();
        setupNavigation();
        setupDrawer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBadge();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (poller != null) handler.removeCallbacks(poller);
}
    
    private void startPolling() {
        poller = new Runnable() {
            @Override
            public void run() {
                updateBadge();
                handler.postDelayed(this, 10000);
            }
        };
        handler.post(poller);
    }

    private void updateBadge() {
        api.getItems(null, null, new ApiService.Callback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> items) {
                api.getSales(new ApiService.Callback<List<Sale>>() {
                    @Override
                    public void onSuccess(List<Sale> sales) {
                        int unread = calculateUnread(items, sales);
                        runOnUiThread(() -> {
                            if (unread > 0) {
                                badgeCount.setText(String.valueOf(unread > 9 ? "9+" : unread));
                                badgeCount.setVisibility(View.VISIBLE);
                            } else {
                                badgeCount.setVisibility(View.GONE);
                            }
                        });
                    }
                    @Override public void onError(String error) {}
                });
            }
            @Override public void onError(String error) {}
        });
    }

    private int calculateUnread(List<Item> items, List<Sale> sales) {
        Set<String> readIds = prefs.getStringSet("read_notifications", new HashSet<>());
        int count = 0;
        if (items != null) {
            for (Item item : items) {
                String id = "low-stock-" + item.getId();
                if (!readIds.contains(id) && item.getQuantity() <= item.getMinStockLevel() && item.getQuantity() > 0) count++;
                id = "out-stock-" + item.getId();
                if (!readIds.contains(id) && item.getQuantity() <= 0) count++;
            }
        }
        return count;
    }

    private void markAllRead() {
        api.getItems(null, null, new ApiService.Callback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> items) {
                Set<String> readIds = new HashSet<>();
                if (items != null) {
                    for (Item item : items) {
                        if (item.getQuantity() <= item.getMinStockLevel()) {
                            readIds.add("low-stock-" + item.getId());
                        }
                        if (item.getQuantity() <= 0) {
                            readIds.add("out-stock-" + item.getId());
                        }
                    }
                }
                prefs.edit().putStringSet("read_notifications", readIds).apply();
                runOnUiThread(() -> badgeCount.setVisibility(View.GONE));
            }
            @Override public void onError(String error) {}
        });
    }

    private void checkAuth() {
        api.getCurrentUser(new ApiService.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                updateUserEmail(result.getEmail(), result.getRole());
                setupDrawerMenu(result.isAdmin());
            }

            @Override
            public void onError(String error) {
                navigateToLogin();
            }
        });
    }

    private void updateUserEmail(String email, String role) {
        NavigationView navView = findViewById(R.id.navigationView);
        if (navView != null) {
            View headerView = navView.getHeaderView(0);
            if (headerView != null) {
                TextView tvEmail = headerView.findViewById(R.id.tvUserEmail);
                if (tvEmail != null) {
                    tvEmail.setText(email);
                }
                TextView tvRole = headerView.findViewById(R.id.tvUserRole);
                if (tvRole != null) {
                    tvRole.setText(role != null ? role.toUpperCase() : "STAFF");
                }
            }
        }
    }
    
    private void setupDrawerMenu(boolean isAdmin) {
        NavigationView navView = findViewById(R.id.navigationView);
        if (navView == null) return;
        
        Menu menu = navView.getMenu();
        
        // Hide admin-only items for non-admin users
        menu.findItem(R.id.nav_items).setVisible(isAdmin);
        menu.findItem(R.id.nav_categories).setVisible(isAdmin);
        menu.findItem(R.id.nav_stock).setVisible(isAdmin);
        menu.findItem(R.id.nav_reports).setVisible(isAdmin);
        menu.findItem(R.id.nav_users).setVisible(isAdmin);
    }

    private void setupDrawer() {
        binding.btnMenu.setOnClickListener(v -> {
            binding.drawerLayout.openDrawer(GravityCompat.START);
        });

        binding.btnNotifications.setOnClickListener(v -> {
            markAllRead();
            Intent intent = new Intent(this, NotificationsActivity.class);
            startActivity(intent);
        });

        binding.navigationView.setNavigationItemSelectedListener(this);
    }

    private void navigateToFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        Fragment fragment = null;

        if (itemId == R.id.nav_dashboard) {
            fragment = new DashboardFragment();
        } else if (itemId == R.id.nav_pos) {
            fragment = new PosFragment();
        } else if (itemId == R.id.nav_services) {
            fragment = new ServicesFragment();
        } else if (itemId == R.id.nav_items) {
            Intent intent = new Intent(this, ItemsActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_categories) {
            Intent intent = new Intent(this, CategoriesActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_stock) {
            Intent intent = new Intent(this, StockActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_reports) {
            Intent intent = new Intent(this, ReportsActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_analytics) {
            Intent intent = new Intent(this, com.dtcteam.pypos.ui.analytics.AnalyticsActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_users) {
            Intent intent = new Intent(this, UsersActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_account) {
            fragment = new AccountFragment();
        } else if (itemId == R.id.nav_logout) {
            logout();
            return true;
        }

        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                fragment = new DashboardFragment();
            } else if (itemId == R.id.nav_pos) {
                fragment = new PosFragment();
            } else if (itemId == R.id.nav_services) {
                fragment = new ServicesFragment();
        } else if (itemId == R.id.nav_sales) {
            Intent intent = new Intent(this, SalesActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.nav_settings) {
                fragment = new SettingsFragment();
            }
            
            if (fragment != null) {
                getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
                return true;
            }
            return false;
        });

        binding.bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
    }

    private void logout() {
        api.logout(new ApiService.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                navigateToLogin();
            }

            @Override
            public void onError(String error) {
                navigateToLogin();
            }
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
