package com.dtcteam.pypos.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.dtcteam.pypos.R;
import com.dtcteam.pypos.ThemeManager;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.BottomSheetPinBinding;
import com.dtcteam.pypos.databinding.FragmentSettingsBinding;
import com.dtcteam.pypos.model.User;
import com.dtcteam.pypos.ui.login.LoginActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private final ApiService api = ApiService.getInstance();
    private SharedPreferences prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        prefs = requireContext().getSharedPreferences("pypos_prefs", requireContext().MODE_PRIVATE);
        
        loadUserInfo();
        updatePinStatus();
        updateThemeText();
        
        binding.cardTheme.setOnClickListener(v -> showThemeSelectionDialog());
        
        binding.cardLogout.setOnClickListener(v -> logout());
        
        binding.switchPin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showSetupPinDialog();
            } else {
                disablePin();
            }
        });
    }

    private void updatePinStatus() {
        String savedPin = prefs.getString("pin", null);
        if (savedPin != null) {
            binding.tvPinStatus.setText("Enabled");
            binding.switchPin.setChecked(true);
        } else {
            binding.tvPinStatus.setText("Not set");
            binding.switchPin.setChecked(false);
        }
    }

    private void updateThemeText() {
        ThemeManager tm = ThemeManager.getInstance(requireContext());
        binding.tvThemeSelection.setText(tm.getThemeName());
    }

    private void showThemeSelectionDialog() {
        ThemeManager tm = ThemeManager.getInstance(requireContext());
        String[] themes = {"Light", "Dark", "System Default"};
        int currentMode = tm.getThemeMode();
        
        int checkedItem = 2; // Default
        if (currentMode == ThemeManager.THEME_LIGHT) checkedItem = 0;
        else if (currentMode == ThemeManager.THEME_DARK) checkedItem = 1;

        new AlertDialog.Builder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
            .setTitle("Choose Theme")
            .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                int selectedMode;
                switch (which) {
                    case 0:
                        selectedMode = ThemeManager.THEME_LIGHT;
                        break;
                    case 1:
                        selectedMode = ThemeManager.THEME_DARK;
                        break;
                    default:
                        selectedMode = ThemeManager.THEME_SYSTEM;
                        break;
                }
                
                // Save preference first
                tm.setThemeMode(selectedMode);
                updateThemeText();
                
                // Apply theme change immediately without recreation
                switch (selectedMode) {
                    case ThemeManager.THEME_LIGHT:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        break;
                    case ThemeManager.THEME_DARK:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        break;
                    default:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        break;
                }
                
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showSetupPinDialog() {
        BottomSheetPinBinding pinBinding = BottomSheetPinBinding.inflate(LayoutInflater.from(requireContext()));
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(pinBinding.getRoot());
        
        final StringBuilder firstPin = new StringBuilder();
        final boolean[] isConfirmMode = {false};
        
        pinBinding.etHiddenPin.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String pin = s.toString();
                updatePinDots(pinBinding, pin.length());
                
                if (pin.length() == 4) {
                    if (!isConfirmMode[0]) {
                        firstPin.append(pin);
                        pinBinding.etHiddenPin.setText("");
                        isConfirmMode[0] = true;
                        pinBinding.tvError.setText("Confirm your PIN");
                        pinBinding.tvError.setVisibility(View.VISIBLE);
                    } else {
                        if (pin.toString().equals(firstPin.toString())) {
                            prefs.edit().putString("pin", pin).apply();
                            dialog.dismiss();
                            binding.switchPin.setChecked(true);
                            binding.tvPinStatus.setText("Enabled");
                            Toast.makeText(requireContext(), "PIN enabled!", Toast.LENGTH_SHORT).show();
                        } else {
                            pinBinding.tvError.setText("PINs don't match. Try again.");
                            pinBinding.tvError.setVisibility(View.VISIBLE);
                            pinBinding.etHiddenPin.setText("");
                            firstPin.setLength(0);
                            isConfirmMode[0] = false;
                            updatePinDots(pinBinding, 0);
                        }
                    }
                }
            }
        });
        
        pinBinding.tvError.setText("Set your PIN");
        pinBinding.tvError.setVisibility(View.VISIBLE);
        pinBinding.etHiddenPin.requestFocus();
        dialog.show();
    }

    private void updatePinDots(BottomSheetPinBinding pinBinding, int length) {
        pinBinding.pinDot1.setBackgroundResource(length >= 1 ? com.dtcteam.pypos.R.drawable.bg_pin_circle_filled_large : com.dtcteam.pypos.R.drawable.bg_pin_circle_empty_large);
        pinBinding.pinDot2.setBackgroundResource(length >= 2 ? com.dtcteam.pypos.R.drawable.bg_pin_circle_filled_large : com.dtcteam.pypos.R.drawable.bg_pin_circle_empty_large);
        pinBinding.pinDot3.setBackgroundResource(length >= 3 ? com.dtcteam.pypos.R.drawable.bg_pin_circle_filled_large : com.dtcteam.pypos.R.drawable.bg_pin_circle_empty_large);
        pinBinding.pinDot4.setBackgroundResource(length >= 4 ? com.dtcteam.pypos.R.drawable.bg_pin_circle_filled_large : com.dtcteam.pypos.R.drawable.bg_pin_circle_empty_large);
    }

    private void disablePin() {
        prefs.edit().remove("pin").apply();
        binding.tvPinStatus.setText("Not set");
        Toast.makeText(requireContext(), "PIN disabled", Toast.LENGTH_SHORT).show();
    }

    private void loadUserInfo() {
        api.getCurrentUser(new ApiService.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                if (binding != null && result != null) {
                    binding.tvEmail.setText(result.getEmail());
                }
            }

            @Override
            public void onError(String error) {
                // Handle silently
            }
        });
    }

    private void logout() {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.logout(new ApiService.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onError(String error) {
                if (binding != null) {
                    binding.loadingIndicator.setVisibility(View.GONE);
                }
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
