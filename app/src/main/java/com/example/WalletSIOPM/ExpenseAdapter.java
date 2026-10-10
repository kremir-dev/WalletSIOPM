package com.example.WalletSIOPM;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private final List<Expense> expenseList;
    private final AboneDatabase db;
    private final ExecutorService executor;
    private final Runnable onDataChangedListener;

    public ExpenseAdapter(List<Expense> expenseList, AboneDatabase db, ExecutorService executor, Runnable onDataChangedListener) {
        this.expenseList = expenseList;
        this.db = db;
        this.executor = executor;
        this.onDataChangedListener = onDataChangedListener;
    }

    public static int getBrandColor(String name) {
        if (name == null || name.isEmpty()) return Color.parseColor("#4A4A6A");
        String lower = name.toLowerCase();
        if (lower.contains("migros") || lower.contains("carrefour") || lower.contains("market")) return Color.parseColor("#FF6600");
        if (lower.contains("starbucks") || lower.contains("kahve")) return Color.parseColor("#00704A");
        if (lower.contains("trendyol") || lower.contains("hepsiburada")) return Color.parseColor("#F27A1A");
        if (lower.contains("yemeksepeti") || lower.contains("getir")) return Color.parseColor("#5D3EBC");

        int hash = name.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;
        return Color.rgb(Math.max(40, Math.min(200, r)), Math.max(40, Math.min(200, g)), Math.max(40, Math.min(200, b)));
    }

    public static String getBrandDomain(String name) {
        if (name == null) return "example.com";
        String lower = name.toLowerCase();
        if (lower.contains("migros")) return "migros.com.tr";
        if (lower.contains("carrefour")) return "carrefoursa.com";
        if (lower.contains("starbucks")) return "starbucks.com";
        if (lower.contains("trendyol")) return "trendyol.com";
        if (lower.contains("hepsiburada")) return "hepsiburada.com";
        if (lower.contains("yemeksepeti")) return "yemeksepeti.com";
        if (lower.contains("getir")) return "getir.com";
        return name.replaceAll("[^a-zA-Z0-9]", "").toLowerCase() + ".com";
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_abonelik, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.tvIsim.setText(expense.getTitle());

        String initial = !expense.getTitle().isEmpty() ? expense.getTitle().substring(0, 1).toUpperCase() : "?";
        holder.tvLogoInitials.setText(initial);
        Drawable bg = holder.tvLogoInitials.getBackground();
        if (bg instanceof GradientDrawable) {
            ((GradientDrawable) bg).setColor(getBrandColor(expense.getTitle()));
        }

        String logoUrl = "https://logo.clearbit.com/" + getBrandDomain(expense.getTitle());
        Glide.with(holder.itemView.getContext())
                .load(logoUrl)
                .circleCrop()
                .into(holder.ivBrandLogo);

        String currency = expense.getCurrency() != null ? expense.getCurrency() : "₺";
        String detailText = currency + expense.getAmount() + " • " + expense.getDate() + " (" + expense.getCategory() + ")";
        holder.tvTutar.setText(detailText);
        holder.tvPaymentMethod.setText("💳 " + expense.getPaymentMethod());

        holder.itemView.setOnClickListener(v -> {
            String notes = expense.getNotes() == null || expense.getNotes().isEmpty() ? "-" : expense.getNotes();
            new AlertDialog.Builder(v.getContext())
                    .setTitle(expense.getTitle())
                    .setMessage(
                            "Amount: " + currency + expense.getAmount() + "\n" +
                                    "Date: " + expense.getDate() + "\n" +
                                    "Category: " + expense.getCategory() + "\n" +
                                    "Payment Method: " + expense.getPaymentMethod() + "\n" +
                                    "Note: " + notes
                    )
                    .setPositiveButton("Close", null)
                    .show();
        });

        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Expense")
                    .setMessage("Are you sure you want to delete " + expense.getTitle() + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        executor.execute(() -> {
                            db.expenseDao().sil(expense);
                            ((Activity) v.getContext()).runOnUiThread(() -> {
                                int pos = holder.getAbsoluteAdapterPosition();
                                if (pos != RecyclerView.NO_POSITION) {
                                    expenseList.remove(pos);
                                    notifyItemRemoved(pos);
                                    if (onDataChangedListener != null) {
                                        onDataChangedListener.run();
                                    }
                                }
                            });
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvIsim, tvTutar, tvPaymentMethod, tvLogoInitials;
        ImageView ivBrandLogo;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIsim = itemView.findViewById(R.id.tvIsim);
            tvTutar = itemView.findViewById(R.id.tvTutar);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
            tvLogoInitials = itemView.findViewById(R.id.tvLogoInitials);
            ivBrandLogo = itemView.findViewById(R.id.ivBrandLogo);
        }
    }
}