const fs = require('fs');

// 1. Modelo ComercioPedido
fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/model/ComercioPedido.kt', `package com.xxcamixx.contabilidad.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comercio_pedidos")
data class ComercioPedido(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val country: String = "Colombia",
    val timestamp: Long = System.currentTimeMillis()
)
`, 'utf-8');

console.log('Modelo ComercioPedido creado.');
