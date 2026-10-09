package com.example.subscriptiontracker;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private AboneDatabase db;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private AboneAdapter adapter;
    private List<Abonelik> allSubscriptionList = new ArrayList<>();
    private List<Abonelik> filteredList = new ArrayList<>();
    private RecyclerView rvAbonelikler;
    private boolean isAuthenticated = false;

    private EditText etSearch;
    private ChipGroup chipGroupCategory;
    private ChipGroup chipGroupPayment;
    private ImageButton btnSort;

    private String currentQuery = "";
    private String selectedCategory = "All";
    private String selectedPaymentMethod = "All";
    private int currentSortType = 0; // 0: Default, 1: Price High-Low, 2: Price Low-High, 3: Name A-Z

    private void loadData() {
        executor.execute(() -> {
            List<Abonelik> fetchedList = db.aboneDao().tumunuGetir();
            runOnUiThread(() -> {
                allSubscriptionList.clear();
                allSubscriptionList.addAll(fetchedList);
                applyFilterAndSort();
                updateSummaryCard(fetchedList);
            });
        });
    }

    private void applyFilterAndSort() {
        filteredList.clear();

        for (Abonelik sub : allSubscriptionList) {
            boolean matchesName = sub.getName() != null && sub.getName().toLowerCase().contains(currentQuery.toLowerCase());
            boolean matchesCategory = selectedCategory.equals("All") || (sub.getCategory() != null && sub.getCategory().equalsIgnoreCase(selectedCategory));
            boolean matchesPayment = selectedPaymentMethod.equals("All") || (sub.getPaymentMethod() != null && sub.getPaymentMethod().equalsIgnoreCase(selectedPaymentMethod));

            if (matchesName && matchesCategory && matchesPayment) {
                filteredList.add(sub);
            }
        }

        // Sıralama Mantığı
        if (currentSortType == 1) { // Fiyata göre (En yüksek -> En düşük)
            Collections.sort(filteredList, (a, b) -> Double.compare(parseAmount(b.getAmount()), parseAmount(a.getAmount())));
        } else if (currentSortType == 2) { // Fiyata göre (En düşük -> En yüksek)
            Collections.sort(filteredList, (a, b) -> Double.compare(parseAmount(a.getAmount()), parseAmount(b.getAmount())));
        } else if (currentSortType == 3) { // İsme göre (A -> Z)
            Collections.sort(filteredList, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        }

        adapter.notifyDataSetChanged();
    }

    private double parseAmount(String amountStr) {
        try {
            return Double.parseDouble(amountStr.replace(",", "."));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void showSortDialog() {
        String[] options = {"Default (Addition Date)", "Price: High to Low", "Price: Low to High", "Name: A to Z"};
        new AlertDialog.Builder(this)
                .setTitle("Sort Subscriptions")
                .setSingleChoiceItems(options, currentSortType, (dialog, which) -> {
                    currentSortType = which;
                    applyFilterAndSort();
                    dialog.dismiss();
                })
                .show();
    }

    private void checkBiometricAuth() {
        SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        boolean isLockEnabled = prefs.getBoolean(SettingsActivity.KEY_APP_LOCK_ENABLED, false);

        if (isLockEnabled && !isAuthenticated) {
            Executor mainExecutor = ContextCompat.getMainExecutor(this);
            BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this, mainExecutor,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            isAuthenticated = true;
                            View mainView = findViewById(R.id.main);
                            if (mainView != null) {
                                mainView.setVisibility(View.VISIBLE);
                            }
                            loadData();
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            finish();
                        }
                    });

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Subscription Tracker Lock")
                    .setSubtitle("Authenticate to view your subscriptions")
                    .setNegativeButtonText("Cancel")
                    .build();

            biometricPrompt.authenticate(promptInfo);
        }
    }

    private void updateSummaryCard(List<Abonelik> list) {
        SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        String defaultCurrency = prefs.getString("default_currency", "₺");
        float budgetLimit = prefs.getFloat(SettingsActivity.KEY_BUDGET_LIMIT, 0f);

        double totalCostInDefaultCurrency = 0.0;

        for (Abonelik a : list) {
            try {
                String amountStr = a.getAmount() != null ? a.getAmount().replace(",", ".") : "0";
                double amount = Double.parseDouble(amountStr);
                String subCurrency = (a.getCurrency() != null && !a.getCurrency().isEmpty()) ? a.getCurrency() : "₺";

                double convertedAmount = convertCurrency(amount, subCurrency, defaultCurrency);
                totalCostInDefaultCurrency += convertedAmount;
            } catch (Exception ignored) {
            }
        }

        TextView tvAylikToplam = findViewById(R.id.tvAylikToplam);
        TextView tvToplamAbonelik = findViewById(R.id.tvToplamAbonelik);
        TextView tvYaklasanOdeme = findViewById(R.id.tvYaklasanOdeme);

        if (tvAylikToplam != null) {
            tvAylikToplam.setText(String.format(Locale.getDefault(), "%s%.2f", defaultCurrency, totalCostInDefaultCurrency));

            if (budgetLimit > 0 && totalCostInDefaultCurrency > budgetLimit) {
                tvAylikToplam.setTextColor(Color.parseColor("#FF6B6B"));
            } else {
                tvAylikToplam.setTextColor(Color.parseColor("#F0F0F5"));
            }
        }

        if (tvToplamAbonelik != null) {
            if (budgetLimit > 0 && totalCostInDefaultCurrency > budgetLimit) {
                tvToplamAbonelik.setText(String.format(Locale.getDefault(), "⚠️️ Budget Exceeded! (Limit: %s%.0f)", defaultCurrency, budgetLimit));
                tvToplamAbonelik.setTextColor(Color.parseColor("#FF6B6B"));
            } else {
                tvToplamAbonelik.setText(list.size() + " Active Subscriptions");
                tvToplamAbonelik.setTextColor(Color.parseColor("#A0A0B5"));
            }
        }

        if (tvYaklasanOdeme != null) {
            tvYaklasanOdeme.setText(!list.isEmpty() ? "First Added: " + list.get(0).getName() : "Next: -");
        }

        ProgressBar progressBar = findViewById(R.id.budgetProgressBar);
        if (progressBar != null) {
            if (budgetLimit > 0) {
                progressBar.setVisibility(View.VISIBLE);
                int progress = (int) ((totalCostInDefaultCurrency / budgetLimit) * 100);
                progressBar.setProgress(Math.min(100, progress));
                if (progress > 100) {
                    progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#FF5252")));
                } else if (progress > 75) {
                    progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#FFA726")));
                } else {
                    progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#4CD964")));
                }
            } else {
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    private double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency == null || fromCurrency.isEmpty()) fromCurrency = "₺";
        if (toCurrency == null || toCurrency.isEmpty()) toCurrency = "₺";

        if (fromCurrency.equals(toCurrency)) return amount;

        double fromRate = CurrencyExchangeManager.getRateToTRY(this, fromCurrency);
        double toRate = CurrencyExchangeManager.getRateToTRY(this, toCurrency);

        return (amount * fromRate) / toRate;
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_subscriptions); // Ana ekrandayız

            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_subscriptions) {
                    return true; // Zaten Ana ekrandayız
                } else if (id == R.id.nav_income) {
                    startActivity(new Intent(MainActivity.this, IncomeActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_expenses) {
                    startActivity(new Intent(MainActivity.this, ExpenseActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_analytics) {
                    // Analiz sayfasına geçiş
                    startActivity(new Intent(MainActivity.this, AnalyticsActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        int themeMode = prefs.getInt(SettingsActivity.KEY_THEME_MODE, 0);
        SettingsActivity.applyTheme(themeMode);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        db = AboneDatabase.getInstance(this);

        View mainView = findViewById(R.id.main);
        boolean isLockEnabled = prefs.getBoolean(SettingsActivity.KEY_APP_LOCK_ENABLED, false);

        if (isLockEnabled) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            if (mainView != null) mainView.setVisibility(View.INVISIBLE);
            checkBiometricAuth();
        } else {
            loadData();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                NotificationWorker.class,
                24, TimeUnit.HOURS
        ).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "SubscriptionReminderWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );

        rvAbonelikler = findViewById(R.id.rvAbonelikler);
        etSearch = findViewById(R.id.etSearch);
        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        btnSort = findViewById(R.id.btnSort);



        // Arama Kutusu Dinleyicisi
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                applyFilterAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Kategori Chip Seçim Dinleyicisi
        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip selectedChip = group.findViewById(checkedIds.get(0));
                if (selectedChip != null) {
                    selectedCategory = selectedChip.getText().toString();
                }
            } else {
                selectedCategory = "All";
            }
            applyFilterAndSort();
        });

        // Ödeme Yöntemi Chip Seçim Dinleyicisi
        chipGroupPayment = findViewById(R.id.chipGroupPayment);
        chipGroupPayment.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip selectedChip = group.findViewById(checkedIds.get(0));
                if (selectedChip != null) {
                    selectedPaymentMethod = selectedChip.getText().toString();
                    if (selectedPaymentMethod.equals("All Methods")) {
                        selectedPaymentMethod = "All";
                    }
                }
            } else {
                selectedPaymentMethod = "All";
            }
            applyFilterAndSort();
        });

        // Sıralama Butonu Tıklaması
        btnSort.setOnClickListener(v -> showSortDialog());

        ImageButton btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> showAddSubscriptionDialog());
        }

        setupBottomNav();

        // Adapter'ı filtrelenmiş liste (`filteredList`) ile bağla
        adapter = new AboneAdapter(filteredList, db, executor, this::loadData);
        rvAbonelikler.setAdapter(adapter);
        rvAbonelikler.setLayoutManager(new LinearLayoutManager(this));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_subscriptions);
        }

        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int themeMode = sharedPreferences.getInt(SettingsActivity.KEY_THEME_MODE, 0);
        SettingsActivity.applyTheme(themeMode);

        CurrencyExchangeManager.fetchLatestRates(this, null);
        loadData();
    }

    private void showAddSubscriptionDialog() {
        SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        String defaultCurrency = prefs.getString("default_currency", "₺");

        ScrollView scrollView = new ScrollView(MainActivity.this);
        LinearLayout dialogLayout = new LinearLayout(MainActivity.this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(50, 40, 50, 10);
        scrollView.addView(dialogLayout);

        EditText etName = new EditText(MainActivity.this);
        etName.setHint("Subscription Name");
        dialogLayout.addView(etName);

        EditText etAmount = new EditText(MainActivity.this);
        etAmount.setHint("Amount");
        dialogLayout.addView(etAmount);

        EditText etCurrency = new EditText(MainActivity.this);
        etCurrency.setHint("Select Currency (Tap to select)");
        etCurrency.setText(defaultCurrency);
        etCurrency.setFocusable(false);
        etCurrency.setClickable(true);

        String[] currencyOptions = {"₺", "$", "€", "£"};
        etCurrency.setOnClickListener(view -> {
            String currentCurrency = etCurrency.getText().toString();
            int selectedIndex = 0;
            for (int i = 0; i < currencyOptions.length; i++) {
                if (currencyOptions[i].equalsIgnoreCase(currentCurrency)) {
                    selectedIndex = i;
                    break;
                }
            }
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Select Currency")
                    .setSingleChoiceItems(currencyOptions, selectedIndex, (dialog, which) -> {
                        etCurrency.setText(currencyOptions[which]);
                        dialog.dismiss();
                    })
                    .show();
        });
        dialogLayout.addView(etCurrency);

        EditText etDate = new EditText(MainActivity.this);
        etDate.setHint("Start Date (Tap to select)");
        etDate.setFocusable(false);
        etDate.setClickable(true);

        etDate.setOnClickListener(view -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePicker = new DatePickerDialog(
                    MainActivity.this,
                    (dpView, selectedYear, selectedMonth, selectedDay) -> {
                        String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                        etDate.setText(selectedDate);
                    },
                    year, month, day
            );
            datePicker.show();
        });
        dialogLayout.addView(etDate);

        EditText etBillingCycle = new EditText(MainActivity.this);
        etBillingCycle.setHint("Select Billing Cycle (Tap to select)");
        etBillingCycle.setFocusable(false);
        etBillingCycle.setClickable(true);

        String[] cycleOptions = {"7 Days", "14 Days", "1 Month", "3 Months", "6 Months", "Yearly"};
        etBillingCycle.setOnClickListener(view -> {
            String currentCycle = etBillingCycle.getText().toString();
            int selectedIndex = 2; // Default 1 Month
            for (int i = 0; i < cycleOptions.length; i++) {
                if (cycleOptions[i].equalsIgnoreCase(currentCycle)) {
                    selectedIndex = i;
                    break;
                }
            }
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Select Billing Cycle")
                    .setSingleChoiceItems(cycleOptions, selectedIndex, (dialog, which) -> {
                        etBillingCycle.setText(cycleOptions[which]);
                        dialog.dismiss();
                    })
                    .show();
        });
        dialogLayout.addView(etBillingCycle);

        EditText etCategory = new EditText(MainActivity.this);
        etCategory.setHint("Select Category (Tap to select)");
        etCategory.setFocusable(false);
        etCategory.setClickable(true);

        List<String> categoryList = new ArrayList<>(Arrays.asList(
                "Music", "Movies & TV", "Software & Cloud", "Gaming",
                "Education & Books", "Sports & Fitness", "Other", "➕ Add Custom Category..."
        ));

        etCategory.setOnClickListener(view -> {
            String[] categoryOptions = categoryList.toArray(new String[0]);
            String currentCat = etCategory.getText().toString();
            int selectedIndex = 0;
            for (int i = 0; i < categoryOptions.length; i++) {
                if (categoryOptions[i].equalsIgnoreCase(currentCat)) {
                    selectedIndex = i;
                    break;
                }
            }
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Select Category")
                    .setItems(categoryOptions, (dialog, which) -> {
                        if (which == categoryOptions.length - 1 && categoryOptions[which].equals("➕ Add Custom Category...")) {
                            EditText customInput = new EditText(MainActivity.this);
                            customInput.setHint("Enter custom category name");
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Add Custom Category")
                                    .setView(customInput)
                                    .setPositiveButton("Add", (d, w) -> {
                                        String newCat = customInput.getText().toString().trim();
                                        if (!newCat.isEmpty()) {
                                            categoryList.add(categoryList.size() - 1, newCat);
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

        EditText etPaymentMethod = new EditText(MainActivity.this);
        etPaymentMethod.setHint("Select Payment Method (Tap to select)");
        etPaymentMethod.setText("Credit Card");
        etPaymentMethod.setFocusable(false);
        etPaymentMethod.setClickable(true);

        String[] paymentOptions = {"Credit Card", "Debit Card", "Virtual Card", "Cash", "Bank Transfer"};
        etPaymentMethod.setOnClickListener(view -> {
            String currentPm = etPaymentMethod.getText().toString();
            int selectedIndex = 0;
            for (int i = 0; i < paymentOptions.length; i++) {
                if (paymentOptions[i].equalsIgnoreCase(currentPm)) {
                    selectedIndex = i;
                    break;
                }
            }
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Select Payment Method")
                    .setSingleChoiceItems(paymentOptions, selectedIndex, (dialog, which) -> {
                        etPaymentMethod.setText(paymentOptions[which]);
                        dialog.dismiss();
                    })
                    .show();
        });
        dialogLayout.addView(etPaymentMethod);

        EditText etNotes = new EditText(MainActivity.this);
        etNotes.setHint("Add note (optional)");
        dialogLayout.addView(etNotes);

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Add Subscription")
                .setView(scrollView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString();
                    String amount = etAmount.getText().toString();
                    String currency = etCurrency.getText().toString();
                    String date = etDate.getText().toString();
                    String billingCycle = etBillingCycle.getText().toString();
                    String category = etCategory.getText().toString();
                    String paymentMethod = etPaymentMethod.getText().toString();
                    String notes = etNotes.getText().toString();

                    if (!name.isEmpty() && !amount.isEmpty() && !date.isEmpty()) {
                        executor.execute(() -> {
                            db.aboneDao().ekle(new Abonelik(name, amount, date, category, notes, billingCycle, currency, paymentMethod));
                            runOnUiThread(() -> {
                                loadData();
                            });
                        });
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}