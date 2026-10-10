package com.example.WalletSIOPM;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<TransactionItem> list;

    public TransactionAdapter(List<TransactionItem> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionItem item = list.get(position);

        boolean isIncome = "INCOME".equalsIgnoreCase(item.getType());
        String sign = isIncome ? "+" : "-";
        String colorHex = isIncome ? "#4CD964" : "#FF5252"; // Yeşil / Kırmızı

        holder.text1.setText(item.getTitle() + " (" + item.getDate() + ")");
        holder.text1.setTextColor(Color.parseColor("#F0F0F5"));

        holder.text2.setText(String.format(Locale.getDefault(), "%s %s%s", sign, item.getCurrency(), item.getAmount()));
        holder.text2.setTextColor(Color.parseColor(colorHex));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}