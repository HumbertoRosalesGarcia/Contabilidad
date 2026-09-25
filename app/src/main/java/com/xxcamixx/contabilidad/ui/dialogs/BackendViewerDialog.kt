package com.xxcamixx.contabilidad.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.xxcamixx.contabilidad.model.*
import com.xxcamixx.contabilidad.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackendViewerDialog(
    viewModel: com.xxcamixx.contabilidad.viewmodel.FinanceViewModel? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    var isServerOnline by remember { mutableStateOf<Boolean?>(null) }
    var isLoadingUsers by remember { mutableStateOf(true) }
    var isLoadingClientData by remember { mutableStateOf(false) }

    var mainSection by remember { mutableStateOf(0) } // 0: Servidor & Disco, 1: Clientes & Datos
    var serverStats by remember { mutableStateOf<ServerStatsResponse?>(null) }
    var isRefreshingStats by remember { mutableStateOf(false) }

    var usersMap by remember { mutableStateOf<Map<String, UserData>>(emptyMap()) }
    var backupsSummary by remember { mutableStateOf<List<BackupFileInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedUserEmail by remember { mutableStateOf<String?>(null) }
    var showClientSelector by remember { mutableStateOf(false) }
    var selectedClientPayload by remember { mutableStateOf<CloudPayload?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Cierres, 1: Productos, 2: Finanzas, 3: Pedidos, 4: Fiadores, 5: Respaldos, 6: JSON

    fun uploadClientBackupPayload(email: String, payload: CloudPayload, onSuccessMessage: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                RetrofitInstance.api.uploadBackup(email, payload)
                withContext(Dispatchers.Main) {
                    selectedClientPayload = payload
                    Toast.makeText(context, "✅ $onSuccessMessage", Toast.LENGTH_SHORT).show()
                    if (email.equals(viewModel?.userId, ignoreCase = true)) {
                        viewModel?.checkAndAutoRestoreIfEmpty()
                    }
                    try {
                        val summaryRes = RetrofitInstance.api.getBackupsSummary().backups
                        backupsSummary = summaryRes
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "❌ Error al guardar en servidor: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Explorador de Disco
    var diskExplorerPath by remember { mutableStateOf("/") }
    var diskData by remember { mutableStateOf<DiskExplorerResponse?>(null) }
    var isLoadingDisk by remember { mutableStateOf(false) }
    var diskError by remember { mutableStateOf<String?>(null) }

    fun loadDiskPath(path: String) {
        isLoadingDisk = true
        diskError = null
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val res = RetrofitInstance.api.getDiskContents(path)
                withContext(Dispatchers.Main) {
                    diskData = res
                    diskExplorerPath = res.currentPath
                    isLoadingDisk = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    diskError = e.localizedMessage ?: "Error al explorar disco"
                    isLoadingDisk = false
                }
            }
        }
    }

    fun refreshAllData() {
        isLoadingUsers = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val users = RetrofitInstance.api.getAllUsers()
                val summaryRes = try { RetrofitInstance.api.getBackupsSummary().backups } catch (_: Exception) { emptyList() }
                val statsRes = try { RetrofitInstance.api.getServerStats() } catch (_: Exception) { null }
                withContext(Dispatchers.Main) {
                    usersMap = users
                    backupsSummary = summaryRes
                    serverStats = statsRes
                    isServerOnline = true
                    isLoadingUsers = false
                    if (selectedUserEmail == null && users.isNotEmpty()) {
                        selectedUserEmail = users.keys.firstOrNull()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isServerOnline = false
                    isLoadingUsers = false
                }
            }
        }
    }

    fun refreshStatsOnly() {
        isRefreshingStats = true
        coroutineScope.launch(Dispatchers.IO) {
            val statsRes = try { RetrofitInstance.api.getServerStats() } catch (_: Exception) { null }
            withContext(Dispatchers.Main) {
                if (statsRes != null) {
                    serverStats = statsRes
                    isServerOnline = true
                }
                isRefreshingStats = false
            }
        }
    }

    // Cargar datos al abrir
    LaunchedEffect(Unit) {
        refreshAllData()
        loadDiskPath("/")
    }

    // Cargar respaldo del cliente seleccionado
    LaunchedEffect(selectedUserEmail) {
        val email = selectedUserEmail
        if (email != null) {
            isLoadingClientData = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val payload = RetrofitInstance.api.getBackup(email)
                    withContext(Dispatchers.Main) {
                        selectedClientPayload = payload
                        isLoadingClientData = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        selectedClientPayload = null
                        isLoadingClientData = false
                    }
                }
            }
        } else {
            selectedClientPayload = null
        }
    }

    // Filtrar lista de usuarios según búsqueda
    val filteredUsers = remember(usersMap, searchQuery) {
        usersMap.filter { (email, data) ->
            if (searchQuery.isBlank()) true
            else email.contains(searchQuery, ignoreCase = true) || data.name.contains(searchQuery, ignoreCase = true)
        }
    }

    // Identificar productos activos y eliminados comparando historial de respaldos (Tienda y Comercio)
    val (activeProducts, deletedProducts) = remember(selectedClientPayload) {
        val payload = selectedClientPayload
        if (payload == null) {
            Pair(emptyList<Product>(), emptyList<Product>())
        } else {
            val allRecords = payload.backups ?: emptyList()
            val latestRecord = allRecords.maxByOrNull { it.timestamp }

            val rawStoreProds = latestRecord?.data?.products ?: payload.products ?: emptyList()
            val rawComercioProds = (latestRecord?.data?.comercioProducts ?: payload.comercioProducts ?: emptyList()).map { cp ->
                Product(
                    id = cp.id,
                    name = cp.name,
                    purchasePrice = cp.costPerUnit,
                    price = cp.salePricePerUnit,
                    stock = cp.quantityInStock.toInt(),
                    unit = cp.unit,
                    category = "Comercio"
                )
            }
            val currentProds = rawStoreProds + rawComercioProds

            val currentProdNames = currentProds.map { it.name.trim().lowercase() }.toSet()

            // Buscar en todos los respaldos históricos aquellos productos que ya no están en la lista actual
            val historicalDeleted = mutableMapOf<String, Product>()
            for (record in allRecords) {
                val pastStore = record.data.products
                val pastComercio = (record.data.comercioProducts ?: emptyList()).map { cp ->
                    Product(
                        id = cp.id,
                        name = cp.name,
                        purchasePrice = cp.costPerUnit,
                        price = cp.salePricePerUnit,
                        stock = cp.quantityInStock.toInt(),
                        unit = cp.unit,
                        category = "Comercio"
                    )
                }
                for (p in (pastStore + pastComercio)) {
                    val key = p.name.trim().lowercase()
                    if (!currentProdNames.contains(key) && !historicalDeleted.containsKey(key)) {
                        historicalDeleted[key] = p
                    }
                }
            }

            Pair(currentProds, historicalDeleted.values.toList())
        }
    }

    // Transacciones del cliente
    val clientTransactions = remember(selectedClientPayload) {
        val payload = selectedClientPayload
        if (payload == null) emptyList()
        else {
            val allRecords = payload.backups ?: emptyList()
            val latestRecord = allRecords.maxByOrNull { it.timestamp }
            latestRecord?.data?.transactions ?: payload.transactions ?: emptyList()
        }
    }

    // Fiadores del cliente
    val clientFiadores = remember(selectedClientPayload) {
        val payload = selectedClientPayload
        if (payload == null) emptyList()
        else {
            val allRecords = payload.backups ?: emptyList()
            val latestRecord = allRecords.maxByOrNull { it.timestamp }
            latestRecord?.data?.fiadores ?: payload.fiadores ?: emptyList()
        }
    }

    // Pedidos de comercio del cliente
    val clientPedidos = remember(selectedClientPayload) {
        val payload = selectedClientPayload
        if (payload == null) emptyList()
        else {
            val allRecords = payload.backups ?: emptyList()
            val latestRecord = allRecords.maxByOrNull { it.timestamp }
            latestRecord?.data?.comercioPedidos ?: payload.comercioPedidos ?: emptyList()
        }
    }

    // Productos de comercio del cliente
    val clientComercioProducts = remember(selectedClientPayload) {
        val payload = selectedClientPayload
        if (payload == null) emptyList()
        else {
            val allRecords = payload.backups ?: emptyList()
            val latestRecord = allRecords.maxByOrNull { it.timestamp }
            latestRecord?.data?.comercioProducts ?: payload.comercioProducts ?: emptyList()
        }
    }

    // Movimientos de pedidos del cliente
    val clientMovements = remember(selectedClientPayload) {
        val payload = selectedClientPayload
        if (payload == null) emptyList()
        else {
            val allRecords = payload.backups ?: emptyList()
            val latestRecord = allRecords.maxByOrNull { it.timestamp }
            latestRecord?.data?.comercioMovements ?: payload.comercioMovements ?: emptyList()
        }
    }

    // Cierres de caja del cliente (Directo de respaldo o reconstruido por cierreId)
    val clientCierres = remember(selectedClientPayload) {
        val payload = selectedClientPayload
        if (payload == null) emptyList()
        else {
            val allRecords = payload.backups ?: emptyList()
            val latestRecord = allRecords.maxByOrNull { it.timestamp }
            val explicit = latestRecord?.data?.cierreSessions ?: payload.cierreSessions ?: emptyList()
            if (explicit.isNotEmpty()) {
                explicit
            } else {
                val allTx = latestRecord?.data?.transactions ?: payload.transactions ?: emptyList()
                val allMov = latestRecord?.data?.comercioMovements ?: payload.comercioMovements ?: emptyList()
                val cierreIds = (allTx.mapNotNull { it.cierreId } + allMov.mapNotNull { it.cierreId }).distinct()
                cierreIds.map { cId ->
                    val txForCierre = allTx.filter { it.cierreId == cId }
                    val movForCierre = allMov.filter { it.cierreId == cId }
                    val inc = txForCierre.filter { it.isIncome }.sumOf { it.amount } + movForCierre.filter { it.type == "IN" || it.type == "VENTA" }.sumOf { it.total }
                    val exp = txForCierre.filter { !it.isIncome }.sumOf { it.amount } + movForCierre.filter { it.type == "OUT" || it.type == "COMPRA" }.sumOf { it.total }
                    val timestamp = txForCierre.firstOrNull()?.timestamp ?: movForCierre.firstOrNull()?.timestamp ?: 0L
                    val mode = when {
                        movForCierre.isNotEmpty() && txForCierre.isNotEmpty() -> "TODOS"
                        movForCierre.isNotEmpty() -> "PEDIDOS"
                        else -> "PERSONAL/TIENDA"
                    }
                    CierreSession(
                        id = cId,
                        mode = mode,
                        name = "Cierre #$cId",
                        totalIncomes = inc,
                        totalExpenses = exp,
                        timestamp = timestamp
                    )
                }
            }
        }
    }

    // Derivaciones para los 3 Modos Operativos (Personal, Tienda y Pedidos)
    val personalTransactions = remember(clientTransactions) {
        clientTransactions.filter { !it.description.startsWith("Venta: ", ignoreCase = true) && !it.category.equals("Tienda", ignoreCase = true) }
    }
    val storeTransactions = remember(clientTransactions) {
        clientTransactions.filter { it.description.startsWith("Venta: ", ignoreCase = true) || it.category.equals("Tienda", ignoreCase = true) }
    }
    val storeProducts = remember(activeProducts) {
        activeProducts.filter { it.category != "Comercio" }
    }
    val deletedStoreProducts = remember(deletedProducts) {
        deletedProducts.filter { it.category != "Comercio" }
    }
    val personalFiadores = remember(clientFiadores) {
        clientFiadores.filter { !it.isStore && it.originMode != "TIENDA" }
    }
    val storeFiadores = remember(clientFiadores) {
        clientFiadores.filter { it.isStore || it.originMode == "TIENDA" }
    }
    val personalCierres = remember(clientCierres) {
        clientCierres.filter { it.mode.contains("PERSONAL", ignoreCase = true) || it.mode.contains("TODOS", ignoreCase = true) }
    }
    val tiendaCierres = remember(clientCierres) {
        clientCierres.filter { it.mode.contains("TIENDA", ignoreCase = true) || it.mode.contains("TODOS", ignoreCase = true) }
    }
    val pedidosCierres = remember(clientCierres) {
        clientCierres.filter { it.mode.contains("PEDIDOS", ignoreCase = true) || it.mode.contains("COMERCIO", ignoreCase = true) || it.mode.contains("TODOS", ignoreCase = true) }
    }

    fun updateBackupData(
        description: String,
        transform: (BackupData) -> BackupData
    ) {
        val email = selectedUserEmail ?: return
        val currentPayload = selectedClientPayload ?: CloudPayload(backups = emptyList())
        val allB = currentPayload.backups?.toMutableList() ?: mutableListOf()
        val latestB = allB.maxByOrNull { it.timestamp }
        val currentData = latestB?.data ?: BackupData(
            transactions = currentPayload.transactions ?: emptyList(),
            reminders = currentPayload.reminders ?: emptyList(),
            fiadores = currentPayload.fiadores ?: emptyList(),
            products = currentPayload.products ?: emptyList(),
            comercioProducts = currentPayload.comercioProducts ?: emptyList(),
            comercioPedidos = currentPayload.comercioPedidos ?: emptyList(),
            comercioMovements = currentPayload.comercioMovements ?: emptyList(),
            cierreSessions = currentPayload.cierreSessions ?: emptyList()
        )

        val updatedData = transform(currentData)

        val newRec = BackupRecord(
            id = UUID.randomUUID().toString(),
            name = description,
            timestamp = System.currentTimeMillis(),
            data = updatedData
        )
        val finalBackups = listOf(newRec) + allB
        val finalPayload = currentPayload.copy(
            backups = finalBackups,
            transactions = updatedData.transactions,
            reminders = updatedData.reminders,
            fiadores = updatedData.fiadores,
            products = updatedData.products,
            comercioProducts = updatedData.comercioProducts,
            comercioPedidos = updatedData.comercioPedidos,
            comercioMovements = updatedData.comercioMovements,
            cierreSessions = updatedData.cierreSessions
        )
        uploadClientBackupPayload(email, finalPayload, description)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF121216),
            border = BorderStroke(1.dp, Color(0xFF2E2E36))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {

                // --- 1. CABECERA: TÍTULO, ESTADO DEL SERVIDOR Y ACCIONES ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🖥️ Backend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isServerOnline == true) Color(0xFF1B5E20) else if (isServerOnline == false) Color(0xFFB71C1C) else Color(0xFF424242)
                        ) {
                            Text(
                                text = if (isServerOnline == true) "● ONLINE" else if (isServerOnline == false) "● OFFLINE" else "● CONECTANDO...",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { refreshAllData() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Recargar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Text(
                    text = "Oracle Cloud VM: http://158.247.123.136:3000",
                    fontSize = 10.sp,
                    color = Color(0xFF9E9E9E)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // --- SELECTOR DE SESIONES AISLADAS (SERVIDOR & DISCO vs CLIENTES & DATOS) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF141418))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Sesión 0: Servidor & Disco
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (mainSection == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { mainSection = 0 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🖥️ Servidor & Disco",
                                fontSize = 12.sp,
                                fontWeight = if (mainSection == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (mainSection == 0) Color.Black else Color(0xFF9E9E9E)
                            )
                        }
                    }

                    // Sesión 1: Clientes & Datos
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (mainSection == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { mainSection = 1 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "👥 Clientes & Datos (${usersMap.size})",
                                fontSize = 12.sp,
                                fontWeight = if (mainSection == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (mainSection == 1) Color.Black else Color(0xFF9E9E9E)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (mainSection == 0) {
                    // ==========================================
                    // SESIÓN 1: HARDWARE, CAPACIDAD Y EXPLORADOR
                    // ==========================================
                    ServerCapacityCard(
                        stats = serverStats,
                        isRefreshing = isRefreshingStats,
                        onRefresh = { refreshStatsOnly() },
                        onExploreDisk = { loadDiskPath("/") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DiskExplorerSection(
                        diskData = diskData,
                        isLoading = isLoadingDisk,
                        error = diskError,
                        onNavigate = { loadDiskPath(it) },
                        onRefresh = { loadDiskPath(diskExplorerPath) }
                    )
                } else {
                    // ==========================================
                    // SESIÓN 2: CLIENTES, CIERRES Y NEGOCIO
                    // ==========================================
                    val currentUser = usersMap[selectedUserEmail]
                    var showEditRoleDialog by remember { mutableStateOf(false) }
                    var newRoleSelected by remember { mutableStateOf(currentUser?.role ?: "INVITADO") }

                    if (showEditRoleDialog && currentUser != null && selectedUserEmail != null) {
                        AlertDialog(
                            onDismissRequest = { showEditRoleDialog = false },
                            title = { Text("Editar Perfil / Rol de Cliente", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Cliente: ${currentUser.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Correo: $selectedUserEmail", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Selecciona el Rol:", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("INVITADO", "INVITADO-GOLD", "GOLD", "ADMIN").forEach { r ->
                                            FilterChip(
                                                selected = newRoleSelected == r,
                                                onClick = { newRoleSelected = r },
                                                label = { Text(r, fontSize = 11.sp, fontWeight = if (newRoleSelected == r) FontWeight.Bold else FontWeight.Normal) }
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val email = selectedUserEmail ?: return@Button
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                RetrofitInstance.api.manageUser(UserManageRequest(email = email, action = "updateRole", role = newRoleSelected))
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Rol actualizado a $newRoleSelected", Toast.LENGTH_SHORT).show()
                                                    refreshAllData()
                                                    showEditRoleDialog = false
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                ) { Text("Guardar Rol") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showEditRoleDialog = false }) { Text("Cancelar") }
                            }
                        )
                    }

                    if (showClientSelector || selectedUserEmail == null) {
                        // Tarjetas métricas de clientes simétricas
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val usersWithBackupCount = remember(backupsSummary, usersMap) {
                                if (backupsSummary.isNotEmpty()) backupsSummary.size
                                else usersMap.size
                            }
                            val onlineCount = remember(usersMap) {
                                val now = System.currentTimeMillis()
                                usersMap.count { (_, u) -> (now - u.lastActive) < 60000L }
                            }

                            Card(
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A20)),
                                border = BorderStroke(1.dp, Color(0xFF2C2C36))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("👥 Clientes", fontSize = 11.sp, color = Color(0xFFAAAAAA), maxLines = 1, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("${usersMap.size}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A20)),
                                border = BorderStroke(1.dp, Color(0xFF2C2C36))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("☁️ Respaldos", fontSize = 11.sp, color = Color(0xFFAAAAAA), maxLines = 1, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("$usersWithBackupCount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A20)),
                                border = BorderStroke(1.dp, Color(0xFF2C2C36))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("🟢 En línea", fontSize = 11.sp, color = Color(0xFFAAAAAA), maxLines = 1, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("$onlineCount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Buscador de clientes simétrico y sin cortes de texto
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF18181E),
                            border = BorderStroke(1.dp, Color(0xFF2E2E38))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                "Buscar cliente por correo o nombre...",
                                                color = Color(0xFF7E7E8E),
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Limpiar",
                                            tint = Color(0xFF9E9E9E),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Carrusel horizontal de clientes
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredUsers.keys.toList()) { email ->
                                val user = filteredUsers[email]
                                val isSelected = email == selectedUserEmail
                                val hasBackup = backupsSummary.any { it.userId.equals(email, ignoreCase = true) }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color(0xFF1E1E22),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF33333A)
                                    ),
                                    modifier = Modifier.clickable {
                                        selectedUserEmail = email
                                        showClientSelector = false
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF3E3E48)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (user?.name?.firstOrNull() ?: 'U').uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isSelected) Color.Black else Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = user?.name ?: email,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                                maxLines = 1
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = email,
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF9E9E9E),
                                                    maxLines = 1
                                                )
                                                if (hasBackup) {
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("☁️", fontSize = 8.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (currentUser != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(
                                    onClick = { showClientSelector = false },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Ocultar selector ▴", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    } else if (currentUser != null) {
                        // Tarjeta compacta del cliente seleccionado con botón de cambiar y editar rol
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showClientSelector = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E)),
                            border = BorderStroke(1.dp, Color(0xFF2C2C32))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (currentUser.name.firstOrNull() ?: 'U').uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = currentUser.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = when (currentUser.role.uppercase()) {
                                                    "ADMIN" -> Color(0xFFB8860B)
                                                    "GOLD" -> Color(0xFFFFD700)
                                                    "INVITADO-GOLD" -> Color(0xFFD4AF37)
                                                    else -> Color(0xFF424242)
                                                },
                                                modifier = Modifier.clickable {
                                                    newRoleSelected = currentUser.role
                                                    showEditRoleDialog = true
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = currentUser.role,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Black
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Icon(Icons.Filled.Edit, contentDescription = "Editar Rol", tint = Color.Black, modifier = Modifier.size(9.dp))
                                                }
                                            }
                                        }
                                        Text(
                                            text = "$selectedUserEmail",
                                            fontSize = 10.sp,
                                            color = Color(0xFF9E9E9E)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable { showClientSelector = true }
                                ) {
                                    Text(
                                        text = "Cambiar ▾",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (currentUser != null) {
                        // Resumen superior interactivo de los 3 Modos y Cierres (Siempre visible y clickeable)
                        val personalNet = personalTransactions.filter { it.isIncome }.sumOf { it.amount } - personalTransactions.filter { !it.isIncome }.sumOf { it.amount }
                        val tiendaStk = storeProducts.sumOf { it.stock }
                        val pedidosStk = clientComercioProducts.sumOf { it.quantityInStock.toInt() }

                        BackendCierresSummaryTopCard(
                            personalSaldo = personalNet,
                            personalTxCount = personalTransactions.size,
                            tiendaStock = tiendaStk,
                            tiendaProdCount = storeProducts.size,
                            pedidosCount = clientPedidos.size,
                            pedidosStock = pedidosStk,
                            cierres = clientCierres,
                            currencyFormat = currencyFormat,
                            onSelectTab = { selectedTab = it }
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Pestañas de Navegación de los 3 Modos Operativos y Cierres
                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            edgePadding = 4.dp,
                            containerColor = Color(0xFF141418),
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("👤 Modo Personal", fontSize = 12.sp, maxLines = 1, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("🏪 Modo Tienda", fontSize = 12.sp, maxLines = 1, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium) }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = { Text("📦 Modo Pedidos", fontSize = 12.sp, maxLines = 1, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium) }
                            )
                            Tab(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                text = { Text("🔒 Cierres (${clientCierres.size})", fontSize = 12.sp, maxLines = 1, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Medium) }
                            )
                            Tab(
                                selected = selectedTab == 4,
                                onClick = { selectedTab = 4 },
                                text = { Text("🗂️ Respaldos & RAW", fontSize = 12.sp, maxLines = 1, fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Medium) }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (isLoadingClientData) {
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            when (selectedTab) {
                                0 -> BackendPersonalModeTab(
                                    transactions = personalTransactions,
                                    fiadores = personalFiadores,
                                    cierres = personalCierres,
                                    currencyFormat = currencyFormat,
                                    dateFormat = dateFormat,
                                    onUpdateData = ::updateBackupData
                                )
                                1 -> BackendTiendaModeTab(
                                    products = storeProducts,
                                    deletedProducts = deletedStoreProducts,
                                    transactions = storeTransactions,
                                    fiadores = storeFiadores,
                                    cierres = tiendaCierres,
                                    currencyFormat = currencyFormat,
                                    dateFormat = dateFormat,
                                    onUpdateData = ::updateBackupData
                                )
                                2 -> BackendPedidosModeTab(
                                    clientPedidos = clientPedidos,
                                    clientComercioProducts = clientComercioProducts,
                                    clientMovements = clientMovements,
                                    cierres = pedidosCierres,
                                    currencyFormat = currencyFormat,
                                    dateFormat = dateFormat,
                                    onUpdateData = ::updateBackupData
                                )
                                3 -> BackendCierresTab(
                                    cierres = clientCierres,
                                    currencyFormat = currencyFormat,
                                    dateFormat = dateFormat,
                                    onUpdateData = ::updateBackupData
                                )
                                4 -> BackendBackupsAndRawTab(
                                    payload = selectedClientPayload,
                                    dateFormat = dateFormat,
                                    gson = gson,
                                    context = context
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("Selecciona un cliente para ver sus datos del servidor", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.BackendProductsTab(
    activeProducts: List<Product>,
    deletedProducts: List<Product>,
    currencyFormat: NumberFormat,
    onUpdateData: (String, (BackupData) -> BackupData) -> Unit
) {
    var productFilter by remember { mutableStateOf("TODOS") }

    var showAddProductDialog by remember { mutableStateOf(false) }
    var newProdName by remember { mutableStateOf("") }
    var newProdStock by remember { mutableStateOf("") }
    var newProdCost by remember { mutableStateOf("") }
    var newProdSale by remember { mutableStateOf("") }
    var newProdUnit by remember { mutableStateOf("Uds") }
    var newProdIsComercio by remember { mutableStateOf(false) }

    var showAdjustStockDialog by remember { mutableStateOf(false) }
    var productToAdjust by remember { mutableStateOf<Product?>(null) }
    var isAddingStock by remember { mutableStateOf(true) }
    var adjustDeltaText by remember { mutableStateOf("1") }

    var showEditProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var editProdName by remember { mutableStateOf("") }
    var editProdUnit by remember { mutableStateOf("") }
    var editProdCost by remember { mutableStateOf("") }
    var editProdSale by remember { mutableStateOf("") }

    var showRetireConfirmDialog by remember { mutableStateOf(false) }
    var productToRetire by remember { mutableStateOf<Product?>(null) }

    // Diálogo para Agregar Producto al Cliente
    if (showAddProductDialog) {
        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("Agregar Producto al Cliente", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newProdName,
                        onValueChange = { newProdName = it },
                        label = { Text("Nombre del producto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = newProdStock,
                            onValueChange = { newProdStock = it.filter { c -> c.isDigit() } },
                            label = { Text("Stock inicial") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newProdUnit,
                            onValueChange = { newProdUnit = it },
                            label = { Text("Unidad") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = newProdCost,
                            onValueChange = { newProdCost = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Costo") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newProdSale,
                            onValueChange = { newProdSale = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Precio Venta") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Módulo:", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = !newProdIsComercio,
                            onClick = { newProdIsComercio = false },
                            label = { Text("🏪 Tienda", fontSize = 11.sp) }
                        )
                        Spacer(Modifier.width(6.dp))
                        FilterChip(
                            selected = newProdIsComercio,
                            onClick = { newProdIsComercio = true },
                            label = { Text("📦 Pedidos", fontSize = 11.sp) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newProdName.trim()
                        val stock = newProdStock.toDoubleOrNull() ?: 0.0
                        val cost = newProdCost.toDoubleOrNull() ?: 0.0
                        val sale = newProdSale.toDoubleOrNull() ?: 0.0
                        val unit = newProdUnit.ifBlank { "Uds" }
                        if (name.isNotEmpty() && sale > 0) {
                            onUpdateData("Producto '$name' agregado con éxito") { cData ->
                                if (newProdIsComercio) {
                                    val cProds = cData.comercioProducts.toMutableList()
                                    val nextId = (cProds.maxOfOrNull { it.id } ?: 0) + 1
                                    val pedId = cData.comercioPedidos.firstOrNull()?.id ?: 1
                                    cProds.add(
                                        ComercioProduct(
                                            id = nextId,
                                            pedidoId = pedId,
                                            name = name,
                                            quantityInStock = stock,
                                            totalPurchased = stock,
                                            costPerUnit = cost,
                                            salePricePerUnit = sale,
                                            unit = unit,
                                            country = "Colombia"
                                        )
                                    )
                                    val cMovs = cData.comercioMovements.toMutableList()
                                    val nextMovId = (cMovs.maxOfOrNull { it.id } ?: 0) + 1
                                    if (stock > 0) {
                                        cMovs.add(
                                            ComercioMovement(
                                                id = nextMovId,
                                                productId = nextId,
                                                productName = name,
                                                type = "COMPRA",
                                                quantity = stock,
                                                pricePerUnit = cost,
                                                total = stock * cost,
                                                note = "Agregado por Admin desde Backend",
                                                country = "Colombia",
                                                timestamp = System.currentTimeMillis()
                                            )
                                        )
                                    }
                                    cData.copy(comercioProducts = cProds, comercioMovements = cMovs)
                                } else {
                                    val sProds = cData.products.toMutableList()
                                    val nextId = (sProds.maxOfOrNull { it.id } ?: 0) + 1
                                    sProds.add(
                                        Product(
                                            id = nextId,
                                            name = name,
                                            stock = stock.toInt(),
                                            purchasePrice = cost,
                                            price = sale,
                                            unit = unit,
                                            category = "General"
                                        )
                                    )
                                    cData.copy(products = sProds)
                                }
                            }
                            showAddProductDialog = false
                            newProdName = ""
                            newProdStock = ""
                            newProdCost = ""
                            newProdSale = ""
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo para Editar Producto
    if (showEditProductDialog && productToEdit != null) {
        val prod = productToEdit!!
        AlertDialog(
            onDismissRequest = { showEditProductDialog = false },
            title = { Text("✏️ Editar Producto", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editProdName,
                        onValueChange = { editProdName = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editProdUnit,
                        onValueChange = { editProdUnit = it },
                        label = { Text("Unidad (Uds, Kg, etc)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = editProdCost,
                            onValueChange = { editProdCost = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Costo") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editProdSale,
                            onValueChange = { editProdSale = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Precio Venta") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = editProdName.trim()
                        val cost = editProdCost.toDoubleOrNull() ?: prod.purchasePrice
                        val sale = editProdSale.toDoubleOrNull() ?: prod.price
                        val unit = editProdUnit.ifBlank { prod.unit }
                        if (name.isNotEmpty()) {
                            onUpdateData("Producto '$name' editado") { cData ->
                                if (prod.category == "Comercio") {
                                    val cProds = cData.comercioProducts.map { cp ->
                                        if (cp.id == prod.id || cp.name.equals(prod.name, ignoreCase = true)) {
                                            cp.copy(name = name, costPerUnit = cost, salePricePerUnit = sale, unit = unit)
                                        } else cp
                                    }
                                    cData.copy(comercioProducts = cProds)
                                } else {
                                    val sProds = cData.products.map { p ->
                                        if (p.id == prod.id || p.name.equals(prod.name, ignoreCase = true)) {
                                            p.copy(name = name, purchasePrice = cost, price = sale, unit = unit)
                                        } else p
                                    }
                                    cData.copy(products = sProds)
                                }
                            }
                            showEditProductDialog = false
                        }
                    }
                ) { Text("Guardar Cambios") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProductDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo para Aumentar / Retirar Stock
    if (showAdjustStockDialog && productToAdjust != null) {
        val prod = productToAdjust!!
        AlertDialog(
            onDismissRequest = { showAdjustStockDialog = false },
            title = {
                Text(
                    text = if (isAddingStock) "➕ Aumentar Stock" else "➖ Retirar Stock",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Producto: ${prod.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Stock actual: ${prod.stock} ${prod.unit}", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = adjustDeltaText,
                        onValueChange = { adjustDeltaText = it.filter { c -> c.isDigit() } },
                        label = { Text(if (isAddingStock) "Unidades a ingresar" else "Unidades a retirar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val units = adjustDeltaText.toIntOrNull() ?: 0
                        if (units > 0) {
                            val delta = if (isAddingStock) units else -units
                            onUpdateData("Ajuste Stock (${if (delta > 0) "+$delta" else "$delta"}) - ${prod.name}") { cData ->
                                if (prod.category == "Comercio") {
                                    val cProds = cData.comercioProducts.map { cp ->
                                        if (cp.id == prod.id || cp.name.equals(prod.name, ignoreCase = true)) {
                                            val newSt = (cp.quantityInStock + delta).coerceAtLeast(0.0)
                                            val newPur = if (delta > 0) cp.totalPurchased + delta else cp.totalPurchased
                                            cp.copy(quantityInStock = newSt, totalPurchased = newPur)
                                        } else cp
                                    }
                                    val cMovs = cData.comercioMovements.toMutableList()
                                    val nextMovId = (cMovs.maxOfOrNull { it.id } ?: 0) + 1
                                    cMovs.add(
                                        ComercioMovement(
                                            id = nextMovId,
                                            productId = prod.id,
                                            productName = prod.name,
                                            type = if (delta > 0) "COMPRA" else "AJUSTE",
                                            quantity = Math.abs(delta).toDouble(),
                                            pricePerUnit = prod.purchasePrice,
                                            total = Math.abs(delta) * prod.purchasePrice,
                                            note = if (delta > 0) "Aumento de stock por Admin" else "Retiro de stock por Admin",
                                            country = "Colombia",
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                    cData.copy(comercioProducts = cProds, comercioMovements = cMovs)
                                } else {
                                    val sProds = cData.products.map { p ->
                                        if (p.id == prod.id || p.name.equals(prod.name, ignoreCase = true)) {
                                            val newSt = (p.stock + delta).coerceAtLeast(0)
                                            p.copy(stock = newSt)
                                        } else p
                                    }
                                    cData.copy(products = sProds)
                                }
                            }
                            showAdjustStockDialog = false
                        }
                    }
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustStockDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo de Confirmación para Retirar Producto por Completo
    if (showRetireConfirmDialog && productToRetire != null) {
        val prod = productToRetire!!
        AlertDialog(
            onDismissRequest = { showRetireConfirmDialog = false },
            title = { Text("⚠️ Retirar Producto del Cliente", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text("¿Deseas dar de baja y retirar definitivamente el producto '${prod.name}' del catálogo de este cliente en el servidor?")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    onClick = {
                        onUpdateData("Producto '${prod.name}' retirado con éxito") { cData ->
                            if (prod.category == "Comercio") {
                                val cProds = cData.comercioProducts.filterNot { it.id == prod.id || it.name.equals(prod.name, ignoreCase = true) }
                                cData.copy(comercioProducts = cProds)
                            } else {
                                val sProds = cData.products.filterNot { it.id == prod.id || it.name.equals(prod.name, ignoreCase = true) }
                                cData.copy(products = sProds)
                            }
                        }
                        showRetireConfirmDialog = false
                    }
                ) { Text("Retirar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showRetireConfirmDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = productFilter == "TODOS",
            onClick = { productFilter = "TODOS" },
            label = { Text("Todos (${activeProducts.size + deletedProducts.size})", fontSize = 11.sp) }
        )
        FilterChip(
            selected = productFilter == "ACTIVOS",
            onClick = { productFilter = "ACTIVOS" },
            label = { Text("Activos (${activeProducts.size})", fontSize = 11.sp) }
        )
        FilterChip(
            selected = productFilter == "ELIMINADOS",
            onClick = { productFilter = "ELIMINADOS" },
            label = {
                Text(
                    "⚠️ Eliminados (${deletedProducts.size})",
                    fontSize = 11.sp,
                    color = if (deletedProducts.isNotEmpty()) Color(0xFFFF5252) else Color.Gray
                )
            }
        )
        Button(
            onClick = { showAddProductDialog = true },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
            Spacer(Modifier.width(4.dp))
            Text("Agregar Producto", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }

    val displayedProducts = when (productFilter) {
        "ACTIVOS" -> activeProducts.map { it to false }
        "ELIMINADOS" -> deletedProducts.map { it to true }
        else -> activeProducts.map { it to false } + deletedProducts.map { it to true }
    }

    if (displayedProducts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay productos para mostrar en este filtro", color = Color.Gray, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(displayedProducts) { (p, isDeleted) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDeleted) Color(0xFF261214) else Color(0xFF1E1E22)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isDeleted) Color(0xFFD32F2F).copy(alpha = 0.7f) else Color(0xFF33333A)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (isDeleted) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFD32F2F)) {
                                        Text("ELIMINADO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), softWrap = false)
                                    }
                                }
                            }
                            Text(
                                text = "Stock: ${p.stock} ${p.unit} | Costo: ${currencyFormat.format(p.purchasePrice)}",
                                fontSize = 11.sp,
                                color = Color(0xFFB0B0B5),
                                softWrap = false
                            )
                            Text(
                                text = "Precio: ${currencyFormat.format(p.price)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                softWrap = false
                            )
                        }

                        // Acciones rápidas para el Admin en el Backend
                        if (!isDeleted) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1B5E20),
                                    modifier = Modifier.clickable {
                                        productToAdjust = p
                                        adjustDeltaText = "1"
                                        isAddingStock = true
                                        showAdjustStockDialog = true
                                    }
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Add, contentDescription = "Aumentar Stock", tint = Color.White, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text("Stock", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, softWrap = false)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.clickable {
                                        productToAdjust = p
                                        adjustDeltaText = "1"
                                        isAddingStock = false
                                        showAdjustStockDialog = true
                                    }
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Remove, contentDescription = "Retirar Unidades", tint = Color.White, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text("Retirar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, softWrap = false)
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        productToEdit = p
                                        editProdName = p.name
                                        editProdUnit = p.unit
                                        editProdCost = p.purchasePrice.toString()
                                        editProdSale = p.price.toString()
                                        showEditProductDialog = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color(0xFF64B5F6), modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        productToRetire = p
                                        showRetireConfirmDialog = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Dar de Baja", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.BackendTransactionsTab(
    clientTransactions: List<Transaction>,
    currencyFormat: NumberFormat,
    dateFormat: SimpleDateFormat,
    onUpdateData: (String, (BackupData) -> BackupData) -> Unit
) {
    val totalIncome = clientTransactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = clientTransactions.filter { !it.isIncome }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    var showAddTxDialog by remember { mutableStateOf(false) }
    var txDesc by remember { mutableStateOf("") }
    var txAmount by remember { mutableStateOf("") }
    var txIsIncome by remember { mutableStateOf(true) }
    var txIsCash by remember { mutableStateOf(true) }
    var txNote by remember { mutableStateOf("") }

    var showEditTxDialog by remember { mutableStateOf(false) }
    var txToEdit by remember { mutableStateOf<Transaction?>(null) }
    var editTxDesc by remember { mutableStateOf("") }
    var editTxAmount by remember { mutableStateOf("") }
    var editTxIsIncome by remember { mutableStateOf(true) }
    var editTxIsCash by remember { mutableStateOf(true) }
    var editTxNote by remember { mutableStateOf("") }

    var showDeleteTxDialog by remember { mutableStateOf(false) }
    var txToDelete by remember { mutableStateOf<Transaction?>(null) }

    // Dialog: Agregar Transacción
    if (showAddTxDialog) {
        AlertDialog(
            onDismissRequest = { showAddTxDialog = false },
            title = { Text("Nueva Transacción", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = txIsIncome,
                            onClick = { txIsIncome = true },
                            label = { Text("🟢 Ingreso", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = !txIsIncome,
                            onClick = { txIsIncome = false },
                            label = { Text("🔴 Gasto / Egreso", fontSize = 11.sp) }
                        )
                    }
                    OutlinedTextField(
                        value = txDesc,
                        onValueChange = { txDesc = it },
                        label = { Text("Descripción") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = txAmount,
                        onValueChange = { txAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Monto ($)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Método:", fontSize = 12.sp, color = Color.Gray)
                        FilterChip(
                            selected = txIsCash,
                            onClick = { txIsCash = true },
                            label = { Text("Efectivo", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = !txIsCash,
                            onClick = { txIsCash = false },
                            label = { Text("Digital / Bancario", fontSize = 11.sp) }
                        )
                    }
                    OutlinedTextField(
                        value = txNote,
                        onValueChange = { txNote = it },
                        label = { Text("Nota opcional") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val desc = txDesc.trim()
                        val amt = txAmount.toDoubleOrNull() ?: 0.0
                        if (desc.isNotEmpty() && amt > 0.0) {
                            onUpdateData("Transacción agregada: $desc") { cData ->
                                val nextId = (cData.transactions.maxOfOrNull { it.id } ?: 0) + 1
                                val newTx = Transaction(
                                    id = nextId,
                                    description = desc,
                                    amount = amt,
                                    isIncome = txIsIncome,
                                    cashAmount = if (txIsCash) amt else 0.0,
                                    digitalAmount = if (!txIsCash) amt else 0.0,
                                    note = txNote.trim(),
                                    timestamp = System.currentTimeMillis()
                                )
                                cData.copy(transactions = cData.transactions + newTx)
                            }
                            showAddTxDialog = false
                            txDesc = ""
                            txAmount = ""
                            txNote = ""
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTxDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Editar Transacción
    if (showEditTxDialog && txToEdit != null) {
        val t = txToEdit!!
        AlertDialog(
            onDismissRequest = { showEditTxDialog = false },
            title = { Text("✏️ Editar Transacción", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = editTxIsIncome,
                            onClick = { editTxIsIncome = true },
                            label = { Text("🟢 Ingreso", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = !editTxIsIncome,
                            onClick = { editTxIsIncome = false },
                            label = { Text("🔴 Gasto / Egreso", fontSize = 11.sp) }
                        )
                    }
                    OutlinedTextField(
                        value = editTxDesc,
                        onValueChange = { editTxDesc = it },
                        label = { Text("Descripción") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTxAmount,
                        onValueChange = { editTxAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Monto ($)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Método:", fontSize = 12.sp, color = Color.Gray)
                        FilterChip(
                            selected = editTxIsCash,
                            onClick = { editTxIsCash = true },
                            label = { Text("Efectivo", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = !editTxIsCash,
                            onClick = { editTxIsCash = false },
                            label = { Text("Digital / Bancario", fontSize = 11.sp) }
                        )
                    }
                    OutlinedTextField(
                        value = editTxNote,
                        onValueChange = { editTxNote = it },
                        label = { Text("Nota") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val desc = editTxDesc.trim()
                        val amt = editTxAmount.toDoubleOrNull() ?: t.amount
                        if (desc.isNotEmpty() && amt > 0.0) {
                            onUpdateData("Transacción editada: $desc") { cData ->
                                val updated = cData.transactions.map { item ->
                                    if (item.id == t.id) {
                                        item.copy(
                                            description = desc,
                                            amount = amt,
                                            isIncome = editTxIsIncome,
                                            cashAmount = if (editTxIsCash) amt else 0.0,
                                            digitalAmount = if (!editTxIsCash) amt else 0.0,
                                            note = editTxNote.trim()
                                        )
                                    } else item
                                }
                                cData.copy(transactions = updated)
                            }
                            showEditTxDialog = false
                        }
                    }
                ) { Text("Guardar Cambios") }
            },
            dismissButton = {
                TextButton(onClick = { showEditTxDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Eliminar Transacción
    if (showDeleteTxDialog && txToDelete != null) {
        val t = txToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteTxDialog = false },
            title = { Text("🗑️ Eliminar Transacción", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("¿Estás seguro de eliminar la transacción '${t.description}' por ${currencyFormat.format(t.amount)}?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    onClick = {
                        onUpdateData("Transacción eliminada: ${t.description}") { cData ->
                            cData.copy(transactions = cData.transactions.filterNot { it.id == t.id })
                        }
                        showDeleteTxDialog = false
                    }
                ) { Text("Eliminar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTxDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF182218)),
            border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                Text("Ingresos", fontSize = 9.sp, color = Color(0xFF81C784))
                Text(currencyFormat.format(totalIncome), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), softWrap = false)
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF251818)),
            border = BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                Text("Egresos", fontSize = 9.sp, color = Color(0xFFE57373))
                Text(currencyFormat.format(totalExpense), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935), softWrap = false)
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
            border = BorderStroke(1.dp, Color(0xFF3F51B5).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                Text("Balance", fontSize = 9.sp, color = Color(0xFF90CAF9))
                Text(currencyFormat.format(balance), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (balance >= 0) MaterialTheme.colorScheme.primary else Color(0xFFE53935), softWrap = false)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Transacciones (${clientTransactions.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Button(
            onClick = { showAddTxDialog = true },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(30.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.Black)
            Spacer(Modifier.width(3.dp))
            Text("Nueva Transacción", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }

    if (clientTransactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay transacciones registradas en el servidor", color = Color.Gray, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(clientTransactions) { t ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                    border = BorderStroke(1.dp, Color(0xFF2C2C32))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                            Text(t.description, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val tDate = dateFormat.format(Date(t.timestamp))
                            Text("Fecha: $tDate | Método: ${if (t.cashAmount > 0) "Efectivo" else "Digital"}", fontSize = 10.sp, color = Color(0xFF9E9E9E))
                            if (t.note.isNotBlank()) {
                                Text("Nota: ${t.note}", fontSize = 10.sp, color = Color(0xFFB0B0B5), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = (if (t.isIncome) "+" else "-") + currencyFormat.format(t.amount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (t.isIncome) Color(0xFF4CAF50) else Color(0xFFE53935),
                                softWrap = false,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            IconButton(
                                onClick = {
                                    txToEdit = t
                                    editTxDesc = t.description
                                    editTxAmount = t.amount.toString()
                                    editTxIsIncome = t.isIncome
                                    editTxIsCash = t.cashAmount > 0
                                    editTxNote = t.note
                                    showEditTxDialog = true
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color(0xFF64B5F6), modifier = Modifier.size(15.dp))
                            }
                            IconButton(
                                onClick = {
                                    txToDelete = t
                                    showDeleteTxDialog = true
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF5252), modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.BackendPedidosTab(
    clientPedidos: List<ComercioPedido>,
    clientComercioProducts: List<ComercioProduct>,
    clientMovements: List<ComercioMovement>,
    currencyFormat: NumberFormat,
    dateFormat: SimpleDateFormat,
    onUpdateData: (String, (BackupData) -> BackupData) -> Unit
) {
    var showAddPedidoDialog by remember { mutableStateOf(false) }
    var newPedidoName by remember { mutableStateOf("") }
    var newPedidoCountry by remember { mutableStateOf("Colombia") }

    var showEditPedidoDialog by remember { mutableStateOf(false) }
    var pedidoToEdit by remember { mutableStateOf<ComercioPedido?>(null) }
    var editPedidoName by remember { mutableStateOf("") }
    var editPedidoCountry by remember { mutableStateOf("Colombia") }

    var showDeletePedidoDialog by remember { mutableStateOf(false) }
    var pedidoToDelete by remember { mutableStateOf<ComercioPedido?>(null) }

    var showAddProdToPedDialog by remember { mutableStateOf(false) }
    var targetPedidoForProd by remember { mutableStateOf<ComercioPedido?>(null) }
    var newProdName by remember { mutableStateOf("") }
    var newProdQty by remember { mutableStateOf("") }
    var newProdCost by remember { mutableStateOf("") }
    var newProdSale by remember { mutableStateOf("") }
    var newProdUnit by remember { mutableStateOf("Uds") }

    var showAdjustStockDialog by remember { mutableStateOf(false) }
    var prodToAdjust by remember { mutableStateOf<ComercioProduct?>(null) }
    var isAddingStock by remember { mutableStateOf(true) }
    var adjustDeltaText by remember { mutableStateOf("1") }

    var showEditProdDialog by remember { mutableStateOf(false) }
    var prodToEdit by remember { mutableStateOf<ComercioProduct?>(null) }
    var editProdName by remember { mutableStateOf("") }
    var editProdUnit by remember { mutableStateOf("Uds") }
    var editProdCost by remember { mutableStateOf("") }
    var editProdSale by remember { mutableStateOf("") }

    var showDeleteProdDialog by remember { mutableStateOf(false) }
    var prodToDelete by remember { mutableStateOf<ComercioProduct?>(null) }

    var showDeleteMovDialog by remember { mutableStateOf(false) }
    var movToDelete by remember { mutableStateOf<ComercioMovement?>(null) }

    // Dialog: Crear Pedido
    if (showAddPedidoDialog) {
        AlertDialog(
            onDismissRequest = { showAddPedidoDialog = false },
            title = { Text("📦 Crear Nuevo Pedido", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPedidoName,
                        onValueChange = { newPedidoName = it },
                        label = { Text("Nombre del pedido (ej. Lapiceros Septiembre)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPedidoCountry,
                        onValueChange = { newPedidoCountry = it },
                        label = { Text("País / Destino") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newPedidoName.trim()
                        if (name.isNotEmpty()) {
                            onUpdateData("Nuevo Pedido: $name") { cData ->
                                val nextId = (cData.comercioPedidos.maxOfOrNull { it.id } ?: 0) + 1
                                val newPed = ComercioPedido(
                                    id = nextId,
                                    name = name,
                                    country = newPedidoCountry.ifBlank { "Colombia" },
                                    timestamp = System.currentTimeMillis()
                                )
                                cData.copy(comercioPedidos = cData.comercioPedidos + newPed)
                            }
                            showAddPedidoDialog = false
                            newPedidoName = ""
                        }
                    }
                ) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { showAddPedidoDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Editar Pedido
    if (showEditPedidoDialog && pedidoToEdit != null) {
        val ped = pedidoToEdit!!
        AlertDialog(
            onDismissRequest = { showEditPedidoDialog = false },
            title = { Text("✏️ Editar Pedido", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editPedidoName,
                        onValueChange = { editPedidoName = it },
                        label = { Text("Nombre del pedido") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPedidoCountry,
                        onValueChange = { editPedidoCountry = it },
                        label = { Text("País") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = editPedidoName.trim()
                        if (name.isNotEmpty()) {
                            onUpdateData("Pedido editado: $name") { cData ->
                                val updated = cData.comercioPedidos.map {
                                    if (it.id == ped.id) it.copy(name = name, country = editPedidoCountry.ifBlank { "Colombia" }) else it
                                }
                                cData.copy(comercioPedidos = updated)
                            }
                            showEditPedidoDialog = false
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showEditPedidoDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Eliminar Pedido
    if (showDeletePedidoDialog && pedidoToDelete != null) {
        val ped = pedidoToDelete!!
        AlertDialog(
            onDismissRequest = { showDeletePedidoDialog = false },
            title = { Text("🗑️ Eliminar Pedido", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("¿Deseas eliminar el pedido '${ped.name}' junto con sus productos y registros?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    onClick = {
                        onUpdateData("Pedido eliminado: ${ped.name}") { cData ->
                            val prodsToRemove = cData.comercioProducts.filter { it.pedidoId == ped.id }.map { it.id }.toSet()
                            cData.copy(
                                comercioPedidos = cData.comercioPedidos.filterNot { it.id == ped.id },
                                comercioProducts = cData.comercioProducts.filterNot { it.pedidoId == ped.id },
                                comercioMovements = cData.comercioMovements.filterNot { prodsToRemove.contains(it.productId) }
                            )
                        }
                        showDeletePedidoDialog = false
                    }
                ) { Text("Eliminar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePedidoDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Agregar Producto al Pedido
    if (showAddProdToPedDialog && targetPedidoForProd != null) {
        val ped = targetPedidoForProd!!
        AlertDialog(
            onDismissRequest = { showAddProdToPedDialog = false },
            title = { Text("➕ Agregar Producto a '${ped.name}'", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newProdName,
                        onValueChange = { newProdName = it },
                        label = { Text("Nombre del producto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = newProdQty,
                            onValueChange = { newProdQty = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Cantidad comprada") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newProdUnit,
                            onValueChange = { newProdUnit = it },
                            label = { Text("Unidad") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = newProdCost,
                            onValueChange = { newProdCost = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Costo Unitario ($)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newProdSale,
                            onValueChange = { newProdSale = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Precio Venta ($)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newProdName.trim()
                        val qty = newProdQty.toDoubleOrNull() ?: 0.0
                        val cost = newProdCost.toDoubleOrNull() ?: 0.0
                        val sale = newProdSale.toDoubleOrNull() ?: 0.0
                        val unit = newProdUnit.ifBlank { "Uds" }
                        if (name.isNotEmpty() && qty > 0.0) {
                            onUpdateData("Producto '$name' agregado al pedido ${ped.name}") { cData ->
                                val nextProdId = (cData.comercioProducts.maxOfOrNull { it.id } ?: 0) + 1
                                val newP = ComercioProduct(
                                    id = nextProdId,
                                    pedidoId = ped.id,
                                    name = name,
                                    unit = unit,
                                    quantityInStock = qty,
                                    totalPurchased = qty,
                                    costPerUnit = cost,
                                    salePricePerUnit = sale,
                                    country = ped.country
                                )
                                val cMovs = cData.comercioMovements.toMutableList()
                                val nextMovId = (cMovs.maxOfOrNull { it.id } ?: 0) + 1
                                cMovs.add(
                                    ComercioMovement(
                                        id = nextMovId,
                                        productId = nextProdId,
                                        productName = name,
                                        type = "COMPRA",
                                        quantity = qty,
                                        pricePerUnit = cost,
                                        total = qty * cost,
                                        note = "Ingreso inicial de mercancía",
                                        country = ped.country,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                                cData.copy(
                                    comercioProducts = cData.comercioProducts + newP,
                                    comercioMovements = cMovs
                                )
                            }
                            showAddProdToPedDialog = false
                            newProdName = ""
                            newProdQty = ""
                            newProdCost = ""
                            newProdSale = ""
                        }
                    }
                ) { Text("Guardar Producto") }
            },
            dismissButton = {
                TextButton(onClick = { showAddProdToPedDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Ajustar Stock de Producto de Comercio (+ / -)
    if (showAdjustStockDialog && prodToAdjust != null) {
        val prod = prodToAdjust!!
        AlertDialog(
            onDismissRequest = { showAdjustStockDialog = false },
            title = { Text(if (isAddingStock) "➕ Ingresar Stock" else "➖ Retirar Stock", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Producto: ${prod.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Stock actual: ${prod.quantityInStock.toInt()} ${prod.unit} (Total comprado: ${prod.totalPurchased.toInt()})", fontSize = 11.sp, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = adjustDeltaText,
                        onValueChange = { adjustDeltaText = it.filter { c -> c.isDigit() } },
                        label = { Text(if (isAddingStock) "Unidades a ingresar" else "Unidades a retirar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val units = adjustDeltaText.toIntOrNull() ?: 0
                        if (units > 0) {
                            val delta = if (isAddingStock) units else -units
                            onUpdateData("Stock ${prod.name} (${if (delta > 0) "+$delta" else "$delta"})") { cData ->
                                val nextMovId = (cData.comercioMovements.maxOfOrNull { it.id } ?: 0) + 1
                                val newMov = ComercioMovement(
                                    id = nextMovId,
                                    productId = prod.id,
                                    productName = prod.name,
                                    type = if (delta > 0) "COMPRA" else "AJUSTE",
                                    quantity = Math.abs(delta).toDouble(),
                                    pricePerUnit = prod.costPerUnit,
                                    total = Math.abs(delta) * prod.costPerUnit,
                                    note = if (delta > 0) "Aumento de stock por Admin" else "Retiro de mercancía por Admin",
                                    country = prod.country,
                                    timestamp = System.currentTimeMillis()
                                )
                                val updatedProds = cData.comercioProducts.map { cp ->
                                    if (cp.id == prod.id) {
                                        val newSt = (cp.quantityInStock + delta).coerceAtLeast(0.0)
                                        val newPur = if (delta > 0) cp.totalPurchased + delta else cp.totalPurchased
                                        cp.copy(quantityInStock = newSt, totalPurchased = newPur)
                                    } else cp
                                }
                                cData.copy(comercioProducts = updatedProds, comercioMovements = cData.comercioMovements + newMov)
                            }
                            showAdjustStockDialog = false
                        }
                    }
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustStockDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Editar Producto de Comercio
    if (showEditProdDialog && prodToEdit != null) {
        val prod = prodToEdit!!
        AlertDialog(
            onDismissRequest = { showEditProdDialog = false },
            title = { Text("✏️ Editar Producto", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editProdName,
                        onValueChange = { editProdName = it },
                        label = { Text("Nombre del producto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editProdUnit,
                        onValueChange = { editProdUnit = it },
                        label = { Text("Unidad") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = editProdCost,
                            onValueChange = { editProdCost = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Costo ($)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editProdSale,
                            onValueChange = { editProdSale = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Precio Venta ($)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = editProdName.trim()
                        val cost = editProdCost.toDoubleOrNull() ?: prod.costPerUnit
                        val sale = editProdSale.toDoubleOrNull() ?: prod.salePricePerUnit
                        val unit = editProdUnit.ifBlank { prod.unit }
                        if (name.isNotEmpty()) {
                            onUpdateData("Producto editado: $name") { cData ->
                                val updated = cData.comercioProducts.map {
                                    if (it.id == prod.id) it.copy(name = name, costPerUnit = cost, salePricePerUnit = sale, unit = unit) else it
                                }
                                cData.copy(comercioProducts = updated)
                            }
                            showEditProdDialog = false
                        }
                    }
                ) { Text("Guardar Cambios") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProdDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Eliminar Producto
    if (showDeleteProdDialog && prodToDelete != null) {
        val prod = prodToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteProdDialog = false },
            title = { Text("🗑️ Eliminar Producto", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("¿Deseas eliminar definitivamente el producto '${prod.name}' de este pedido?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    onClick = {
                        onUpdateData("Producto eliminado: ${prod.name}") { cData ->
                            cData.copy(
                                comercioProducts = cData.comercioProducts.filterNot { it.id == prod.id },
                                comercioMovements = cData.comercioMovements.filterNot { it.productId == prod.id }
                            )
                        }
                        showDeleteProdDialog = false
                    }
                ) { Text("Eliminar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteProdDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Eliminar Movimiento
    if (showDeleteMovDialog && movToDelete != null) {
        val mov = movToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteMovDialog = false },
            title = { Text("🗑️ Eliminar Movimiento", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("¿Deseas eliminar el movimiento '${mov.type} - ${mov.productName}' (${mov.quantity} uds)?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    onClick = {
                        onUpdateData("Movimiento #${mov.id} eliminado") { cData ->
                            cData.copy(comercioMovements = cData.comercioMovements.filterNot { it.id == mov.id })
                        }
                        showDeleteMovDialog = false
                    }
                ) { Text("Eliminar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMovDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Header de Pedidos
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Pedidos de Comercio (${clientPedidos.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Button(
            onClick = { showAddPedidoDialog = true },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(30.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.Black)
            Spacer(Modifier.width(3.dp))
            Text("Nuevo Pedido", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }

    val displayPedidos = if (clientPedidos.isEmpty() && clientComercioProducts.isNotEmpty()) {
        listOf(ComercioPedido(id = 1, name = "Pedido Principal", country = "Colombia"))
    } else {
        clientPedidos
    }

    if (displayPedidos.isEmpty() && clientMovements.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No hay pedidos de comercio guardados en el servidor", color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showAddPedidoDialog = true }) {
                    Text("Crear Primer Pedido")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayPedidos) { ped ->
                val prods = clientComercioProducts.filter { it.pedidoId == ped.id || (displayPedidos.size == 1 && it.pedidoId == 0) }
                val prodIds = prods.map { it.id }.toSet()
                val orderMovs = clientMovements.filter { prodIds.contains(it.productId) }

                val totalItemsStock = prods.sumOf { it.quantityInStock }
                val totalItemsPurchased = prods.sumOf { it.totalPurchased }
                val totalCostPurchased = prods.sumOf { it.costPerUnit * it.totalPurchased }
                val totalPotentialSales = prods.sumOf { it.salePricePerUnit * it.quantityInStock }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B20)),
                    border = BorderStroke(1.dp, Color(0xFF33333E))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        // Cabecera del Pedido
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                Text(
                                    ped.name.ifBlank { "Pedido #${ped.id}" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFFFD700)
                                )
                                Text(
                                    "Destino: ${ped.country} | Fecha: ${dateFormat.format(Date(ped.timestamp))}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        pedidoToEdit = ped
                                        editPedidoName = ped.name
                                        editPedidoCountry = ped.country
                                        showEditPedidoDialog = true
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color(0xFF64B5F6), modifier = Modifier.size(15.dp))
                                }
                                IconButton(
                                    onClick = {
                                        pedidoToDelete = ped
                                        showDeletePedidoDialog = true
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF5252), modifier = Modifier.size(15.dp))
                                }
                            }
                        }

                        // Métricas del Pedido
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF141418),
                            border = BorderStroke(1.dp, Color(0xFF26262E)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Stock / Comprado", fontSize = 9.sp, color = Color(0xFF9E9E9E))
                                    Text("${totalItemsStock.toInt()} / ${totalItemsPurchased.toInt()} Uds", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Inversión Inicial", fontSize = 9.sp, color = Color(0xFF9E9E9E))
                                    Text(currencyFormat.format(totalCostPurchased), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Valor en Stock", fontSize = 9.sp, color = Color(0xFF9E9E9E))
                                    Text(currencyFormat.format(totalPotentialSales), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                }
                            }
                        }

                        // Sección de Productos del Pedido
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📦 Productos (${prods.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            TextButton(
                                onClick = {
                                    targetPedidoForProd = ped
                                    newProdName = ""
                                    newProdQty = ""
                                    newProdCost = ""
                                    newProdSale = ""
                                    showAddProdToPedDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("Agregar Producto", fontSize = 10.sp)
                            }
                        }

                        if (prods.isEmpty()) {
                            Text("No hay productos asignados a este pedido", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                prods.forEach { cp ->
                                    Card(
                                        shape = RoundedCornerShape(6.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF222228)),
                                        border = BorderStroke(1.dp, Color(0xFF2E2E38)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                                Text(cp.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                                Text(
                                                    "Stock: ${cp.quantityInStock.toInt()} / ${cp.totalPurchased.toInt()} ${cp.unit} | Costo: ${currencyFormat.format(cp.costPerUnit)}",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFFB0B0B5)
                                                )
                                                Text(
                                                    "Venta: ${currencyFormat.format(cp.salePricePerUnit)} (Vendidos: ${cp.totalSold.toInt()})",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF1B5E20),
                                                    modifier = Modifier.clickable {
                                                        prodToAdjust = cp
                                                        adjustDeltaText = "1"
                                                        isAddingStock = true
                                                        showAdjustStockDialog = true
                                                    }
                                                ) {
                                                    Row(modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                                        Text("Stock", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFE65100),
                                                    modifier = Modifier.clickable {
                                                        prodToAdjust = cp
                                                        adjustDeltaText = "1"
                                                        isAddingStock = false
                                                        showAdjustStockDialog = true
                                                    }
                                                ) {
                                                    Row(modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Filled.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                                        Text("Retirar", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                                IconButton(
                                                    onClick = {
                                                        prodToEdit = cp
                                                        editProdName = cp.name
                                                        editProdUnit = cp.unit
                                                        editProdCost = cp.costPerUnit.toString()
                                                        editProdSale = cp.salePricePerUnit.toString()
                                                        showEditProdDialog = true
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color(0xFF64B5F6), modifier = Modifier.size(13.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        prodToDelete = cp
                                                        showDeleteProdDialog = true
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF5252), modifier = Modifier.size(13.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Sección de Movimientos del Pedido
                        if (orderMovs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("📋 Historial de Movimientos (${orderMovs.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCE93D8))
                            Spacer(modifier = Modifier.height(3.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                orderMovs.takeLast(10).reversed().forEach { mov ->
                                    val isInc = mov.type == "VENTA" || mov.type == "IN"
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF16161A),
                                        border = BorderStroke(1.dp, Color(0xFF26262E)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(modifier = Modifier.weight(1f).padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(3.dp),
                                                    color = if (isInc) Color(0xFF1B5E20) else Color(0xFF424242)
                                                ) {
                                                    Text(
                                                        mov.type,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    "${mov.productName} (${mov.quantity} uds) - ${currencyFormat.format(mov.total)}",
                                                    fontSize = 10.sp,
                                                    color = if (isInc) Color(0xFF81C784) else Color(0xFFB0B0B5),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(dateFormat.format(Date(mov.timestamp)), fontSize = 9.sp, color = Color(0xFF757575))
                                                IconButton(
                                                    onClick = {
                                                        movToDelete = mov
                                                        showDeleteMovDialog = true
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF5252), modifier = Modifier.size(11.dp))
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
    }
}

@Composable
private fun ColumnScope.BackendFiadoresTab(
    clientFiadores: List<Fiador>,
    currencyFormat: NumberFormat,
    dateFormat: SimpleDateFormat,
    onUpdateData: (String, (BackupData) -> BackupData) -> Unit
) {
    var showAddFiadorDialog by remember { mutableStateOf(false) }
    var newFiadorName by remember { mutableStateOf("") }
    var newFiadorAmount by remember { mutableStateOf("") }
    var newFiadorReason by remember { mutableStateOf("") }
    var newFiadorPhone by remember { mutableStateOf("") }

    var showAbonoDialog by remember { mutableStateOf(false) }
    var fiadorForAbono by remember { mutableStateOf<Fiador?>(null) }
    var abonoAmountText by remember { mutableStateOf("") }

    var showEditFiadorDialog by remember { mutableStateOf(false) }
    var fiadorToEdit by remember { mutableStateOf<Fiador?>(null) }
    var editFiadorName by remember { mutableStateOf("") }
    var editFiadorAmount by remember { mutableStateOf("") }
    var editFiadorReason by remember { mutableStateOf("") }
    var editFiadorPhone by remember { mutableStateOf("") }

    var showDeleteFiadorDialog by remember { mutableStateOf(false) }
    var fiadorToDelete by remember { mutableStateOf<Fiador?>(null) }

    // Dialog: Agregar Fiador
    if (showAddFiadorDialog) {
        AlertDialog(
            onDismissRequest = { showAddFiadorDialog = false },
            title = { Text("👥 Nuevo Fiador / Deuda", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newFiadorName,
                        onValueChange = { newFiadorName = it },
                        label = { Text("Nombre del cliente fiador") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newFiadorAmount,
                        onValueChange = { newFiadorAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Monto de la deuda ($)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newFiadorReason,
                        onValueChange = { newFiadorReason = it },
                        label = { Text("Motivo / Concepto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newFiadorPhone,
                        onValueChange = { newFiadorPhone = it },
                        label = { Text("Teléfono de contacto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newFiadorName.trim()
                        val amt = newFiadorAmount.toDoubleOrNull() ?: 0.0
                        if (name.isNotEmpty() && amt > 0.0) {
                            onUpdateData("Fiador registrado: $name") { cData ->
                                val nextId = (cData.fiadores.maxOfOrNull { it.id } ?: 0) + 1
                                val newF = Fiador(
                                    id = nextId,
                                    name = name,
                                    amount = amt,
                                    paidAmount = 0.0,
                                    phone = newFiadorPhone.trim(),
                                    reason = newFiadorReason.trim(),
                                    targetDateInMillis = System.currentTimeMillis() + 30L * 24 * 3600 * 1000
                                )
                                cData.copy(fiadores = cData.fiadores + newF)
                            }
                            showAddFiadorDialog = false
                            newFiadorName = ""
                            newFiadorAmount = ""
                            newFiadorReason = ""
                            newFiadorPhone = ""
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddFiadorDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Registrar Abono
    if (showAbonoDialog && fiadorForAbono != null) {
        val f = fiadorForAbono!!
        val rest = f.amount - f.paidAmount
        AlertDialog(
            onDismissRequest = { showAbonoDialog = false },
            title = { Text("💵 Registrar Abono a Deuda", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Cliente: ${f.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Deuda pendiente: ${currencyFormat.format(rest)}", fontSize = 12.sp, color = Color(0xFFFFB74D))
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = abonoAmountText,
                        onValueChange = { abonoAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Monto del abono ($)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val payment = abonoAmountText.toDoubleOrNull() ?: 0.0
                        if (payment > 0.0) {
                            onUpdateData("Abono de ${currencyFormat.format(payment)} a ${f.name}") { cData ->
                                val updated = cData.fiadores.map { item ->
                                    if (item.id == f.id) {
                                        val newPaid = (item.paidAmount + payment).coerceAtMost(item.amount)
                                        item.copy(paidAmount = newPaid)
                                    } else item
                                }
                                cData.copy(fiadores = updated)
                            }
                            showAbonoDialog = false
                        }
                    }
                ) { Text("Confirmar Abono") }
            },
            dismissButton = {
                TextButton(onClick = { showAbonoDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Editar Fiador
    if (showEditFiadorDialog && fiadorToEdit != null) {
        val f = fiadorToEdit!!
        AlertDialog(
            onDismissRequest = { showEditFiadorDialog = false },
            title = { Text("✏️ Editar Fiador", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editFiadorName,
                        onValueChange = { editFiadorName = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editFiadorAmount,
                        onValueChange = { editFiadorAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Monto total de la deuda") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editFiadorReason,
                        onValueChange = { editFiadorReason = it },
                        label = { Text("Concepto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editFiadorPhone,
                        onValueChange = { editFiadorPhone = it },
                        label = { Text("Teléfono") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = editFiadorName.trim()
                        val amt = editFiadorAmount.toDoubleOrNull() ?: f.amount
                        if (name.isNotEmpty()) {
                            onUpdateData("Fiador editado: $name") { cData ->
                                val updated = cData.fiadores.map { item ->
                                    if (item.id == f.id) {
                                        item.copy(name = name, amount = amt, reason = editFiadorReason.trim(), phone = editFiadorPhone.trim())
                                    } else item
                                }
                                cData.copy(fiadores = updated)
                            }
                            showEditFiadorDialog = false
                        }
                    }
                ) { Text("Guardar Cambios") }
            },
            dismissButton = {
                TextButton(onClick = { showEditFiadorDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Eliminar Fiador
    if (showDeleteFiadorDialog && fiadorToDelete != null) {
        val f = fiadorToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteFiadorDialog = false },
            title = { Text("🗑️ Eliminar Fiador", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("¿Deseas eliminar definitivamente el registro de deuda de '${f.name}' por ${currencyFormat.format(f.amount)}?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    onClick = {
                        onUpdateData("Fiador eliminado: ${f.name}") { cData ->
                            cData.copy(fiadores = cData.fiadores.filterNot { it.id == f.id })
                        }
                        showDeleteFiadorDialog = false
                    }
                ) { Text("Eliminar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFiadorDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Fiadores y Deudas (${clientFiadores.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Button(
            onClick = { showAddFiadorDialog = true },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(30.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.Black)
            Spacer(Modifier.width(3.dp))
            Text("Nuevo Fiador", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }

    if (clientFiadores.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay fiadores o deudas registradas en el servidor", color = Color.Gray, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(clientFiadores) { f ->
                val rest = f.amount - f.paidAmount
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                    border = BorderStroke(1.dp, Color(0xFF2C2C32))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(f.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, modifier = Modifier.weight(1f).padding(end = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Resta: " + currencyFormat.format(rest), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFFFB74D), softWrap = false)
                        }
                        Text("Deuda Total: ${currencyFormat.format(f.amount)} | Abonado: ${currencyFormat.format(f.paidAmount)}", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                        if (f.reason.isNotBlank()) Text("Motivo: ${f.reason}", fontSize = 10.sp, color = Color(0xFFB0B0B5))
                        if (f.phone.isNotBlank()) Text("Tel: ${f.phone}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1B5E20),
                                modifier = Modifier.clickable {
                                    fiadorForAbono = f
                                    abonoAmountText = ""
                                    showAbonoDialog = true
                                }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Text("Abono", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    fiadorToEdit = f
                                    editFiadorName = f.name
                                    editFiadorAmount = f.amount.toString()
                                    editFiadorReason = f.reason
                                    editFiadorPhone = f.phone
                                    showEditFiadorDialog = true
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color(0xFF64B5F6), modifier = Modifier.size(15.dp))
                            }
                            IconButton(
                                onClick = {
                                    fiadorToDelete = f
                                    showDeleteFiadorDialog = true
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF5252), modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.BackendBackupsTab(
    backupsList: List<BackupRecord>,
    dateFormat: SimpleDateFormat
) {
    if (backupsList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay versiones históricas de respaldos para este cliente", color = Color.Gray, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(backupsList) { bRecord ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                    border = BorderStroke(1.dp, Color(0xFF2C2C32))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(bRecord.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text(dateFormat.format(Date(bRecord.timestamp)), fontSize = 11.sp, color = Color(0xFF9E9E9E))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Contenido: ${bRecord.data.transactions.size} transacciones, ${bRecord.data.products.size} productos, ${bRecord.data.fiadores.size} fiadores",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.BackendRawJsonTab(
    payload: CloudPayload?,
    gson: Gson,
    context: Context
) {
    val jsonStr = remember(payload) {
        try { gson.toJson(payload) } catch (_: Exception) { "{}" }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("JSON del Servidor:", fontSize = 12.sp, color = Color(0xFF9E9E9E))
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Backend JSON", jsonStr))
                    Toast.makeText(context, "JSON copiado al portapapeles", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copiar JSON", fontSize = 12.sp)
            }
        }

        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
            border = BorderStroke(1.dp, Color(0xFF2A2A30))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                Text(
                    text = jsonStr,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF80CBC4),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

@Composable
private fun ServerCapacityCard(
    stats: ServerStatsResponse?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onExploreDisk: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A)),
        border = BorderStroke(1.dp, Color(0xFF2E2E38))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            // Título + Botón de refresco
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Capacidad del Servidor (Oracle Cloud)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Actualizar Estadísticas",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (stats == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Obteniendo capacidad de RAM, Disco y CPU en vivo...", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                }
            } else {
                // Gráficas de RAM y Disco en 2 Columnas Sencillas y Simétricas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Barra de RAM
                    val ramPercent = (stats.ram.usagePercent / 100.0).toFloat().coerceIn(0f, 1f)
                    val ramColor = when {
                        stats.ram.usagePercent >= 85.0 -> Color(0xFFE53935)
                        stats.ram.usagePercent >= 70.0 -> Color(0xFFFFD700)
                        else -> Color(0xFF4CAF50)
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                        border = BorderStroke(1.dp, Color(0xFF2A2A34))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🧠 RAM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    "${String.format(Locale.US, "%.1f", stats.ram.usagePercent)}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ramColor
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { ramPercent },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                color = ramColor,
                                trackColor = Color(0xFF33333E)
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Uso: ${formatBytes(stats.ram.usedBytes)}", fontSize = 9.sp, color = Color(0xFF9E9E9E))
                                Text("Libre: ${formatBytes(stats.ram.freeBytes)}", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF81C784))
                            }
                            Text("Total: ${formatBytes(stats.ram.totalBytes)}", fontSize = 8.sp, color = Color(0xFF757575))
                        }
                    }

                    // Barra de Disco SSD (Interactivo / Click para Explorar)
                    val diskPercent = (stats.disk.usagePercent / 100.0).toFloat().coerceIn(0f, 1f)
                    val diskColor = when {
                        stats.disk.usagePercent >= 85.0 -> Color(0xFFE53935)
                        stats.disk.usagePercent >= 70.0 -> Color(0xFFFFD700)
                        else -> Color(0xFF4CAF50)
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onExploreDisk() },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💾 Disco SSD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Filled.FolderOpen,
                                        contentDescription = "Explorar",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    "${String.format(Locale.US, "%.1f", stats.disk.usagePercent)}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = diskColor
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { diskPercent },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                color = diskColor,
                                trackColor = Color(0xFF33333E)
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Uso: ${formatBytes(stats.disk.usedBytes)}", fontSize = 9.sp, color = Color(0xFF9E9E9E))
                                Text("Libre: ${formatBytes(stats.disk.freeBytes)}", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF81C784))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total: ${formatBytes(stats.disk.totalBytes)}", fontSize = 8.sp, color = Color(0xFF757575))
                                Text("Tocar para ver 🔍", fontSize = 8.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Resumen de CPU, Uptime y Sistema
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1B1B20),
                    border = BorderStroke(1.dp, Color(0xFF26262E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "⚡ CPU: ${stats.cpu.cores} núcleos",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD4AF37)
                        )
                        Text(
                            "⏱️ Encendido: ${formatUptime(stats.uptime.systemSeconds)}",
                            fontSize = 10.sp,
                            color = Color(0xFFBDBDBD)
                        )
                        Text(
                            "🐧 Linux (${stats.system.arch})",
                            fontSize = 10.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackendCierresSummaryTopCard(
    personalSaldo: Double,
    personalTxCount: Int,
    tiendaStock: Int,
    tiendaProdCount: Int,
    pedidosCount: Int,
    pedidosStock: Int,
    cierres: List<CierreSession>,
    currencyFormat: NumberFormat,
    onSelectTab: (Int) -> Unit
) {
    val totalIncomes = cierres.sumOf { it.totalIncomes }
    val totalExpenses = cierres.sumOf { it.totalExpenses }
    val netBalance = totalIncomes - totalExpenses

    val personalCierres = cierres.filter { it.mode.contains("PERSONAL", ignoreCase = true) }
    val tiendaCierres = cierres.filter { it.mode.contains("TIENDA", ignoreCase = true) }
    val pedidosCierres = cierres.filter { it.mode.contains("PEDIDOS", ignoreCase = true) || it.mode.contains("COMERCIO", ignoreCase = true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF17171C)),
        border = BorderStroke(1.dp, Color(0xFF2C2C36))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTab(3) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "🔒 Resumen Cierres de Caja",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "${cierres.size} ses.",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    "Cierres Neto: ${currencyFormat.format(netBalance)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (netBalance >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Desglose simétrico de los 3 Modos Operativos clickeables que llevan a su modo respectivo
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Modo Personal (Tab 0)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF14243B),
                    border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectTab(0) }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👤 Personal ↗", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6), maxLines = 1)
                        Text(
                            "Saldo ${currencyFormat.format(personalSaldo)}",
                            fontSize = 8.sp,
                            color = if (personalSaldo >= 0) Color(0xFFBBDEFB) else Color(0xFFFF8A80),
                            maxLines = 1,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Modo Tienda (Tab 1)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF2E2614),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectTab(1) }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏪 Tienda ↗", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700), maxLines = 1)
                        Text(
                            "$tiendaProdCount prods | $tiendaStock stk",
                            fontSize = 8.sp,
                            color = Color(0xFFFFF176),
                            maxLines = 1,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Modo Pedidos (Tab 2)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF281C2E),
                    border = BorderStroke(1.dp, Color(0xFFBA68C8).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectTab(2) }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📦 Pedidos ↗", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCE93D8), maxLines = 1)
                        Text(
                            "$pedidosCount ped. | $pedidosStock stk",
                            fontSize = 8.sp,
                            color = Color(0xFFE1BEE7),
                            maxLines = 1,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// COMPOSABLES WRAPPER PARA CADA MODO OPERATIVO (PERSONAL, TIENDA, PEDIDOS)
// =========================================================================

@Composable
private fun ColumnScope.BackendPersonalModeTab(
    transactions: List<Transaction>,
    fiadores: List<Fiador>,
    cierres: List<CierreSession>,
    currencyFormat: NumberFormat,
    dateFormat: SimpleDateFormat,
    onUpdateData: (String, (BackupData) -> BackupData) -> Unit
) {
    var personalSubTab by remember { mutableStateOf(0) } // 0: Transacciones, 1: Fiadores, 2: Cierres

    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
    val balance = totalIncome - totalExpense
    val totalDebt = fiadores.sumOf { maxOf(0.0, it.amount - it.paidAmount) }

    // Banner de métricas del Modo Personal
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14243B).copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👤 Modo Personal", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF64B5F6))
                Text(
                    "Saldo Neto: ${currencyFormat.format(balance)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (balance >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1A1A22),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Text("🟢 Ingresos", fontSize = 9.sp, color = Color(0xFF9E9E9E))
                        Text(currencyFormat.format(totalIncome), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1A1A22),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Text("🔴 Gastos", fontSize = 9.sp, color = Color(0xFF9E9E9E))
                        Text(currencyFormat.format(totalExpense), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1A1A22),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Text("👥 Fiadores", fontSize = 9.sp, color = Color(0xFF9E9E9E))
                        Text(currencyFormat.format(totalDebt), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D))
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Selector de sub-sección del modo personal
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF16161A)).padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (personalSubTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
            modifier = Modifier.weight(1f).clickable { personalSubTab = 0 }
        ) {
            Text(
                "💰 Transacciones (${transactions.size})",
                fontSize = 11.sp,
                fontWeight = if (personalSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                color = if (personalSubTab == 0) Color.Black else Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (personalSubTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
            modifier = Modifier.weight(1f).clickable { personalSubTab = 1 }
        ) {
            Text(
                "👥 Fiadores (${fiadores.size})",
                fontSize = 11.sp,
                fontWeight = if (personalSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                color = if (personalSubTab == 1) Color.Black else Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (personalSubTab == 2) MaterialTheme.colorScheme.primary else Color.Transparent,
            modifier = Modifier.weight(1f).clickable { personalSubTab = 2 }
        ) {
            Text(
                "🔒 Cierres (${cierres.size})",
                fontSize = 11.sp,
                fontWeight = if (personalSubTab == 2) FontWeight.Bold else FontWeight.Medium,
                color = if (personalSubTab == 2) Color.Black else Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    when (personalSubTab) {
        0 -> BackendTransactionsTab(transactions, currencyFormat, dateFormat, onUpdateData)
        1 -> BackendFiadoresTab(fiadores, currencyFormat, dateFormat, onUpdateData)
        2 -> BackendCierresTab(cierres, currencyFormat, dateFormat, onUpdateData)
    }
}

@Composable
private fun ColumnScope.BackendTiendaModeTab(
    products: List<Product>,
    deletedProducts: List<Product>,
    transactions: List<Transaction>,
    fiadores: List<Fiador>,
    cierres: List<CierreSession>,
    currencyFormat: NumberFormat,
    dateFormat: SimpleDateFormat,
    onUpdateData: (String, (BackupData) -> BackupData) -> Unit
) {
    var tiendaSubTab by remember { mutableStateOf(0) } // 0: Inventario, 1: Ventas/Gastos, 2: Fiadores, 3: Cierres

    val totalStock = products.sumOf { it.stock }
    val totalCost = products.sumOf { it.stock * it.purchasePrice }
    val totalSaleValue = products.sumOf { it.stock * it.price }
    val totalStoreVentas = transactions.filter { it.isIncome }.sumOf { it.amount }

    // Banner de métricas del Modo Tienda
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2614).copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏪 Modo Tienda", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFFFD700))
                Text(
                    "Ventas: ${currencyFormat.format(totalStoreVentas)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1A1A22),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Text("📦 Prods / Stock", fontSize = 9.sp, color = Color(0xFF9E9E9E))
                        Text("${products.size} / $totalStock uds", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1A1A22),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Text("💰 Costo Inv.", fontSize = 9.sp, color = Color(0xFF9E9E9E))
                        Text(currencyFormat.format(totalCost), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1A1A22),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Text("🏷️ Valor Venta", fontSize = 9.sp, color = Color(0xFF9E9E9E))
                        Text(currencyFormat.format(totalSaleValue), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Selector de sub-sección del modo tienda
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF16161A)).padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (tiendaSubTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
            modifier = Modifier.weight(1f).clickable { tiendaSubTab = 0 }
        ) {
            Text(
                "📦 Inventario (${products.size})",
                fontSize = 11.sp,
                fontWeight = if (tiendaSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                color = if (tiendaSubTab == 0) Color.Black else Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (tiendaSubTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
            modifier = Modifier.weight(1f).clickable { tiendaSubTab = 1 }
        ) {
            Text(
                "💵 Ventas (${transactions.size})",
                fontSize = 11.sp,
                fontWeight = if (tiendaSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                color = if (tiendaSubTab == 1) Color.Black else Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (tiendaSubTab == 2) MaterialTheme.colorScheme.primary else Color.Transparent,
            modifier = Modifier.weight(1f).clickable { tiendaSubTab = 2 }
        ) {
            Text(
                "👥 Fiadores (${fiadores.size})",
                fontSize = 11.sp,
                fontWeight = if (tiendaSubTab == 2) FontWeight.Bold else FontWeight.Medium,
                color = if (tiendaSubTab == 2) Color.Black else Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (tiendaSubTab == 3) MaterialTheme.colorScheme.primary else Color.Transparent,
            modifier = Modifier.weight(1f).clickable { tiendaSubTab = 3 }
        ) {
            Text(
                "🔒 Cierres (${cierres.size})",
                fontSize = 11.sp,
                fontWeight = if (tiendaSubTab == 3) FontWeight.Bold else FontWeight.Medium,
                color = if (tiendaSubTab == 3) Color.Black else Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    when (tiendaSubTab) {
        0 -> BackendProductsTab(products, deletedProducts, currencyFormat, onUpdateData)
        1 -> BackendTransactionsTab(transactions, currencyFormat, dateFormat, onUpdateData)
        2 -> BackendFiadoresTab(fiadores, currencyFormat, dateFormat, onUpdateData)
        3 -> BackendCierresTab(cierres, currencyFormat, dateFormat, onUpdateData)
    }
}

@Composable
private fun ColumnScope.BackendPedidosModeTab(
    clientPedidos: List<ComercioPedido>,
    clientComercioProducts: List<ComercioProduct>,
    clientMovements: List<ComercioMovement>,
    cierres: List<CierreSession>,
    currencyFormat: NumberFormat,
    dateFormat: SimpleDateFormat,
    onUpdateData: (String, (BackupData) -> BackupData) -> Unit
) {
    var pedidosSubTab by remember { mutableStateOf(0) } // 0: Pedidos y Productos, 1: Cierres Pedidos

    // Selector superior si hay cierres de pedidos
    if (cierres.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF16161A)).padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (pedidosSubTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier.weight(1f).clickable { pedidosSubTab = 0 }
            ) {
                Text(
                    "📦 Pedidos & Productos (${clientPedidos.size})",
                    fontSize = 11.sp,
                    fontWeight = if (pedidosSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                    color = if (pedidosSubTab == 0) Color.Black else Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (pedidosSubTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier.weight(1f).clickable { pedidosSubTab = 1 }
            ) {
                Text(
                    "🔒 Cierres Pedidos (${cierres.size})",
                    fontSize = 11.sp,
                    fontWeight = if (pedidosSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                    color = if (pedidosSubTab == 1) Color.Black else Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }

    if (pedidosSubTab == 0) {
        BackendPedidosTab(
            clientPedidos = clientPedidos,
            clientComercioProducts = clientComercioProducts,
            clientMovements = clientMovements,
            currencyFormat = currencyFormat,
            dateFormat = dateFormat,
            onUpdateData = onUpdateData
        )
    } else {
        BackendCierresTab(
            cierres = cierres,
            currencyFormat = currencyFormat,
            dateFormat = dateFormat,
            onUpdateData = onUpdateData
        )
    }
}

@Composable
private fun ColumnScope.BackendBackupsAndRawTab(
    payload: CloudPayload?,
    dateFormat: SimpleDateFormat,
    gson: Gson,
    context: Context
) {
    var rawSubTab by remember { mutableStateOf(0) } // 0: Respaldos, 1: RAW JSON

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF16161A)).padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (rawSubTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
            modifier = Modifier.weight(1f).clickable { rawSubTab = 0 }
        ) {
            Text(
                "🗂️ Historial Respaldos (${payload?.backups?.size ?: 0})",
                fontSize = 11.sp,
                fontWeight = if (rawSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                color = if (rawSubTab == 0) Color.Black else Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (rawSubTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
            modifier = Modifier.weight(1f).clickable { rawSubTab = 1 }
        ) {
            Text(
                "📄 Inspeccionar JSON RAW",
                fontSize = 11.sp,
                fontWeight = if (rawSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                color = if (rawSubTab == 1) Color.Black else Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    if (rawSubTab == 0) {
        BackendBackupsTab(payload?.backups ?: emptyList(), dateFormat)
    } else {
        BackendRawJsonTab(payload, gson, context)
    }
}

@Composable
private fun ColumnScope.BackendCierresTab(
    cierres: List<CierreSession>,
    currencyFormat: NumberFormat,
    dateFormat: SimpleDateFormat,
    onUpdateData: (String, (BackupData) -> BackupData) -> Unit
) {
    var modeFilter by remember { mutableStateOf("TODOS") }

    var showAddCierreDialog by remember { mutableStateOf(false) }
    var newCierreName by remember { mutableStateOf("") }
    var newCierreMode by remember { mutableStateOf("TIENDA") }
    var newCierreIncomes by remember { mutableStateOf("") }
    var newCierreExpenses by remember { mutableStateOf("") }

    var showEditCierreDialog by remember { mutableStateOf(false) }
    var cierreToEdit by remember { mutableStateOf<CierreSession?>(null) }
    var editCierreName by remember { mutableStateOf("") }
    var editCierreMode by remember { mutableStateOf("TIENDA") }
    var editCierreIncomes by remember { mutableStateOf("") }
    var editCierreExpenses by remember { mutableStateOf("") }

    var showDeleteCierreDialog by remember { mutableStateOf(false) }
    var cierreToDelete by remember { mutableStateOf<CierreSession?>(null) }

    // Dialog: Agregar Cierre Manual
    if (showAddCierreDialog) {
        AlertDialog(
            onDismissRequest = { showAddCierreDialog = false },
            title = { Text("🔒 Nuevo Cierre de Caja", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Módulo:", fontSize = 12.sp, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("PERSONAL", "TIENDA", "PEDIDOS", "TODOS").forEach { m ->
                            FilterChip(
                                selected = newCierreMode == m,
                                onClick = { newCierreMode = m },
                                label = { Text(m, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newCierreName,
                        onValueChange = { newCierreName = it },
                        label = { Text("Nombre del cierre (ej. Cierre del Día)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = newCierreIncomes,
                            onValueChange = { newCierreIncomes = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Total Ventas / Ingresos") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newCierreExpenses,
                            onValueChange = { newCierreExpenses = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Total Gastos / Egresos") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newCierreName.trim().ifBlank { "Cierre ${newCierreMode.lowercase()}" }
                        val inc = newCierreIncomes.toDoubleOrNull() ?: 0.0
                        val exp = newCierreExpenses.toDoubleOrNull() ?: 0.0
                        onUpdateData("Nuevo Cierre: $name") { cData ->
                            val nextId = (cData.cierreSessions.maxOfOrNull { it.id } ?: 0) + 1
                            val newC = CierreSession(
                                id = nextId,
                                mode = newCierreMode,
                                name = name,
                                totalIncomes = inc,
                                totalExpenses = exp,
                                timestamp = System.currentTimeMillis()
                            )
                            cData.copy(cierreSessions = cData.cierreSessions + newC)
                        }
                        showAddCierreDialog = false
                        newCierreName = ""
                        newCierreIncomes = ""
                        newCierreExpenses = ""
                    }
                ) { Text("Crear Cierre") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCierreDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Editar Cierre
    if (showEditCierreDialog && cierreToEdit != null) {
        val c = cierreToEdit!!
        AlertDialog(
            onDismissRequest = { showEditCierreDialog = false },
            title = { Text("✏️ Editar Cierre de Caja", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Módulo:", fontSize = 12.sp, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("PERSONAL", "TIENDA", "PEDIDOS", "TODOS").forEach { m ->
                            FilterChip(
                                selected = editCierreMode == m,
                                onClick = { editCierreMode = m },
                                label = { Text(m, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = editCierreName,
                        onValueChange = { editCierreName = it },
                        label = { Text("Nombre del cierre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = editCierreIncomes,
                            onValueChange = { editCierreIncomes = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Total Ingresos") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editCierreExpenses,
                            onValueChange = { editCierreExpenses = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Total Egresos") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = editCierreName.trim()
                        val inc = editCierreIncomes.toDoubleOrNull() ?: c.totalIncomes
                        val exp = editCierreExpenses.toDoubleOrNull() ?: c.totalExpenses
                        if (name.isNotEmpty()) {
                            onUpdateData("Cierre editado: $name") { cData ->
                                val updated = cData.cierreSessions.map { item ->
                                    if (item.id == c.id) {
                                        item.copy(name = name, mode = editCierreMode, totalIncomes = inc, totalExpenses = exp)
                                    } else item
                                }
                                cData.copy(cierreSessions = updated)
                            }
                            showEditCierreDialog = false
                        }
                    }
                ) { Text("Guardar Cambios") }
            },
            dismissButton = {
                TextButton(onClick = { showEditCierreDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Eliminar Cierre
    if (showDeleteCierreDialog && cierreToDelete != null) {
        val c = cierreToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteCierreDialog = false },
            title = { Text("🗑️ Eliminar Cierre de Caja", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text("¿Deseas eliminar la sesión '${c.name}'? Al eliminarla, los movimientos y transacciones que pertenecían a este cierre volverán al estado abierto (sin cierre asignado).")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    onClick = {
                        onUpdateData("Cierre eliminado: ${c.name}") { cData ->
                            val updatedTx = cData.transactions.map { if (it.cierreId == c.id) it.copy(cierreId = null) else it }
                            val updatedMov = cData.comercioMovements.map { if (it.cierreId == c.id) it.copy(cierreId = null) else it }
                            val updatedFiad = cData.fiadores.map { if (it.cierreId == c.id) it.copy(cierreId = null) else it }
                            cData.copy(
                                cierreSessions = cData.cierreSessions.filterNot { it.id == c.id },
                                transactions = updatedTx,
                                comercioMovements = updatedMov,
                                fiadores = updatedFiad
                            )
                        }
                        showDeleteCierreDialog = false
                    }
                ) { Text("Eliminar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCierreDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = modeFilter == "TODOS",
            onClick = { modeFilter = "TODOS" },
            label = { Text("Todos (${cierres.size})", fontSize = 11.sp) }
        )
        FilterChip(
            selected = modeFilter == "PERSONAL",
            onClick = { modeFilter = "PERSONAL" },
            label = { Text("👤 Personal", fontSize = 11.sp) }
        )
        FilterChip(
            selected = modeFilter == "TIENDA",
            onClick = { modeFilter = "TIENDA" },
            label = { Text("🏪 Tienda", fontSize = 11.sp) }
        )
        FilterChip(
            selected = modeFilter == "PEDIDOS",
            onClick = { modeFilter = "PEDIDOS" },
            label = { Text("📦 Pedidos", fontSize = 11.sp) }
        )
        Button(
            onClick = { showAddCierreDialog = true },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(28.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Black)
            Spacer(Modifier.width(2.dp))
            Text("Nuevo Cierre", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }

    val filteredCierres = remember(cierres, modeFilter) {
        when (modeFilter) {
            "TODOS" -> cierres
            "PERSONAL" -> cierres.filter { it.mode.contains("PERSONAL", ignoreCase = true) }
            "TIENDA" -> cierres.filter { it.mode.contains("TIENDA", ignoreCase = true) }
            "PEDIDOS" -> cierres.filter { it.mode.contains("PEDIDOS", ignoreCase = true) || it.mode.contains("COMERCIO", ignoreCase = true) }
            else -> cierres
        }
    }

    if (filteredCierres.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Text("No hay cierres registrados para este filtro", color = Color.Gray, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredCierres) { cierre ->
                val net = cierre.totalIncomes - cierre.totalExpenses
                val modeColor = when {
                    cierre.mode.contains("TIENDA", ignoreCase = true) -> Color(0xFFFFD700)
                    cierre.mode.contains("PEDIDOS", ignoreCase = true) || cierre.mode.contains("COMERCIO", ignoreCase = true) -> Color(0xFFCE93D8)
                    cierre.mode.contains("PERSONAL", ignoreCase = true) -> Color(0xFF64B5F6)
                    else -> Color(0xFF81C784)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                    border = BorderStroke(1.dp, Color(0xFF2C2C32))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f).padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = modeColor.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, modeColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = cierre.mode,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = modeColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cierre.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (cierre.timestamp > 0L) dateFormat.format(Date(cierre.timestamp)) else "Sin fecha",
                                    fontSize = 10.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        cierreToEdit = cierre
                                        editCierreName = cierre.name
                                        editCierreMode = cierre.mode
                                        editCierreIncomes = cierre.totalIncomes.toString()
                                        editCierreExpenses = cierre.totalExpenses.toString()
                                        showEditCierreDialog = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color(0xFF64B5F6), modifier = Modifier.size(13.dp))
                                }
                                IconButton(
                                    onClick = {
                                        cierreToDelete = cierre
                                        showDeleteCierreDialog = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF5252), modifier = Modifier.size(13.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("🟢 Ventas", fontSize = 9.sp, color = Color(0xFF9E9E9E), maxLines = 1)
                                Text(
                                    currencyFormat.format(cierre.totalIncomes),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4CAF50),
                                    softWrap = false
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔴 Gastos", fontSize = 9.sp, color = Color(0xFF9E9E9E), maxLines = 1)
                                Text(
                                    currencyFormat.format(cierre.totalExpenses),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE53935),
                                    softWrap = false
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("Neto", fontSize = 9.sp, color = Color(0xFF9E9E9E), maxLines = 1)
                                Text(
                                    currencyFormat.format(net),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (net >= 0) MaterialTheme.colorScheme.primary else Color(0xFFE53935),
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
}

private fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        days > 0 -> "${days}d ${hours}h ${minutes}m"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m ${seconds % 60}s"
    }
}

@Composable
private fun ColumnScope.DiskExplorerSection(
    diskData: DiskExplorerResponse?,
    isLoading: Boolean,
    error: String?,
    onNavigate: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().weight(1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A)),
        border = BorderStroke(1.dp, Color(0xFF2E2E38))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            // Cabecera: Título + Ruta actual (Breadcrumb) + Botón Subir + Refrescar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Explorador de Archivos (Disco Servidor)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (diskData?.parentPath != null) {
                        IconButton(
                            onClick = { onNavigate(diskData.parentPath) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Filled.ArrowUpward,
                                contentDescription = "Subir de nivel",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Actualizar directorio",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Barra de ruta actual tipo barra de direcciones de Windows
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF101012),
                border = BorderStroke(1.dp, Color(0xFF2C2C34)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📁", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = diskData?.currentPath ?: "/",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Calculando pesos y carpetas...", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                    }
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️ Error al explorar:", color = Color(0xFFE53935), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(error, color = Color.Gray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Reintentar", color = Color.Black, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                val items = diskData?.items ?: emptyList()
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Directorio vacío", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    val maxItemSize = items.maxOfOrNull { it.sizeBytes }?.coerceAtLeast(1L) ?: 1L
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(items) { item ->
                            val isDir = item.isDirectory
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDir) Color(0xFF1E1E24) else Color(0xFF1A1A1E),
                                border = BorderStroke(1.dp, if (isDir) Color(0xFF2C2C36) else Color(0xFF24242A)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isDir) {
                                        if (isDir) {
                                            onNavigate(item.path)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isDir) "📁" else "📄",
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = item.name,
                                                fontSize = 12.sp,
                                                fontWeight = if (isDir) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isDir) Color.White else Color(0xFFDCDCDC),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            // Mini barra de proporción de peso
                                            val proportion = (item.sizeBytes.toFloat() / maxItemSize.toFloat()).coerceIn(0f, 1f)
                                            LinearProgressIndicator(
                                                progress = { proportion },
                                                modifier = Modifier.width(90.dp).height(3.dp).clip(RoundedCornerShape(2.dp)),
                                                color = if (isDir) MaterialTheme.colorScheme.primary else Color(0xFF9E9E9E),
                                                trackColor = Color(0xFF2C2C36)
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatBytes(item.sizeBytes),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.sizeBytes > 1024L * 1024L * 500L) Color(0xFFFFD700) else Color(0xFFB0B0B0)
                                        )
                                        if (isDir) {
                                            Text("Abrir ▸", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
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


