package com.example.WalletSIOPM;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

public class AboneAdapter extends RecyclerView.Adapter<AboneAdapter.AboneViewHolder> {
    private final List<Abonelik> subscriptionList;
    private final AboneDatabase db;
    private final ExecutorService executor;
    private final Runnable onDataChangedListener;

    public AboneAdapter(List<Abonelik> subscriptionList, AboneDatabase db, ExecutorService executor, Runnable onDataChangedListener) {
        this.subscriptionList = subscriptionList;
        this.db = db;
        this.executor = executor;
        this.onDataChangedListener = onDataChangedListener;
    }

    public static int getBrandColor(String name) {
        if (name == null || name.isEmpty()) return Color.parseColor("#4A4A6A");
        String lower = name.toLowerCase();
        if (lower.contains("netflix")) return Color.parseColor("#E50914");
        if (lower.contains("spotify")) return Color.parseColor("#1DB954");
        if (lower.contains("youtube") || lower.contains("google")) return Color.parseColor("#FF0000");
        if (lower.contains("disney")) return Color.parseColor("#113CCF");
        if (lower.contains("amazon") || lower.contains("prime")) return Color.parseColor("#FF9900");
        if (lower.contains("apple") || lower.contains("icloud") || lower.contains("music")) return Color.parseColor("#555555");
        if (lower.contains("github") || lower.contains("chatgpt") || lower.contains("openai")) return Color.parseColor("#10A37F");
        if (lower.contains("blutv") || lower.contains("gain") || lower.contains("exxen")) return Color.parseColor("#3B82F6");

        int hash = name.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;
        return Color.rgb(Math.max(40, Math.min(200, r)), Math.max(40, Math.min(200, g)), Math.max(40, Math.min(200, b)));
    }

    public static int getBrandIconRes(String name) {
        if (name == null) return 0;
        String lower = name.toLowerCase();
        if (lower.contains("netflix")) return R.drawable.ic_brand_netflix;
        if (lower.contains("spotify")) return R.drawable.ic_brand_spotify;
        if (lower.contains("youtube") || lower.contains("google")) return R.drawable.ic_brand_youtube;
        return 0;
    }

    public static String getBrandDomain(String name) {
        if (name == null) return "example.com";
        String lower = name.toLowerCase();
        if (lower.contains("netflix")) return "netflix.com";
        if (lower.contains("spotify")) return "spotify.com";
        if (lower.contains("youtube")) return "youtube.com";
        if (lower.contains("disney")) return "disneyplus.com";
        if (lower.contains("amazon") || lower.contains("prime")) return "amazon.com";
        if (lower.contains("apple") || lower.contains("icloud")) return "apple.com";
        if (lower.contains("github")) return "github.com";
        if (lower.contains("chatgpt") || lower.contains("openai")) return "openai.com";
        if (lower.contains("google")) return "google.com";
        if (lower.contains("steam")) return "steampowered.com";
        if (lower.contains("playstation") || lower.contains("psn")) return "playstation.com";
        if (lower.contains("xbox")) return "xbox.com";
        if (lower.contains("blutv")) return "blutv.com";
        if (lower.contains("exxen")) return "exxen.com";
        if (lower.contains("gain")) return "gain.tv";
        return name.replaceAll("[^a-zA-Z0-9]", "").toLowerCase() + ".com";
    }

    @NonNull
    @Override
    public AboneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_abonelik, parent, false);
        return new AboneViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AboneViewHolder holder, int position) {
        Abonelik subscription = subscriptionList.get(position);
        holder.tvIsim.setText(subscription.getName());

        int localIconRes = getBrandIconRes(subscription.getName());
        if (localIconRes != 0) {
            holder.ivBrandLogo.setImageResource(localIconRes);
            holder.ivBrandLogo.setBackground(holder.tvLogoInitials.getBackground());
            Drawable bg = holder.ivBrandLogo.getBackground();
            if (bg instanceof GradientDrawable) {
                ((GradientDrawable) bg).setColor(getBrandColor(subscription.getName()));
            }
            holder.ivBrandLogo.setPadding(16, 16, 16, 16);
            holder.tvLogoInitials.setVisibility(View.GONE);
            holder.ivBrandLogo.setVisibility(View.VISIBLE);
        } else {
            holder.tvLogoInitials.setVisibility(View.VISIBLE);
            String initial = !subscription.getName().isEmpty() ? subscription.getName().substring(0, 1).toUpperCase() : "?";
            holder.tvLogoInitials.setText(initial);
            Drawable bg = holder.tvLogoInitials.getBackground();
            if (bg instanceof GradientDrawable) {
                ((GradientDrawable) bg).setColor(getBrandColor(subscription.getName()));
            }
            holder.ivBrandLogo.setPadding(0, 0, 0, 0);
            holder.ivBrandLogo.setBackground(null);

            // Fetch Real Favicon / Logo via Google Favicon API (100% reliable)
            String domain = getBrandDomain(subscription.getName());
            String logoUrl = "https://www.google.com/s2/favicons?domain=" + domain + "&sz=128";
            Glide.with(holder.itemView.getContext())
                    .load(logoUrl)
                    .circleCrop()
                    .into(holder.ivBrandLogo);
        }

        // Dinamik Para Birimi Okuma
        String currency = (subscription.getCurrency() != null && !subscription.getCurrency().isEmpty())
                ? subscription.getCurrency() : "₺";

        // "Para Birimi + Tutar • Tarih (Periyot)" formatı
        String cycleStr = (subscription.getBillingCycle() != null && !subscription.getBillingCycle().isEmpty())
                ? " (" + subscription.getBillingCycle() + ")" : "";
        String detailText = currency + subscription.getAmount() + " • " + subscription.getDate() + cycleStr;
        holder.tvTutar.setText(detailText);
        holder.tvPaymentMethod.setText("💳 " + subscription.getPaymentMethod());

        // Tıklayınca açılan detay diyaloğu
        holder.itemView.setOnClickListener(v -> {
            String notesDisplay = (subscription.getNotes() == null || subscription.getNotes().isEmpty()) ? "-" : subscription.getNotes();
            String cycleDisplay = (subscription.getBillingCycle() == null || subscription.getBillingCycle().isEmpty()) ? "-" : subscription.getBillingCycle();

            new AlertDialog.Builder(v.getContext())
                    .setTitle(subscription.getName())
                    .setMessage(
                            "Amount: " + currency + subscription.getAmount() + "\n" +
                                    "Date: " + subscription.getDate() + "\n" +
                                    "Category: " + subscription.getCategory() + "\n" +
                                    "Payment Method: " + subscription.getPaymentMethod() + "\n" +
                                    "Billing Cycle: " + cycleDisplay + "\n" +
                                    "Note: " + notesDisplay
                    )
                    .setPositiveButton("Edit", (dialog, which) -> {
                        openEditForm(v.getContext(), subscription, holder.getAbsoluteAdapterPosition());
                    })
                    .setNegativeButton("Close", null)
                    .show();
        });

        // Uzun basarak silme diyaloğu
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Subscription")
                    .setMessage("Are you sure you want to delete " + subscription.getName() + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        executor.execute(() -> {
                            db.aboneDao().sil(subscription);
                            ((Activity) v.getContext()).runOnUiThread(() -> {
                                int positionToRemove = holder.getAbsoluteAdapterPosition();
                                if (positionToRemove != RecyclerView.NO_POSITION) {
                                    subscriptionList.remove(positionToRemove);
                                    notifyItemRemoved(positionToRemove);

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

    private void openEditForm(Context context, Abonelik subscription, int position) {
        ScrollView scrollView = new ScrollView(context);
        LinearLayout dialogLayout = new LinearLayout(context);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(50, 40, 50, 10);
        scrollView.addView(dialogLayout);

        EditText etName = new EditText(context);
        etName.setHint("Subscription Name");
        etName.setText(subscription.getName());
        dialogLayout.addView(etName);

        EditText etAmount = new EditText(context);
        etAmount.setHint("Amount");
        etAmount.setText(subscription.getAmount());
        dialogLayout.addView(etAmount);

        // Para Birimi Alanı
        EditText etCurrency = new EditText(context);
        etCurrency.setHint("Select Currency");
        String currentCurrency = (subscription.getCurrency() != null && !subscription.getCurrency().isEmpty())
                ? subscription.getCurrency() : "₺";
        etCurrency.setText(currentCurrency);
        etCurrency.setFocusable(false);
        etCurrency.setClickable(true);

        String[] currencyOptions = {"₺", "$", "€", "£"};
        etCurrency.setOnClickListener(v -> {
            String currText = etCurrency.getText().toString();
            int selIdx = 0;
            for (int i = 0; i < currencyOptions.length; i++) {
                if (currencyOptions[i].equalsIgnoreCase(currText)) {
                    selIdx = i;
                    break;
                }
            }
            new AlertDialog.Builder(context)
                    .setTitle("Select Currency")
                    .setSingleChoiceItems(currencyOptions, selIdx, (dialog, which) -> {
                        etCurrency.setText(currencyOptions[which]);
                        dialog.dismiss();
                    })
                    .show();
        });
        dialogLayout.addView(etCurrency);

        // Ödeme Yöntemi Alanı
        EditText etPaymentMethod = new EditText(context);
        etPaymentMethod.setHint("Select Payment Method");
        etPaymentMethod.setText(subscription.getPaymentMethod());
        etPaymentMethod.setFocusable(false);
        etPaymentMethod.setClickable(true);

        String[] paymentOptions = {"Credit Card", "Debit Card", "Virtual Card", "Cash", "Bank Transfer"};
        etPaymentMethod.setOnClickListener(v -> {
            String pmText = etPaymentMethod.getText().toString();
            int selIdx = 0;
            for (int i = 0; i < paymentOptions.length; i++) {
                if (paymentOptions[i].equalsIgnoreCase(pmText)) {
                    selIdx = i;
                    break;
                }
            }
            new AlertDialog.Builder(context)
                    .setTitle("Select Payment Method")
                    .setSingleChoiceItems(paymentOptions, selIdx, (dialog, which) -> {
                        etPaymentMethod.setText(paymentOptions[which]);
                        dialog.dismiss();
                    })
                    .show();
        });
        dialogLayout.addView(etPaymentMethod);

        // Tarih Seçimi
        EditText etDate = new EditText(context);
        etDate.setHint("Select Date");
        etDate.setText(subscription.getDate());
        etDate.setFocusable(false);
        etDate.setClickable(true);
        etDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePicker = new DatePickerDialog(
                    context,
                    (dpView, selectedYear, selectedMonth, selectedDay) -> {
                        String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                        etDate.setText(selectedDate);
                    },
                    year, month, day
            );
            datePicker.show();
        });
        dialogLayout.addView(etDate);

        // Kategori Seçimi (Özel Kategori Ekleme Destekli)
        EditText etCategory = new EditText(context);
        etCategory.setHint("Select Category");
        etCategory.setText(subscription.getCategory());
        etCategory.setFocusable(false);
        etCategory.setClickable(true);

        List<String> categoryList = CategoryStore.getCategories(
                context, Collections.singletonList(subscription.getCategory()));
        categoryList.add("➕ Add Custom Category...");

        etCategory.setOnClickListener(v -> {
            String[] categoryOptions = categoryList.toArray(new String[0]);
            String catText = etCategory.getText().toString();
            int selIdx = 0;
            for (int i = 0; i < categoryOptions.length; i++) {
                if (categoryOptions[i].equalsIgnoreCase(catText)) {
                    selIdx = i;
                    break;
                }
            }
            new AlertDialog.Builder(context)
                    .setTitle("Select Category")
                    .setItems(categoryOptions, (dialog, which) -> {
                        if (which == categoryOptions.length - 1 && categoryOptions[which].equals("➕ Add Custom Category...")) {
                            EditText customInput = new EditText(context);
                            customInput.setHint("Enter custom category name");
                            new AlertDialog.Builder(context)
                                    .setTitle("Add Custom Category")
                                    .setView(customInput)
                                    .setPositiveButton("Add", (d, w) -> {
                                        String newCat = customInput.getText().toString().trim();
                                        if (!newCat.isEmpty()) {
                                            CategoryStore.addCategory(context, newCat);
                                            boolean categoryExists = false;
                                            for (String existingCategory : categoryList) {
                                                if (existingCategory.equalsIgnoreCase(newCat)) {
                                                    categoryExists = true;
                                                    break;
                                                }
                                            }
                                            if (!categoryExists) {
                                                categoryList.add(categoryList.size() - 1, newCat);
                                            }
                                            etCategory.setText(newCat);
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        } else {
                            etCategory.setText(categoryOptions[which]);
                        }
                    })
                    .show();
        });
        dialogLayout.addView(etCategory);

        // Ödeme Periyodu Seçimi
        EditText etBillingCycle = new EditText(context);
        etBillingCycle.setHint("Select Billing Cycle");
        etBillingCycle.setText(subscription.getBillingCycle());
        etBillingCycle.setFocusable(false);
        etBillingCycle.setClickable(true);

        String[] cycleOptions = {"7 Days", "14 Days", "1 Month", "3 Months", "6 Months", "Yearly"};
        etBillingCycle.setOnClickListener(v -> {
            String cycleText = etBillingCycle.getText().toString();
            int selIdx = 2;
            for (int i = 0; i < cycleOptions.length; i++) {
                if (cycleOptions[i].equalsIgnoreCase(cycleText)) {
                    selIdx = i;
                    break;
                }
            }
            new AlertDialog.Builder(context)
                    .setTitle("Select Billing Cycle")
                    .setSingleChoiceItems(cycleOptions, selIdx, (dialog, which) -> {
                        etBillingCycle.setText(cycleOptions[which]);
                        dialog.dismiss();
                    })
                    .show();
        });
        dialogLayout.addView(etBillingCycle);

        EditText etNotes = new EditText(context);
        etNotes.setHint("Add note (optional)");
        etNotes.setText(subscription.getNotes());
        dialogLayout.addView(etNotes);

        new AlertDialog.Builder(context)
                .setTitle("Edit Subscription")
                .setView(scrollView)
                .setPositiveButton("Save", (dialog, which) -> {
                    subscription.setName(etName.getText().toString());
                    subscription.setAmount(etAmount.getText().toString());
                    subscription.setCurrency(etCurrency.getText().toString());
                    subscription.setPaymentMethod(etPaymentMethod.getText().toString());
                    subscription.setDate(etDate.getText().toString());
                    subscription.setCategory(etCategory.getText().toString());
                    CategoryStore.addCategory(context, subscription.getCategory());
                    subscription.setBillingCycle(etBillingCycle.getText().toString());
                    subscription.setNotes(etNotes.getText().toString());

                    executor.execute(() -> {
                        db.aboneDao().guncelle(subscription);
                        ((Activity) context).runOnUiThread(() -> {
                            notifyItemChanged(position);

                            if (onDataChangedListener != null) {
                                onDataChangedListener.run();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return subscriptionList.size();
    }

    public static class AboneViewHolder extends RecyclerView.ViewHolder {
        TextView tvIsim;
        TextView tvTutar;
        TextView tvPaymentMethod;
        TextView tvLogoInitials;
        ImageView ivBrandLogo;

        public AboneViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIsim = itemView.findViewById(R.id.tvIsim);
            tvTutar = itemView.findViewById(R.id.tvTutar);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
            tvLogoInitials = itemView.findViewById(R.id.tvLogoInitials);
            ivBrandLogo = itemView.findViewById(R.id.ivBrandLogo);
        }
    }
}