package com.example.WalletSIOPM;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface IncomeDao {

    @Insert
    void ekle(Income income);

    @Delete
    void sil(Income income);

    @Update
    void guncelle(Income income);

    @Query("SELECT * FROM income_table")
    List<Income> tumGelirleriGetir();

    @Query("DELETE FROM income_table")
    void tumunuSil();
}
