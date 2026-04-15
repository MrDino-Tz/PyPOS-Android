package com.dtcteam.pypos.ui.items;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.dtcteam.pypos.R;
import com.dtcteam.pypos.model.Item;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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

    private boolean isServiceItem(Item item) {
        if (item.isService()) return true;
        String name = item.getName() != null ? item.getName().toLowerCase() : "";
        return name.contains("printing") || name.contains("scanning") || name.contains("binding");
    }

    private void loadImage(ImageView imageView, String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            new Thread(() -> {
                try {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(imageUrl).build();
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        byte[] imageData = response.body().bytes();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                        imageView.post(() -> imageView.setImageBitmap(bitmap));
                    }
                } catch (Exception e) {
                    // Use default icon
                }
            }).start();
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.tvName.setText(item.getName());
        holder.tvSku.setText(item.getSku());
        holder.tvCategory.setText(item.getCategoryName() != null ? item.getCategoryName() : "Uncategorized");
        holder.tvUnitPrice.setText(currencyFormat.format(item.getUnitPrice()));
        
        // Show stock quantity or infinity for services
        if (isServiceItem(item)) {
            holder.tvStock.setText("∞");
            holder.tvStatus.setText("∞");
            holder.tvStatus.setTextColor(0xFFa3a3a3);
        } else {
            holder.tvStock.setText("Stock: " + item.getQuantity());
            if (item.getQuantity() <= 0) {
                holder.tvStatus.setText("Out of Stock");
                holder.tvStatus.setTextColor(0xFFFB2C36);
            } else if (item.getQuantity() <= item.getMinStockLevel()) {
                holder.tvStatus.setText("Low Stock");
                holder.tvStatus.setTextColor(0xFFF0B100);
            } else {
                holder.tvStatus.setText("In Stock");
                holder.tvStatus.setTextColor(0xFF00C951);
            }
        }

        // Load image
        loadImage(holder.ivItemImage, item.getImageUrl());

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
        ImageView ivItemImage;
        TextView tvName, tvSku, tvCategory, tvStock, tvUnitPrice, tvStatus;
        ImageButton btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.ivItemImage);
            tvName = itemView.findViewById(R.id.tvName);
            tvSku = itemView.findViewById(R.id.tvSku);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvUnitPrice = itemView.findViewById(R.id.tvUnitPrice);
            tvStock = itemView.findViewById(R.id.tvStock);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}