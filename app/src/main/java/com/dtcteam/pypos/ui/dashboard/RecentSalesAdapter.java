package com.dtcteam.pypos.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.model.Sale;
import java.util.ArrayList;
import java.util.Locale;

public class RecentSalesAdapter extends RecyclerView.Adapter<RecentSalesAdapter.ViewHolder> {

    private ArrayList<Sale> sales = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Sale sale);
    }

    public void setSales(ArrayList<Sale> sales) {
        this.sales = sales;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sale_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sale sale = sales.get(position);
        holder.bind(sale);
    }

    @Override
    public int getItemCount() {
        return sales.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSaleId, tvSaleTime, tvSaleAmount;

        ViewHolder(View itemView) {
            super(itemView);
            tvSaleId = itemView.findViewById(R.id.tvSaleId);
            tvSaleTime = itemView.findViewById(R.id.tvSaleTime);
            tvSaleAmount = itemView.findViewById(R.id.tvSaleAmount);
        }

        void bind(Sale sale) {
            Locale usLocale = new Locale("en", "US");
            tvSaleId.setText("#" + String.format("%05d", sale.getId()));
            tvSaleAmount.setText("TSH " + String.format(usLocale, "%,d", (long) sale.getFinalAmount()));
            
            if (sale.getCreatedAt() != null && !sale.getCreatedAt().isEmpty()) {
                try {
                    String time = sale.getCreatedAt().substring(11, 19);
                    tvSaleTime.setText(time);
                } catch (Exception e) {
                    tvSaleTime.setText("");
                }
            } else {
                tvSaleTime.setText("");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(sale);
                }
            });
        }
    }
}
