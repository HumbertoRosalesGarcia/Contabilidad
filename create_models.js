const fs = require('fs');

// 1. ComercioProduct.kt
fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/model/ComercioProduct.kt', `package com.xxcamixx.contabilidad.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comercio_products")
data class ComercioProduct(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val unit: String = "Uds",
    val quantityInStock: Double,
    val totalPurchased: Double,
    val totalSold: Double = 0.0,
    val costPerUnit: Double,
    val salePricePerUnit: Double = 0.0,
    val country: String = "Colombia",
    val createdAt: Long = System.currentTimeMillis()
)
`, 'utf-8');

// 2. ComercioMovement.kt
fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/model/ComercioMovement.kt', `package com.xxcamixx.contabilidad.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comercio_movements")
data class ComercioMovement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productName: String,
    val type: String,
    val quantity: Double,
    val pricePerUnit: Double,
    val total: Double,
    val note: String = "",
    val country: String = "Colombia",
    val timestamp: Long = System.currentTimeMillis()
)
`, 'utf-8');

console.log('Modelos creados');
