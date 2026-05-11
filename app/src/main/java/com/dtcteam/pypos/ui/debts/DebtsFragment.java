package com.dtcteam.pypos.ui.debts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.FragmentDebtsBinding;
import com.dtcteam.pypos.model.Debt;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DebtsFragment extends Fragment {

    private FragmentDebtsBinding binding;
    private final ApiService api = ApiService.getInstance();
    private final List<Debt> debtsList = new ArrayList<>();
    private DebtAdapter adapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("sw", "TZ"));

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDebtsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        adapter = new DebtAdapter(debtsList, debt -> {
            // Handle debt click (e.g., show payment dialog)
            showPaymentDialog(debt);
        });
        
        binding.debtsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.debtsRecyclerView.setAdapter(adapter);
        
        loadDebts();
    }

    private void loadDebts() {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        api.getDebts(new ApiService.Callback<List<Debt>>() {
            @Override
            public void onSuccess(List<Debt> result) {
                debtsList.clear();
                debtsList.addAll(result);
                adapter.notifyDataSetChanged();
                updateSummary();
                binding.loadingIndicator.setVisibility(View.GONE);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                binding.loadingIndicator.setVisibility(View.GONE);
            }
        });
    }

    private void updateSummary() {
        double totalReceivables = 0;
        double totalPayables = 0;
        
        for (Debt debt : debtsList) {
            if ("receivable".equals(debt.getType())) {
                totalReceivables += debt.getRemainingAmount();
            } else {
                totalPayables += debt.getRemainingAmount();
            }
        }
        
        binding.tvReceivablesAmount.setText(currencyFormat.format(totalReceivables));
        binding.tvPayablesAmount.setText(currencyFormat.format(totalPayables));
    }

    private void showPaymentDialog(Debt debt) {
        // For now, just show a toast. In a real app, you'd show a dialog to enter payment amount.
        Toast.makeText(requireContext(), "Payment for " + debt.getPersonName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
