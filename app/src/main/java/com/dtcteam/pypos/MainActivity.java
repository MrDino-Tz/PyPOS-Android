package com.dtcteam.pypos;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivityMainBinding;
import com.dtcteam.pypos.model.User;
import com.dtcteam.pypos.ui.account.AccountFragment;
import com.dtcteam.pypos.ui.categories.CategoriesFragment;
import com.dtcteam.pypos.ui.dashboard.DashboardFragment;
import com.dtcteam.pypos.ui.items.ItemsFragment;
import com.dtcteam.pypos.ui.login.LoginActivity;
import com.dtcteam.pypos.ui.pos.PosFragment;
import com.dtcteam.pypos.ui.reports.ReportsFragment;
import com.dtcteam.pypos.ui.sales.SalesFragment;
import com.dtcteam.pypos.ui.settings.ServicesFragment;
import com.dtcteam.pypos.ui.settings.SettingsFragment;
import com.dtcteam.pypos.ui.stock.StockFragment;
import com.dtcteam.pypos.ui.users.UsersFragment;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;
    private final ApiService api = ApiService.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        checkAuth();
        setupNavigation();
        setupDrawer();
    }

    private void checkAuth() {
        api.getCurrentUser(new ApiService.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                updateUserEmail(result.getEmail());
            }

            @Override
            public void onError(String error) {
                navigateToLogin();
            }
        });
    }

    private void updateUserEmail(String email) {
        NavigationView navView = findViewById(R.id.navigationView);
        if (navView != null) {
            View headerView = navView.getHeaderView(0);
            if (headerView != null) {
                TextView tvEmail = headerView.findViewById(R.id.tvUserEmail);
                if (tvEmail != null) {
                    tvEmail.setText(email);
                }
            }
        }
    }

    private void setupDrawer() {
        binding.btnMenu.setOnClickListener(v -> {
            binding.drawerLayout.openDrawer(GravityCompat.START);
        });

        binding.btnNotifications.setOnClickListener(v -> {
            // TODO: Show notifications
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
            fragment = new ItemsFragment();
        } else if (itemId == R.id.nav_categories) {
            fragment = new CategoriesFragment();
        } else if (itemId == R.id.nav_stock) {
            fragment = new StockFragment();
        } else if (itemId == R.id.nav_sales) {
            fragment = new SalesFragment();
        } else if (itemId == R.id.nav_reports) {
            fragment = new ReportsFragment();
        } else if (itemId == R.id.nav_users) {
            fragment = new UsersFragment();
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
                fragment = new SalesFragment();
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
