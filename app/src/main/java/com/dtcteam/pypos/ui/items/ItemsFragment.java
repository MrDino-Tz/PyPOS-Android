package com.dtcteam.pypos.ui.items;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.FragmentItemsBinding;
import com.dtcteam.pypos.model.Item;
import java.util.ArrayList;
import java.util.List;

public class ItemsFragment extends Fragment {

    private FragmentItemsBinding binding;
    private final ApiService api = ApiService.getInstance();
    private ArrayList<Item> items = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentItemsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        binding.btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
        
        loadItems();
    }

    private void loadItems() {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.getItems(null, null, new ApiService.Callback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> result) {
                items.clear();
                if (result != null) {
                    for (Item item : result) {
                        if (!item.isService()) {
                            items.add(item);
                        }
                    }
                }
                binding.loadingIndicator.setVisibility(View.GONE);
            }

            @Override
            public void onError(String error) {
                binding.loadingIndicator.setVisibility(View.GONE);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
