package com.xxcamixx.contabilidad.model

data class BackupData(
    val transactions: List<Transaction> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val fiadores: List<Fiador> = emptyList(),
    val products: List<Product> = emptyList(),
    val comercioProducts: List<ComercioProduct> = emptyList(),
    val comercioPedidos: List<ComercioPedido> = emptyList(),
    val comercioMovements: List<ComercioMovement> = emptyList(),
    val cierreSessions: List<CierreSession> = emptyList()
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
    val comercioMovements: List<ComercioMovement>? = null,
    val cierreSessions: List<CierreSession>? = null
)
data class SyncResponse(val code: String, val message: String)

data class UserSyncRequest(val email: String, val name: String, val profileImage: String? = null, val lastActive: Long = 0L)
data class UserSyncResponse(val role: String?, val isBanned: Boolean, val consumedSeconds: Long, val planDuration: Long, val registeredAt: Long = 0L)
data class UserTimeResponse(val code: String?, val role: String?, val consumedSeconds: Long, val planDuration: Long, val isBanned: Boolean, val registeredAt: Long = 0L)
data class UserData(val name: String = "Usuario", val role: String = "INVITADO", val registeredAt: Long = 0L, val consumedSeconds: Long = 0L, val isBanned: Boolean = false, val planDuration: Long = 2592000L, val profileImage: String? = null, val lastActive: Long = 0L)
data class UserTimeRequest(val email: String, val seconds: Long = 0L, val name: String? = null, val profileImage: String? = null)
data class UserManageRequest(val email: String, val action: String, val role: String? = null, val planDuration: Long? = null)

data class ChatMessage(val sender: String, val text: String, val imageUrl: String? = null, val timestamp: Long)
data class ChatSendRequest(val sender: String, val receiver: String, val text: String, val imageUrl: String? = null)

// --- NUEVOS MODELOS Y FUNCIONES PARA MONEDA ---
data class BcvResponse(val code: String, val tasa: Double)

// --- MODELOS PARA PANEL DE CONTROL BACKEND (ADMIN) ---
data class BackupFileInfo(
    val userId: String,
    val sizeBytes: Long = 0L,
    val lastModified: Double = 0.0,
    val backupCount: Int = 0
)

data class BackupsSummaryResponse(
    val code: String = "000000",
    val totalFiles: Int = 0,
    val backups: List<BackupFileInfo> = emptyList()
)

data class RamStats(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val usagePercent: Double = 0.0
)

data class DiskStats(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val usagePercent: Double = 0.0
)

data class CpuStats(
    val cores: Int = 0,
    val model: String = "Desconocido",
    val speedMhz: Double = 0.0,
    val loadAvg: List<Double> = emptyList()
)

data class UptimeStats(
    val systemSeconds: Long = 0L,
    val processSeconds: Long = 0L
)

data class SystemInfoStats(
    val platform: String = "",
    val arch: String = "",
    val release: String = "",
    val nodeVersion: String = "",
    val processMemoryBytes: Long = 0L,
    val backupsDirBytes: Long = 0L,
    val backupsFilesCount: Int = 0
)

data class ServerStatsResponse(
    val code: String = "000000",
    val timestamp: Long = 0L,
    val ram: RamStats = RamStats(),
    val disk: DiskStats = DiskStats(),
    val cpu: CpuStats = CpuStats(),
    val uptime: UptimeStats = UptimeStats(),
    val system: SystemInfoStats = SystemInfoStats()
)

data class DiskItemInfo(
    val name: String = "",
    val path: String = "",
    val isDirectory: Boolean = false,
    val sizeBytes: Long = 0L,
    val modifiedAt: Double = 0.0
)

data class DiskExplorerResponse(
    val code: String = "000000",
    val currentPath: String = "/",
    val parentPath: String? = null,
    val totalDiskBytes: Long = 0L,
    val usedDiskBytes: Long = 0L,
    val freeDiskBytes: Long = 0L,
    val items: List<DiskItemInfo> = emptyList()
)