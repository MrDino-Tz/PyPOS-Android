package com.dtcteam.pypos.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.FragmentAccountBinding;
import com.dtcteam.pypos.model.User;
import com.dtcteam.pypos.ui.login.LoginActivity;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private final ApiService api = ApiService.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        binding.btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        binding.btnLogout.setOnClickListener(v -> logout());
        
        loadUserInfo();
    }

    private void loadUserInfo() {
        api.getCurrentUser(new ApiService.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                if (result != null) {
                    binding.tvUserEmail.setText(result.getEmail());
                }
            }

            @Override
            public void onError(String error) {}
        });
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
