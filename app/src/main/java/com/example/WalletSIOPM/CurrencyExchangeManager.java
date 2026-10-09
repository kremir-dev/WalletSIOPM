package com.example.subscriptiontracker;

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
    private static final String KEY_RATE_USD = "rate_usd";
    private static final String KEY_RATE_EUR = "rate_eur";
    private static final String KEY_RATE_GBP = "rate_gbp";
    private static final String KEY_LAST_UPDATE = "last_update";

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
                        double tryRate = rates.optDouble("TRY", 34.0);
                        double eurRate = rates.optDouble("EUR", 0.92);
                        double gbpRate = rates.optDouble("GBP", 0.78);

                        double rateUSD = tryRate;
                        double rateEUR = tryRate / eurRate;
                        double rateGBP = tryRate / gbpRate;

                        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                        prefs.edit()
                                .putFloat(KEY_RATE_USD, (float) rateUSD)
                                .putFloat(KEY_RATE_EUR, (float) rateEUR)
                                .putFloat(KEY_RATE_GBP, (float) rateGBP)
                                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
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

    public static double getRateToTRY(Context context, String currency) {
        if (currency == null) return 1.0;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String cleanCurr = currency.trim();

        switch (cleanCurr) {
            case "$": case "USD":
                return prefs.getFloat(KEY_RATE_USD, 34.0f);
            case "€": case "EUR":
                return prefs.getFloat(KEY_RATE_EUR, 37.5f);
            case "£": case "GBP":
                return prefs.getFloat(KEY_RATE_GBP, 44.5f);
            case "₿": case "BTC":
                return 3500000.0f;
            case "Ξ": case "ETH":
                return 130000.0f;
            case "USDT":
                return prefs.getFloat(KEY_RATE_USD, 34.0f);
            case "CAD":
                return prefs.getFloat(KEY_RATE_USD, 34.0f) * 0.72f;
            case "AUD":
                return prefs.getFloat(KEY_RATE_USD, 34.0f) * 0.65f;
            default:
                return 1.0f;
        }
    }

    public static String getLastUpdateTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long time = prefs.getLong(KEY_LAST_UPDATE, 0);
        if (time == 0) return "Never updated (Using default rates)";
        return DateFormat.format("dd/MM/yyyy HH:mm", new Date(time)).toString();
    }
}