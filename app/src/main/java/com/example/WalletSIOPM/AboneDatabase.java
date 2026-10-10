package com.example.WalletSIOPM;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Abonelik.class, Income.class, Expense.class}, version = 6, exportSchema = false)
public abstract class AboneDatabase extends RoomDatabase {

    public abstract AboneDao aboneDao();
    public abstract IncomeDao incomeDao();
    public abstract ExpenseDao expenseDao();

    private static volatile AboneDatabase INSTANCE;

    public static AboneDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AboneDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AboneDatabase.class,
                                    "abone_database"
                            )
                            .fallbackToDestructiveMigration() // Şema değiştiğinde eski tabloyu sıfırlayıp yenisini oluşturur
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}