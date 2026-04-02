package com.dtcteam.pypos.ui.pos;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.api.ApiService;
import com.dtcteam.pypos.databinding.FragmentPosBinding;
import com.dtcteam.pypos.model.CartItem;
import com.dtcteam.pypos.model.Category;
import com.dtcteam.pypos.model.Item;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PosFragment extends Fragment {

    private FragmentPosBinding binding;
    private final ApiService api = ApiService.getInstance();
    private ArrayList<Item> items = new ArrayList<>();
    private ArrayList<Item> serviceItems = new ArrayList<>();
    private ArrayList<Category> categories = new ArrayList<>();
    private ArrayList<CartItem> cart = new ArrayList<>();
    private ItemsAdapter itemsAdapter;
    private CartAdapter cartAdapter;
    private int selectedCategoryId = -1;
    private final String[] mainServices = {"black & white printing", "document scanning", "binding"};
    private ArrayList<Item> primaryServices = new ArrayList<>();
    private ArrayList<Item> otherServices = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerViews();
        setupSearch();
        loadData();
        
        binding.btnClearCart.setOnClickListener(v -> clearCart());
        binding.btnCheckout.setOnClickListener(v -> showCheckoutDialog());
    }

    private void setupRecyclerViews() {
        itemsAdapter = new ItemsAdapter(items, item -> addToCart(item));
        binding.itemsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.itemsRecyclerView.setAdapter(itemsAdapter);

        cartAdapter = new CartAdapter(cart, this::updateQuantity, this::removeFromCart);
        binding.cartRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.cartRecyclerView.setAdapter(cartAdapter);
    }

    private void setupSearch() {
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                loadItems();
            }
        });
    }

    private void loadData() {
        if (binding == null) return;
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        api.getCategories(new ApiService.Callback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> result) {
                if (binding == null) return;
                categories = new ArrayList<>(result);
                setupCategoryChips();
                loadItems();
            }

            @Override
            public void onError(String error) {
                binding.loadingIndicator.setVisibility(View.GONE);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadItems() {
        if (binding == null) return;
        String search = binding.searchInput.getText() != null ? binding.searchInput.getText().toString() : "";
        
        api.getItems(search.isEmpty() ? null : search, selectedCategoryId > 0 ? selectedCategoryId : null, new ApiService.Callback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> result) {
                // Separate regular items from service items
                ArrayList<Item> allItems = new ArrayList<>(result);
                ArrayList<Item> services = new ArrayList<>();
                ArrayList<Item> regularItems = new ArrayList<>();
                
                for (Item item : allItems) {
                    if (item.isService()) {
                        services.add(item);
                    } else {
                        regularItems.add(item);
                    }
                }
                
                serviceItems = services;
                items = regularItems;
                
                // Separate primary and other services
                primaryServices.clear();
                otherServices.clear();
                for (Item service : services) {
                    boolean isPrimary = false;
                    for (String main : mainServices) {
                        if (service.getName().toLowerCase().contains(main)) {
                            isPrimary = true;
                            break;
                        }
                    }
                    if (isPrimary) {
                        primaryServices.add(service);
                    } else {
                        otherServices.add(service);
                    }
                }
                
                itemsAdapter.notifyDataSetChanged();
                displayServices();
                binding.loadingIndicator.setVisibility(View.GONE);
            }

            @Override
            public void onError(String error) {
                binding.loadingIndicator.setVisibility(View.GONE);
            }
        });
    }

    private void setupCategoryChips() {
        if (binding == null || binding.categoryChips == null) return;
        binding.categoryChips.removeAllViews();
        
        Chip allChip = new Chip(requireContext());
        allChip.setText("All");
        allChip.setCheckable(true);
        allChip.setChecked(true);
        allChip.setOnClickListener(v -> {
            selectedCategoryId = -1;
            loadItems();
        });
        binding.categoryChips.addView(allChip);
        
        for (Category category : categories) {
            Chip chip = new Chip(requireContext());
            chip.setText(category.getName());
            chip.setCheckable(true);
            chip.setOnClickListener(v -> {
                selectedCategoryId = category.getId();
                loadItems();
            });
            binding.categoryChips.addView(chip);
        }
    }

    private void displayServices() {
        LinearLayout servicesContainer = binding.getRoot().findViewById(R.id.servicesContainer);
        if (servicesContainer == null) return;
        
        servicesContainer.removeAllViews();
        View servicesSection = binding.getRoot().findViewById(R.id.servicesSection);
        
        if (primaryServices.isEmpty() && otherServices.isEmpty()) {
            if (servicesSection != null) servicesSection.setVisibility(View.GONE);
            return;
        }
        
        if (servicesSection != null) servicesSection.setVisibility(View.VISIBLE);
        
        // Display primary services
        for (Item service : primaryServices) {
            View card = createServiceCard(service);
            servicesContainer.addView(card);
        }
        
        // Display "More" button if there are other services
        if (!otherServices.isEmpty()) {
            View moreCard = createMoreServicesCard();
            servicesContainer.addView(moreCard);
        }
    }

    private View createServiceCard(Item service) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            (int) (80 * getResources().getDisplayMetrics().density),
            (int) (70 * getResources().getDisplayMetrics().density)
        );
        params.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
        card.setLayoutParams(params);
        
        card.setBackgroundResource(R.drawable.bg_service_card);
        card.setPadding(8, 8, 8, 8);
        card.setOnClickListener(v -> addToCart(service));
        
        TextView name = new TextView(requireContext());
        name.setText(service.getName().length() > 15 ? service.getName().substring(0, 12) + "..." : service.getName());
        name.setTextSize(9);
        name.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        
        TextView price = new TextView(requireContext());
        price.setText("TSH " + (int) service.getUnitPrice());
        price.setTextSize(11);
        price.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
        price.setGravity(Gravity.CENTER);
        price.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        
        card.addView(name);
        card.addView(price);
        
        return card;
    }

    private View createMoreServicesCard() {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            (int) (60 * getResources().getDisplayMetrics().density),
            (int) (70 * getResources().getDisplayMetrics().density)
        );
        params.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
        card.setLayoutParams(params);
        
        card.setBackgroundResource(R.drawable.bg_more_services);
        card.setPadding(8, 8, 8, 8);
        card.setOnClickListener(v -> showMoreServicesDialog());
        
        TextView plus = new TextView(requireContext());
        plus.setText("+");
        plus.setTextSize(20);
        plus.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400));
        plus.setGravity(Gravity.CENTER);
        
        TextView more = new TextView(requireContext());
        more.setText("More (" + otherServices.size() + ")");
        more.setTextSize(9);
        more.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500));
        more.setGravity(Gravity.CENTER);
        
        card.addView(plus);
        card.addView(more);
        
        return card;
    }

    private void showMoreServicesDialog() {
        String[] serviceNames = new String[otherServices.size()];
        final int[] selectedIndex = {0};
        
        for (int i = 0; i < otherServices.size(); i++) {
            serviceNames[i] = otherServices.get(i).getName() + " - TSH " + (int) otherServices.get(i).getUnitPrice();
        }
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("More Services")
            .setItems(serviceNames, (dialog, which) -> {
                addToCart(otherServices.get(which));
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void addToCart(Item item) {
        for (CartItem cartItem : cart) {
            if (cartItem.getItemId() == item.getId()) {
                // For service items, no stock check needed
                if (item.isService() || cartItem.getQuantity() < item.getQuantity()) {
                    cartItem.setQuantity(cartItem.getQuantity() + 1);
                    cartItem.setSubtotal(cartItem.getQuantity() * cartItem.getUnitPrice());
                } else {
                    Toast.makeText(requireContext(), "Not enough stock", Toast.LENGTH_SHORT).show();
                }
                cartAdapter.notifyDataSetChanged();
                updateTotal();
                return;
            }
        }
        
        if (item.getQuantity() > 0 || item.isService()) {
            CartItem newItem = new CartItem(
                item.getId(),
                item.getName(),
                item.getSku(),
                item.getUnitPrice(),
                1,
                item.getUnitPrice(),
                item.isService() ? 9999 : item.getQuantity()
            );
            cart.add(newItem);
            cartAdapter.notifyDataSetChanged();
            updateTotal();
        } else {
            Toast.makeText(requireContext(), "Item out of stock", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateQuantity(CartItem item, int delta) {
        int newQty = item.getQuantity() + delta;
        if (newQty <= 0) {
            removeFromCart(item);
            return;
        }
        if (!item.isService() && newQty > item.getMaxQty()) {
            Toast.makeText(requireContext(), "Not enough stock", Toast.LENGTH_SHORT).show();
            return;
        }
        item.setQuantity(newQty);
        item.setSubtotal(newQty * item.getUnitPrice());
        cartAdapter.notifyDataSetChanged();
        updateTotal();
    }

    private void removeFromCart(CartItem item) {
        cart.remove(item);
        cartAdapter.notifyDataSetChanged();
        updateTotal();
    }

    private void clearCart() {
        cart.clear();
        cartAdapter.notifyDataSetChanged();
        updateTotal();
    }

    private void updateTotal() {
        double total = 0;
        int count = 0;
        for (CartItem item : cart) {
            total += item.getSubtotal();
            count += item.getQuantity();
        }
        
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.US);
        binding.tvTotal.setText("TSH " + nf.format((long) total));
        binding.tvCartCount.setText("(" + count + " items)");
        binding.btnCheckout.setEnabled(!cart.isEmpty());
    }

    private void showCheckoutDialog() {
        if (cart.isEmpty()) {
            Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_checkout, null);
        TextInputEditText paymentInput = dialogView.findViewById(R.id.paymentInput);
        
        double total = getTotal();
        paymentInput.setText(String.valueOf((long) total));
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Checkout")
            .setView(dialogView)
            .setPositiveButton("Complete Sale", (dialog, which) -> {
                String paymentStr = paymentInput.getText() != null ? paymentInput.getText().toString() : "0";
                double payment = Double.parseDouble(paymentStr);
                
                if (payment < total) {
                    Toast.makeText(requireContext(), "Insufficient payment", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                processSale();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private double getTotal() {
        double total = 0;
        for (CartItem item : cart) {
            total += item.getSubtotal();
        }
        return total;
    }

    private void processSale() {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        List<Map<String, Object>> saleItems = new ArrayList<>();
        for (CartItem item : cart) {
            Map<String, Object> saleItem = new HashMap<>();
            saleItem.put("item_id", item.getItemId());
            saleItem.put("quantity", item.getQuantity());
            saleItem.put("unit_price", item.getUnitPrice());
            saleItems.add(saleItem);
        }
        
        api.createSale(saleItems, "cash", 0.0, null, new ApiService.Callback<com.dtcteam.pypos.model.Sale>() {
            @Override
            public void onSuccess(com.dtcteam.pypos.model.Sale result) {
                Toast.makeText(requireContext(), "Sale completed!", Toast.LENGTH_SHORT).show();
                clearCart();
                loadData();
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
