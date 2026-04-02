package com.dtcteam.pypos.ui.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.model.Item;
import java.util.ArrayList;
import java.util.Locale;

public class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ViewHolder> {

    private ArrayList<Item> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    public ItemsAdapter(ArrayList<Item> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pos, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvPrice, tvStock;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvPrice = itemView.findViewById(R.id.tvItemPrice);
            tvStock = itemView.findViewById(R.id.tvItemStock);
        }

        void bind(Item item, OnItemClickListener listener) {
            tvName.setText(item.getName());
            Locale usLocale = new Locale("en", "US");
            tvPrice.setText("TSH " + String.format(usLocale, "%,d", (long) item.getUnitPrice()));
            tvStock.setText("Stock: " + item.getQuantity());
            
            if (item.getQuantity() == 0) {
                itemView.setAlpha(0.5f);
            } else {
                itemView.setAlpha(1.0f);
            }
            
            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
