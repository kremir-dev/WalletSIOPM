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
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalyticsActivity extends SecureActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AboneDatabase db;
    private TextView tvAnalyticsIncome;
    private TextView tvAnalyticsExpense;
    private TextView tvSmartInsight;
    private TextView tvMonthComparison;
    private PieChart pieChart;
    private PieChart pieChartPercentage;
    private BarChart barChart;
    private LineChart lineChart;
    private int analyticsPeriod;

    private static final class MonthStats {
        final String label;
        double income;
        double expense;

        MonthStats(String label) {
            this.label = label;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        SettingsActivity.applyTheme(prefs.getInt(SettingsActivity.PREF_THEME_MODE, 0));
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_analytics);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainAnalytics), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = AboneDatabase.getInstance(this);
        tvAnalyticsIncome = findViewById(R.id.tvAnalyticsIncome);
        tvAnalyticsExpense = findViewById(R.id.tvAnalyticsExpense);
        tvSmartInsight = findViewById(R.id.tvSmartInsight);
        tvMonthComparison = findViewById(R.id.tvMonthComparison);
        pieChart = findViewById(R.id.pieChart);
        pieChartPercentage = findViewById(R.id.pieChartPercentage);
        barChart = findViewById(R.id.barChart);
        lineChart = findViewById(R.id.lineChart);
        MaterialButtonToggleGroup periodGroup = findViewById(R.id.analyticsPeriodGroup);
        periodGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                analyticsPeriod = checkedId == R.id.btnPeriodYear ? 1 : 0;
                loadFinancialData();
            }
        });
        NavigationHelper.setup(this, R.id.nav_analytics);

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

    private void loadFinancialData() {
        executor.execute(() -> {
            List<Income> incomes = db.incomeDao().tumGelirleriGetir();
            List<Abonelik> subscriptions = db.aboneDao().tumunuGetir();
            List<Expense> expenses = db.expenseDao().tumGiderleriGetir();
            String currency = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                    .getString("default_currency", "₺");
            Map<String, Double> currentCategories = new HashMap<>();
            List<MonthStats> months = new ArrayList<>();
            Calendar thisMonth = UpcomingPaymentUtils.monthStart(Calendar.getInstance());

            int monthCount = analyticsPeriod == 0 ? 6 : thisMonth.get(Calendar.MONTH) + 1;
            Calendar yearStart = (Calendar) thisMonth.clone();
            yearStart.set(Calendar.MONTH, Calendar.JANUARY);
            for (int index = 0; index < monthCount; index++) {
                Calendar start = analyticsPeriod == 0
                        ? (Calendar) thisMonth.clone() : (Calendar) yearStart.clone();
                start.add(Calendar.MONTH, analyticsPeriod == 0 ? index - (monthCount - 1) : index);
                months.add(calculateMonth(start, subscriptions, expenses, incomes, currency,
                        analyticsPeriod == 1 || index == monthCount - 1, currentCategories));
            }

            MonthStats current = analyticsPeriod == 0
                    ? months.get(months.size() - 1) : sumMonths(months, "Year to date");
            MonthStats previous = analyticsPeriod == 0
                    ? months.get(months.size() - 2)
                    : calculatePreviousYearToDate(thisMonth, subscriptions, expenses, incomes, currency);
            String comparison;
            if (previous.expense == 0) {
                comparison = current.expense == 0
                        ? (analyticsPeriod == 0 ? "No expenses recorded this month or last month."
                        : "No expenses recorded this year or the same period last year.")
                        : "Spending is up from zero in the comparison period.";
            } else {
                double change = (current.expense - previous.expense) / previous.expense * 100;
                comparison = String.format(Locale.getDefault(),
                        "%s%.2f this period • %s%.2f previous period • %s%.1f%%",
                        currency, current.expense, currency, previous.expense,
                        change >= 0 ? "+" : "", change);
            }

            String insight;
            if (current.expense == 0) {
                insight = analyticsPeriod == 0
                        ? "No spending is recorded for this month yet. Add dated expenses or subscriptions to see your trend."
                        : "No spending is recorded for this year yet. Add dated expenses or subscriptions to see your trend.";
            } else if (current.income >= current.expense) {
                insight = String.format(Locale.getDefault(),
                        "Recorded income covers spending for this period. Remaining: %s%.2f.\n\nThe trend uses dated expenses and scheduled subscription renewals.",
                        currency, current.income - current.expense);
            } else {
                insight = String.format(Locale.getDefault(),
                        "Spending is higher than recorded income by %s%.2f for this period.\n\nReview the category breakdown and upcoming renewals to find savings.",
                        currency, current.expense - current.income);
            }

            List<PieEntry> categoryEntries = new ArrayList<>();
            for (Map.Entry<String, Double> entry : currentCategories.entrySet()) {
                double amount = entry.getValue();
                if (Double.isFinite(amount) && amount > 0 && amount <= Float.MAX_VALUE) {
                    categoryEntries.add(new PieEntry((float) amount, entry.getKey()));
                }
            }
            if (categoryEntries.isEmpty()) {
                categoryEntries.add(new PieEntry(1f, "No Expenses"));
            }

            runOnUiThread(() -> {
                tvAnalyticsIncome.setText(String.format(Locale.getDefault(), "%s%.2f", currency, current.income));
                tvAnalyticsExpense.setText(String.format(Locale.getDefault(), "%s%.2f", currency, current.expense));
                tvMonthComparison.setText(comparison);
                tvSmartInsight.setText(insight);
                String periodLabel = analyticsPeriod == 0 ? "This Month" : "Year to Date";
                ((TextView) findViewById(R.id.tvAnalyticsIncomeLabel)).setText(periodLabel + " Income");
                ((TextView) findViewById(R.id.tvAnalyticsExpenseLabel)).setText(periodLabel + " Expense");
                ((TextView) findViewById(R.id.tvCategoryChartTitle)).setText(periodLabel + " by Category");
                ((TextView) findViewById(R.id.tvIncomeExpenseChartTitle))
                        .setText("Income vs Expense " + periodLabel);
                ((TextView) findViewById(R.id.tvSpendingTrendTitle)).setText(
                        analyticsPeriod == 0
                                ? "Actual Monthly Spending (Last 6 Months)"
                                : "Actual Monthly Spending (This Year)");
                setupPieChart(categoryEntries, currency);
                setupPercentagePieChart(categoryEntries);
                setupBarChart(current.income, current.expense, currency);
                setupLineChart(months, currency);
            });
        });
    }

    private MonthStats calculateMonth(Calendar monthStart, List<Abonelik> subscriptions,
                                      List<Expense> expenses, List<Income> incomes,
                                      String currency, boolean includeCategories,
                                      Map<String, Double> categories) {
        Calendar start = UpcomingPaymentUtils.monthStart(monthStart);
        Calendar next = (Calendar) start.clone();
        next.add(Calendar.MONTH, 1);
        Date startDate = start.getTime();
        Date endDate = new Date(next.getTimeInMillis() - 1);
        MonthStats month = new MonthStats(
                new java.text.SimpleDateFormat("MMM", Locale.getDefault()).format(startDate));

        for (UpcomingPaymentUtils.DuePayment due :
                UpcomingPaymentUtils.getPaymentsInPeriod(subscriptions, startDate, endDate)) {
            Abonelik subscription = due.getSubscription();
            double amount = parseAmount(subscription.getAmount());
            if (Double.isFinite(amount)) {
                double converted = convertCurrency(amount, subscription.getCurrency(), currency);
                month.expense += converted;
                if (includeCategories) {
                    addCategory(categories, subscription.getCategory(), converted);
                }
            }
        }
        if (expenses != null) {
            for (Expense expense : expenses) {
                Date date = UpcomingPaymentUtils.parseDate(expense.getDate());
                if (date == null || date.before(startDate) || date.after(endDate)) {
                    continue;
                }
                double amount = parseAmount(expense.getAmount());
                if (Double.isFinite(amount)) {
                    double converted = convertCurrency(amount, expense.getCurrency(), currency);
                    month.expense += converted;
                    if (includeCategories) {
                        addCategory(categories, expense.getCategory(), converted);
                    }
                }
            }
        }
        if (incomes != null) {
            for (Income income : incomes) {
                Date date = UpcomingPaymentUtils.parseDate(income.getDate());
                if (date == null || date.before(startDate) || date.after(endDate)) {
                    continue;
                }
                double amount = parseAmount(income.getAmount());
                if (Double.isFinite(amount)) {
                    month.income += convertCurrency(amount, income.getCurrency(), currency);
                }
            }
        }
        return month;
    }

    private MonthStats sumMonths(List<MonthStats> months, String label) {
        MonthStats total = new MonthStats(label);
        for (MonthStats month : months) {
            total.income += month.income;
            total.expense += month.expense;
        }
        return total;
    }

    private MonthStats calculatePreviousYearToDate(Calendar thisMonth, List<Abonelik> subscriptions,
                                                   List<Expense> expenses, List<Income> incomes,
                                                   String currency) {
        Calendar yearStart = UpcomingPaymentUtils.monthStart(thisMonth);
        yearStart.add(Calendar.YEAR, -1);
        yearStart.set(Calendar.MONTH, Calendar.JANUARY);
        MonthStats total = new MonthStats("Previous year to date");
        int monthsElapsed = thisMonth.get(Calendar.MONTH) + 1;
        for (int index = 0; index < monthsElapsed; index++) {
            Calendar month = (Calendar) yearStart.clone();
            month.add(Calendar.MONTH, index);
            MonthStats stats = calculateMonth(month, subscriptions, expenses, incomes,
                    currency, false, new HashMap<>());
            total.income += stats.income;
            total.expense += stats.expense;
        }
        return total;
    }

    private double parseAmount(String amount) {
        if (amount == null) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(amount.replace(",", "."));
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    private void addCategory(Map<String, Double> categories, String category, double amount) {
        String label = category == null || category.trim().isEmpty() ? "Other" : category;
        categories.put(label, categories.getOrDefault(label, 0.0) + amount);
    }

    private double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        String from = fromCurrency == null || fromCurrency.isEmpty() ? "₺" : fromCurrency;
        if (from.equals(toCurrency)) {
            return amount;
        }
        double fromRate = CurrencyExchangeManager.getRateToTRY(this, from);
        double toRate = CurrencyExchangeManager.getRateToTRY(this, toCurrency);
        return amount * fromRate / toRate;
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
        pieChart.setData(new PieData(dataSet));
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText(analyticsPeriod == 0 ? "This Month" : "Year to Date");
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
        String period = analyticsPeriod == 0 ? "This month (" : "Year to date (";
        BarDataSet dataSet = new BarDataSet(entries, period + currency + ")");
        dataSet.setColors(new int[]{Color.parseColor("#4CD964"), Color.parseColor("#FF5252")});
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.text_primary));
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f %s", value, currency);
            }
        });
        barChart.setData(new BarData(dataSet));
        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setDrawLabels(false);
        barChart.getAxisLeft().setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        barChart.invalidate();
    }

    private void setupLineChart(List<MonthStats> months, String currency) {
        ArrayList<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < months.size(); i++) {
            MonthStats month = months.get(i);
            entries.add(new Entry(i, (float) month.expense));
            labels.add(month.label);
        }
        String title = analyticsPeriod == 0
                ? "Monthly spending (last 6 months)" : "Monthly spending (this year)";
        LineDataSet dataSet = new LineDataSet(entries, title);
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
        lineChart.setData(new LineData(dataSet));
        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setDrawLabels(true);
        lineChart.getXAxis().setGranularity(1f);
        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = Math.round(value);
                return index >= 0 && index < labels.size() ? labels.get(index) : "";
            }
        });
        lineChart.getAxisLeft().setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        lineChart.invalidate();
    }
}
