package com.xxcamixx.contabilidad.ui.screens

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xxcamixx.contabilidad.model.BackupRecord
import com.xxcamixx.contabilidad.model.Fiador
import com.xxcamixx.contabilidad.model.Product
import com.xxcamixx.contabilidad.model.Reminder
import com.xxcamixx.contabilidad.model.Transaction
import com.xxcamixx.contabilidad.network.RetrofitInstance
import com.xxcamixx.contabilidad.model.UserData
import com.xxcamixx.contabilidad.CierresDialog
import com.xxcamixx.contabilidad.model.UserManageRequest
import com.xxcamixx.contabilidad.model.UserSyncRequest
import com.xxcamixx.contabilidad.model.UserTimeRequest
import com.xxcamixx.contabilidad.ui.components.DashboardCard
import com.xxcamixx.contabilidad.ui.components.PlanCardInfo
import com.xxcamixx.contabilidad.ui.components.TransactionItem
import com.xxcamixx.contabilidad.ui.dialogs.AddProductDialog
import com.xxcamixx.contabilidad.ui.dialogs.AddToCartDialog
import com.xxcamixx.contabilidad.ui.dialogs.AdminChatListDialog
import com.xxcamixx.contabilidad.ui.dialogs.AutoSyncSetupDialog
import com.xxcamixx.contabilidad.ui.dialogs.CalendarDialog
import com.xxcamixx.contabilidad.ui.dialogs.ChatDialog
import com.xxcamixx.contabilidad.ui.dialogs.ComercioPedidosHistoryDialog
import com.xxcamixx.contabilidad.ui.dialogs.CheckoutDialog
import com.xxcamixx.contabilidad.ui.dialogs.DeleteQuantityDialog
import com.xxcamixx.contabilidad.ui.dialogs.ExpenseBreakdownDialog
import com.xxcamixx.contabilidad.ui.dialogs.FiadorDialog
import com.xxcamixx.contabilidad.ui.dialogs.LimitDialog
import com.xxcamixx.contabilidad.ui.components.ProductInfoDialog
import com.xxcamixx.contabilidad.ui.dialogs.ProfileDialog
import com.xxcamixx.contabilidad.ui.dialogs.RedWarningDialog
import com.xxcamixx.contabilidad.ui.dialogs.ReminderDialog
import com.xxcamixx.contabilidad.ui.dialogs.ScheduledFiadoresDialog
import com.xxcamixx.contabilidad.ui.dialogs.ScheduledRemindersDialog
import com.xxcamixx.contabilidad.ui.dialogs.SoundSettingsDialog
import com.xxcamixx.contabilidad.ui.dialogs.SummaryDialog
import com.xxcamixx.contabilidad.util.formatBs
import com.xxcamixx.contabilidad.util.formatDate
import com.xxcamixx.contabilidad.util.formatMoneyMain
import com.xxcamixx.contabilidad.util.formatMoneySec
import com.xxcamixx.contabilidad.util.showChatNotification
import com.xxcamixx.contabilidad.util.showPremiumToastMsg
import com.xxcamixx.contabilidad.viewmodel.FinanceViewModel
import com.xxcamixx.contabilidad.viewmodel.ProductDraftState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(viewModel: FinanceViewModel, userName: String, initialRole: String, initialConsumedSeconds: Long, initialPlanDuration: Long, onLogout: () -> Unit, isDarkTheme: Boolean, onThemeToggle: () -> Unit) {
    val context = LocalContext.current
    val authPrefs = context.getSharedPreferences("GlobalAuthPrefs", Context.MODE_PRIVATE)

    val isSuperAdmin = viewModel.userId.lowercase(Locale.getDefault()) == "zonacami77777@gmail.com"

    var localUserName by remember { mutableStateOf(userName) }
    var currentRole by remember { mutableStateOf(if (isSuperAdmin) "ADMIN" else initialRole) }
    var currentConsumed by remember { mutableStateOf(initialConsumedSeconds) }
    var currentPlanDuration by remember { mutableStateOf(initialPlanDuration) }

    var showPlansDialog by remember { mutableStateOf(!isSuperAdmin && initialRole != "GOLD" && initialRole != "ADMIN" && initialRole != "PRUEBA" && initialRole != "Invitado-Gold") }
    var showRoleUpgradeDialog by remember { mutableStateOf<String?>(null) }
    var showRoleDowngradeDialog by remember { mutableStateOf(false) }
    var showWarningDialog by remember { mutableStateOf<String?>(null) }
    var showTrialWarning by remember { mutableStateOf(false) }

    var showGoldWelcomeDialog by remember { mutableStateOf(initialRole == "Invitado-Gold" && initialConsumedSeconds < 300L) }

    // --- 6.1. CONTROL DE ESTADOS DEL PERFIL ---
    var showProfileDialog by remember { mutableStateOf(false) }
    var preselectedDateForEvent by remember { mutableStateOf<Long?>(null) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var isSyncingAccount by remember { mutableStateOf(false) }

    // --- NUEVO: ESTADO CATEGORÍAS Y FOTO ---
    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var showManageStoreCategoriesDialog by remember { mutableStateOf(false) }
    var expandedImageUri by remember { mutableStateOf<String?>(null) }

    // --- NUEVO: MODALES DE DESGLOSE DE GASTOS E INGRESOS ---
    var showCashExpensesDialog by remember { mutableStateOf(false) }
    var showDigitalExpensesDialog by remember { mutableStateOf(false) }
    var showAllExpensesDialog by remember { mutableStateOf(false) }
    var showAllIncomesDialog by remember { mutableStateOf(false) }
    var showCierresDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while(true) {
            delay(30000L)

            // Se eliminó la llamada a viewModel.silentBackup()
            // Ahora solo se consume el tiempo del plan (30s) sin forzar el guardado.

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

    var show24hWarningModal by remember { mutableStateOf(false) } // NUEVO ESTADO PARA EL MODAL DE 24 HORAS

    // --- 6.2. TICKER EN VIVO DE 1 SEGUNDO Y CONTROL DE MODOS ---
    LaunchedEffect(Unit) {
        val hasShownTrialWarning = authPrefs.getBoolean("hasShownTrialWarning_${viewModel.userId}", false)
        val hasShown24hWarning = authPrefs.getBoolean("hasShown24hWarning_${viewModel.userId}", false)

        while (true) {
            delay(1000L)
            if (!isSuperAdmin) {
                currentConsumed++
                val timeLeftSecs = currentPlanDuration - currentConsumed

                // Alerta de 3 horas para modo prueba
                if ((currentRole == "PRUEBA" || currentRole == "Invitado-Gold") && timeLeftSecs in 1..10800 && !hasShownTrialWarning) {
                    showTrialWarning = true
                    authPrefs.edit().putBoolean("hasShownTrialWarning_${viewModel.userId}", true).apply()
                }

                // NUEVO: Alerta obligatoria de 24 horas para membresías activas
                if (currentRole != "INVITADO" && currentRole != "INVITADO_PRUEBA" && currentRole != "PRUEBA" && currentRole != "Invitado-Gold") {
                    if (timeLeftSecs in 1..86400 && !hasShown24hWarning) {
                        show24hWarningModal = true
                        authPrefs.edit().putBoolean("hasShown24hWarning_${viewModel.userId}", true).apply()
                    }
                }

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
    val comercioPedidos by viewModel.comercioPedidos.collectAsState(initial = emptyList())
    val comercioProducts by viewModel.comercioProducts.collectAsState(initial = emptyList())
    val comercioMovements by viewModel.comercioMovements.collectAsState(initial = emptyList())

    var currentTab by remember { mutableStateOf(0) }
    val currentMode = remember(currentTab) {
        when (currentTab) {
            0 -> "PERSONAL"
            1 -> "PEDIDOS"
            else -> "TIENDA"
        }
    }
    val currentTabReminders = remember(reminders, currentMode) {
        reminders.filter {
            it.originMode == currentMode || (it.originMode.isEmpty() && (if (currentMode == "PERSONAL") !it.isStore else it.isStore))
        }
    }
    val currentTabFiadores = remember(fiadores, currentMode) {
        fiadores.filter {
            it.originMode == currentMode || (it.originMode.isEmpty() && (if (currentMode == "PERSONAL") !it.isStore else it.isStore))
        }
    }

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
    var showAddEventSelectionDialog by remember { mutableStateOf(false) }
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
    var showComercioPedidosHistoryDialog by remember { mutableStateOf(false) }
    var showComercioSearch by remember { mutableStateOf(true) }

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

    val allActiveReminders = remember(reminders, currentUiTime) { reminders.filter { it.targetDateInMillis <= currentUiTime } }
    val allActiveFiadores = remember(fiadores, currentUiTime) { fiadores.filter { it.targetDateInMillis <= currentUiTime } }

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
                        val firstName = localUserName.split(" ").first()
                        Column(modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showProfileDialog = true }.padding(horizontal = 4.dp, vertical = 2.dp)) {
                            Text(text = if (currentTab == 0) "Hola, $firstName $crownEmoji" else "Tienda de $firstName 🏪", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            if (viewModel.selectedCountry == "Venezuela" && viewModel.bcvRate > 0) {
                                Text(text = "Tasa BCV: ${formatBs(viewModel.bcvRate)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, actionIconContentColor = MaterialTheme.colorScheme.onPrimary),
                    actions = {
                        if (currentTab == 1) {
                            IconButton(onClick = { showComercioSearch = !showComercioSearch }) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = "Buscar",
                                    tint = if (showComercioSearch) Color(0xFFB388FF) else MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
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

                            // --- NUEVO: BOTÓN DE CIERRES ---
                            DropdownMenuItem(
                                text = { Text("🔒 Cierres") },
                                onClick = { showCierresDialog = true; showMenu = false }
                            )
                            Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                            // --- 1. NUEVO: BOTÓN DE OPCIONES CENTRALIZADO ---
                            DropdownMenuItem(
                                text = { Text("⚙️ Opciones") },
                                onClick = { showOptionsDialog = true; showMenu = false }
                            )
                            Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                            DropdownMenuItem(
                                text = { Text("📦 Historial de pedidos") },
                                onClick = { showComercioPedidosHistoryDialog = true; showMenu = false }
                            )
                            Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                            if (currentTab == 0) {
                                DropdownMenuItem(text = { Text("🏷️ Gestionar Categorías") }, onClick = { showManageCategoriesDialog = true; showMenu = false })
                                Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)
                            } else if (currentTab == 2) {
                                DropdownMenuItem(text = { Text("🏷️ Gestionar Categorías (Tienda)") }, onClick = { showManageStoreCategoriesDialog = true; showMenu = false })
                                Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)
                            }

                            if (currentTab == 0) { DropdownMenuItem(text = { Text("⚠️ Borrar Historial", color = Color(0xFFE53935)) }, onClick = { showDeleteHistoryConfirmDialog = true; showMenu = false }) }

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
                    NavigationBarItem(icon = { Icon(Icons.Filled.ShoppingCart, "Pedidos") }, label = { Text("Pedidos") }, selected = currentTab == 1, onClick = { currentTab = 1 }, colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary))
                            NavigationBarItem(icon = { Icon(Icons.Filled.Storefront, "Tienda") }, label = { Text("Tienda") }, selected = currentTab == 2, onClick = { currentTab = 2 }, colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary))
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

            // --- 6.3. ALERTA DE BIENVENIDA INVITADO GOLD ---
            if (showGoldWelcomeDialog) {
                val timeLeftSecs = currentPlanDuration - currentConsumed
                val hours = timeLeftSecs / 3600
                val mins = (timeLeftSecs % 3600) / 60
                AlertDialog(
                    onDismissRequest = { showGoldWelcomeDialog = false },
                    title = { Text("¡Modo Invitado-Gold! 🌟", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    // TEXTO MODIFICADO: Se eliminó la mención a los 30 segundos
                    text = { Text("Disfrutas de acceso total a todas las herramientas sin ninguna restricción. Te quedan $hours horas y $mins minutos de este privilegio.") },
                    confirmButton = { Button(onClick = { showGoldWelcomeDialog = false }) { Text("¡Entendido!") } }
                )
            }

            // --- 6.4. MODAL DE PERFIL (LLAMADA) ---
            if (showProfileDialog) {
                ProfileDialog(
                    currentName = localUserName,
                    currentRole = currentRole,
                    consumedSecs = currentConsumed,
                    planDurationSecs = currentPlanDuration,
                    userId = viewModel.userId,
                    onDismiss = { showProfileDialog = false },
                    onNameChange = { newName ->
                        localUserName = newName
                        authPrefs.edit().putString("userName", newName).apply()
                        coroutineScope.launch(Dispatchers.IO) {
                            val currentPic = authPrefs.getString("profilePic_${viewModel.userId}", null)
                            try { RetrofitInstance.api.syncUser(UserSyncRequest(viewModel.userId, newName, currentPic)) } catch(e: Exception){}
                        }
                    },
                    onImageChange = { newPic ->
                        coroutineScope.launch(Dispatchers.IO) {
                            try { RetrofitInstance.api.syncUser(UserSyncRequest(viewModel.userId, localUserName, newPic)) } catch(e: Exception){}
                        }
                    },
                    onViewImage = { uri -> expandedImageUri = uri },
                    onUpgradeClick = {
                        showProfileDialog = false
                        showPlansDialog = true
                    }
                )
            }

            // --- 6.5. ALERTAS DE PRUEBA Y TIEMPO ---
            if (showTrialWarning) {
                AlertDialog(
                    onDismissRequest = { showTrialWarning = false },
                    title = { Text("¡Tu Prueba está por terminar! ⏳", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    text = { Text("Te quedan menos de 3 horas de tu Modo de Prueba. Una vez que el tiempo culmine, pasarás automáticamente al Modo Invitado, el cual tiene funciones restringidas de solo lectura. Contacta al administrador si deseas adquirir un plan completo.") },
                    confirmButton = { Button(onClick = { showTrialWarning = false }) { Text("Entendido") } }
                )
            }

            // NUEVO: MODAL DE ADVERTENCIA 24 HORAS RESTANTES
            if (show24hWarningModal) {
                AlertDialog(
                    onDismissRequest = { show24hWarningModal = false },
                    title = { Text("¡Tu Membresía está por terminar! ⚠️", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    text = {
                        Text("Te quedan menos de 24 horas de tiempo activo en la aplicación. Por favor, comunícate con el administrador para que te restablezca tu membresía.\n\nRecuerda realizar el pago primero y tener a la mano el capture de pantalla como soporte para agilizar la activación.")
                    },
                    confirmButton = {
                        Button(onClick = { show24hWarningModal = false }) {
                            Text("Entendido")
                        }
                    }
                )
            }

            if (showInventoryScreen) {
                InventoryScreen(
                    products = products,
                    shoppingCart = shoppingCart,
                    selectedCountry = viewModel.selectedCountry,
                    bcvRate = viewModel.bcvRate,
                    categories = viewModel.customStoreCategories.toList(),
                    onBack = { showInventoryScreen = false },
                    onAddProductClick = { selectedCat ->
                        productToEdit = null
                        productDraftState.clear()
                        productDraftState.category = selectedCat
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
                            DashboardCard(
                                balance = balance,
                                income = totalIncome,
                                expense = totalExpense,
                                cashExpense = personalCashExpense,
                                digitalExpense = personalDigitalExpense,
                                onCashClick = { showCashExpensesDialog = true },
                                onDigitalClick = { showDigitalExpensesDialog = true },
                                onIncomeClick = { showAllIncomesDialog = true },
                                onExpenseClick = { showAllExpensesDialog = true }
                            )
                            AnimatedVisibility(visible = viewModel.minBalanceThreshold > 0 && balance < viewModel.minBalanceThreshold) {
                                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).background(Color(0xFFD32F2F), RoundedCornerShape(8.dp)).padding(12.dp)) {
                                    Text("⚠️ ¡Alerta! Tu saldo está por debajo del límite crítico (${formatMoneyMain(viewModel.minBalanceThreshold, viewModel.selectedCountry)}).", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            val personalActiveFiadores = remember(allActiveFiadores) { allActiveFiadores.filter { it.originMode == "PERSONAL" || (it.originMode.isEmpty() && !it.isStore) } }
                            personalActiveFiadores.forEach { fiador ->
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

                            val personalActiveReminders = remember(allActiveReminders) { allActiveReminders.filter { it.originMode == "PERSONAL" || (it.originMode.isEmpty() && !it.isStore) } }
                            personalActiveReminders.forEach { reminder ->
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
                                        TransactionItem(
                                            transaction = transaction,
                                            onDelete = {
                                                viewModel.deleteTransaction(transaction)
                                                coroutineScope.launch {
                                                    val result = snackbarHostState.showSnackbar(message = "Registro eliminado 🗑️", actionLabel = "Deshacer ↩️", duration = SnackbarDuration.Short)
                                                    if (result == SnackbarResult.ActionPerformed) viewModel.insertRawTransaction(transaction)
                                                }
                                            },
                                            onImageClick = { uri -> expandedImageUri = uri }
                                        )
                                    }
                                }
                            }
                        }
                    } else if (tab == 1) { ComercioScreen(viewModel, comercioPedidos, comercioProducts, comercioMovements, viewModel.selectedCountry, viewModel.bcvRate, onOpenHistory = { showComercioPedidosHistoryDialog = true }, showSearch = showComercioSearch, onToggleSearch = { showComercioSearch = !showComercioSearch }, onEditFiador = { fiadorToEdit = it; showFiadorDialog = true }) } else {
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
                            activeFiadores = allActiveFiadores.filter { it.originMode == "TIENDA" || (it.originMode.isEmpty() && it.isStore) },
                            activeReminders = allActiveReminders.filter { it.originMode == "TIENDA" || (it.originMode.isEmpty() && it.isStore) },
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
                                        val currentPic = authPrefs.getString("profilePic_${viewModel.userId}", null)
                                        val response = RetrofitInstance.api.syncUser(UserSyncRequest(viewModel.userId, localUserName, currentPic))
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

        // --- 6.6. BLOQUE PANEL DE ADMINISTRADOR ---
        if (showAdminPanelDialog) {
            var usersList by remember { mutableStateOf<Map<String, UserData>?>(null) }; var isLoadingUsers by remember { mutableStateOf(true) }
            var roleToAssign by remember { mutableStateOf<String?>(null) }; var targetEmailToAssign by remember { mutableStateOf<String?>(null) }
            var adminTick by remember { mutableStateOf(0) }

            LaunchedEffect(Unit) {
                while(true) {
                    delay(1000L)
                    adminTick++
                }
            }

            LaunchedEffect(Unit) { try { usersList = RetrofitInstance.api.getAllUsers() } catch (_: Exception) { customToastMessage = "Error cargando usuarios" }; isLoadingUsers = false }
            fun manageUser(targetEmail: String, action: String, newRole: String? = null, pDuration: Long? = null) { coroutineScope.launch(Dispatchers.IO) { try { RetrofitInstance.api.manageUser(UserManageRequest(targetEmail, action, newRole, pDuration)); val updatedList = RetrofitInstance.api.getAllUsers(); launch(Dispatchers.Main) { usersList = updatedList; adminTick = 0; customToastMessage = "Acción completada" } } catch (_: Exception) { launch(Dispatchers.Main) { customToastMessage = "Fallo de conexión" } } } }

            if (roleToAssign != null && targetEmailToAssign != null) {
                var customHours by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
                    title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Duración para $roleToAssign ⏱️", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)); IconButton(onClick = { roleToAssign = null; targetEmailToAssign = null }) { Icon(Icons.Filled.Close, "Cerrar") } } },
                    containerColor = MaterialTheme.colorScheme.surface,
                    text = {
                        Column {
                            Text("Selecciona la duración del plan:", fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Button(onClick = { manageUser(targetEmailToAssign!!, "setRole", roleToAssign!!, 2592000L); roleToAssign = null; targetEmailToAssign = null }) { Text("1 Mes") }
                                Button(onClick = { manageUser(targetEmailToAssign!!, "setRole", roleToAssign!!, 15552000L); roleToAssign = null; targetEmailToAssign = null }) { Text("6 Meses") }
                                Button(onClick = { manageUser(targetEmailToAssign!!, "setRole", roleToAssign!!, 31104000L); roleToAssign = null; targetEmailToAssign = null }) { Text("1 Año") }
                            }
                            Divider(modifier = Modifier.padding(vertical = 12.dp))
                            Text("O ingresar horas personalizadas:", fontSize = 14.sp)
                            OutlinedTextField(value = customHours, onValueChange = { customHours = it }, label = { Text("Horas") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                            Button(onClick = { val h = customHours.toLongOrNull() ?: 0L; if (h > 0) { manageUser(targetEmailToAssign!!, "setRole", roleToAssign!!, h * 3600L); roleToAssign = null; targetEmailToAssign = null } }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Aplicar Horas") }
                        }
                    },
                    confirmButton = { }, dismissButton = { }
                )
            }

            AlertDialog(
                onDismissRequest = { showAdminPanelDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false), modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp),
                title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Panel de Administrador 👑", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = { showAdminPanelDialog = false }) { Icon(Icons.Filled.Close, "Cerrar") } } },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (isLoadingUsers) { CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally)) }
                        else if (usersList.isNullOrEmpty()) { Text("No hay usuarios registrados.", modifier = Modifier.padding(16.dp)) }
                        else {
                            val sortedUsers = usersList!!.entries.sortedByDescending { it.value.lastActive }
                            LazyColumn {
                                items(sortedUsers) { (email, data) ->
                                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(data.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                                val statusColor = if (data.isBanned) Color.Red else if (data.role == "PREMIUM" || data.role == "GOLD") Color(0xFFFFD700) else Color(0xFF2196F3)
                                                Text(if (data.isBanned) "BLOQUEADO" else data.role, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            Text(email, fontSize = 12.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val timeLeft = data.planDuration - data.consumedSeconds
                                            val days = timeLeft / 86400; val hours = (timeLeft % 86400) / 3600; val mins = (timeLeft % 3600) / 60
                                            Text("Tiempo Restante: $days d, $hours h, $mins m", fontSize = 13.sp)
                                            Text("Última actividad: ${formatDate(data.lastActive)}", fontSize = 11.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(onClick = { roleToAssign = "BÁSICO"; targetEmailToAssign = email }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8D6E63))) { Text("Básico", fontSize = 11.sp) }
                                                Button(onClick = { roleToAssign = "PREMIUM"; targetEmailToAssign = email }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) { Text("Premium", fontSize = 11.sp) }
                                                Button(onClick = { roleToAssign = "GOLD"; targetEmailToAssign = email }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)) { Text("Gold", fontSize = 11.sp) }
                                                Button(onClick = { manageUser(email, "ban") }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Ban", fontSize = 11.sp) }
                                                Button(onClick = { manageUser(email, "unban") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("Unban", fontSize = 11.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { }
            )
        }

        // --- NUEVO: MODALES DE DESGLOSE DE GASTOS E INGRESOS ---
        val cashExpensesList = remember(personalTransactions) { personalTransactions.filter { !it.isIncome && it.cashAmount > 0 } }
        val digitalExpensesList = remember(personalTransactions) { personalTransactions.filter { !it.isIncome && it.digitalAmount > 0 } }
        val allExpensesList = remember(personalTransactions) { personalTransactions.filter { !it.isIncome } }
        val allIncomesList = remember(personalTransactions) { personalTransactions.filter { it.isIncome } }

        if (showCashExpensesDialog) {
            ExpenseBreakdownDialog(
                title = "Gastos en Efectivo 💵",
                expenses = cashExpensesList,
                onDismiss = { showCashExpensesDialog = false },
                onDelete = {
                    viewModel.deleteTransaction(it)
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(message = "Gasto eliminado 🗑️", actionLabel = "Deshacer ↩️", duration = SnackbarDuration.Short)
                        if (result == SnackbarResult.ActionPerformed) viewModel.insertRawTransaction(it)
                    }
                },
                onImageClick = { uri -> expandedImageUri = uri }
            )
        }

        if (showDigitalExpensesDialog) {
            ExpenseBreakdownDialog(
                title = "Gastos en Digital 💳",
                expenses = digitalExpensesList,
                onDismiss = { showDigitalExpensesDialog = false },
                onDelete = {
                    viewModel.deleteTransaction(it)
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(message = "Gasto eliminado 🗑️", actionLabel = "Deshacer ↩️", duration = SnackbarDuration.Short)
                        if (result == SnackbarResult.ActionPerformed) viewModel.insertRawTransaction(it)
                    }
                },
                onImageClick = { uri -> expandedImageUri = uri }
            )
        }

        if (showAllExpensesDialog) {
            ExpenseBreakdownDialog(
                title = "Todos los Gastos 🔴",
                expenses = allExpensesList,
                onDismiss = { showAllExpensesDialog = false },
                onDelete = {
                    viewModel.deleteTransaction(it)
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(message = "Gasto eliminado 🗑️", actionLabel = "Deshacer ↩️", duration = SnackbarDuration.Short)
                        if (result == SnackbarResult.ActionPerformed) viewModel.insertRawTransaction(it)
                    }
                },
                onImageClick = { uri -> expandedImageUri = uri }
            )
        }

        if (showAllIncomesDialog) {
            ExpenseBreakdownDialog(
                title = "Todos los Ingresos 🟢",
                expenses = allIncomesList,
                onDismiss = { showAllIncomesDialog = false },
                onDelete = {
                    viewModel.deleteTransaction(it)
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(message = "Ingreso eliminado 🗑️", actionLabel = "Deshacer ↩️", duration = SnackbarDuration.Short)
                        if (result == SnackbarResult.ActionPerformed) viewModel.insertRawTransaction(it)
                    }
                },
                onImageClick = { uri -> expandedImageUri = uri }
            )
        }

        // --- 6.8. DIÁLOGO DE AGREGAR / EDITAR PRODUCTO (INVENTARIO) ---
        if (showAddProductDialog) {
            AddProductDialog(
                isEditMode = productToEdit != null,
                draftState = productDraftState,
                selectedCountry = viewModel.selectedCountry,
                bcvRate = viewModel.bcvRate,
                categories = viewModel.customStoreCategories.toList(),
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
                            category = productDraftState.category,
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
                                imageUri = productDraftState.imageUri,
                                category = productDraftState.category
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

        // --- 6.9. DIÁLOGO PARA AÑADIR AL CARRITO DESDE EL INVENTARIO ---
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

        // --- 6.10. DIÁLOGO DE CHECKOUT (COBRAR CARRITO) ---
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

        // --- 6.11. DIÁLOGOS PARA ELIMINAR STOCK PARCIAL Y TOTAL ---
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

        // --- 6.12. DETALLE DEL PRODUCTO ---
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

        // --- 6.13. LÍMITE DE SALDO CRÍTICO ---
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

        // --- 6.14. RESUMEN DE TOTALES ---
        if (showSummaryDialog) {
            SummaryDialog(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = balance,
                transactionCount = personalTransactions.size,
                onDismiss = { showSummaryDialog = false }
            )
        }

        // --- 6.15. BORRAR HISTORIAL DE MOVIMIENTOS PERSONALES ---
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

        // --- 6.16. REINICIAR GANANCIAS DE TIENDA ---
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

        // --- 6.17. SONIDOS Y ASISTENTE ---
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

        // --- 6.18. CALENDARIO Y AGENDA ---
        if (showCalendarDialog) {
            CalendarDialog(
                currentTab = currentTab,
                reminders = currentTabReminders,
                fiadores = currentTabFiadores,
                products = if (currentTab == 2) products else emptyList(),
                onDismiss = { showCalendarDialog = false },
                onDayClick = { dayMillis, _ ->
                    preselectedDateForEvent = dayMillis
                    showCalendarDialog = false
                    showAddEventSelectionDialog = true
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

        if (showAddEventSelectionDialog) {
            AlertDialog(
                onDismissRequest = { showAddEventSelectionDialog = false; preselectedDateForEvent = null },
                title = { Text("¿Qué deseas agregar?", fontWeight = FontWeight.Bold) },
                text = { Text("Selecciona si deseas agregar una Deuda (para pagar) o un Deudor (para cobrar) en este día.") },
                confirmButton = {
                    Button(onClick = {
                        showAddEventSelectionDialog = false
                        showFiadorDialog = true
                    }) {
                        Text("Un Deudor (Cobrar)")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showAddEventSelectionDialog = false
                        showReminderDialog = true
                    }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                        Text("Una Deuda (Pagar)")
                    }
                }
            )
        }

        // --- 6.19. RECORDATORIOS / DEUDAS PERSONALES ---
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
                            isStore = (currentTab != 0),
                            context = context,
                            onConfigured = { msg -> customToastMessage = msg },
                            originMode = currentMode
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

        // --- 6.20. FIADORES / DEUDORES ---
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

        if (showFiadorDialog) {
            FiadorDialog(
                initialFiador = fiadorToEdit,
                initialName = checkoutToFiadorName,
                initialCart = checkoutToFiadorCart,
                initialCash = checkoutToFiadorCash,
                initialDigital = checkoutToFiadorDigital,
                products = if (currentTab == 2) products else emptyList(),
                selectedCountry = viewModel.selectedCountry,
                bcvRate = viewModel.bcvRate,
                preselectedDate = preselectedDateForEvent,
                isStore = (currentTab == 2 && checkoutToFiadorCart.isNotEmpty()),
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
                        isStore = (currentTab == 2 && cart.isNotEmpty()),
                        context = context,
                        onConfigured = { msg -> customToastMessage = msg },
                        originMode = currentMode
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

        // --- 6.21. SINCRONIZACIÓN NUBE / BACKUPS ---
        var showAutoSyncDialog by remember { mutableStateOf(false) }

        if (showCloudSyncDialog) {
            AlertDialog(
                onDismissRequest = { showCloudSyncDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp),
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

                        Divider(modifier = Modifier.padding(vertical = 16.dp))

                        Text("Sincronización Automática", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val syncHour = if (viewModel.autoSyncHour > 12) viewModel.autoSyncHour - 12 else if (viewModel.autoSyncHour == 0) 12 else viewModel.autoSyncHour
                        val syncPm = if (viewModel.autoSyncHour >= 12) "PM" else "AM"
                        val syncMin = viewModel.autoSyncMinute.toString().padStart(2, '0')
                        val syncText = if (viewModel.autoSyncFrequency > 0) "Diaria a las $syncHour:$syncMin $syncPm" else "Desactivada"

                        Text("Estado: $syncText", fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                showCloudSyncDialog = false
                                showAutoSyncDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) { Text("⏰ Configurar Auto-Sincronización") }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showCloudSyncDialog = false }) { Text("Cerrar") } }
            )
        }

        if (showAutoSyncDialog) {
            AutoSyncSetupDialog(
                initialFrequency = viewModel.autoSyncFrequency,
                initialHour = viewModel.autoSyncHour,
                initialMinute = viewModel.autoSyncMinute,
                onDismiss = { showAutoSyncDialog = false },
                onSave = { freq, h, m ->
                    viewModel.updateAutoSyncSchedule(application = context.applicationContext as Application, frequencyDays = freq, hour = h, minute = m)
                    customToastMessage = if (freq > 0) "Auto-Sincronización activada ✅" else "Auto-Sincronización desactivada ❌"
                    showAutoSyncDialog = false
                }
            )
        }

        if (showBackupNameDialog) {
            AlertDialog(
                onDismissRequest = { showBackupNameDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp),
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
                properties = DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp),
                title = { Text("Copias Guardadas ☁️", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    if (isLoadingList) {
                        CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))
                    } else if (cloudBackupsList.isNullOrEmpty()) {
                        Text("No tienes copias de seguridad en la nube.", color = Color.Gray)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                            // SOLUCIÓN 1: Cambiamos !! por ?: emptyList() y agregamos el key = { it.id }
                            items(cloudBackupsList ?: emptyList(), key = { it.id }) { record ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(record.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(formatDate(record.timestamp), fontSize = 12.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(onClick = {
                                                // SOLUCIÓN 2: Activar el icono de carga mientras procesa la eliminación
                                                isLoadingList = true
                                                viewModel.deleteBackupRecord(record.id) { msg ->
                                                    customToastMessage = msg
                                                    viewModel.fetchBackupList { list ->
                                                        cloudBackupsList = list
                                                        isLoadingList = false
                                                    }
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

        // --- 6.22. OPCIONES, PLANES, CHATS Y PANEL ADMIN ---
        if (showOptionsDialog) {
            AlertDialog(
                onDismissRequest = { showOptionsDialog = false }, properties = DialogProperties(dismissOnClickOutside = false),
                title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Opciones ⚙️", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = { showOptionsDialog = false }) { Icon(Icons.Filled.Close, "Cerrar") } } },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    // Se agregó verticalScroll para que funcione sin problemas en pantallas de todos los tamaños
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                        // --- 1. MODO OSCURO ---
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onThemeToggle() }.padding(vertical = 12.dp)) {
                            Icon(if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text("Modo Oscuro", modifier = Modifier.weight(1f), fontSize = 16.sp)
                            Switch(checked = isDarkTheme, onCheckedChange = { onThemeToggle() })
                        }
                        Divider(color = Color.Gray.copy(alpha = 0.2f))

                        // --- 2. GESTOR DE SONIDOS ---
                        // Al tocar aquí, cerrará este modal y abrirá tu motor de sonidos ya programado
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showOptionsDialog = false; showSoundDialog = true }.padding(vertical = 16.dp)) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sonidos de Notificaciones", fontSize = 16.sp)
                                Text("Tonos, toques y asistente de voz", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Divider(color = Color.Gray.copy(alpha = 0.2f))

                        // --- 3. COPIAS DE SEGURIDAD (Ingresada al menú Opciones) ---
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                            if (isManualSyncAllowed) {
                                showOptionsDialog = false
                                showCloudSyncDialog = true
                            } else {
                                showPremiumToastMsg(context)
                            }
                        }.padding(vertical = 16.dp)) {
                            Icon(Icons.Filled.CloudSync, contentDescription = null, tint = if (isManualSyncAllowed) MaterialTheme.colorScheme.primary else Color.Gray)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isManualSyncAllowed) "Copias de Seguridad" else "👑 Copias de Seguridad", fontSize = 16.sp, color = if (isManualSyncAllowed) MaterialTheme.colorScheme.onSurface else Color.Gray)
                                Text("Guardar o restaurar datos en la nube", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Divider(color = Color.Gray.copy(alpha = 0.2f))

                        // --- 4. MONEDA DE LA APP (Mantenido intacto) ---
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
        if (showComercioPedidosHistoryDialog) {
            ComercioPedidosHistoryDialog(
                pedidos = comercioPedidos,
                products = comercioProducts,
                country = viewModel.selectedCountry,
                bcvRate = viewModel.bcvRate,
                onDismiss = { showComercioPedidosHistoryDialog = false },
                onRestockProduct = { prod, qty ->
                    viewModel.updateComercioProductStock(prod, qty, prod.costPerUnit, "Restock desde historial", context)
                },
                onDeletePedido = { ped ->
                    viewModel.deleteComercioPedido(ped)
                }
            )
        }
        if (showAddDialog) {
            com.xxcamixx.contabilidad.ui.dialogs.AddTransactionDialog(
                categories = viewModel.customCategories.toList(),
                onDismiss = { showAddDialog = false },
                onConfirm = { desc, amount, isInc, note, method, cat, uri ->
                    viewModel.addTransaction(desc, amount, isInc, note, method, cat, uri)
                    showAddDialog = false
                }
            )
        }

        if (showCierresDialog) {
            CierresDialog(
                onDismiss = { showCierresDialog = false },
                viewModel = viewModel
            )
        }

        if (showManageStoreCategoriesDialog) {
            com.xxcamixx.contabilidad.ui.dialogs.ManageCategoriesDialog(
                categories = viewModel.customStoreCategories,
                onDismiss = { showManageStoreCategoriesDialog = false },
                onAdd = { viewModel.addStoreCategory(it) },
                onRemove = { viewModel.removeStoreCategory(it) }
            )
        }

        if (showManageCategoriesDialog) {
            com.xxcamixx.contabilidad.ui.dialogs.ManageCategoriesDialog(
                categories = viewModel.customCategories,
                onDismiss = { showManageCategoriesDialog = false },
                onAdd = { viewModel.addCategory(it) },
                onRemove = { viewModel.removeCategory(it) }
            )
        }
    }
}