package com.xxcamixx.contabilidad.viewmodel

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.xxcamixx.contabilidad.data.AppDatabase
import com.xxcamixx.contabilidad.model.BackupData
import com.xxcamixx.contabilidad.model.BackupRecord
import com.xxcamixx.contabilidad.model.CloudPayload
import com.xxcamixx.contabilidad.model.Fiador
import com.xxcamixx.contabilidad.model.Product
import com.xxcamixx.contabilidad.model.ComercioProduct
import com.xxcamixx.contabilidad.model.ComercioPedido
import com.xxcamixx.contabilidad.model.ComercioMovement
import com.xxcamixx.contabilidad.model.Reminder
import com.xxcamixx.contabilidad.model.Transaction
import com.xxcamixx.contabilidad.model.CierreSession
import com.xxcamixx.contabilidad.network.CloudSyncWorker
import com.xxcamixx.contabilidad.network.RetrofitInstance
import com.xxcamixx.contabilidad.receiver.ReminderReceiver
import com.xxcamixx.contabilidad.util.AppSounds
import com.xxcamixx.contabilidad.util.formatCOP
import com.xxcamixx.contabilidad.util.formatMoneyMain
import com.xxcamixx.contabilidad.util.formatQty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class FinanceViewModel(application: Application, val userId: String) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application, userId).financeDao()
    private val userPrefs = application.getSharedPreferences("FinancePrefs_$userId", Context.MODE_PRIVATE)

    private val _selectedCountryFlow = MutableStateFlow(userPrefs.getString("selectedCountry", "Colombia") ?: "Colombia")

    var selectedCountry by mutableStateOf(_selectedCountryFlow.value)
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: Flow<List<Transaction>> = _selectedCountryFlow.flatMapLatest { dao.getAllTransactions(it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val cierreSessions: Flow<List<CierreSession>> = _selectedCountryFlow.flatMapLatest { dao.getAllCierreSessions(it) }

    fun createCierreSession(mode: String, name: String, totalIncomes: Double, totalExpenses: Double) {
        viewModelScope.launch {
            val session = CierreSession(
                mode = mode,
                name = name,
                totalIncomes = totalIncomes,
                totalExpenses = totalExpenses,
                country = selectedCountry
            )
            val sessionId = dao.insertCierreSession(session).toInt()

            // Mark items as closed depending on mode
            if (mode == "PERSONAL") {
                dao.updatePersonalTransactionsWithCierre(selectedCountry, sessionId)
                dao.updateFiadoresWithCierre(selectedCountry, sessionId, "PERSONAL")
            } else if (mode == "TIENDA") {
                dao.updateTiendaTransactionsWithCierre(selectedCountry, sessionId)
                dao.updateFiadoresWithCierre(selectedCountry, sessionId, "TIENDA")
            } else if (mode == "PEDIDOS") {
                dao.updateComercioMovementsWithCierre(selectedCountry, sessionId)
            }
        }
    }

    fun getTransactionsForCierre(cierreId: Int): Flow<List<Transaction>> {
        return dao.getTransactionsByCierreId(selectedCountry, cierreId)
    }

    fun getComercioMovementsForCierre(cierreId: Int): Flow<List<ComercioMovement>> {
        return dao.getComercioMovementsByCierreId(selectedCountry, cierreId)
    }

    fun getFiadoresForCierre(cierreId: Int): Flow<List<Fiador>> {
        return dao.getFiadoresByCierreId(selectedCountry, cierreId)
    }

    fun deleteCierreSession(session: CierreSession) {
        viewModelScope.launch {
            dao.deleteCierreSession(session)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val reminders: Flow<List<Reminder>> = _selectedCountryFlow.flatMapLatest { dao.getAllReminders(it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val fiadores: Flow<List<Fiador>> = _selectedCountryFlow.flatMapLatest { dao.getAllFiadores(it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val products: Flow<List<Product>> = _selectedCountryFlow.flatMapLatest { dao.getAllProducts(it) }

    // --- NUEVO: Gestión de Categorías ---
    var customCategories by mutableStateOf(userPrefs.getStringSet("customCategories", setOf("Comida", "Transporte", "Deuda", "Servicios"))!!.toList())
        private set

    fun addCategory(cat: String) {
        val updated = (customCategories + cat).distinct()
        customCategories = updated
        userPrefs.edit().putStringSet("customCategories", updated.toSet()).apply()
    }
    fun removeCategory(cat: String) {
        val updated = customCategories - cat
        customCategories = updated
        userPrefs.edit().putStringSet("customCategories", updated.toSet()).apply()
    }
    // ------------------------------------

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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.deleteOrphanComercioMovements()
            } catch (e: Exception) {}
        }
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
            } catch (e: Exception) {}
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

    fun manualBackup(backupName: String, onResult: (String) -> Unit) {
        isSyncing = true
        syncMessage = "Guardando tus cuentas actuales..."
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val transactions = dao.getBackupTransactions().map {
                    val b64 = com.xxcamixx.contabilidad.util.uriToBase64(app, it.imageUri)
                    if (b64 != null) it.copy(imageUri = b64) else it
                }
                val reminders = dao.getBackupReminders()
                val fiadores = dao.getBackupFiadores()
                val products = dao.getBackupProducts().map {
                    val b64 = com.xxcamixx.contabilidad.util.uriToBase64(app, it.imageUri)
                    if (b64 != null) it.copy(imageUri = b64) else it
                }
                val comercioProducts = dao.getBackupComercioProducts().map {
                    val b64 = com.xxcamixx.contabilidad.util.uriToBase64(app, it.imageUri)
                    if (b64 != null) it.copy(imageUri = b64) else it
                }
                val comercioPedidos = dao.getBackupComercioPedidos().map {
                    val b64 = com.xxcamixx.contabilidad.util.uriToBase64(app, it.imageUri)
                    if (b64 != null) it.copy(imageUri = b64) else it
                }
                val comercioMovements = dao.getBackupComercioMovements()

                val currentData = BackupData(
                    transactions = transactions,
                    reminders = reminders,
                    fiadores = fiadores,
                    products = products,
                    comercioProducts = comercioProducts,
                    comercioPedidos = comercioPedidos,
                    comercioMovements = comercioMovements
                )
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
                dao.deleteAllComercioProducts(); dao.deleteAllComercioMovements(); dao.deleteAllComercioPedidos()

                record.data.transactions.forEach { val safeCountry = (it.country as String?) ?: "Colombia"; dao.insertTransaction(it.copy(id = 0, country = safeCountry)) }
                record.data.reminders.forEach { val safeCountry = (it.country as String?) ?: "Colombia"; val safeOrigin = (it.originMode as String?) ?: if (it.isStore) "TIENDA" else "PERSONAL"; dao.insertReminder(it.copy(id = 0, country = safeCountry, originMode = safeOrigin)) }
                record.data.fiadores.forEach { val safeCountry = (it.country as String?) ?: "Colombia"; val safeOrigin = (it.originMode as String?) ?: if (it.isStore) "TIENDA" else "PERSONAL"; dao.insertFiador(it.copy(id = 0, country = safeCountry, originMode = safeOrigin)) }
                record.data.products.forEach { val safeCountry = (it.country as String?) ?: "Colombia"; dao.insertProduct(it.copy(id = 0, country = safeCountry)) }

                // Restauración de Pedidos y Mapeo de IDs antiguos a nuevos
                val pedidoIdMap = mutableMapOf<Int, Int>()
                record.data.comercioPedidos.forEach { oldPedido ->
                    val safeCountry = (oldPedido.country as String?) ?: "Colombia"
                    val newPedidoId = dao.insertComercioPedido(oldPedido.copy(id = 0, country = safeCountry)).toInt()
                    pedidoIdMap[oldPedido.id] = newPedidoId
                }

                // Restauración de Productos de Pedido y Mapeo de IDs
                val prodIdMap = mutableMapOf<Int, Int>()
                record.data.comercioProducts.forEach { oldProd ->
                    val safeCountry = (oldProd.country as String?) ?: "Colombia"
                    val targetPedidoId = pedidoIdMap[oldProd.pedidoId] ?: oldProd.pedidoId
                    val newProdId = dao.insertComercioProduct(oldProd.copy(id = 0, pedidoId = targetPedidoId, country = safeCountry)).toInt()
                    prodIdMap[oldProd.id] = newProdId
                }

                // Restauración de Movimientos de Comercio
                record.data.comercioMovements.forEach { oldMov ->
                    val safeCountry = (oldMov.country as String?) ?: "Colombia"
                    val targetProdId = prodIdMap[oldMov.productId] ?: oldMov.productId
                    dao.insertComercioMovement(oldMov.copy(id = 0, productId = targetProdId, country = safeCountry))
                }

                launch(Dispatchers.Main) { onResult("¡Respaldo '${record.name}' restaurado! ☁️📥"); isSyncing = false; syncMessage = "" }
            } catch (e: Exception) { launch(Dispatchers.Main) { onResult("Error al restaurar los datos: ${e.message}"); isSyncing = false; syncMessage = "" } }
        }
    }

    fun addTransaction(description: String, amount: Double, isIncome: Boolean, note: String, method: String, category: String? = null, imageUri: String? = null) {
        viewModelScope.launch {
            val cash = if (method == "Efectivo") amount else 0.0
            val digital = if (method == "Digital") amount else 0.0
            dao.insertTransaction(Transaction(description = description, amount = amount, isIncome = isIncome, note = note, cashAmount = cash, digitalAmount = digital, country = selectedCountry, category = category, imageUri = imageUri))
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

    fun addReminder(title: String, amount: Double, dateInMillis: Long, isStore: Boolean, context: Context, onConfigured: (String) -> Unit, originMode: String = if (isStore) "TIENDA" else "PERSONAL") {
        viewModelScope.launch {
            val reminderId = dao.insertReminder(Reminder(title = title, amount = amount, targetDateInMillis = dateInMillis, isStore = isStore, country = selectedCountry, originMode = originMode)).toInt()
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

    fun addFiador(name: String, phone: String, cartItems: List<Pair<Product, Int>>, personalDebtAmount: Double, dateInMillis: Long, initialCash: Double = 0.0, initialDigital: Double = 0.0, isStore: Boolean, context: Context, onConfigured: (String) -> Unit, originMode: String = if (isStore) "TIENDA" else "PERSONAL") {
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

            val fiadorId = dao.insertFiador(Fiador(name = name, phone = phone, amount = totalAmount, reason = reason, targetDateInMillis = dateInMillis, paidAmount = initialPaidAmount, paymentHistory = history, isStore = isStore, totalCost = totalCost, country = selectedCountry, originMode = originMode)).toInt()

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
                country = selectedCountry,
                originMode = "TIENDA"
            )
            val id = dao.insertFiador(f).toInt()
            val remaining = totalAmount - paidAmount
            val voiceText = createFiadorVoiceText(name, remaining, "Deuda retomada manualmente", true)
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

    // --- COMERCIO ---
    val comercioPedidos: kotlinx.coroutines.flow.Flow<List<ComercioPedido>> = _selectedCountryFlow.flatMapLatest { dao.getAllComercioPedidos(it) }
    val comercioProducts: kotlinx.coroutines.flow.Flow<List<ComercioProduct>> = _selectedCountryFlow.flatMapLatest { dao.getAllComercioProducts(it) }
    val comercioMovements: kotlinx.coroutines.flow.Flow<List<ComercioMovement>> = _selectedCountryFlow.flatMapLatest { dao.getAllComercioMovements(it) }


    fun addComercioPedido(name: String, imageUri: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertComercioPedido(ComercioPedido(name = name, imageUri = imageUri, country = selectedCountry))
        }
    }

    fun updateComercioPedido(pedido: ComercioPedido) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateComercioPedido(pedido)
        }
    }
    fun deleteComercioProduct(product: ComercioProduct) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteComercioMovementsByProductId(product.id)
            dao.deleteComercioProduct(product)
        }
    }
    fun updateComercioProduct(product: ComercioProduct) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateComercioProduct(product)
        }
    }

    fun deleteComercioPedido(pedido: ComercioPedido) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteComercioMovementsByPedidoId(pedido.id)
            dao.deleteComercioProductsByPedidoId(pedido.id)
            dao.deleteComercioPedido(pedido)
        }
    }

    fun checkoutComercioCart(items: List<Triple<ComercioProduct, Double, Double>>, customerName: String = "") {
        checkoutComercioCartWithPayment(
            items = items,
            customerName = customerName,
            paymentMethod = "Efectivo",
            cashReceived = items.sumOf { it.second * it.third },
            digitalReceived = 0.0,
            changeAmount = 0.0,
            isFiado = false
        )
    }

    fun checkoutComercioCartWithPayment(
        items: List<Triple<ComercioProduct, Double, Double>>,
        customerName: String,
        paymentMethod: String,
        cashReceived: Double,
        digitalReceived: Double,
        changeAmount: Double,
        isFiado: Boolean,
        phone: String = "",
        dueDateMillis: Long = 0L,
        context: Context? = null,
        originMode: String = "PEDIDOS"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val saleTimestamp = System.currentTimeMillis()
            val totalSale = items.sumOf { it.second * it.third }
            val paidAmount = when (paymentMethod) {
                "Digital" -> digitalReceived
                "Efectivo" -> cashReceived
                else -> cashReceived + digitalReceived
            }
            val remainingDebt = if (isFiado) maxOf(0.0, totalSale - paidAmount) else 0.0

            val paymentSummary = when {
                isFiado && paidAmount > 0 -> "Fiado | Abonó: ${formatMoneyMain(paidAmount, selectedCountry)} (Efectivo: ${formatMoneyMain(cashReceived, selectedCountry)}, Digital: ${formatMoneyMain(digitalReceived, selectedCountry)}) | Debe: ${formatMoneyMain(remainingDebt, selectedCountry)}"
                isFiado -> "Fiado total | Debe: ${formatMoneyMain(remainingDebt, selectedCountry)}"
                paymentMethod == "Dividido" -> "Pago Dividido | Efectivo: ${formatMoneyMain(cashReceived, selectedCountry)} | Digital: ${formatMoneyMain(digitalReceived, selectedCountry)}" + if (changeAmount > 0) " | Vuelto: ${formatMoneyMain(changeAmount, selectedCountry)}" else ""
                paymentMethod == "Digital" -> "Pago Digital: ${formatMoneyMain(digitalReceived, selectedCountry)}" + if (changeAmount > 0) " | Vuelto: ${formatMoneyMain(changeAmount, selectedCountry)}" else ""
                else -> "Pago Efectivo: ${formatMoneyMain(cashReceived, selectedCountry)}" + if (changeAmount > 0) " | Vuelto: ${formatMoneyMain(changeAmount, selectedCountry)}" else ""
            }

            val noteText = if (customerName.isNotBlank()) "Cliente: $customerName | $paymentSummary" else paymentSummary

            val currentTransactionId = java.util.UUID.randomUUID().toString()
            for (item in items) {
                val p = item.first
                val q = item.second
                val sp = item.third
                dao.updateComercioProduct(p.copy(
                    quantityInStock = p.quantityInStock - q,
                    totalSold = p.totalSold + q,
                    salePricePerUnit = sp
                ))
                dao.insertComercioMovement(ComercioMovement(
                    productId = p.id,
                    productName = p.name,
                    type = "VENTA",
                    quantity = q,
                    pricePerUnit = sp,
                    total = q * sp,
                    note = noteText,
                    timestamp = saleTimestamp,
                    country = selectedCountry,
                    transactionId = currentTransactionId
                ))
            }

            if (isFiado) {
                val productNames = items.joinToString(", ") { "${formatQty(it.second)}${it.first.unit} ${it.first.name}" }
                val totalCost = items.sumOf { it.first.costPerUnit * it.second }
                val history = if (paidAmount > 0) {
                    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                    val mStr = when { cashReceived > 0 && digitalReceived == 0.0 -> "Efectivo"; digitalReceived > 0 && cashReceived == 0.0 -> "Digital"; else -> "Múltiple" }
                    "$dateStr: +${formatCOP(paidAmount)} ($mStr)"
                } else ""
                val targetDate = if (dueDateMillis > 0) dueDateMillis else (System.currentTimeMillis() + 7 * 86400000L)
                val fiadorName = if (customerName.isNotBlank()) customerName else "Cliente Pedido"
                val fiadorId = dao.insertFiador(Fiador(
                    name = fiadorName,
                    phone = phone,
                    amount = totalSale,
                    reason = productNames,
                    targetDateInMillis = targetDate,
                    paidAmount = paidAmount,
                    paymentHistory = history,
                    isStore = true,
                    totalCost = totalCost,
                    country = selectedCountry,
                    originMode = originMode
                )).toInt()

                if (context != null && targetDate > 0) {
                    val voiceText = createFiadorVoiceText(fiadorName, remainingDebt, productNames, true)
                    scheduleNotification(context, targetDate, "¡Cobrar a $fiadorName! 💰", "Monto: ${formatCOP(remainingDebt)} - $productNames", fiadorId + 100000, "FIADOR_TRIGGER", voiceText)
                }
            }
        }
    }

    fun addComercioProduct(pedidoId: Int, name: String, unit: String, quantity: Double, cost: Double, salePrice: Double, imageUri: String? = null) {
        if(pedidoId == 0) return
        viewModelScope.launch(Dispatchers.IO) {
            val productId = dao.insertComercioProduct(ComercioProduct(
                pedidoId = pedidoId, name = name, unit = unit, quantityInStock = quantity, totalPurchased = quantity,
                costPerUnit = cost, salePricePerUnit = salePrice, country = selectedCountry, imageUri = imageUri
            ))
            dao.insertComercioMovement(ComercioMovement(
                productId = productId.toInt(), productName = name, type = "COMPRA",
                quantity = quantity, pricePerUnit = cost, total = quantity * cost,
                note = "Pedido inicial", country = selectedCountry
            ))
        }
    }

    fun sellComercioProduct(product: ComercioProduct, quantity: Double, salePrice: Double) {
        if (quantity > product.quantityInStock) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateComercioProduct(product.copy(
                quantityInStock = product.quantityInStock - quantity,
                totalSold = product.totalSold + quantity,
                salePricePerUnit = salePrice
            ))
            dao.insertComercioMovement(ComercioMovement(
                productId = product.id, productName = product.name, type = "VENTA",
                quantity = quantity, pricePerUnit = salePrice, total = quantity * salePrice,
                country = selectedCountry
            ))
        }
    }


    fun updateComercioProductStock(product: ComercioProduct, quantity: Double, newCost: Double, note: String = "Reabastecimiento", context: Context? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val totalOldCost = product.quantityInStock * product.costPerUnit
            val totalNewCost = quantity * newCost
            val newTotalQty = product.quantityInStock + quantity
            val avgCost = if (newTotalQty > 0) (totalOldCost + totalNewCost) / newTotalQty else newCost
            dao.updateComercioProduct(product.copy(
                quantityInStock = newTotalQty,
                totalPurchased = product.totalPurchased + quantity,
                costPerUnit = avgCost
            ))
            dao.insertComercioMovement(ComercioMovement(
                productId = product.id, productName = product.name, type = "COMPRA",
                quantity = quantity, pricePerUnit = newCost, total = quantity * newCost,
                note = note, country = selectedCountry
            ))
        }
    }

    fun restockComercioProduct(product: ComercioProduct, quantity: Double, newCost: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val totalOldCost = product.quantityInStock * product.costPerUnit
            val totalNewCost = quantity * newCost
            val newTotalQty = product.quantityInStock + quantity
            val avgCost = if (newTotalQty > 0) (totalOldCost + totalNewCost) / newTotalQty else newCost
            dao.updateComercioProduct(product.copy(
                quantityInStock = newTotalQty,
                totalPurchased = product.totalPurchased + quantity,
                costPerUnit = avgCost
            ))
            dao.insertComercioMovement(ComercioMovement(
                productId = product.id, productName = product.name, type = "COMPRA",
                quantity = quantity, pricePerUnit = newCost, total = quantity * newCost,
                note = "Reabastecimiento", country = selectedCountry
            ))
        }
    }


    fun adjustComercioStock(product: ComercioProduct, delta: Double) {
        if (delta == 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            if (delta > 0) {
                val newQty = product.quantityInStock + delta
                val newPurchased = product.totalPurchased + delta
                dao.updateComercioProduct(product.copy(
                    quantityInStock = newQty,
                    totalPurchased = newPurchased
                ))
                dao.insertComercioMovement(ComercioMovement(
                    productId = product.id, productName = product.name, type = "COMPRA",
                    quantity = delta, pricePerUnit = product.costPerUnit, total = delta * product.costPerUnit,
                    note = "Ajuste de inventario (+)", country = selectedCountry
                ))
            } else {
                val discount = Math.min(product.quantityInStock, Math.abs(delta))
                if (discount <= 0.0) return@launch
                val newQty = product.quantityInStock - discount
                val newPurchased = maxOf(0.0, product.totalPurchased - discount)
                dao.updateComercioProduct(product.copy(
                    quantityInStock = newQty,
                    totalPurchased = newPurchased
                ))
                dao.insertComercioMovement(ComercioMovement(
                    productId = product.id, productName = product.name, type = "COMPRA",
                    quantity = -discount, pricePerUnit = product.costPerUnit, total = -discount * product.costPerUnit,
                    note = "Ajuste de inventario (-)", country = selectedCountry
                ))
            }
        }
    }

}
