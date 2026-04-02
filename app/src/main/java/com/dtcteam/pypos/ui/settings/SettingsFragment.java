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
        
        binding.btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
        
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
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        switch (themeMode) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                binding.tvThemeSelection.setText("Light");
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                binding.tvThemeSelection.setText("Dark");
                break;
            default:
                binding.tvThemeSelection.setText("System Default");
                break;
        }
    }

    private void showThemeSelectionDialog() {
        String[] themes = {"Light", "Dark", "System Default"};
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        
        int checkedItem = 2; // Default
        if (themeMode == AppCompatDelegate.MODE_NIGHT_NO) checkedItem = 0;
        else if (themeMode == AppCompatDelegate.MODE_NIGHT_YES) checkedItem = 1;

        new AlertDialog.Builder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
            .setTitle("Choose Theme")
            .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                int selectedMode;
                switch (which) {
                    case 0:
                        selectedMode = AppCompatDelegate.MODE_NIGHT_NO;
                        break;
                    case 1:
                        selectedMode = AppCompatDelegate.MODE_NIGHT_YES;
                        break;
                    default:
                        selectedMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                        break;
                }
                
                prefs.edit().putInt("theme_mode", selectedMode).apply();
                AppCompatDelegate.setDefaultNightMode(selectedMode);
                updateThemeText();
                dialog.dismiss();
            })
            .show();
    }

    private void showSetupPinDialog() {
        BottomSheetPinBinding pinBinding = BottomSheetPinBinding.inflate(LayoutInflater.from(requireContext()));
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(pinBinding.getRoot());
        
        final StringBuilder enteredPin = new StringBuilder();
        final StringBuilder[] firstPin = {new StringBuilder()};
        final boolean[] isConfirmMode = {false};
        
        pinBinding.getRoot().findViewById(R.id.btn1).setOnClickListener(v -> addDigit("1", enteredPin, pinBinding, dialog, isConfirmMode, firstPin));
        pinBinding.getRoot().findViewById(R.id.btn2).setOnClickListener(v -> addDigit("2", enteredPin, pinBinding, dialog, isConfirmMode, firstPin));
        pinBinding.getRoot().findViewById(R.id.btn3).setOnClickListener(v -> addDigit("3", enteredPin, pinBinding, dialog, isConfirmMode, firstPin));
        pinBinding.getRoot().findViewById(R.id.btn4).setOnClickListener(v -> addDigit("4", enteredPin, pinBinding, dialog, isConfirmMode, firstPin));
        pinBinding.getRoot().findViewById(R.id.btn5).setOnClickListener(v -> addDigit("5", enteredPin, pinBinding, dialog, isConfirmMode, firstPin));
        pinBinding.getRoot().findViewById(R.id.btn6).setOnClickListener(v -> addDigit("6", enteredPin, pinBinding, dialog, isConfirmMode, firstPin));
        pinBinding.getRoot().findViewById(R.id.btn7).setOnClickListener(v -> addDigit("7", enteredPin, pinBinding, dialog, isConfirmMode, firstPin));
        pinBinding.getRoot().findViewById(R.id.btn8).setOnClickListener(v -> addDigit("8", enteredPin, pinBinding, dialog, isConfirmMode, firstPin));
        pinBinding.getRoot().findViewById(R.id.btn9).setOnClickListener(v -> addDigit("9", enteredPin, pinBinding, dialog, isConfirmMode, firstPin));
        pinBinding.getRoot().findViewById(R.id.btn0).setOnClickListener(v -> addDigit("0", enteredPin, pinBinding, dialog, isConfirmMode, firstPin));
        
        pinBinding.getRoot().findViewById(R.id.btnDelete).setOnClickListener(v -> {
            if (enteredPin.length() > 0) {
                enteredPin.deleteCharAt(enteredPin.length() - 1);
                updatePinDots(pinBinding, enteredPin.length());
            }
        });
        
        pinBinding.tvError.setText("Set your PIN");
        dialog.show();
    }

    private void addDigit(String digit, StringBuilder enteredPin, BottomSheetPinBinding pinBinding, BottomSheetDialog dialog, boolean[] isConfirmMode, StringBuilder[] firstPin) {
        if (enteredPin.length() < 4) {
            enteredPin.append(digit);
            updatePinDots(pinBinding, enteredPin.length());
            
            if (enteredPin.length() == 4) {
                if (!isConfirmMode[0]) {
                    firstPin[0] = new StringBuilder(enteredPin);
                    enteredPin.setLength(0);
                    isConfirmMode[0] = true;
                    pinBinding.tvError.setText("Confirm your PIN");
                    updatePinDots(pinBinding, 0);
                } else {
                    if (enteredPin.toString().equals(firstPin[0].toString())) {
                        prefs.edit().putString("pin", enteredPin.toString()).apply();
                        dialog.dismiss();
                        binding.switchPin.setChecked(true);
                        binding.tvPinStatus.setText("Enabled");
                        Toast.makeText(requireContext(), "PIN enabled!", Toast.LENGTH_SHORT).show();
                    } else {
                        pinBinding.tvError.setText("PINs don't match. Try again.");
                        pinBinding.tvError.setVisibility(View.VISIBLE);
                        enteredPin.setLength(0);
                        firstPin[0] = new StringBuilder();
                        isConfirmMode[0] = false;
                        updatePinDots(pinBinding, 0);
                    }
                }
            }
        }
    }

    private void updatePinDots(BottomSheetPinBinding pinBinding, int length) {
        pinBinding.pinDot1.setBackgroundResource(length >= 1 ? com.dtcteam.pypos.R.drawable.bg_pin_dot : com.dtcteam.pypos.R.drawable.bg_pin_dot_empty);
        pinBinding.pinDot2.setBackgroundResource(length >= 2 ? com.dtcteam.pypos.R.drawable.bg_pin_dot : com.dtcteam.pypos.R.drawable.bg_pin_dot_empty);
        pinBinding.pinDot3.setBackgroundResource(length >= 3 ? com.dtcteam.pypos.R.drawable.bg_pin_dot : com.dtcteam.pypos.R.drawable.bg_pin_dot_empty);
        pinBinding.pinDot4.setBackgroundResource(length >= 4 ? com.dtcteam.pypos.R.drawable.bg_pin_dot : com.dtcteam.pypos.R.drawable.bg_pin_dot_empty);
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
