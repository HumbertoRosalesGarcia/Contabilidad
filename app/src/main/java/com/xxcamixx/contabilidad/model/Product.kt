package com.xxcamixx.contabilidad.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val purchasePrice: Double = 0.0,
    val price: Double,
    val stock: Int,
    val unit: String = "Uds",
    val expirationDateInMillis: Long? = null,
    val entryDateInMillis: Long = System.currentTimeMillis(),
    val minStock: Int = 0,
    val imageUri: String? = null,
    val country: String = "Colombia"
)