package com.dtcteam.pypos.ui.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.model.CartItem;
import java.util.ArrayList;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private ArrayList<CartItem> cart;
    private OnQuantityChangeListener quantityListener;
    private OnRemoveListener removeListener;

    public interface OnQuantityChangeListener {
        void onQuantityChange(CartItem item, int delta);
    }

    public interface OnRemoveListener {
        void onRemove(CartItem item);
    }

    public CartAdapter(ArrayList<CartItem> cart, OnQuantityChangeListener qtyListener, OnRemoveListener removeListener) {
        this.cart = cart;
        this.quantityListener = qtyListener;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cart.get(position);
        holder.bind(item, quantityListener, removeListener);
    }

    @Override
    public int getItemCount() {
        return cart.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvPrice, tvQty, tvSubtotal;
        private ImageButton btnMinus, btnPlus, btnRemove;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCartItemName);
            tvPrice = itemView.findViewById(R.id.tvCartItemPrice);
            tvQty = itemView.findViewById(R.id.tvCartItemQty);
            tvSubtotal = itemView.findViewById(R.id.tvCartItemSubtotal);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }

        void bind(CartItem item, OnQuantityChangeListener qtyListener, OnRemoveListener removeListener) {
            tvName.setText(item.getName());
            Locale usLocale = new Locale("en", "US");
            tvPrice.setText("TSH " + String.format(usLocale, "%,d", (long) item.getUnitPrice()));
            tvQty.setText(String.valueOf(item.getQuantity()));
            tvSubtotal.setText("TSH " + String.format(usLocale, "%,d", (long) item.getSubtotal()));
            
            btnMinus.setOnClickListener(v -> qtyListener.onQuantityChange(item, -1));
            btnPlus.setOnClickListener(v -> qtyListener.onQuantityChange(item, 1));
            btnRemove.setOnClickListener(v -> removeListener.onRemove(item));
        }
    }
}
