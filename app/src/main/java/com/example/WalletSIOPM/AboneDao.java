package com.example.subscriptiontracker;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface AboneDao {

    @Insert
    void ekle(Abonelik abonelik);

    @Delete
    void sil(Abonelik abonelik);

    @Update
    void guncelle(Abonelik abonelik);

    @Query("SELECT * FROM Subscription")
    List<Abonelik> tumunuGetir();

    @Query("DELETE FROM Subscription")
    void tumunuSil();



}
