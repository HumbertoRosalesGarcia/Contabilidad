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

    // Explorador de Disco
    var diskExplorerPath by remember { mutableStateOf("/") }
    var diskData by remember { mutableStateOf<DiskExplorerResponse?>(null) }
    var isLoadingDisk by remember { mutableStateOf(false) }
    var diskError by remember { mutableStateOf<String?>(null) }

    var usersMap by remember { mutableStateOf<Map<String, UserData>>(emptyMap()) }
    var backupsSummary by remember { mutableStateOf<List<BackupFileInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedUserEmail by remember { mutableStateOf<String?>(null) }

    var selectedClientPayload by remember { mutableStateOf<CloudPayload?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Cierres, 1: Productos, 2: Finanzas, 3: Pedidos, 4: Fiadores, 5: Respaldos, 6: JSON

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
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
                    // Tarjetas métricas de clientes
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
                            modifier = Modifier.weight(1f).height(58.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C20)),
                            border = BorderStroke(1.dp, Color(0xFF2E2E36))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Clientes", fontSize = 11.sp, color = Color(0xFF9E9E9E), maxLines = 1)
                                Text("${usersMap.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f).height(58.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C20)),
                            border = BorderStroke(1.dp, Color(0xFF2E2E36))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Respaldos", fontSize = 11.sp, color = Color(0xFF9E9E9E), maxLines = 1)
                                Text("$usersWithBackupCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f).height(58.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C20)),
                            border = BorderStroke(1.dp, Color(0xFF2E2E36))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("En línea", fontSize = 11.sp, color = Color(0xFF9E9E9E), maxLines = 1)
                                Text("$onlineCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buscador y Selector de Cliente
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar cliente por correo o nombre...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Limpiar", tint = Color(0xFF9E9E9E), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Carrusel horizontal de clientes
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredUsers.keys.toList()) { email ->
                            val user = filteredUsers[email]
                            val isSelected = email == selectedUserEmail
                            val hasBackup = backupsSummary.any { it.userId.equals(email, ignoreCase = true) }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color(0xFF1E1E22),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF33333A)
                                ),
                                modifier = Modifier.clickable { selectedUserEmail = email }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF3E3E48)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (user?.name?.firstOrNull() ?: 'U').uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = user?.name ?: email,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = email,
                                                fontSize = 10.sp,
                                                color = Color(0xFF9E9E9E),
                                                maxLines = 1
                                            )
                                            if (hasBackup) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("☁️", fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ficha del cliente seleccionado y pestañas
                    val currentUser = usersMap[selectedUserEmail]
                    if (currentUser != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E)),
                            border = BorderStroke(1.dp, Color(0xFF2C2C32))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cliente: ${currentUser.name}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Correo: $selectedUserEmail",
                                        fontSize = 11.sp,
                                        color = Color(0xFF9E9E9E)
                                    )
                                    val regDateStr = if (currentUser.registeredAt > 0L) dateFormat.format(Date(currentUser.registeredAt)) else "Desconocida"
                                    Text(
                                        text = "Registrado: $regDateStr",
                                        fontSize = 10.sp,
                                        color = Color(0xFF808080)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when (currentUser.role.uppercase()) {
                                            "ADMIN" -> Color(0xFFB8860B)
                                            "GOLD" -> Color(0xFFFFD700)
                                            "INVITADO-GOLD" -> Color(0xFFD4AF37)
                                            else -> Color(0xFF424242)
                                        }
                                    ) {
                                        Text(
                                            text = currentUser.role,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val lastActStr = if (currentUser.lastActive > 0L) dateFormat.format(Date(currentUser.lastActive)) else "Sin registro"
                                    Text(
                                        text = "Activo: $lastActStr",
                                        fontSize = 10.sp,
                                        color = Color(0xFF9E9E9E)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Resumen superior de Cierres de caja del cliente seleccionado
                        if (clientCierres.isNotEmpty()) {
                            BackendCierresSummaryTopCard(
                                cierres = clientCierres,
                                currencyFormat = currencyFormat
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Pestañas de Navegación de Datos
                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            edgePadding = 0.dp,
                            containerColor = Color(0xFF18181A),
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("🔒 Cierres (${clientCierres.size})", fontSize = 12.sp, maxLines = 1) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("📦 Productos (${activeProducts.size + deletedProducts.size})", fontSize = 12.sp, maxLines = 1) }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = { Text("💰 Finanzas (${clientTransactions.size})", fontSize = 12.sp, maxLines = 1) }
                            )
                            Tab(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                text = { Text("📋 Pedidos (${clientPedidos.size})", fontSize = 12.sp, maxLines = 1) }
                            )
                            Tab(
                                selected = selectedTab == 4,
                                onClick = { selectedTab = 4 },
                                text = { Text("👥 Fiadores (${clientFiadores.size})", fontSize = 12.sp, maxLines = 1) }
                            )
                            Tab(
                                selected = selectedTab == 5,
                                onClick = { selectedTab = 5 },
                                text = { Text("🗂️ Respaldos", fontSize = 12.sp, maxLines = 1) }
                            )
                            Tab(
                                selected = selectedTab == 6,
                                onClick = { selectedTab = 6 },
                                text = { Text("📄 RAW", fontSize = 12.sp, maxLines = 1) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isLoadingClientData) {
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else if (selectedClientPayload == null) {
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.CloudOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Este cliente aún no tiene respaldos guardados en el servidor.", color = Color.Gray, fontSize = 13.sp)
                                }
                            }
                        } else {
                            when (selectedTab) {
                                0 -> BackendCierresTab(clientCierres, currencyFormat, dateFormat)
                                1 -> BackendProductsTab(activeProducts, deletedProducts, currencyFormat)
                                2 -> BackendTransactionsTab(clientTransactions, currencyFormat, dateFormat)
                                3 -> BackendPedidosTab(clientPedidos, clientMovements, dateFormat)
                                4 -> BackendFiadoresTab(clientFiadores, currencyFormat)
                                5 -> BackendBackupsTab(selectedClientPayload?.backups ?: emptyList(), dateFormat)
                                6 -> BackendRawJsonTab(selectedClientPayload, gson, context)
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
    currencyFormat: NumberFormat
) {
    var productFilter by remember { mutableStateOf("TODOS") }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                if (isDeleted) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFD32F2F)) {
                                        Text("ELIMINADO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text(
                                text = "Stock registrado: ${p.stock} ${p.unit} | Costo: ${currencyFormat.format(p.purchasePrice)}",
                                fontSize = 11.sp,
                                color = Color(0xFFB0B0B5)
                            )
                            Text(
                                text = "Precio de Venta: ${currencyFormat.format(p.price)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
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
    dateFormat: SimpleDateFormat
) {
    val totalIncome = clientTransactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = clientTransactions.filter { !it.isIncome }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF182218)),
            border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Ingresos", fontSize = 10.sp, color = Color(0xFF81C784))
                Text(currencyFormat.format(totalIncome), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF251818)),
            border = BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Egresos", fontSize = 10.sp, color = Color(0xFFE57373))
                Text(currencyFormat.format(totalExpense), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
            border = BorderStroke(1.dp, Color(0xFF3F51B5).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Balance", fontSize = 10.sp, color = Color(0xFF90CAF9))
                Text(currencyFormat.format(balance), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (balance >= 0) MaterialTheme.colorScheme.primary else Color(0xFFE53935))
            }
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
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.description, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            val tDate = dateFormat.format(Date(t.timestamp))
                            Text("Fecha: $tDate | Método: ${if (t.cashAmount > 0) "Efectivo" else "Digital"}", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                            if (t.note.isNotBlank()) {
                                Text("Nota: ${t.note}", fontSize = 10.sp, color = Color(0xFFB0B0B5))
                            }
                        }
                        Text(
                            text = (if (t.isIncome) "+" else "-") + currencyFormat.format(t.amount),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (t.isIncome) Color(0xFF4CAF50) else Color(0xFFE53935)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.BackendPedidosTab(
    clientPedidos: List<ComercioPedido>,
    clientMovements: List<ComercioMovement>,
    dateFormat: SimpleDateFormat
) {
    if (clientPedidos.isEmpty() && clientMovements.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay pedidos de comercio guardados en el servidor", color = Color.Gray, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (clientPedidos.isNotEmpty()) {
                item {
                    Text("Pedidos Registrados (${clientPedidos.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                items(clientPedidos) { ped ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                        border = BorderStroke(1.dp, Color(0xFF2C2C32))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ped.name.ifBlank { "Sin nombre" }, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                Text(dateFormat.format(Date(ped.timestamp)), fontSize = 11.sp, color = Color(0xFF9E9E9E))
                            }
                            Text("País: ${ped.country}", fontSize = 11.sp, color = Color(0xFFB0B0B5))
                        }
                    }
                }
            }

            if (clientMovements.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Movimientos de Mercancía (${clientMovements.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                items(clientMovements) { mov ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E)),
                        border = BorderStroke(1.dp, Color(0xFF2E2E36))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tipo: ${mov.type} - Cantidad: ${mov.quantity}", fontSize = 12.sp, color = if (mov.type == "IN") Color(0xFF4CAF50) else Color(0xFFE53935))
                            Text(mov.note, fontSize = 11.sp, color = Color(0xFF9E9E9E))
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
    currencyFormat: NumberFormat
) {
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                    border = BorderStroke(1.dp, Color(0xFF2C2C32))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(f.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("Resta: " + currencyFormat.format(f.amount - f.paidAmount), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFFFB74D))
                        }
                        Text("Deuda Total: ${currencyFormat.format(f.amount)} | Abonado: ${currencyFormat.format(f.paidAmount)}", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                        if (f.reason.isNotBlank()) Text("Motivo: ${f.reason}", fontSize = 11.sp, color = Color(0xFFB0B0B5))
                        if (f.phone.isNotBlank()) Text("Tel: ${f.phone}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
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
    cierres: List<CierreSession>,
    currencyFormat: NumberFormat
) {
    if (cierres.isEmpty()) return

    val totalIncomes = cierres.sumOf { it.totalIncomes }
    val totalExpenses = cierres.sumOf { it.totalExpenses }
    val netBalance = totalIncomes - totalExpenses

    // Agrupación por los tres modos
    val personalCierres = cierres.filter { it.mode.contains("PERSONAL", ignoreCase = true) }
    val tiendaCierres = cierres.filter { it.mode.contains("TIENDA", ignoreCase = true) }
    val pedidosCierres = cierres.filter { it.mode.contains("PEDIDOS", ignoreCase = true) || it.mode.contains("COMERCIO", ignoreCase = true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF17171C)),
        border = BorderStroke(1.dp, Color(0xFF332B1A))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "🔒 Resumen de Cierres de Caja",
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
                            "${cierres.size} sesiones",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    "Neto: ${currencyFormat.format(netBalance)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (netBalance >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Ingresos vs Egresos Totales
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E1E22),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🟢 Ventas / Ingresos", fontSize = 10.sp, color = Color(0xFF9E9E9E))
                        Text(
                            currencyFormat.format(totalIncomes),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E1E22),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔴 Egresos / Gastos", fontSize = 10.sp, color = Color(0xFF9E9E9E))
                        Text(
                            currencyFormat.format(totalExpenses),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Desglose visual en los 3 modos
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Modo Personal
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF14243B),
                    border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👤 Personal", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6))
                        Text(
                            "${personalCierres.size} c. (${currencyFormat.format(personalCierres.sumOf { it.totalIncomes })})",
                            fontSize = 8.sp,
                            color = Color(0xFFBBDEFB),
                            maxLines = 1
                        )
                    }
                }

                // Modo Tienda
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF2E2614),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏪 Tienda", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                        Text(
                            "${tiendaCierres.size} c. (${currencyFormat.format(tiendaCierres.sumOf { it.totalIncomes })})",
                            fontSize = 8.sp,
                            color = Color(0xFFFFF176),
                            maxLines = 1
                        )
                    }
                }

                // Modo Pedidos / Comercio
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF281C2E),
                    border = BorderStroke(1.dp, Color(0xFFBA68C8).copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📦 Pedidos", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCE93D8))
                        Text(
                            "${pedidosCierres.size} c. (${currencyFormat.format(pedidosCierres.sumOf { it.totalIncomes })})",
                            fontSize = 8.sp,
                            color = Color(0xFFE1BEE7),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.BackendCierresTab(
    cierres: List<CierreSession>,
    currencyFormat: NumberFormat,
    dateFormat: SimpleDateFormat
) {
    var modeFilter by remember { mutableStateOf("TODOS") }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = modeColor.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, modeColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = cierre.mode,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = modeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cierre.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = if (cierre.timestamp > 0L) dateFormat.format(Date(cierre.timestamp)) else "Sin fecha",
                                fontSize = 11.sp,
                                color = Color(0xFF9E9E9E)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("🟢 Ingresos / Ventas", fontSize = 10.sp, color = Color(0xFF9E9E9E))
                                Text(
                                    currencyFormat.format(cierre.totalIncomes),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔴 Egresos / Gastos", fontSize = 10.sp, color = Color(0xFF9E9E9E))
                                Text(
                                    currencyFormat.format(cierre.totalExpenses),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE53935)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Balance Neto", fontSize = 10.sp, color = Color(0xFF9E9E9E))
                                Text(
                                    currencyFormat.format(net),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (net >= 0) MaterialTheme.colorScheme.primary else Color(0xFFE53935)
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
