package com.dtcteam.pypos.ui.settings;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.FragmentServicesBinding;
import com.dtcteam.pypos.model.Item;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

public class ServicesFragment extends Fragment {

    private FragmentServicesBinding binding;
    private final ApiService api = ApiService.getInstance();
    private ArrayList<Item> services = new ArrayList<>();
    private ArrayList<Item> filteredServices = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentServicesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        binding.servicesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.servicesRecyclerView.setAdapter(new ServicesAdapter());
        
        setupSearch();
        loadServices();
    }

    private void setupSearch() {
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterServices(s.toString());
            }
        });
    }

    private void loadServices() {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.getItems(null, null, new ApiService.Callback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> result) {
                services.clear();
                for (Item item : result) {
                    if (item.isService()) {
                        services.add(item);
                    }
                }
                filteredServices = new ArrayList<>(services);
                binding.servicesRecyclerView.getAdapter().notifyDataSetChanged();
                binding.loadingIndicator.setVisibility(View.GONE);
            }

            @Override
            public void onError(String error) {
                binding.loadingIndicator.setVisibility(View.GONE);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterServices(String query) {
        filteredServices.clear();
        if (query.isEmpty()) {
            filteredServices = new ArrayList<>(services);
        } else {
            for (Item item : services) {
                if (item.getName().toLowerCase().contains(query.toLowerCase()) ||
                    item.getSku().toLowerCase().contains(query.toLowerCase())) {
                    filteredServices.add(item);
                }
            }
        }
        binding.servicesRecyclerView.getAdapter().notifyDataSetChanged();
    }

    private void showEditDialog(Item service) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_price, null);
        EditText priceInput = dialogView.findViewById(R.id.priceInput);
        priceInput.setText(String.valueOf((int) service.getUnitPrice()));
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Price")
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                String priceStr = priceInput.getText().toString();
                if (priceStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a price", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                double newPrice = Double.parseDouble(priceStr);
                updatePrice(service.getId(), newPrice);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updatePrice(int id, double newPrice) {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.updateItem(id, newPrice, new ApiService.Callback<Item>() {
            @Override
            public void onSuccess(Item result) {
                Toast.makeText(requireContext(), "Price updated", Toast.LENGTH_SHORT).show();
                loadServices();
            }

            @Override
            public void onError(String error) {
                binding.loadingIndicator.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class ServicesAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ServicesAdapter.ViewHolder> {
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Item service = filteredServices.get(position);
            holder.bind(service);
        }

        @Override
        public int getItemCount() {
            return filteredServices.size();
        }

        class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            private com.dtcteam.pypos.databinding.ItemServiceBinding itemBinding;

            ViewHolder(View itemView) {
                super(itemView);
                itemBinding = com.dtcteam.pypos.databinding.ItemServiceBinding.bind(itemView);
            }

            void bind(Item service) {
                itemBinding.tvServiceName.setText(service.getName());
                itemBinding.tvServiceSku.setText(service.getSku());
                itemBinding.tvServicePrice.setText("TSH " + (int) service.getUnitPrice());
                
                itemBinding.btnEdit.setOnClickListener(v -> showEditDialog(service));
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
