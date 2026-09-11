package com.xxcamixx.contabilidad.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

fun SupportSQLiteDatabase.addColumnIfNotExists(tableName: String, columnName: String, columnDef: String) {
    try {
        val cursor = query("PRAGMA table_info(`$tableName`)")
        var exists = false
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (nameIndex != -1 && cursor.getString(nameIndex).equals(columnName, ignoreCase = true)) {
                exists = true
                break
            }
        }
        cursor.close()
        if (!exists) {
            execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDef")
        }
    } catch (_: Exception) {
        try { execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDef") } catch (_: Exception) {}
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `price` REAL NOT NULL, `stock` INTEGER NOT NULL, `expirationDateInMillis` INTEGER)") } }
val MIGRATION_5_6 = object : Migration(5, 6) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("products", "entryDateInMillis", "INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}") } }
val MIGRATION_6_7 = object : Migration(6, 7) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("fiadores", "phone", "TEXT NOT NULL DEFAULT ''") } }
val MIGRATION_7_8 = object : Migration(7, 8) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("products", "unit", "TEXT NOT NULL DEFAULT 'Uds'") } }
val MIGRATION_8_9 = object : Migration(8, 9) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("products", "purchasePrice", "REAL NOT NULL DEFAULT 0.0"); db.addColumnIfNotExists("transactions", "profit", "REAL NOT NULL DEFAULT 0.0") } }
val MIGRATION_9_10 = object : Migration(9, 10) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("products", "minStock", "INTEGER NOT NULL DEFAULT 0") } }
val MIGRATION_10_11 = object : Migration(10, 11) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("transactions", "cashAmount", "REAL NOT NULL DEFAULT 0.0"); db.addColumnIfNotExists("transactions", "digitalAmount", "REAL NOT NULL DEFAULT 0.0") } }
val MIGRATION_11_12 = object : Migration(11, 12) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("fiadores", "paidAmount", "REAL NOT NULL DEFAULT 0.0"); db.addColumnIfNotExists("fiadores", "paymentHistory", "TEXT NOT NULL DEFAULT ''") } }
val MIGRATION_12_13 = object : Migration(12, 13) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("products", "imageUri", "TEXT DEFAULT NULL") } }
val MIGRATION_13_14 = object : Migration(13, 14) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("reminders", "isStore", "INTEGER NOT NULL DEFAULT 0"); db.addColumnIfNotExists("fiadores", "isStore", "INTEGER NOT NULL DEFAULT 1") } }
val MIGRATION_14_15 = object : Migration(14, 15) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("reminders", "amount", "REAL NOT NULL DEFAULT 0.0") } }
val MIGRATION_15_16 = object : Migration(15, 16) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("fiadores", "totalCost", "REAL NOT NULL DEFAULT 0.0") } }
val MIGRATION_16_17 = object : Migration(16, 17) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("transactions", "country", "TEXT NOT NULL DEFAULT 'Colombia'"); db.addColumnIfNotExists("reminders", "country", "TEXT NOT NULL DEFAULT 'Colombia'"); db.addColumnIfNotExists("fiadores", "country", "TEXT NOT NULL DEFAULT 'Colombia'"); db.addColumnIfNotExists("products", "country", "TEXT NOT NULL DEFAULT 'Colombia'") } }
val MIGRATION_17_18 = object : Migration(17, 18) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("transactions", "category", "TEXT DEFAULT NULL"); db.addColumnIfNotExists("transactions", "imageUri", "TEXT DEFAULT NULL") } }
val MIGRATION_18_19 = object : Migration(18, 19) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS `comercio_products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `unit` TEXT NOT NULL DEFAULT 'Uds', `quantityInStock` REAL NOT NULL, `totalPurchased` REAL NOT NULL, `totalSold` REAL NOT NULL DEFAULT 0.0, `costPerUnit` REAL NOT NULL, `salePricePerUnit` REAL NOT NULL DEFAULT 0.0, `country` TEXT NOT NULL DEFAULT 'Colombia', `createdAt` INTEGER NOT NULL DEFAULT " + System.currentTimeMillis() + ")"); db.execSQL("CREATE TABLE IF NOT EXISTS `comercio_movements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `productId` INTEGER NOT NULL, `productName` TEXT NOT NULL, `type` TEXT NOT NULL, `quantity` REAL NOT NULL, `pricePerUnit` REAL NOT NULL, `total` REAL NOT NULL, `note` TEXT NOT NULL DEFAULT '', `country` TEXT NOT NULL DEFAULT 'Colombia', `timestamp` INTEGER NOT NULL DEFAULT " + System.currentTimeMillis() + ")") } }
val MIGRATION_19_20 = object : Migration(19, 20) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS `comercio_pedidos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `country` TEXT NOT NULL DEFAULT 'Colombia', `timestamp` INTEGER NOT NULL DEFAULT " + System.currentTimeMillis() + ")"); db.addColumnIfNotExists("comercio_products", "pedidoId", "INTEGER NOT NULL DEFAULT 0") } }
val MIGRATION_20_21 = object : Migration(20, 21) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("comercio_products", "imageUri", "TEXT DEFAULT NULL"); db.addColumnIfNotExists("comercio_pedidos", "imageUri", "TEXT DEFAULT NULL") } }
val MIGRATION_21_22 = object : Migration(21, 22) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("fiadores", "originMode", "TEXT NOT NULL DEFAULT 'TIENDA'"); try { db.execSQL("UPDATE `fiadores` SET `originMode` = 'PERSONAL' WHERE `isStore` = 0") } catch (_: Exception){}; db.addColumnIfNotExists("reminders", "originMode", "TEXT NOT NULL DEFAULT 'PERSONAL'"); try { db.execSQL("UPDATE `reminders` SET `originMode` = 'TIENDA' WHERE `isStore` = 1") } catch (_: Exception){} } }

val MIGRATION_22_23 = object : Migration(22, 23) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("comercio_movements", "transactionId", "TEXT DEFAULT NULL") } }

val MIGRATION_23_24 = object : Migration(23, 24) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS `cierre_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mode` TEXT NOT NULL, `name` TEXT NOT NULL, `totalIncomes` REAL NOT NULL, `totalExpenses` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `country` TEXT NOT NULL)"); db.addColumnIfNotExists("transactions", "cierreId", "INTEGER DEFAULT NULL"); db.addColumnIfNotExists("fiadores", "cierreId", "INTEGER DEFAULT NULL"); db.addColumnIfNotExists("comercio_movements", "cierreId", "INTEGER DEFAULT NULL") } }

val MIGRATION_24_25 = object : Migration(24, 25) { override fun migrate(db: SupportSQLiteDatabase) { db.addColumnIfNotExists("products", "category", "TEXT DEFAULT NULL") } }
