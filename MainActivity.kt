package com.xxcamixx.contabilidad

// --- Importaciones Base ---
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import android.speech.tts.TextToSpeech
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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
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

// --- NUEVOS MODELOS Y FUNCIONES PARA MONEDA ---
data class BcvResponse(val code: String, val tasa: Double)

fun formatBs(amount: Double): String {
    // MODIFICADO: "#,##0.00" asegura que siempre se muestren dos decimales
    val format = DecimalFormat("#,##0.00").apply { decimalFormatSymbols = decimalFormatSymbols.apply { groupingSeparator = '.'; decimalSeparator = ',' } }
    return "Bs ${format.format(amount)}"
}

fun formatUSD(amount: Double): String {
    // MODIFICADO: "#,##0.00" asegura que siempre se muestren dos decimales
    val format = DecimalFormat("#,##0.00").apply { decimalFormatSymbols = decimalFormatSymbols.apply { groupingSeparator = ','; decimalSeparator = '.' } }
    return "$${format.format(amount)}"
}

fun formatMoneyMain(amount: Double, country: String): String {
    return if (country == "Venezuela") formatUSD(amount) else formatCOP(amount)
}

fun formatMoneySec(amount: Double, country: String, bcvRate: Double): String {
    return if (country == "Venezuela" && bcvRate > 0) "= ${formatBs(amount * bcvRate)}" else ""
}

fun cleanDecimalInput(input: String): String {
    var dotCount = 0
    return input.replace(',', '.').filter {
        if (it == '.') {
            dotCount++
            dotCount <= 1
        } else {
            it.isDigit()
        }
    }
}
// ----------------------------------------------

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

    // NUEVO ENDPOINT BCV
    @GET("api/bcv") suspend fun getBcvRate(): BcvResponse
}

object RetrofitInstance {
    val api: ServerApi by lazy { Retrofit.Builder().baseUrl("http://158.247.123.136:3000/").addConverterFactory(GsonConverterFactory.create()).build().create(ServerApi::class.java) }
}

class CloudSyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val userId = inputData.getString("USER_ID") ?: return Result.failure()
        val db = AppDatabase.getDatabase(applicationContext, userId).financeDao()
        return try {
            val transactions = db.getBackupTransactions()
            val reminders = db.getBackupReminders()
            val fiadores = db.getBackupFiadores()
            val products = db.getBackupProducts()

            val newData = BackupData(transactions, reminders, fiadores, products)
            val timeString = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val newRecord = BackupRecord(UUID.randomUUID().toString(), "Automático - $timeString", System.currentTimeMillis(), newData)

            val remotePayload = RetrofitInstance.api.getBackup(userId)
            val existingBackups = mutableListOf<BackupRecord>()

            if (remotePayload != null) {
                if (remotePayload.backups != null) { existingBackups.addAll(remotePayload.backups) }
                else if (remotePayload.transactions != null) { existingBackups.add(BackupRecord("old", "Respaldo Antiguo", 0L, BackupData(remotePayload.transactions, remotePayload.reminders ?: emptyList(), remotePayload.fiadores ?: emptyList(), remotePayload.products ?: emptyList()))) }
            }

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
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var defaultToneGen: ToneGenerator? = null

    fun init() {
        if (defaultToneGen == null) {
            try {
                defaultToneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            } catch (e: Exception) {}
        }
    }

    fun play(context: Context, soundUri: String?) {
        try {
            init()
            if (soundUri.isNullOrEmpty()) {
                defaultToneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                return
            }
            mediaPlayer?.release()
            mediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(context, Uri.parse(soundUri))
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setOnPreparedListener { it.start() }
                setOnCompletionListener { it.release() }
                prepareAsync()
            }
        } catch (e: Exception) {
            defaultToneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        }
    }
}

object AppVoice {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    // Función limpiadora para que la voz no diga "Punto" o "Guión"
    private fun cleanSpeechText(input: String): String {
        return input
            .replace("...", ",")
            .replace(Regex("\\.(?=\\s|$)"), ",")
            .replace("/", " ")
            .replace("-", " ")
            .replace("_", " ")
    }

    fun speak(context: Context, text: String) {
        val sanitizedText = cleanSpeechText(text)

        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("es", "ES")
                    isInitialized = true
                    tts?.speak(pendingText ?: sanitizedText, TextToSpeech.QUEUE_FLUSH, null, "NotifID")
                    pendingText = null
                }
            }
            pendingText = sanitizedText
        } else if (isInitialized) {
            tts?.speak(sanitizedText, TextToSpeech.QUEUE_FLUSH, null, "NotifID")
        }
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

    val authPrefs = context.getSharedPreferences("GlobalAuthPrefs", Context.MODE_PRIVATE)
    val userId = authPrefs.getString("lastKnownUserId", null)
    var useVoice = false
    if (userId != null) {
        val userPrefs = context.getSharedPreferences("FinancePrefs_$userId", Context.MODE_PRIVATE)
        useVoice = userPrefs.getBoolean("voiceEnabled", false)
    }

    if (useVoice) {
        AppVoice.speak(context, "$title... $text")
    } else {
        AppSounds.play(context, "")
    }
}

@SuppressLint("ScheduleExactAlarm")
fun scheduleNextChatSync(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        action = "com.xxcamixx.contabilidad.CHAT_SYNC"
        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
    }
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
    val digitalAmount: Double = 0.0,
    val country: String = "Colombia" // <-- AGREGADO
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double = 0.0,
    val targetDateInMillis: Long,
    val isStore: Boolean = false,
    val country: String = "Colombia" // <-- AGREGADO
)

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
    val country: String = "Colombia" // <-- AGREGADO
)

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
    val country: String = "Colombia" // <-- AGREGADO
)

@Dao
interface FinanceDao {
    @Query("SELECT * FROM transactions WHERE country = :country ORDER BY timestamp DESC")
    fun getAllTransactions(country: String): Flow<List<Transaction>>

    @Insert suspend fun insertTransaction(transaction: Transaction)
    @Delete suspend fun deleteTransaction(transaction: Transaction)
    @Query("DELETE FROM transactions") suspend fun deleteAllTransactions()

    @Query("DELETE FROM transactions WHERE description NOT LIKE 'Venta: %' AND country = :country")
    suspend fun deletePersonalTransactions(country: String)

    @Query("UPDATE transactions SET profit = 0.0, cashAmount = 0.0, digitalAmount = 0.0 WHERE country = :country")
    suspend fun resetAllProfits(country: String)

    @Query("SELECT * FROM reminders WHERE country = :country ORDER BY targetDateInMillis ASC")
    fun getAllReminders(country: String): Flow<List<Reminder>>

    @Insert suspend fun insertReminder(reminder: Reminder): Long
    @Update suspend fun updateReminder(reminder: Reminder)
    @Delete suspend fun deleteReminder(reminder: Reminder)
    @Query("DELETE FROM reminders") suspend fun deleteAllReminders()

    @Query("SELECT * FROM fiadores WHERE country = :country ORDER BY targetDateInMillis ASC")
    fun getAllFiadores(country: String): Flow<List<Fiador>>

    @Insert suspend fun insertFiador(fiador: Fiador): Long
    @Update suspend fun updateFiador(fiador: Fiador)
    @Delete suspend fun deleteFiador(fiador: Fiador)
    @Query("DELETE FROM fiadores") suspend fun deleteAllFiadores()

    @Query("SELECT * FROM products WHERE country = :country ORDER BY name ASC")
    fun getAllProducts(country: String): Flow<List<Product>>

    @Insert suspend fun insertProduct(product: Product): Long
    @Update suspend fun updateProduct(product: Product)
    @Delete suspend fun deleteProduct(product: Product)
    @Query("DELETE FROM products") suspend fun deleteAllProducts()

    // --- NUEVO: Métodos exclusivos para Backups en la nube (Obtienen datos de TODOS los países) ---
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC") suspend fun getBackupTransactions(): List<Transaction>
    @Query("SELECT * FROM reminders ORDER BY targetDateInMillis ASC") suspend fun getBackupReminders(): List<Reminder>
    @Query("SELECT * FROM fiadores ORDER BY targetDateInMillis ASC") suspend fun getBackupFiadores(): List<Fiador>
    @Query("SELECT * FROM products ORDER BY name ASC") suspend fun getBackupProducts(): List<Product>
}

val MIGRATION_4_5 = object : Migration(4, 5) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `price` REAL NOT NULL, `stock` INTEGER NOT NULL, `expirationDateInMillis` INTEGER)") } }
val MIGRATION_5_6 = object : Migration(5, 6) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `products` ADD COLUMN `entryDateInMillis` INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}") } }
val MIGRATION_6_7 = object : Migration(6, 7) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `fiadores` ADD COLUMN `phone` TEXT NOT NULL DEFAULT ''") } }
val MIGRATION_7_8 = object : Migration(7, 8) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `products` ADD COLUMN `unit` TEXT NOT NULL DEFAULT 'Uds'") } }
val MIGRATION_8_9 = object : Migration(8, 9) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `products` ADD COLUMN `purchasePrice` REAL NOT NULL DEFAULT 0.0"); db.execSQL("ALTER TABLE `transactions` ADD COLUMN `profit` REAL NOT NULL DEFAULT 0.0") } }
val MIGRATION_9_10 = object : Migration(9, 10) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `products` ADD COLUMN `minStock` INTEGER NOT NULL DEFAULT 0") } }
val MIGRATION_10_11 = object : Migration(10, 11) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `transactions` ADD COLUMN `cashAmount` REAL NOT NULL DEFAULT 0.0"); db.execSQL("ALTER TABLE `transactions` ADD COLUMN `digitalAmount` REAL NOT NULL DEFAULT 0.0") } }
val MIGRATION_11_12 = object : Migration(11, 12) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `fiadores` ADD COLUMN `paidAmount` REAL NOT NULL DEFAULT 0.0"); db.execSQL("ALTER TABLE `fiadores` ADD COLUMN `paymentHistory` TEXT NOT NULL DEFAULT ''") } }
val MIGRATION_12_13 = object : Migration(12, 13) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `products` ADD COLUMN `imageUri` TEXT DEFAULT NULL") } }
val MIGRATION_13_14 = object : Migration(13, 14) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `reminders` ADD COLUMN `isStore` INTEGER NOT NULL DEFAULT 0"); db.execSQL("ALTER TABLE `fiadores` ADD COLUMN `isStore` INTEGER NOT NULL DEFAULT 1") } }
val MIGRATION_14_15 = object : Migration(14, 15) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `reminders` ADD COLUMN `amount` REAL NOT NULL DEFAULT 0.0") } }
val MIGRATION_15_16 = object : Migration(15, 16) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `fiadores` ADD COLUMN `totalCost` REAL NOT NULL DEFAULT 0.0") } }
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `country` TEXT NOT NULL DEFAULT 'Colombia'")
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `country` TEXT NOT NULL DEFAULT 'Colombia'")
        db.execSQL("ALTER TABLE `fiadores` ADD COLUMN `country` TEXT NOT NULL DEFAULT 'Colombia'")
        db.execSQL("ALTER TABLE `products` ADD COLUMN `country` TEXT NOT NULL DEFAULT 'Colombia'")
    }
}

@Database(entities = [Transaction::class, Reminder::class, Fiador::class, Product::class], version = 17, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
    companion object {
        @Volatile private var INSTANCES = mutableMapOf<String, AppDatabase>()
        fun getDatabase(context: Context, userId: String): AppDatabase {
            return INSTANCES[userId] ?: synchronized(this) {
                val dbName = "finance_database_$userId"
                val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, dbName)
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
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

    private val _selectedCountryFlow = MutableStateFlow(userPrefs.getString("selectedCountry", "Colombia") ?: "Colombia")

    var selectedCountry by mutableStateOf(_selectedCountryFlow.value)
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: Flow<List<Transaction>> = _selectedCountryFlow.flatMapLatest { dao.getAllTransactions(it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val reminders: Flow<List<Reminder>> = _selectedCountryFlow.flatMapLatest { dao.getAllReminders(it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val fiadores: Flow<List<Fiador>> = _selectedCountryFlow.flatMapLatest { dao.getAllFiadores(it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val products: Flow<List<Product>> = _selectedCountryFlow.flatMapLatest { dao.getAllProducts(it) }

    var bcvRate by mutableStateOf(userPrefs.getFloat("bcvRate", 0f).toDouble())
        private set

    var minBalanceThreshold by mutableStateOf(userPrefs.getFloat("minBalance", 0f).toDouble()); private set
    var personalSoundUri by mutableStateOf(userPrefs.getString("personalSoundUri", "")); private set
    var storeSoundUri by mutableStateOf(userPrefs.getString("storeSoundUri", "")); private set
    var touchSoundUri by mutableStateOf(userPrefs.getString("touchSoundUri", "")); private set
    var isVoiceAssistantEnabled by mutableStateOf(userPrefs.getBoolean("voiceEnabled", false)); private set

    var isSyncing by mutableStateOf(false); private set
    var syncMessage by mutableStateOf(""); private set
    var lastSyncDate by mutableStateOf(userPrefs.getLong("lastSync", 0L)); private set
    var autoSyncFrequency by mutableStateOf(userPrefs.getInt("syncFrequency", 0)); private set
    var autoSyncHour by mutableStateOf(userPrefs.getInt("syncHour", 2)); private set
    var autoSyncMinute by mutableStateOf(userPrefs.getInt("syncMinute", 0)); private set

    var pocketDebt by mutableStateOf(userPrefs.getFloat("pocketDebt_${_selectedCountryFlow.value}", if (_selectedCountryFlow.value == "Colombia") userPrefs.getFloat("pocketDebt", 0f) else 0f).toDouble()); private set

    init {
        scheduleAutoSync(application, autoSyncFrequency, autoSyncHour, autoSyncMinute)
        if (selectedCountry == "Venezuela") fetchBcvRate()
    }

    fun updateCountry(country: String) {
        selectedCountry = country
        _selectedCountryFlow.value = country
        userPrefs.edit().putString("selectedCountry", country).apply()
        pocketDebt = userPrefs.getFloat("pocketDebt_$country", if (country == "Colombia") userPrefs.getFloat("pocketDebt", 0f) else 0f).toDouble()
        if (country == "Venezuela") fetchBcvRate()
    }

    private fun fetchBcvRate() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getBcvRate()
                if (response.tasa > 0) {
                    bcvRate = response.tasa
                    userPrefs.edit().putFloat("bcvRate", response.tasa.toFloat()).apply()
                }
            } catch (e: Exception) {
            }
        }
    }

    fun updateMinBalance(amount: Double) { minBalanceThreshold = amount; userPrefs.edit().putFloat("minBalance", amount.toFloat()).apply() }
    fun updatePersonalSoundPreference(uri: String, context: Context) { personalSoundUri = uri; userPrefs.edit().putString("personalSoundUri", uri).apply(); AppSounds.play(context, uri) }
    fun updateStoreSoundPreference(uri: String, context: Context) { storeSoundUri = uri; userPrefs.edit().putString("storeSoundUri", uri).apply(); AppSounds.play(context, uri) }
    fun updateTouchSoundPreference(uri: String, context: Context) { touchSoundUri = uri; userPrefs.edit().putString("touchSoundUri", uri).apply(); AppSounds.play(context, uri) }
    fun updateVoicePreference(enabled: Boolean) { isVoiceAssistantEnabled = enabled; userPrefs.edit().putBoolean("voiceEnabled", enabled).apply() }

    fun addPocketDebt(amount: Double) {
        val newDebt = pocketDebt + amount
        pocketDebt = newDebt
        userPrefs.edit().putFloat("pocketDebt_$selectedCountry", newDebt.toFloat()).apply()
    }

    fun reimbursePocketDebt(amount: Double) {
        viewModelScope.launch {
            val newDebt = pocketDebt - amount
            if (newDebt >= 0) {
                pocketDebt = newDebt
                userPrefs.edit().putFloat("pocketDebt_$selectedCountry", newDebt.toFloat()).apply()
                dao.insertTransaction(
                    Transaction(description = "Venta: Reembolso a Bolsillo", amount = 0.0, isIncome = true, note = "Vuelto devuelto de la caja al bolsillo personal", profit = 0.0, cashAmount = -amount, digitalAmount = 0.0, country = selectedCountry)
                )
            }
        }
        AppSounds.play(getApplication<Application>(), touchSoundUri)
    }

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

    // NUEVO MÉTODO PARA SINCRONIZACIÓN BAJO PERFIL CADA 30 SEGUNDOS
    fun silentBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentData = BackupData(dao.getBackupTransactions(), dao.getBackupReminders(), dao.getBackupFiadores(), dao.getBackupProducts())
                val timeString = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                val newRecord = BackupRecord(UUID.randomUUID().toString(), "AutoSync 30s", System.currentTimeMillis(), currentData)
                val remotePayload = RetrofitInstance.api.getBackup(userId)
                val existingBackups = mutableListOf<BackupRecord>()
                if (remotePayload != null) {
                    if (remotePayload.backups != null) { existingBackups.addAll(remotePayload.backups) }
                }
                existingBackups.add(0, newRecord)
                // Mantiene solo los últimos 10 respaldos en la nube para no saturar
                if (existingBackups.size > 10) { existingBackups.removeAt(existingBackups.size - 1) }
                RetrofitInstance.api.uploadBackup(userId, CloudPayload(backups = existingBackups))
            } catch (e: Exception) {
                // Falla silenciosamente si no hay internet
            }
        }
    }

    fun manualBackup(backupName: String, onResult: (String) -> Unit) {
        isSyncing = true
        syncMessage = "Guardando tus cuentas actuales..."
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentData = BackupData(dao.getBackupTransactions(), dao.getBackupReminders(), dao.getBackupFiadores(), dao.getBackupProducts())
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
                launch(Dispatchers.Main) { lastSyncDate = now; onResult("¡Respaldo '$backupName' guardado! ☁️✅"); isSyncing = false; syncMessage = "" }
            } catch (e: Exception) { launch(Dispatchers.Main) { onResult("Error al subir el respaldo: ${e.message}"); isSyncing = false; syncMessage = "" } }
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
        isSyncing = true
        syncMessage = "Restaurando tu cuenta guardada..."
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.deleteAllTransactions(); dao.deleteAllReminders(); dao.deleteAllFiadores(); dao.deleteAllProducts()
                record.data.transactions.forEach { val safeCountry = (it.country as String?) ?: "Colombia"; dao.insertTransaction(it.copy(id = 0, country = safeCountry)) }
                record.data.reminders.forEach { val safeCountry = (it.country as String?) ?: "Colombia"; dao.insertReminder(it.copy(id = 0, country = safeCountry)) }
                record.data.fiadores.forEach { val safeCountry = (it.country as String?) ?: "Colombia"; dao.insertFiador(it.copy(id = 0, country = safeCountry)) }
                record.data.products.forEach { val safeCountry = (it.country as String?) ?: "Colombia"; dao.insertProduct(it.copy(id = 0, country = safeCountry)) }
                launch(Dispatchers.Main) { onResult("¡Respaldo '${record.name}' restaurado! ☁️📥"); isSyncing = false; syncMessage = "" }
            } catch (_: Exception) { launch(Dispatchers.Main) { onResult("Error al restaurar los datos."); isSyncing = false; syncMessage = "" } }
        }
    }

    fun addTransaction(description: String, amount: Double, isIncome: Boolean, note: String, method: String) {
        viewModelScope.launch {
            val cash = if (method == "Efectivo") amount else 0.0
            val digital = if (method == "Digital") amount else 0.0
            dao.insertTransaction(Transaction(description = description, amount = amount, isIncome = isIncome, note = note, cashAmount = cash, digitalAmount = digital, country = selectedCountry))
        }
        AppSounds.play(getApplication<Application>(), touchSoundUri)
    }

    fun insertRawTransaction(transaction: Transaction) { viewModelScope.launch { val safeCountry = (transaction.country as String?) ?: selectedCountry; dao.insertTransaction(transaction.copy(id = 0, country = safeCountry)) } }
    fun deleteTransaction(transaction: Transaction) { viewModelScope.launch { dao.deleteTransaction(transaction) }; AppSounds.play(getApplication<Application>(), touchSoundUri) }
    fun deleteTransactionsList(list: List<Transaction>) { viewModelScope.launch { list.forEach { dao.deleteTransaction(it) } }; AppSounds.play(getApplication<Application>(), touchSoundUri) }
    fun deletePersonalTransactions() { viewModelScope.launch { dao.deletePersonalTransactions(selectedCountry) } }
    fun resetAllProfits() { viewModelScope.launch { dao.resetAllProfits(selectedCountry) }; AppSounds.play(getApplication<Application>(), touchSoundUri) }

    fun addProduct(name: String, purchasePrice: Double, price: Double, stock: Int, unit: String, expirationDateInMillis: Long?, minStock: Int, imageUri: String?, context: Context, onConfigured: (String) -> Unit) { viewModelScope.launch { val productId = dao.insertProduct(Product(name = name, purchasePrice = purchasePrice, price = price, stock = stock, unit = unit, expirationDateInMillis = expirationDateInMillis, minStock = minStock, imageUri = imageUri, country = selectedCountry)).toInt(); if (expirationDateInMillis != null) scheduleNotification(context, expirationDateInMillis, "¡Producto por Vencer! ⚠️", "El producto $name ha alcanzado su fecha de caducidad.", productId + 200000, "EXPIRE_TRIGGER"); onConfigured("Producto guardado en inventario") }; AppSounds.play(context, touchSoundUri) }
    fun editProduct(product: Product, context: Context, onConfigured: (String) -> Unit) { viewModelScope.launch { dao.updateProduct(product); cancelAlarm(context, product.id + 200000, "EXPIRE_TRIGGER"); if (product.expirationDateInMillis != null) scheduleNotification(context, product.expirationDateInMillis, "¡Producto por Vencer! ⚠️", "El producto ${product.name} ha alcanzado su fecha de caducidad.", product.id + 200000, "EXPIRE_TRIGGER"); onConfigured("Producto actualizado") }; AppSounds.play(context, touchSoundUri) }
    fun deleteProductEntirely(product: Product, context: Context) { viewModelScope.launch { dao.deleteProduct(product); if (product.expirationDateInMillis != null) cancelAlarm(context, product.id + 200000, "EXPIRE_TRIGGER") }; AppSounds.play(context, touchSoundUri) }

    fun processCartSale(cartItems: List<Pair<Product, Int>>, buyerName: String, paymentSummary: String, netCash: Double, netDigital: Double, context: Context, onSold: (String) -> Unit) {
        viewModelScope.launch {
            var totalSaleCOP = 0.0; var totalProfitCOP = 0.0; val itemNames = mutableListOf<String>()
            cartItems.forEach { (product, qty) -> val newStock = product.stock - qty; dao.updateProduct(product.copy(stock = newStock)); totalSaleCOP += (product.price * qty); totalProfitCOP += ((product.price - product.purchasePrice) * qty); itemNames.add("${qty}${product.unit} ${product.name}"); if (product.minStock > 0 && newStock <= product.minStock && product.stock > product.minStock) scheduleNotification(context, System.currentTimeMillis() + 1000L, "¡Stock Crítico! ⚠️", "El producto ${product.name} tiene solo $newStock unidades restantes.", product.id + 300000, "STOCK_TRIGGER") }
            val finalNote = buildString { if (buyerName.isNotBlank()) append("Cliente: $buyerName\n"); append("$paymentSummary\n"); append("Items: ${itemNames.joinToString(", ")}") }; val desc = if (cartItems.size == 1) "Venta: ${cartItems.first().first.name}" else "Venta: Varios Productos"
            dao.insertTransaction(Transaction(description = desc, amount = totalSaleCOP, isIncome = true, note = finalNote, profit = totalProfitCOP, cashAmount = netCash, digitalAmount = netDigital, country = selectedCountry)); onSold("Venta registrada exitosamente"); AppSounds.play(context, touchSoundUri)
        }
    }

    fun reduceProductStock(product: Product, qty: Int, context: Context) { viewModelScope.launch { val newStock = product.stock - qty; if (newStock <= 0) { dao.deleteProduct(product); if (product.expirationDateInMillis != null) cancelAlarm(context, product.id + 200000, "EXPIRE_TRIGGER") } else { dao.updateProduct(product.copy(stock = newStock)); if (product.minStock > 0 && newStock <= product.minStock && product.stock > product.minStock) scheduleNotification(context, System.currentTimeMillis() + 1000L, "¡Stock Crítico! ⚠️", "El producto ${product.name} tiene solo $newStock unidades restantes.", product.id + 300000, "STOCK_TRIGGER") } }; AppSounds.play(context, touchSoundUri) }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun restoreProductStock(product: Product, qty: Int, context: Context) {
        viewModelScope.launch {
            val currentInDb = dao.getAllProducts(selectedCountry).firstOrNull()?.find { it.id == product.id };
            if (currentInDb != null) dao.updateProduct(currentInDb.copy(stock = currentInDb.stock + qty))
            else {
                dao.insertProduct(product);
                if (product.expirationDateInMillis != null) scheduleNotification(context, product.expirationDateInMillis, "¡Producto por Vencer! ⚠️", "El producto ${product.name} ha alcanzado su fecha de caducidad.", product.id + 200000, "EXPIRE_TRIGGER")
            }
        }
    }

    fun addReminder(title: String, amount: Double, dateInMillis: Long, isStore: Boolean, context: Context, onConfigured: (String) -> Unit) {
        viewModelScope.launch {
            val reminderId = dao.insertReminder(Reminder(title = title, amount = amount, targetDateInMillis = dateInMillis, isStore = isStore, country = selectedCountry)).toInt()
            val amountStr = amount.toLong().toString()
            val voiceText = if (amount > 0) "Debes pagar tu deuda de $amountStr pesos a $title" else "Debes pagar a $title"
            val textMsg = if (amount > 0) "$title: ${formatCOP(amount)}" else title
            val success = scheduleNotification(context, dateInMillis, "¡Hora de Pagar! ⏰", textMsg, reminderId, "ALARM_TRIGGER", voiceText)
            if (success) onConfigured("Alarma programada para las ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(dateInMillis))}")
        }
        AppSounds.play(context, touchSoundUri)
    }

    fun updateExistingReminder(reminder: Reminder, context: Context, onConfigured: (String) -> Unit) {
        viewModelScope.launch {
            dao.updateReminder(reminder)
            val amountStr = reminder.amount.toLong().toString()
            val voiceText = if (reminder.amount > 0) "Debes pagar tu deuda de $amountStr pesos a ${reminder.title}" else "Debes pagar a ${reminder.title}"
            val textMsg = if (reminder.amount > 0) "${reminder.title}: ${formatCOP(reminder.amount)}" else reminder.title
            val success = scheduleNotification(context, reminder.targetDateInMillis, "¡Hora de Pagar! ⏰", textMsg, reminder.id, "ALARM_TRIGGER", voiceText)
            if (success) onConfigured("Alarma actualizada para las ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(reminder.targetDateInMillis))}")
        }
        AppSounds.play(context, touchSoundUri)
    }

    fun deleteReminder(reminder: Reminder, context: Context) { viewModelScope.launch { dao.deleteReminder(reminder); cancelAlarm(context, reminder.id, "ALARM_TRIGGER") } }

    private fun createFiadorVoiceText(name: String, amount: Double, reason: String, isStore: Boolean): String {
        val amountStr = amount.toLong().toString()
        if (isStore) {
            val voiceReason = reason.replace("Uds ", " unidades de ").replace("Kg ", " kilos de ").replace("L ", " litros de ")
            return "$name te debe $amountStr pesos, por la deuda de $voiceReason"
        } else {
            return "$name te debe $amountStr pesos"
        }
    }

    fun addFiador(name: String, phone: String, cartItems: List<Pair<Product, Int>>, personalDebtAmount: Double, dateInMillis: Long, initialCash: Double = 0.0, initialDigital: Double = 0.0, isStore: Boolean, context: Context, onConfigured: (String) -> Unit) {
        viewModelScope.launch {
            val totalAmount = if (isStore) cartItems.sumOf { it.first.price * it.second } else personalDebtAmount
            val totalCost = if (isStore) cartItems.sumOf { it.first.purchasePrice * it.second } else 0.0
            val reason = if (isStore) cartItems.joinToString(", ") { "${it.second}${it.first.unit} ${it.first.name}" } else "Préstamo personal"

            val initialPaidAmount = initialCash + initialDigital
            val history = if (initialPaidAmount > 0) {
                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                val methodStr = when { initialCash > 0 && initialDigital == 0.0 -> "Efectivo"; initialDigital > 0 && initialCash == 0.0 -> "Digital"; else -> "Múltiple" }
                "$dateStr: +${formatCOP(initialPaidAmount)} ($methodStr)"
            } else ""

            val fiadorId = dao.insertFiador(Fiador(name = name, phone = phone, amount = totalAmount, reason = reason, targetDateInMillis = dateInMillis, paidAmount = initialPaidAmount, paymentHistory = history, isStore = isStore, totalCost = totalCost, country = selectedCountry)).toInt()

            val totalProfitGenerated = maxOf(0.0, initialPaidAmount - totalCost)
            var cashProfit = 0.0
            var digitalProfit = 0.0

            if (initialPaidAmount > 0) {
                cashProfit = totalProfitGenerated * (initialCash / initialPaidAmount)
                digitalProfit = totalProfitGenerated * (initialDigital / initialPaidAmount)
            }

            val itemsNote = if (isStore && cartItems.isNotEmpty()) "Productos: $reason" else ""

            if (initialCash > 0) {
                val desc = if (isStore) "Venta: Abono inicial ($name)" else "Ingreso: Abono inicial ($name)"
                val finalNote = if (itemsNote.isNotEmpty()) "Abono inicial en Efectivo\n$itemsNote" else "Abono inicial en Efectivo"
                dao.insertTransaction(Transaction(description = desc, amount = initialCash, isIncome = true, note = finalNote, cashAmount = initialCash, digitalAmount = 0.0, profit = cashProfit, country = selectedCountry))
            }

            if (initialDigital > 0) {
                val desc = if (isStore) "Venta: Abono inicial ($name)" else "Ingreso: Abono inicial ($name)"
                val finalNote = if (itemsNote.isNotEmpty()) "Abono inicial en Digital\n$itemsNote" else "Abono inicial en Digital"
                dao.insertTransaction(Transaction(description = desc, amount = initialDigital, isIncome = true, note = finalNote, cashAmount = 0.0, digitalAmount = initialDigital, profit = digitalProfit, country = selectedCountry))
            }

            if (initialCash == 0.0 && initialDigital == 0.0) {
                val desc = if (isStore) "Venta a crédito ($name)" else "Ingreso a crédito ($name)"
                val finalNote = if (itemsNote.isNotEmpty()) "Venta fiada sin abono inicial\n$itemsNote" else "Venta fiada sin abono inicial"
                dao.insertTransaction(Transaction(description = desc, amount = 0.0, isIncome = true, note = finalNote, cashAmount = 0.0, digitalAmount = 0.0, profit = 0.0, country = selectedCountry))
            }

            if (isStore) {
                cartItems.forEach { (product, qty) ->
                    val newStock = product.stock - qty
                    if(newStock >= 0) {
                        dao.updateProduct(product.copy(stock = newStock))
                        if (product.minStock > 0 && newStock <= product.minStock && product.stock > product.minStock) {
                            scheduleNotification(context, System.currentTimeMillis() + 1000L, "¡Stock Crítico! ⚠️", "El producto ${product.name} tiene solo $newStock unidades restantes.", product.id + 300000, "STOCK_TRIGGER")
                        }
                    }
                }
            }

            val remaining = totalAmount - initialPaidAmount
            val voiceText = createFiadorVoiceText(name, remaining, reason, isStore)
            val success = scheduleNotification(context, dateInMillis, "¡Cobrar a $name! 💰", "Monto: ${formatCOP(remaining)} - $reason", fiadorId + 100000, "FIADOR_TRIGGER", voiceText)
            if (success) onConfigured("Recordatorio de fiador para las ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(dateInMillis))}")
        }
        AppSounds.play(context, touchSoundUri)
    }

    fun updateExistingFiador(fiador: Fiador, context: Context, onConfigured: (String) -> Unit) {
        viewModelScope.launch {
            dao.updateFiador(fiador)
            val remaining = fiador.amount - fiador.paidAmount
            val voiceText = createFiadorVoiceText(fiador.name, remaining, fiador.reason, fiador.isStore)
            val success = scheduleNotification(context, fiador.targetDateInMillis, "¡Cobrar a ${fiador.name}! 💰", "Monto: ${formatCOP(remaining)} - ${fiador.reason}", fiador.id + 100000, "FIADOR_TRIGGER", voiceText)
            if (success) onConfigured("Recordatorio actualizado para las ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(fiador.targetDateInMillis))}")
        }
        AppSounds.play(context, touchSoundUri)
    }

    fun deleteFiador(fiador: Fiador, context: Context) { viewModelScope.launch { dao.deleteFiador(fiador); cancelAlarm(context, fiador.id + 100000, "FIADOR_TRIGGER") } }

    fun restoreFiador(name: String, totalAmount: Double, paidAmount: Double, context: Context, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val f = Fiador(
                name = name,
                phone = "",
                amount = totalAmount,
                reason = "Deuda retomada manualmente",
                targetDateInMillis = System.currentTimeMillis() + 86400000L, // Alerta para el día siguiente
                paidAmount = paidAmount,
                paymentHistory = "Restaurado tras cobro accidental",
                isStore = true,
                totalCost = 0.0,
                country = selectedCountry
            )
            val id = dao.insertFiador(f).toInt()
            val remaining = totalAmount - paidAmount
            val voiceText = "$name te debe ${remaining.toLong()} pesos"
            scheduleNotification(context, f.targetDateInMillis, "¡Cobrar a $name! 💰", "Monto: ${formatCOP(remaining)}", id + 100000, "FIADOR_TRIGGER", voiceText)
            launch(Dispatchers.Main) { onResult("Deuda de $name restaurada correctamente ♻️") }
        }
        AppSounds.play(context, touchSoundUri)
    }

    fun registerAbonoFiador(fiador: Fiador, abono: Double, method: String, context: Context, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val oldPaidAmount = fiador.paidAmount
            val newPaidAmount = oldPaidAmount + abono
            val remaining = fiador.amount - newPaidAmount

            val oldProfit = maxOf(0.0, oldPaidAmount - fiador.totalCost)
            val newProfit = maxOf(0.0, newPaidAmount - fiador.totalCost)
            val generatedProfitForThisAbono = newProfit - oldProfit

            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val historyEntry = "$dateStr: +${formatCOP(abono)} ($method)"
            val newHistory = if (fiador.paymentHistory.isEmpty()) historyEntry else fiador.paymentHistory + "\n$historyEntry"

            val cash = if(method == "Efectivo") abono else 0.0
            val digital = if(method == "Digital") abono else 0.0
            val desc = if (fiador.isStore) "Venta: Abono de ${fiador.name}" else "Ingreso: Abono de ${fiador.name}"

            dao.insertTransaction(Transaction(description = desc, amount = abono, isIncome = true, note = "Abono de deuda parcial", cashAmount = cash, digitalAmount = digital, profit = generatedProfitForThisAbono, country = selectedCountry))

            if (newPaidAmount >= fiador.amount) {
                dao.deleteFiador(fiador)
                cancelAlarm(context, fiador.id + 100000, "FIADOR_TRIGGER")
                launch(Dispatchers.Main) { onResult("¡Deuda de ${fiador.name} saldada por completo! 🎉") }
            } else {
                dao.updateFiador(fiador.copy(paidAmount = newPaidAmount, paymentHistory = newHistory))
                val voiceText = createFiadorVoiceText(fiador.name, remaining, fiador.reason, fiador.isStore)
                scheduleNotification(context, fiador.targetDateInMillis, "¡Cobrar a ${fiador.name}! 💰", "Monto: ${formatCOP(remaining)} - ${fiador.reason}", fiador.id + 100000, "FIADOR_TRIGGER", voiceText)
                launch(Dispatchers.Main) { onResult("Abono de ${formatCOP(abono)} registrado. Resta: ${formatCOP(remaining)}") }
            }
            AppSounds.play(context, touchSoundUri)
        }
    }

    private fun cancelAlarm(context: Context, id: Int, actionPrefix: String) { val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager; val intent = Intent(context, ReminderReceiver::class.java).apply { action = "com.xxcamixx.contabilidad.${actionPrefix}_${id}" }; val pendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); alarmManager.cancel(pendingIntent) }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleNotification(context: Context, timeInMillis: Long, notifTitle: String, notifText: String, id: Int, actionPrefix: String, voiceText: String? = null): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = Uri.parse("package:${context.packageName}"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent); Toast.makeText(context, "⚠️ Otorga el permiso de Alarmas Exactas.", Toast.LENGTH_LONG).show(); return false
            }
        }
        val isPersonal = actionPrefix == "ALARM_TRIGGER"
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.xxcamixx.contabilidad.${actionPrefix}_$id"
            putExtra("NOTIFICATION_TITLE", notifTitle)
            putExtra("NOTIFICATION_TEXT", notifText)
            putExtra("ID", id)
            putExtra("NOTIF_TYPE", if (isPersonal) "PERSONAL" else "STORE")
            if (voiceText != null) putExtra("VOICE_TEXT", voiceText)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
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

class ProductDraftState {
    var name by mutableStateOf("")
    var purchasePriceRaw by mutableStateOf("")
    var priceRaw by mutableStateOf("")
    var stockRaw by mutableStateOf("")
    var minStockRaw by mutableStateOf("")
    var selectedUnit by mutableStateOf("Uds")
    var hasExpiry by mutableStateOf(false)
    var expiryDateMillis by mutableStateOf<Long?>(null)
    var imageUri by mutableStateOf<String?>(null)

    fun clear() {
        name = ""
        purchasePriceRaw = ""
        priceRaw = ""
        stockRaw = ""
        minStockRaw = ""
        selectedUnit = "Uds"
        hasExpiry = false
        expiryDateMillis = null
        imageUri = null
    }

    fun loadFrom(product: Product) {
        name = product.name
        val pCost = product.purchasePrice
        // Modificación crucial: Usar alta precisión para no perder decimales
        // al reconstruir los bolívares en el modo Venezuela.
        purchasePriceRaw = if (pCost > 0) {
            if (pCost % 1.0 == 0.0) pCost.toLong().toString() else {
                val df = java.text.DecimalFormat("#.######", java.text.DecimalFormatSymbols(Locale.US))
                df.format(pCost)
            }
        } else ""

        val pPrice = product.price
        priceRaw = if (pPrice > 0) {
            if (pPrice % 1.0 == 0.0) pPrice.toLong().toString() else {
                val df = java.text.DecimalFormat("#.######", java.text.DecimalFormatSymbols(Locale.US))
                df.format(pPrice)
            }
        } else ""

        stockRaw = product.stock.toString()
        minStockRaw = if (product.minStock > 0) product.minStock.toString() else ""
        selectedUnit = product.unit
        hasExpiry = product.expirationDateInMillis != null
        expiryDateMillis = product.expirationDateInMillis
        imageUri = product.imageUri
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
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // BLOQUEAR ROTACIÓN DE PANTALLA EN TODA LA APP
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

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

@SuppressLint("ContextGetResource", "DiscouragedApi")
@Composable
fun LoginScreen(onLoginSuccess: (String, String, String, Long, Long) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    // SOLUCIÓN ERROR 1: Extraemos el valor del stringResource fuera del onClick para respetar el entorno Composable
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    val webClientId = if (resId != 0) androidx.compose.ui.res.stringResource(id = resId) else ""

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.AccountCircle, contentDescription = "Login", modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Acceso a Billetera", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Sincronización segura en la nube", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(webClientId)
                                    .setAutoSelectEnabled(true)
                                    .build()
                                val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
                                val result = credentialManager.getCredential(request = request, context = context)
                                val credential = result.credential

                                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                    val displayName = googleIdTokenCredential.displayName ?: "Usuario"
                                    val userId = googleIdTokenCredential.id
                                    val isSuperAdmin = userId.lowercase(Locale.getDefault()) == "zonacami77777@gmail.com"

                                    try {
                                        val response = RetrofitInstance.api.syncUser(UserSyncRequest(email = userId, name = displayName))
                                        val finalRole = if (isSuperAdmin) "ADMIN" else (response.role ?: "INVITADO")
                                        if (response.isBanned && !isSuperAdmin) {
                                            isLoading = false
                                            Toast.makeText(context, "🚫 Tu cuenta está bloqueada o vencida.", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Usted se encuentra bajo el PLAN $finalRole, Bienvenido", Toast.LENGTH_LONG).show()
                                            onLoginSuccess(displayName, userId, finalRole, response.consumedSeconds, response.planDuration)
                                        }
                                    } catch (_: Exception) {
                                        val fallbackRole = if (isSuperAdmin) "ADMIN" else "BÁSICO"
                                        Toast.makeText(context, "Modo sin conexión activado. Usted se encuentra bajo el PLAN $fallbackRole, Bienvenido", Toast.LENGTH_LONG).show()
                                        onLoginSuccess(displayName, userId, fallbackRole, 0L, 2592000L)
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Error al procesar la credencial", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                isLoading = false
                                Toast.makeText(context, "Inicio de sesión cancelado o fallido", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
                ) {
                    Text("Iniciar sesión con Google", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            val addAccountIntent = Intent(Settings.ACTION_ADD_ACCOUNT).apply { putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google")) }
                            context.startActivity(addAccountIntent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "No se pudo abrir la configuración", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Añadir cuenta", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Añadir cuenta nueva", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.fillMaxWidth(0.8f))
                Spacer(modifier = Modifier.height(16.dp))

                // BOTÓN DE INVITADO GOLD
                Button(
                    onClick = {
                        isLoading = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val authPrefs = context.getSharedPreferences("GlobalAuthPrefs", Context.MODE_PRIVATE)
                            var guestId = authPrefs.getString("trialGuestId", null)
                            if (guestId == null) {
                                guestId = "prueba_" + UUID.randomUUID().toString().substring(0, 8)
                                authPrefs.edit().putString("trialGuestId", guestId).apply()
                            }
                            try {
                                RetrofitInstance.api.syncUser(UserSyncRequest(email = guestId, name = "Usuario de Prueba"))
                                // Inicia con 1 día exacto (86400 segundos)
                                RetrofitInstance.api.manageUser(UserManageRequest(guestId, "setRole", "Invitado-Gold", 86400L))
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, "Modo Invitado-Gold Activado ⏳", Toast.LENGTH_LONG).show()
                                    onLoginSuccess("Usuario de Prueba", guestId, "Invitado-Gold", 0L, 86400L)
                                }
                            } catch (e: Exception) {
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, "Modo Invitado-Gold Local (Sin conexión) ⏳", Toast.LENGTH_LONG).show()
                                    onLoginSuccess("Usuario de Prueba", guestId, "Invitado-Gold", 0L, 86400L)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                ) {
                    Text("Prueba 1 día gratis 🌟", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_SYNC_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "No se pudo abrir la configuración", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Gestionar o Eliminar cuentas del dispositivo", color = Color.Gray, textDecoration = TextDecoration.Underline, textAlign = TextAlign.Center)
                }
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

    var showPlansDialog by remember { mutableStateOf(!isSuperAdmin && initialRole != "GOLD" && initialRole != "ADMIN" && initialRole != "PRUEBA" && initialRole != "Invitado-Gold") }
    var showRoleUpgradeDialog by remember { mutableStateOf<String?>(null) }
    var showRoleDowngradeDialog by remember { mutableStateOf(false) }
    var showWarningDialog by remember { mutableStateOf<String?>(null) }
    var showTrialWarning by remember { mutableStateOf(false) }

    // NUEVO: Estado para mostrar el modal de bienvenida al Invitado-Gold (Solo si lleva menos de 5 mins consumidos)
    var showGoldWelcomeDialog by remember { mutableStateOf(initialRole == "Invitado-Gold" && initialConsumedSeconds < 300L) }

    var preselectedDateForEvent by remember { mutableStateOf<Long?>(null) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var isSyncingAccount by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while(true) {
            delay(30000L)

            // --- NUEVO: Sincronización bajo perfil cada 30 segundos ---
            if (currentRole != "INVITADO" && currentRole != "INVITADO_PRUEBA") {
                viewModel.silentBackup()
            }
            // -------------------------------------------------------------

            try {
                val response = RetrofitInstance.api.addUserTime(UserTimeRequest(viewModel.userId, 30L))
                if (response.isBanned && !isSuperAdmin) { Toast.makeText(context, "Tu tiempo ha culminado o has sido bloqueado.", Toast.LENGTH_LONG).show(); onLogout(); break }

                val newRole = response.role ?: currentRole
                if (newRole != currentRole && !isSuperAdmin) {
                    if (newRole == "INVITADO" || newRole == "INVITADO_PRUEBA") { showRoleDowngradeDialog = true } else { showRoleUpgradeDialog = newRole }
                    currentRole = newRole
                    authPrefs.edit().putString("userRole", currentRole).putString("lastKnownRole", currentRole).apply()
                }

                if (response.planDuration > 0L) {
                    currentConsumed = response.consumedSeconds
                    currentPlanDuration = response.planDuration
                    authPrefs.edit().putLong("consumedSeconds", currentConsumed).putLong("planDuration", currentPlanDuration).apply()
                }

                if (currentRole != "INVITADO" && currentRole != "INVITADO_PRUEBA" && !isSuperAdmin) {
                    val timeLeftSecs = currentPlanDuration - currentConsumed
                    val daysLeft = timeLeftSecs / 86400L
                    val lastWarning = authPrefs.getLong("lastWarning_$daysLeft", 0L)
                    val now = System.currentTimeMillis()
                    if (currentRole != "PRUEBA" && currentRole != "Invitado-Gold" && daysLeft in listOf(7L, 3L, 2L, 1L) && (now - lastWarning > 86400000L)) {
                        showWarningDialog = "Tu plan expirará en $daysLeft días. Realiza el pago para mantener tus privilegios o pasarás a ser INVITADO."
                        authPrefs.edit().putLong("lastWarning_$daysLeft", now).apply()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // TICKER EN VIVO DE 1 SEGUNDO Y CONTROL DE MODOS
    LaunchedEffect(Unit) {
        val hasShownTrialWarning = authPrefs.getBoolean("hasShownTrialWarning_${viewModel.userId}", false)
        while (true) {
            delay(1000L)
            if (!isSuperAdmin) {
                currentConsumed++
                val timeLeftSecs = currentPlanDuration - currentConsumed

                // ADVERTENCIA 3 HORAS (10800 Segundos) DEL MODO PRUEBA
                if ((currentRole == "PRUEBA" || currentRole == "Invitado-Gold") && timeLeftSecs in 1..10800 && !hasShownTrialWarning) {
                    showTrialWarning = true
                    authPrefs.edit().putBoolean("hasShownTrialWarning_${viewModel.userId}", true).apply()
                }

                // TRANSICIÓN AUTOMÁTICA AL CADUCAR EL TIEMPO
                if (timeLeftSecs <= 0 && currentRole != "INVITADO" && currentRole != "INVITADO_PRUEBA") {
                    if (currentRole == "PRUEBA" || currentRole == "Invitado-Gold") {
                        currentRole = "INVITADO_PRUEBA"
                        authPrefs.edit().putString("userRole", "INVITADO_PRUEBA").putString("lastKnownRole", "INVITADO_PRUEBA").apply()
                        coroutineScope.launch(Dispatchers.IO) { try { RetrofitInstance.api.manageUser(UserManageRequest(viewModel.userId, "setRole", "INVITADO_PRUEBA", 2592000L)) } catch (e: Exception){} }
                    } else {
                        showRoleDowngradeDialog = true
                        currentRole = "INVITADO"
                        authPrefs.edit().putString("userRole", "INVITADO").putString("lastKnownRole", "INVITADO").apply()
                    }
                }
            }
        }
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
        while (true) {
            delay(5000L)
            try {
                val isChatOpen = showChatDialog || showAdminChatList
                val lastRead = authPrefs.getLong("lastReadChat_${viewModel.userId}", 0L)
                val lastNotified = authPrefs.getLong("lastNotified_${viewModel.userId}", System.currentTimeMillis())
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

    var currentTab by remember { mutableStateOf(0) }
    val currentTabReminders = remember(reminders, currentTab) { reminders.filter { it.isStore == (currentTab == 1) } }
    val currentTabFiadores = remember(fiadores, currentTab) { fiadores.filter { it.isStore == (currentTab == 1) } }

    val personalTransactions = remember(transactions) { transactions.filter { !it.description.startsWith("Venta: ") } }
    val storeTransactions = remember(transactions) { transactions.filter { it.isIncome && it.description.startsWith("Venta:") } }
    val totalStoreCash = remember(storeTransactions) { storeTransactions.sumOf { it.cashAmount } }
    val totalStoreDigital = remember(storeTransactions) { storeTransactions.sumOf { it.digitalAmount } }
    val totalProfit = remember(storeTransactions) { storeTransactions.sumOf { it.profit } }

    var showInventoryScreen by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    var showAddProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    val productDraftState = remember { ProductDraftState() }

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

    var checkoutToFiadorName by remember { mutableStateOf("") }
    var checkoutToFiadorCart by remember { mutableStateOf<List<Pair<Product, Int>>>(emptyList()) }
    var checkoutToFiadorCash by remember { mutableStateOf(0.0) }
    var checkoutToFiadorDigital by remember { mutableStateOf(0.0) }

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

    val isLockedStore = currentRole == "BÁSICO" || currentRole == "INVITADO" || currentRole == "INVITADO_PRUEBA"
    val isManualSyncAllowed = currentRole != "INVITADO" && currentRole != "INVITADO_PRUEBA"
    val isResumenAllowed = currentRole == "PREMIUM" || currentRole == "GOLD" || currentRole == "ADMIN" || currentRole == "PRUEBA" || currentRole == "Invitado-Gold"
    val isBorrarHistorialAllowed = currentRole != "INVITADO_PRUEBA"

    val snackbarHostState = remember { SnackbarHostState() }
    val totalIncome = remember(personalTransactions) { personalTransactions.filter { it.isIncome }.sumOf { it.amount } }
    val totalExpense = remember(personalTransactions) { personalTransactions.filter { !it.isIncome }.sumOf { it.amount } }
    val balance = totalIncome - totalExpense

    val personalCashIncome = remember(personalTransactions) { personalTransactions.filter { it.isIncome }.sumOf { it.cashAmount } }
    val personalCashExpense = remember(personalTransactions) { personalTransactions.filter { !it.isIncome }.sumOf { it.cashAmount } }
    val personalDigitalIncome = remember(personalTransactions) { personalTransactions.filter { it.isIncome }.sumOf { it.digitalAmount } }
    val personalDigitalExpense = remember(personalTransactions) { personalTransactions.filter { !it.isIncome }.sumOf { it.digitalAmount } }
    val personalCashBalance = personalCashIncome - personalCashExpense
    val personalDigitalBalance = personalDigitalIncome - personalDigitalExpense

    var currentUiTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(60000L); currentUiTime = System.currentTimeMillis() } }

    val activeReminders = remember(currentTabReminders, currentUiTime) { currentTabReminders.filter { it.targetDateInMillis <= currentUiTime } }
    val activeFiadores = remember(currentTabFiadores, currentUiTime) { currentTabFiadores.filter { it.targetDateInMillis <= currentUiTime } }

    LaunchedEffect(customToastMessage ?: "") { if (customToastMessage != null) { delay(3000L); customToastMessage = null } }
    LaunchedEffect(undoMessage ?: "") { if (undoMessage != null) { delay(5000L); undoMessage = null; undoAction = null } }

    BackHandler {
        if (showInventoryScreen) {
            showInventoryScreen = false
        } else if (backPressedOnce) {
            (context as? ComponentActivity)?.finish()
        } else {
            backPressedOnce = true
            Toast.makeText(context, "Presiona Atrás de nuevo para salir", Toast.LENGTH_SHORT).show()
            coroutineScope.launch { delay(2000L); backPressedOnce = false }
        }
    }

    val crownEmoji = when (currentRole) { "INVITADO", "INVITADO_PRUEBA" -> "🪵"; "PRUEBA", "Invitado-Gold" -> "⏳"; "BÁSICO" -> "🥉"; "PREMIUM" -> "🥈"; "GOLD" -> "🥇"; "ADMIN" -> "👑"; else -> "🪵" }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!showInventoryScreen) {
                TopAppBar(
                    title = {
                        val firstName = userName.split(" ").first()
                        Column {
                            Text(text = if (currentTab == 0) "Hola, $firstName $crownEmoji" else "Tienda de $firstName 🏪", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            if (viewModel.selectedCountry == "Venezuela" && viewModel.bcvRate > 0) {
                                Text(text = "Tasa BCV: ${formatBs(viewModel.bcvRate)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, actionIconContentColor = MaterialTheme.colorScheme.onPrimary),
                    actions = {
                        IconButton(onClick = { showCalendarDialog = true }) { Icon(Icons.Filled.DateRange, "Calendario") }

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

                            if (currentTab == 0) {
                                if (isResumenAllowed) { DropdownMenuItem(text = { Text("📊 Resumen de Totales") }, onClick = { showSummaryDialog = true; showMenu = false }) } else { DropdownMenuItem(text = { Text("👑 📊 Resumen de Totales", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }, onClick = { showPremiumToastMsg(context); showMenu = false }) }
                                DropdownMenuItem(text = { Text("🔔 Saldo Crítico") }, onClick = { showLimitDialog = true; showMenu = false })
                            }

                            DropdownMenuItem(text = { Text("⚙️ Opciones") }, onClick = { showOptionsDialog = true; showMenu = false })

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
        floatingActionButton = {
            if (!showInventoryScreen && currentTab == 0) {
                FloatingActionButton(onClick = {
                    if (currentRole == "INVITADO_PRUEBA") {
                        Toast.makeText(context, "Modo Restringido: Solo puedes visualizar información.", Toast.LENGTH_SHORT).show()
                    } else {
                        showAddDialog = true
                    }
                }, containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary) {
                    Icon(Icons.Filled.Add, "Agregar")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.background)) {

            // NUEVO: ALERTA DE BIENVENIDA INVITADO GOLD
            if (showGoldWelcomeDialog) {
                val timeLeftSecs = currentPlanDuration - currentConsumed
                val hours = timeLeftSecs / 3600
                val mins = (timeLeftSecs % 3600) / 60
                AlertDialog(
                    onDismissRequest = { showGoldWelcomeDialog = false },
                    title = { Text("¡Modo Invitado-Gold! 🌟", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    text = { Text("Disfrutas de acceso total a todas las herramientas sin ninguna restricción. Te quedan $hours horas y $mins minutos de este privilegio.\n\nTus datos se estarán guardando y respaldando bajo perfil en la nube cada 30 segundos automáticamente.") },
                    confirmButton = { Button(onClick = { showGoldWelcomeDialog = false }) { Text("¡Entendido!") } }
                )
            }

            // ALERTAS DE PRUEBA Y TIEMPO
            if (showTrialWarning) {
                AlertDialog(
                    onDismissRequest = { showTrialWarning = false },
                    title = { Text("¡Tu Prueba está por terminar! ⏳", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    text = { Text("Te quedan menos de 3 horas de tu Modo de Prueba. Una vez que el tiempo culmine, pasarás automáticamente al Modo Invitado, el cual tiene funciones restringidas de solo lectura. Contacta al administrador si deseas adquirir un plan completo.") },
                    confirmButton = { Button(onClick = { showTrialWarning = false }) { Text("Entendido") } }
                )
            }

            if (showInventoryScreen) {
                InventoryScreen(
                    products = products,
                    shoppingCart = shoppingCart,
                    selectedCountry = viewModel.selectedCountry,
                    bcvRate = viewModel.bcvRate,
                    onBack = { showInventoryScreen = false },
                    onAddProductClick = {
                        productToEdit = null
                        productDraftState.clear()
                        showAddProductDialog = true
                    },
                    onAddToCartClick = { productToAddToCart = it },
                    onOpenCheckout = { showCheckoutDialog = true },
                    onEditClick = {
                        productToEdit = it
                        productDraftState.loadFrom(it)
                        showAddProductDialog = true
                    },
                    onDeleteClick = { productToDelete = it; qtyToDelete = "1"; showDeleteQtyDialog = true },
                    onLongDeleteClick = { productToFullDelete = it },
                    onInfoClick = { productToInfo = it }
                )
            } else {
                Crossfade(targetState = currentTab, label = "TabSwitch") { tab ->
                    if (tab == 0) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            DashboardCard(balance, totalIncome, totalExpense, personalCashBalance, personalDigitalBalance)
                            AnimatedVisibility(visible = viewModel.minBalanceThreshold > 0 && balance < viewModel.minBalanceThreshold) {
                                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).background(Color(0xFFD32F2F), RoundedCornerShape(8.dp)).padding(12.dp)) {
                                    Text("⚠️ ¡Alerta! Tu saldo está por debajo del límite crítico (${formatMoneyMain(viewModel.minBalanceThreshold, viewModel.selectedCountry)}).", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            activeFiadores.forEach { fiador ->
                                val remaining = fiador.amount - fiador.paidAmount
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).background(Color(0xFFFBC02D), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).clickable { fiadorToEdit = fiador; showFiadorDialog = true }.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val phoneStr = if (fiador.phone.isNotBlank()) " 📞 ${fiador.phone}" else ""
                                        Text("💰 Cobrar a ${fiador.name}$phoneStr", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Resta: ${formatMoneyMain(remaining, viewModel.selectedCountry)} de ${formatMoneyMain(fiador.amount, viewModel.selectedCountry)} - ${fiador.reason}", color = Color.Black.copy(alpha = 0.8f), fontSize = 12.sp)
                                    }
                                    IconButton(onClick = { viewModel.deleteFiador(fiador, context) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Filled.Check, contentDescription = "Saldado", tint = Color.Black)
                                    }
                                }
                            }

                            activeReminders.forEach { reminder ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).background(Color(0xFF1976D2), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).clickable { reminderToEdit = reminder; showReminderDialog = true }.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("📅 Pagar: ${reminder.title}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        if (reminder.amount > 0) {
                                            Text("Monto: ${formatMoneyMain(reminder.amount, viewModel.selectedCountry)}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteReminder(reminder, context) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Filled.Check, contentDescription = "Hecho", tint = Color.White)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Movimientos Recientes 📋", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 16.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                                items(personalTransactions, key = { it.id }) { transaction ->
                                    Box(modifier = Modifier.animateItem(placementSpec = tween(400))) {
                                        TransactionItem(transaction = transaction, onDelete = {
                                            viewModel.deleteTransaction(transaction)
                                            coroutineScope.launch {
                                                val result = snackbarHostState.showSnackbar(message = "Registro eliminado 🗑️", actionLabel = "Deshacer ↩️", duration = SnackbarDuration.Short)
                                                if (result == SnackbarResult.ActionPerformed) viewModel.insertRawTransaction(transaction)
                                            }
                                        })
                                    }
                                }
                            }
                        }
                    } else {
                        StoreScreen(
                            products = products,
                            transactions = transactions,
                            shoppingCart = shoppingCart,
                            isLockedStore = isLockedStore,
                            totalStoreCash = totalStoreCash,
                            totalStoreDigital = totalStoreDigital,
                            selectedCountry = viewModel.selectedCountry,
                            bcvRate = viewModel.bcvRate,
                            onOpenInventory = { showInventoryScreen = true },
                            onOpenCheckout = { showCheckoutDialog = true },
                            onResetProfitsClick = { showResetProfitsDialog = true },
                            onDeleteVentas = { list -> viewModel.deleteTransactionsList(list) },
                            showPremiumToast = {
                                if (currentRole == "INVITADO_PRUEBA") Toast.makeText(context, "Modo Restringido: Solo puedes visualizar información.", Toast.LENGTH_SHORT).show()
                                else showPremiumToastMsg(context)
                            },
                            totalProfit = totalProfit,
                            activeFiadores = activeFiadores,
                            activeReminders = activeReminders,
                            onSettleFiador = { viewModel.deleteFiador(it, context) },
                            onEditFiador = { fiadorToEdit = it; showFiadorDialog = true },
                            onSettleReminder = { viewModel.deleteReminder(it, context) },
                            onEditReminder = { reminderToEdit = it; showReminderDialog = true },
                            onRestoreFiador = { nameToRestore, totalAmount, paidAmount ->
                                viewModel.restoreFiador(nameToRestore, totalAmount, paidAmount, context) { msg ->
                                    customToastMessage = msg
                                }
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = undoMessage != null, enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(300)), exit = fadeOut(tween(500)), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp, start = 16.dp, end = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF323232), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = undoMessage ?: "", color = Color.White, modifier = Modifier.weight(1f), fontSize = 14.sp)
                    TextButton(onClick = { undoAction?.invoke(); undoMessage = null; undoAction = null }) {
                        Text("DESHACER", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            AnimatedVisibility(visible = customToastMessage != null, enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { 50 }), exit = fadeOut(tween(1500)), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)) {
                Box(modifier = Modifier.background(Color.DarkGray.copy(alpha = 0.9f), RoundedCornerShape(24.dp)).padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Text(text = customToastMessage ?: "", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            if (!showInventoryScreen && currentTab == 0) {
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp)) {
                    FloatingActionButton(
                        onClick = {
                            if (!isSyncingAccount) {
                                isSyncingAccount = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val response = RetrofitInstance.api.syncUser(UserSyncRequest(viewModel.userId, userName))
                                        launch(Dispatchers.Main) {
                                            val newRole = response.role ?: currentRole
                                            if (newRole != currentRole && !isSuperAdmin) {
                                                if (newRole == "INVITADO" || newRole == "INVITADO_PRUEBA") { showRoleDowngradeDialog = true } else { showRoleUpgradeDialog = newRole }
                                                currentRole = newRole
                                                authPrefs.edit().putString("userRole", currentRole).putString("lastKnownRole", currentRole).apply()
                                            }
                                            if (response.planDuration > 0L) {
                                                currentConsumed = response.consumedSeconds
                                                currentPlanDuration = response.planDuration
                                                authPrefs.edit().putLong("consumedSeconds", currentConsumed).putLong("planDuration", currentPlanDuration).apply()
                                            }
                                            customToastMessage = "Sincronización completada ✅"
                                            isSyncingAccount = false
                                        }
                                    } catch (e: Exception) {
                                        launch(Dispatchers.Main) {
                                            customToastMessage = "Falla de sincronización"
                                            isSyncingAccount = false
                                        }
                                    }
                                }
                            }
                        },
                        containerColor = Color.DarkGray.copy(alpha = 0.5f),
                        contentColor = Color.White,
                        modifier = Modifier.size(40.dp),
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = "Sincronizar Cuenta", modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (isSyncingAccount || viewModel.isSyncing) {
                val displayMessage = if (isSyncingAccount) "Sincronizando tu información..." else viewModel.syncMessage
                Dialog(onDismissRequest = { }) {
                    Box(modifier = Modifier.size(240.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(60.dp), strokeWidth = 6.dp)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(displayMessage, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // BLOQUE PANEL DE ADMINISTRADOR
        if (showAdminPanelDialog) {
            var usersList by remember { mutableStateOf<Map<String, UserData>?>(null) }; var isLoadingUsers by remember { mutableStateOf(true) }
            var roleToAssign by remember { mutableStateOf<String?>(null) }; var targetEmailToAssign by remember { mutableStateOf<String?>(null) }
            val formatTimeLeft = { secs: Long, maxSecs: Long -> val left = maxSecs - secs; if (left <= 0) "0s" else { val days = left / 86400; val hours = (left % 86400) / 3600; "${days}d ${hours}h" } }
            LaunchedEffect(Unit) { try { usersList = RetrofitInstance.api.getAllUsers() } catch (_: Exception) { customToastMessage = "Error cargando usuarios" }; isLoadingUsers = false }
            fun manageUser(targetEmail: String, action: String, newRole: String? = null, pDuration: Long? = null) { coroutineScope.launch(Dispatchers.IO) { try { RetrofitInstance.api.manageUser(UserManageRequest(targetEmail, action, newRole, pDuration)); val updatedList = RetrofitInstance.api.getAllUsers(); launch(Dispatchers.Main) { usersList = updatedList; customToastMessage = "Acción completada" } } catch (_: Exception) { launch(Dispatchers.Main) { customToastMessage = "Fallo de conexión" } } } }

            if (roleToAssign != null && targetEmailToAssign != null) {
                var customHours by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
                    title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Duración para $roleToAssign ⏱️", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)); IconButton(onClick = { roleToAssign = null; targetEmailToAssign = null }) { Icon(Icons.Filled.Close, "Cerrar") } } },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            if (roleToAssign == "PRUEBA" || roleToAssign == "INVITADO_PRUEBA" || roleToAssign == "Invitado-Gold") {
                                Text("Configuración de Prueba/Invitado", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { manageUser(targetEmailToAssign!!, "setRole", roleToAssign, 86400L); roleToAssign = null }, modifier = Modifier.fillMaxWidth()) { Text("1 Día (24 horas)") }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = customHours, onValueChange = { customHours = it.filter { c -> c.isDigit() } }, label = { Text("Asignar Horas exactas") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { val h = customHours.toLongOrNull() ?: 0L; if (h > 0) { manageUser(targetEmailToAssign!!, "setRole", roleToAssign, h * 3600L); roleToAssign = null } }, modifier = Modifier.fillMaxWidth(), enabled = customHours.isNotEmpty()) { Text("Guardar Horas") }

                                if (roleToAssign == "INVITADO_PRUEBA") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Nota: El Invitado de Prueba solo tiene permisos de visualización. No puede registrar datos.", fontSize = 12.sp, color = Color.Gray)
                                }
                            } else {
                                Button(onClick = { manageUser(targetEmailToAssign!!, "setRole", roleToAssign, 2592000L); roleToAssign = null }, modifier = Modifier.fillMaxWidth()) { Text("1 Mes (30 días)") }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { manageUser(targetEmailToAssign!!, "setRole", roleToAssign, 15552000L); roleToAssign = null }, modifier = Modifier.fillMaxWidth()) { Text("6 Meses (180 días)") }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { manageUser(targetEmailToAssign!!, "setRole", roleToAssign, 31104000L); roleToAssign = null }, modifier = Modifier.fillMaxWidth()) { Text("1 Año (360 días)") }
                            }
                        }
                    },
                    confirmButton = {}, dismissButton = { }, containerColor = MaterialTheme.colorScheme.surface
                )
            }

            AlertDialog(
                onDismissRequest = { }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
                title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Panel de Control 🛠️", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = { showAdminPanelDialog = false }) { Icon(Icons.Filled.Close, "Cerrar") } } },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    if (isLoadingUsers) { CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)) }
                    else if (usersList.isNullOrEmpty()) { Text("No hay usuarios registrados.", color = Color.Gray) }
                    else {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            items(usersList!!.entries.toList()) { entry ->
                                val email = entry.key; val userData = entry.value; var expandedRoleMenu by remember { mutableStateOf(false) }
                                val isAdminAccount = email.lowercase(Locale.getDefault()) == "zonacami77777@gmail.com"

                                val listCrown = when (userData.role ?: "INVITADO") { "INVITADO", "INVITADO_PRUEBA" -> "🪵"; "PRUEBA", "Invitado-Gold" -> "⏳"; "BÁSICO" -> "🥉"; "PREMIUM" -> "🥈"; "GOLD" -> "🥇"; "ADMIN" -> "👑"; else -> "🪵" }

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
                                                    listOf("INVITADO", "INVITADO_PRUEBA", "PRUEBA", "Invitado-Gold", "BÁSICO", "PREMIUM", "GOLD").forEach { newRole ->
                                                        DropdownMenuItem(text = { Text(newRole) }, onClick = {
                                                            expandedRoleMenu = false
                                                            if (newRole == "INVITADO") { manageUser(email, "setRole", newRole, 2592000L) }
                                                            else { roleToAssign = newRole; targetEmailToAssign = email }
                                                        })
                                                    }
                                                }
                                            }
                                            OutlinedButton(onClick = { manageUser(email, "resetTime") }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("Reset") }

                                            if (!isAdminAccount) {
                                                if (userData.isBanned) { Button(onClick = { manageUser(email, "unban") }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("Desbloq.") } }
                                                else { Button(onClick = { manageUser(email, "ban") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), contentPadding = PaddingValues(0.dp)) { Text("Bloquear") } }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }, confirmButton = { }, dismissButton = { }
            )
        }

        // 1. Diálogo de Agregar Transacción (Personal)
        if (showAddDialog) {
            AddTransactionDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { desc, amount, isIncome, note, method ->
                    viewModel.addTransaction(desc, amount, isIncome, note, method)
                    showAddDialog = false
                }
            )
        }

        // 2. Diálogo de Agregar / Editar Producto (Inventario)
        if (showAddProductDialog) {
            AddProductDialog(
                isEditMode = productToEdit != null,
                draftState = productDraftState,
                selectedCountry = viewModel.selectedCountry,
                bcvRate = viewModel.bcvRate,
                onDismiss = {
                    showAddProductDialog = false
                    productDraftState.clear()
                    productToEdit = null
                },
                onConfirm = { finalPurchase, finalPrice ->
                    val stock = productDraftState.stockRaw.toIntOrNull() ?: 0
                    val minStock = productDraftState.minStockRaw.toIntOrNull() ?: 0
                    val expDate = if (productDraftState.hasExpiry) productDraftState.expiryDateMillis else null

                    if (productToEdit == null) {
                        viewModel.addProduct(
                            name = productDraftState.name,
                            purchasePrice = finalPurchase,
                            price = finalPrice,
                            stock = stock,
                            unit = productDraftState.selectedUnit,
                            expirationDateInMillis = expDate,
                            minStock = minStock,
                            imageUri = productDraftState.imageUri,
                            context = context,
                            onConfigured = { msg -> customToastMessage = msg }
                        )
                    } else {
                        viewModel.editProduct(
                            product = productToEdit!!.copy(
                                name = productDraftState.name,
                                purchasePrice = finalPurchase,
                                price = finalPrice,
                                stock = stock,
                                unit = productDraftState.selectedUnit,
                                expirationDateInMillis = expDate,
                                minStock = minStock,
                                imageUri = productDraftState.imageUri
                            ),
                            context = context,
                            onConfigured = { msg -> customToastMessage = msg }
                        )
                    }
                    showAddProductDialog = false
                    productDraftState.clear()
                    productToEdit = null
                }
            )
        }

        // 3. Diálogo para añadir al carrito desde el inventario
        if (productToAddToCart != null) {
            val currentInCart = shoppingCart.find { it.first.id == productToAddToCart!!.id }?.second ?: 0
            AddToCartDialog(
                product = productToAddToCart!!,
                currentCartQty = currentInCart,
                onDismiss = { productToAddToCart = null },
                onConfirm = { qty ->
                    val existing = shoppingCart.find { it.first.id == productToAddToCart!!.id }
                    if (existing != null) {
                        val idx = shoppingCart.indexOf(existing)
                        shoppingCart[idx] = existing.copy(second = existing.second + qty)
                    } else {
                        shoppingCart.add(Pair(productToAddToCart!!, qty))
                    }
                    productToAddToCart = null
                }
            )
        }

        // 4. Diálogo de Checkout (Cobrar Carrito)
        if (showCheckoutDialog) {
            CheckoutDialog(
                cartItems = shoppingCart,
                products = products,
                totalStoreCash = totalStoreCash,
                totalStoreDigital = totalStoreDigital,
                selectedCountry = viewModel.selectedCountry,
                bcvRate = viewModel.bcvRate,
                onDismiss = { showCheckoutDialog = false },
                onConfirmSale = { items, buyer, summary, netCash, netDigital, pocketDebtAmount ->
                    viewModel.processCartSale(
                        cartItems = items,
                        buyerName = buyer,
                        paymentSummary = summary,
                        netCash = netCash,
                        netDigital = netDigital,
                        context = context,
                        onSold = { msg -> customToastMessage = msg }
                    )
                    if (pocketDebtAmount > 0) {
                        viewModel.addPocketDebt(pocketDebtAmount)
                    }
                    shoppingCart.clear()
                    showCheckoutDialog = false
                },
                onFiarVenta = { buyer, initialCash, initialDigital ->
                    checkoutToFiadorName = buyer
                    checkoutToFiadorCart = shoppingCart.toList()
                    checkoutToFiadorCash = initialCash
                    checkoutToFiadorDigital = initialDigital
                    showCheckoutDialog = false
                    showFiadorDialog = true
                }
            )
        }

        // 5. Diálogos para eliminar stock parcial y total
        if (showDeleteQtyDialog && productToDelete != null) {
            DeleteQuantityDialog(
                product = productToDelete!!,
                initialQty = qtyToDelete,
                onDismiss = { showDeleteQtyDialog = false; productToDelete = null },
                onConfirm = { qty ->
                    qtyToDelete = qty.toString()
                    showDeleteQtyDialog = false
                    showRedWarningDialog = true
                }
            )
        }

        if (showRedWarningDialog && productToDelete != null) {
            val q = qtyToDelete.toIntOrNull() ?: 1
            RedWarningDialog(
                productName = productToDelete!!.name,
                qty = q,
                onDismiss = { showRedWarningDialog = false; productToDelete = null },
                onConfirm = {
                    viewModel.reduceProductStock(productToDelete!!, q, context)
                    showRedWarningDialog = false
                    productToDelete = null
                }
            )
        }

        if (productToFullDelete != null) {
            AlertDialog(
                onDismissRequest = { productToFullDelete = null },
                title = { Text("Eliminar Producto 🗑️", fontWeight = FontWeight.Bold) },
                text = { Text("¿Deseas eliminar '${productToFullDelete!!.name}' completamente del inventario?") },
                containerColor = MaterialTheme.colorScheme.surface,
                confirmButton = {
                    Button(
                        onClick = {
                            val restoredProduct = productToFullDelete!!
                            viewModel.deleteProductEntirely(productToFullDelete!!, context)
                            undoMessage = "Producto '${productToFullDelete!!.name}' eliminado"
                            undoAction = { viewModel.restoreProductStock(restoredProduct, 0, context) }
                            productToFullDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { productToFullDelete = null }) { Text("Cancelar") }
                }
            )
        }

        // 6. Detalle del Producto
        if (productToInfo != null) {
            ProductInfoDialog(
                product = productToInfo!!,
                selectedCountry = viewModel.selectedCountry,
                bcvRate = viewModel.bcvRate,
                onDismiss = { productToInfo = null },
                onDeleteCompletely = {
                    val restoredProduct = productToInfo!!
                    viewModel.deleteProductEntirely(productToInfo!!, context)
                    undoMessage = "Producto '${productToInfo!!.name}' eliminado"
                    undoAction = { viewModel.restoreProductStock(restoredProduct, 0, context) }
                    productToInfo = null
                }
            )
        }

        // 7. Límite de Saldo Crítico
        if (showLimitDialog) {
            LimitDialog(
                currentLimit = viewModel.minBalanceThreshold,
                onDismiss = { showLimitDialog = false },
                onConfirm = { limit ->
                    viewModel.updateMinBalance(limit)
                    showLimitDialog = false
                }
            )
        }

        // 8. Resumen de Totales
        if (showSummaryDialog) {
            SummaryDialog(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = balance,
                transactionCount = personalTransactions.size,
                onDismiss = { showSummaryDialog = false }
            )
        }

        // 9. Borrar Historial de Movimientos Personales
        if (showDeleteHistoryConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteHistoryConfirmDialog = false },
                title = { Text("Borrar Historial Personal ⚠️", fontWeight = FontWeight.Bold) },
                text = { Text("¿Estás seguro de que deseas borrar todas las transacciones personales? Las ventas de la tienda no se borrarán.") },
                containerColor = MaterialTheme.colorScheme.surface,
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deletePersonalTransactions()
                            showDeleteHistoryConfirmDialog = false
                            customToastMessage = "Historial personal borrado 🗑️"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) { Text("Borrar") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteHistoryConfirmDialog = false }) { Text("Cancelar") }
                }
            )
        }

        // 10. Reiniciar Ganancias de Tienda
        if (showResetProfitsDialog) {
            AlertDialog(
                onDismissRequest = { showResetProfitsDialog = false },
                title = { Text("Reiniciar Ganancias 🔄", fontWeight = FontWeight.Bold) },
                text = { Text("¿Deseas reiniciar el contador de ganancias obtenidas a $0?") },
                containerColor = MaterialTheme.colorScheme.surface,
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetAllProfits()
                            showResetProfitsDialog = false
                            customToastMessage = "Ganancias reiniciadas a $0"
                        }
                    ) { Text("Reiniciar") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetProfitsDialog = false }) { Text("Cancelar") }
                }
            )
        }

        // 11. Sonidos y Asistente
        if (showSoundDialog) {
            SoundSettingsDialog(
                personalSoundUri = viewModel.personalSoundUri,
                storeSoundUri = viewModel.storeSoundUri,
                touchSoundUri = viewModel.touchSoundUri,
                isVoiceEnabled = viewModel.isVoiceAssistantEnabled,
                onDismiss = { showSoundDialog = false },
                onSelectPersonal = { uri -> viewModel.updatePersonalSoundPreference(uri, context) },
                onSelectStore = { uri -> viewModel.updateStoreSoundPreference(uri, context) },
                onSelectTouch = { uri -> viewModel.updateTouchSoundPreference(uri, context) },
                onVoiceToggle = { enabled -> viewModel.updateVoicePreference(enabled) }
            )
        }

        // 12. Calendario y Agenda
        if (showCalendarDialog) {
            CalendarDialog(
                currentTab = currentTab,
                reminders = reminders,
                fiadores = fiadores,
                products = products,
                onDismiss = { showCalendarDialog = false },
                onDayClick = { dayMillis, _ ->
                    preselectedDateForEvent = dayMillis
                    showCalendarDialog = false
                    if (currentTab == 0) {
                        showReminderDialog = true
                    } else {
                        showFiadorDialog = true
                    }
                },
                onViewReminders = {
                    showCalendarDialog = false
                    showRemindersListDialog = true
                },
                onViewFiadores = {
                    showCalendarDialog = false
                    showFiadoresListDialog = true
                }
            )
        }

        // 13. Recordatorios / Deudas Personales
        if (showRemindersListDialog) {
            ScheduledRemindersDialog(
                reminders = currentTabReminders,
                onDismiss = { showRemindersListDialog = false },
                onDelete = { reminder -> viewModel.deleteReminder(reminder, context) },
                onEdit = { reminder ->
                    reminderToEdit = reminder
                    showRemindersListDialog = false
                    showReminderDialog = true
                },
                onCreateNew = {
                    reminderToEdit = null
                    preselectedDateForEvent = null
                    showRemindersListDialog = false
                    showReminderDialog = true
                }
            )
        }

        if (showReminderDialog) {
            ReminderDialog(
                initialReminder = reminderToEdit,
                preselectedDate = preselectedDateForEvent,
                onDismiss = {
                    showReminderDialog = false
                    reminderToEdit = null
                    preselectedDateForEvent = null
                },
                onConfirm = { title, amount, dateMillis ->
                    if (reminderToEdit == null) {
                        viewModel.addReminder(
                            title = title,
                            amount = amount,
                            dateInMillis = dateMillis,
                            isStore = (currentTab == 1),
                            context = context,
                            onConfigured = { msg -> customToastMessage = msg }
                        )
                    } else {
                        viewModel.updateExistingReminder(
                            reminder = reminderToEdit!!.copy(title = title, amount = amount, targetDateInMillis = dateMillis),
                            context = context,
                            onConfigured = { msg -> customToastMessage = msg }
                        )
                    }
                    showReminderDialog = false
                    reminderToEdit = null
                    preselectedDateForEvent = null
                }
            )
        }

        // 14. Fiadores / Deudores
        if (showFiadoresListDialog) {
            ScheduledFiadoresDialog(
                fiadores = currentTabFiadores,
                onDismiss = { showFiadoresListDialog = false },
                onDelete = { fiador -> viewModel.deleteFiador(fiador, context) },
                onEdit = { fiador ->
                    fiadorToEdit = fiador
                    showFiadoresListDialog = false
                    showFiadorDialog = true
                },
                onCreateNew = {
                    fiadorToEdit = null
                    checkoutToFiadorName = ""
                    checkoutToFiadorCart = emptyList()
                    checkoutToFiadorCash = 0.0
                    checkoutToFiadorDigital = 0.0
                    preselectedDateForEvent = null
                    showFiadoresListDialog = false
                    showFiadorDialog = true
                }
            )
        }

        // SOLUCIÓN ERRORES 2 Y 3: Inyección de selectedCountry y bcvRate al llamar el FiadorDialog
        if (showFiadorDialog) {
            FiadorDialog(
                initialFiador = fiadorToEdit,
                initialName = checkoutToFiadorName,
                initialCart = checkoutToFiadorCart,
                initialCash = checkoutToFiadorCash,
                initialDigital = checkoutToFiadorDigital,
                products = products,
                selectedCountry = viewModel.selectedCountry, // NUEVO
                bcvRate = viewModel.bcvRate,                 // NUEVO
                preselectedDate = preselectedDateForEvent,
                isStore = (currentTab == 1 || checkoutToFiadorCart.isNotEmpty()),
                onDismiss = {
                    showFiadorDialog = false
                    fiadorToEdit = null
                    checkoutToFiadorName = ""
                    checkoutToFiadorCart = emptyList()
                    checkoutToFiadorCash = 0.0
                    checkoutToFiadorDigital = 0.0
                    preselectedDateForEvent = null
                },
                onConfirmNew = { name, phone, cart, pAmount, dateMillis, initialCash, initialDigital ->
                    viewModel.addFiador(
                        name = name,
                        phone = phone,
                        cartItems = cart,
                        personalDebtAmount = pAmount,
                        dateInMillis = dateMillis,
                        initialCash = initialCash,
                        initialDigital = initialDigital,
                        isStore = (currentTab == 1 || cart.isNotEmpty()),
                        context = context,
                        onConfigured = { msg -> customToastMessage = msg }
                    )
                    shoppingCart.clear()
                    showFiadorDialog = false
                    checkoutToFiadorName = ""
                    checkoutToFiadorCart = emptyList()
                    checkoutToFiadorCash = 0.0
                    checkoutToFiadorDigital = 0.0
                    preselectedDateForEvent = null
                },
                onConfirmEdit = { fiador, dateMillis ->
                    viewModel.updateExistingFiador(
                        fiador = fiador.copy(targetDateInMillis = dateMillis),
                        context = context,
                        onConfigured = { msg -> customToastMessage = msg }
                    )
                    showFiadorDialog = false
                    fiadorToEdit = null
                    preselectedDateForEvent = null
                },
                onConfirmAbono = { fiador, abono, method ->
                    viewModel.registerAbonoFiador(
                        fiador = fiador,
                        abono = abono,
                        method = method,
                        context = context,
                        onResult = { msg -> customToastMessage = msg }
                    )
                    showFiadorDialog = false
                    fiadorToEdit = null
                }
            )
        }

        // 15. Sincronización Nube / Backups
        if (showCloudSyncDialog) {
            AlertDialog(
                onDismissRequest = { showCloudSyncDialog = false },
                title = { Text("Sincronización en la Nube ☁️", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    Column {
                        Text("Guarda o restaura copias de seguridad de tus datos en la nube.", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                showCloudSyncDialog = false
                                backupNameInput = "Respaldo - " + SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
                                showBackupNameDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("📤 Crear Nueva Copia de Seguridad") }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                showCloudSyncDialog = false
                                isLoadingList = true
                                showBackupListDialog = true
                                viewModel.fetchBackupList { list ->
                                    cloudBackupsList = list
                                    isLoadingList = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("📥 Ver y Restaurar Copias") }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showCloudSyncDialog = false }) { Text("Cerrar") } }
            )
        }

        if (showBackupNameDialog) {
            AlertDialog(
                onDismissRequest = { showBackupNameDialog = false },
                title = { Text("Nombre del Respaldo ✍️", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    OutlinedTextField(
                        value = backupNameInput,
                        onValueChange = { backupNameInput = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (backupNameInput.isNotBlank()) {
                            viewModel.manualBackup(backupNameInput.trim()) { msg -> customToastMessage = msg }
                            showBackupNameDialog = false
                        }
                    }) { Text("Guardar") }
                },
                dismissButton = { TextButton(onClick = { showBackupNameDialog = false }) { Text("Cancelar") } }
            )
        }

        if (showBackupListDialog) {
            AlertDialog(
                onDismissRequest = { showBackupListDialog = false },
                title = { Text("Copias Guardadas ☁️", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    if (isLoadingList) {
                        CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))
                    } else if (cloudBackupsList.isNullOrEmpty()) {
                        Text("No tienes copias de seguridad en la nube.", color = Color.Gray)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                            items(cloudBackupsList!!) { record ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(record.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(formatDate(record.timestamp), fontSize = 12.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(onClick = {
                                                viewModel.deleteBackupRecord(record.id) { msg ->
                                                    customToastMessage = msg
                                                    viewModel.fetchBackupList { list -> cloudBackupsList = list }
                                                }
                                            }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                                                Text("Eliminar")
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(onClick = {
                                                viewModel.restoreFromRecord(record) { msg ->
                                                    customToastMessage = msg
                                                    showBackupListDialog = false
                                                }
                                            }) {
                                                Text("Restaurar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showBackupListDialog = false }) { Text("Cerrar") } }
            )
        }

        // 16. Opciones, Planes, Chats y Panel Admin
        if (showOptionsDialog) {
            AlertDialog(
                onDismissRequest = { showOptionsDialog = false }, properties = DialogProperties(dismissOnClickOutside = false),
                title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Opciones ⚙️", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = { showOptionsDialog = false }) { Icon(Icons.Filled.Close, "Cerrar") } } },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onThemeToggle() }.padding(vertical = 12.dp)) {
                            Icon(if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text("Modo Oscuro", modifier = Modifier.weight(1f), fontSize = 16.sp)
                            Switch(checked = isDarkTheme, onCheckedChange = { onThemeToggle() })
                        }
                        Divider(color = Color.Gray.copy(alpha = 0.2f))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showOptionsDialog = false; showSoundDialog = true }.padding(vertical = 16.dp)) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text("Sonidos y Asistente de Voz", fontSize = 16.sp)
                        }
                        Divider(color = Color.Gray.copy(alpha = 0.2f))
                        Spacer(Modifier.height(16.dp))
                        Text("Moneda de la Aplicación", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            FilterChip(selected = viewModel.selectedCountry == "Colombia", onClick = { viewModel.updateCountry("Colombia") }, label = { Text("🇨🇴 Colombia", fontWeight = FontWeight.Bold) })
                            FilterChip(selected = viewModel.selectedCountry == "Venezuela", onClick = { viewModel.updateCountry("Venezuela") }, label = { Text("🇻🇪 Venezuela", fontWeight = FontWeight.Bold) })
                        }
                        if (viewModel.selectedCountry == "Venezuela") {
                            Text("Se usará el Dólar ($) como moneda base para proteger de la inflación. El equivalente exacto en Bolívares (Bs) se generará automáticamente.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center)
                        }
                    }
                },
                confirmButton = {}
            )
        }

        if (showAdminChatList && currentRole == "ADMIN") { AdminChatListDialog(onDismiss = { showAdminChatList = false }, onSelectClient = { email -> chatTargetEmail = email; showAdminChatList = false; showChatDialog = true }) }
        if (showChatDialog) { ChatDialog(currentUserEmail = viewModel.userId, targetClientEmail = chatTargetEmail, isAdmin = (currentRole == "ADMIN"), onDismiss = { showChatDialog = false }) }

        if (showPlansDialog) {
            AlertDialog(
                onDismissRequest = { }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
                title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Planes Disponibles 🚀", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)); IconButton(onClick = { showPlansDialog = false }) { Icon(Icons.Filled.Close, "Cerrar") } } },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    Column {
                        Text("Mejora tu plan comunicándote con el Administrador para desbloquear todo el potencial de la aplicación.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (currentRole == "INVITADO" || currentRole == "INVITADO_PRUEBA") { PlanCardInfo("BÁSICO 🥉", "1, 6 o 12 meses.", listOf("Pestaña Personal", "Saldo crítico y Sonidos", "Gestión de Deudas", "Respaldo manual en nube", "Borrar Historial"), listOf("Control de Tienda y Ventas", "Inventario", "Sincronización Automática")) }
                            if (currentRole == "INVITADO" || currentRole == "INVITADO_PRUEBA" || currentRole == "BÁSICO") { PlanCardInfo("PREMIUM 🥈", "1, 6 o 12 meses.", listOf("Todo lo del Básico", "Acceso total a Tienda", "Inventario y Fechas", "Resumen de Totales", "Sincronización Automática"), listOf("Prioridad de Soporte")) }
                            if (currentRole == "INVITADO" || currentRole == "INVITADO_PRUEBA" || currentRole == "BÁSICO" || currentRole == "PREMIUM") { PlanCardInfo("GOLD 🥇", "1, 6 o 12 meses.", listOf("Uso de toda la aplicación sin ninguna restricción", "Borrado completo", "Prioridad y Soporte total"), emptyList(), isGold = true) }
                        }
                    }
                },
                confirmButton = { }, dismissButton = { }
            )
        }
    }
}

@Composable
fun StoreScreen(
    products: List<Product>,
    transactions: List<Transaction>,
    shoppingCart: List<Pair<Product, Int>>,
    isLockedStore: Boolean,
    totalStoreCash: Double,
    totalStoreDigital: Double,
    selectedCountry: String,
    bcvRate: Double,
    onOpenInventory: () -> Unit,
    onOpenCheckout: () -> Unit,
    onResetProfitsClick: () -> Unit,
    onDeleteVentas: (List<Transaction>) -> Unit,
    showPremiumToast: () -> Unit,
    totalProfit: Double,
    activeFiadores: List<Fiador>,
    activeReminders: List<Reminder>,
    onSettleFiador: (Fiador) -> Unit,
    onEditFiador: (Fiador) -> Unit,
    onSettleReminder: (Reminder) -> Unit,
    onEditReminder: (Reminder) -> Unit,
    onRestoreFiador: (String, Double, Double) -> Unit
) {
    val totalInventoryValue = remember(products) { products.sumOf { it.price * it.stock } }
    val totalInversion = remember(products) { products.sumOf { it.purchasePrice * it.stock } }
    val gananciaFutura = remember(products) { products.sumOf { (it.price - it.purchasePrice) * it.stock } }

    var showVendidosDialog by remember { mutableStateOf(false) }
    var showInversionDialog by remember { mutableStateOf(false) }
    var showGananciaFuturaDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showInversionDialog) {
        AlertDialog(
            onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
            title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Inversión 📦", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = { showInversionDialog = false }) { Icon(Icons.Filled.Close, "Cerrar") } } },
            text = { Text("El dinero total que invertiste en los productos actuales (calculado por su precio de compra original) es:\n\n${formatMoneyMain(totalInversion, selectedCountry)}", fontSize = 16.sp) },
            confirmButton = { }, dismissButton = { }, containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showGananciaFuturaDialog) {
        AlertDialog(
            onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
            title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Ganancias 🚀", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = { showGananciaFuturaDialog = false }) { Icon(Icons.Filled.Close, "Cerrar") } } },
            text = {
                Column {
                    Text("Ganancia Obtenida (Ventas Realizadas):", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                    Text(formatMoneyMain(totalProfit, selectedCountry), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Desglose Actual:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Caja (Efectivo):", fontSize = 13.sp); Text(formatMoneyMain(totalStoreCash, selectedCountry), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)) }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Banco (Digital):", fontSize = 13.sp); Text(formatMoneyMain(totalStoreDigital, selectedCountry), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)) }
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    Text("Ganancia Futura (Inventario Restante):", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                    Text(formatMoneyMain(gananciaFutura, selectedCountry), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Esta es la ganancia neta que obtendrás al vender todo tu stock actual.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                }
            },
            confirmButton = { }, dismissButton = { }, containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        activeReminders.forEachIndexed { index, reminder ->
            AnimatedVisibility(visible = true) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).padding(top = if(index == 0) 16.dp else 0.dp).background(Color(0xFF1976D2), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).clickable { onEditReminder(reminder) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) { Text("📅 Pagar: ${reminder.title}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp); if (reminder.amount > 0) { Text("Monto: ${formatMoneyMain(reminder.amount, selectedCountry)}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp) } }
                    IconButton(onClick = { onSettleReminder(reminder) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Check, contentDescription = "Hecho", tint = Color.White) }
                }
            }
        }

        activeFiadores.forEachIndexed { index, fiador ->
            val remaining = fiador.amount - fiador.paidAmount
            AnimatedVisibility(visible = true) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).padding(top = if(index == 0 && activeReminders.isEmpty()) 16.dp else 0.dp).background(Color(0xFFFBC02D), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).clickable { onEditFiador(fiador) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) { val phoneStr = if(fiador.phone.isNotBlank()) " \uD83D\uDCDE ${fiador.phone}" else ""; Text("💰 Cobrar a ${fiador.name}$phoneStr", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text("Resta: ${formatMoneyMain(remaining, selectedCountry)} de ${formatMoneyMain(fiador.amount, selectedCountry)} - ${fiador.reason}", color = Color.Black.copy(alpha=0.8f), fontSize = 12.sp) }
                    IconButton(onClick = { onSettleFiador(fiador) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Check, contentDescription = "Saldado", tint = Color.Black) }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(top = if(activeFiadores.isNotEmpty() || activeReminders.isNotEmpty()) 0.dp else 0.dp), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { showInversionDialog = true }.padding(4.dp)) {
                        Text("Valor del Inventario", color = Color.Gray, fontSize = 13.sp)
                        Text(text = formatMoneyMain(totalInventoryValue, selectedCountry), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        if (selectedCountry == "Venezuela") {
                            Text(text = formatMoneySec(totalInventoryValue, selectedCountry, bcvRate), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).alpha(if(isLockedStore) 0.5f else 1f).padding(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if(isLockedStore) "👑 Ganancias" else "Ganancias Obtenidas", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.clickable { if (isLockedStore) showPremiumToast() else showGananciaFuturaDialog = true })
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.Refresh, contentDescription = "Reiniciar", tint = Color.Gray, modifier = Modifier.size(20.dp).clickable { if (isLockedStore) showPremiumToast() else onResetProfitsClick() })
                        }
                        Text(text = formatMoneyMain(totalProfit, selectedCountry), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4CAF50), modifier = Modifier.clickable { if (isLockedStore) showPremiumToast() else showGananciaFuturaDialog = true })
                        if (selectedCountry == "Venezuela") {
                            Text(text = formatMoneySec(totalProfit, selectedCountry, bcvRate), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
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
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp).clickable { onOpenCheckout() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2)), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Carrito activo ($totalItems artículos)", fontWeight = FontWeight.Bold, color = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Total: ${formatMoneyMain(totalCart, selectedCountry)}", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                            if (selectedCountry == "Venezuela" && bcvRate > 0) {
                                Text(" ${formatMoneySec(totalCart, selectedCountry, bcvRate).replace("=", "-")}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Cobrar", tint = Color.White)
                }
            }
        }
    }

    if (showVendidosDialog) {
        ProductosVendidosDialog(
            transactions = transactions,
            activeFiadores = activeFiadores,
            onDismiss = { showVendidosDialog = false },
            onDeleteVentas = onDeleteVentas,
            onRestoreFiador = onRestoreFiador
        )
    }
}

@Composable
fun InventoryScreen(
    products: List<Product>,
    shoppingCart: List<Pair<Product, Int>>,
    selectedCountry: String,
    bcvRate: Double,
    onBack: () -> Unit,
    onAddProductClick: () -> Unit,
    onAddToCartClick: (Product) -> Unit,
    onOpenCheckout: () -> Unit,
    onEditClick: (Product) -> Unit,
    onDeleteClick: (Product) -> Unit,
    onLongDeleteClick: (Product) -> Unit,
    onInfoClick: (Product) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("A-Z") }
    var expandedImageUri by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focusManager.clearFocus() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { focusManager.clearFocus(); onBack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás", tint = MaterialTheme.colorScheme.onPrimary) }
                Text("Inventario 📦", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Buscar en el inventario...") }, leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            ScrollableTabRow(selectedTabIndex = listOf("A-Z", "Poco Stock", "Vencimiento", "Precio", "Recientes").indexOf(sortBy), modifier = Modifier.fillMaxWidth(), edgePadding = 16.dp, containerColor = Color.Transparent, divider = {}, indicator = {}) {
                listOf("A-Z", "Poco Stock", "Vencimiento", "Precio", "Recientes").forEach { tab ->
                    FilterChip(selected = sortBy == tab, onClick = { sortBy = tab; focusManager.clearFocus() }, label = { Text(tab) }, modifier = Modifier.padding(end = 8.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(visible = shoppingCart.isNotEmpty()) {
                val totalCart = shoppingCart.sumOf { it.first.price * it.second }; val totalItems = shoppingCart.sumOf { it.second }
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp).clickable { focusManager.clearFocus(); onOpenCheckout() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2)), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Carrito activo ($totalItems artículos)", fontWeight = FontWeight.Bold, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Total: ${formatMoneyMain(totalCart, selectedCountry)}", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                                if (selectedCountry == "Venezuela" && bcvRate > 0) {
                                    Text(" ${formatMoneySec(totalCart, selectedCountry, bcvRate).replace("=", "-")}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Cobrar", tint = Color.White)
                    }
                }
            }

            val sortedProducts = remember(products, searchQuery, sortBy) {
                val filtered = products.filter { it.name.contains(searchQuery, ignoreCase = true) }
                when (sortBy) {
                    "A-Z" -> filtered.sortedBy { it.name.lowercase(Locale.getDefault()) }
                    "Precio" -> filtered.sortedByDescending { it.price }
                    "Vencimiento" -> filtered.sortedWith(compareBy<Product> { it.expirationDateInMillis == null }.thenBy { it.expirationDateInMillis })
                    "Recientes" -> filtered.sortedByDescending { it.entryDateInMillis }
                    "Poco Stock" -> filtered.filter { it.minStock > 0 && it.stock <= it.minStock }.sortedBy { it.stock }
                    else -> filtered
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                if (sortedProducts.isEmpty()) { item { Text("No se encontraron productos.", color = Color.Gray, modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center) } }
                items(sortedProducts, key = { it.id }) { product ->
                    ProductItem(
                        product = product,
                        selectedCountry = selectedCountry,
                        bcvRate = bcvRate,
                        onAddToCart = { focusManager.clearFocus(); onAddToCartClick(product) },
                        onEdit = { focusManager.clearFocus(); onEditClick(product) },
                        onDelete = { focusManager.clearFocus(); onDeleteClick(product) },
                        onLongDelete = { focusManager.clearFocus(); onLongDeleteClick(product) },
                        onInfo = { focusManager.clearFocus(); onInfoClick(product) },
                        onImageClick = { focusManager.clearFocus(); expandedImageUri = product.imageUri }
                    )
                }
            }
        }
        FloatingActionButton(onClick = { focusManager.clearFocus(); onAddProductClick() }, containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 16.dp)) { Icon(Icons.Filled.Add, contentDescription = "Agregar Producto") }

        if (expandedImageUri != null) { ExpandedImageDialog(imageUri = expandedImageUri!!, onDismiss = { expandedImageUri = null }) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductItem(
    product: Product,
    selectedCountry: String,
    bcvRate: Double,
    onAddToCart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLongDelete: () -> Unit,
    onInfo: () -> Unit,
    onImageClick: () -> Unit
) {
    val context = LocalContext.current
    val isOutOfStock = product.stock <= 0; val isLowStock = product.minStock > 0 && product.stock <= product.minStock && !isOutOfStock
    val isExpired = product.expirationDateInMillis != null && product.expirationDateInMillis < System.currentTimeMillis()
    val unitColor = when (product.unit) { "Kg" -> Color(0xFFFF9800); "L" -> Color(0xFF03A9F4); else -> Color.Gray }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).combinedClickable(onClick = onInfo, onLongClick = onLongDelete), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(unitColor))
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {

                Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.2f)).clickable { if (product.imageUri != null) onImageClick() }, contentAlignment = Alignment.Center) {
                    if (product.imageUri != null) {
                        val bitmap = remember(product.imageUri) { try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, Uri.parse(product.imageUri))) } else { @Suppress("DEPRECATION") android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.parse(product.imageUri)) } } catch (e: Exception) { null } }
                        if (bitmap != null) { Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Imagen de ${product.name}", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } else { Icon(Icons.Filled.ImageNotSupported, contentDescription = null, tint = Color.Gray) }
                    } else { Icon(Icons.Filled.Inventory, contentDescription = null, tint = Color.Gray) }
                }
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(text = product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)); if (isExpired) { Spacer(modifier = Modifier.width(8.dp)); Surface(color = Color(0xFFD32F2F), shape = RoundedCornerShape(4.dp)) { Text("⚠️ VENCIDO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) } }; if (isLowStock) { Spacer(modifier = Modifier.width(8.dp)); Surface(color = Color(0xFFE65100), shape = RoundedCornerShape(4.dp)) { Text("⚠️ POCO STOCK", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) } } }

                    val unitName = when(product.unit) { "Kg" -> "Kilo"; "L" -> "Litro"; else -> "Unidad" }
                    Text(text = "Precio por $unitName: ${formatMoneyMain(product.price, selectedCountry)} ${formatMoneySec(product.price, selectedCountry, bcvRate)}", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) { Text(text = "Stock: ${product.stock} ${product.unit}", color = if (isOutOfStock) Color.Red else Color.Gray, fontSize = 14.sp, fontWeight = if (isOutOfStock) FontWeight.Bold else FontWeight.Normal); if (product.expirationDateInMillis != null) { Spacer(modifier = Modifier.width(8.dp)); Text(text = "Vence: ${formatDateOnly(product.expirationDateInMillis)}", color = if (isExpired) Color.Red else Color.Gray, fontSize = 12.sp) } }
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
fun ExpandedImageDialog(imageUri: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = { }, // Bloqueado al botón de retroceso/fuera
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), // Quitamos el clickable de fondo
            contentAlignment = Alignment.Center
        ) {
            var bitmap by remember(imageUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
            LaunchedEffect(imageUri) {
                val loadedBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, Uri.parse(imageUri)))
                        } else {
                            @Suppress("DEPRECATION")
                            android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.parse(imageUri))
                        }
                    } catch (e: Exception) { null }
                }
                bitmap = loadedBitmap
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Imagen Expandida",
                    modifier = Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                CircularProgressIndicator(color = Color.White)
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(32.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
    }
}

@Composable
fun CustomDatePickerDialog(initialDateMillis: Long?, onDismiss: () -> Unit, onDateSelected: (Long) -> Unit) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); if (initialDateMillis != null) { timeInMillis = initialDateMillis; set(Calendar.DAY_OF_MONTH, 1) } }) }
    var selectedDate by remember { mutableStateOf<Calendar?>(initialDateMillis?.let { Calendar.getInstance().apply { timeInMillis = it } }) }
    val formatMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false), modifier = Modifier.fillMaxWidth().padding(16.dp), containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Seleccionar Fecha 🗓️", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") }
            }
        },
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
        dismissButton = { }
    )
}

@Composable
fun AddToCartDialog(product: Product, currentCartQty: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var qtyRaw by remember { mutableStateOf("") }
    val maxAvailable = product.stock - currentCartQty
    val focusRequester = remember { FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { delay(200); focusRequester.requestFocus(); keyboardController?.show() }

    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Añadir al Carrito 🛒", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Disponible para añadir: $maxAvailable ${product.unit}", color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = qtyRaw, onValueChange = { n -> val d = n.filter { it.isDigit() }; qtyRaw = d }, label = { Text("Cantidad") }, placeholder = { Text("0", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.Gray.copy(alpha = 0.5f)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.width(150.dp).padding(vertical = 8.dp).focusRequester(focusRequester), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                )
            }
        },
        confirmButton = { Button(onClick = { val q = qtyRaw.toIntOrNull() ?: 0; if (q > 0 && q <= maxAvailable) { onConfirm(q) } }) { Text("Añadir") } },
        dismissButton = { }
    )
}

@Composable
fun PaymentInputRow(name: String, amountRaw: String, sym: String, visualTrans: VisualTransformation, selectedCountry: String, onAmountChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(name, modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = amountRaw,
            onValueChange = { onAmountChange(if (selectedCountry == "Venezuela") cleanDecimalInput(it) else cleanAmountInput(it)) },
            modifier = Modifier.weight(1.5f).height(54.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            visualTransformation = visualTrans,
            leadingIcon = { Text(sym, color = Color.Gray, modifier = Modifier.padding(start=8.dp)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutDialog(cartItems: MutableList<Pair<Product, Int>>, products: List<Product>, totalStoreCash: Double, totalStoreDigital: Double, selectedCountry: String, bcvRate: Double, onDismiss: () -> Unit, onConfirmSale: (List<Pair<Product, Int>>, String, String, Double, Double, Double) -> Unit, onFiarVenta: (String, Double, Double) -> Unit) {
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

    var checkoutCurrency by remember { mutableStateOf("USD") }
    LaunchedEffect(Unit) { if (selectedCountry == "Venezuela") checkoutCurrency = "BS" }
    val isBsInput = selectedCountry == "Venezuela" && checkoutCurrency == "BS"
    val inputMultiplier = if (isBsInput && bcvRate > 0) 1 / bcvRate else 1.0
    val sym = if (isBsInput) "Bs" else "$"
    val visualTrans = if (selectedCountry == "Venezuela") VisualTransformation.None else AmountVisualTransformation()

    if (itemToEdit != null) {
        var editQtyRaw by remember { mutableStateOf(itemToEdit!!.second.second.toString()) }
        AlertDialog(
            onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
            title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Editar cantidad", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center); IconButton(onClick = { itemToEdit = null }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { Text(itemToEdit!!.second.first.name, fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = editQtyRaw, onValueChange = { editQtyRaw = it.filter { c -> c.isDigit() } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), label = { Text("Nueva Cantidad") }, singleLine = true, modifier = Modifier.width(150.dp), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.Bold)) } },
            confirmButton = { Button(onClick = { val newQty = editQtyRaw.toIntOrNull() ?: 0; if (newQty > 0 && newQty <= itemToEdit!!.second.first.stock) { cartItems[itemToEdit!!.first] = itemToEdit!!.second.first to newQty; itemToEdit = null } else if (newQty == 0) { cartItems.removeAt(itemToEdit!!.first); itemToEdit = null } }) { Text("Guardar") } },
            dismissButton = { }
        )
    }

    if (productToSelectQty != null) {
        var qtyRaw by remember { mutableStateOf("") }
        val p = productToSelectQty!!
        val currentInCart = cartItems.find { it.first.id == p.id }?.second ?: 0
        val maxAvailable = p.stock - currentInCart
        val focusRequester = remember { FocusRequester() }
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

        LaunchedEffect(Unit) { delay(200); focusRequester.requestFocus(); keyboardController?.show() }

        AlertDialog(
            onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
            title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Añadir ${p.name}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = { productToSelectQty = null }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { Text("Disponible: $maxAvailable ${p.unit}", color = Color.Gray); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = qtyRaw, onValueChange = { qtyRaw = it.filter { c -> c.isDigit() } }, label = { Text("Cantidad") }, placeholder = { Text("0", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.Gray.copy(alpha = 0.5f)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.width(150.dp).padding(vertical = 8.dp).focusRequester(focusRequester), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.Bold)) } },
            confirmButton = { Button(onClick = { val q = qtyRaw.toIntOrNull() ?: 0; if (q > 0 && q <= maxAvailable) { val existing = cartItems.find { it.first.id == p.id }; if (existing != null) { val idx = cartItems.indexOf(existing); cartItems[idx] = existing.copy(second = existing.second + q) } else { cartItems.add(Pair(p, q)) }; productToSelectQty = null; showProductSearch = false; searchQuery = "" } }) { Text("Añadir") } },
            dismissButton = { }
        )
    }

    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (showProductSearch) "Buscar Producto 🔍" else if (step == 1) "Resumen de Venta 🛒" else "Opciones de Pago 💳", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (showProductSearch) {
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Nombre del producto...") }, leadingIcon = { Icon(Icons.Filled.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    val filteredProducts = products.filter { it.name.contains(searchQuery, ignoreCase = true) && it.stock > 0 }
                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                        items(filteredProducts) { p ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { productToSelectQty = p }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text("Stock: ${p.stock} ${p.unit}", color = Color.Gray, fontSize = 12.sp) }; Text(formatCOP(p.price), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                            Divider(color = Color.Gray.copy(alpha = 0.2f))
                        }
                    }
                } else if (step == 1) {
                    if (cartItems.isEmpty()) { Text("El carrito está vacío.", modifier = Modifier.padding(16.dp)) } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                            itemsIndexed(cartItems) { index, item ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text("${item.second}x ${item.first.name}", modifier = Modifier.weight(1f), fontSize = 14.sp, maxLines=1, overflow = TextOverflow.Ellipsis)
                                    // MODIFICADO: Muestra siempre los decimales respetando el país
                                    Text(formatMoneyMain(item.first.price * item.second, selectedCountry), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    IconButton(onClick = { itemToEdit = Pair(index, item) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Edit, "Editar", tint = Color.Blue, modifier = Modifier.size(16.dp)) }
                                    IconButton(onClick = { cartItems.removeAt(index) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Delete, "Eliminar", tint = Color.Red, modifier = Modifier.size(16.dp)) }
                                }
                            }
                        }
                        TextButton(onClick = { showProductSearch = true }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp)) { Text("+ Agregar producto nuevo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Total a cobrar:", fontSize = 12.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatMoneyMain(totalCOP, selectedCountry), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                            if (selectedCountry == "Venezuela" && bcvRate > 0) {
                                Text(" ${formatMoneySec(totalCOP, selectedCountry, bcvRate)}", fontSize = 16.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(value = buyerName, onValueChange = { buyerName = it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() } }, label = { Text("Nombre del Cliente (Opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth().background(Color.DarkGray.copy(alpha=0.2f), RoundedCornerShape(8.dp)).padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Caja (Efectivo)", fontSize=10.sp, color=Color.Gray); Text(formatCOP(totalStoreCash), fontSize=13.sp, fontWeight=FontWeight.Bold) }; Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Banco (Digital)", fontSize=10.sp, color=Color.Gray); Text(formatCOP(totalStoreDigital), fontSize=13.sp, fontWeight=FontWeight.Bold) } }
                    Spacer(modifier = Modifier.height(12.dp)); Text("Total de la compra:", fontSize = 13.sp);
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatMoneyMain(totalCOP, selectedCountry), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        if (selectedCountry == "Venezuela" && bcvRate > 0) {
                            Text(" ${formatMoneySec(totalCOP, selectedCountry, bcvRate)}", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedCountry == "Venezuela") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            FilterChip(selected = checkoutCurrency == "USD", onClick = { checkoutCurrency = "USD" }, label = { Text("Ingresar en $") })
                            FilterChip(selected = checkoutCurrency == "BS", onClick = { checkoutCurrency = "BS" }, label = { Text("Ingresar en Bs") })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isDivided = !isDivided; pocketChange = false }) { Switch(checked = isDivided, onCheckedChange = { isDivided = it; pocketChange = false }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, uncheckedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary, uncheckedTrackColor = Color.Gray, uncheckedBorderColor = Color.Transparent)); Spacer(modifier = Modifier.width(8.dp)); Text(if (isDivided) "Pago Dividido Múltiple" else "Pago Único", fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!isDivided) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) { FilterChip(selected = simpleMethod == "Efectivo", onClick = { simpleMethod = "Efectivo" }, label = { Text("Efectivo") }); FilterChip(selected = simpleMethod == "Digital", onClick = { simpleMethod = "Digital" }, label = { Text("Digital") }) }
                        PaymentInputRow("Monto Recibido", simpleReceivedRaw, sym, visualTrans, selectedCountry) { simpleReceivedRaw = it }

                        val rec = (simpleReceivedRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier
                        val change = rec - totalCOP

                        if (change > 0) {
                            Text("Vuelto a devolver: ${formatMoneyMain(change, selectedCountry)} ${formatMoneySec(change, selectedCountry, bcvRate)}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { pocketChange = !pocketChange }) { Checkbox(checked = pocketChange, onCheckedChange = { pocketChange = it }); Text("Sacar vuelto de mi bolsillo", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                            if (pocketChange) { Text("El vuelto se dará de tu bolsillo personal. La ganancia ingresará completa a la tienda y la tienda te deberá el vuelto.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp)) } else { val hasEnoughFunds = if (simpleMethod == "Efectivo") change <= totalStoreCash else change <= totalStoreDigital; if (!hasEnoughFunds) { Text("⚠️ Fondos insuficientes en ${if(simpleMethod == "Efectivo") "Caja" else "Banco"}. La caja quedará en negativo.", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp) } else { Text("El vuelto saldrá de: $simpleMethod", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontSize = 12.sp) } }
                        } else if (change < 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { if (rec > 0) { Text("Falta: ${formatMoneyMain(abs(change), selectedCountry)} ${formatMoneySec(abs(change), selectedCountry, bcvRate)}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp) } else { Text("Cobro pendiente", color = Color.Gray, fontSize = 14.sp) }; Spacer(modifier = Modifier.height(12.dp)); Button(onClick = { val cash = if (simpleMethod == "Efectivo") rec else 0.0; val digital = if (simpleMethod == "Digital") rec else 0.0; onFiarVenta(buyerName, cash, digital) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D), contentColor = Color.Black)) { Text(if (rec > 0) "Fiar el restante 🗓️" else "Fiar esta venta 🗓️", fontWeight = FontWeight.Bold) } }
                        } else if (change == 0.0 && rec > 0) { Text("Pago exacto ✅", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold) }
                    } else {
                        PaymentInputRow("Efectivo Recibido", cashRaw, sym, visualTrans, selectedCountry) { cashRaw = it }
                        PaymentInputRow("Digital Recibido", digitalRaw, sym, visualTrans, selectedCountry) { digitalRaw = it }
                        val cV = (cashRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier
                        val qV = (digitalRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier
                        val receivedCOP = cV + qV
                        val changeCOP = receivedCOP - totalCOP

                        if (changeCOP > 0) {
                            Text("Vuelto a devolver: ${formatMoneyMain(changeCOP, selectedCountry)} ${formatMoneySec(changeCOP, selectedCountry, bcvRate)}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { pocketChange = !pocketChange }) { Checkbox(checked = pocketChange, onCheckedChange = { pocketChange = it }); Text("Sacar vuelto de mi bolsillo", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                            if (pocketChange) { Text("El vuelto se dará de tu bolsillo. La ganancia ingresará intacta y la tienda te deberá el vuelto.", fontSize = 12.sp, color = Color.Gray) } else { Text("¿De dónde darás el vuelto?", fontSize = 12.sp, color = Color.Gray); PaymentInputRow("Vuelto Efectivo", cashChangeRaw, sym, visualTrans, selectedCountry) { cashChangeRaw = it }; PaymentInputRow("Vuelto Digital", digitalChangeRaw, sym, visualTrans, selectedCountry) { digitalChangeRaw = it }; val cc = (cashChangeRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; val dc = (digitalChangeRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; if (cc + dc != changeCOP) { Text("La suma del vuelto no cuadra con ${formatMoneyMain(changeCOP, selectedCountry)}", color = Color.Red, fontSize = 11.sp) } else if (cc > totalStoreCash) { Text("⚠️ Caja quedará en negativo al dar el vuelto.", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold) } else if (dc > totalStoreDigital) { Text("⚠️ Banco quedará en negativo al dar el vuelto.", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold) } else { Text("Vuelto distribuido correctamente ✅", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
                        } else if (changeCOP < 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { if (receivedCOP > 0) { Text("Falta dinero: ${formatMoneyMain(abs(changeCOP), selectedCountry)} ${formatMoneySec(abs(changeCOP), selectedCountry, bcvRate)}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp) } else { Text("Cobro pendiente", color = Color.Gray, fontSize = 14.sp) }; Spacer(modifier = Modifier.height(12.dp)); Button(onClick = { onFiarVenta(buyerName, cV, qV) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D), contentColor = Color.Black)) { Text(if (receivedCOP > 0) "Fiar el restante 🗓️" else "Fiar esta venta 🗓️", fontWeight = FontWeight.Bold) } }
                        } else if (changeCOP == 0.0 && receivedCOP > 0) { Text("Pago completo y exacto ✅", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                }
            }
        },
        confirmButton = {
            if (step == 1 && !showProductSearch) {
                Button(onClick = { step = 2 }, enabled = cartItems.isNotEmpty()) { Text("Siguiente") }
            } else if (step == 2) {
                var isEnabled = false
                if (!isDivided) { val rec = (simpleReceivedRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; if (rec >= totalCOP) { isEnabled = true } } else { val cV = (cashRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; val dV = (digitalRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; val changeCOP = (cV + dV) - totalCOP; if (changeCOP == 0.0) isEnabled = true; if (changeCOP > 0.0) { if (pocketChange) { isEnabled = true } else { val cc = (cashChangeRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; val dc = (digitalChangeRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; if (abs(cc + dc - changeCOP) < 0.01) isEnabled = true } } }
                Button(onClick = { var netC = 0.0; var netD = 0.0; var summary = ""; var pocketDebtAmount = 0.0; if (!isDivided) { val rec = (simpleReceivedRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; val change = rec - totalCOP; if (simpleMethod == "Efectivo") { netC = totalCOP; summary = "Pago Efectivo: ${formatMoneyMain(rec, selectedCountry)}" + if(change>0) " | Vuelto: ${formatMoneyMain(change, selectedCountry)}" + if(pocketChange) " (De bolsillo)" else "" else "" } else { netD = totalCOP; summary = "Pago Digital: ${formatMoneyMain(rec, selectedCountry)}" + if(change>0) " | Vuelto: ${formatMoneyMain(change, selectedCountry)}" + if(pocketChange) " (De bolsillo)" else "" else "" }; if (pocketChange && change > 0) { pocketDebtAmount = change } } else { val cV = (cashRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; val dV = (digitalRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; val changeCOP = (cV + dV) - totalCOP; if (pocketChange) { netC = cV; netD = dV; summary = "Efectivo recibido: ${formatMoneyMain(cV, selectedCountry)} | Digital recibido: ${formatMoneyMain(dV, selectedCountry)}"; if (changeCOP > 0) { summary += "\nVuelto: ${formatMoneyMain(changeCOP, selectedCountry)} (De bolsillo)"; pocketDebtAmount = changeCOP } } else { val cc = (cashChangeRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; val dc = (digitalChangeRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; netC = cV - cc; netD = dV - dc; summary = "Efectivo recibido: ${formatMoneyMain(cV, selectedCountry)} | Digital recibido: ${formatMoneyMain(dV, selectedCountry)}"; if (cc > 0 || dc > 0) { summary += "\nVuelto Efectivo: ${formatMoneyMain(cc, selectedCountry)} | Vuelto Digital: ${formatMoneyMain(dc, selectedCountry)}" } } }; onConfirmSale(cartItems.toList(), buyerName.trim(), summary, netC, netD, pocketDebtAmount) }, enabled = isEnabled) { Text("Confirmar Venta") }
            }
        },
        dismissButton = {
            if (showProductSearch) { TextButton(onClick = { showProductSearch = false }) { Text("Volver al carrito") } } else if (step == 2) { TextButton(onClick = { step = 1 }) { Text("Atrás") } }
        }
    )
}

@Composable
fun AddTransactionDialog(onDismiss: () -> Unit, onConfirm: (String, Double, Boolean, String, String) -> Unit) {
    var isIncome by remember { mutableStateOf(true) }
    var descIncome by remember { mutableStateOf("") }
    var amountIncome by remember { mutableStateOf("") }
    var descExpense by remember { mutableStateOf("") }
    var amountExpense by remember { mutableStateOf("") }
    var noteExpense by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Efectivo") }

    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Nuevo Movimiento ✍️", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(4.dp)) {
                    Button(onClick = { isIncome = true }, colors = ButtonDefaults.buttonColors(containerColor = if (isIncome) Color(0xFF4CAF50) else Color.Transparent, contentColor = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurface), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f), elevation = null) { Text("Ingresos 👝", fontWeight = FontWeight.Bold) }
                    Button(onClick = { isIncome = false }, colors = ButtonDefaults.buttonColors(containerColor = if (!isIncome) Color(0xFFF44336) else Color.Transparent, contentColor = if (!isIncome) Color.White else MaterialTheme.colorScheme.onSurface), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f), elevation = null) { Text("Gastos 👛", fontWeight = FontWeight.Bold) }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("Método de Pago", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    FilterChip(selected = method == "Efectivo", onClick = { method = "Efectivo" }, label = { Text("Efectivo") })
                    FilterChip(selected = method == "Digital", onClick = { method = "Digital" }, label = { Text("Digital") })
                }
                Spacer(modifier = Modifier.height(8.dp))

                Crossfade(targetState = isIncome, label = "") { showIncome ->
                    Column {
                        if (showIncome) {
                            OutlinedTextField(value = descIncome, onValueChange = { input -> descIncome = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = amountIncome, onValueChange = { amountIncome = cleanAmountInput(it) }, label = { Text("Monto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = AmountVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                        } else {
                            OutlinedTextField(value = descExpense, onValueChange = { input -> descExpense = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = amountExpense, onValueChange = { amountExpense = cleanAmountInput(it) }, label = { Text("Monto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = AmountVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = noteExpense, onValueChange = { input -> noteExpense = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }, label = { Text("Nota (Opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (isIncome) { val a = amountIncome.toDoubleOrNull(); if (descIncome.isNotBlank() && a != null) { onConfirm(descIncome.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, a, true, "", method) } } else { val a = amountExpense.toDoubleOrNull(); if (descExpense.isNotBlank() && a != null) { onConfirm(descExpense.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, a, false, noteExpense.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, method) } }
            }) { Text("Guardar ✔️") }
        },
        dismissButton = { }
    )
}

@Composable
fun LimitDialog(currentLimit: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    val initial = if(currentLimit > 0) currentLimit.toLong().toString() else ""; var amountRaw by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Saldo Crítico 🔔", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
        containerColor = MaterialTheme.colorScheme.surface,
        text = { Column { Text("Te avisaremos si tu saldo baja de esta cantidad (Déjalo en 0 para apagar):", fontSize = 14.sp); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = amountRaw, onValueChange = { amountRaw = cleanAmountInput(it) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), leadingIcon = { Icon(Icons.Filled.AttachMoney, null) }, singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = AmountVisualTransformation()) } },
        confirmButton = { Button(onClick = { onConfirm(amountRaw.toDoubleOrNull() ?: 0.0) }) { Text("Guardar") } }, dismissButton = { }
    )
}

@Composable
fun SummaryDialog(totalIncome: Double, totalExpense: Double, balance: Double, transactionCount: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Resumen de Totales 📊", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
        containerColor = MaterialTheme.colorScheme.surface,
        text = { Column(modifier = Modifier.fillMaxWidth()) { Text("Aquí tienes el balance histórico general de todos tus movimientos personales:", fontSize = 14.sp, color = Color.Gray); Spacer(modifier = Modifier.height(16.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total Ingresos:", fontWeight = FontWeight.Bold); Column(horizontalAlignment = Alignment.End) { Text(formatCOP(totalIncome), color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold) } }; Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total Gastado:", fontWeight = FontWeight.Bold); Column(horizontalAlignment = Alignment.End) { Text(formatCOP(totalExpense), color = Color(0xFFF44336), fontWeight = FontWeight.Bold) } }; Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Balance Total:", fontWeight = FontWeight.Bold); Column(horizontalAlignment = Alignment.End) { Text(formatCOP(balance), color = if (balance >= 0) Color(0xFF4CAF50) else Color(0xFFE53935), fontWeight = FontWeight.Bold) } }; Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Cant. Movimientos:", fontWeight = FontWeight.Bold); Text("$transactionCount", fontWeight = FontWeight.Bold) } } },
        confirmButton = { }, dismissButton = { }
    )
}

@Composable
fun SoundSettingsDialog(personalSoundUri: String?, storeSoundUri: String?, touchSoundUri: String?, isVoiceEnabled: Boolean, onDismiss: () -> Unit, onSelectPersonal: (String) -> Unit, onSelectStore: (String) -> Unit, onSelectTouch: (String) -> Unit, onVoiceToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION).toFloat() }
    var currentVolume by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION).toFloat()) }
    var targetForPicker by remember { mutableStateOf("") }
    var selectedSoundTab by remember { mutableStateOf(0) }
    val tabs = listOf("General ⚙️", "Personal 👤", "Tienda 🏪")

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) { try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}; when (targetForPicker) { "PERSONAL" -> onSelectPersonal(uri.toString()); "STORE" -> onSelectStore(uri.toString()); "TOUCH" -> onSelectTouch(uri.toString()) } } }
    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == android.app.Activity.RESULT_OK) { val uri: Uri? = result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI); if (uri != null) { when (targetForPicker) { "PERSONAL" -> onSelectPersonal(uri.toString()); "STORE" -> onSelectStore(uri.toString()); "TOUCH" -> onSelectTouch(uri.toString()) } } } }

    fun openRingtonePicker(target: String) { targetForPicker = target; val intent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply { putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_NOTIFICATION); putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true); putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true) }; ringtonePicker.launch(intent) }
    fun openAudioPicker(target: String) { targetForPicker = target; audioPicker.launch("audio/*") }

    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Configuración de Sonido 🎵", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = selectedSoundTab, containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary) { tabs.forEachIndexed { index, title -> Tab(selected = selectedSoundTab == index, onClick = { selectedSoundTab = index }, text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1) }) } }
                Spacer(modifier = Modifier.height(16.dp))
                Crossfade(targetState = selectedSoundTab, label = "SoundTabs") { tab ->
                    Column(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        when (tab) {
                            0 -> {
                                Text("Ajusta el volumen (escucharás un tono al soltar).", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(16.dp))
                                Text("Volumen del Sistema", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Text("🔉", fontSize = 20.sp); Slider(value = currentVolume, onValueChange = { currentVolume = it; audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, it.toInt(), 0) }, onValueChangeFinished = { AppSounds.play(context, "") }, valueRange = 0f..maxVolume, modifier = Modifier.weight(1f).padding(horizontal = 8.dp)); Text("🔊", fontSize = 20.sp) }
                                Text("${(currentVolume / maxVolume * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(24.dp)); Divider(color = Color.Gray.copy(alpha = 0.2f)); Spacer(modifier = Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onVoiceToggle(!isVoiceEnabled) }.padding(vertical = 8.dp)) { Switch(checked = isVoiceEnabled, onCheckedChange = onVoiceToggle); Spacer(modifier = Modifier.width(12.dp)); Column { Text("Asistente de Voz 🎙️", fontWeight = FontWeight.Bold); Text("Leer notificaciones en voz alta", fontSize = 12.sp, color = Color.Gray) } }
                                Spacer(modifier = Modifier.height(16.dp)); Divider(color = Color.Gray.copy(alpha = 0.2f)); Spacer(modifier = Modifier.height(16.dp))
                                Text("👆 Sonido de Toques (Acciones en App)", fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { OutlinedButton(onClick = { onSelectTouch("") }, modifier = Modifier.weight(1f).padding(end=4.dp), contentPadding = PaddingValues(0.dp)) { Text("Por Defecto", fontSize = 11.sp) }; Button(onClick = { openRingtonePicker("TOUCH") }, modifier = Modifier.weight(1f).padding(horizontal=2.dp), contentPadding = PaddingValues(0.dp)) { Text("Tono", fontSize = 11.sp) }; Button(onClick = { openAudioPicker("TOUCH") }, modifier = Modifier.weight(1f).padding(start=4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary), contentPadding = PaddingValues(0.dp)) { Text("Audio", fontSize = 11.sp) } }
                            }
                            1 -> {
                                Text("Notificaciones de Agenda Personal", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(16.dp)); Text("🔵 Recordatorios de Deudas", fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { OutlinedButton(onClick = { onSelectPersonal("") }, modifier = Modifier.weight(1f).padding(end=4.dp), contentPadding = PaddingValues(0.dp)) { Text("Por Defecto", fontSize = 11.sp) }; Button(onClick = { openRingtonePicker("PERSONAL") }, modifier = Modifier.weight(1f).padding(horizontal=2.dp), contentPadding = PaddingValues(0.dp)) { Text("Tono", fontSize = 11.sp) }; Button(onClick = { openAudioPicker("PERSONAL") }, modifier = Modifier.weight(1f).padding(start=4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary), contentPadding = PaddingValues(0.dp)) { Text("Audio", fontSize = 11.sp) } }
                            }
                            2 -> {
                                Text("Notificaciones de la Tienda", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(16.dp)); Text("🏪 Cobros, Fiadores y Stock", fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { OutlinedButton(onClick = { onSelectStore("") }, modifier = Modifier.weight(1f).padding(end=4.dp), contentPadding = PaddingValues(0.dp)) { Text("Por Defecto", fontSize = 11.sp) }; Button(onClick = { openRingtonePicker("STORE") }, modifier = Modifier.weight(1f).padding(horizontal=2.dp), contentPadding = PaddingValues(0.dp)) { Text("Tono", fontSize = 11.sp) }; Button(onClick = { openAudioPicker("STORE") }, modifier = Modifier.weight(1f).padding(start=4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary), contentPadding = PaddingValues(0.dp)) { Text("Audio", fontSize = 11.sp) } }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { }, dismissButton = { }
    )
}

@Composable
fun CalendarDialog(currentTab: Int, reminders: List<Reminder>, fiadores: List<Fiador>, products: List<Product>, onDismiss: () -> Unit, onDayClick: (Long, Boolean) -> Unit, onViewReminders: () -> Unit, onViewFiadores: () -> Unit) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    val formatMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val titleText = if (currentTab == 0) "Agenda Personal \uD83D\uDDD3\uFE0F" else "Agenda de Tienda \uD83D\uDDD3\uFE0F"

    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false), modifier = Modifier.fillMaxWidth().padding(16.dp), containerColor = MaterialTheme.colorScheme.surface,
        title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(titleText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { val newCal = currentMonth.clone() as Calendar; newCal.add(Calendar.MONTH, -1); newCal.set(Calendar.DAY_OF_MONTH, 1); currentMonth = newCal }) { Icon(Icons.Filled.ChevronLeft, "Anterior") }; Text(text = formatMonth.format(currentMonth.time).replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, fontSize = 16.sp); IconButton(onClick = { val newCal = currentMonth.clone() as Calendar; newCal.add(Calendar.MONTH, 1); newCal.set(Calendar.DAY_OF_MONTH, 1); currentMonth = newCal }) { Icon(Icons.Filled.ChevronRight, "Siguiente") } }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) { listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb").forEach { Text(text = it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) } }
                Spacer(modifier = Modifier.height(8.dp))

                val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH); val tempCal = currentMonth.clone() as Calendar; tempCal.set(Calendar.DAY_OF_MONTH, 1); val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1; val totalCells = daysInMonth + firstDayOfWeek; val rows = (totalCells + 6) / 7
                Column(modifier = Modifier.fillMaxWidth()) {
                    for (i in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (j in 0..6) {
                                val cellIndex = i * 7 + j; val dayNumber = cellIndex - firstDayOfWeek + 1
                                if (dayNumber in 1..daysInMonth) {
                                    val dayCal = currentMonth.clone() as Calendar; dayCal.set(Calendar.DAY_OF_MONTH, dayNumber)
                                    val hasReminder = reminders.any { isSameDay(it.targetDateInMillis, dayCal.timeInMillis) }; val hasFiador = fiadores.any { isSameDay(it.targetDateInMillis, dayCal.timeInMillis) }; val hasProduct = if (currentTab == 1) products.any { it.expirationDateInMillis != null && isSameDay(it.expirationDateInMillis, dayCal.timeInMillis) } else false; val hasEvents = hasReminder || hasFiador || hasProduct
                                    val bgColor = when { hasProduct -> Color(0xFFD32F2F); hasReminder -> Color(0xFF1976D2); hasFiador -> Color(0xFFFBC02D); else -> Color.Transparent }; val textColor = if (bgColor == Color.Transparent) MaterialTheme.colorScheme.onSurface else Color.White
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(CircleShape).background(bgColor).clickable { onDayClick(dayCal.timeInMillis, hasEvents) }, contentAlignment = Alignment.Center) { Text(text = dayNumber.toString(), fontSize = 14.sp, fontWeight = if (hasEvents) FontWeight.Bold else FontWeight.Normal, color = textColor) }
                                } else { Box(modifier = Modifier.weight(1f).aspectRatio(1f)) }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp)); Divider(color = Color.Gray.copy(alpha = 0.2f))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onViewReminders, modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2), contentColor = Color.White)) { Text("💸 Mis Deudas", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center) }
                    Button(onClick = onViewFiadores, modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D), contentColor = Color.Black)) { Text("📋 Mis Deudores", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center) }
                }
            }
        },
        confirmButton = {}, dismissButton = { }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(isEditMode: Boolean, draftState: ProductDraftState, selectedCountry: String, bcvRate: Double, onDismiss: () -> Unit, onConfirm: (Double, Double) -> Unit) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var inputCurrency by remember { mutableStateOf("USD") }
    val isBsInput = selectedCountry == "Venezuela" && inputCurrency == "BS"

    // NUEVAS VARIABLES: Mantienen la independencia y precisión de ambos valores
    var bsPurchase by remember { mutableStateOf("") }
    var bsPrice by remember { mutableStateOf("") }
    var usdPurchase by remember { mutableStateOf("") }
    var usdPrice by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> if (uri != null) { try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) { e.printStackTrace() }; draftState.imageUri = uri.toString() } }
    if (showDatePicker) { CustomDatePickerDialog(initialDateMillis = draftState.expiryDateMillis ?: System.currentTimeMillis(), onDismiss = { showDatePicker = false }, onDateSelected = { selected -> draftState.expiryDateMillis = selected; showDatePicker = false }) }

    // MODIFICADO: Lógica de carga sin pérdida por redondeo visual
    LaunchedEffect(Unit) {
        if (selectedCountry == "Venezuela") {
            val rawP = draftState.purchasePriceRaw.toDoubleOrNull() ?: 0.0
            val rawV = draftState.priceRaw.toDoubleOrNull() ?: 0.0
            if (bcvRate > 0) {
                // Al redondear la vista a 2 decimales, un 1699.999 se restaura visualmente a 1700
                val dfBs = java.text.DecimalFormat("#.##", java.text.DecimalFormatSymbols(Locale.US))
                val dfUsd = java.text.DecimalFormat("#.##", java.text.DecimalFormatSymbols(Locale.US))

                bsPurchase = if (rawP > 0) dfBs.format(rawP * bcvRate) else ""
                bsPrice = if (rawV > 0) dfBs.format(rawV * bcvRate) else ""

                usdPurchase = if (rawP > 0) dfUsd.format(rawP) else ""
                usdPrice = if (rawV > 0) dfUsd.format(rawV) else ""
            }
            inputCurrency = "BS"
            draftState.purchasePriceRaw = bsPurchase
            draftState.priceRaw = bsPrice
        }
    }

    fun switchCurrency(toBs: Boolean) {
        if (bcvRate <= 0) return
        if (toBs) {
            inputCurrency = "BS"
            draftState.purchasePriceRaw = bsPurchase
            draftState.priceRaw = bsPrice
        } else {
            inputCurrency = "USD"
            draftState.purchasePriceRaw = usdPurchase
            draftState.priceRaw = usdPrice
        }
    }

    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(if (isEditMode) "Editar Producto ✏️" else "Nuevo Producto 🏷️", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).clickable { imagePickerLauncher.launch(arrayOf("image/*")) }, contentAlignment = Alignment.Center) {
                    var bitmap by remember(draftState.imageUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                    LaunchedEffect(draftState.imageUri) { if (draftState.imageUri != null) { val loadedBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, Uri.parse(draftState.imageUri!!))) } else { @Suppress("DEPRECATION") android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.parse(draftState.imageUri!!)) } } catch (e: Exception) { null } }; bitmap = loadedBitmap } else { bitmap = null } }
                    if (draftState.imageUri != null) { if (bitmap != null) { Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = "Imagen del producto", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop) } else { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) } } else { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Image, contentDescription = "Añadir foto", modifier = Modifier.size(48.dp), tint = Color.Gray); Text("Añadir foto del producto", color = Color.Gray, fontSize = 12.sp) } }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = draftState.name, onValueChange = { input -> draftState.name = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }, label = { Text("Nombre del Producto") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences))
                Spacer(modifier = Modifier.height(16.dp))

                if (selectedCountry == "Venezuela") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        FilterChip(selected = inputCurrency == "USD", onClick = { if (inputCurrency == "BS") switchCurrency(false) }, label = { Text("Dólares ($)") })
                        FilterChip(selected = inputCurrency == "BS", onClick = { if (inputCurrency == "USD") switchCurrency(true) }, label = { Text("Bolívares (Bs)") })
                    }
                    Text("Al cambiar de pestaña, el valor que hayas escrito se convertirá automáticamente.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp, top = 4.dp), textAlign = TextAlign.Center)
                }

                val visualTrans = if (selectedCountry == "Venezuela") VisualTransformation.None else AmountVisualTransformation()
                val cleaner = { input: String -> if (selectedCountry == "Venezuela") cleanDecimalInput(input) else cleanAmountInput(input) }
                val symbolText = if (selectedCountry == "Venezuela") if (isBsInput) "(Bs)" else "($)" else ""
                val leadingSym = if (selectedCountry == "Venezuela") if (isBsInput) "Bs" else "$" else "$"

                OutlinedTextField(
                    value = draftState.purchasePriceRaw,
                    onValueChange = { input ->
                        val cleaned = cleaner(input)
                        draftState.purchasePriceRaw = cleaned
                        if (selectedCountry == "Venezuela" && bcvRate > 0) {
                            val num = cleaned.toDoubleOrNull() ?: 0.0
                            val df = java.text.DecimalFormat("#.##", java.text.DecimalFormatSymbols(Locale.US))
                            if (isBsInput) {
                                bsPurchase = cleaned
                                usdPurchase = if (num > 0) df.format(num / bcvRate) else ""
                            } else {
                                usdPurchase = cleaned
                                bsPurchase = if (num > 0) df.format(num * bcvRate) else ""
                            }
                        }
                    },
                    label = { Text("Precio de Compra (Costo) $symbolText") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = visualTrans,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text(leadingSym, color = Color.Gray, modifier = Modifier.padding(start=8.dp)) }
                )
                if (selectedCountry == "Venezuela" && draftState.purchasePriceRaw.isNotEmpty()) {
                    val entered = draftState.purchasePriceRaw.toDoubleOrNull() ?: 0.0
                    val converted = if (isBsInput) (if (bcvRate > 0) "= ${formatUSD(entered / bcvRate)}" else "= $0.00") else "= ${formatBs(entered * bcvRate)}"
                    Text(converted, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = draftState.priceRaw,
                    onValueChange = { input ->
                        val cleaned = cleaner(input)
                        draftState.priceRaw = cleaned
                        if (selectedCountry == "Venezuela" && bcvRate > 0) {
                            val num = cleaned.toDoubleOrNull() ?: 0.0
                            val df = java.text.DecimalFormat("#.##", java.text.DecimalFormatSymbols(Locale.US))
                            if (isBsInput) {
                                bsPrice = cleaned
                                usdPrice = if (num > 0) df.format(num / bcvRate) else ""
                            } else {
                                usdPrice = cleaned
                                bsPrice = if (num > 0) df.format(num * bcvRate) else ""
                            }
                        }
                    },
                    label = { Text("Precio de Venta al Público $symbolText") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = visualTrans,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text(leadingSym, color = Color.Gray, modifier = Modifier.padding(start=8.dp)) }
                )
                if (selectedCountry == "Venezuela" && draftState.priceRaw.isNotEmpty()) {
                    val entered = draftState.priceRaw.toDoubleOrNull() ?: 0.0
                    val converted = if (isBsInput) (if (bcvRate > 0) "= ${formatUSD(entered / bcvRate)}" else "= $0.00") else "= ${formatBs(entered * bcvRate)}"
                    Text(converted, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                }

                Spacer(modifier = Modifier.height(12.dp)); Text("Unidad de Medida", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) { FilterChip(selected = draftState.selectedUnit == "Uds", onClick = { draftState.selectedUnit = "Uds" }, label = { Text("Unidad") }); FilterChip(selected = draftState.selectedUnit == "Kg", onClick = { draftState.selectedUnit = "Kg" }, label = { Text("Kilos") }); FilterChip(selected = draftState.selectedUnit == "L", onClick = { draftState.selectedUnit = "L" }, label = { Text("Litros") }) }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = draftState.stockRaw, onValueChange = { n -> val d = n.filter { it.isDigit() }; draftState.stockRaw = d }, label = { Text("Cantidad Inicial en Stock") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = draftState.minStockRaw, onValueChange = { n -> val d = n.filter { it.isDigit() }; draftState.minStockRaw = d }, label = { Text("Alerta de cantidad baja") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { draftState.hasExpiry = !draftState.hasExpiry }) { Checkbox(checked = draftState.hasExpiry, onCheckedChange = { draftState.hasExpiry = it }); Text("Tiene fecha de vencimiento", fontSize = 14.sp) }
                if (draftState.hasExpiry) { Spacer(modifier = Modifier.height(4.dp)); OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(if (draftState.expiryDateMillis == null) "Seleccionar Fecha 📅" else "Vence: ${formatDateOnly(draftState.expiryDateMillis!!)}") } }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalPurchase: Double
                val finalPrice: Double

                if (selectedCountry == "Venezuela" && bcvRate > 0) {
                    // Siempre calcula el resultado final que se guarda en la DB priorizando el Bolívar,
                    // sin importar en qué pestaña estés. Garantiza que el redondeo visual no afecte los datos reales.
                    val pBs = bsPurchase.toDoubleOrNull() ?: 0.0
                    val vBs = bsPrice.toDoubleOrNull() ?: 0.0
                    finalPurchase = pBs / bcvRate
                    finalPrice = vBs / bcvRate
                } else {
                    finalPurchase = draftState.purchasePriceRaw.toDoubleOrNull() ?: 0.0
                    finalPrice = draftState.priceRaw.toDoubleOrNull() ?: 0.0
                }

                onConfirm(finalPurchase, finalPrice)
            }) { Text("Guardar ✔️") }
        },
        dismissButton = { }
    )
}

@Composable
fun DeleteQuantityDialog(product: Product, initialQty: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var qtyRaw by remember { mutableStateOf(initialQty) }; val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Eliminar Stock 🗑️", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
        containerColor = MaterialTheme.colorScheme.surface,
        text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { Text("¿Cuántas unidades de '${product.name}' deseas eliminar?", textAlign = TextAlign.Center, fontSize = 14.sp); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = qtyRaw, onValueChange = { n -> val d = n.filter { it.isDigit() }; qtyRaw = d }, label = { Text("Cantidad") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.width(150.dp), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.Bold)); Text("Stock actual: ${product.stock} ${product.unit}", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) } },
        confirmButton = { Button(onClick = { val q = qtyRaw.toIntOrNull() ?: -1; if (q > product.stock) { Toast.makeText(context, "Supera el stock actual.", Toast.LENGTH_SHORT).show() } else if (q <= 0) { Toast.makeText(context, "No se puede eliminar 0.", Toast.LENGTH_SHORT).show() } else { onConfirm(q) } }) { Text("Siguiente") } }, dismissButton = { }
    )
}

@Composable
fun RedWarningDialog(productName: String, qty: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false), containerColor = Color(0xFFD32F2F), titleContentColor = Color.White, textContentColor = Color.White,
        title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("¡Acción Irreversible! ⚠️", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar", tint = Color.White) } } },
        text = { Text("Estás a punto de eliminar $qty unidades de '$productName' de tu inventario. ¿Estás seguro?") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFD32F2F))) { Text("Sí, Eliminar", fontWeight = FontWeight.Bold) } }, dismissButton = { }
    )
}

@Composable
fun ProductosVendidosDialog(
    transactions: List<Transaction>,
    activeFiadores: List<Fiador>,
    onDismiss: () -> Unit,
    onDeleteVentas: (List<Transaction>) -> Unit,
    onRestoreFiador: (String, Double, Double) -> Unit // NUEVO
) {
    val context = LocalContext.current
    val ventas = remember(transactions) { transactions.filter { it.isIncome && it.description.startsWith("Venta") } }
    var searchQuery by remember { mutableStateOf("") }

    val processedItems = remember(ventas, searchQuery) {
        val filteredVentas = ventas.filter { sale -> sale.description.contains(searchQuery, ignoreCase = true) || sale.note.contains(searchQuery, ignoreCase = true) || formatDate(sale.timestamp).contains(searchQuery, ignoreCase = true) }

        val list = mutableListOf<Any>()
        val fiadorGroups = mutableMapOf<String, MutableList<Transaction>>()

        filteredVentas.forEach { t ->
            val name = when {
                t.description.startsWith("Venta: Abono inicial (") && t.description.endsWith(")") -> t.description.removePrefix("Venta: Abono inicial (").removeSuffix(")")
                t.description.startsWith("Venta a crédito (") && t.description.endsWith(")") -> t.description.removePrefix("Venta a crédito (").removeSuffix(")")
                t.description.startsWith("Venta: Abono de ") -> t.description.removePrefix("Venta: Abono de ")
                else -> null
            }

            if (name != null) {
                fiadorGroups.getOrPut(name) { mutableListOf() }.add(t)
            } else {
                list.add(t)
            }
        }

        fiadorGroups.forEach { (name, txs) ->
            list.add(Pair(name, txs.sortedByDescending { it.timestamp }))
        }

        list.sortedByDescending {
            if (it is Transaction) it.timestamp
            else {
                @Suppress("UNCHECKED_CAST")
                val pair = it as Pair<String, List<Transaction>>
                pair.second.firstOrNull()?.timestamp ?: 0L
            }
        }
    }

    val totalMonto = remember(ventas) { ventas.sumOf { it.amount } }
    val totalGanancia = remember(ventas) { ventas.sumOf { it.profit } }
    var showConfirmDelete by remember { mutableStateOf(false) }

    // NUEVO ESTADO PARA RESTAURAR FIADOR
    var fiadorToRestoreName by remember { mutableStateOf<String?>(null) }
    var fiadorToRestoreAbonado by remember { mutableStateOf(0.0) }

    if (showConfirmDelete) { AlertDialog(onDismissRequest = { showConfirmDelete = false }, title = { Text("Limpiar Historial ⚠️", fontWeight = FontWeight.Bold) }, text = { Text("¿Estás seguro de que deseas borrar este historial de ventas?\n\nEsta acción eliminará permanentemente todos los registros mostrados actualmente.") }, confirmButton = { Button(onClick = { onDeleteVentas(ventas); showConfirmDelete = false; onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))) { Text("Limpiar Todo", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { showConfirmDelete = false }) { Text("Cancelar") } }) }

    // NUEVO DIÁLOGO DE RESTAURACIÓN
    if (fiadorToRestoreName != null) {
        var restoreAmountRaw by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { fiadorToRestoreName = null },
            title = { Text("Retomar Deuda ♻️", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column {
                    Text("Ingresa la deuda TOTAL original de ${fiadorToRestoreName}. (Debe ser mayor a lo que ya abonó: ${formatCOP(fiadorToRestoreAbonado)})", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = restoreAmountRaw,
                        onValueChange = { restoreAmountRaw = cleanAmountInput(it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Monto TOTAL de la deuda") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val totalAmount = restoreAmountRaw.toDoubleOrNull() ?: 0.0
                    if (totalAmount > fiadorToRestoreAbonado) {
                        onRestoreFiador(fiadorToRestoreName!!, totalAmount, fiadorToRestoreAbonado)
                        fiadorToRestoreName = null
                    } else {
                        Toast.makeText(context, "Monto total inválido", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { fiadorToRestoreName = null }) { Text("Cancelar") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Productos Vendidos 🛍️", fontWeight = FontWeight.Bold, fontSize = 20.sp); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { if(ventas.isEmpty()) { Toast.makeText(context, "No hay ventas para exportar", Toast.LENGTH_SHORT).show(); return@OutlinedButton }; val reporte = buildString { appendLine("📊 REPORTE DE VENTAS"); appendLine("Fecha de Exportación: ${formatDate(System.currentTimeMillis())}"); appendLine("--------------------------------"); ventas.forEachIndexed { index, sale -> appendLine("Venta #${ventas.size - index} - ${formatDateOnly(sale.timestamp)}"); appendLine(sale.description); appendLine(sale.note); appendLine("Total: ${formatCOP(sale.amount)} | Ganancia: ${formatCOP(sale.profit)}"); appendLine("--------------------------------") }; appendLine("TOTAL VENTAS: ${formatCOP(totalMonto)}"); appendLine("TOTAL GANANCIA: ${formatCOP(totalGanancia)}") }; val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, reporte) }; context.startActivity(Intent.createChooser(intent, "Exportar Reporte")) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Filled.Share, contentDescription = "Exportar", modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Exportar", fontSize = 12.sp) }
                    OutlinedButton(onClick = { if(ventas.isNotEmpty()) showConfirmDelete = true else Toast.makeText(context, "No hay ventas para limpiar", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Filled.Delete, contentDescription = "Limpiar", modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Limpiar", fontSize = 12.sp) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Buscar...") }, leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") }, modifier = Modifier.fillMaxWidth().height(50.dp), singleLine = true, shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(12.dp))

                if (processedItems.isEmpty()) {
                    Text("No se encontraron ventas registradas.", color = Color.Gray, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(processedItems) { item ->
                            if (item is Transaction) {
                                val sale = item
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text(formatDate(sale.timestamp), fontSize = 11.sp, color = Color.Gray)
                                            Text(formatCOP(sale.amount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(sale.note, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                                        if (sale.profit > 0) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) { Text("Ganancia: ${formatCOP(sale.profit)}", fontSize = 11.sp, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                }
                            } else {
                                @Suppress("UNCHECKED_CAST")
                                val group = item as Pair<String, List<Transaction>>
                                val name = group.first
                                val txs = group.second
                                var expanded by remember { mutableStateOf(false) }
                                val activeFiador = activeFiadores.find { it.name == name && it.isStore }
                                val totalAbonado = txs.sumOf { it.amount }
                                val groupGanancia = txs.sumOf { it.profit }
                                val latestDate = txs.firstOrNull()?.timestamp ?: 0L

                                val isPaidComplete = activeFiador == null
                                val badgeColor = if (isPaidComplete) Color(0xFF4CAF50) else Color.Red
                                val statusText = if (isPaidComplete) "Pago completo ✅" else "Falta por pagar: ${formatCOP(activeFiador!!.amount - activeFiador.paidAmount)}"

                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text(formatDate(latestDate), fontSize = 11.sp, color = Color.Gray)
                                            Text(formatCOP(totalAbonado), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Abonos de $name", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                Text(statusText, fontSize = 13.sp, color = badgeColor, fontWeight = FontWeight.Bold)
                                            }
                                            // NUEVO BOTÓN: Solo aparece si fue marcado como pagado/completado
                                            if (isPaidComplete) {
                                                IconButton(onClick = {
                                                    fiadorToRestoreName = name
                                                    fiadorToRestoreAbonado = totalAbonado
                                                }) {
                                                    Icon(Icons.Filled.ErrorOutline, contentDescription = "Retomar deuda", tint = Color(0xFFFBC02D))
                                                }
                                            }
                                            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Color.Gray)
                                        }

                                        if (groupGanancia > 0) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                color = Color.Transparent,
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
                                            ) {
                                                Text("Ganancia Obtenida: ${formatCOP(groupGanancia)}", fontSize = 12.sp, color = Color(0xFF4CAF50), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        AnimatedVisibility(visible = expanded) {
                                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                                Divider(color = Color.Gray.copy(alpha = 0.2f))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Detalle de movimientos:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                                txs.forEach { tx ->
                                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                            Text(formatDate(tx.timestamp), fontSize = 10.sp, color = Color.Gray)
                                                            Text(tx.description, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                            if (tx.note.isNotBlank()) Text(tx.note, fontSize = 12.sp)
                                                        }
                                                        Text(formatCOP(tx.amount), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun ScheduledRemindersDialog(reminders: List<Reminder>, onDismiss: () -> Unit, onDelete: (Reminder) -> Unit, onEdit: (Reminder) -> Unit, onCreateNew: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("A quien le debo 📋", fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface, text = { if (reminders.isEmpty()) { Text("No tienes deudas activas registradas.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp)) } else { LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) { itemsIndexed(reminders) { index, reminder -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(text = "${index + 1}.", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.width(28.dp)); Column(modifier = Modifier.weight(1f)) { Text(reminder.title, fontWeight = FontWeight.Bold, fontSize = 16.sp); if (reminder.amount > 0) { Text("Deuda: ${formatCOP(reminder.amount)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary) }; Text(formatDate(reminder.targetDateInMillis), fontSize = 12.sp, color = Color.Gray) }; IconButton(onClick = { onEdit(reminder) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color.Blue.copy(alpha = 0.7f)) }; IconButton(onClick = { onDelete(reminder) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.7f)) } }; if (index < reminders.size - 1) { Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp) } } } } }, confirmButton = { Button(onClick = onCreateNew) { Text("Crear") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}

@Composable
fun ScheduledFiadoresDialog(fiadores: List<Fiador>, onDismiss: () -> Unit, onDelete: (Fiador) -> Unit, onEdit: (Fiador) -> Unit, onCreateNew: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Quien me debe 🤝", fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface, text = { if (fiadores.isEmpty()) { Text("No tienes personas que te deban dinero.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp)) } else { LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) { itemsIndexed(fiadores) { index, fiador -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(text = "${index + 1}.", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.width(28.dp)); Column(modifier = Modifier.weight(1f)) { val phoneStr = if(fiador.phone.isNotBlank()) " 📞 ${fiador.phone}" else ""; val remaining = fiador.amount - fiador.paidAmount; Text(fiador.name + phoneStr, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("Resta: ${formatCOP(remaining)} (Total: ${formatCOP(fiador.amount)}) - ${fiador.reason}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary); Text(formatDate(fiador.targetDateInMillis), fontSize = 12.sp, color = Color.Gray) }; IconButton(onClick = { onEdit(fiador) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color.Blue.copy(alpha = 0.7f)) }; IconButton(onClick = { onDelete(fiador) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.7f)) } }; if (index < fiadores.size - 1) { Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp) } } } } }, confirmButton = { Button(onClick = onCreateNew) { Text("Agregar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(initialReminder: Reminder? = null, preselectedDate: Long? = null, onDismiss: () -> Unit, onConfirm: (String, Double, Long) -> Unit) {
    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var amountRaw by remember { mutableStateOf(if(initialReminder != null && initialReminder.amount > 0) initialReminder.amount.toLong().toString() else "") }
    var tempDateMillis by remember { mutableStateOf<Long?>(initialReminder?.targetDateInMillis ?: preselectedDate) }
    var activeScreen by remember { mutableStateOf(if (initialReminder == null && preselectedDate == null) "NEW_INFO" else if (initialReminder == null && preselectedDate != null) "NEW_TIME" else "EDIT_OPTIONS") }
    var isEditDateOnly by remember { mutableStateOf(false) }
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = initialReminder?.targetDateInMillis ?: preselectedDate ?: System.currentTimeMillis() } }
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showDatePicker) {
        CustomDatePickerDialog(initialDateMillis = tempDateMillis ?: System.currentTimeMillis(), onDismiss = { showDatePicker = false }, onDateSelected = { selected -> val cal = Calendar.getInstance().apply { timeInMillis = tempDateMillis ?: System.currentTimeMillis() }; val hour = cal.get(Calendar.HOUR_OF_DAY); val minute = cal.get(Calendar.MINUTE); val newCal = Calendar.getInstance().apply { timeInMillis = selected; set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0) }; tempDateMillis = newCal.timeInMillis; showDatePicker = false; if (isEditDateOnly) { onConfirm(title, amountRaw.toDoubleOrNull() ?: 0.0, tempDateMillis!!) } else { activeScreen = "NEW_TIME" } })
    }

    if (activeScreen == "EDIT_OPTIONS") { AlertDialog(onDismissRequest = onDismiss, title = { Text("¿Qué deseas editar? ✏️", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, containerColor = MaterialTheme.colorScheme.surface, text = { Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Button(onClick = { activeScreen = "EDIT_INFO" }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("📝 Información de la Deuda") }; Button(onClick = { isEditDateOnly = true; showDatePicker = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("📅 Fecha de Cobro") }; Button(onClick = { activeScreen = "EDIT_TIME" }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("⏰ Hora de Cobro") } } }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }) }

    if (activeScreen == "NEW_INFO" || activeScreen == "EDIT_INFO") {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (activeScreen == "EDIT_INFO") "Editar Deuda ✏️" else "Nueva Deuda 📅", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { input -> title = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } },
                        label = { Text("¿Qué debes pagar? (ej. Luz)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = amountRaw,
                        onValueChange = { amountRaw = cleanAmountInput(it) },
                        label = { Text("Monto de la deuda") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = AmountVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Text("$", color = Color.Gray, modifier = Modifier.padding(start=8.dp)) }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsedAmount = amountRaw.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && parsedAmount > 0) {
                        val capTitle = title.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        if (activeScreen == "EDIT_INFO") {
                            onConfirm(capTitle, parsedAmount, tempDateMillis!!)
                        } else {
                            title = capTitle; showDatePicker = true
                        }
                    } else {
                        Toast.makeText(context, "Ingresa un nombre y monto válido", Toast.LENGTH_SHORT).show()
                    }
                }) { Text(if (activeScreen == "EDIT_INFO") "Guardar" else "Siguiente") }
            },
            dismissButton = { TextButton(onClick = { if (activeScreen == "EDIT_INFO") activeScreen = "EDIT_OPTIONS" else onDismiss() }) { Text(if (activeScreen == "EDIT_INFO") "Atrás" else "Cancelar") } }
        )
    }

    if (activeScreen == "NEW_TIME" || activeScreen == "EDIT_TIME") {
        val currentHourInt = calendar.get(Calendar.HOUR).let { if (it == 0) 12 else it }; val currentMinInt = calendar.get(Calendar.MINUTE); val currentHourStr = currentHourInt.toString(); val currentMinStr = currentMinInt.toString().padStart(2, '0'); var customHour by remember { mutableStateOf(if(initialReminder != null) currentHourInt.toString() else "") }; var customMinute by remember { mutableStateOf(if(initialReminder != null) currentMinStr else "") }; var isPm by remember { mutableStateOf(calendar.get(Calendar.AM_PM) == Calendar.PM) }; val minuteFocusRequester = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, title = { Text("Ingresar Hora ⏰", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { val phoneTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()); Text("Hora actual del teléfono: $phoneTime", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp).alpha(0.7f)); Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) { OutlinedTextField(value = customHour, onValueChange = { input -> if (input.isEmpty()) { customHour = input } else if (input.length <= 2 && input.all { char -> char.isDigit() }) { val h = input.toIntOrNull(); if (h != null) { if (input.length == 1 && h == 0) { customHour = input } else if (h in 1..12) { customHour = input; if (input.length == 2) minuteFocusRequester.requestFocus() } } } }, placeholder = { Text(currentHourStr, color = Color.Gray.copy(alpha=0.4f), fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, modifier = Modifier.width(80.dp), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 28.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true); Text(" : ", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp)); OutlinedTextField(value = customMinute, onValueChange = { input -> if (input.isEmpty()) { customMinute = input } else if (input.length <= 2 && input.all { it.isDigit() }) { val m = input.toIntOrNull(); if (m != null && m in 0..59) { customMinute = input } } }, placeholder = { Text(currentMinStr, color = Color.Gray.copy(alpha=0.4f), fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, modifier = Modifier.width(80.dp).focusRequester(minuteFocusRequester), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 28.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true); Spacer(modifier = Modifier.width(8.dp)); Column { FilterChip(selected = !isPm, onClick = { isPm = false }, label = { Text("AM") }); FilterChip(selected = isPm, onClick = { isPm = true }, label = { Text("PM") }) } } } },
            confirmButton = { TextButton(onClick = { val finalH = customHour.toIntOrNull() ?: currentHourInt; val finalM = customMinute.toIntOrNull() ?: currentMinInt; var hour24 = finalH; if (isPm && hour24 < 12) hour24 += 12; if (!isPm && hour24 == 12) hour24 = 0; if (tempDateMillis != null) { val baseCal = Calendar.getInstance().apply { timeInMillis = tempDateMillis!! }; val localCal = Calendar.getInstance().apply { set(baseCal.get(Calendar.YEAR), baseCal.get(Calendar.MONTH), baseCal.get(Calendar.DAY_OF_MONTH), hour24, finalM, 0) }; onConfirm(title, amountRaw.toDoubleOrNull() ?: 0.0, localCal.timeInMillis) } }) { Text(if (activeScreen == "EDIT_TIME") "Guardar" else "Aceptar") } },
            dismissButton = { TextButton(onClick = { if (activeScreen == "NEW_TIME") activeScreen = "NEW_INFO" else activeScreen = "EDIT_OPTIONS" }) { Text("Atrás") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiadorDialog(
    initialFiador: Fiador? = null,
    initialName: String = "",
    initialCart: List<Pair<Product, Int>> = emptyList(),
    initialCash: Double = 0.0,
    initialDigital: Double = 0.0,
    products: List<Product>,
    selectedCountry: String, // NUEVO
    bcvRate: Double,         // NUEVO
    preselectedDate: Long? = null,
    isStore: Boolean,
    onDismiss: () -> Unit,
    onConfirmNew: (String, String, List<Pair<Product, Int>>, Double, Long, Double, Double) -> Unit,
    onConfirmEdit: (Fiador, Long) -> Unit,
    onConfirmAbono: (Fiador, Double, String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialFiador?.name ?: initialName) }
    var phone by remember { mutableStateOf(initialFiador?.phone ?: "") }
    var tempDateMillis by remember { mutableStateOf<Long?>(initialFiador?.targetDateInMillis ?: preselectedDate) }
    var activeScreen by remember { mutableStateOf(if (initialFiador == null && preselectedDate == null) "NEW_INFO" else if (initialFiador == null && preselectedDate != null) "NEW_TIME" else "EDIT_OPTIONS") }
    var isEditDateOnly by remember { mutableStateOf(false) }

    val cartItems = remember { mutableStateListOf<Pair<Product, Int>>().apply { if (initialFiador == null) addAll(initialCart) } }

    var personalDebtAmountRaw by remember { mutableStateOf("") }

    var expandedProduct by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var qtyRaw by remember { mutableStateOf("1") }
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = initialFiador?.targetDateInMillis ?: preselectedDate ?: System.currentTimeMillis() } }

    val initialPaidAmount = initialCash + initialDigital
    val initialMethod = when {
        initialCash > 0 && initialDigital == 0.0 -> "Efectivo"
        initialDigital > 0 && initialCash == 0.0 -> "Digital"
        else -> "Múltiple"
    }

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
            title = { Text(if (activeScreen == "EDIT_INFO") "Editar Deudor ✏️" else "Nuevo Deudor 🤝", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = name, onValueChange = { input -> name = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }, label = { Text("Nombre de la persona") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono (Opcional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))

                    if (initialFiador == null) {
                        if (isStore) {
                            if (initialCart.isEmpty()) {
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
                            }

                            if (cartItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(if (initialCart.isEmpty()) 16.dp else 0.dp))
                                Text(if (initialCart.isEmpty()) "Lista de deuda:" else "Resumen de la deuda:", fontSize = 12.sp, color = Color.Gray)
                                cartItems.forEachIndexed { index, item ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("• ${item.second}${item.first.unit} ${item.first.name}", fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        // MODIFICADO: Agregada la función formatMoneyMain
                                        Text(formatMoneyMain(item.first.price * item.second, selectedCountry), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        if (initialCart.isEmpty()) {
                                            IconButton(onClick = { cartItems.removeAt(index) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, null, tint = Color.Red, modifier = Modifier.size(16.dp)) }
                                        }
                                    }
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                val total = cartItems.sumOf { it.first.price * it.second }
                                Text("Total Deuda: ${formatCOP(total)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                                if (initialPaidAmount > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Abono inicial: ${formatCOP(initialPaidAmount)} ($initialMethod)", fontSize = 14.sp, color = Color(0xFF4CAF50))
                                    Text("Resta por pagar: ${formatCOP(total - initialPaidAmount)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Red)
                                }
                            }
                        } else {
                            Text("Monto de la deuda:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = personalDebtAmountRaw,
                                onValueChange = { personalDebtAmountRaw = cleanAmountInput(it) },
                                label = { Text("¿Cuánto dinero le prestaste?") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = AmountVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Text("$", color = Color.Gray, modifier = Modifier.padding(start=8.dp)) }
                            )

                            if (initialPaidAmount > 0) {
                                val totalPersonal = personalDebtAmountRaw.toDoubleOrNull() ?: 0.0
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Abono inicial: ${formatCOP(initialPaidAmount)} ($initialMethod)", fontSize = 14.sp, color = Color(0xFF4CAF50))
                                Text("Resta por pagar: ${formatCOP(totalPersonal - initialPaidAmount)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Red)
                            }
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
                        val pAmount = personalDebtAmountRaw.toDoubleOrNull() ?: 0.0

                        if (initialFiador == null && isStore && cartItems.isEmpty()) {
                            Toast.makeText(context, "Agrega productos a la deuda", Toast.LENGTH_SHORT).show()
                        } else if (initialFiador == null && !isStore && pAmount <= 0) {
                            Toast.makeText(context, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                        } else {
                            if (activeScreen == "EDIT_INFO") {
                                onConfirmEdit(initialFiador!!.copy(name = capName, phone = phone), tempDateMillis!!)
                            } else {
                                name = capName; showDatePicker = true
                            }
                        }
                    } else {
                        Toast.makeText(context, "Escribe el nombre del deudor", Toast.LENGTH_SHORT).show()
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
                        OutlinedTextField(value = customHour, onValueChange = { input -> if (input.isEmpty()) { customHour = input } else if (input.length <= 2 && input.all { char -> char.isDigit() }) { val h = input.toIntOrNull(); if (h != null) { if (input.length == 1 && h == 0) { customHour = input } else if (h in 1..12) { customHour = input; if (input.length == 2) minuteFocusRequester.requestFocus() } } } }, placeholder = { Text(currentHourStr, color = Color.Gray.copy(alpha=0.4f), fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, modifier = Modifier.width(80.dp), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 28.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true); Text(" : ", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp)); OutlinedTextField(value = customMinute, onValueChange = { input -> if (input.isEmpty()) { customMinute = input } else if (input.length <= 2 && input.all { it.isDigit() }) { val m = input.toIntOrNull(); if (m != null && m in 0..59) { customMinute = input } } }, placeholder = { Text(currentMinStr, color = Color.Gray.copy(alpha=0.4f), fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, modifier = Modifier.width(80.dp).focusRequester(minuteFocusRequester), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 28.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true); Spacer(modifier = Modifier.width(8.dp)); Column { FilterChip(selected = !isPm, onClick = { isPm = false }, label = { Text("AM") }); FilterChip(selected = isPm, onClick = { isPm = true }, label = { Text("PM") }) }
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
                            val pAmount = personalDebtAmountRaw.toDoubleOrNull() ?: 0.0
                            onConfirmNew(name, phone, cartItems.toList(), pAmount, localCal.timeInMillis, initialCash, initialDigital)
                        }
                    }
                }) { Text(if (activeScreen == "EDIT_TIME") "Guardar" else "Aceptar") }
            },
            dismissButton = { TextButton(onClick = { if (activeScreen == "NEW_TIME") activeScreen = "NEW_INFO" else activeScreen = "EDIT_OPTIONS" }) { Text("Atrás") } }
        )
    }
}

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
fun PlanCardInfo(
    title: String,
    subtitle: String,
    features: List<String>,
    restrictions: List<String>,
    isGold: Boolean = false // <-- NUEVO: Parámetro para saber si es el plan GOLD
) {
    Card(
        modifier = Modifier.width(280.dp).fillMaxHeight(),
        // MODIFICADO: Agrega el borde dorado si es el plan GOLD
        border = if (isGold) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD700)) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight()) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))

            // Características Permitidas
            Column(modifier = Modifier.weight(1f)) {
                features.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(Icons.Filled.Check, contentDescription = "Permitido", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(feature, fontSize = 13.sp)
                    }
                }
                if (restrictions.isNotEmpty()) { Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha=0.2f)) }

                // Restricciones
                restrictions.forEach { restriction ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp).alpha(0.5f)) {
                        Icon(Icons.Filled.Close, contentDescription = "No Permitido", tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(restriction, fontSize = 13.sp, textDecoration = TextDecoration.LineThrough)
                    }
                }
            }

            // NUEVO: Botón de Compra Atractivo
            Button(
                onClick = { /* Acción para contactar al admin/comprar */ },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)
            ) {
                Text("¡Adquiérelo Ahora!", fontWeight = FontWeight.Bold)
            }
        }
    }
}
@Composable
fun ProductInfoDialog(product: Product, selectedCountry: String, bcvRate: Double, onDismiss: () -> Unit, onDeleteCompletely: () -> Unit) {
    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Detalle del Producto ℹ️", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(product.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // PRECIO UNITARIO
                val unitName = when(product.unit) { "Kg" -> "Kilo"; "L" -> "Litro"; else -> "Unidad" }
                Text("Precio por $unitName:", fontSize = 14.sp, color = Color.Gray)
                Text(formatMoneyMain(product.price, selectedCountry), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (selectedCountry == "Venezuela" && bcvRate > 0) {
                    // Muestra el equivalente en Bs abajo
                    Text(formatMoneySec(product.price, selectedCountry, bcvRate).replace("= ", ""), fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Stock Disponible: ${product.stock} ${product.unit}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.Gray.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                // VALOR TOTAL EN INVENTARIO
                val totalValue = product.price * product.stock
                Text("Valor Total en Inventario:", fontSize = 14.sp, color = Color.Gray)
                Text(formatMoneyMain(totalValue, selectedCountry), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                if (selectedCountry == "Venezuela" && bcvRate > 0) {
                    // Muestra el equivalente en Bs abajo
                    Text(formatMoneySec(totalValue, selectedCountry, bcvRate).replace("= ", ""), fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = { }, dismissButton = { TextButton(onClick = onDeleteCompletely, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Text("Eliminar del Inventario", fontWeight = FontWeight.Bold) } }
    )
}

@Composable
fun DashboardCard(balance: Double, income: Double, expense: Double, cashBalance: Double, digitalBalance: Double) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Saldo Total", color = Color.Gray, fontSize = 16.sp)
            Text(text = formatCOP(balance), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = if (balance >= 0) Color(0xFF4CAF50) else Color(0xFFE53935))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth().background(Color.DarkGray.copy(alpha=0.1f), RoundedCornerShape(8.dp)).padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Efectivo", fontSize=12.sp, color=Color.Gray)
                    Text(formatCOP(cashBalance), fontSize=14.sp, fontWeight=FontWeight.Bold, color = if (cashBalance >= 0) Color(0xFF4CAF50) else Color(0xFFE53935))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Digital", fontSize=12.sp, color=Color.Gray)
                    Text(formatCOP(digitalBalance), fontSize=14.sp, fontWeight=FontWeight.Bold, color = if (digitalBalance >= 0) Color(0xFF4CAF50) else Color(0xFFE53935))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.clip(CircleShape).background(Color(0xFFE8F5E9).copy(alpha = 0.2f)).padding(4.dp)) { Text("🟢", fontSize = 12.sp) }; Spacer(modifier = Modifier.width(4.dp)); Text("Ingresos", color = Color.Gray) }; Text(formatCOP(income), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.clip(CircleShape).background(Color(0xFFFFEBEE).copy(alpha = 0.2f)).padding(4.dp)) { Text("🔴", fontSize = 12.sp) }; Spacer(modifier = Modifier.width(4.dp)); Text("Gastos", color = Color.Gray) }; Text(formatCOP(expense), fontWeight = FontWeight.Bold, color = Color(0xFFF44336)) }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onDelete: () -> Unit) {
    val isIncome = transaction.isIncome
    val color = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
    val emoji = getSmartEmoji(transaction.description, isIncome)

    val methodText = when {
        transaction.cashAmount > 0 && transaction.digitalAmount == 0.0 -> "Efectivo"
        transaction.digitalAmount > 0 && transaction.cashAmount == 0.0 -> "Digital"
        transaction.cashAmount > 0 && transaction.digitalAmount > 0 -> "Mixto"
        else -> ""
    }

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Text(text = emoji, fontSize = 24.sp) }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(text = transaction.description, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)

                val noteAndMethod = buildString {
                    if (transaction.note.isNotBlank()) append(transaction.note)
                    if (methodText.isNotEmpty()) {
                        if (isNotEmpty()) append(" | ")
                        append(methodText)
                    }
                }
                if (noteAndMethod.isNotEmpty()) {
                    Text(text = noteAndMethod, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 2.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Text(text = formatDate(transaction.timestamp), color = Color.Gray.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) { Text(text = "${if (isIncome) "+" else "-"}${formatCOP(transaction.amount)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color, maxLines = 1) }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Delete, "Eliminar", tint = Color.Gray, modifier = Modifier.size(20.dp)) }
        }
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
@SuppressLint("ObsoleteSdkInt")
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        // Bloque WakeLock Seguro (Ahora dura 8 segundos para darle tiempo a la voz de terminar)
        var wakeLock: PowerManager.WakeLock? = null
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MiBilletera::AlarmaWakeLock")
            wakeLock?.acquire(8000)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
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
                                val userPrefs = context.getSharedPreferences("FinancePrefs_$userId", Context.MODE_PRIVATE)
                                val useVoice = userPrefs.getBoolean("voiceEnabled", false)

                                if (useVoice) {
                                    AppVoice.speak(context, "Nuevo mensaje de $notificationSender. $notificationMsg")
                                } else {
                                    AppSounds.init()
                                }

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

            // --- NUEVO: Extraemos el texto fantasma si existe ---
            val voiceText = intent.getStringExtra("VOICE_TEXT")
            val id = intent.getIntExtra("ID", 0)

            val authPrefs = context.getSharedPreferences("GlobalAuthPrefs", Context.MODE_PRIVATE)
            val userId = authPrefs.getString("lastKnownUserId", null)
            var useVoice = false
            var customSound = ""

            if (userId != null) {
                val userPrefs = context.getSharedPreferences("FinancePrefs_$userId", Context.MODE_PRIVATE)
                useVoice = userPrefs.getBoolean("voiceEnabled", false)
                customSound = userPrefs.getString("customSoundUri", "") ?: ""
            }

            if (useVoice) {
                // Selecciona el texto fantasma (fluido) o el normal (si es otra notificación)
                val textToSpeak = voiceText ?: "$notifTitle. $notifText"
                // Limpieza rápida extra para que no diga "punto" en notificaciones normales
                val cleanText = textToSpeak.replace(Regex("\\.(?=\\s|$)"), ",")
                AppVoice.speak(context, cleanText)
            } else {
                AppSounds.init()
                AppSounds.play(context, customSound)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null) {
                val channelId = "finance_alarms_v6"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(channelId, "Recordatorios de App", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Notificaciones para recordar pagos y cobros"
                        enableVibration(true)
                        setSound(null, null)
                    }
                    notificationManager.createNotificationChannel(channel)
                }
                val tapIntent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
                val tapPendingIntent = PendingIntent.getActivity(context, id, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(notifTitle) // Muestra el título con emojis
                    .setContentText(notifText) // Muestra el texto con los signos $
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
                    .setContentIntent(tapPendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(id, notification)
            }
        } finally {
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