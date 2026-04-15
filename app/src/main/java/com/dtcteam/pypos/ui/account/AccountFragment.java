package com.dtcteam.pypos.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.api.SupabaseClient;
import com.dtcteam.pypos.databinding.FragmentAccountBinding;
import com.dtcteam.pypos.model.User;
import com.dtcteam.pypos.ui.login.LoginActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private final ApiService api = ApiService.getInstance();
    private final SupabaseClient supabase = SupabaseClient.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        
        binding.btnEditProfile.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Edit profile coming soon", Toast.LENGTH_SHORT).show();
        });
        
        binding.btnSettings.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Settings coming soon", Toast.LENGTH_SHORT).show();
        });
        
        binding.fabChangePhoto.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Change photo coming soon", Toast.LENGTH_SHORT).show();
        });
        
        loadUserInfo();
    }

    private void loadUserInfo() {
        String userEmail = supabase.getUserEmail();
        String userRole = supabase.getUserRole();
        
        if (userEmail != null) {
            binding.tvUserEmail.setText(userEmail);
            String username = userEmail.split("@")[0];
            binding.tvUserName.setText(username.substring(0, 1).toUpperCase() + username.substring(1));
        }
        
        if (userRole != null) {
            binding.tvUserEmail.setText(userRole.equals("admin") ? "ADMIN" : "Staff");
        }
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout", (dialog, which) -> logout())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void logout() {
        api.logout(new ApiService.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            }

            @Override
            public void onError(String error) {
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
