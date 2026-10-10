package com.example.WalletSIOPM;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class CurrencyExchangeManager {

    private static final String PREF_NAME = "ExchangeRatesPrefs";
    private static final String PREF_RATE_USD = "rate_usd";
    private static final String PREF_RATE_EUR = "rate_eur";
    private static final String PREF_RATE_GBP = "rate_gbp";
    private static final String PREF_LAST_UPDATE = "last_update";

    public interface CurrencyUpdateListener {
        void onRatesUpdated(boolean success);
    }

    public static void fetchLatestRates(Context context, CurrencyUpdateListener listener) {
        new Thread(() -> {
            try {
                URL url = new URL("https://open.er-api.com/v6/latest/USD");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    if (json.getString("result").equals("success")) {
                        JSONObject rates = json.getJSONObject("rates");
                        double tryRate = rates.optDouble("TRY", Double.NaN);
                        double eurRate = rates.optDouble("EUR", Double.NaN);
                        double gbpRate = rates.optDouble("GBP", Double.NaN);
                        if (!isValidRate(tryRate) || !isValidRate(eurRate) || !isValidRate(gbpRate)) {
                            if (listener != null) {
                                new Handler(Looper.getMainLooper()).post(() -> listener.onRatesUpdated(false));
                            }
                            return;
                        }

                        double rateUSD = tryRate;
                        double rateEUR = tryRate / eurRate;
                        double rateGBP = tryRate / gbpRate;
                        if (!isValidRate(rateEUR) || !isValidRate(rateGBP)) {
                            if (listener != null) {
                                new Handler(Looper.getMainLooper()).post(() -> listener.onRatesUpdated(false));
                            }
                            return;
                        }

                        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString(PREF_RATE_USD, Double.toString(rateUSD))
                                .putString(PREF_RATE_EUR, Double.toString(rateEUR))
                                .putString(PREF_RATE_GBP, Double.toString(rateGBP))
                                .putLong(PREF_LAST_UPDATE, System.currentTimeMillis())
                                .apply();

                        if (listener != null) {
                            new Handler(Looper.getMainLooper()).post(() -> listener.onRatesUpdated(true));
                        }
                        return;
                    }
                }
                if (listener != null) {
                    new Handler(Looper.getMainLooper()).post(() -> listener.onRatesUpdated(false));
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (listener != null) {
                    new Handler(Looper.getMainLooper()).post(() -> listener.onRatesUpdated(false));
                }
            }
        }).start();
    }

    private static boolean isValidRate(double rate) {
        return Double.isFinite(rate) && rate > 0.0 && rate <= 1_000_000_000.0;
    }

    private static double getStoredRate(SharedPreferences prefs, String preferenceName, double fallback) {
        Object storedRate = prefs.getAll().get(preferenceName);
        if (storedRate instanceof Number) {
            double rate = ((Number) storedRate).doubleValue();
            return isValidRate(rate) ? rate : fallback;
        }
        if (storedRate instanceof String) {
            try {
                double rate = Double.parseDouble((String) storedRate);
                return isValidRate(rate) ? rate : fallback;
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }

    public static double getRateToTRY(Context context, String currency) {
        if (currency == null) return 1.0;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String cleanCurr = currency.trim();

        switch (cleanCurr) {
            case "$": case "USD":
                return getStoredRate(prefs, PREF_RATE_USD, 34.0);
            case "€": case "EUR":
                return getStoredRate(prefs, PREF_RATE_EUR, 37.5);
            case "£": case "GBP":
                return getStoredRate(prefs, PREF_RATE_GBP, 44.5);
            case "₿": case "BTC":
                return 3500000.0f;
            case "Ξ": case "ETH":
                return 130000.0f;
            case "USDT":
                return getStoredRate(prefs, PREF_RATE_USD, 34.0);
            case "CAD":
                return getStoredRate(prefs, PREF_RATE_USD, 34.0) * 0.72;
            case "AUD":
                return getStoredRate(prefs, PREF_RATE_USD, 34.0) * 0.65;
            default:
                return 1.0f;
        }
    }

    public static String getLastUpdateTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long time = prefs.getLong(PREF_LAST_UPDATE, 0);
        if (time == 0) return "Never updated (Using default rates)";
        return DateFormat.format("dd/MM/yyyy HH:mm", new Date(time)).toString();
    }
}