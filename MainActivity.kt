package com.xxcamixx.contabilidad

// --- Importaciones Base ---
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

// --- UI y Jetpack Compose ---
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel

// --- Room ---
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// --- Corrutinas, Retrofit y WorkManager ---
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import androidx.work.*

// --- Utilidades ---
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.abs

// --- Google Credential Manager ---
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

// ==========================================
// 0. API RETROFIT PARA SINCRONIZACIÓN, ROLES Y CHAT
// ==========================================
data class BackupData(val transactions: List<Transaction> = emptyList(), val reminders: List<Reminder> = emptyList(), val fiadores: List<Fiador> = emptyList(), val products: List<Product> = emptyList())
data class BackupRecord(val id: String, val name: String, val timestamp: Long, val data: BackupData)
data class CloudPayload(val backups: List<BackupRecord>? = null, val transactions: List<Transaction>? = null, val reminders: List<Reminder>? = null, val fiadores: List<Fiador>? = null, val products: List<Product>? = null)
data class SyncResponse(val code: String, val message: String)

data class UserSyncRequest(val email: String, val name: String)
data class UserSyncResponse(val role: String?, val isBanned: Boolean, val consumedSeconds: Long, val planDuration: Long)
data class UserTimeResponse(val code: String?, val role: String?, val consumedSeconds: Long, val planDuration: Long, val isBanned: Boolean)
data class UserData(val name: String = "Usuario", val role: String = "INVITADO", val registeredAt: Long = 0L, val consumedSeconds: Long = 0L, val isBanned: Boolean = false, val planDuration: Long = 2592000L)
data class UserTimeRequest(val email: String, val seconds: Long)
data class UserManageRequest(val email: String, val action: String, val role: String? = null, val planDuration: Long? = null)

data class ChatMessage(val sender: String, val text: String, val timestamp: Long)
data class ChatSendRequest(val sender: String, val receiver: String, val text: String)

interface ServerApi {
    @POST("api/backup/{userId}") suspend fun uploadBackup(@Path("userId") userId: String, @Body data: CloudPayload): SyncResponse
    @GET("api/backup/{userId}") suspend fun getBackup(@Path("userId") userId: String): CloudPayload?
    @POST("api/users/sync") suspend fun syncUser(@Body request: UserSyncRequest): UserSyncResponse
    @POST("api/users/time") suspend fun addUserTime(@Body request: UserTimeRequest): UserTimeResponse
    @GET("api/users") suspend fun getAllUsers(): Map<String, UserData>
    @POST("api/users/manage") suspend fun manageUser(@Body request: UserManageRequest): SyncResponse
    @GET("api/chat/{email}") suspend fun getChat(@Path("email") email: String): List<ChatMessage>
    @GET("api/admin/chats") suspend fun getAllChats(): Map<String, List<ChatMessage>>
    @POST("api/chat") suspend fun sendMessage(@Body req: ChatSendRequest): SyncResponse
    @DELETE("api/chat/{email}") suspend fun clearChat(@Path("email") email: String): SyncResponse
}

object RetrofitInstance {
    val api: ServerApi by lazy { Retrofit.Builder().baseUrl("http://158.247.123.136:3000/").addConverterFactory(GsonConverterFactory.create()).build().create(ServerApi::class.java) }
}

class CloudSyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val userId = inputData.getString("USER_ID") ?: return Result.failure()
        val db = AppDatabase.getDatabase(applicationContext, userId).financeDao()
        return try {
            val transactions = db.getAllTransactions().firstOrNull() ?: emptyList(); val reminders = db.getAllReminders().firstOrNull() ?: emptyList(); val fiadores = db.getAllFiadores().firstOrNull() ?: emptyList(); val products = db.getAllProducts().firstOrNull() ?: emptyList()
            val newData = BackupData(transactions, reminders, fiadores, products); val timeString = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val newRecord = BackupRecord(UUID.randomUUID().toString(), "Automático - $timeString", System.currentTimeMillis(), newData)
            val remotePayload = RetrofitInstance.api.getBackup(userId); val existingBackups = mutableListOf<BackupRecord>()
            if (remotePayload != null) { if (remotePayload.backups != null) { existingBackups.addAll(remotePayload.backups) } else if (remotePayload.transactions != null) { existingBackups.add(BackupRecord("old", "Respaldo Antiguo", 0L, BackupData(remotePayload.transactions, remotePayload.reminders ?: emptyList(), remotePayload.fiadores ?: emptyList(), remotePayload.products ?: emptyList()))) } }
            existingBackups.add(0, newRecord)
            if (existingBackups.size > 15) { existingBackups.removeAt(existingBackups.size - 1) }
            RetrofitInstance.api.uploadBackup(userId, CloudPayload(backups = existingBackups))
            applicationContext.getSharedPreferences("FinancePrefs_$userId", Context.MODE_PRIVATE).edit().putLong("lastSync", System.currentTimeMillis()).apply()
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }
}

// ==========================================
// 1. MOTOR DE SONIDOS Y NOTIFICACIONES
// ==========================================
object AppSounds {
    private var toneGen: ToneGenerator? = null
    fun init() { if(toneGen == null) toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    fun play(soundType: Int) {
        if (soundType == 0) return
        try { when(soundType) { 1 -> toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 150); 2 -> toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP2, 150); 3 -> toneGen?.startTone(ToneGenerator.TONE_SUP_RADIO_ACK, 150); 4 -> toneGen?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 150); 5 -> toneGen?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150); 6 -> toneGen?.startTone(ToneGenerator.TONE_PROP_PROMPT, 150); 7 -> toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 150); 8 -> toneGen?.startTone(ToneGenerator.TONE_PROP_NACK, 150); 9 -> toneGen?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 150); 10 -> toneGen?.startTone(ToneGenerator.TONE_SUP_BUSY, 150) } } catch(_: Exception){}
    }
}

fun showChatNotification(context: Context, title: String, text: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "chat_notifications"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Mensajes de Chat", NotificationManager.IMPORTANCE_HIGH).apply { enableVibration(true) }
        notificationManager.createNotificationChannel(channel)
    }
    val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val notification = NotificationCompat.Builder(context, channelId).setSmallIcon(android.R.drawable.ic_dialog_email).setContentTitle(title).setContentText(text).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(pendingIntent).build()
    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    AppSounds.play(6)
}

@SuppressLint("ScheduleExactAlarm")
fun scheduleNextChatSync(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java).apply { action = "com.xxcamixx.contabilidad.CHAT_SYNC" }
    val pendingIntent = PendingIntent.getBroadcast(context, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val triggerTime = System.currentTimeMillis() + 60000L
    try {
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    } catch (_: Exception) {}
}

// ==========================================
// 2. BASE DE DATOS (ROOM)
// ==========================================
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
    val digitalAmount: Double = 0.0
)

@Entity(tableName = "reminders") data class Reminder(@PrimaryKey(autoGenerate = true) val id: Int = 0, val title: String, val targetDateInMillis: Long)
@Entity(tableName = "fiadores") data class Fiador(@PrimaryKey(autoGenerate = true) val id: Int = 0, val name: String, val phone: String = "", val amount: Double, val reason: String, val targetDateInMillis: Long, val paidAmount: Double = 0.0, val paymentHistory: String = "")
@Entity(tableName = "products") data class Product(@PrimaryKey(autoGenerate = true) val id: Int = 0, val name: String, val purchasePrice: Double = 0.0, val price: Double, val stock: Int, val unit: String = "Uds", val expirationDateInMillis: Long? = null, val entryDateInMillis: Long = System.currentTimeMillis(), val minStock: Int = 0)

@Dao
interface FinanceDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC") fun getAllTransactions(): Flow<List<Transaction>>
    @Insert suspend fun insertTransaction(transaction: Transaction)
    @Delete suspend fun deleteTransaction(transaction: Transaction)
    @Query("DELETE FROM transactions") suspend fun deleteAllTransactions()
    @Query("DELETE FROM transactions WHERE description NOT LIKE 'Venta: %'") suspend fun deletePersonalTransactions()
    @Query("UPDATE transactions SET profit = 0.0, cashAmount = 0.0, digitalAmount = 0.0") suspend fun resetAllProfits()

    @Query("SELECT * FROM reminders ORDER BY targetDateInMillis ASC") fun getAllReminders(): Flow<List<Reminder>>
    @Insert suspend fun insertReminder(reminder: Reminder): Long
    @Update suspend fun updateReminder(reminder: Reminder)
    @Delete suspend fun deleteReminder(reminder: Reminder)
    @Query("DELETE FROM reminders") suspend fun deleteAllReminders()

    @Query("SELECT * FROM fiadores ORDER BY targetDateInMillis ASC") fun getAllFiadores(): Flow<List<Fiador>>
    @Insert suspend fun insertFiador(fiador: Fiador): Long
    @Update suspend fun updateFiador(fiador: Fiador)
    @Delete suspend fun deleteFiador(fiador: Fiador)
    @Query("DELETE FROM fiadores") suspend fun deleteAllFiadores()

    @Query("SELECT * FROM products ORDER BY name ASC") fun getAllProducts(): Flow<List<Product>>
    @Insert suspend fun insertProduct(product: Product): Long
    @Update suspend fun updateProduct(product: Product)
    @Delete suspend fun deleteProduct(product: Product)
    @Query("DELETE FROM products") suspend fun deleteAllProducts()
}

val MIGRATION_4_5 = object : Migration(4, 5) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `price` REAL NOT NULL, `stock` INTEGER NOT NULL, `expirationDateInMillis` INTEGER)") } }
val MIGRATION_5_6 = object : Migration(5, 6) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `products` ADD COLUMN `entryDateInMillis` INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}") } }
val MIGRATION_6_7 = object : Migration(6, 7) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `fiadores` ADD COLUMN `phone` TEXT NOT NULL DEFAULT ''") } }
val MIGRATION_7_8 = object : Migration(7, 8) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `products` ADD COLUMN `unit` TEXT NOT NULL DEFAULT 'Uds'") } }
val MIGRATION_8_9 = object : Migration(8, 9) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `products` ADD COLUMN `purchasePrice` REAL NOT NULL DEFAULT 0.0"); db.execSQL("ALTER TABLE `transactions` ADD COLUMN `profit` REAL NOT NULL DEFAULT 0.0") } }
val MIGRATION_9_10 = object : Migration(9, 10) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `products` ADD COLUMN `minStock` INTEGER NOT NULL DEFAULT 0") } }
val MIGRATION_10_11 = object : Migration(10, 11) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `transactions` ADD COLUMN `cashAmount` REAL NOT NULL DEFAULT 0.0"); db.execSQL("ALTER TABLE `transactions` ADD COLUMN `digitalAmount` REAL NOT NULL DEFAULT 0.0") } }
val MIGRATION_11_12 = object : Migration(11, 12) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `fiadores` ADD COLUMN `paidAmount` REAL NOT NULL DEFAULT 0.0"); db.execSQL("ALTER TABLE `fiadores` ADD COLUMN `paymentHistory` TEXT NOT NULL DEFAULT ''") } }

@Database(entities = [Transaction::class, Reminder::class, Fiador::class, Product::class], version = 12, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
    companion object {
        @Volatile private var INSTANCES = mutableMapOf<String, AppDatabase>()
        fun getDatabase(context: Context, userId: String): AppDatabase {
            return INSTANCES[userId] ?: synchronized(this) {
                val dbName = "finance_database_$userId"
                val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, dbName)
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    .build()
                INSTANCES[userId] = instance
                instance
            }
        }
    }
}

// ==========================================
// 3. VIEWMODEL Y LÓGICA
// ==========================================
class FinanceViewModel(application: Application, val userId: String) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application, userId).financeDao()
    private val userPrefs = application.getSharedPreferences("FinancePrefs_$userId", Context.MODE_PRIVATE)

    val transactions: Flow<List<Transaction>> = dao.getAllTransactions()
    val reminders: Flow<List<Reminder>> = dao.getAllReminders()
    val fiadores: Flow<List<Fiador>> = dao.getAllFiadores()
    val products: Flow<List<Product>> = dao.getAllProducts()

    var minBalanceThreshold by mutableStateOf(userPrefs.getFloat("minBalance", 0f).toDouble()); private set
    var selectedSound by mutableStateOf(userPrefs.getInt("selectedSound", 1)); private set
    var isSyncing by mutableStateOf(false); private set
    var lastSyncDate by mutableStateOf(userPrefs.getLong("lastSync", 0L)); private set
    var autoSyncFrequency by mutableStateOf(userPrefs.getInt("syncFrequency", 0)); private set
    var autoSyncHour by mutableStateOf(userPrefs.getInt("syncHour", 2)); private set
    var autoSyncMinute by mutableStateOf(userPrefs.getInt("syncMinute", 0)); private set

    init { scheduleAutoSync(application, autoSyncFrequency, autoSyncHour, autoSyncMinute) }

    fun updateMinBalance(amount: Double) { minBalanceThreshold = amount; userPrefs.edit().putFloat("minBalance", amount.toFloat()).apply() }
    fun updateSoundPreference(soundIndex: Int) { selectedSound = soundIndex; userPrefs.edit().putInt("selectedSound", soundIndex).apply(); AppSounds.play(soundIndex) }

    fun updateAutoSyncSchedule(application: Application, frequencyDays: Int, hour: Int, minute: Int) {
        autoSyncFrequency = frequencyDays; autoSyncHour = hour; autoSyncMinute = minute
        userPrefs.edit().putInt("syncFrequency", frequencyDays).putInt("syncHour", hour).putInt("syncMinute", minute).apply()
        scheduleAutoSync(application, frequencyDays, hour, minute)
    }

    private fun scheduleAutoSync(application: Application, frequencyDays: Int, hour: Int, minute: Int) {
        val workManager = WorkManager.getInstance(application)
        if (frequencyDays <= 0 || userId == "guest_user") { workManager.cancelUniqueWork("CloudSync_$userId") } else {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0) }
            if (target.before(now)) { target.add(Calendar.DAY_OF_MONTH, 1) }
            val initialDelay = target.timeInMillis - now.timeInMillis
            val syncRequest = PeriodicWorkRequestBuilder<CloudSyncWorker>(frequencyDays.toLong(), TimeUnit.DAYS).setInitialDelay(initialDelay, TimeUnit.MILLISECONDS).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setInputData(workDataOf("USER_ID" to userId)).build()
            workManager.enqueueUniquePeriodicWork("CloudSync_$userId", ExistingPeriodicWorkPolicy.REPLACE, syncRequest)
        }
    }

    fun manualBackup(backupName: String, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            isSyncing = true
            try {
                val currentData = BackupData(transactions.firstOrNull() ?: emptyList(), reminders.firstOrNull() ?: emptyList(), fiadores.firstOrNull() ?: emptyList(), products.firstOrNull() ?: emptyList())
                val newRecord = BackupRecord(UUID.randomUUID().toString(), backupName, System.currentTimeMillis(), currentData)
                val remotePayload = RetrofitInstance.api.getBackup(userId)
                val existingBackups = mutableListOf<BackupRecord>()
                if (remotePayload != null) {
                    if (remotePayload.backups != null) { existingBackups.addAll(remotePayload.backups) }
                    else if (remotePayload.transactions != null) { existingBackups.add(BackupRecord("old", "Respaldo Antiguo", 0L, BackupData(remotePayload.transactions, remotePayload.reminders ?: emptyList(), remotePayload.fiadores ?: emptyList(), remotePayload.products ?: emptyList()))) }
                }
                existingBackups.add(0, newRecord)
                if (existingBackups.size > 15) { existingBackups.removeAt(existingBackups.size - 1) }
                RetrofitInstance.api.uploadBackup(userId, CloudPayload(backups = existingBackups))
                val now = System.currentTimeMillis()
                userPrefs.edit().putLong("lastSync", now).apply()
                launch(Dispatchers.Main) { lastSyncDate = now; onResult("¡Respaldo '$backupName' guardado! ☁️✅"); isSyncing = false }
            } catch (e: Exception) { launch(Dispatchers.Main) { onResult("Error al subir el respaldo: ${e.message}"); isSyncing = false } }
        }
    }

    fun fetchBackupList(onResult: (List<BackupRecord>?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val remotePayload = RetrofitInstance.api.getBackup(userId)
                val list = mutableListOf<BackupRecord>()
                if (remotePayload != null) {
                    if (remotePayload.backups != null) { list.addAll(remotePayload.backups) }
                    else if (remotePayload.transactions != null) { list.add(BackupRecord("old", "Respaldo Antiguo", 0L, BackupData(remotePayload.transactions, remotePayload.reminders ?: emptyList(), remotePayload.fiadores ?: emptyList(), remotePayload.products ?: emptyList()))) }
                }
                launch(Dispatchers.Main) { onResult(if (list.isEmpty()) null else list) }
            } catch (_: Exception) { launch(Dispatchers.Main) { onResult(null) } }
        }
    }

    fun deleteBackupRecord(recordId: String, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val remotePayload = RetrofitInstance.api.getBackup(userId)
                if (remotePayload != null && remotePayload.backups != null) {
                    val updatedBackups = remotePayload.backups.filter { it.id != recordId }
                    RetrofitInstance.api.uploadBackup(userId, CloudPayload(backups = updatedBackups))
                    launch(Dispatchers.Main) { onResult("Copia de seguridad eliminada 🗑️") }
                }
            } catch (_: Exception) { launch(Dispatchers.Main) { onResult("Error al eliminar la copia.") } }
        }
    }

    fun restoreFromRecord(record: BackupRecord, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            isSyncing = true
            try {
                dao.deleteAllTransactions(); dao.deleteAllReminders(); dao.deleteAllFiadores(); dao.deleteAllProducts()
                record.data.transactions.forEach { dao.insertTransaction(it.copy(id = 0)) }
                record.data.reminders.forEach { dao.insertReminder(it.copy(id = 0)) }
                record.data.fiadores.forEach { dao.insertFiador(it.copy(id = 0)) }
                record.data.products.forEach { dao.insertProduct(it.copy(id = 0)) }
                launch(Dispatchers.Main) { onResult("¡Respaldo '${record.name}' restaurado! ☁️📥"); isSyncing = false }
            } catch (_: Exception) { launch(Dispatchers.Main) { onResult("Error al restaurar los datos."); isSyncing = false } }
        }
    }

    fun addTransaction(description: String, amount: Double, isIncome: Boolean, note: String) { viewModelScope.launch { dao.insertTransaction(Transaction(description = description, amount = amount, isIncome = isIncome, note = note)) }; AppSounds.play(selectedSound) }
    fun insertRawTransaction(transaction: Transaction) { viewModelScope.launch { dao.insertTransaction(transaction.copy(id = 0)) } }
    fun deleteTransaction(transaction: Transaction) { viewModelScope.launch { dao.deleteTransaction(transaction) }; AppSounds.play(selectedSound) }
    fun deleteTransactionsList(list: List<Transaction>) { viewModelScope.launch { list.forEach { dao.deleteTransaction(it) } }; AppSounds.play(selectedSound) }
    fun deletePersonalTransactions() { viewModelScope.launch { dao.deletePersonalTransactions() } }
    fun resetAllProfits() { viewModelScope.launch { dao.resetAllProfits() }; AppSounds.play(selectedSound) }

    fun addProduct(name: String, purchasePrice: Double, price: Double, stock: Int, unit: String, expirationDateInMillis: Long?, minStock: Int, context: Context, onConfigured: (String) -> Unit) { viewModelScope.launch { val productId = dao.insertProduct(Product(name = name, purchasePrice = purchasePrice, price = price, stock = stock, unit = unit, expirationDateInMillis = expirationDateInMillis, minStock = minStock)).toInt(); if (expirationDateInMillis != null) scheduleNotification(context, expirationDateInMillis, "¡Producto por Vencer! ⚠️", "El producto $name ha alcanzado su fecha de caducidad.", productId + 200000, "EXPIRE_TRIGGER"); onConfigured("Producto guardado en inventario") }; AppSounds.play(selectedSound) }
    fun editProduct(product: Product, context: Context, onConfigured: (String) -> Unit) { viewModelScope.launch { dao.updateProduct(product); cancelAlarm(context, product.id + 200000, "EXPIRE_TRIGGER"); if (product.expirationDateInMillis != null) scheduleNotification(context, product.expirationDateInMillis, "¡Producto por Vencer! ⚠️", "El producto ${product.name} ha alcanzado su fecha de caducidad.", product.id + 200000, "EXPIRE_TRIGGER"); onConfigured("Producto actualizado") }; AppSounds.play(selectedSound) }
    fun deleteProductEntirely(product: Product, context: Context) { viewModelScope.launch { dao.deleteProduct(product); if (product.expirationDateInMillis != null) cancelAlarm(context, product.id + 200000, "EXPIRE_TRIGGER") }; AppSounds.play(selectedSound) }

    fun processCartSale(cartItems: List<Pair<Product, Int>>, buyerName: String, paymentSummary: String, netCash: Double, netDigital: Double, context: Context, onSold: (String) -> Unit) {
        viewModelScope.launch {
            var totalSaleCOP = 0.0; var totalProfitCOP = 0.0; val itemNames = mutableListOf<String>()
            cartItems.forEach { (product, qty) -> val newStock = product.stock - qty; dao.updateProduct(product.copy(stock = newStock)); totalSaleCOP += (product.price * qty); totalProfitCOP += ((product.price - product.purchasePrice) * qty); itemNames.add("${qty}${product.unit} ${product.name}"); if (product.minStock > 0 && newStock <= product.minStock && product.stock > product.minStock) scheduleNotification(context, System.currentTimeMillis() + 1000L, "¡Stock Crítico! ⚠️", "El producto ${product.name} tiene solo $newStock unidades restantes.", product.id + 300000, "STOCK_TRIGGER") }
            val finalNote = buildString { if (buyerName.isNotBlank()) append("Cliente: $buyerName\n"); append("$paymentSummary\n"); append("Items: ${itemNames.joinToString(", ")}") }; val desc = if (cartItems.size == 1) "Venta: ${cartItems.first().first.name}" else "Venta: Varios Productos"
            dao.insertTransaction(Transaction(description = desc, amount = totalSaleCOP, isIncome = true, note = finalNote, profit = totalProfitCOP, cashAmount = netCash, digitalAmount = netDigital)); onSold("Venta registrada exitosamente"); AppSounds.play(selectedSound)
        }
    }

    fun reduceProductStock(product: Product, qty: Int, context: Context) { viewModelScope.launch { val newStock = product.stock - qty; if (newStock <= 0) { dao.deleteProduct(product); if (product.expirationDateInMillis != null) cancelAlarm(context, product.id + 200000, "EXPIRE_TRIGGER") } else { dao.updateProduct(product.copy(stock = newStock)); if (product.minStock > 0 && newStock <= product.minStock && product.stock > product.minStock) scheduleNotification(context, System.currentTimeMillis() + 1000L, "¡Stock Crítico! ⚠️", "El producto ${product.name} tiene solo $newStock unidades restantes.", product.id + 300000, "STOCK_TRIGGER") } }; AppSounds.play(selectedSound) }
    fun restoreProductStock(product: Product, qty: Int, context: Context) { viewModelScope.launch { val currentInDb = dao.getAllProducts().firstOrNull()?.find { it.id == product.id }; if (currentInDb != null) dao.updateProduct(currentInDb.copy(stock = currentInDb.stock + qty)) else { dao.insertProduct(product); if (product.expirationDateInMillis != null) scheduleNotification(context, product.expirationDateInMillis, "¡Producto por Vencer! ⚠️", "El producto ${product.name} ha alcanzado su fecha de caducidad.", product.id + 200000, "EXPIRE_TRIGGER") } } }
    fun addReminder(title: String, dateInMillis: Long, context: Context, onConfigured: (String) -> Unit) { viewModelScope.launch { val reminderId = dao.insertReminder(Reminder(title = title, targetDateInMillis = dateInMillis)).toInt(); val success = scheduleNotification(context, dateInMillis, "¡Hora de Pagar! ⏰", title, reminderId, "ALARM_TRIGGER"); if (success) onConfigured("Alarma programada para las ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(dateInMillis))}") }; AppSounds.play(selectedSound) }
    fun updateExistingReminder(reminder: Reminder, context: Context, onConfigured: (String) -> Unit) { viewModelScope.launch { dao.updateReminder(reminder); val success = scheduleNotification(context, reminder.targetDateInMillis, "¡Hora de Pagar! ⏰", reminder.title, reminder.id, "ALARM_TRIGGER"); if (success) onConfigured("Alarma actualizada para las ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(reminder.targetDateInMillis))}") }; AppSounds.play(selectedSound) }
    fun deleteReminder(reminder: Reminder, context: Context) { viewModelScope.launch { dao.deleteReminder(reminder); cancelAlarm(context, reminder.id, "ALARM_TRIGGER") } }
    fun addFiador(name: String, phone: String, cartItems: List<Pair<Product, Int>>, dateInMillis: Long, context: Context, onConfigured: (String) -> Unit) { viewModelScope.launch { val totalAmount = cartItems.sumOf { it.first.price * it.second }; val reason = cartItems.joinToString(", ") { "${it.second}${it.first.unit} ${it.first.name}" }; val fiadorId = dao.insertFiador(Fiador(name = name, phone = phone, amount = totalAmount, reason = reason, targetDateInMillis = dateInMillis)).toInt(); cartItems.forEach { (product, qty) -> val newStock = product.stock - qty; if(newStock >= 0) { dao.updateProduct(product.copy(stock = newStock)); if (product.minStock > 0 && newStock <= product.minStock && product.stock > product.minStock) scheduleNotification(context, System.currentTimeMillis() + 1000L, "¡Stock Crítico! ⚠️", "El producto ${product.name} tiene solo $newStock unidades restantes.", product.id + 300000, "STOCK_TRIGGER") } }; val success = scheduleNotification(context, dateInMillis, "¡Cobrar a $name! 💰", "Monto: ${formatCOP(totalAmount)} - $reason", fiadorId + 100000, "FIADOR_TRIGGER"); if (success) onConfigured("Recordatorio de fiador para las ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(dateInMillis))}") }; AppSounds.play(selectedSound) }
    fun updateExistingFiador(fiador: Fiador, context: Context, onConfigured: (String) -> Unit) { viewModelScope.launch { dao.updateFiador(fiador); val success = scheduleNotification(context, fiador.targetDateInMillis, "¡Cobrar a ${fiador.name}! 💰", "Monto: ${formatCOP(fiador.amount - fiador.paidAmount)} - ${fiador.reason}", fiador.id + 100000, "FIADOR_TRIGGER"); if (success) onConfigured("Recordatorio actualizado para las ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(fiador.targetDateInMillis))}") }; AppSounds.play(selectedSound) }
    fun deleteFiador(fiador: Fiador, context: Context) { viewModelScope.launch { dao.deleteFiador(fiador); cancelAlarm(context, fiador.id + 100000, "FIADOR_TRIGGER") } }

    fun registerAbonoFiador(fiador: Fiador, abono: Double, method: String, context: Context, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val newPaidAmount = fiador.paidAmount + abono
            val remaining = fiador.amount - newPaidAmount
            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val historyEntry = "$dateStr: +${formatCOP(abono)} ($method)"
            val newHistory = if (fiador.paymentHistory.isEmpty()) historyEntry else fiador.paymentHistory + "\n$historyEntry"

            val cash = if(method == "Efectivo") abono else 0.0
            val digital = if(method == "Digital") abono else 0.0
            dao.insertTransaction(Transaction(description = "Abono: ${fiador.name}", amount = abono, isIncome = true, note = "Abono de deuda parcial", cashAmount = cash, digitalAmount = digital))

            if (newPaidAmount >= fiador.amount) {
                dao.deleteFiador(fiador)
                cancelAlarm(context, fiador.id + 100000, "FIADOR_TRIGGER")
                launch(Dispatchers.Main) { onResult("¡Deuda de ${fiador.name} saldada por completo! 🎉") }
            } else {
                dao.updateFiador(fiador.copy(paidAmount = newPaidAmount, paymentHistory = newHistory))
                launch(Dispatchers.Main) { onResult("Abono de ${formatCOP(abono)} registrado. Resta: ${formatCOP(remaining)}") }
            }
            AppSounds.play(selectedSound)
        }
    }

    private fun cancelAlarm(context: Context, id: Int, actionPrefix: String) { val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager; val intent = Intent(context, ReminderReceiver::class.java).apply { action = "com.xxcamixx.contabilidad.${actionPrefix}_${id}" }; val pendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); alarmManager.cancel(pendingIntent) }
    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleNotification(context: Context, timeInMillis: Long, notifTitle: String, notifText: String, id: Int, actionPrefix: String): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = Uri.parse("package:${context.packageName}"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent); Toast.makeText(context, "⚠️ Otorga el permiso de Alarmas Exactas.", Toast.LENGTH_LONG).show(); return false
            }
        }
        val intent = Intent(context, ReminderReceiver::class.java).apply { action = "com.xxcamixx.contabilidad.${actionPrefix}_$id"; putExtra("NOTIFICATION_TITLE", notifTitle); putExtra("NOTIFICATION_TEXT", notifText); putExtra("ID", id); addFlags(Intent.FLAG_RECEIVER_FOREGROUND); addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES) }
        val pendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarmClockInfo = AlarmManager.AlarmClockInfo(timeInMillis, pendingIntent); alarmManager.setAlarmClock(alarmClockInfo, pendingIntent); return true
    }
}

class FinanceViewModelFactory(private val application: Application, private val userId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return FinanceViewModel(application, userId) as T
    }
}

// ==========================================
// 4. ACTIVIDAD PRINCIPAL & TEMA
// ==========================================
val CustomDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50), onPrimary = Color.White, secondary = Color(0xFF81C784), onSecondary = Color.Black,
    background = Color(0xFF121212), onBackground = Color(0xFFE0E0E0), surface = Color(0xFF1E1E1E), onSurface = Color(0xFFE0E0E0)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSounds.init()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        scheduleNextChatSync(this)

        setContent {
            val systemTheme = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemTheme) }
            var showSplash by remember { mutableStateOf(true) }

            val authPrefs = getSharedPreferences("GlobalAuthPrefs", Context.MODE_PRIVATE)
            var loggedInUser by remember { mutableStateOf(authPrefs.getString("userName", null)) }
            var loggedInUserId by remember { mutableStateOf(authPrefs.getString("userId", null)) }
            var userRole by remember { mutableStateOf(authPrefs.getString("userRole", "INVITADO") ?: "INVITADO") }
            var consumedSeconds by remember { mutableStateOf(authPrefs.getLong("consumedSeconds", 0L)) }
            var planDuration by remember { mutableStateOf(authPrefs.getLong("planDuration", 2592000L)) }

            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(Unit) { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }

            MaterialTheme(colorScheme = if (isDarkTheme) CustomDarkColorScheme else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (showSplash) {
                        TechSplashScreen(onTimeout = { showSplash = false })
                    } else if (loggedInUserId == null) {
                        LoginScreen(onLoginSuccess = { name, id, role, secs, duration ->
                            authPrefs.edit().putString("userName", name).putString("userId", id).putString("userRole", role).putLong("consumedSeconds", secs).putLong("planDuration", duration).putString("lastKnownUserId", id).putString("lastKnownRole", role).apply()
                            loggedInUser = name; loggedInUserId = id; userRole = role; consumedSeconds = secs; planDuration = duration
                        })
                    } else {
                        val viewModel: FinanceViewModel = viewModel(key = loggedInUserId, factory = FinanceViewModelFactory(application, loggedInUserId!!))
                        FinanceScreen(
                            viewModel = viewModel, userName = loggedInUser ?: "Usuario", initialRole = userRole, initialConsumedSeconds = consumedSeconds, initialPlanDuration = planDuration,
                            onLogout = { authPrefs.edit().remove("userName").remove("userId").remove("userRole").remove("consumedSeconds").remove("planDuration").apply(); loggedInUser = null; loggedInUserId = null; userRole = "INVITADO" },
                            isDarkTheme = isDarkTheme, onThemeToggle = { isDarkTheme = !isDarkTheme }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. ANIMACIÓN DE CARGA Y LOGIN
// ==========================================
@Composable
fun TechSplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) { delay(2200L); onTimeout() }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val infiniteTransition = rememberInfiniteTransition(label = "spin")
        val rotation1 by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing)), label = "spin1")
        val rotation2 by infiniteTransition.animateFloat(initialValue = 360f, targetValue = 0f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing)), label = "spin2")

        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawArc(color = Color(0xFF4CAF50), startAngle = rotation1, sweepAngle = 270f, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)); drawArc(color = Color(0xFF81C784), startAngle = rotation2, sweepAngle = 200f, useCenter = false, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round), size = Size(size.width * 0.75f, size.height * 0.75f), topLeft = Offset(size.width * 0.125f, size.height * 0.125f)) }
            Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(60.dp))
        }
        Text("Accediendo a Billetera...", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp))
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (String, String, String, Long, Long) -> Unit) {
    val context = LocalContext.current; val coroutineScope = rememberCoroutineScope(); val credentialManager = remember { CredentialManager.create(context) }; var isLoading by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.AccountCircle, contentDescription = "Login", modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.height(24.dp)); Text("Acceso a Billetera", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground); Spacer(modifier = Modifier.height(8.dp)); Text("Sincronización segura en la nube", fontSize = 14.sp, color = Color.Gray); Spacer(modifier = Modifier.height(32.dp))
            if (isLoading) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) } else {
                Button(onClick = { isLoading = true; coroutineScope.launch { try { val webClientId = context.getString(context.resources.getIdentifier("default_web_client_id", "string", context.packageName)); val googleIdOption = GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(webClientId).setAutoSelectEnabled(true).build(); val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build(); val result = credentialManager.getCredential(request = request, context = context); val credential = result.credential
                    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) { val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data); val displayName = googleIdTokenCredential.displayName ?: "Usuario"; val userId = googleIdTokenCredential.id
                        val isSuperAdmin = userId.lowercase(Locale.getDefault()) == "zonacami77777@gmail.com"
                        try { val response = RetrofitInstance.api.syncUser(UserSyncRequest(email = userId, name = displayName))
                            val finalRole = if (isSuperAdmin) "ADMIN" else (response.role ?: "INVITADO")
                            if (response.isBanned && !isSuperAdmin) { isLoading = false; Toast.makeText(context, "🚫 Tu cuenta está bloqueada o vencida.", Toast.LENGTH_LONG).show() }
                            else {
                                Toast.makeText(context, "Usted se encuentra bajo el PLAN $finalRole, Bienvenido", Toast.LENGTH_LONG).show()
                                onLoginSuccess(displayName, userId, finalRole, response.consumedSeconds, response.planDuration)
                            }
                        } catch (_: Exception) {
                            val fallbackRole = if (isSuperAdmin) "ADMIN" else "BÁSICO"
                            Toast.makeText(context, "Modo sin conexión activado. Usted se encuentra bajo el PLAN $fallbackRole, Bienvenido", Toast.LENGTH_LONG).show()
                            onLoginSuccess(displayName, userId, fallbackRole, 0L, 2592000L)
                        }
                    } else { isLoading = false; Toast.makeText(context, "Error al procesar la credencial", Toast.LENGTH_SHORT).show() }
                } catch (e: Exception) { e.printStackTrace(); isLoading = false; Toast.makeText(context, "Inicio de sesión cancelado o fallido", Toast.LENGTH_SHORT).show() } } }, modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)) { Text("Iniciar sesión con Google", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = { try { val addAccountIntent = Intent(Settings.ACTION_ADD_ACCOUNT).apply { putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google")) }; context.startActivity(addAccountIntent) } catch (_: Exception) { Toast.makeText(context, "No se pudo abrir la configuración", Toast.LENGTH_LONG).show() } }, modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)) { Icon(Icons.Filled.Add, contentDescription = "Añadir cuenta", modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Añadir cuenta nueva", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ==========================================
// 6. INTERFAZ GRÁFICA PRINCIPAL Y CONTROL DE PLANES EN VIVO
// ==========================================
fun showPremiumToastMsg(context: Context) { Toast.makeText(context, "👑 Esta función es exclusiva para planes de pago.", Toast.LENGTH_SHORT).show() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(viewModel: FinanceViewModel, userName: String, initialRole: String, initialConsumedSeconds: Long, initialPlanDuration: Long, onLogout: () -> Unit, isDarkTheme: Boolean, onThemeToggle: () -> Unit) {
    val context = LocalContext.current
    val authPrefs = context.getSharedPreferences("GlobalAuthPrefs", Context.MODE_PRIVATE)

    val isSuperAdmin = viewModel.userId.lowercase(Locale.getDefault()) == "zonacami77777@gmail.com"

    var currentRole by remember { mutableStateOf(if (isSuperAdmin) "ADMIN" else initialRole) }
    var currentConsumed by remember { mutableStateOf(initialConsumedSeconds) }
    var currentPlanDuration by remember { mutableStateOf(initialPlanDuration) }
    var showPlansDialog by remember { mutableStateOf(!isSuperAdmin) }

    var showRoleUpgradeDialog by remember { mutableStateOf<String?>(null) }
    var showRoleDowngradeDialog by remember { mutableStateOf(false) }
    var showWarningDialog by remember { mutableStateOf<String?>(null) }

    var preselectedDateForEvent by remember { mutableStateOf<Long?>(null) }
    var showEventChoiceDialog by remember { mutableStateOf(false) }
    var showDayEventsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while(true) {
            delay(30000L)
            try {
                val response = RetrofitInstance.api.addUserTime(UserTimeRequest(viewModel.userId, 30L))
                if (response.isBanned && !isSuperAdmin) { Toast.makeText(context, "Tu tiempo ha culminado o has sido bloqueado.", Toast.LENGTH_LONG).show(); onLogout(); break }

                // CORRECCIÓN 1: Si el servidor no envía el rol (null), mantenemos el currentRole en lugar de forzar "INVITADO"
                val newRole = response.role ?: currentRole
                if (newRole != currentRole && !isSuperAdmin) {
                    if (newRole == "INVITADO") { showRoleDowngradeDialog = true } else { showRoleUpgradeDialog = newRole }
                    currentRole = newRole
                    authPrefs.edit().putString("userRole", currentRole).putString("lastKnownRole", currentRole).apply()
                }

                // CORRECCIÓN 2: Evitamos fallos si el JSON del servidor omite estos valores (quedarían en 0 por defecto)
                if (response.planDuration > 0L) {
                    currentConsumed = response.consumedSeconds
                    currentPlanDuration = response.planDuration
                    authPrefs.edit().putLong("consumedSeconds", currentConsumed).putLong("planDuration", currentPlanDuration).apply()
                }

                if (currentRole != "INVITADO" && !isSuperAdmin) {
                    val timeLeftSecs = currentPlanDuration - currentConsumed

                    // CORRECCIÓN 3: Validación local real de expiración del tiempo
                    if (timeLeftSecs <= 0) {
                        showRoleDowngradeDialog = true
                        currentRole = "INVITADO"
                        authPrefs.edit().putString("userRole", "INVITADO").putString("lastKnownRole", "INVITADO").apply()
                    } else {
                        val daysLeft = timeLeftSecs / 86400L
                        val lastWarning = authPrefs.getLong("lastWarning_$daysLeft", 0L)
                        val now = System.currentTimeMillis()
                        if (daysLeft in listOf(7L, 3L, 2L, 1L) && (now - lastWarning > 86400000L)) {
                            showWarningDialog = "Tu plan expirará en $daysLeft días. Realiza el pago para mantener tus privilegios o pasarás a ser INVITADO."
                            authPrefs.edit().putLong("lastWarning_$daysLeft", now).apply()
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            if (!isSuperAdmin) currentConsumed++
        }
    }

    if (showRoleUpgradeDialog != null) {
        AlertDialog(
            onDismissRequest = { showRoleUpgradeDialog = null },
            title = { Text("¡Felicidades! 🎉", fontWeight = FontWeight.Bold) },
            text = { Text("Tu plan ha sido actualizado a ${showRoleUpgradeDialog}. ¡Disfruta de tus nuevos privilegios!") },
            confirmButton = { Button(onClick = { showRoleUpgradeDialog = null }) { Text("Aceptar") } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showRoleDowngradeDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDowngradeDialog = false },
            title = { Text("Plan Expirado ⚠️", fontWeight = FontWeight.Bold) },
            text = { Text("El tiempo de tu plan ha culminado. Ahora eres INVITADO. Contacta al administrador para renovar.") },
            confirmButton = { Button(onClick = { showRoleDowngradeDialog = false }) { Text("Aceptar") } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showWarningDialog != null) {
        AlertDialog(
            onDismissRequest = { showWarningDialog = null },
            title = { Text("Aviso de Expiración ⏳", fontWeight = FontWeight.Bold) },
            text = { Text(showWarningDialog!!) },
            confirmButton = { Button(onClick = { showWarningDialog = null }) { Text("Entendido") } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    var showChatDialog by remember { mutableStateOf(false) }
    var chatTargetEmail by remember { mutableStateOf("") }
    var showAdminChatList by remember { mutableStateOf(false) }
    var unreadCount by remember { mutableStateOf(0) }

    LaunchedEffect(showChatDialog, showAdminChatList) {
        if (showChatDialog || showAdminChatList) {
            authPrefs.edit().putLong("lastReadChat_${viewModel.userId}", System.currentTimeMillis()).apply()
            authPrefs.edit().putLong("lastNotified_${viewModel.userId}", System.currentTimeMillis()).apply()
            unreadCount = 0
        }
    }

    LaunchedEffect(Unit) {
        var lastMsgTimestamp = System.currentTimeMillis()
        while (true) {
            delay(5000L)
            try {
                val isChatOpen = showChatDialog || showAdminChatList
                val lastRead = authPrefs.getLong("lastReadChat_${viewModel.userId}", 0L)
                var lastNotified = authPrefs.getLong("lastNotified_${viewModel.userId}", System.currentTimeMillis())
                var currentUnread = 0; var latestMsgTimestamp = lastNotified; var notificationMsg: String? = null; var notificationSender: String? = null

                if (currentRole == "ADMIN") {
                    val chats = RetrofitInstance.api.getAllChats()
                    chats.forEach { (email, msgs) -> msgs.forEach { msg -> if (msg.sender != viewModel.userId) { if (msg.timestamp > lastRead) currentUnread++; if (msg.timestamp > latestMsgTimestamp) { latestMsgTimestamp = msg.timestamp; notificationMsg = msg.text; notificationSender = email.substringBefore("@") } } } }
                } else {
                    val msgs = RetrofitInstance.api.getChat(viewModel.userId)
                    msgs.forEach { msg -> if (msg.sender != viewModel.userId) { if (msg.timestamp > lastRead) currentUnread++; if (msg.timestamp > latestMsgTimestamp) { latestMsgTimestamp = msg.timestamp; notificationMsg = msg.text; notificationSender = "Soporte" } } }
                }
                if (isChatOpen) { authPrefs.edit().putLong("lastReadChat_${viewModel.userId}", System.currentTimeMillis()).apply(); unreadCount = 0 } else { unreadCount = currentUnread }
                if (notificationMsg != null && !isChatOpen && latestMsgTimestamp > lastNotified) { showChatNotification(context, "Nuevo mensaje de $notificationSender", notificationMsg!!); authPrefs.edit().putLong("lastNotified_${viewModel.userId}", latestMsgTimestamp).apply() }
            } catch (_: Exception) {}
        }
    }

    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val reminders by viewModel.reminders.collectAsState(initial = emptyList())
    val fiadores by viewModel.fiadores.collectAsState(initial = emptyList())
    val products by viewModel.products.collectAsState(initial = emptyList())

    val personalTransactions = remember(transactions) { transactions.filter { !it.description.startsWith("Venta: ") } }
    val storeTransactions = remember(transactions) { transactions.filter { it.isIncome && it.description.startsWith("Venta:") } }
    val totalStoreCash = remember(storeTransactions) { storeTransactions.sumOf { it.cashAmount } }
    val totalStoreDigital = remember(storeTransactions) { storeTransactions.sumOf { it.digitalAmount } }
    val totalProfit = remember(storeTransactions) { storeTransactions.sumOf { it.profit } }

    var currentTab by remember { mutableStateOf(0) }
    var showInventoryScreen by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    val shoppingCart = remember { mutableStateListOf<Pair<Product, Int>>() }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var productToAddToCart by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showDeleteQtyDialog by remember { mutableStateOf(false) }
    var showRedWarningDialog by remember { mutableStateOf(false) }
    var qtyToDelete by remember { mutableStateOf("") }
    var productToInfo by remember { mutableStateOf<Product?>(null) }
    var productToFullDelete by remember { mutableStateOf<Product?>(null) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var showDeleteHistoryConfirmDialog by remember { mutableStateOf(false) }
    var showResetProfitsDialog by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var showRemindersListDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<Reminder?>(null) }
    var showFiadoresListDialog by remember { mutableStateOf(false) }
    var showFiadorDialog by remember { mutableStateOf(false) }
    var fiadorToEdit by remember { mutableStateOf<Fiador?>(null) }
    var customToastMessage by remember { mutableStateOf<String?>(null) }
    var backPressedOnce by remember { mutableStateOf(false) }
    var undoMessage by remember { mutableStateOf<String?>(null) }
    var undoAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var showBackupNameDialog by remember { mutableStateOf(false) }
    var backupNameInput by remember { mutableStateOf("") }
    var showBackupListDialog by remember { mutableStateOf(false) }
    var cloudBackupsList by remember { mutableStateOf<List<BackupRecord>?>(null) }
    var isLoadingList by remember { mutableStateOf(false) }
    var showAdminPanelDialog by remember { mutableStateOf(false) }

    val isLockedStore = currentRole == "BÁSICO" || currentRole == "INVITADO"
    val isAutoSyncAllowed = currentRole == "PREMIUM" || currentRole == "GOLD" || currentRole == "ADMIN"
    val isManualSyncAllowed = currentRole != "INVITADO"
    val isResumenAllowed = currentRole == "PREMIUM" || currentRole == "GOLD" || currentRole == "ADMIN"
    val isBorrarHistorialAllowed = true
    val isDeudasAllowed = currentRole != "INVITADO"

    val snackbarHostState = remember { SnackbarHostState() }; val coroutineScope = rememberCoroutineScope()
    val totalIncome = remember(personalTransactions) { personalTransactions.filter { it.isIncome }.sumOf { it.amount } }; val totalExpense = remember(personalTransactions) { personalTransactions.filter { !it.isIncome }.sumOf { it.amount } }; val balance = totalIncome - totalExpense
    var currentUiTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000L); currentUiTime = System.currentTimeMillis() } }
    val activeReminders = remember(reminders, currentUiTime) { reminders.filter { it.targetDateInMillis <= currentUiTime } }; val activeFiadores = remember(fiadores, currentUiTime) { fiadores.filter { it.targetDateInMillis <= currentUiTime } }

    LaunchedEffect(customToastMessage ?: "") { if (customToastMessage != null) { delay(3000L); customToastMessage = null } }
    LaunchedEffect(undoMessage ?: "") { if (undoMessage != null) { delay(5000L); undoMessage = null; undoAction = null } }

    BackHandler { if (showInventoryScreen) { showInventoryScreen = false } else if (backPressedOnce) { (context as? ComponentActivity)?.finish() } else { backPressedOnce = true; Toast.makeText(context, "Presiona Atrás de nuevo para salir", Toast.LENGTH_SHORT).show(); coroutineScope.launch { delay(2000L); backPressedOnce = false } } }

    val crownEmoji = when (currentRole ?: "INVITADO") { "INVITADO" -> "🪵"; "BÁSICO" -> "🥉"; "PREMIUM" -> "🥈"; "GOLD" -> "🥇"; "ADMIN" -> "👑"; else -> "🪵" }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!showInventoryScreen) {
                TopAppBar(
                    title = { val firstName = userName.split(" ").first(); Text(text = if (currentTab == 0) "Hola, $firstName $crownEmoji" else "Tienda de $firstName 🏪", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, actionIconContentColor = MaterialTheme.colorScheme.onPrimary),
                    actions = {
                        IconButton(onClick = { showCalendarDialog = true }) { Icon(Icons.Filled.DateRange, "Calendario") }
                        IconButton(onClick = onThemeToggle) { Icon(if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode, "Tema") }

                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, "Menú") }
                            if (unreadCount > 0) {
                                Box(modifier = Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 8.dp).size(12.dp).clip(CircleShape).background(Color.Red), contentAlignment = Alignment.Center) {
                                    Text(unreadCount.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                            if (currentRole == "ADMIN") {
                                DropdownMenuItem(text = { Text("🛠️ Panel de Administrador") }, onClick = { showAdminPanelDialog = true; showMenu = false })
                                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Text("💬 Mensajes de Clientes"); if (unreadCount > 0) { Spacer(modifier = Modifier.width(8.dp)); Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.Red), contentAlignment = Alignment.Center) { Text(unreadCount.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } } } }, onClick = { showAdminChatList = true; showMenu = false })
                                Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)
                            } else {
                                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Text("💬 Servicio al Cliente"); if (unreadCount > 0) { Spacer(modifier = Modifier.width(8.dp)); Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.Red), contentAlignment = Alignment.Center) { Text(unreadCount.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } } } }, onClick = { chatTargetEmail = viewModel.userId; showChatDialog = true; showMenu = false })
                                Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)
                            }

                            if (isManualSyncAllowed) { DropdownMenuItem(text = { Text("☁️ Sincronización Nube") }, onClick = { showCloudSyncDialog = true; showMenu = false }) }
                            else { DropdownMenuItem(text = { Text("👑 Sincronización Nube", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }, onClick = { showPremiumToastMsg(context); showMenu = false }) }
                            Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                            if (isResumenAllowed) { DropdownMenuItem(text = { Text("📊 Resumen de Totales") }, onClick = { showSummaryDialog = true; showMenu = false }) } else { DropdownMenuItem(text = { Text("👑 📊 Resumen de Totales", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }, onClick = { showPremiumToastMsg(context); showMenu = false }) }
                            DropdownMenuItem(text = { Text("🔔 Saldo Crítico") }, onClick = { showLimitDialog = true; showMenu = false })

                            DropdownMenuItem(text = { Text("🎵 Sonidos") }, onClick = { showSoundDialog = true; showMenu = false })

                            if (currentTab == 0 && isBorrarHistorialAllowed) { DropdownMenuItem(text = { Text("⚠️ Borrar Historial", color = Color(0xFFE53935)) }, onClick = { showDeleteHistoryConfirmDialog = true; showMenu = false }) }

                            DropdownMenuItem(text = { Text("🚪 Cerrar Sesión", color = Color(0xFFE53935)) }, onClick = { onLogout(); showMenu = false })
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!showInventoryScreen) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                    NavigationBarItem(icon = { Icon(Icons.Filled.Person, "Personal") }, label = { Text("Personal") }, selected = currentTab == 0, onClick = { currentTab = 0 }, colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary))
                    NavigationBarItem(icon = { Icon(Icons.Filled.Storefront, "Tienda") }, label = { Text("Tienda") }, selected = currentTab == 1, onClick = { currentTab = 1 }, colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary))
                }
            }
        },
        floatingActionButton = { if (!showInventoryScreen && currentTab == 0) { FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary) { Icon(Icons.Filled.Add, "Agregar") } } }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.background)) {
            if (showInventoryScreen) {
                InventoryScreen(products = products, shoppingCart = shoppingCart, onBack = { showInventoryScreen = false }, onAddProductClick = { productToEdit = null; showAddProductDialog = true }, onAddToCartClick = { productToAddToCart = it }, onOpenCheckout = { showCheckoutDialog = true }, onEditClick = { productToEdit = it; showAddProductDialog = true }, onDeleteClick = { productToDelete = it; qtyToDelete = "1"; showDeleteQtyDialog = true }, onLongDeleteClick = { productToFullDelete = it }, onInfoClick = { productToInfo = it })
            } else {
                Crossfade(targetState = currentTab, label = "TabSwitch") { tab ->
                    if (tab == 0) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            DashboardCard(balance, totalIncome, totalExpense)
                            AnimatedVisibility(visible = viewModel.minBalanceThreshold > 0 && balance < viewModel.minBalanceThreshold) { Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).background(Color(0xFFD32F2F), RoundedCornerShape(8.dp)).padding(12.dp)) { Text("⚠️ ¡Alerta! Tu saldo está por debajo del límite crítico (${formatCOP(viewModel.minBalanceThreshold)}).", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) } }
                            activeReminders.forEach { reminder -> AnimatedVisibility(visible = true) { Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).background(Color(0xFF1976D2), RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text("📅 Es hora de pagar: ${reminder.title}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f)); IconButton(onClick = { viewModel.deleteReminder(reminder, context) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Check, contentDescription = "Hecho", tint = Color.White) } } } }
                            activeFiadores.forEach { fiador ->
                                val remaining = fiador.amount - fiador.paidAmount
                                AnimatedVisibility(visible = true) { Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).background(Color(0xFFFBC02D), RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { val phoneStr = if(fiador.phone.isNotBlank()) " 📞 ${fiador.phone}" else ""; Text("💰 Cobrar a ${fiador.name}$phoneStr", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text("Resta: ${formatCOP(remaining)} de ${formatCOP(fiador.amount)} - ${fiador.reason}", color = Color.Black.copy(alpha=0.8f), fontSize = 12.sp) }; IconButton(onClick = { viewModel.deleteFiador(fiador, context) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Check, contentDescription = "Saldado", tint = Color.Black) } } } }
                            Spacer(modifier = Modifier.height(16.dp)); Text("Movimientos Recientes 📋", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 16.dp)); Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) { items(personalTransactions, key = { it.id }) { transaction -> Box(modifier = Modifier.animateItem(placementSpec = tween(400))) { TransactionItem(transaction = transaction, onDelete = { viewModel.deleteTransaction(transaction); coroutineScope.launch { val result = snackbarHostState.showSnackbar(message = "Registro eliminado 🗑️", actionLabel = "Deshacer ↩️", duration = SnackbarDuration.Short); if (result == SnackbarResult.ActionPerformed) viewModel.insertRawTransaction(transaction) } }) } } }
                        }
                    } else {
                        StoreScreen(products = products, transactions = transactions, shoppingCart = shoppingCart, isLockedStore = isLockedStore, totalStoreCash = totalStoreCash, totalStoreDigital = totalStoreDigital, onOpenInventory = { showInventoryScreen = true }, onOpenCheckout = { showCheckoutDialog = true }, onResetProfitsClick = { showResetProfitsDialog = true }, onDeleteVentas = { list -> viewModel.deleteTransactionsList(list) }, showPremiumToast = { showPremiumToastMsg(context) }, totalProfit = totalProfit)
                    }
                }
            }

            AnimatedVisibility(visible = undoMessage != null, enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(300)), exit = fadeOut(tween(500)), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp, start = 16.dp, end = 16.dp)) { Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF323232), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(text = undoMessage ?: "", color = Color.White, modifier = Modifier.weight(1f), fontSize = 14.sp); TextButton(onClick = { undoAction?.invoke(); undoMessage = null; undoAction = null }) { Text("DESHACER", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } } }
            AnimatedVisibility(visible = customToastMessage != null, enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { 50 }), exit = fadeOut(tween(1500)), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)) { Box(modifier = Modifier.background(Color.DarkGray.copy(alpha = 0.9f), RoundedCornerShape(24.dp)).padding(horizontal = 24.dp, vertical = 12.dp)) { Text(text = customToastMessage ?: "", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) } }
        }

        if (showAdminChatList && currentRole == "ADMIN") { AdminChatListDialog(onDismiss = { showAdminChatList = false }, onSelectClient = { email -> chatTargetEmail = email; showAdminChatList = false; showChatDialog = true }) }
        if (showChatDialog) { ChatDialog(currentUserEmail = viewModel.userId, targetClientEmail = chatTargetEmail, isAdmin = (currentRole == "ADMIN"), onDismiss = { showChatDialog = false }) }

        if (showPlansDialog) {
            AlertDialog(
                onDismissRequest = { showPlansDialog = false },
                title = { Text("Planes Disponibles 🚀", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    Column {
                        Text("Mejora tu plan comunicándote con el Administrador para desbloquear todo el potencial de la aplicación.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PlanCardInfo("INVITADO 🪵", "Prueba de 1 mes.", listOf("Pestaña Personal", "Saldo crítico y Sonidos"), listOf("Control de Tienda y Ventas", "Inventario", "Sincronización Automática", "Gestión de Deudas", "Borrar Historial"))
                            PlanCardInfo("BÁSICO 🥉", "1, 6 o 12 meses.", listOf("Pestaña Personal", "Saldo crítico y Sonidos", "Gestión de Deudas", "Respaldo manual en nube", "Borrar Historial"), listOf("Control de Tienda y Ventas", "Inventario", "Sincronización Automática"))
                            PlanCardInfo("PREMIUM 🥈", "1, 6 o 12 meses.", listOf("Todo lo del Básico", "Acceso total a Tienda", "Inventario y Fechas", "Resumen de Totales", "Sincronización Automática"), listOf("Prioridad de Soporte"))
                            PlanCardInfo("GOLD 🥇", "1, 6 o 12 meses.", listOf("Uso de toda la aplicación sin ninguna restricción", "Borrado completo", "Prioridad y Soporte total"), emptyList())
                        }
                    }
                },
                confirmButton = { Button(onClick = { showPlansDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("¡Entendido, Comenzar!") } },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            )
        }

        if (showAdminPanelDialog) {
            var usersList by remember { mutableStateOf<Map<String, UserData>?>(null) }; var isLoadingUsers by remember { mutableStateOf(true) }
            var roleToAssign by remember { mutableStateOf<String?>(null) }; var targetEmailToAssign by remember { mutableStateOf<String?>(null) }
            val formatTimeLeft = { secs: Long, maxSecs: Long -> val left = maxSecs - secs; if (left <= 0) "0s" else { val days = left / 86400; val hours = (left % 86400) / 3600; "${days}d ${hours}h" } }
            LaunchedEffect(Unit) { try { usersList = RetrofitInstance.api.getAllUsers() } catch (_: Exception) { customToastMessage = "Error cargando usuarios" }; isLoadingUsers = false }
            fun manageUser(targetEmail: String, action: String, newRole: String? = null, pDuration: Long? = null) { coroutineScope.launch(Dispatchers.IO) { try { RetrofitInstance.api.manageUser(UserManageRequest(targetEmail, action, newRole, pDuration)); val updatedList = RetrofitInstance.api.getAllUsers(); launch(Dispatchers.Main) { usersList = updatedList; customToastMessage = "Acción completada" } } catch (_: Exception) { launch(Dispatchers.Main) { customToastMessage = "Fallo de conexión" } } } }

            if (roleToAssign != null && targetEmailToAssign != null) {
                AlertDialog(
                    onDismissRequest = { roleToAssign = null; targetEmailToAssign = null },
                    title = { Text("Duración para $roleToAssign ⏱️", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    text = {
                        Column {
                            Button(onClick = { manageUser(targetEmailToAssign!!, "setRole", roleToAssign, 2592000L); roleToAssign = null }, modifier = Modifier.fillMaxWidth()) { Text("1 Mes (30 días)") }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { manageUser(targetEmailToAssign!!, "setRole", roleToAssign, 15552000L); roleToAssign = null }, modifier = Modifier.fillMaxWidth()) { Text("6 Meses (180 días)") }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { manageUser(targetEmailToAssign!!, "setRole", roleToAssign, 31104000L); roleToAssign = null }, modifier = Modifier.fillMaxWidth()) { Text("1 Año (360 días)") }
                        }
                    },
                    confirmButton = {}, dismissButton = { TextButton(onClick = { roleToAssign = null; targetEmailToAssign = null }) { Text("Cancelar") } }, containerColor = MaterialTheme.colorScheme.surface
                )
            }

            AlertDialog(
                onDismissRequest = { showAdminPanelDialog = false }, title = { Text("Panel de Control 🛠️", fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    if (isLoadingUsers) { CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)) }
                    else if (usersList.isNullOrEmpty()) { Text("No hay usuarios registrados.", color = Color.Gray) }
                    else {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            items(usersList!!.entries.toList()) { entry ->
                                val email = entry.key; val userData = entry.value; var expandedRoleMenu by remember { mutableStateOf(false) }
                                val isAdminAccount = email.lowercase(Locale.getDefault()) == "zonacami77777@gmail.com"

                                val listCrown = when (userData.role ?: "INVITADO") { "INVITADO" -> "🪵"; "BÁSICO" -> "🥉"; "PREMIUM" -> "🥈"; "GOLD" -> "🥇"; "ADMIN" -> "👑"; else -> "🪵" }

                                Card(modifier = Modifier.fillMaxWidth().padding(vertical=4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.4f))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("${userData.name} $listCrown", fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(email, fontSize = 12.sp, color = Color.Gray)
                                        if (isAdminAccount) { Text("Administrador del Sistema ✅", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=4.dp)) }
                                        else if (userData.isBanned) { Text("SUSPENDIDO 🚫", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top=4.dp)) }
                                        else { Text("Tiempo restante (${userData.role}): ${formatTimeLeft(userData.consumedSeconds, userData.planDuration)}", color = Color(0xFFE65100), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=4.dp)) }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                OutlinedButton(onClick = { expandedRoleMenu = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) { Text("Rol") }
                                                DropdownMenu(expanded = expandedRoleMenu, onDismissRequest = { expandedRoleMenu = false }) {
                                                    listOf("INVITADO", "BÁSICO", "PREMIUM", "GOLD").forEach { newRole ->
                                                        DropdownMenuItem(text = { Text(newRole) }, onClick = {
                                                            expandedRoleMenu = false
                                                            if (newRole == "INVITADO") { manageUser(email, "setRole", newRole, 2592000L) }
                                                            else { roleToAssign = newRole; targetEmailToAssign = email }
                                                        })
                                                    }
                                                }
                                            }
                                            OutlinedButton(onClick = { manageUser(email, "resetTime") }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("Reset") }
                                            if (userData.isBanned) { Button(onClick = { manageUser(email, "unban") }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("Desbloquear") } }
                                            else { Button(onClick = { manageUser(email, "ban") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), contentPadding = PaddingValues(0.dp)) { Text("Bloquear") } }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }, confirmButton = { TextButton(onClick = { showAdminPanelDialog = false }) { Text("Cerrar") } }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
            )
        }

        if (showCloudSyncDialog) {
            val autoTimePickerDialog = remember { android.app.TimePickerDialog(context, android.R.style.Theme_Holo_Dialog, { _, h, m -> viewModel.updateAutoSyncSchedule(viewModel.getApplication(), viewModel.autoSyncFrequency, h, m) }, viewModel.autoSyncHour, viewModel.autoSyncMinute, false) }
            val isAutoEnabled = viewModel.autoSyncFrequency > 0

            AlertDialog(
                onDismissRequest = { if (!viewModel.isSyncing) showCloudSyncDialog = false }, title = { Text("Sincronización Nube ☁️", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tus datos se guardan en el servidor de forma segura.", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 13.sp); Spacer(modifier = Modifier.height(16.dp))
                        if (viewModel.lastSyncDate > 0L) { Text("Último respaldo en este equipo:", fontSize = 13.sp); Text(formatDate(viewModel.lastSyncDate), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth().alpha(if (isAutoSyncAllowed) 1f else 0.5f).clickable { if (isAutoSyncAllowed) { val newFreq = if (isAutoEnabled) 0 else 1; viewModel.updateAutoSyncSchedule(viewModel.getApplication(), newFreq, viewModel.autoSyncHour, viewModel.autoSyncMinute) } else { showPremiumToastMsg(context) } }, verticalAlignment = Alignment.CenterVertically) { Switch(checked = isAutoEnabled && isAutoSyncAllowed, onCheckedChange = null, enabled = isAutoSyncAllowed, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)); Spacer(modifier = Modifier.width(12.dp)); Text(if(isAutoSyncAllowed) "Modo Automático" else "👑 Modo Automático", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                        if (!isAutoSyncAllowed) { Text("La actualización automática es Premium.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top=4.dp)) }; Spacer(modifier = Modifier.height(16.dp))

                        if (viewModel.isSyncing) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary); Text("Procesando...", modifier = Modifier.padding(top = 8.dp)) } else {
                            if (isAutoEnabled && isAutoSyncAllowed) {
                                var expandedFreq by remember { mutableStateOf(false) }; val freqOptions = listOf(1 to "Diario", 7 to "Semanal", 30 to "Mensual"); val currentFreqText = freqOptions.find { it.first == viewModel.autoSyncFrequency }?.second ?: "Diario"
                                Text("Configuración Automática ⏱️", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Gray); Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { OutlinedButton(onClick = { expandedFreq = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 4.dp)) { Text(currentFreqText, maxLines=1) }; DropdownMenu(expanded = expandedFreq, onDismissRequest = { expandedFreq = false }) { freqOptions.forEach { (days, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { viewModel.updateAutoSyncSchedule(viewModel.getApplication(), days, viewModel.autoSyncHour, viewModel.autoSyncMinute); expandedFreq = false }) } } }; OutlinedButton(onClick = { autoTimePickerDialog.show() }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) { val formatStr = String.format("%02d:%02d", if(viewModel.autoSyncHour == 0) 12 else if(viewModel.autoSyncHour > 12) viewModel.autoSyncHour - 12 else viewModel.autoSyncHour, viewModel.autoSyncMinute); val amPm = if(viewModel.autoSyncHour >= 12) "PM" else "AM"; Text("$formatStr $amPm ⏰", maxLines=1) } }
                                Text("La app subirá tus datos automáticamente.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center)
                            } else { Button(onClick = { if (isManualSyncAllowed) { backupNameInput = ""; showBackupNameDialog = true } else { showPremiumToastMsg(context) } }, modifier = Modifier.fillMaxWidth(), colors = if (isManualSyncAllowed) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Text(if (isManualSyncAllowed) "Crear Respaldo Manual ⬆️" else "👑 Crear Respaldo Manual ⬆️", color = if (isManualSyncAllowed) Color.White else Color.Gray) } }
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = { if (isManualSyncAllowed) { isLoadingList = true; viewModel.fetchBackupList { list -> isLoadingList = false; cloudBackupsList = list; showBackupListDialog = true } } else { showPremiumToastMsg(context) } }, modifier = Modifier.fillMaxWidth()) { if (isLoadingList) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary) else Text(if (isManualSyncAllowed) "Listas de Copias de Seguridad 🗂️" else "👑 Listas de Copias de Seguridad 🗂️", color = if (isManualSyncAllowed) MaterialTheme.colorScheme.primary else Color.Gray) }
                        }
                    }
                }, confirmButton = {}, dismissButton = { if (!viewModel.isSyncing) TextButton(onClick = { showCloudSyncDialog = false }) { Text("Cerrar") } }
            )
        }

        if (showBackupNameDialog) { AlertDialog(onDismissRequest = { showBackupNameDialog = false }, title = { Text("Nombre del Respaldo", fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface, text = { OutlinedTextField(value = backupNameInput, onValueChange = { backupNameInput = it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() } }, label = { Text("Ej: Antes de formatear") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)) }, confirmButton = { Button(onClick = { if (backupNameInput.isNotBlank()) { viewModel.manualBackup(backupNameInput.trim()) { msg -> customToastMessage = msg; showBackupNameDialog = false; showCloudSyncDialog = false } } else { Toast.makeText(context, "Ingresa un nombre", Toast.LENGTH_SHORT).show() } }) { Text("Guardar Respaldo") } }, dismissButton = { TextButton(onClick = { showBackupNameDialog = false }) { Text("Cancelar") } }) }

        if (showBackupListDialog) {
            AlertDialog(
                onDismissRequest = { showBackupListDialog = false },
                title = { Text("Copias de Seguridad 🗂️", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    if (cloudBackupsList.isNullOrEmpty()) {
                        Text("No hay copias guardadas en la nube.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                            items(cloudBackupsList!!) { record ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical=4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.4f))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(record.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                if (record.timestamp > 0) { Text(formatDate(record.timestamp), fontSize = 12.sp, color = Color.Gray) }
                                            }
                                            IconButton(onClick = { viewModel.deleteBackupRecord(record.id) { msg -> customToastMessage = msg; cloudBackupsList = cloudBackupsList?.filter { it.id != record.id } } }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.8f))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = { viewModel.restoreFromRecord(record) { msg -> customToastMessage = msg; showBackupListDialog = false; showCloudSyncDialog = false } }, modifier = Modifier.fillMaxWidth()) {
                                            Text("Restaurar esta copia")
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showBackupListDialog = false }) { Text("Volver") } }
            )
        }

        if (showAddDialog) AddTransactionDialog(onDismiss = { showAddDialog = false }, onConfirm = { d, a, i, n -> viewModel.addTransaction(d, a, i, n); showAddDialog = false })
        if (showLimitDialog) LimitDialog(currentLimit = viewModel.minBalanceThreshold, onDismiss = { showLimitDialog = false }, onConfirm = { viewModel.updateMinBalance(it); showLimitDialog = false })

        if (showEventChoiceDialog) {
            AlertDialog(
                onDismissRequest = { showEventChoiceDialog = false; preselectedDateForEvent = null },
                title = { Text("¿Qué quieres recordar?", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = {
                                showEventChoiceDialog = false
                                reminderToEdit = null
                                showReminderDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) { Text("¿A quién le debes? 🔵") }
                        Button(
                            onClick = {
                                showEventChoiceDialog = false
                                fiadorToEdit = null
                                showFiadorDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D), contentColor = Color.Black)
                        ) { Text("¿Quién te debe? 🟡") }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showEventChoiceDialog = false; preselectedDateForEvent = null }) { Text("Cancelar") } }
            )
        }

        if (showDayEventsDialog && preselectedDateForEvent != null) {
            val dayReminders = reminders.filter { isSameDay(it.targetDateInMillis, preselectedDateForEvent!!) }
            val dayFiadores = fiadores.filter { isSameDay(it.targetDateInMillis, preselectedDateForEvent!!) }
            val dayProducts = products.filter { it.expirationDateInMillis != null && isSameDay(it.expirationDateInMillis, preselectedDateForEvent!!) }

            AlertDialog(
                onDismissRequest = { showDayEventsDialog = false; preselectedDateForEvent = null },
                title = { Text("Eventos del Día 📅", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(dayReminders) { r ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                    showDayEventsDialog = false
                                    reminderToEdit = r
                                    showReminderDialog = true
                                },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2).copy(alpha = 0.2f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("🔵", modifier = Modifier.padding(end = 8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Pagar Deuda", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(r.title, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        items(dayFiadores) { f ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                    showDayEventsDialog = false
                                    fiadorToEdit = f
                                    showFiadorDialog = true
                                },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFBC02D).copy(alpha = 0.2f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("🟡", modifier = Modifier.padding(end = 8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        val remaining = f.amount - f.paidAmount
                                        Text("Cobrar Dinero", fontSize = 12.sp, color = Color(0xFFFBC02D))
                                        Text("${f.name} - Resta: ${formatCOP(remaining)}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        items(dayProducts) { p ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.2f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("🔴", modifier = Modifier.padding(end = 8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Vencimiento de Producto", fontSize = 12.sp, color = Color(0xFFD32F2F))
                                        Text("${p.name} (${p.stock} ${p.unit})", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showDayEventsDialog = false
                        showEventChoiceDialog = true
                    }) { Text("Añadir Otro Evento") }
                },
                dismissButton = {
                    TextButton(onClick = { showDayEventsDialog = false; preselectedDateForEvent = null }) { Text("Cerrar") }
                }
            )
        }

        if (showCalendarDialog) {
            CalendarDialog(
                reminders = reminders,
                fiadores = fiadores,
                products = products,
                onDismiss = { showCalendarDialog = false },
                onDayClick = { dateMillis, hasEvents ->
                    if (isDeudasAllowed) {
                        preselectedDateForEvent = dateMillis
                        if (hasEvents) {
                            showDayEventsDialog = true
                        } else {
                            showEventChoiceDialog = true
                        }
                    } else {
                        showPremiumToastMsg(context)
                    }
                },
                onViewReminders = { showRemindersListDialog = true },
                onViewFiadores = { showFiadoresListDialog = true }
            )
        }

        if (showSummaryDialog) SummaryDialog(totalIncome = totalIncome, totalExpense = totalExpense, balance = balance, transactionCount = personalTransactions.size, onDismiss = { showSummaryDialog = false })
        if (showSoundDialog) SoundSettingsDialog(currentSound = viewModel.selectedSound, onDismiss = { showSoundDialog = false }, onSelect = { viewModel.updateSoundPreference(it); showSoundDialog = false })

        if (showDeleteHistoryConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteHistoryConfirmDialog = false },
                title = { Text("Borrar Historial Personal ⚠️", fontWeight = FontWeight.Bold) },
                text = { Text("¿Estás seguro de que deseas borrar todo el historial personal de transacciones?") },
                confirmButton = { Button(onClick = { viewModel.deletePersonalTransactions(); showDeleteHistoryConfirmDialog = false; customToastMessage = "Historial borrado 🗑️" }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)) { Text("Sí, borrar", fontWeight = FontWeight.Bold) } },
                dismissButton = { TextButton(onClick = { showDeleteHistoryConfirmDialog = false }) { Text("Cancelar") } },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        if (showResetProfitsDialog) {
            AlertDialog(
                onDismissRequest = { showResetProfitsDialog = false },
                title = { Text("Opciones de Ganancias 💰", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Ganancias Totales: ${formatCOP(totalProfit)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        Text("Disponible en Efectivo (Caja):", fontSize = 14.sp, color = Color.Gray)
                        Text(formatCOP(totalStoreCash), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Disponible en Digital (Banco):", fontSize = 14.sp, color = Color.Gray)
                        Text(formatCOP(totalStoreDigital), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                confirmButton = { Button(onClick = { showResetProfitsDialog = false }) { Text("Cerrar") } },
                dismissButton = { TextButton(onClick = { viewModel.resetAllProfits(); showResetProfitsDialog = false; customToastMessage = "Caja y Banco reiniciados a $0" }) { Text("Reiniciar a $0", color = Color.Red) } },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        if (productToInfo != null) { ProductInfoDialog(product = productToInfo!!, onDismiss = { productToInfo = null }, onDeleteCompletely = { productToFullDelete = productToInfo; productToInfo = null }) }

        if (productToFullDelete != null) {
            AlertDialog(
                onDismissRequest = { productToFullDelete = null },
                title = { Text("Eliminar Producto ⚠️", fontWeight = FontWeight.Bold) },
                text = { Text("¿Estás seguro de que deseas eliminar completamente '${productToFullDelete!!.name}' de tu inventario?") },
                confirmButton = { Button(onClick = { val restoredProduct = productToFullDelete!!; viewModel.deleteProductEntirely(productToFullDelete!!, context); undoMessage = "Producto '${productToFullDelete!!.name}' eliminado"; undoAction = { viewModel.restoreProductStock(restoredProduct, 0, context) }; productToFullDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)) { Text("Eliminar", fontWeight = FontWeight.Bold) } },
                dismissButton = { TextButton(onClick = { productToFullDelete = null }) { Text("Cancelar") } },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        if (showAddProductDialog) { AddProductDialog(initialProduct = productToEdit, onDismiss = { showAddProductDialog = false; productToEdit = null }, onConfirm = { name, purchasePrice, price, stock, unit, expiryDate, minStock -> if (productToEdit != null) { viewModel.editProduct(productToEdit!!.copy(name = name, purchasePrice = purchasePrice, price = price, stock = stock, unit = unit, expirationDateInMillis = expiryDate, minStock = minStock), context) { msg -> customToastMessage = msg } } else { viewModel.addProduct(name, purchasePrice, price, stock, unit, expiryDate, minStock, context) { msg -> customToastMessage = msg } }; showAddProductDialog = false; productToEdit = null }) }

        if (productToAddToCart != null) { AddToCartDialog(product = productToAddToCart!!, currentCartQty = shoppingCart.find { it.first.id == productToAddToCart!!.id }?.second ?: 0, onDismiss = { productToAddToCart = null }, onConfirm = { qty -> val existing = shoppingCart.find { it.first.id == productToAddToCart!!.id }; if (existing != null) { val idx = shoppingCart.indexOf(existing); shoppingCart[idx] = existing.copy(second = existing.second + qty) } else { shoppingCart.add(Pair(productToAddToCart!!, qty)) }; customToastMessage = "Añadido al carrito 🛒"; productToAddToCart = null }) }

        if (showCheckoutDialog) {
            CheckoutDialog(
                cartItems = shoppingCart,
                products = products,
                totalStoreCash = totalStoreCash,
                totalStoreDigital = totalStoreDigital,
                onDismiss = { showCheckoutDialog = false },
                onConfirmSale = { items, buyer, summary, netCash, netDigital ->
                    viewModel.processCartSale(items, buyer, summary, netCash, netDigital, context) { msg -> customToastMessage = msg }
                    shoppingCart.clear()
                    showCheckoutDialog = false
                    showInventoryScreen = false
                }
            )
        }

        if (showDeleteQtyDialog && productToDelete != null) { DeleteQuantityDialog(product = productToDelete!!, initialQty = qtyToDelete, onDismiss = { showDeleteQtyDialog = false; productToDelete = null }, onConfirm = { qty -> qtyToDelete = qty.toString(); showDeleteQtyDialog = false; showRedWarningDialog = true }) }
        if (showRedWarningDialog && productToDelete != null) { RedWarningDialog(productName = productToDelete!!.name, qty = qtyToDelete.toIntOrNull() ?: 1, onDismiss = { showRedWarningDialog = false; productToDelete = null }, onConfirm = { val p = productToDelete!!; val q = qtyToDelete.toIntOrNull() ?: 1; viewModel.reduceProductStock(p, q, context); undoMessage = "Eliminados $q uds. de '${p.name}'"; undoAction = { viewModel.restoreProductStock(p, q, context) }; showRedWarningDialog = false; productToDelete = null }) }

        if (showRemindersListDialog) { ScheduledRemindersDialog(reminders = reminders, onDismiss = { showRemindersListDialog = false }, onDelete = { viewModel.deleteReminder(it, context) }, onEdit = { reminderToEdit = it; showRemindersListDialog = false; showReminderDialog = true }, onCreateNew = { reminderToEdit = null; showRemindersListDialog = false; showReminderDialog = true }) }

        if (showReminderDialog) { ReminderDialog(initialReminder = reminderToEdit, preselectedDate = preselectedDateForEvent, onDismiss = { showReminderDialog = false; reminderToEdit = null; preselectedDateForEvent = null; showRemindersListDialog = true }, onConfirm = { t, d -> if (reminderToEdit != null) { viewModel.updateExistingReminder(reminderToEdit!!.copy(title = t, targetDateInMillis = d), context) { customToastMessage = it } } else { viewModel.addReminder(t, d, context) { customToastMessage = it } }; showReminderDialog = false; reminderToEdit = null; preselectedDateForEvent = null; showRemindersListDialog = true }) }

        if (showFiadoresListDialog) { ScheduledFiadoresDialog(fiadores = fiadores, onDismiss = { showFiadoresListDialog = false }, onDelete = { viewModel.deleteFiador(it, context) }, onEdit = { fiadorToEdit = it; showFiadoresListDialog = false; showFiadorDialog = true }, onCreateNew = { fiadorToEdit = null; showFiadoresListDialog = false; showFiadorDialog = true }) }

        if (showFiadorDialog) {
            FiadorDialog(
                initialFiador = fiadorToEdit,
                products = products,
                preselectedDate = preselectedDateForEvent,
                onDismiss = { showFiadorDialog = false; fiadorToEdit = null; preselectedDateForEvent = null; showFiadoresListDialog = true },
                onConfirmNew = { n, p, cartItems, d -> viewModel.addFiador(n, p, cartItems, d, context) { customToastMessage = it }; showFiadorDialog = false; fiadorToEdit = null; preselectedDateForEvent = null; showFiadoresListDialog = true },
                onConfirmEdit = { f, d -> viewModel.updateExistingFiador(f.copy(targetDateInMillis = d), context) { customToastMessage = it }; showFiadorDialog = false; fiadorToEdit = null; preselectedDateForEvent = null; showFiadoresListDialog = true },
                onConfirmAbono = { f, abono, method -> viewModel.registerAbonoFiador(f, abono, method, context) { customToastMessage = it }; showFiadorDialog = false; fiadorToEdit = null; preselectedDateForEvent = null; showFiadoresListDialog = true }
            )
        }
    }
}

// === COMPONENTES DE CHAT ===
@Composable
fun AdminChatListDialog(onDismiss: () -> Unit, onSelectClient: (String) -> Unit) {
    var chats by remember { mutableStateOf<Map<String, List<ChatMessage>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try { chats = RetrofitInstance.api.getAllChats() } catch (_: Exception) {}
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Mensajes de Clientes 💬", fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface,
        text = {
            if (isLoading) { CircularProgressIndicator() }
            else if (chats.isEmpty()) { Text("No hay mensajes de clientes por ahora.", color = Color.Gray) }
            else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(chats.keys.toList()) { clientEmail ->
                        val lastMsg = chats[clientEmail]?.lastOrNull()
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelectClient(clientEmail) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(clientEmail, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (lastMsg != null) {
                                    Text("${if(lastMsg.sender=="zonacami77777@gmail.com") "Tú:" else "Cliente:"} ${lastMsg.text}", maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Gray, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }, properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDialog(currentUserEmail: String, targetClientEmail: String, isAdmin: Boolean, onDismiss: () -> Unit) {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(targetClientEmail) {
        while (true) {
            try {
                val newMsgs = RetrofitInstance.api.getChat(targetClientEmail)
                if (newMsgs != messages) {
                    messages = newMsgs
                    if (messages.isNotEmpty()) { listState.animateScrollToItem(messages.size - 1) }
                }
            } catch (_: Exception) {}
            delay(3000L)
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false }, title = { Text("Borrar Chat 🗑️", fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface,
            text = { Text("¿Estás seguro de que deseas borrar este historial de chat? Esta acción es irreversible para ambas partes.") },
            confirmButton = { Button(onClick = { coroutineScope.launch(Dispatchers.IO) { try { RetrofitInstance.api.clearChat(targetClientEmail); launch(Dispatchers.Main) { messages = emptyList(); showClearConfirm = false } } catch(_: Exception){} } }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Cancelar") } }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) }
                    Text(if (isAdmin) "Chat: ${targetClientEmail.substringBefore("@")}" else "Soporte Técnico 💬", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { showClearConfirm = true }) { Icon(Icons.Filled.Delete, "Borrar Chat", tint = Color.White) }
                }

                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), state = listState) {
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    if (messages.isEmpty()) { item { Text("No hay mensajes todavía. ¡Escribe algo para empezar!", color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) } }
                    items(messages) { msg ->
                        val isMe = msg.sender == currentUserEmail
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start) {
                            Box(modifier = Modifier.background(if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isMe) 16.dp else 4.dp, bottomEnd = if (isMe) 4.dp else 16.dp)).padding(12.dp).widthIn(max = 250.dp)) {
                                Column {
                                    Text(msg.text, color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                                    Text(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp)), fontSize = 10.sp, color = (if (isMe) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.6f), modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = inputText, onValueChange = { inputText = it }, modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribe un mensaje...") }, shape = RoundedCornerShape(24.dp), maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FloatingActionButton(onClick = {
                        if (inputText.isNotBlank()) {
                            focusManager.clearFocus()
                            val textToSend = inputText; inputText = ""
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val receiver = if (isAdmin) targetClientEmail else "zonacami77777@gmail.com"
                                    RetrofitInstance.api.sendMessage(ChatSendRequest(currentUserEmail, receiver, textToSend))
                                    val updated = RetrofitInstance.api.getChat(targetClientEmail)
                                    launch(Dispatchers.Main) { messages = updated; if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }
                                } catch (_: Exception) {}
                            }
                        }
                    }, containerColor = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) { Icon(Icons.Filled.Send, null, tint = Color.White) }
                }
            }
        }
    }
}


@Composable
fun PlanCardInfo(title: String, subtitle: String, features: List<String>, restrictions: List<String>) {
    Card(modifier = Modifier.width(280.dp).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.6f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))
            features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Icon(Icons.Filled.Check, contentDescription = "Permitido", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(feature, fontSize = 13.sp)
                }
            }
            if (restrictions.isNotEmpty()) { Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha=0.2f)) }
            restrictions.forEach { restriction ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp).alpha(0.5f)) {
                    Icon(Icons.Filled.Close, contentDescription = "No Permitido", tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(restriction, fontSize = 13.sp, textDecoration = TextDecoration.LineThrough)
                }
            }
        }
    }
}

// ==========================================
// 7. COMPONENTES DE LA TIENDA E INVENTARIO
// ==========================================
@Composable
fun StoreScreen(products: List<Product>, transactions: List<Transaction>, shoppingCart: List<Pair<Product, Int>>, isLockedStore: Boolean, totalStoreCash: Double, totalStoreDigital: Double, onOpenInventory: () -> Unit, onOpenCheckout: () -> Unit, onResetProfitsClick: () -> Unit, onDeleteVentas: (List<Transaction>) -> Unit, showPremiumToast: () -> Unit, totalProfit: Double) {
    val totalInventoryValue = remember(products) { products.sumOf { it.price * it.stock } }
    var showVendidosDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Valor del Inventario", color = Color.Gray, fontSize = 13.sp)
                        Text(text = formatCOP(totalInventoryValue), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).alpha(if(isLockedStore) 0.5f else 1f).clickable { if (isLockedStore) showPremiumToast() else onResetProfitsClick() }.padding(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Text(if(isLockedStore) "👑 Ganancias" else "Ganancias Obtenidas", color = Color.Gray, fontSize = 13.sp); Spacer(modifier = Modifier.width(4.dp)); Icon(Icons.Filled.Refresh, contentDescription = "Reiniciar", tint = Color.Gray, modifier = Modifier.size(12.dp)) }
                        Text(text = formatCOP(totalProfit), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4CAF50))
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (isLockedStore) showPremiumToast() else showVendidosDialog = true }, modifier = Modifier.weight(1f).height(50.dp).alpha(if(isLockedStore) 0.5f else 1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)) { Text(if(isLockedStore) "👑 Ventas" else "Ventas", fontWeight = FontWeight.Bold) }
            Button(onClick = { if (isLockedStore) showPremiumToast() else onOpenInventory() }, modifier = Modifier.weight(1f).height(50.dp).alpha(if(isLockedStore) 0.5f else 1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) { Text(if(isLockedStore) "👑 Inventario 📦" else "Inventario 📦", fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = shoppingCart.isNotEmpty()) {
            val totalCart = shoppingCart.sumOf { it.first.price * it.second }; val totalItems = shoppingCart.sumOf { it.second }
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp).clickable { onOpenCheckout() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2)), shape = RoundedCornerShape(12.dp)) { Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = Color.White); Spacer(Modifier.width(12.dp)); Column(modifier = Modifier.weight(1f)) { Text("Carrito activo ($totalItems artículos)", fontWeight = FontWeight.Bold, color = Color.White); Text("Total: ${formatCOP(totalCart)}", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f)) }; Icon(Icons.Filled.ArrowForward, contentDescription = "Cobrar", tint = Color.White) } }
        }
    }
    if (showVendidosDialog) ProductosVendidosDialog(transactions = transactions, onDismiss = { showVendidosDialog = false }, onDeleteVentas = onDeleteVentas)
}

@Composable
fun InventoryScreen(products: List<Product>, shoppingCart: List<Pair<Product, Int>>, onBack: () -> Unit, onAddProductClick: () -> Unit, onAddToCartClick: (Product) -> Unit, onOpenCheckout: () -> Unit, onEditClick: (Product) -> Unit, onDeleteClick: (Product) -> Unit, onLongDeleteClick: (Product) -> Unit, onInfoClick: (Product) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }; var sortBy by remember { mutableStateOf("A-Z") }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás", tint = MaterialTheme.colorScheme.onPrimary) }; Text("Inventario 📦", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.weight(1f)) }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Buscar en el inventario...") }, leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            ScrollableTabRow(selectedTabIndex = listOf("A-Z", "Poco Stock", "Vencimiento", "Precio", "Recientes").indexOf(sortBy), modifier = Modifier.fillMaxWidth(), edgePadding = 16.dp, containerColor = Color.Transparent, divider = {}, indicator = {}) { listOf("A-Z", "Poco Stock", "Vencimiento", "Precio", "Recientes").forEach { tab -> FilterChip(selected = sortBy == tab, onClick = { sortBy = tab }, label = { Text(tab) }, modifier = Modifier.padding(end = 8.dp)) } }
            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(visible = shoppingCart.isNotEmpty()) {
                val totalCart = shoppingCart.sumOf { it.first.price * it.second }; val totalItems = shoppingCart.sumOf { it.second }
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp).clickable { onOpenCheckout() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2)), shape = RoundedCornerShape(12.dp)) { Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = Color.White); Spacer(Modifier.width(12.dp)); Column(modifier = Modifier.weight(1f)) { Text("Carrito activo ($totalItems artículos)", fontWeight = FontWeight.Bold, color = Color.White); Text("Total: ${formatCOP(totalCart)}", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f)) }; Icon(Icons.Filled.ArrowForward, contentDescription = "Cobrar", tint = Color.White) } }
            }
            val sortedProducts = remember(products, searchQuery, sortBy) {
                val filtered = products.filter { it.name.contains(searchQuery, ignoreCase = true) }
                when (sortBy) { "A-Z" -> filtered.sortedBy { it.name.lowercase(Locale.getDefault()) }; "Precio" -> filtered.sortedByDescending { it.price }; "Vencimiento" -> filtered.sortedWith(compareBy<Product> { it.expirationDateInMillis == null }.thenBy { it.expirationDateInMillis }); "Recientes" -> filtered.sortedByDescending { it.entryDateInMillis }; "Poco Stock" -> filtered.filter { it.minStock > 0 && it.stock <= it.minStock }.sortedBy { it.stock }; else -> filtered }
            }
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                if (sortedProducts.isEmpty()) { item { Text("No se encontraron productos.", color = Color.Gray, modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center) } }
                items(sortedProducts, key = { it.id }) { product -> ProductItem(product = product, onAddToCart = { onAddToCartClick(product) }, onEdit = { onEditClick(product) }, onDelete = { onDeleteClick(product) }, onLongDelete = { onLongDeleteClick(product) }, onInfo = { onInfoClick(product) }) }
            }
        }
        FloatingActionButton(onClick = onAddProductClick, containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 16.dp)) { Icon(Icons.Filled.AddShoppingCart, contentDescription = "Agregar Producto") }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductItem(product: Product, onAddToCart: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onLongDelete: () -> Unit, onInfo: () -> Unit) {
    val isOutOfStock = product.stock <= 0; val isLowStock = product.minStock > 0 && product.stock <= product.minStock && !isOutOfStock
    val isExpired = product.expirationDateInMillis != null && product.expirationDateInMillis < System.currentTimeMillis()
    val unitColor = when (product.unit) { "Kg" -> Color(0xFFFF9800); "L" -> Color(0xFF03A9F4); else -> Color.Gray }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).combinedClickable(onClick = onInfo, onLongClick = onLongDelete), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(unitColor))
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(text = product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)); if (isExpired) { Spacer(modifier = Modifier.width(8.dp)); Surface(color = Color(0xFFD32F2F), shape = RoundedCornerShape(4.dp)) { Text("⚠️ VENCIDO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) } }; if (isLowStock) { Spacer(modifier = Modifier.width(8.dp)); Surface(color = Color(0xFFE65100), shape = RoundedCornerShape(4.dp)) { Text("⚠️ POCO STOCK", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) } } }
                    Text(text = "Precio: ${formatCOP(product.price)}", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    val totalStockVal = product.price * product.stock; if (totalStockVal > 0) { Text(text = "Total en stock: ${formatCOP(totalStockVal)}", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(text = "Stock: ${product.stock} ${product.unit}", color = if (isOutOfStock) Color.Red else Color.Gray, fontSize = 14.sp, fontWeight = if (isOutOfStock) FontWeight.Bold else FontWeight.Normal); if (product.expirationDateInMillis != null) { Spacer(modifier = Modifier.width(8.dp)); Text(text = "Vence: ${formatDateOnly(product.expirationDateInMillis)}", color = if (isExpired) Color.Red else Color.Gray, fontSize = 12.sp) } }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Button(onClick = onAddToCart, enabled = !isOutOfStock, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), modifier = Modifier.height(36.dp)) { Text("Añadir 🛒") }; Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(top = 4.dp)) { IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Edit, "Editar", tint = Color.Blue, modifier = Modifier.size(20.dp)) }; IconButton(onClick = { if (product.stock <= 0) onLongDelete() else onDelete() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Delete, "Borrar", tint = Color.Red, modifier = Modifier.size(20.dp)) } } }
            }
        }
    }
}

// ==========================================
// 8. DIÁLOGOS Y COMPONENTES REUTILIZABLES
// ==========================================
@Composable
fun CustomDatePickerDialog(initialDateMillis: Long?, onDismiss: () -> Unit, onDateSelected: (Long) -> Unit) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); if (initialDateMillis != null) { timeInMillis = initialDateMillis; set(Calendar.DAY_OF_MONTH, 1) } }) }
    var selectedDate by remember { mutableStateOf<Calendar?>(initialDateMillis?.let { Calendar.getInstance().apply { timeInMillis = it } }) }
    val formatMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth().padding(16.dp), containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Seleccionar Fecha 🗓️", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { val newCal = currentMonth.clone() as Calendar; newCal.add(Calendar.MONTH, -1); newCal.set(Calendar.DAY_OF_MONTH, 1); currentMonth = newCal }) { Icon(Icons.Filled.ChevronLeft, "Anterior") }
                    Text(text = formatMonth.format(currentMonth.time).replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { val newCal = currentMonth.clone() as Calendar; newCal.add(Calendar.MONTH, 1); newCal.set(Calendar.DAY_OF_MONTH, 1); currentMonth = newCal }) { Icon(Icons.Filled.ChevronRight, "Siguiente") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb").forEach {
                        Text(text = it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                val tempCal = currentMonth.clone() as Calendar
                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1
                val totalCells = daysInMonth + firstDayOfWeek
                val rows = (totalCells + 6) / 7

                Column(modifier = Modifier.fillMaxWidth()) {
                    for (i in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (j in 0..6) {
                                val cellIndex = i * 7 + j
                                val dayNumber = cellIndex - firstDayOfWeek + 1
                                if (dayNumber in 1..daysInMonth) {
                                    val dayCal = currentMonth.clone() as Calendar
                                    dayCal.set(Calendar.DAY_OF_MONTH, dayNumber)
                                    val isDaySelected = selectedDate != null && isSameDay(selectedDate!!.timeInMillis, dayCal.timeInMillis)
                                    Box(
                                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(CircleShape).background(if (isDaySelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent).clickable { selectedDate = dayCal.clone() as Calendar },
                                        contentAlignment = Alignment.Center
                                    ) { Text(text = dayNumber.toString(), fontSize = 14.sp, fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Normal, color = if (isDaySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
                                } else { Box(modifier = Modifier.weight(1f).aspectRatio(1f)) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { if (selectedDate != null) onDateSelected(selectedDate!!.timeInMillis) }, enabled = selectedDate != null) { Text("Seleccionar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun AddToCartDialog(product: Product, currentCartQty: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var qtyRaw by remember { mutableStateOf("1") }; val maxAvailable = product.stock - currentCartQty
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Añadir al Carrito 🛒", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface, text = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("Disponible para añadir: $maxAvailable ${product.unit}", color = Color.Gray); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = qtyRaw, onValueChange = { n -> val d = n.filter { it.isDigit() }; qtyRaw = d }, label = { Text("Cantidad") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(0.6f), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp)) } }, confirmButton = { Button(onClick = { val q = qtyRaw.toIntOrNull() ?: 0; if (q > 0 && q <= maxAvailable) { onConfirm(q) } }) { Text("Añadir") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
fun PaymentInputRow(name: String, amountRaw: String, onAmountChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(name, modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold); OutlinedTextField(value = amountRaw, onValueChange = { onAmountChange(cleanAmountInput(it)) }, modifier = Modifier.weight(1.5f).height(54.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, visualTransformation = AmountVisualTransformation(), leadingIcon = { Text("$", color = Color.Gray, modifier = Modifier.padding(start=8.dp)) }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutDialog(
    cartItems: MutableList<Pair<Product, Int>>,
    products: List<Product>,
    totalStoreCash: Double,
    totalStoreDigital: Double,
    onDismiss: () -> Unit,
    onConfirmSale: (List<Pair<Product, Int>>, String, String, Double, Double) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var buyerName by remember { mutableStateOf("") }
    var isDivided by remember { mutableStateOf(false) }
    var simpleMethod by remember { mutableStateOf("Efectivo") }
    var simpleReceivedRaw by remember { mutableStateOf("") }
    var cashRaw by remember { mutableStateOf("") }
    var digitalRaw by remember { mutableStateOf("") }
    var cashChangeRaw by remember { mutableStateOf("") }
    var digitalChangeRaw by remember { mutableStateOf("") }
    var itemToEdit by remember { mutableStateOf<Pair<Int, Pair<Product, Int>>?>(null) }

    var showProductSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var productToSelectQty by remember { mutableStateOf<Product?>(null) }

    var pocketChange by remember { mutableStateOf(false) }

    val totalCOP = cartItems.sumOf { it.first.price * it.second }

    if (itemToEdit != null) {
        var editQtyRaw by remember { mutableStateOf(itemToEdit!!.second.second.toString()) }
        AlertDialog(
            onDismissRequest = { itemToEdit = null },
            title = { Text("Editar cantidad de ${itemToEdit!!.second.first.name}") },
            text = {
                OutlinedTextField(
                    value = editQtyRaw,
                    onValueChange = { editQtyRaw = it.filter { c -> c.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Nueva Cantidad") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newQty = editQtyRaw.toIntOrNull() ?: 0
                    if (newQty > 0 && newQty <= itemToEdit!!.second.first.stock) {
                        cartItems[itemToEdit!!.first] = itemToEdit!!.second.first to newQty
                        itemToEdit = null
                    } else if (newQty == 0) {
                        cartItems.removeAt(itemToEdit!!.first)
                        itemToEdit = null
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { itemToEdit = null }) { Text("Cancelar") } }
        )
    }

    if (productToSelectQty != null) {
        var qtyRaw by remember { mutableStateOf("1") }
        val p = productToSelectQty!!
        val currentInCart = cartItems.find { it.first.id == p.id }?.second ?: 0
        val maxAvailable = p.stock - currentInCart

        AlertDialog(
            onDismissRequest = { productToSelectQty = null },
            title = { Text("Añadir ${p.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Disponible: $maxAvailable ${p.unit}", color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = qtyRaw,
                        onValueChange = { qtyRaw = it.filter { c -> c.isDigit() } },
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.6f),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val q = qtyRaw.toIntOrNull() ?: 0
                    if (q > 0 && q <= maxAvailable) {
                        val existing = cartItems.find { it.first.id == p.id }
                        if (existing != null) {
                            val idx = cartItems.indexOf(existing)
                            cartItems[idx] = existing.copy(second = existing.second + q)
                        } else {
                            cartItems.add(Pair(p, q))
                        }
                        productToSelectQty = null
                        showProductSearch = false
                        searchQuery = ""
                    }
                }) { Text("Añadir") }
            },
            dismissButton = { TextButton(onClick = { productToSelectQty = null }) { Text("Cancelar") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (showProductSearch) "Buscar Producto 🔍" else if (step == 1) "Resumen de Venta 🛒" else "Opciones de Pago 💳", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (showProductSearch) {
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Nombre del producto...") }, leadingIcon = { Icon(Icons.Filled.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    val filteredProducts = products.filter { it.name.contains(searchQuery, ignoreCase = true) && it.stock > 0 }
                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                        items(filteredProducts) { p ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { productToSelectQty = p }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Stock: ${p.stock} ${p.unit}", color = Color.Gray, fontSize = 12.sp)
                                }
                                Text(formatCOP(p.price), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Divider(color = Color.Gray.copy(alpha = 0.2f))
                        }
                    }
                } else if (step == 1) {
                    if (cartItems.isEmpty()) {
                        Text("El carrito está vacío.", modifier = Modifier.padding(16.dp))
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                            itemsIndexed(cartItems) { index, item ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text("${item.second}x ${item.first.name}", modifier = Modifier.weight(1f), fontSize = 14.sp, maxLines=1, overflow = TextOverflow.Ellipsis)
                                    Text(formatCOP(item.first.price * item.second), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    IconButton(onClick = { itemToEdit = Pair(index, item) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Edit, "Editar", tint = Color.Blue, modifier = Modifier.size(16.dp)) }
                                    IconButton(onClick = { cartItems.removeAt(index) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Delete, "Eliminar", tint = Color.Red, modifier = Modifier.size(16.dp)) }
                                }
                            }
                        }

                        TextButton(onClick = { showProductSearch = true }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp)) {
                            Text("+ Agregar producto nuevo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Total a cobrar:", fontSize = 12.sp, color = Color.Gray)
                        Text(formatCOP(totalCOP), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(value = buyerName, onValueChange = { buyerName = it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() } }, label = { Text("Nombre del Cliente (Opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth().background(Color.DarkGray.copy(alpha=0.2f), RoundedCornerShape(8.dp)).padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Caja (Efectivo)", fontSize=10.sp, color=Color.Gray)
                            Text(formatCOP(totalStoreCash), fontSize=13.sp, fontWeight=FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Banco (Digital)", fontSize=10.sp, color=Color.Gray)
                            Text(formatCOP(totalStoreDigital), fontSize=13.sp, fontWeight=FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Total de la compra:", fontSize = 13.sp)
                    Text(formatCOP(totalCOP), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isDivided = !isDivided; pocketChange = false }) {
                        Switch(checked = isDivided, onCheckedChange = { isDivided = it; pocketChange = false })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isDivided) "Pago Dividido Múltiple" else "Pago Único", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isDivided) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            FilterChip(selected = simpleMethod == "Efectivo", onClick = { simpleMethod = "Efectivo" }, label = { Text("Efectivo") })
                            FilterChip(selected = simpleMethod == "Digital", onClick = { simpleMethod = "Digital" }, label = { Text("Digital") })
                        }
                        PaymentInputRow("Monto Recibido", simpleReceivedRaw, { simpleReceivedRaw = it })

                        val rec = simpleReceivedRaw.toDoubleOrNull() ?: 0.0
                        val change = rec - totalCOP

                        if (change > 0) {
                            Text("Vuelto a devolver: ${formatCOP(change)}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { pocketChange = !pocketChange }) {
                                Checkbox(checked = pocketChange, onCheckedChange = { pocketChange = it })
                                Text("Sacar vuelto de mi bolsillo", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            if (pocketChange) {
                                Text("El vuelto se dará de tu bolsillo personal. La ganancia ingresará completa a la tienda.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                            } else {
                                val hasEnoughFunds = if (simpleMethod == "Efectivo") change <= totalStoreCash else change <= totalStoreDigital
                                if (!hasEnoughFunds) {
                                    Text("⚠️ Fondos insuficientes en ${if(simpleMethod == "Efectivo") "Caja" else "Banco"} para dar este vuelto.", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                } else {
                                    Text("El vuelto saldrá de: $simpleMethod", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontSize = 12.sp)
                                }
                            }
                        } else if (change < 0 && rec > 0) {
                            Text("Falta: ${formatCOP(abs(change))}", color = Color.Red, fontWeight = FontWeight.Bold)
                        } else if (change == 0.0 && rec > 0) {
                            Text("Pago exacto ✅", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        PaymentInputRow("Efectivo Recibido", cashRaw, { cashRaw = it })
                        PaymentInputRow("Digital Recibido", digitalRaw, { digitalRaw = it })
                        val cV = cashRaw.toDoubleOrNull() ?: 0.0
                        val qV = digitalRaw.toDoubleOrNull() ?: 0.0
                        val receivedCOP = cV + qV
                        val changeCOP = receivedCOP - totalCOP

                        if (changeCOP > 0) {
                            Text("Vuelto a devolver: ${formatCOP(changeCOP)}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { pocketChange = !pocketChange }) {
                                Checkbox(checked = pocketChange, onCheckedChange = { pocketChange = it })
                                Text("Sacar vuelto de mi bolsillo", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            if (pocketChange) {
                                Text("El vuelto se dará de tu bolsillo. La ganancia ingresará intacta.", fontSize = 12.sp, color = Color.Gray)
                            } else {
                                Text("¿De dónde darás el vuelto?", fontSize = 12.sp, color = Color.Gray)
                                PaymentInputRow("Vuelto Efectivo", cashChangeRaw, { cashChangeRaw = it })
                                PaymentInputRow("Vuelto Digital", digitalChangeRaw, { digitalChangeRaw = it })
                                val cc = cashChangeRaw.toDoubleOrNull() ?: 0.0
                                val dc = digitalChangeRaw.toDoubleOrNull() ?: 0.0

                                if (cc > totalStoreCash) {
                                    Text("⚠️ No tienes suficiente Efectivo en Caja.", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else if (dc > totalStoreDigital) {
                                    Text("⚠️ No tienes suficiente dinero en Banco.", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else if (cc + dc != changeCOP) {
                                    Text("La suma del vuelto no cuadra con ${formatCOP(changeCOP)}", color = Color.Red, fontSize = 11.sp)
                                } else {
                                    Text("Vuelto distribuido correctamente ✅", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (changeCOP < 0 && receivedCOP > 0) {
                            Text("Falta dinero: ${formatCOP(abs(changeCOP))}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else if (changeCOP == 0.0 && receivedCOP > 0) {
                            Text("Pago completo y exacto ✅", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showProductSearch) {
                // No confirm button needed here
            } else if (step == 1) {
                Button(onClick = { step = 2 }, enabled = cartItems.isNotEmpty()) { Text("Siguiente") }
            } else {
                var isEnabled = false
                if (!isDivided) {
                    val rec = simpleReceivedRaw.toDoubleOrNull() ?: 0.0
                    val change = rec - totalCOP
                    if (rec >= totalCOP) {
                        if (pocketChange) {
                            isEnabled = true
                        } else {
                            val hasEnoughFunds = if (simpleMethod == "Efectivo") change <= totalStoreCash else change <= totalStoreDigital
                            if (hasEnoughFunds) isEnabled = true
                        }
                    }
                } else {
                    val cV = cashRaw.toDoubleOrNull() ?: 0.0
                    val qV = digitalRaw.toDoubleOrNull() ?: 0.0
                    val receivedCOP = cV + qV
                    val changeCOP = receivedCOP - totalCOP
                    if (changeCOP == 0.0) isEnabled = true
                    if (changeCOP > 0.0) {
                        if (pocketChange) {
                            isEnabled = true
                        } else {
                            val cc = cashChangeRaw.toDoubleOrNull() ?: 0.0
                            val dc = digitalChangeRaw.toDoubleOrNull() ?: 0.0
                            if (cc + dc == changeCOP && cc <= totalStoreCash && dc <= totalStoreDigital) isEnabled = true
                        }
                    }
                }

                Button(
                    onClick = {
                        var netC = 0.0
                        var netD = 0.0
                        var summary = ""

                        if (!isDivided) {
                            val rec = simpleReceivedRaw.toDoubleOrNull() ?: 0.0
                            val change = rec - totalCOP
                            if (simpleMethod == "Efectivo") {
                                netC = totalCOP
                                summary = "Pago Efectivo: ${formatCOP(rec)}" + if(change>0) " | Vuelto: ${formatCOP(change)}" + if(pocketChange) " (De bolsillo)" else "" else ""
                            } else {
                                netD = totalCOP
                                summary = "Pago Digital: ${formatCOP(rec)}" + if(change>0) " | Vuelto: ${formatCOP(change)}" + if(pocketChange) " (De bolsillo)" else "" else ""
                            }
                        } else {
                            val cV = cashRaw.toDoubleOrNull() ?: 0.0
                            val dV = digitalRaw.toDoubleOrNull() ?: 0.0
                            val changeCOP = (cV + dV) - totalCOP

                            if (pocketChange) {
                                netC = cV
                                netD = dV
                                summary = "Efectivo recibido: ${formatCOP(cV)} | Digital recibido: ${formatCOP(dV)}"
                                if (changeCOP > 0) summary += "\nVuelto: ${formatCOP(changeCOP)} (De bolsillo)"
                            } else {
                                val cc = cashChangeRaw.toDoubleOrNull() ?: 0.0
                                val dc = digitalChangeRaw.toDoubleOrNull() ?: 0.0
                                netC = cV - cc
                                netD = dV - dc
                                summary = "Efectivo recibido: ${formatCOP(cV)} | Digital recibido: ${formatCOP(dV)}"
                                if (cc > 0 || dc > 0) {
                                    summary += "\nVuelto Efectivo: ${formatCOP(cc)} | Vuelto Digital: ${formatCOP(dc)}"
                                }
                            }
                        }
                        onConfirmSale(cartItems.toList(), buyerName.trim(), summary, netC, netD)
                    },
                    enabled = isEnabled
                ) { Text("Confirmar Venta") }
            }
        },
        dismissButton = {
            if (showProductSearch) {
                TextButton(onClick = { showProductSearch = false }) { Text("Volver al carrito") }
            } else {
                TextButton(onClick = { if (step == 2) step = 1 else onDismiss() }) { Text(if (step == 2) "Atrás" else "Cancelar") }
            }
        }
    )
}

@Composable
fun ProductInfoDialog(product: Product, onDismiss: () -> Unit, onDeleteCompletely: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Detalle del Producto ℹ️", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, containerColor = MaterialTheme.colorScheme.surface, text = { Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text(product.name, fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(16.dp)); Text("Precio Unitario: ${formatCOP(product.price)}", fontSize = 14.sp); Spacer(modifier = Modifier.height(8.dp)); Text("Stock Disponible: ${product.stock} ${product.unit}", fontSize = 14.sp); Spacer(modifier = Modifier.height(16.dp)); Divider(color = Color.Gray.copy(alpha = 0.2f)); Spacer(modifier = Modifier.height(16.dp)); val totalUsd = product.price * product.stock; Text("Valor Total en Inventario:", fontSize = 14.sp, color = Color.Gray); Text(formatCOP(totalUsd), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary) } }, confirmButton = { Button(onClick = onDismiss) { Text("Cerrar") } }, dismissButton = { TextButton(onClick = onDeleteCompletely, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Text("Eliminar del Inventario", fontWeight = FontWeight.Bold) } })
}

@Composable
fun DashboardCard(balance: Double, income: Double, expense: Double) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Saldo Actual", color = Color.Gray, fontSize = 16.sp); Text(text = formatCOP(balance), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = if (balance >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)); Spacer(modifier = Modifier.height(24.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.clip(CircleShape).background(Color(0xFFE8F5E9).copy(alpha = 0.2f)).padding(4.dp)) { Text("🟢", fontSize = 12.sp) }; Spacer(modifier = Modifier.width(4.dp)); Text("Ingresos", color = Color.Gray) }; Text(formatCOP(income), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)) }; Column(horizontalAlignment = Alignment.CenterHorizontally) { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.clip(CircleShape).background(Color(0xFFFFEBEE).copy(alpha = 0.2f)).padding(4.dp)) { Text("🔴", fontSize = 12.sp) }; Spacer(modifier = Modifier.width(4.dp)); Text("Gastos", color = Color.Gray) }; Text(formatCOP(expense), fontWeight = FontWeight.Bold, color = Color(0xFFF44336)) } } } }
}

@Composable
fun TransactionItem(transaction: Transaction, onDelete: () -> Unit) {
    val isIncome = transaction.isIncome; val color = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336); val emoji = getSmartEmoji(transaction.description, isIncome)
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Text(text = emoji, fontSize = 24.sp) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) { Text(text = transaction.description, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis); if (transaction.note.isNotBlank()) Text(text = transaction.note, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 2.dp), maxLines = 1, overflow = TextOverflow.Ellipsis); Text(text = formatDate(transaction.timestamp), color = Color.Gray.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1) }; Column(horizontalAlignment = Alignment.End) { Text(text = "${if (isIncome) "+" else "-"}${formatCOP(transaction.amount)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color, maxLines = 1) }; IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Delete, "Eliminar", tint = Color.Gray, modifier = Modifier.size(20.dp)) } } }
}

@Composable
fun AddTransactionDialog(onDismiss: () -> Unit, onConfirm: (String, Double, Boolean, String) -> Unit) {
    var isIncome by remember { mutableStateOf(true) }; var descIncome by remember { mutableStateOf("") }; var amountIncome by remember { mutableStateOf("") }; var descExpense by remember { mutableStateOf("") }; var amountExpense by remember { mutableStateOf("") }; var noteExpense by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nuevo Movimiento ✍️", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface, text = { Column { Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(4.dp)) { Button(onClick = { isIncome = true }, colors = ButtonDefaults.buttonColors(containerColor = if (isIncome) Color(0xFF4CAF50) else Color.Transparent, contentColor = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurface), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f), elevation = null) { Text("Ingresos 👝", fontWeight = FontWeight.Bold) }; Button(onClick = { isIncome = false }, colors = ButtonDefaults.buttonColors(containerColor = if (!isIncome) Color(0xFFF44336) else Color.Transparent, contentColor = if (!isIncome) Color.White else MaterialTheme.colorScheme.onSurface), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f), elevation = null) { Text("Gastos 👛", fontWeight = FontWeight.Bold) } }; Spacer(modifier = Modifier.height(16.dp)); Crossfade(targetState = isIncome, label = "") { showIncome -> Column { if (showIncome) { OutlinedTextField(value = descIncome, onValueChange = { input -> descIncome = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences)); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = amountIncome, onValueChange = { amountIncome = cleanAmountInput(it) }, label = { Text("Monto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = AmountVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth()) } else { OutlinedTextField(value = descExpense, onValueChange = { input -> descExpense = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences)); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = amountExpense, onValueChange = { amountExpense = cleanAmountInput(it) }, label = { Text("Monto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = AmountVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = noteExpense, onValueChange = { input -> noteExpense = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }, label = { Text("Nota (Opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences)) } } } } }, confirmButton = { Button(onClick = { if (isIncome) { val a = amountIncome.toDoubleOrNull(); if (descIncome.isNotBlank() && a != null) { val capTitle = descIncome.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }; onConfirm(capTitle, a, true, "") } } else { val a = amountExpense.toDoubleOrNull(); if (descExpense.isNotBlank() && a != null) { val capTitle = descExpense.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }; onConfirm(capTitle, a, false, noteExpense.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) } } }) { Text("Guardar ✔️") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar ❌") } })
}

@Composable
fun LimitDialog(currentLimit: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    val initial = if(currentLimit > 0) currentLimit.toLong().toString() else ""; var amountRaw by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Saldo Crítico 🔔") }, containerColor = MaterialTheme.colorScheme.surface, text = { Column { Text("Te avisaremos si tu saldo baja de esta cantidad (Déjalo en 0 para apagar):", fontSize = 14.sp); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = amountRaw, onValueChange = { amountRaw = cleanAmountInput(it) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), leadingIcon = { Icon(Icons.Filled.AttachMoney, null) }, singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = AmountVisualTransformation()) } }, confirmButton = { Button(onClick = { onConfirm(amountRaw.toDoubleOrNull() ?: 0.0) }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
fun SummaryDialog(totalIncome: Double, totalExpense: Double, balance: Double, transactionCount: Int, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Resumen de Totales 📊", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, containerColor = MaterialTheme.colorScheme.surface, text = { Column(modifier = Modifier.fillMaxWidth()) { Text("Aquí tienes el balance histórico general de todos tus movimientos personales:", fontSize = 14.sp, color = Color.Gray); Spacer(modifier = Modifier.height(16.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total Ingresos:", fontWeight = FontWeight.Bold); Column(horizontalAlignment = Alignment.End) { Text(formatCOP(totalIncome), color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold) } }; Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total Gastado:", fontWeight = FontWeight.Bold); Column(horizontalAlignment = Alignment.End) { Text(formatCOP(totalExpense), color = Color(0xFFF44336), fontWeight = FontWeight.Bold) } }; Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Balance Total:", fontWeight = FontWeight.Bold); Column(horizontalAlignment = Alignment.End) { Text(formatCOP(balance), color = if (balance >= 0) Color(0xFF4CAF50) else Color(0xFFE53935), fontWeight = FontWeight.Bold) } }; Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Cant. Movimientos:", fontWeight = FontWeight.Bold); Text("$transactionCount", fontWeight = FontWeight.Bold) } } }, confirmButton = { Button(onClick = onDismiss) { Text("Entendido") } })
}

@Composable
fun SoundSettingsDialog(currentSound: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    val options = listOf("Silencio 🔕", "Bip Simple 🎵", "Doble Bip 🎶", "Interferencia 📻", "Alerta Corta ⚠️", "Timbre 🛎️", "Notificación 🔔", "Éxito ✅", "Error ❌", "Llamada 📞", "Ocupado 📵")
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Sonidos de la App") }, containerColor = MaterialTheme.colorScheme.surface, text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) { options.forEachIndexed { index, name -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = currentSound == index, onClick = { onSelect(index) }); Text(text = name, modifier = Modifier.padding(start = 8.dp)) } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}

@Composable
fun CalendarDialog(
    reminders: List<Reminder>,
    fiadores: List<Fiador>,
    products: List<Product>,
    onDismiss: () -> Unit,
    onDayClick: (Long, Boolean) -> Unit,
    onViewReminders: () -> Unit,
    onViewFiadores: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    val formatMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Fiadores y Deudas 🗓️", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { val newCal = currentMonth.clone() as Calendar; newCal.add(Calendar.MONTH, -1); newCal.set(Calendar.DAY_OF_MONTH, 1); currentMonth = newCal }) { Icon(Icons.Filled.ChevronLeft, "Anterior") }
                    Text(text = formatMonth.format(currentMonth.time).replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { val newCal = currentMonth.clone() as Calendar; newCal.add(Calendar.MONTH, 1); newCal.set(Calendar.DAY_OF_MONTH, 1); currentMonth = newCal }) { Icon(Icons.Filled.ChevronRight, "Siguiente") }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb").forEach {
                        Text(text = it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                val tempCal = currentMonth.clone() as Calendar
                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1
                val totalCells = daysInMonth + firstDayOfWeek
                val rows = (totalCells + 6) / 7

                Column(modifier = Modifier.fillMaxWidth()) {
                    for (i in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (j in 0..6) {
                                val cellIndex = i * 7 + j
                                val dayNumber = cellIndex - firstDayOfWeek + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val dayCal = currentMonth.clone() as Calendar
                                    dayCal.set(Calendar.DAY_OF_MONTH, dayNumber)

                                    val hasReminder = reminders.any { isSameDay(it.targetDateInMillis, dayCal.timeInMillis) }
                                    val hasFiador = fiadores.any { isSameDay(it.targetDateInMillis, dayCal.timeInMillis) }
                                    val hasProduct = products.any { it.expirationDateInMillis != null && isSameDay(it.expirationDateInMillis, dayCal.timeInMillis) }
                                    val hasEvents = hasReminder || hasFiador || hasProduct

                                    val bgColor = when {
                                        hasProduct -> Color(0xFFD32F2F)
                                        hasReminder -> Color(0xFF1976D2)
                                        hasFiador -> Color(0xFFFBC02D)
                                        else -> Color.Transparent
                                    }

                                    val textColor = if (bgColor == Color.Transparent) MaterialTheme.colorScheme.onSurface else Color.White

                                    Box(
                                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(CircleShape).background(bgColor).clickable {
                                            onDayClick(dayCal.timeInMillis, hasEvents)
                                        },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = dayNumber.toString(), fontSize = 14.sp, fontWeight = if (hasEvents) FontWeight.Bold else FontWeight.Normal, color = textColor)
                                    }
                                } else {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onViewReminders, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1976D2))) { Text("Ver Deudas", fontWeight = FontWeight.Bold) }
                TextButton(onClick = onViewFiadores, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFBC02D))) { Text("Ver Cobros", fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(initialProduct: Product? = null, onDismiss: () -> Unit, onConfirm: (String, Double, Double, Int, String, Long?, Int) -> Unit) {
    val context = LocalContext.current; var name by remember { mutableStateOf(initialProduct?.name ?: "") }; val initialPurchase = initialProduct?.purchasePrice?.let { if(it > 0) it.toLong().toString() else "" } ?: ""; var purchasePriceRaw by remember { mutableStateOf(initialPurchase) }; val initialPrice = initialProduct?.price?.let { it.toLong().toString() } ?: ""; var priceRaw by remember { mutableStateOf(initialPrice) }; var stockRaw by remember { mutableStateOf(initialProduct?.stock?.toString() ?: "") }; var minStockRaw by remember { mutableStateOf(if ((initialProduct?.minStock ?: 0) > 0) initialProduct!!.minStock.toString() else "") }; var selectedUnit by remember { mutableStateOf(initialProduct?.unit ?: "Uds") }; var hasExpiry by remember { mutableStateOf(initialProduct?.expirationDateInMillis != null) }; var expiryDateMillis by remember { mutableStateOf<Long?>(initialProduct?.expirationDateInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        CustomDatePickerDialog(initialDateMillis = expiryDateMillis ?: System.currentTimeMillis(), onDismiss = { showDatePicker = false }, onDateSelected = { selected -> expiryDateMillis = selected; showDatePicker = false })
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initialProduct != null) "Editar Producto ✏️" else "Nuevo Producto 🏷️", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface, text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) { OutlinedTextField(value = name, onValueChange = { input -> name = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }, label = { Text("Nombre del Producto") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences)); Spacer(modifier = Modifier.height(12.dp)); OutlinedTextField(value = purchasePriceRaw, onValueChange = { purchasePriceRaw = cleanAmountInput(it) }, label = { Text("Precio de Compra") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = AmountVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Text("$", color = Color.Gray, modifier = Modifier.padding(start=8.dp)) }); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = priceRaw, onValueChange = { priceRaw = cleanAmountInput(it) }, label = { Text("Precio de Venta") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = AmountVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Text("$", color = Color.Gray, modifier = Modifier.padding(start=8.dp)) }); Spacer(modifier = Modifier.height(12.dp)); Text("Unidad de Medida", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) { FilterChip(selected = selectedUnit == "Uds", onClick = { selectedUnit = "Uds" }, label = { Text("Unidad") }); FilterChip(selected = selectedUnit == "Kg", onClick = { selectedUnit = "Kg" }, label = { Text("Kilos") }); FilterChip(selected = selectedUnit == "L", onClick = { selectedUnit = "L" }, label = { Text("Litros") }) }; Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = stockRaw, onValueChange = { n -> val d = n.filter { it.isDigit() }; stockRaw = d }, label = { Text("Cantidad Inicial en Stock") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = minStockRaw, onValueChange = { n -> val d = n.filter { it.isDigit() }; minStockRaw = d }, label = { Text("Alerta de cantidad baja") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { hasExpiry = !hasExpiry }) { Checkbox(checked = hasExpiry, onCheckedChange = { hasExpiry = it }); Text("Tiene fecha de vencimiento", fontSize = 14.sp) }; if (hasExpiry) { Spacer(modifier = Modifier.height(4.dp)); OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(if (expiryDateMillis == null) "Seleccionar Fecha 📅" else "Vence: ${formatDateOnly(expiryDateMillis!!)}") } } } }, confirmButton = { Button(onClick = { val parsedPrice = priceRaw.toDoubleOrNull() ?: 0.0; val parsedPurchase = purchasePriceRaw.toDoubleOrNull() ?: 0.0; val s = stockRaw.toIntOrNull(); val minS = minStockRaw.toIntOrNull() ?: 0; if (name.isNotBlank() && parsedPrice > 0 && s != null) { val capName = name.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }; onConfirm(capName, parsedPurchase, parsedPrice, s, selectedUnit, if (hasExpiry) expiryDateMillis else null, minS) } else { Toast.makeText(context, "Llena los campos correctamente", Toast.LENGTH_SHORT).show() } }) { Text("Guardar ✔️") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar ❌") } })
}

@Composable
fun DeleteQuantityDialog(product: Product, initialQty: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var qtyRaw by remember { mutableStateOf(initialQty) }; val context = LocalContext.current
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Eliminar Stock 🗑️", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface, text = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("¿Cuántas unidades de '${product.name}' deseas eliminar?", textAlign = TextAlign.Center, fontSize = 14.sp); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = qtyRaw, onValueChange = { n -> val d = n.filter { it.isDigit() }; qtyRaw = d }, label = { Text("Cantidad") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(0.6f), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp)); Text("Stock actual: ${product.stock} ${product.unit}", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) } }, confirmButton = { Button(onClick = { val q = qtyRaw.toIntOrNull() ?: -1; if (q > product.stock) { Toast.makeText(context, "Supera el stock actual.", Toast.LENGTH_SHORT).show() } else if (q <= 0) { Toast.makeText(context, "No se puede eliminar 0.", Toast.LENGTH_SHORT).show() } else { onConfirm(q) } }) { Text("Siguiente") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
fun RedWarningDialog(productName: String, qty: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFFD32F2F), titleContentColor = Color.White, textContentColor = Color.White, title = { Text("¡Acción Irreversible! ⚠️", fontWeight = FontWeight.Bold) }, text = { Text("Estás a punto de eliminar $qty unidades de '$productName' de tu inventario. ¿Estás seguro?") }, confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFD32F2F))) { Text("Sí, Eliminar", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("Cancelar") } })
}

@Composable
fun ProductosVendidosDialog(transactions: List<Transaction>, onDismiss: () -> Unit, onDeleteVentas: (List<Transaction>) -> Unit) {
    val context = LocalContext.current; val ventas = remember(transactions) { transactions.filter { it.isIncome && it.description.startsWith("Venta:") } }; var searchQuery by remember { mutableStateOf("") }; val filteredVentas = remember(ventas, searchQuery) { ventas.filter { sale -> sale.description.contains(searchQuery, ignoreCase = true) || sale.note.contains(searchQuery, ignoreCase = true) || formatDate(sale.timestamp).contains(searchQuery, ignoreCase = true) } }; val totalMonto = remember(filteredVentas) { filteredVentas.sumOf { it.amount } }; val totalGanancia = remember(filteredVentas) { filteredVentas.sumOf { it.profit } }; var showConfirmDelete by remember { mutableStateOf(false) }
    if (showConfirmDelete) { AlertDialog(onDismissRequest = { showConfirmDelete = false }, title = { Text("Limpiar Historial ⚠️", fontWeight = FontWeight.Bold) }, text = { Text("¿Estás seguro de que deseas borrar este historial de ventas?\n\nEsta acción eliminará permanentemente todos los registros mostrados actualmente.") }, confirmButton = { Button(onClick = { onDeleteVentas(filteredVentas); showConfirmDelete = false; onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))) { Text("Limpiar Todo", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { showConfirmDelete = false }) { Text("Cancelar") } }) }
    AlertDialog(onDismissRequest = onDismiss, title = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Productos Vendidos 🛍️", fontWeight = FontWeight.Bold, fontSize = 20.sp); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } }, containerColor = MaterialTheme.colorScheme.surface, text = { Column(modifier = Modifier.fillMaxSize()) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { if(filteredVentas.isEmpty()) { Toast.makeText(context, "No hay ventas para exportar", Toast.LENGTH_SHORT).show(); return@OutlinedButton }; val reporte = buildString { appendLine("📊 REPORTE DE VENTAS"); appendLine("Fecha de Exportación: ${formatDate(System.currentTimeMillis())}"); appendLine("--------------------------------"); filteredVentas.forEachIndexed { index, sale -> appendLine("Venta #${filteredVentas.size - index} - ${formatDateOnly(sale.timestamp)}"); appendLine(sale.note); appendLine("Total: ${formatCOP(sale.amount)} | Ganancia: ${formatCOP(sale.profit)}"); appendLine("--------------------------------") }; appendLine("TOTAL VENTAS: ${formatCOP(totalMonto)}"); appendLine("TOTAL GANANCIA: ${formatCOP(totalGanancia)}") }; val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, reporte) }; context.startActivity(Intent.createChooser(intent, "Exportar Reporte")) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Filled.Share, contentDescription = "Exportar", modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Exportar", fontSize = 12.sp) }; OutlinedButton(onClick = { if(filteredVentas.isNotEmpty()) showConfirmDelete = true else Toast.makeText(context, "No hay ventas para limpiar", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Filled.Delete, contentDescription = "Limpiar", modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Limpiar", fontSize = 12.sp) } }; Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Buscar...") }, leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") }, modifier = Modifier.fillMaxWidth().height(50.dp), singleLine = true, shape = RoundedCornerShape(12.dp)); Spacer(modifier = Modifier.height(12.dp)); if (filteredVentas.isEmpty()) { Text("No se encontraron ventas registradas.", color = Color.Gray, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center) } else { LazyColumn(modifier = Modifier.fillMaxSize()) { itemsIndexed(filteredVentas) { index, sale -> Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) { Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) { Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text(formatDate(sale.timestamp), fontSize = 11.sp, color = Color.Gray); Text(formatCOP(sale.amount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }; Spacer(modifier = Modifier.height(4.dp)); Text(sale.note, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface); if (sale.profit > 0) { Spacer(modifier = Modifier.height(6.dp)); Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) { Text("Ganancia: ${formatCOP(sale.profit)}", fontSize = 11.sp, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold) } } } } } } } } }, confirmButton = {} )
}

@Composable
fun ScheduledRemindersDialog(reminders: List<Reminder>, onDismiss: () -> Unit, onDelete: (Reminder) -> Unit, onEdit: (Reminder) -> Unit, onCreateNew: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("A quien le debo 📋", fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface, text = { if (reminders.isEmpty()) { Text("No tienes deudas activas registradas.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp)) } else { LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) { itemsIndexed(reminders) { index, reminder -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(text = "${index + 1}.", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.width(28.dp)); Column(modifier = Modifier.weight(1f)) { Text(reminder.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(formatDate(reminder.targetDateInMillis), fontSize = 12.sp, color = Color.Gray) }; IconButton(onClick = { onEdit(reminder) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color.Blue.copy(alpha = 0.7f)) }; IconButton(onClick = { onDelete(reminder) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.7f)) } }; if (index < reminders.size - 1) { Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp) } } } } }, confirmButton = { Button(onClick = onCreateNew) { Text("Crear") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}

@Composable
fun ScheduledFiadoresDialog(fiadores: List<Fiador>, onDismiss: () -> Unit, onDelete: (Fiador) -> Unit, onEdit: (Fiador) -> Unit, onCreateNew: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Quien me debe 🤝", fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface, text = { if (fiadores.isEmpty()) { Text("No tienes personas que te deban dinero.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp)) } else { LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) { itemsIndexed(fiadores) { index, fiador -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(text = "${index + 1}.", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.width(28.dp)); Column(modifier = Modifier.weight(1f)) { val phoneStr = if(fiador.phone.isNotBlank()) " 📞 ${fiador.phone}" else ""; val remaining = fiador.amount - fiador.paidAmount; Text(fiador.name + phoneStr, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("Resta: ${formatCOP(remaining)} (Total: ${formatCOP(fiador.amount)}) - ${fiador.reason}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(formatDate(fiador.targetDateInMillis), fontSize = 12.sp, color = Color.Gray) }; IconButton(onClick = { onEdit(fiador) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color.Blue.copy(alpha = 0.7f)) }; IconButton(onClick = { onDelete(fiador) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.7f)) } }; if (index < fiadores.size - 1) { Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp) } } } } }, confirmButton = { Button(onClick = onCreateNew) { Text("Agregar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(initialReminder: Reminder? = null, preselectedDate: Long? = null, onDismiss: () -> Unit, onConfirm: (String, Long) -> Unit) {
    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var tempDateMillis by remember { mutableStateOf<Long?>(initialReminder?.targetDateInMillis ?: preselectedDate) }
    var activeScreen by remember { mutableStateOf(if (initialReminder == null && preselectedDate == null) "NEW_INFO" else if (initialReminder == null && preselectedDate != null) "NEW_TIME" else "EDIT_OPTIONS") }
    var isEditDateOnly by remember { mutableStateOf(false) }
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = initialReminder?.targetDateInMillis ?: preselectedDate ?: System.currentTimeMillis() } }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        CustomDatePickerDialog(initialDateMillis = tempDateMillis ?: System.currentTimeMillis(), onDismiss = { showDatePicker = false }, onDateSelected = { selected -> val cal = Calendar.getInstance().apply { timeInMillis = tempDateMillis ?: System.currentTimeMillis() }; val hour = cal.get(Calendar.HOUR_OF_DAY); val minute = cal.get(Calendar.MINUTE); val newCal = Calendar.getInstance().apply { timeInMillis = selected; set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0) }; tempDateMillis = newCal.timeInMillis; showDatePicker = false; if (isEditDateOnly) { onConfirm(title, tempDateMillis!!) } else { activeScreen = "NEW_TIME" } })
    }

    if (activeScreen == "EDIT_OPTIONS") { AlertDialog(onDismissRequest = onDismiss, title = { Text("¿Qué deseas editar? ✏️", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, containerColor = MaterialTheme.colorScheme.surface, text = { Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Button(onClick = { activeScreen = "EDIT_INFO" }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("📝 Información de la Deuda") }; Button(onClick = { isEditDateOnly = true; showDatePicker = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("📅 Fecha de Cobro") }; Button(onClick = { activeScreen = "EDIT_TIME" }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("⏰ Hora de Cobro") } } }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }) }
    if (activeScreen == "NEW_INFO" || activeScreen == "EDIT_INFO") { AlertDialog(onDismissRequest = onDismiss, title = { Text(if (activeScreen == "EDIT_INFO") "Editar Deuda ✏️" else "Nueva Deuda 📅", fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface, text = { OutlinedTextField(value = title, onValueChange = { input -> title = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }, label = { Text("¿Qué debes pagar? (ej. Luz)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences)) }, confirmButton = { Button(onClick = { if (title.isNotBlank()) { val capTitle = title.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }; if (activeScreen == "EDIT_INFO") { onConfirm(capTitle, tempDateMillis!!) } else { title = capTitle; showDatePicker = true } } }) { Text(if (activeScreen == "EDIT_INFO") "Guardar" else "Siguiente") } }, dismissButton = { TextButton(onClick = { if (activeScreen == "EDIT_INFO") activeScreen = "EDIT_OPTIONS" else onDismiss() }) { Text(if (activeScreen == "EDIT_INFO") "Atrás" else "Cancelar") } }) }
    if (activeScreen == "NEW_TIME" || activeScreen == "EDIT_TIME") {
        val currentHourInt = calendar.get(Calendar.HOUR).let { if (it == 0) 12 else it }; val currentMinInt = calendar.get(Calendar.MINUTE); val currentHourStr = currentHourInt.toString(); val currentMinStr = currentMinInt.toString().padStart(2, '0'); var customHour by remember { mutableStateOf(if(initialReminder != null) currentHourInt.toString() else "") }; var customMinute by remember { mutableStateOf(if(initialReminder != null) currentMinStr else "") }; var isPm by remember { mutableStateOf(calendar.get(Calendar.AM_PM) == Calendar.PM) }; val minuteFocusRequester = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, title = { Text("Ingresar Hora ⏰", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { val phoneTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()); Text("Hora actual del teléfono: $phoneTime", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp).alpha(0.7f)); Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) { OutlinedTextField(value = customHour, onValueChange = { input -> if (input.isEmpty()) { customHour = input } else if (input.length <= 2 && input.all { char -> char.isDigit() }) { val h = input.toIntOrNull(); if (h != null) { if (input.length == 1 && h == 0) { customHour = input } else if (h in 1..12) { customHour = input; if (input.length == 2) minuteFocusRequester.requestFocus() } } } }, placeholder = { Text(currentHourStr, color = Color.Gray.copy(alpha=0.4f), fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, modifier = Modifier.width(80.dp), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 28.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true); Text(" : ", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp)); OutlinedTextField(value = customMinute, onValueChange = { input -> if (input.isEmpty()) { customMinute = input } else if (input.length <= 2 && input.all { it.isDigit() }) { val m = input.toIntOrNull(); if (m != null && m in 0..59) { customMinute = input } } }, placeholder = { Text(currentMinStr, color = Color.Gray.copy(alpha=0.4f), fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, modifier = Modifier.width(80.dp).focusRequester(minuteFocusRequester), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 28.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true); Spacer(modifier = Modifier.width(8.dp)); Column { FilterChip(selected = !isPm, onClick = { isPm = false }, label = { Text("AM") }); FilterChip(selected = isPm, onClick = { isPm = true }, label = { Text("PM") }) } } } },
            confirmButton = { TextButton(onClick = { val finalH = customHour.toIntOrNull() ?: currentHourInt; val finalM = customMinute.toIntOrNull() ?: currentMinInt; var hour24 = finalH; if (isPm && hour24 < 12) hour24 += 12; if (!isPm && hour24 == 12) hour24 = 0; if (tempDateMillis != null) { val baseCal = Calendar.getInstance().apply { timeInMillis = tempDateMillis!! }; val localCal = Calendar.getInstance().apply { set(baseCal.get(Calendar.YEAR), baseCal.get(Calendar.MONTH), baseCal.get(Calendar.DAY_OF_MONTH), hour24, finalM, 0) }; onConfirm(title, localCal.timeInMillis) } }) { Text(if (activeScreen == "EDIT_TIME") "Guardar" else "Aceptar") } },
            dismissButton = { TextButton(onClick = { if (activeScreen == "NEW_TIME") activeScreen = "NEW_INFO" else activeScreen = "EDIT_OPTIONS" }) { Text("Atrás") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiadorDialog(
    initialFiador: Fiador? = null,
    products: List<Product>,
    preselectedDate: Long? = null,
    onDismiss: () -> Unit,
    onConfirmNew: (String, String, List<Pair<Product, Int>>, Long) -> Unit,
    onConfirmEdit: (Fiador, Long) -> Unit,
    onConfirmAbono: (Fiador, Double, String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialFiador?.name ?: "") }
    var phone by remember { mutableStateOf(initialFiador?.phone ?: "") }
    var tempDateMillis by remember { mutableStateOf<Long?>(initialFiador?.targetDateInMillis ?: preselectedDate) }
    var activeScreen by remember { mutableStateOf(if (initialFiador == null && preselectedDate == null) "NEW_INFO" else if (initialFiador == null && preselectedDate != null) "NEW_TIME" else "EDIT_OPTIONS") }
    var isEditDateOnly by remember { mutableStateOf(false) }
    val cartItems = remember { mutableStateListOf<Pair<Product, Int>>() }
    var expandedProduct by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var qtyRaw by remember { mutableStateOf("1") }
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = initialFiador?.targetDateInMillis ?: preselectedDate ?: System.currentTimeMillis() } }

    var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        CustomDatePickerDialog(
            initialDateMillis = tempDateMillis ?: System.currentTimeMillis(),
            onDismiss = { showDatePicker = false },
            onDateSelected = { selected ->
                val cal = Calendar.getInstance().apply { timeInMillis = tempDateMillis ?: System.currentTimeMillis() }
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val minute = cal.get(Calendar.MINUTE)
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = selected
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                tempDateMillis = newCal.timeInMillis
                showDatePicker = false
                if (isEditDateOnly) {
                    onConfirmEdit(initialFiador!!.copy(name = name, phone = phone), tempDateMillis!!)
                } else {
                    activeScreen = "NEW_TIME"
                }
            }
        )
    }

    if (activeScreen == "EDIT_OPTIONS") {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("¿Qué deseas hacer? ✏️", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { activeScreen = "ABONO" }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("💰 Registrar Abono") }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { activeScreen = "EDIT_INFO" }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("📝 Información del Fiador") }
                    Button(onClick = { isEditDateOnly = true; showDatePicker = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("📅 Fecha de Cobro") }
                    Button(onClick = { activeScreen = "EDIT_TIME" }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("⏰ Hora de Cobro") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
        )
    }

    if (activeScreen == "ABONO" && initialFiador != null) {
        var abonoRaw by remember { mutableStateOf("") }
        var abonoMethod by remember { mutableStateOf("Efectivo") }
        val remaining = initialFiador.amount - initialFiador.paidAmount

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Abonar a Deuda 💰", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Deuda Total: ${formatCOP(initialFiador.amount)}", fontWeight = FontWeight.Bold)
                    Text("Abonado hasta ahora: ${formatCOP(initialFiador.paidAmount)}", color = Color(0xFF4CAF50))
                    Text("Resta por pagar: ${formatCOP(remaining)}", color = Color.Red, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (initialFiador.paymentHistory.isNotEmpty()) {
                        Text("Historial de Abonos:", fontSize = 12.sp, color = Color.Gray)
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))) {
                            Text(initialFiador.paymentHistory, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    OutlinedTextField(
                        value = abonoRaw,
                        onValueChange = { abonoRaw = cleanAmountInput(it) },
                        label = { Text("Monto a abonar") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = AmountVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Text("$", color = Color.Gray, modifier = Modifier.padding(start=8.dp)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        FilterChip(selected = abonoMethod == "Efectivo", onClick = { abonoMethod = "Efectivo" }, label = { Text("Efectivo") })
                        FilterChip(selected = abonoMethod == "Digital", onClick = { abonoMethod = "Digital" }, label = { Text("Digital") })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val abono = abonoRaw.toDoubleOrNull() ?: 0.0
                    if (abono > 0 && abono <= remaining) {
                        onConfirmAbono(initialFiador, abono, abonoMethod)
                    } else if (abono > remaining) {
                        Toast.makeText(context, "El abono supera la deuda restante", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Confirmar Abono") }
            },
            dismissButton = { TextButton(onClick = { activeScreen = "EDIT_OPTIONS" }) { Text("Atrás") } }
        )
    }

    if (activeScreen == "NEW_INFO" || activeScreen == "EDIT_INFO") {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (activeScreen == "EDIT_INFO") "Editar Fiador ✏️" else "Nuevo Fiador 🤝", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = name, onValueChange = { input -> name = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }, label = { Text("Nombre de la persona") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono (Opcional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    if (initialFiador == null) {
                        Text("Productos a fiar:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box {
                            Button(onClick = { expandedProduct = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors()) { Text(selectedProduct?.name ?: "🔽 Seleccionar Producto", color = MaterialTheme.colorScheme.onSurface) }
                            DropdownMenu(expanded = expandedProduct, onDismissRequest = { expandedProduct = false }) {
                                products.filter { it.stock > 0 }.forEach { p ->
                                    DropdownMenuItem(text = { Text("${p.name} (Disp: ${p.stock} ${p.unit}) - ${formatCOP(p.price)}") }, onClick = { selectedProduct = p; expandedProduct = false })
                                }
                                if(products.none { it.stock > 0 }) {
                                    DropdownMenuItem(text = { Text("No hay productos en stock") }, onClick = { expandedProduct = false })
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedTextField(value = qtyRaw, onValueChange = { n -> val d = n.filter { it.isDigit() }; qtyRaw = d }, label = { Text("Cant.") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                val q = qtyRaw.toIntOrNull() ?: 0
                                if (selectedProduct != null && q > 0 && q <= selectedProduct!!.stock) {
                                    val existing = cartItems.find { it.first.id == selectedProduct!!.id }
                                    if (existing != null) {
                                        val newQ = existing.second + q; if (newQ <= selectedProduct!!.stock) { val idx = cartItems.indexOf(existing); cartItems[idx] = existing.copy(second = newQ) } else { Toast.makeText(context, "Supera el stock disponible", Toast.LENGTH_SHORT).show() }
                                    } else {
                                        cartItems.add(Pair(selectedProduct!!, q))
                                    }
                                    qtyRaw = "1"; selectedProduct = null
                                }
                            }) { Text("Añadir") }
                        }
                        if (cartItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Lista de deuda:", fontSize = 12.sp, color = Color.Gray)
                            cartItems.forEachIndexed { index, item ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("• ${item.second}${item.first.unit} ${item.first.name}", fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    Text(formatCOP(item.first.price * item.second), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { cartItems.removeAt(index) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, null, tint = Color.Red, modifier = Modifier.size(16.dp)) }
                                }
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            val total = cartItems.sumOf { it.first.price * it.second }
                            Text("Total Deuda: ${formatCOP(total)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Text("Detalle original:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(initialFiador.reason, fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Monto Deuda Total: ${formatCOP(initialFiador.amount)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Abonado: ${formatCOP(initialFiador.paidAmount)}", fontSize = 14.sp, color = Color(0xFF4CAF50))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        val capName = name.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        if (initialFiador == null && cartItems.isEmpty()) {
                            Toast.makeText(context, "Agrega productos a la deuda", Toast.LENGTH_SHORT).show()
                        } else {
                            if (activeScreen == "EDIT_INFO") {
                                onConfirmEdit(initialFiador!!.copy(name = capName, phone = phone), tempDateMillis!!)
                            } else {
                                name = capName; showDatePicker = true
                            }
                        }
                    } else {
                        Toast.makeText(context, "Escribe el nombre del fiador", Toast.LENGTH_SHORT).show()
                    }
                }) { Text(if (activeScreen == "EDIT_INFO") "Guardar" else "Siguiente") }
            },
            dismissButton = { TextButton(onClick = { if (activeScreen == "EDIT_INFO") activeScreen = "EDIT_OPTIONS" else onDismiss() }) { Text(if (activeScreen == "EDIT_INFO") "Atrás" else "Cancelar") } }
        )
    }

    if (activeScreen == "NEW_TIME" || activeScreen == "EDIT_TIME") {
        val currentHourInt = calendar.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val currentMinInt = calendar.get(Calendar.MINUTE)
        val currentHourStr = currentHourInt.toString()
        val currentMinStr = currentMinInt.toString().padStart(2, '0')
        var customHour by remember { mutableStateOf(if(initialFiador != null) currentHourInt.toString() else "") }
        var customMinute by remember { mutableStateOf(if(initialFiador != null) currentMinStr else "") }
        var isPm by remember { mutableStateOf(calendar.get(Calendar.AM_PM) == Calendar.PM) }
        val minuteFocusRequester = remember { FocusRequester() }

        AlertDialog(
            onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, title = { Text("Hora de la Alerta ⏰", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    val phoneTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    Text("Hora actual del teléfono: $phoneTime", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp).alpha(0.7f))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = customHour, onValueChange = { input -> if (input.isEmpty()) { customHour = input } else if (input.length <= 2 && input.all { char -> char.isDigit() }) { val h = input.toIntOrNull(); if (h != null) { if (input.length == 1 && h == 0) { customHour = input } else if (h in 1..12) { customHour = input; if (input.length == 2) minuteFocusRequester.requestFocus() } } } }, placeholder = { Text(currentHourStr, color = Color.Gray.copy(alpha=0.4f), fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, modifier = Modifier.width(80.dp), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 28.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        Text(" : ", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                        OutlinedTextField(value = customMinute, onValueChange = { input -> if (input.isEmpty()) { customMinute = input } else if (input.length <= 2 && input.all { it.isDigit() }) { val m = input.toIntOrNull(); if (m != null && m in 0..59) { customMinute = input } } }, placeholder = { Text(currentMinStr, color = Color.Gray.copy(alpha=0.4f), fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, modifier = Modifier.width(80.dp).focusRequester(minuteFocusRequester), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 28.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column { FilterChip(selected = !isPm, onClick = { isPm = false }, label = { Text("AM") }); FilterChip(selected = isPm, onClick = { isPm = true }, label = { Text("PM") }) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val finalH = customHour.toIntOrNull() ?: currentHourInt
                    val finalM = customMinute.toIntOrNull() ?: currentMinInt
                    var hour24 = finalH
                    if (isPm && hour24 < 12) hour24 += 12
                    if (!isPm && hour24 == 12) hour24 = 0

                    if (tempDateMillis != null) {
                        val baseCal = Calendar.getInstance().apply { timeInMillis = tempDateMillis!! }
                        val localCal = Calendar.getInstance().apply { set(baseCal.get(Calendar.YEAR), baseCal.get(Calendar.MONTH), baseCal.get(Calendar.DAY_OF_MONTH), hour24, finalM, 0) }

                        if (initialFiador != null) {
                            onConfirmEdit(initialFiador.copy(name = name, phone = phone), localCal.timeInMillis)
                        } else {
                            onConfirmNew(name, phone, cartItems.toList(), localCal.timeInMillis)
                        }
                    }
                }) { Text(if (activeScreen == "EDIT_TIME") "Guardar" else "Aceptar") }
            },
            dismissButton = { TextButton(onClick = { if (activeScreen == "NEW_TIME") activeScreen = "NEW_INFO" else activeScreen = "EDIT_OPTIONS" }) { Text("Atrás") } }
        )
    }
}

// ==========================================
// 9. FUNCIONES DE FORMATO Y UTILIDADES
// ==========================================
fun cleanAmountInput(input: String): String { return input.filter { it.isDigit() } }
class AmountVisualTransformation : VisualTransformation { override fun filter(text: AnnotatedString): TransformedText { val inputText = text.text; val formattedInt = if (inputText.isNotEmpty()) { var result = ""; val reversed = inputText.reversed(); for (i in reversed.indices) { result += reversed[i]; if ((i + 1) % 3 == 0 && i != reversed.lastIndex) { result += "." } }; result.reversed() } else ""; val offsetMapping = object : OffsetMapping { override fun originalToTransformed(offset: Int): Int { var transformedCursor = 0; var originalCursor = 0; while(originalCursor < offset) { transformedCursor++; originalCursor++; val remaining = inputText.length - originalCursor; if (remaining > 0 && remaining % 3 == 0) transformedCursor++ }; return transformedCursor }; override fun transformedToOriginal(offset: Int): Int { var originalOffset = 0; var transformedIndex = 0; while (transformedIndex < offset && originalOffset < inputText.length) { if (formattedInt[transformedIndex] == '.') { transformedIndex++ } else { originalOffset++; transformedIndex++ } }; return originalOffset } }; return TransformedText(AnnotatedString(formattedInt), offsetMapping) } }
fun formatCOP(amount: Double): String { val format = DecimalFormat("#,###").apply { decimalFormatSymbols = decimalFormatSymbols.apply { groupingSeparator = '.' } }; return "$${format.format(amount)}" }
fun formatDate(timestamp: Long): String { return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp)) }
fun formatDateOnly(timestamp: Long): String { return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp)) }
fun isSameDay(time1: Long, time2: Long): Boolean { val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }; val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }; return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) }
fun getSmartEmoji(description: String, isIncome: Boolean): String { val descLower = description.lowercase(Locale.getDefault()); return when { listOf("comida", "almuerzo", "cena", "hamburguesa", "kfc", "pizza", "salchipapa").any { descLower.contains(it) } -> "🍔"; listOf("mercado", "supermercado", "compras", "tienda").any { descLower.contains(it) } -> "🛒"; listOf("transporte", "pasaje", "bus", "taxi", "uber", "gasolina").any { descLower.contains(it) } -> "🚕"; listOf("servicios", "luz", "agua", "internet", "factura").any { descLower.contains(it) } -> "💡"; listOf("casa", "arriendo", "hogar", "alquiler").any { descLower.contains(it) } -> "🏠"; listOf("regalo", "cumpleaños", "fiesta", "ropa").any { descLower.contains(it) } -> "🎁"; listOf("medico", "salud", "farmacia", "pastillas").any { descLower.contains(it) } -> "💊"; listOf("salario", "sueldo", "pago", "nomina").any { descLower.contains(it) } -> "💵"; listOf("negocio", "venta", "cliente", "producto", "varios productos").any { descLower.contains(it) } -> "🤝"; listOf("ahorro", "banco", "intereses", "nequi", "bancolombia", "daviplata").any { descLower.contains(it) } -> "🏦"; else -> if (isIncome) "💰" else "💸" } }

// ==========================================
// 10. MOTOR DE NOTIFICACIONES (RECEIVER)
// ==========================================
@SuppressLint("ObsoleteSdkInt") // Suprime alertas falsas del SDK si el Gradle pierde el estado.
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // En Kotlin, los BroadcastReceivers DEBEN soportar Intent y Context nulos para evitar NullPointerExceptions del sistema
        if (context == null || intent == null) return

        if (intent.action == "com.xxcamixx.contabilidad.CHAT_SYNC") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val authPrefs = context.getSharedPreferences("GlobalAuthPrefs", Context.MODE_PRIVATE)
                    val userId = authPrefs.getString("lastKnownUserId", null)
                    val userRole = authPrefs.getString("lastKnownRole", "INVITADO")

                    if (userId != null) {
                        val isSuperAdmin = userId.lowercase(Locale.getDefault()) == "zonacami77777@gmail.com"
                        val effectiveRole = if (isSuperAdmin) "ADMIN" else userRole
                        var lastNotified = authPrefs.getLong("lastNotified_$userId", System.currentTimeMillis())
                        var newestTime = lastNotified
                        var hasNewMsg = false; var notificationMsg: String? = null; var notificationSender: String? = null

                        if (effectiveRole == "ADMIN") {
                            val chats = RetrofitInstance.api.getAllChats()
                            chats.forEach { (email, msgs) ->
                                msgs.forEach { msg ->
                                    if (msg.sender != "zonacami77777@gmail.com" && msg.timestamp > lastNotified) {
                                        notificationMsg = msg.text; notificationSender = email.substringBefore("@")
                                        if (msg.timestamp > newestTime) newestTime = msg.timestamp
                                        hasNewMsg = true
                                    }
                                }
                            }
                        } else {
                            val msgs = RetrofitInstance.api.getChat(userId)
                            msgs.forEach { msg ->
                                if (msg.sender == "zonacami77777@gmail.com" && msg.timestamp > lastNotified) {
                                    notificationMsg = msg.text; notificationSender = "Soporte (Admin)"
                                    if (msg.timestamp > newestTime) newestTime = msg.timestamp
                                    hasNewMsg = true
                                }
                            }
                        }

                        if (hasNewMsg && notificationMsg != null) {
                            showChatNotification(context, "Nuevo mensaje de $notificationSender", notificationMsg!!)
                            authPrefs.edit().putLong("lastNotified_$userId", newestTime).apply()
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    scheduleNextChatSync(context)
                    pendingResult.finish()
                }
            }
            return
        }

        if (intent.action?.startsWith("com.xxcamixx.contabilidad.") != true) return

        val notifTitle = intent.getStringExtra("NOTIFICATION_TITLE") ?: "¡Alerta! ⏰"
        val notifText = intent.getStringExtra("NOTIFICATION_TEXT") ?: "Tienes una alerta pendiente"
        val id = intent.getIntExtra("ID", 0)

        // Uso seguro de variables del sistema (evitando punteros nulos)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MiBilletera::AlarmaWakeLock")
        wakeLock?.acquire(5000)

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null) {
                val channelId = "finance_alarms_v4"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(channelId, "Recordatorios", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Notificaciones para recordar pagos, cobros de fiadores y vencimientos"
                        enableVibration(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                }
                val tapIntent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
                val tapPendingIntent = PendingIntent.getActivity(context, id, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(notifTitle)
                    .setContentText(notifText)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(tapPendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(id, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Protección contra la excepción de "WakeLock under-locked"
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}