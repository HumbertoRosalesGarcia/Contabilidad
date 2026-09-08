package com.xxcamixx.contabilidad.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comercio_pedidos")
data class ComercioPedido(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val country: String = "Colombia",
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
