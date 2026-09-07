const fs = require('fs');

// 3. Migración 18->19
let mig = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/data/Migrations.kt', 'utf-8');
mig = mig.trimEnd() + `\nval MIGRATION_18_19 = object : Migration(18, 19) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS \`comercio_products\` (\`id\` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, \`name\` TEXT NOT NULL, \`unit\` TEXT NOT NULL DEFAULT 'Uds', \`quantityInStock\` REAL NOT NULL, \`totalPurchased\` REAL NOT NULL, \`totalSold\` REAL NOT NULL DEFAULT 0.0, \`costPerUnit\` REAL NOT NULL, \`salePricePerUnit\` REAL NOT NULL DEFAULT 0.0, \`country\` TEXT NOT NULL DEFAULT 'Colombia', \`createdAt\` INTEGER NOT NULL DEFAULT ${Date.now()})"); db.execSQL("CREATE TABLE IF NOT EXISTS \`comercio_movements\` (\`id\` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, \`productId\` INTEGER NOT NULL, \`productName\` TEXT NOT NULL, \`type\` TEXT NOT NULL, \`quantity\` REAL NOT NULL, \`pricePerUnit\` REAL NOT NULL, \`total\` REAL NOT NULL, \`note\` TEXT NOT NULL DEFAULT '', \`country\` TEXT NOT NULL DEFAULT 'Colombia', \`timestamp\` INTEGER NOT NULL DEFAULT ${Date.now()})") } }\n`;
fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/data/Migrations.kt', mig, 'utf-8');

// 4. AppDatabase - agregar entidades y versión 19
let db = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/data/AppDatabase.kt', 'utf-8');
db = db.replace('import com.xxcamixx.contabilidad.model.Product', 'import com.xxcamixx.contabilidad.model.Product\nimport com.xxcamixx.contabilidad.model.ComercioProduct\nimport com.xxcamixx.contabilidad.model.ComercioMovement');
db = db.replace('entities = [Transaction::class, Reminder::class, Fiador::class, Product::class], version = 18', 'entities = [Transaction::class, Reminder::class, Fiador::class, Product::class, ComercioProduct::class, ComercioMovement::class], version = 19');
db = db.replace('.addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18)', '.addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)');
fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/data/AppDatabase.kt', db, 'utf-8');

console.log('Migración y AppDatabase actualizados');
