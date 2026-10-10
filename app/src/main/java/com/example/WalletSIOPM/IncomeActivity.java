package com.example.WalletSIOPM;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IncomeActivity extends SecureActivity {

    private AboneDatabase db;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private TransactionAdapter adapter;
    private List<TransactionItem> transactionList = new ArrayList<>();
    private RecyclerView rvTransactions;
    private TextView tvTotalBalance;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        int themeMode = prefs.getInt(SettingsActivity.PREF_THEME_MODE, 0);
        SettingsActivity.applyTheme(themeMode);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_income);

        db = AboneDatabase.getInstance(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainIncome), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvTransactions = findViewById(R.id.rvIncomes);
        tvTotalBalance = findViewById(R.id.tvTotalIncome);

        ImageButton btnAddIncome = findViewById(R.id.btnAddIncome);
        btnAddIncome.setOnClickListener(v -> showAddIncomeDialog());

        setupBottomNav();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(IncomeActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        adapter = new TransactionAdapter(transactionList);
        rvTransactions.setAdapter(adapter);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));

        loadTransactions();
    }



    private void setupBottomNav() {
        NavigationHelper.setup(this, R.id.nav_income);
    }



    private void loadTransactions() {
        executor.execute(() -> {
            List<Income> incomes = db.incomeDao().tumGelirleriGetir();

            List<TransactionItem> incomeItems = new ArrayList<>();
            double totalIncome = 0.0;

            SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
            String defaultCurrency = prefs.getString("default_currency", "₺");

            if (incomes != null) {
                for (Income inc : incomes) {
                    incomeItems.add(new TransactionItem(inc.getId(), inc.getTitle(), inc.getAmount(), inc.getCurrency(), inc.getDate(), "INCOME"));
                    try {
                        double amount = Double.parseDouble(inc.getAmount().replace(",", "."));
                        String incCurr = inc.getCurrency() != null ? inc.getCurrency() : "₺";
                        totalIncome += convertCurrency(amount, incCurr, defaultCurrency);
                    } catch (Exception ignored) {}
                }
            }

            Collections.sort(incomeItems);

            final double finalTotalIncome = totalIncome;

            runOnUiThread(() -> {
                transactionList.clear();
                transactionList.addAll(incomeItems);
                adapter.notifyDataSetChanged();

                if (tvTotalBalance != null) {
                    tvTotalBalance.setText(String.format(Locale.getDefault(), "%s%.2f", defaultCurrency, finalTotalTotalIncomeFix(finalTotalIncome)));
                }
            });
        });
    }

    private double finalTotalTotalIncomeFix(double val) {
        return val;
    }

    private double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency == null || fromCurrency.isEmpty()) fromCurrency = "₺";
        if (toCurrency == null || toCurrency.isEmpty()) toCurrency = "₺";

        if (fromCurrency.equals(toCurrency)) return amount;

        double fromRate = CurrencyExchangeManager.getRateToTRY(this, fromCurrency);
        double toRate = CurrencyExchangeManager.getRateToTRY(this, toCurrency);

        return (amount * fromRate) / toRate;
    }

    private void showAddIncomeDialog() {
        SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        String defaultCurrency = prefs.getString("default_currency", "₺");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(50, 40, 50, 10);
        scrollView.addView(dialogLayout);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Income Title (e.g. Salary)");
        dialogLayout.addView(etTitle);

        EditText etAmount = new EditText(this);
        etAmount.setHint("Amount");
        dialogLayout.addView(etAmount);

        EditText etCurrency = new EditText(this);
        etCurrency.setHint("Currency");
        etCurrency.setText(defaultCurrency);
        etCurrency.setFocusable(false);
        etCurrency.setClickable(true);

        String[] currencyOptions = {"₺", "$", "€", "£"};
        etCurrency.setOnClickListener(v -> {
            String currentCurrency = etCurrency.getText().toString();
            int selectedIndex = 0;
            for (int i = 0; i < currencyOptions.length; i++) {
                if (currencyOptions[i].equalsIgnoreCase(currentCurrency)) {
                    selectedIndex = i;
                    break;
                }
            }
            new AlertDialog.Builder(this)
                    .setTitle("Select Currency")
                    .setSingleChoiceItems(currencyOptions, selectedIndex, (dialog, which) -> {
                        etCurrency.setText(currencyOptions[which]);
                        dialog.dismiss();
                    })
                    .show();
        });
        dialogLayout.addView(etCurrency);

        EditText etDate = new EditText(this);
        etDate.setHint("Date (Tap to select)");
        etDate.setFocusable(false);
        etDate.setClickable(true);
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        dialogLayout.addView(etDate);

        EditText etNotes = new EditText(this);
        etNotes.setHint("Note (optional)");
        dialogLayout.addView(etNotes);

        new AlertDialog.Builder(this)
                .setTitle("Add Income")
                .setView(scrollView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String title = etTitle.getText().toString();
                    String amount = etAmount.getText().toString();
                    String currency = etCurrency.getText().toString();
                    String date = etDate.getText().toString();
                    String notes = etNotes.getText().toString();

                    if (!title.isEmpty() && !amount.isEmpty() && !date.isEmpty()) {
                        executor.execute(() -> {
                            db.incomeDao().ekle(new Income(title, amount, currency, date, notes));
                            runOnUiThread(this::loadTransactions);
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavigationHelper.setup(this, R.id.nav_income);
        loadTransactions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}