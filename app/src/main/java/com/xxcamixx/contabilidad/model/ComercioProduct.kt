package com.xxcamixx.contabilidad.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comercio_products")
data class ComercioProduct(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pedidoId: Int = 0,
    val name: String,
    val unit: String = "Uds",
    val quantityInStock: Double,
    val totalPurchased: Double,
    val totalSold: Double = 0.0,
    val costPerUnit: Double,
    val salePricePerUnit: Double = 0.0,
    val country: String = "Colombia",
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
