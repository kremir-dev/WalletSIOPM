package com.example.subscriptiontracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalyticsActivity extends AppCompatActivity {

    private AboneDatabase db;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView tvAnalyticsIncome, tvAnalyticsExpense, tvSmartInsight;
    private PieChart pieChart;
    private BarChart barChart;
    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        int themeMode = prefs.getInt(SettingsActivity.KEY_THEME_MODE, 0);
        SettingsActivity.applyTheme(themeMode);

        setContentView(R.layout.activity_analytics);

        db = AboneDatabase.getInstance(this);

        tvAnalyticsIncome = findViewById(R.id.tvAnalyticsIncome);
        tvAnalyticsExpense = findViewById(R.id.tvAnalyticsExpense);
        tvSmartInsight = findViewById(R.id.tvSmartInsight);
        pieChart = findViewById(R.id.pieChart);
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
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_analytics);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_subscriptions) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_expenses) {
                startActivity(new Intent(this, ExpenseActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_income) {
                startActivity(new Intent(this, IncomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_analytics) {
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            }
            return false;
        });
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
            float music = 0, movies = 0, software = 0, gaming = 0, other = 0;

            if (subs != null) {
                for (Abonelik sub : subs) {
                    try {
                        double amount = Double.parseDouble(sub.getAmount().replace(",", "."));
                        String subCurr = sub.getCurrency() != null ? sub.getCurrency() : "₺";
                        double convertedAmount = convertCurrency(amount, subCurr, defaultCurrency);
                        totalExpense += convertedAmount;

                        String cat = sub.getCategory() != null ? sub.getCategory() : "Other";
                        if (cat.contains("Music")) music += convertedAmount;
                        else if (cat.contains("Movie") || cat.contains("TV")) movies += convertedAmount;
                        else if (cat.contains("Software") || cat.contains("Cloud")) software += convertedAmount;
                        else if (cat.contains("Gaming")) gaming += convertedAmount;
                        else other += convertedAmount;
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
                        other += convertedAmount;
                    } catch (Exception ignored) {}
                }
            }

            final double finalIncome = totalIncome;
            final double finalExpense = totalExpense;
            final double yearlyProjection = finalExpense * 12;

            String savingsTip;
            if (finalExpense == 0) {
                savingsTip = "You have no active expenses recorded. Add subscriptions to get AI-powered financial insights!";
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
            if (music > 0) entries.add(new PieEntry(music, "Music"));
            if (movies > 0) entries.add(new PieEntry(movies, "Movies & TV"));
            if (software > 0) entries.add(new PieEntry(software, "Software"));
            if (gaming > 0) entries.add(new PieEntry(gaming, "Gaming"));
            if (other > 0 || entries.isEmpty()) entries.add(new PieEntry(other > 0 ? other : 1f, "Other / One-off"));

            runOnUiThread(() -> {
                tvAnalyticsIncome.setText(String.format(Locale.getDefault(), "%s%.2f", defaultCurrency, finalIncome));
                tvAnalyticsExpense.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbolFix(defaultCurrency), finalExpense));
                tvSmartInsight.setText(savingsTip);
                setupPieChart(entries, defaultCurrency);
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
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_analytics);
        }
        loadFinancialData();
    }

    private void setupPieChart(List<PieEntry> entries, String currency) {
        PieDataSet dataSet = new PieDataSet(entries, "Categories");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(13f);
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
        pieChart.getLegend().setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        pieChart.invalidate();
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