package com.example.subscriptiontracker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionItem implements Comparable<TransactionItem> {
    private long id;
    private String title;
    private String amount;
    private String currency;
    private String date;
    private String type; // "INCOME" veya "EXPENSE"
    private Date rawDate;

    public TransactionItem(long id, String title, String amount, String currency, String date, String type) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.currency = currency != null ? currency : "₺";
        this.date = date;
        this.type = type;
        this.rawDate = parseDate(date);
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null) return new Date(0);
        String[] formats = {"dd/MM/yyyy", "yyyy-MM-dd", "d/M/yyyy"};
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.getDefault());
                return sdf.parse(dateStr);
            } catch (Exception ignored) {}
        }
        return new Date(0);
    }

    @Override
    public int compareTo(TransactionItem other) {
        // En yeni tarih en üstte olacak şekilde sıralama
        if (this.rawDate != null && other.rawDate != null) {
            return other.rawDate.compareTo(this.rawDate);
        }
        return 0;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getDate() { return date; }
    public String getType() { return type; }
}