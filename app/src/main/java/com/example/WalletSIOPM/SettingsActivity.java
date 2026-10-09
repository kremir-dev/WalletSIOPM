package com.example.subscriptiontracker;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;

import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private TextView tvReminderDaysSub;
    private TextView tvReminderTimeSub;
    private TextView tvDefaultCurrencySub;
    private TextView tvBudgetLimitSub;
    private TextView tvAppLockSub;
    private TextView tvThemeSub;
    private TextView tvCurrencyRatesSub;

    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_REMINDER_DAYS = "reminder_days";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";
    private static final String KEY_DEFAULT_CURRENCY = "default_currency";
    public static final String KEY_BUDGET_LIMIT = "budget_limit";
    public static final String KEY_APP_LOCK_ENABLED = "app_lock_enabled";
    public static final String KEY_THEME_MODE = "theme_mode"; // 0: System, 1: Light, 2: Dark

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void applyTheme(int themeMode) {
        if (themeMode == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (themeMode == 2) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        ImageButton btnBack = findViewById(R.id.btnBack);
        MaterialCardView btnNotificationSettings = findViewById(R.id.btnNotificationSettings);
        MaterialCardView btnReminderDays = findViewById(R.id.btnReminderDays);
        MaterialCardView btnReminderTime = findViewById(R.id.btnReminderTime);
        MaterialCardView btnDefaultCurrency = findViewById(R.id.btnDefaultCurrency);
        MaterialCardView btnCurrencyRates = findViewById(R.id.btnCurrencyRates);
        MaterialCardView btnCurrencyConverter = findViewById(R.id.btnCurrencyConverter);
        MaterialCardView btnExportCsv = findViewById(R.id.btnExportCsv);
        MaterialCardView btnBudgetLimit = findViewById(R.id.btnBudgetLimit);
        MaterialCardView btnCategoryBudgets = findViewById(R.id.btnCategoryBudgets);
        MaterialCardView btnAppLock = findViewById(R.id.btnAppLock);
        MaterialCardView btnThemeSettings = findViewById(R.id.btnThemeSettings);
        MaterialCardView btnGithubRepo = findViewById(R.id.btnGithubRepo);
        MaterialCardView btnSendFeedback = findViewById(R.id.btnSendFeedback);
        MaterialCardView btnResetData = findViewById(R.id.btnResetData);
        MaterialCardView btnAboutSettings = findViewById(R.id.btnAboutSettings);

        tvReminderDaysSub = findViewById(R.id.tvReminderDaysSub);
        tvReminderTimeSub = findViewById(R.id.tvReminderTimeSub);
        tvDefaultCurrencySub = findViewById(R.id.tvDefaultCurrencySub);
        tvCurrencyRatesSub = findViewById(R.id.tvCurrencyRatesSub);
        tvBudgetLimitSub = findViewById(R.id.tvBudgetLimitSub);
        tvAppLockSub = findViewById(R.id.tvAppLockSub);
        tvThemeSub = findViewById(R.id.tvThemeSub);

        loadSavedSettings();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isTaskRoot()) {
                    Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
                finish();
            }
        });

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // 1. Sistem Bildirim Ayarları
        btnNotificationSettings.setOnClickListener(v -> {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
            }
            startActivity(intent);
        });

        // 2. Hatırlatma Günü Seçimi
        btnReminderDays.setOnClickListener(v -> {
            String[] options = {"Same Day", "1 Day Before", "2 Days Before", "3 Days Before", "1 Week Before"};
            String current = sharedPreferences.getString(KEY_REMINDER_DAYS, "1 Day Before");
            int selectedIndex = 1;
            for (int i = 0; i < options.length; i++) {
                if (options[i].equalsIgnoreCase(current)) {
                    selectedIndex = i;
                    break;
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle("Select Default Reminder Day")
                    .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                        sharedPreferences.edit().putString(KEY_REMINDER_DAYS, options[which]).apply();
                        tvReminderDaysSub.setText(options[which]);
                        dialog.dismiss();
                    })
                    .show();
        });

        // 3. Hatırlatma Saati Seçimi
        btnReminderTime.setOnClickListener(v -> {
            int currentHour = sharedPreferences.getInt(KEY_REMINDER_HOUR, 9);
            int currentMinute = sharedPreferences.getInt(KEY_REMINDER_MINUTE, 0);

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        sharedPreferences.edit()
                                .putInt(KEY_REMINDER_HOUR, hourOfDay)
                                .putInt(KEY_REMINDER_MINUTE, minute)
                                .apply();
                        updateTimeSubText(hourOfDay, minute);
                    },
                    currentHour,
                    currentMinute,
                    true
            );
            timePickerDialog.show();
        });

        // 4. Varsayılan Para Birimi Seçimi
        btnDefaultCurrency.setOnClickListener(v -> {
            String[] currencyLabels = {"₺ (TRY)", "$ (USD)", "€ (EUR)", "£ (GBP)", "₿ (Bitcoin)", "Ξ (Ethereum)", "USDT", "CAD", "AUD"};
            String[] currencySymbols = {"₺", "$", "€", "£", "₿", "Ξ", "USDT", "CAD", "AUD"};
            String currentSymbol = sharedPreferences.getString(KEY_DEFAULT_CURRENCY, "₺");
            int selectedIndex = 0;
            for (int i = 0; i < currencySymbols.length; i++) {
                if (currencySymbols[i].equalsIgnoreCase(currentSymbol)) {
                    selectedIndex = i;
                    break;
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle("Select Default Currency")
                    .setSingleChoiceItems(currencyLabels, selectedIndex, (dialog, which) -> {
                        String selectedSymbol = currencySymbols[which];
                        sharedPreferences.edit().putString(KEY_DEFAULT_CURRENCY, selectedSymbol).apply();
                        tvDefaultCurrencySub.setText(currencyLabels[which]);
                        dialog.dismiss();
                    })
                    .show();
        });

        // 4.5. Canlı Döviz Kurları Yenileme
        btnCurrencyRates.setOnClickListener(v -> {
            Toast.makeText(this, "Updating exchange rates...", Toast.LENGTH_SHORT).show();
            CurrencyExchangeManager.fetchLatestRates(this, success -> {
                if (success) {
                    Toast.makeText(this, "Exchange rates updated successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to update rates. Using cached/default rates.", Toast.LENGTH_SHORT).show();
                }
                tvCurrencyRatesSub.setText("Last updated: " + CurrencyExchangeManager.getLastUpdateTime(this));
            });
        });

        // 4.6. Para Birimi Çevirici Aracı
        btnCurrencyConverter.setOnClickListener(v -> {
            ScrollView cvScroll = new ScrollView(this);
            LinearLayout cvLayout = new LinearLayout(this);
            cvLayout.setOrientation(LinearLayout.VERTICAL);
            cvLayout.setPadding(50, 40, 50, 10);
            cvScroll.addView(cvLayout);

            EditText etAmount = new EditText(this);
            etAmount.setHint("Amount to convert");
            etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            cvLayout.addView(etAmount);

            String[] currencies = {"TRY", "USD", "EUR", "GBP", "BTC", "ETH", "USDT", "CAD", "AUD"};
            final String[] fromCurr = {"TRY"};
            final String[] toCurr = {"USD"};

            EditText etFrom = new EditText(this);
            etFrom.setHint("From Currency");
            etFrom.setText("TRY");
            etFrom.setFocusable(false);
            etFrom.setClickable(true);
            etFrom.setOnClickListener(view -> {
                new AlertDialog.Builder(this)
                        .setTitle("From Currency")
                        .setSingleChoiceItems(currencies, 0, (d, w) -> {
                            etFrom.setText(currencies[w]);
                            fromCurr[0] = currencies[w];
                            d.dismiss();
                        })
                        .show();
            });
            cvLayout.addView(etFrom);

            EditText etTo = new EditText(this);
            etTo.setHint("To Currency");
            etTo.setText("USD");
            etTo.setFocusable(false);
            etTo.setClickable(true);
            etTo.setOnClickListener(view -> {
                new AlertDialog.Builder(this)
                        .setTitle("To Currency")
                        .setSingleChoiceItems(currencies, 1, (d, w) -> {
                            etTo.setText(currencies[w]);
                            toCurr[0] = currencies[w];
                            d.dismiss();
                        })
                        .show();
            });
            cvLayout.addView(etTo);

            new AlertDialog.Builder(this)
                    .setTitle("Currency Converter")
                    .setView(cvScroll)
                    .setPositiveButton("Convert", (dialog, which) -> {
                        try {
                            double amount = Double.parseDouble(etAmount.getText().toString().replace(",", "."));
                            double amountInTRY = amount * CurrencyExchangeManager.getRateToTRY(this, fromCurr[0]);
                            double finalAmount = amountInTRY / CurrencyExchangeManager.getRateToTRY(this, toCurr[0]);

                            new AlertDialog.Builder(this)
                                    .setTitle("Conversion Result")
                                    .setMessage(String.format(Locale.getDefault(), "%.2f %s = %.2f %s", amount, fromCurr[0], finalAmount, toCurr[0]))
                                    .setPositiveButton("OK", null)
                                    .show();
                        } catch (Exception e) {
                            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // 4.7. CSV Raporu Dışa Aktar
        btnExportCsv.setOnClickListener(v -> {
            executor.execute(() -> {
                AboneDatabase db = AboneDatabase.getInstance(this);
                List<Abonelik> subs = db.aboneDao().tumunuGetir();
                List<Income> incomes = db.incomeDao().tumGelirleriGetir();
                List<Expense> expenses = db.expenseDao().tumGiderleriGetir();

                StringBuilder csv = new StringBuilder();
                csv.append("Type,Name/Title,Amount,Currency,Date,Category,PaymentMethod,BillingCycle,Notes\n");

                if (subs != null) {
                    for (Abonelik sub : subs) {
                        csv.append("Subscription,").append(escapeCsv(sub.getName())).append(",")
                                .append(sub.getAmount()).append(",").append(sub.getCurrency()).append(",")
                                .append(sub.getDate()).append(",").append(escapeCsv(sub.getCategory())).append(",")
                                .append(escapeCsv(sub.getPaymentMethod())).append(",")
                                .append(escapeCsv(sub.getBillingCycle())).append(",")
                                .append(escapeCsv(sub.getNotes())).append("\n");
                    }
                }

                if (incomes != null) {
                    for (Income inc : incomes) {
                        csv.append("Income,").append(escapeCsv(inc.getTitle())).append(",")
                                .append(inc.getAmount()).append(",").append(inc.getCurrency()).append(",")
                                .append(inc.getDate()).append(",-, -, -,").append(escapeCsv(inc.getNotes())).append("\n");
                    }
                }

                if (expenses != null) {
                    for (Expense exp : expenses) {
                        csv.append("OneoffExpense,").append(escapeCsv(exp.getTitle())).append(",")
                                .append(exp.getAmount()).append(",").append(exp.getCurrency()).append(",")
                                .append(exp.getDate()).append(",").append(escapeCsv(exp.getCategory())).append(",")
                                .append(exp.getPaymentMethod()).append(",-,")
                                .append(escapeCsv(exp.getNotes())).append("\n");
                    }
                }

                try {
                    File file = new File(getExternalCacheDir(), "SubscriptionTracker_Report.csv");
                    FileWriter writer = new FileWriter(file);
                    writer.write(csv.toString());
                    writer.close();

                    Uri uri = FileProvider.getUriForFile(
                            this,
                            getPackageName() + ".fileprovider",
                            file
                    );

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/csv");
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    runOnUiThread(() -> {
                        startActivity(Intent.createChooser(intent, "Export CSV Report via"));
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to export CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        // 5. Aylık Bütçe Limiti
        btnBudgetLimit.setOnClickListener(v -> {
            String currSymbol = sharedPreferences.getString(KEY_DEFAULT_CURRENCY, "₺");
            float currentLimit = sharedPreferences.getFloat(KEY_BUDGET_LIMIT, 0f);

            EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setHint("e.g. 1500 (Set 0 to remove)");
            if (currentLimit > 0) {
                input.setText(String.valueOf(currentLimit));
            }

            new AlertDialog.Builder(this)
                    .setTitle("Set Monthly Budget Limit")
                    .setMessage("Enter your target monthly spending limit in " + currSymbol + ":")
                    .setView(input)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String val = input.getText().toString().trim();
                        float limit = val.isEmpty() ? 0f : Float.parseFloat(val);
                        sharedPreferences.edit().putFloat(KEY_BUDGET_LIMIT, limit).apply();

                        if (limit > 0) {
                            tvBudgetLimitSub.setText(String.format(Locale.getDefault(), "%s%.2f", currSymbol, limit));
                        } else {
                            tvBudgetLimitSub.setText("Not set (Unlimited)");
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // 5.5. Kategori Bazlı Bütçe Limitleri (Dinamik + Özel Kategori Destekli)
        btnCategoryBudgets.setOnClickListener(v -> {
            executor.execute(() -> {
                AboneDatabase db = AboneDatabase.getInstance(this);
                List<Abonelik> subs = db.aboneDao().tumunuGetir();
                List<String> categoryList = new ArrayList<>(Arrays.asList("Music", "Movies & TV", "Software & Cloud", "Gaming", "Other"));

                if (subs != null) {
                    for (Abonelik sub : subs) {
                        String cat = sub.getCategory();
                        if (cat != null && !cat.isEmpty() && !categoryList.contains(cat)) {
                            categoryList.add(cat);
                        }
                    }
                }

                runOnUiThread(() -> {
                    ScrollView catScroll = new ScrollView(this);
                    LinearLayout catLayout = new LinearLayout(this);
                    catLayout.setOrientation(LinearLayout.VERTICAL);
                    catLayout.setPadding(50, 40, 50, 10);
                    catScroll.addView(catLayout);

                    EditText[] inputs = new EditText[categoryList.size()];
                    for (int i = 0; i < categoryList.size(); i++) {
                        String catName = categoryList.get(i);
                        TextView tv = new TextView(this);
                        tv.setText(catName + " Limit (" + sharedPreferences.getString(KEY_DEFAULT_CURRENCY, "₺") + "):");
                        catLayout.addView(tv);

                        inputs[i] = new EditText(this);
                        inputs[i].setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        String key = "cat_budget_" + catName.replaceAll("[^a-zA-Z0-9]", "_");
                        float curVal = sharedPreferences.getFloat(key, 0f);
                        if (curVal > 0) inputs[i].setText(String.valueOf(curVal));
                        catLayout.addView(inputs[i]);
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Category Budget Limits")
                            .setView(catScroll)
                            .setPositiveButton("Save", (dialog, which) -> {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                for (int i = 0; i < categoryList.size(); i++) {
                                    String catName = categoryList.get(i);
                                    String key = "cat_budget_" + catName.replaceAll("[^a-zA-Z0-9]", "_");
                                    String val = inputs[i].getText().toString().trim();
                                    float limit = val.isEmpty() ? 0f : Float.parseFloat(val);
                                    editor.putFloat(key, limit);
                                }
                                editor.apply();
                                Toast.makeText(this, "Category budgets saved!", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            });
        });

        // 6. Biyometrik / Uygulama Kilidi Aç-Kapa
        btnAppLock.setOnClickListener(v -> {
            boolean isEnabled = sharedPreferences.getBoolean(KEY_APP_LOCK_ENABLED, false);
            boolean newState = !isEnabled;

            sharedPreferences.edit().putBoolean(KEY_APP_LOCK_ENABLED, newState).apply();
            tvAppLockSub.setText(newState ? "Enabled (Active on launch)" : "Disabled");
            Toast.makeText(this, newState ? "App Lock Enabled" : "App Lock Disabled", Toast.LENGTH_SHORT).show();
        });

        // 7. Tema Ayarları
        btnThemeSettings.setOnClickListener(v -> {
            String[] themes = {"System Default", "Light Theme", "Dark Theme"};
            int currentThemeMode = sharedPreferences.getInt(KEY_THEME_MODE, 0);

            new AlertDialog.Builder(this)
                    .setTitle("Select Theme")
                    .setSingleChoiceItems(themes, currentThemeMode, (dialog, which) -> {
                        sharedPreferences.edit().putInt(KEY_THEME_MODE, which).apply();
                        if (which == 1) {
                            tvThemeSub.setText("Light Theme");
                        } else if (which == 2) {
                            tvThemeSub.setText("Dark Theme");
                        } else {
                            tvThemeSub.setText("System Default");
                        }
                        applyTheme(which);
                        dialog.dismiss();
                    })
                    .show();
        });

        // 8. GitHub Kaynak Kodu
        btnGithubRepo.setOnClickListener(v -> {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kremir-dev/WalletSIOPM"));
                startActivity(browserIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Could not open browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // 9. Geri Bildirim / E-posta Gönder
        btnSendFeedback.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"kremirdev@proton.me"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "WalletSIOPM - Feedback");
            try {
                startActivity(Intent.createChooser(emailIntent, "Send Feedback via Email"));
            } catch (Exception e) {
                Toast.makeText(this, "No email client found", Toast.LENGTH_SHORT).show();
            }
        });

        // 10. TÜM VERİLERİ SIFIRLA (DANGER ZONE)
        btnResetData.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Reset All Data")
                    .setMessage("Are you sure you want to delete ALL data (subscriptions, incomes, and settings)? This action cannot be undone.")
                    .setPositiveButton("Delete All", (dialog, which) -> {
                        executor.execute(() -> {
                            AboneDatabase db = AboneDatabase.getInstance(this);
                            db.aboneDao().tumunuSil();
                            db.incomeDao().tumunuSil();
                            db.expenseDao().tumunuSil();
                            sharedPreferences.edit().clear().apply();
                            runOnUiThread(() -> {
                                loadSavedSettings();
                                Toast.makeText(this, "All subscriptions, incomes, expenses, and settings have been deleted.", Toast.LENGTH_LONG).show();
                            });
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // 11. Hakkında Penceresi
        btnAboutSettings.setOnClickListener(v -> {
            String versionInfo = "1.0";
            try {
                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                versionInfo = pInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            String message = "Subscription Tracker\n" +
                    "Version: " + versionInfo + "\n\n" +
                    "Developer: Emir Kerem Türken\n\n" +
                    "Open Source & License Notice\n" +
                    "This application is open-source software under the GNU GPL v2.0.";

            new AlertDialog.Builder(this)
                    .setTitle("About")
                    .setMessage(message)
                    .setPositiveButton("Close", null)
                    .show();
        });
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }

    private void loadSavedSettings() {
        String days = sharedPreferences.getString(KEY_REMINDER_DAYS, "1 Day Before");
        int hour = sharedPreferences.getInt(KEY_REMINDER_HOUR, 9);
        int minute = sharedPreferences.getInt(KEY_REMINDER_MINUTE, 0);
        String currency = sharedPreferences.getString(KEY_DEFAULT_CURRENCY, "₺");
        float budgetLimit = sharedPreferences.getFloat(KEY_BUDGET_LIMIT, 0f);
        boolean appLock = sharedPreferences.getBoolean(KEY_APP_LOCK_ENABLED, false);
        int themeMode = sharedPreferences.getInt(KEY_THEME_MODE, 0);

        tvReminderDaysSub.setText(days);
        updateTimeSubText(hour, minute);

        if (currency.equals("$")) {
            tvDefaultCurrencySub.setText("$ (USD)");
        } else if (currency.equals("€")) {
            tvDefaultCurrencySub.setText("€ (EUR)");
        } else if (currency.equals("£")) {
            tvDefaultCurrencySub.setText("£ (GBP)");
        } else if (currency.equals("₿")) {
            tvDefaultCurrencySub.setText("₿ (Bitcoin)");
        } else if (currency.equals("Ξ")) {
            tvDefaultCurrencySub.setText("Ξ (Ethereum)");
        } else if (currency.equals("USDT")) {
            tvDefaultCurrencySub.setText("USDT");
        } else if (currency.equals("CAD")) {
            tvDefaultCurrencySub.setText("CAD");
        } else if (currency.equals("AUD")) {
            tvDefaultCurrencySub.setText("AUD");
        } else {
            tvDefaultCurrencySub.setText("₺ (TRY)");
        }

        if (tvCurrencyRatesSub != null) {
            tvCurrencyRatesSub.setText("Last updated: " + CurrencyExchangeManager.getLastUpdateTime(this));
        }

        if (budgetLimit > 0) {
            tvBudgetLimitSub.setText(String.format(Locale.getDefault(), "%s%.2f", currency, budgetLimit));
        } else {
            tvBudgetLimitSub.setText("Not set (Unlimited)");
        }

        tvAppLockSub.setText(appLock ? "Enabled (Active on launch)" : "Disabled");

        if (themeMode == 1) {
            if (tvThemeSub != null) tvThemeSub.setText("Light Theme");
        } else if (themeMode == 2) {
            if (tvThemeSub != null) tvThemeSub.setText("Dark Theme");
        } else {
            if (tvThemeSub != null) tvThemeSub.setText("System Default");
        }
    }

    private void updateTimeSubText(int hour, int minute) {
        tvReminderTimeSub.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}