package com.example.subscriptiontracker;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert
    void ekle(Expense expense);

    @Delete
    void sil(Expense expense);

    @Update
    void guncelle(Expense expense);

    @Query("SELECT * FROM expense_table ORDER BY id DESC")
    List<Expense> tumGiderleriGetir();

    @Query("DELETE FROM expense_table")
    void tumunuSil();
}