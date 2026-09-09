package com.xxcamixx.contabilidad.model

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
    val timestamp: Long = System.currentTimeMillis(),
    val transactionId: String? = null
)
