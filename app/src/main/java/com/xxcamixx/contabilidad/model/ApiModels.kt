package com.xxcamixx.contabilidad.model

data class BackupData(
    val transactions: List<Transaction> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val fiadores: List<Fiador> = emptyList(),
    val products: List<Product> = emptyList(),
    val comercioProducts: List<ComercioProduct> = emptyList(),
    val comercioPedidos: List<ComercioPedido> = emptyList(),
    val comercioMovements: List<ComercioMovement> = emptyList()
)
data class BackupRecord(val id: String, val name: String, val timestamp: Long, val data: BackupData)
data class CloudPayload(
    val backups: List<BackupRecord>? = null,
    val transactions: List<Transaction>? = null,
    val reminders: List<Reminder>? = null,
    val fiadores: List<Fiador>? = null,
    val products: List<Product>? = null,
    val comercioProducts: List<ComercioProduct>? = null,
    val comercioPedidos: List<ComercioPedido>? = null,
    val comercioMovements: List<ComercioMovement>? = null
)
data class SyncResponse(val code: String, val message: String)

data class UserSyncRequest(val email: String, val name: String, val profileImage: String? = null, val lastActive: Long = 0L)
data class UserSyncResponse(val role: String?, val isBanned: Boolean, val consumedSeconds: Long, val planDuration: Long)
data class UserTimeResponse(val code: String?, val role: String?, val consumedSeconds: Long, val planDuration: Long, val isBanned: Boolean)
data class UserData(val name: String = "Usuario", val role: String = "INVITADO", val registeredAt: Long = 0L, val consumedSeconds: Long = 0L, val isBanned: Boolean = false, val planDuration: Long = 2592000L, val profileImage: String? = null, val lastActive: Long = 0L)
data class UserTimeRequest(val email: String, val seconds: Long)
data class UserManageRequest(val email: String, val action: String, val role: String? = null, val planDuration: Long? = null)

data class ChatMessage(val sender: String, val text: String, val imageUrl: String? = null, val timestamp: Long)
data class ChatSendRequest(val sender: String, val receiver: String, val text: String, val imageUrl: String? = null)

// --- NUEVOS MODELOS Y FUNCIONES PARA MONEDA ---
data class BcvResponse(val code: String, val tasa: Double)