const fs = require('fs');

// 1. Update ComercioProduct model - add imageUri
let cp = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/model/ComercioProduct.kt', 'utf-8');
if (!cp.includes('imageUri')) {
    cp = cp.replace('val createdAt: Long = System.currentTimeMillis()', 'val imageUri: String? = null,\n    val createdAt: Long = System.currentTimeMillis()');
    fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/model/ComercioProduct.kt', cp, 'utf-8');
    console.log('ComercioProduct: imageUri added');
}

// 2. Update ComercioPedido model - add imageUri
let ped = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/model/ComercioPedido.kt', 'utf-8');
if (!ped.includes('imageUri')) {
    ped = ped.replace('val timestamp: Long = System.currentTimeMillis()', 'val imageUri: String? = null,\n    val timestamp: Long = System.currentTimeMillis()');
    fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/model/ComercioPedido.kt', ped, 'utf-8');
    console.log('ComercioPedido: imageUri added');
}

// 3. Add migration 20->21
let mig = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/data/Migrations.kt', 'utf-8');
if (!mig.includes('MIGRATION_20_21')) {
    mig = mig.trimEnd() + '\nval MIGRATION_20_21 = object : Migration(20, 21) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE comercio_products ADD COLUMN imageUri TEXT DEFAULT NULL"); db.execSQL("ALTER TABLE comercio_pedidos ADD COLUMN imageUri TEXT DEFAULT NULL") } }\n';
    fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/data/Migrations.kt', mig, 'utf-8');
    console.log('Migration 20->21 added');
}

// 4. Update AppDatabase version
let db = fs.readFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/data/AppDatabase.kt', 'utf-8');
if (!db.includes('MIGRATION_20_21')) {
    db = db.replace('version = 20', 'version = 21');
    db = db.replace('MIGRATION_19_20)', 'MIGRATION_19_20, MIGRATION_20_21)');
    fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/data/AppDatabase.kt', db, 'utf-8');
    console.log('AppDatabase updated to v21');
}

console.log('All DB changes done.');
