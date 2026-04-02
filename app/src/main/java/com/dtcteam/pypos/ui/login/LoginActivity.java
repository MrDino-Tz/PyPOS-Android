package com.dtcteam.pypos.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.ActivityLoginBinding;
import com.dtcteam.pypos.databinding.BottomSheetPinBinding;
import com.dtcteam.pypos.model.User;
import com.dtcteam.pypos.MainActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private final ApiService api = ApiService.getInstance();
    private SharedPreferences prefs;
    private String savedPin;
    private String savedEmail;
    private String savedPassword;
    private BottomSheetDialog pinBottomSheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = getSharedPreferences("pypos_prefs", MODE_PRIVATE);
        savedPin = prefs.getString("pin", null);
        savedEmail = prefs.getString("email", null);
        savedPassword = prefs.getString("password", null);

        setupUI();
        checkAutoLogin();
    }

    private void setupUI() {
        binding.loginButton.setOnClickListener(v -> login());
        
        binding.btnBiometric.setOnClickListener(v -> showBiometricPrompt());
        binding.btnPin.setOnClickListener(v -> showPinBottomSheet());
        
        binding.tvUseCredentials.setOnClickListener(v -> showCredentialsInput());
    }

    private void showPinBottomSheet() {
        BottomSheetPinBinding pinBinding = BottomSheetPinBinding.inflate(LayoutInflater.from(this));
        pinBottomSheet = new BottomSheetDialog(this);
        pinBottomSheet.setContentView(pinBinding.getRoot());

        final StringBuilder enteredPin = new StringBuilder();

        View.OnClickListener numberClickListener = v -> {
            if (enteredPin.length() < 4) {
                String digit = "";
                int id = v.getId();
                if (id == R.id.btn0) digit = "0";
                else if (id == R.id.btn1) digit = "1";
                else if (id == R.id.btn2) digit = "2";
                else if (id == R.id.btn3) digit = "3";
                else if (id == R.id.btn4) digit = "4";
                else if (id == R.id.btn5) digit = "5";
                else if (id == R.id.btn6) digit = "6";
                else if (id == R.id.btn7) digit = "7";
                else if (id == R.id.btn8) digit = "8";
                else if (id == R.id.btn9) digit = "9";

                enteredPin.append(digit);
                updatePinDots(pinBinding, enteredPin.length());

                if (enteredPin.length() == 4) {
                    verifyPin(enteredPin.toString(), pinBinding);
                }
            }
        };

        pinBinding.btn0.setOnClickListener(numberClickListener);
        pinBinding.btn1.setOnClickListener(numberClickListener);
        pinBinding.btn2.setOnClickListener(numberClickListener);
        pinBinding.btn3.setOnClickListener(numberClickListener);
        pinBinding.btn4.setOnClickListener(numberClickListener);
        pinBinding.btn5.setOnClickListener(numberClickListener);
        pinBinding.btn6.setOnClickListener(numberClickListener);
        pinBinding.btn7.setOnClickListener(numberClickListener);
        pinBinding.btn8.setOnClickListener(numberClickListener);
        pinBinding.btn9.setOnClickListener(numberClickListener);

        pinBinding.btnDelete.setOnClickListener(v -> {
            if (enteredPin.length() > 0) {
                enteredPin.deleteCharAt(enteredPin.length() - 1);
                updatePinDots(pinBinding, enteredPin.length());
            }
        });

        pinBottomSheet.show();
    }

    private void updatePinDots(BottomSheetPinBinding pinBinding, int length) {
        pinBinding.pinDot1.setBackgroundResource(length >= 1 ? R.drawable.bg_pin_dot : R.drawable.bg_pin_dot_empty);
        pinBinding.pinDot2.setBackgroundResource(length >= 2 ? R.drawable.bg_pin_dot : R.drawable.bg_pin_dot_empty);
        pinBinding.pinDot3.setBackgroundResource(length >= 3 ? R.drawable.bg_pin_dot : R.drawable.bg_pin_dot_empty);
        pinBinding.pinDot4.setBackgroundResource(length >= 4 ? R.drawable.bg_pin_dot : R.drawable.bg_pin_dot_empty);
    }

    private void verifyPin(String pin, BottomSheetPinBinding pinBinding) {
        if (pin.equals(savedPin)) {
            if (pinBottomSheet != null) {
                pinBottomSheet.dismiss();
            }
            if (savedEmail != null && savedPassword != null) {
                setLoading(true);
                api.login(savedEmail, savedPassword, new ApiService.Callback<User>() {
                    @Override
                    public void onSuccess(User result) {
                        navigateToMain();
                    }

                    @Override
                    public void onError(String error) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Please login with credentials first", Toast.LENGTH_SHORT).show();
                showCredentialsInput();
            }
        } else {
            pinBinding.tvError.setText("Invalid PIN");
            pinBinding.tvError.setVisibility(View.VISIBLE);
            new android.os.Handler().postDelayed(() -> {
                pinBinding.tvError.setVisibility(View.GONE);
                updatePinDots(pinBinding, 0);
            }, 1000);
        }
    }

    private void showCredentialsInput() {
        binding.pinInputSection.setVisibility(View.GONE);
        binding.credentialsSection.setVisibility(View.VISIBLE);
        binding.tvUseCredentials.setVisibility(View.GONE);
        binding.titleText.setText("Welcome Back");
        binding.subtitleText.setText("Sign in to access the POS");
    }

    private void showBiometricPrompt() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Biometric authentication not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Toast.makeText(LoginActivity.this, "Error: " + errString, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    if (savedEmail != null && savedPassword != null) {
                        setLoading(true);
                        api.login(savedEmail, savedPassword, new ApiService.Callback<User>() {
                            @Override
                            public void onSuccess(User result) {
                                navigateToMain();
                            }

                            @Override
                            public void onError(String error) {
                                setLoading(false);
                                Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(LoginActivity.this, "Please login with credentials first", Toast.LENGTH_SHORT).show();
                        showCredentialsInput();
                    }
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Toast.makeText(LoginActivity.this, "Biometric authentication failed", Toast.LENGTH_SHORT).show();
                }
            });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
            .setTitle("Pawin PyPOS")
            .setSubtitle("Use your fingerprint to login")
            .setNegativeButtonText("Cancel")
            .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void checkAutoLogin() {
        if (savedPin != null) {
            showPinBottomSheet();
        }
        
        if (savedPin != null || (savedEmail != null && savedPassword != null)) {
            binding.btnBiometric.setVisibility(View.VISIBLE);
            binding.btnPin.setVisibility(View.VISIBLE);
        } else {
            binding.btnBiometric.setVisibility(View.GONE);
            binding.btnPin.setVisibility(View.GONE);
        }
    }

    private void login() {
        String email = binding.emailInput.getText() != null ? binding.emailInput.getText().toString().trim() : "";
        String password = binding.passwordInput.getText() != null ? binding.passwordInput.getText().toString().trim() : "";

        if (email.isEmpty()) {
            binding.emailLayout.setError("Email is required");
            return;
        }
        binding.emailLayout.setError(null);

        if (password.isEmpty()) {
            binding.passwordLayout.setError("Password is required");
            return;
        }
        binding.passwordLayout.setError(null);

        setLoading(true);

        api.login(email, password, new ApiService.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                prefs.edit()
                    .putString("email", email)
                    .putString("password", password)
                    .apply();
                navigateToMain();
            }

            @Override
            public void onError(String error) {
                binding.errorText.setText(error);
                binding.errorText.setVisibility(View.VISIBLE);
                setLoading(false);
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.loginButton.setEnabled(!loading);
        binding.loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
