package com.example.subscriptiontracker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "expense_table")
public class Expense {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    private String title;
    private String amount;
    private String currency;
    private String date;
    private String category;
    private String paymentMethod;
    private String notes;

    public Expense(@NonNull String title, String amount, String currency, String date, String category, String paymentMethod, String notes) {
        this.title = title;
        this.amount = amount;
        this.currency = currency;
        this.date = date;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @NonNull
    public String getTitle() { return title; }
    public void setTitle(@NonNull String title) { this.title = title; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getCurrency() { return currency != null ? currency : "₺"; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getCategory() { return category != null ? category : "Other"; }
    public void setCategory(String category) { this.category = category; }

    public String getPaymentMethod() { return paymentMethod != null ? paymentMethod : "Credit Card"; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}