package com.example.WalletSIOPM;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Executor;

public class UpcomingPaymentsActivity extends SecureActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AboneDatabase database;
    private PaymentAdapter adapter;
    private TextView summary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        SettingsActivity.applyTheme(prefs.getInt(SettingsActivity.PREF_THEME_MODE, 0));
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upcoming_payments);

        database = AboneDatabase.getInstance(this);
        summary = findViewById(R.id.tvUpcomingSummary);
        RecyclerView list = findViewById(R.id.rvUpcomingPayments);
        adapter = new PaymentAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        RadioGroup horizonSelector = findViewById(R.id.rgUpcomingHorizon);
        horizonSelector.setOnCheckedChangeListener((group, checkedId) -> {
            int horizonDays = checkedId == R.id.rbUpcoming90Days ? 90
                    : checkedId == R.id.rbUpcoming60Days ? 60 : 30;
            loadPayments(horizonDays);
        });
        findViewById(R.id.btnUpcomingBack).setOnClickListener(view -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
        if (prefs.getBoolean(SettingsActivity.PREF_APP_LOCK_ENABLED, false)) {
            findViewById(R.id.mainUpcomingPayments).setVisibility(View.INVISIBLE);
            authenticate();
        } else {
            loadPayments(30);
        }
    }

    private void authenticate() {
        Executor mainExecutor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, mainExecutor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        findViewById(R.id.mainUpcomingPayments).setVisibility(View.VISIBLE);
                        loadPayments(30);
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        finish();
                    }
                });
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Subscription Tracker Lock")
                .setSubtitle("Authenticate to view upcoming payments")
                .setNegativeButtonText("Cancel")
                .build();
        prompt.authenticate(promptInfo);
    }

    private void loadPayments(int horizonDays) {
        executor.execute(() -> {
            List<Abonelik> subscriptions = database.aboneDao().tumunuGetir();
            Calendar periodStart = Calendar.getInstance();
            periodStart.set(Calendar.HOUR_OF_DAY, 0);
            periodStart.set(Calendar.MINUTE, 0);
            periodStart.set(Calendar.SECOND, 0);
            periodStart.set(Calendar.MILLISECOND, 0);
            Calendar periodEnd = (Calendar) periodStart.clone();
            periodEnd.add(Calendar.DAY_OF_MONTH, horizonDays);
            periodEnd.add(Calendar.MILLISECOND, -1);
            List<UpcomingPaymentUtils.DuePayment> payments =
                    UpcomingPaymentUtils.getPaymentsInPeriod(subscriptions,
                            periodStart.getTime(), periodEnd.getTime());
            SharedPreferences prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
            String currency = prefs.getString("default_currency", "₺");
            double total = 0;
            for (UpcomingPaymentUtils.DuePayment payment : payments) {
                Abonelik subscription = payment.getSubscription();
                double amount = parseAmount(subscription.getAmount());
                if (Double.isFinite(amount)) {
                    total += convertCurrency(amount, subscription.getCurrency(), currency);
                }
            }
            double periodTotal = total;
            int paymentCount = payments.size();
            runOnUiThread(() -> {
                if (paymentCount == 0) {
                    summary.setText(getString(R.string.upcoming_empty, horizonDays));
                } else {
                    summary.setText(String.format(Locale.getDefault(),
                            getString(R.string.upcoming_summary),
                            horizonDays, currency, periodTotal, paymentCount));
                }
                adapter.submit(payments);
            });
        });
    }

    private double parseAmount(String value) {
        if (value == null) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
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
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private static final class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder> {
        private final List<UpcomingPaymentUtils.DuePayment> payments = new ArrayList<>();

        void submit(List<UpcomingPaymentUtils.DuePayment> newPayments) {
            payments.clear();
            payments.addAll(newPayments);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_upcoming_payment, parent, false);
            return new PaymentViewHolder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
            UpcomingPaymentUtils.DuePayment payment = payments.get(position);
            Abonelik subscription = payment.getSubscription();
            holder.name.setText(subscription.getName());
            String dueDate = new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
                    .format(payment.getDate());
            holder.details.setText(String.format(Locale.getDefault(), "%s • %s%s • %s",
                    dueDate, subscription.getCurrency(), subscription.getAmount(),
                    subscription.getBillingCycle()));
        }

        @Override
        public int getItemCount() {
            return payments.size();
        }

        static final class PaymentViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView details;

            PaymentViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tvUpcomingName);
                details = itemView.findViewById(R.id.tvUpcomingDetails);
            }
        }
    }
}
