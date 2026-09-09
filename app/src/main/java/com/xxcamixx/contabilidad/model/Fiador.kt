package com.xxcamixx.contabilidad.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fiadores")
data class Fiador(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String = "",
    val amount: Double,
    val reason: String,
    val targetDateInMillis: Long,
    val paidAmount: Double = 0.0,
    val paymentHistory: String = "",
    val isStore: Boolean = true,
    val totalCost: Double = 0.0,
    val country: String = "Colombia",
    val originMode: String = "TIENDA",
    val cierreId: Int? = null
)