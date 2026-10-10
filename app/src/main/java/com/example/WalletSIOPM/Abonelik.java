package com.example.WalletSIOPM;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "Subscription")
public class Abonelik {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    private String name;
    private String amount;
    private String date;
    private String category;
    private String notes;
    private String billingCycle;
    private String currency; // Para birimi alanı (örn: $, ₺, €, £)
    private String paymentMethod; // Ödeme yöntemi alanı (örn: Credit Card, Debit Card, Cash)
    private int reminderDaysBefore; // Kaç gün önce hatırlatılsın (0, 1, 2, 3, 7)

    public Abonelik(@NonNull String name, String amount, String date, String category, String notes, String billingCycle, String currency, String paymentMethod, int reminderDaysBefore) {
        this.name = name;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.notes = notes;
        this.billingCycle = billingCycle;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.reminderDaysBefore = reminderDaysBefore;
    }

    @Ignore
    public Abonelik(@NonNull String name, String amount, String date, String category, String notes, String billingCycle, String currency, String paymentMethod) {
        this(name, amount, date, category, notes, billingCycle, currency, paymentMethod, 1);
    }

    @Ignore
    public Abonelik(@NonNull String name, String amount, String date, String category, String notes, String billingCycle, String currency) {
        this(name, amount, date, category, notes, billingCycle, currency, "Credit Card", 1);
    }

    @Ignore
    public Abonelik(@NonNull String name, String amount, String date, String category, String notes, String billingCycle) {
        this(name, amount, date, category, notes, billingCycle, "₺", "Credit Card", 1);
    }

    @Ignore
    public Abonelik(@NonNull String name, String amount, String date, String category, String notes) {
        this(name, amount, date, category, notes, "", "₺", "Credit Card", 1);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getBillingCycle() { return billingCycle; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }

    public String getCurrency() { return currency != null ? currency : "₺"; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentMethod() { return paymentMethod != null && !paymentMethod.isEmpty() ? paymentMethod : "Credit Card"; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public int getReminderDaysBefore() { return reminderDaysBefore; }
    public void setReminderDaysBefore(int reminderDaysBefore) { this.reminderDaysBefore = reminderDaysBefore; }

    // Geriye dönük uyumluluk takma adları (Aliases)
    @NonNull
    public String getIsim() { return name; }
    public String getTutar() { return amount; }
    public String getTarih() { return date; }
    public String getTip() { return category; }
    public String getNotlar() { return notes; }
    public String getPeriyot() { return billingCycle; }
}