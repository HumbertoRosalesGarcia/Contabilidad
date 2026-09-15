package com.xxcamixx.contabilidad.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val amount: Double,
    val isIncome: Boolean,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val profit: Double = 0.0,
    val cashAmount: Double = 0.0,
    val digitalAmount: Double = 0.0,
    val country: String = "Colombia",
    val category: String? = null, // <-- NUEVO
    val imageUri: String? = null,  // <-- NUEVO
    val cierreId: Int? = null
)