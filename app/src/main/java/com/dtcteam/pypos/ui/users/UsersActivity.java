package com.dtcteam.pypos.ui.users;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivityUsersBinding;
import com.dtcteam.pypos.model.User;
import com.dtcteam.pypos.ui.common.SkeletonAdapter;
import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends AppCompatActivity {

    private ActivityUsersBinding binding;
    private final ApiService api = ApiService.getInstance();
    private ArrayList<User> users = new ArrayList<>();
    private UserAdapter adapter;
    private SkeletonAdapter skeletonAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        setupListeners();
        loadUsers();
    }

    private void setupRecyclerView() {
        skeletonAdapter = new SkeletonAdapter();
        skeletonAdapter.setLayoutResId(R.layout.item_category_skeleton);
        skeletonAdapter.setItemCount(5);
        
        adapter = new UserAdapter();
        binding.usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.usersRecyclerView.setAdapter(adapter);
    }

    private void showSkeleton(boolean show) {
        if (show) {
            binding.usersRecyclerView.setAdapter(skeletonAdapter);
        } else {
            binding.usersRecyclerView.setAdapter(adapter);
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void loadUsers() {
        showSkeleton(true);
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.getUsers(new ApiService.Callback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                binding.loadingIndicator.setVisibility(View.GONE);
                showSkeleton(false);
                users.clear();
                if (result != null) {
                    users.addAll(result);
                }
                adapter.setUsers(users);
            }

            @Override
            public void onError(String error) {
                binding.loadingIndicator.setVisibility(View.GONE);
                showSkeleton(false);
                Toast.makeText(UsersActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
