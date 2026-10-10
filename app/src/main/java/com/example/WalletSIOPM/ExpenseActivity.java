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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpenseActivity extends SecureActivity {

    private AboneDatabase db;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private ExpenseAdapter adapter;
    private List<Expense> expenseList = new ArrayList<>();
    private RecyclerView rvExpenses;
    private TextView tvTotalExpense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        int themeMode = prefs.getInt(SettingsActivity.PREF_THEME_MODE, 0);
        SettingsActivity.applyTheme(themeMode);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_expense);

        db = AboneDatabase.getInstance(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainExpense), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvExpenses = findViewById(R.id.rvExpenses);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);

        ImageButton btnAddExpense = findViewById(R.id.btnAddExpense);
        btnAddExpense.setOnClickListener(v -> showAddExpenseDialog());

        setupBottomNav();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(ExpenseActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        adapter = new ExpenseAdapter(expenseList, db, executor, this::loadExpenses);
        rvExpenses.setAdapter(adapter);
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));

        loadExpenses();
    }

    private void setupBottomNav() {
        NavigationHelper.setup(this, R.id.nav_expenses);
    }

    private void loadExpenses() {
        executor.execute(() -> {
            List<Expense> expenses = db.expenseDao().tumGiderleriGetir();

            SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
            String defaultCurrency = prefs.getString("default_currency", "₺");

            double total = 0.0;
            if (expenses != null) {
                for (Expense exp : expenses) {
                    try {
                        double amount = Double.parseDouble(exp.getAmount().replace(",", "."));
                        String expCurrency = exp.getCurrency() != null ? exp.getCurrency() : "₺";
                        total += convertCurrency(amount, expCurrency, defaultCurrency);
                    } catch (Exception ignored) {}
                }
            }

            final double finalTotal = total;

            runOnUiThread(() -> {
                expenseList.clear();
                if (expenses != null) {
                    expenseList.addAll(expenses);
                }
                adapter.notifyDataSetChanged();

                if (tvTotalExpense != null) {
                    tvTotalExpense.setText(String.format(Locale.getDefault(), "%s%.2f", defaultCurrency, finalTotal));
                }
            });
        });
    }

    private double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency == null || fromCurrency.isEmpty()) fromCurrency = "₺";
        if (toCurrency == null || toCurrency.isEmpty()) toCurrency = "₺";

        if (fromCurrency.equals(toCurrency)) return amount;

        double fromRate = CurrencyExchangeManager.getRateToTRY(this, fromCurrency);
        double toRate = CurrencyExchangeManager.getRateToTRY(this, toCurrency);

        return (amount * fromRate) / toRate;
    }

    private void showAddExpenseDialog() {
        SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        String defaultCurrency = prefs.getString("default_currency", "₺");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(50, 40, 50, 10);
        scrollView.addView(dialogLayout);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Expense Title (e.g. Grocery, Transfer)");
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
            String currText = etCurrency.getText().toString();
            int selIdx = 0;
            for (int i = 0; i < currencyOptions.length; i++) {
                if (currencyOptions[i].equalsIgnoreCase(currText)) {
                    selIdx = i;
                    break;
                }
            }
            new AlertDialog.Builder(this)
                    .setTitle("Select Currency")
                    .setSingleChoiceItems(currencyOptions, selIdx, (dialog, which) -> {
                        etCurrency.setText(currencyOptions[which]);
                        dialog.dismiss();
                    })
                    .show();
        });
        dialogLayout.addView(etCurrency);

        EditText etPaymentMethod = new EditText(this);
        etPaymentMethod.setHint("Payment Method");
        etPaymentMethod.setText("Credit Card");
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
            new AlertDialog.Builder(this)
                    .setTitle("Select Payment Method")
                    .setSingleChoiceItems(paymentOptions, selIdx, (dialog, which) -> {
                        etPaymentMethod.setText(paymentOptions[which]);
                        dialog.dismiss();
                    })
                    .show();
        });
        dialogLayout.addView(etPaymentMethod);

        EditText etDate = new EditText(this);
        etDate.setHint("Date (Tap to select)");
        etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", Calendar.getInstance().get(Calendar.DAY_OF_MONTH), Calendar.getInstance().get(Calendar.MONTH) + 1, Calendar.getInstance().get(Calendar.YEAR)));
        etDate.setFocusable(false);
        etDate.setClickable(true);
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        dialogLayout.addView(etDate);

        EditText etCategory = new EditText(this);
        etCategory.setHint("Category");
        etCategory.setText("Food & Grocery");
        etCategory.setFocusable(false);
        etCategory.setClickable(true);

        String[] categoryOptions = {"Food & Grocery", "Shopping", "Bills & Utilities", "Transport", "Entertainment", "Other"};
        etCategory.setOnClickListener(v -> {
            String catText = etCategory.getText().toString();
            int selIdx = 0;
            for (int i = 0; i < categoryOptions.length; i++) {
                if (categoryOptions[i].equalsIgnoreCase(catText)) {
                    selIdx = i;
                    break;
                }
            }
            new AlertDialog.Builder(this)
                    .setTitle("Select Category")
                    .setSingleChoiceItems(categoryOptions, selIdx, (dialog, which) -> {
                        etCategory.setText(categoryOptions[which]);
                        dialog.dismiss();
                    })
                    .show();
        });
        dialogLayout.addView(etCategory);

        EditText etNotes = new EditText(this);
        etNotes.setHint("Note (optional)");
        dialogLayout.addView(etNotes);

        new AlertDialog.Builder(this)
                .setTitle("Add One-off Expense")
                .setView(scrollView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String title = etTitle.getText().toString();
                    String amount = etAmount.getText().toString();
                    String currency = etCurrency.getText().toString();
                    String paymentMethod = etPaymentMethod.getText().toString();
                    String date = etDate.getText().toString();
                    String category = etCategory.getText().toString();
                    String notes = etNotes.getText().toString();

                    if (!title.isEmpty() && !amount.isEmpty() && !date.isEmpty()) {
                        executor.execute(() -> {
                            db.expenseDao().ekle(new Expense(title, amount, currency, date, category, paymentMethod, notes));
                            runOnUiThread(this::loadExpenses);
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavigationHelper.setup(this, R.id.nav_expenses);
        loadExpenses();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}