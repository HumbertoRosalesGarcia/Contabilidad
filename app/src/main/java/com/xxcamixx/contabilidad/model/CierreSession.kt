package com.xxcamixx.contabilidad.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cierre_sessions")
data class CierreSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mode: String,
    val name: String,
    val totalIncomes: Double,
    val totalExpenses: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val country: String = "Colombia"
)
