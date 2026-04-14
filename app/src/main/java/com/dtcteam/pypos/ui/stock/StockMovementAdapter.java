package com.dtcteam.pypos.ui.stock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.model.StockMovement;
import java.util.ArrayList;
import java.text.NumberFormat;
import java.util.Locale;

public class StockMovementAdapter extends RecyclerView.Adapter<StockMovementAdapter.ViewHolder> {

    private ArrayList<StockMovement> movements = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("sw", "TZ"));

    public void setMovements(ArrayList<StockMovement> movements) {
        this.movements = movements;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock_movement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StockMovement movement = movements.get(position);
        
        holder.tvDate.setText(movement.getCreatedAt());
        holder.tvItem.setText(movement.getItemName());
        
        String type = movement.getType();
        if ("in".equals(type)) {
            holder.tvType.setText("IN");
            holder.tvType.setTextColor(0xFF00C951);
        } else if ("out".equals(type)) {
            holder.tvType.setText("OUT");
            holder.tvType.setTextColor(0xFFFB2C36);
        } else {
            holder.tvType.setText("ADJ");
            holder.tvType.setTextColor(0xFFF0B100);
        }
        
        holder.tvQty.setText("+" + movement.getQuantity());
        holder.tvQty.setTextColor(0xFF00C951);
    }

    @Override
    public int getItemCount() {
        return movements.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvItem, tvType, tvQty;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvItem = itemView.findViewById(R.id.tvItem);
            tvType = itemView.findViewById(R.id.tvType);
            tvQty = itemView.findViewById(R.id.tvQty);
        }
    }
}