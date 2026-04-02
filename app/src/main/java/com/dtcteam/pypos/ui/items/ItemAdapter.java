package com.dtcteam.pypos.ui.items;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.model.Item;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private ArrayList<Item> items = new ArrayList<>();
    private OnItemClickListener listener;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("sw", "TZ"));

    public interface OnItemClickListener {
        void onEditClick(Item item);
        void onDeleteClick(Item item);
    }

    public void setItems(ArrayList<Item> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.tvSku.setText(item.getSku());
        holder.tvName.setText(item.getName());
        holder.tvCategory.setText(item.getCategoryName() != null ? item.getCategoryName() : "Uncategorized");
        holder.tvUnitPrice.setText(currencyFormat.format(item.getUnitPrice()));
        holder.tvCost.setText(currencyFormat.format(item.getCost()));
        holder.tvStock.setText(String.valueOf(item.getQuantity()));
        holder.tvMinStock.setText(String.valueOf(item.getMinStockLevel()));

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSku, tvCategory, tvStock, tvUnitPrice, tvCost, tvMinStock;
        ImageButton btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSku = itemView.findViewById(R.id.tvSku);
            tvName = itemView.findViewById(R.id.tvName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvUnitPrice = itemView.findViewById(R.id.tvUnitPrice);
            tvCost = itemView.findViewById(R.id.tvCost);
            tvStock = itemView.findViewById(R.id.tvStock);
            tvMinStock = itemView.findViewById(R.id.tvMinStock);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
