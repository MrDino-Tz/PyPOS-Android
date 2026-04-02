package com.dtcteam.pypos.ui.sales;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.FragmentSalesBinding;
import com.dtcteam.pypos.model.Sale;
import java.util.ArrayList;
import java.util.List;

public class SalesFragment extends Fragment {

    private FragmentSalesBinding binding;
    private final ApiService api = ApiService.getInstance();
    private ArrayList<Sale> sales = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSalesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        binding.salesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.salesRecyclerView.setAdapter(new SalesAdapter(sales));
        
        loadSales();
    }

    private void loadSales() {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.getSales(new ApiService.Callback<List<Sale>>() {
            @Override
            public void onSuccess(List<Sale> result) {
                sales = new ArrayList<>(result);
                binding.salesRecyclerView.getAdapter().notifyDataSetChanged();
                binding.loadingIndicator.setVisibility(View.GONE);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                binding.loadingIndicator.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
