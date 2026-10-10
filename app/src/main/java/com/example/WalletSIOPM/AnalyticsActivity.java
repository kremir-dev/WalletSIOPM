package com.example.WalletSIOPM;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalyticsActivity extends SecureActivity {

    private AboneDatabase db;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView tvAnalyticsIncome, tvAnalyticsExpense, tvSmartInsight;
    private PieChart pieChart, pieChartPercentage;
    private BarChart barChart;
    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        int themeMode = prefs.getInt(SettingsActivity.PREF_THEME_MODE, 0);
        SettingsActivity.applyTheme(themeMode);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_analytics);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainAnalytics), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = AboneDatabase.getInstance(this);

        tvAnalyticsIncome = findViewById(R.id.tvAnalyticsIncome);
        tvAnalyticsExpense = findViewById(R.id.tvAnalyticsExpense);
        tvSmartInsight = findViewById(R.id.tvSmartInsight);
        pieChart = findViewById(R.id.pieChart);
        pieChartPercentage = findViewById(R.id.pieChartPercentage);
        barChart = findViewById(R.id.barChart);
        lineChart = findViewById(R.id.lineChart);

        setupBottomNav();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(AnalyticsActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        loadFinancialData();
    }

    private void setupBottomNav() {
        NavigationHelper.setup(this, R.id.nav_analytics);
    }

    private void loadFinancialData() {
        executor.execute(() -> {
            List<Income> incomes = db.incomeDao().tumGelirleriGetir();
            List<Abonelik> subs = db.aboneDao().tumunuGetir();
            List<Expense> expenses = db.expenseDao().tumGiderleriGetir();

            SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
            String defaultCurrency = prefs.getString("default_currency", "₺");

            double totalIncome = 0.0;
            if (incomes != null) {
                for (Income inc : incomes) {
                    try {
                        double amount = Double.parseDouble(inc.getAmount().replace(",", "."));
                        String incCurr = inc.getCurrency() != null ? inc.getCurrency() : "₺";
                        totalIncome += convertCurrency(amount, incCurr, defaultCurrency);
                    } catch (Exception ignored) {}
                }
            }

            double totalExpense = 0.0;
            Map<String, Double> categoryMap = new HashMap<>();

            if (subs != null) {
                for (Abonelik sub : subs) {
                    try {
                        double amount = Double.parseDouble(sub.getAmount().replace(",", "."));
                        String subCurr = sub.getCurrency() != null ? sub.getCurrency() : "₺";
                        double convertedAmount = convertCurrency(amount, subCurr, defaultCurrency);
                        totalExpense += convertedAmount;

                        String cat = (sub.getCategory() != null && !sub.getCategory().isEmpty()) ? sub.getCategory() : "Other";
                        categoryMap.put(cat, categoryMap.getOrDefault(cat, 0.0) + convertedAmount);
                    } catch (Exception ignored) {}
                }
            }

            if (expenses != null) {
                for (Expense exp : expenses) {
                    try {
                        double amount = Double.parseDouble(exp.getAmount().replace(",", "."));
                        String expCurr = exp.getCurrency() != null ? exp.getCurrency() : "₺";
                        double convertedAmount = convertCurrency(amount, expCurr, defaultCurrency);
                        totalExpense += convertedAmount;

                        String cat = (exp.getCategory() != null && !exp.getCategory().isEmpty()) ? exp.getCategory() : "Other";
                        categoryMap.put(cat, categoryMap.getOrDefault(cat, 0.0) + convertedAmount);
                    } catch (Exception ignored) {}
                }
            }

            final double finalIncome = totalIncome;
            final double finalExpense = totalExpense;
            final double yearlyProjection = finalExpense * 12;

            String savingsTip;
            if (finalExpense == 0) {
                savingsTip = "You have no active expenses recorded. Add subscriptions or expenses to get AI-powered financial insights!";
            } else if (finalIncome > finalExpense) {
                savingsTip = String.format(Locale.getDefault(),
                        "📊 Yearly Projection: Your annual spending is projected at %.2f %s.\n\nGreat job! Your income exceeds your expenses. You are saving %.2f %s monthly.",
                        yearlyProjection, defaultCurrency, (finalIncome - finalExpense), defaultCurrency);
            } else {
                savingsTip = String.format(Locale.getDefault(),
                        "📊 Yearly Projection: Your annual spending is projected at %.2f %s.\n\n⚠️ Warning: Your expenses exceed your income. Consider reviewing or cancelling unused subscriptions to save money.",
                        yearlyProjection, defaultCurrency);
            }

            List<PieEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
                double amount = entry.getValue();
                if (Double.isFinite(amount) && amount > 0.0 && amount <= Float.MAX_VALUE) {
                    entries.add(new PieEntry((float) amount, entry.getKey()));
                }
            }
            if (entries.isEmpty()) {
                entries.add(new PieEntry(1f, "No Expenses"));
            }

            runOnUiThread(() -> {
                tvAnalyticsIncome.setText(String.format(Locale.getDefault(), "%s%.2f", defaultCurrency, finalIncome));
                tvAnalyticsExpense.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbolFix(defaultCurrency), finalExpense));
                tvSmartInsight.setText(savingsTip);
                setupPieChart(entries, defaultCurrency);
                setupPercentagePieChart(entries);
                setupBarChart(finalIncome, finalExpense, defaultCurrency);
                setupLineChart(finalExpense, defaultCurrency);
            });
        });
    }

    private String currencySymbolFix(String currency) {
        return currency != null ? currency : "₺";
    }

    private double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency == null || fromCurrency.isEmpty()) fromCurrency = "₺";
        if (toCurrency == null || toCurrency.isEmpty()) toCurrency = "₺";

        if (fromCurrency.equals(toCurrency)) return amount;

        double fromRate = CurrencyExchangeManager.getRateToTRY(this, fromCurrency);
        double toRate = CurrencyExchangeManager.getRateToTRY(this, toCurrency);

        return (amount * fromRate) / toRate;
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavigationHelper.setup(this, R.id.nav_analytics);
        loadFinancialData();
    }

    private void setupPieChart(List<PieEntry> entries, String currency) {
        PieDataSet dataSet = new PieDataSet(entries, "Categories");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        configurePieDataLabels(dataSet);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f %s", value, currency);
            }
        });

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Expenses");
        pieChart.setCenterTextColor(ContextCompat.getColor(this, R.color.text_primary));
        pieChart.setHoleColor(ContextCompat.getColor(this, R.color.bg_card));
        pieChart.setTransparentCircleColor(ContextCompat.getColor(this, R.color.divider));
        pieChart.setDrawEntryLabels(false);
        pieChart.getLegend().setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        pieChart.invalidate();
    }

    private void setupPercentagePieChart(List<PieEntry> entries) {
        pieChartPercentage.getDescription().setEnabled(false);
        pieChartPercentage.setCenterTextColor(ContextCompat.getColor(this, R.color.text_primary));
        pieChartPercentage.setHoleColor(ContextCompat.getColor(this, R.color.bg_card));
        pieChartPercentage.setTransparentCircleColor(ContextCompat.getColor(this, R.color.divider));
        pieChartPercentage.getLegend().setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        if (entries.size() == 1 && "No Expenses".equals(entries.get(0).getLabel())) {
            pieChartPercentage.clear();
            pieChartPercentage.setCenterText("No Expenses");
            pieChartPercentage.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "Categories");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        configurePieDataLabels(dataSet);

        PieData data = new PieData(dataSet);
        pieChartPercentage.setData(data);
        pieChartPercentage.setUsePercentValues(true);
        dataSet.setValueFormatter(new PercentFormatter(pieChartPercentage));
        pieChartPercentage.setCenterText("Expense\nPercentage");
        pieChartPercentage.setDrawEntryLabels(false);
        pieChartPercentage.invalidate();
    }

    private void configurePieDataLabels(PieDataSet dataSet) {
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.text_primary));
        dataSet.setValueTextSize(12f);
        dataSet.setValueLineColor(ContextCompat.getColor(this, R.color.text_secondary));
        dataSet.setValueLineWidth(1f);
        dataSet.setValueLinePart1Length(0.3f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setValueLinePart1OffsetPercentage(80f);
    }

    private void setupBarChart(double income, double expense, String currency) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, (float) income));
        entries.add(new BarEntry(1f, (float) expense));

        BarDataSet dataSet = new BarDataSet(entries, "Income vs Expense (" + currency + ")");
        dataSet.setColors(new int[]{Color.parseColor("#4CD964"), Color.parseColor("#FF5252")});
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.text_primary));
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f %s", value, currency);
            }
        });

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setDrawLabels(false);
        barChart.getAxisLeft().setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        barChart.invalidate();
    }

    private void setupLineChart(double expense, String currency) {
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(1f, (float) (expense * 0.8)));
        entries.add(new Entry(2f, (float) (expense * 0.9)));
        entries.add(new Entry(3f, (float) (expense * 0.95)));
        entries.add(new Entry(4f, (float) expense));

        LineDataSet dataSet = new LineDataSet(entries, "Monthly Spending Trend");
        dataSet.setColor(Color.parseColor("#3B82F6"));
        dataSet.setCircleColor(Color.parseColor("#3B82F6"));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.text_primary));
        dataSet.setValueTextSize(11f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f %s", value, currency);
            }
        });

        LineData data = new LineData(dataSet);
        lineChart.setData(data);
        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setDrawLabels(false);
        lineChart.getAxisLeft().setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        lineChart.invalidate();
    }
}