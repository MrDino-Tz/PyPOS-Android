package com.dtcteam.pypos.ui.sales;

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

public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.ViewHolder> {

    private ArrayList<Sale> sales;

    public SalesAdapter(ArrayList<Sale> sales) {
        this.sales = sales;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sale, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(sales.get(position));
    }

    @Override
    public int getItemCount() {
        return sales.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSaleId, tvDate, tvAmount, tvItems;

        ViewHolder(View itemView) {
            super(itemView);
            tvSaleId = itemView.findViewById(R.id.tvSaleId);
            tvDate = itemView.findViewById(R.id.tvSaleDate);
            tvAmount = itemView.findViewById(R.id.tvSaleAmount);
            tvItems = itemView.findViewById(R.id.tvSaleItems);
        }

        void bind(Sale sale) {
            Locale usLocale = new Locale("en", "US");
            tvSaleId.setText("#" + String.format("%05d", sale.getId()));
            tvDate.setText(sale.getCreatedAt());
            tvAmount.setText("TSH " + String.format(usLocale, "%,d", (long) sale.getFinalAmount()));
            tvItems.setText(sale.getSaleItems().size() + " items");
        }
    }
}
