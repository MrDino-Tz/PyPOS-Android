package com.dtcteam.pypos.ui.debts;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.dtcteam.pypos.databinding.ItemDebtBinding;
import com.dtcteam.pypos.model.Debt;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class DebtAdapter extends RecyclerView.Adapter<DebtAdapter.ViewHolder> {

    private final List<Debt> debts;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("sw", "TZ"));
    private OnDebtClickListener listener;

    public interface OnDebtClickListener {
        void onDebtClick(Debt debt);
    }

    public DebtAdapter(List<Debt> debts, OnDebtClickListener listener) {
        this.debts = debts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDebtBinding binding = ItemDebtBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Debt debt = debts.get(position);
        holder.binding.tvPersonName.setText(debt.getPersonName());
        holder.binding.tvAmount.setText(currencyFormat.format(debt.getRemainingAmount()));
        holder.binding.tvStatus.setText(debt.getStatus().toUpperCase());
        holder.binding.tvType.setText(debt.getType().toUpperCase());
        
        // Color coding based on type
        if ("receivable".equals(debt.getType())) {
            holder.binding.ivType.setImageResource(android.R.drawable.arrow_down_float);
            holder.binding.ivType.setImageTintList(ColorStateList.valueOf(Color.parseColor("#00C951")));
        } else {
            holder.binding.ivType.setImageResource(android.R.drawable.arrow_up_float);
            holder.binding.ivType.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FB2C36")));
        }

        // Overdue styling
        if (debt.isOverdue()) {
            holder.binding.tvStatus.setText("OVERDUE");
            holder.binding.tvStatus.setTextColor(Color.RED);
        } else {
            holder.binding.tvStatus.setTextColor(Color.parseColor("#757575"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDebtClick(debt);
        });
    }

    @Override
    public int getItemCount() {
        return debts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemDebtBinding binding;

        public ViewHolder(ItemDebtBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
