package com.xxcamixx.contabilidad.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xxcamixx.contabilidad.model.Transaction
import com.xxcamixx.contabilidad.model.Reminder
import com.xxcamixx.contabilidad.model.Fiador
import com.xxcamixx.contabilidad.model.Product
import com.xxcamixx.contabilidad.model.ComercioProduct
import com.xxcamixx.contabilidad.model.ComercioMovement
import com.xxcamixx.contabilidad.model.ComercioPedido

@Database(entities = [Transaction::class, Reminder::class, Fiador::class, Product::class, ComercioProduct::class, ComercioMovement::class, ComercioPedido::class], version = 22, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
    companion object {
        @Volatile private var INSTANCES = mutableMapOf<String, AppDatabase>()
        fun getDatabase(context: Context, userId: String): AppDatabase {
            return INSTANCES[userId] ?: synchronized(this) {
                val dbName = "finance_database_$userId"
                val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, dbName)
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22)
                    .build()
                INSTANCES[userId] = instance
                instance
            }
        }
    }
}
