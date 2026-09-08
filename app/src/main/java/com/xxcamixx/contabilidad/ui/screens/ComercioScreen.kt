package com.xxcamixx.contabilidad.ui.screens

import androidx.compose.animation.AnimatedVisibility
import android.net.Uri
import android.os.Build
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xxcamixx.contabilidad.model.ComercioPedido
import com.xxcamixx.contabilidad.model.ComercioProduct
import com.xxcamixx.contabilidad.model.ComercioMovement
import com.xxcamixx.contabilidad.model.Fiador
import com.xxcamixx.contabilidad.util.formatMoneyMain
import com.xxcamixx.contabilidad.util.formatMoneySec
import com.xxcamixx.contabilidad.util.formatDate
import com.xxcamixx.contabilidad.util.formatDateOnly
import com.xxcamixx.contabilidad.util.cleanAmountInput
import com.xxcamixx.contabilidad.util.getSmartEmoji
import com.xxcamixx.contabilidad.viewmodel.FinanceViewModel
import com.xxcamixx.contabilidad.ui.dialogs.AddComercioProductDialog
import com.xxcamixx.contabilidad.ui.dialogs.EditComercioProductDialog
import com.xxcamixx.contabilidad.ui.dialogs.ExpandedImageDialog
import com.xxcamixx.contabilidad.ui.dialogs.CustomDatePickerDialog
import com.xxcamixx.contabilidad.ui.dialogs.CustomTimePickerDialog
import com.xxcamixx.contabilidad.ui.dialogs.ImageSourceDialog
import com.xxcamixx.contabilidad.ui.components.PaymentInputRow
import com.xxcamixx.contabilidad.util.AmountVisualTransformation
import com.xxcamixx.contabilidad.util.cleanDecimalInput
import com.xxcamixx.contabilidad.util.loadBitmapFromUri
import com.xxcamixx.contabilidad.util.saveImageToInternalStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.xxcamixx.contabilidad.util.formatCOP

fun formatQty(qty: Double): String = if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()

data class ParsedSaleDetails(
    val customerName: String,
    val paymentType: String,
    val badgeColor: Color,
    val badgeIcon: String,
    val abonoInfo: String?,
    val cashPart: String?,
    val digitalPart: String?,
    val debeInfo: String?,
    val vueltoInfo: String?
)

fun parseComercioSaleNote(note: String, defaultName: String): ParsedSaleDetails {
    var cName = defaultName
    var pType = "Venta"
    var abono: String? = null
    var cash: String? = null
    var digital: String? = null
    var debe: String? = null
    var vuelto: String? = null

    if (note.isBlank()) {
        return ParsedSaleDetails(cName, "Venta", Color(0xFF757575), "🛒", null, null, null, null, null)
    }

    val parts = note.split("|").map { it.trim() }
    for (part in parts) {
        when {
            part.startsWith("Cliente:", ignoreCase = true) -> {
                cName = part.substringAfter("Cliente:").trim()
            }
            part.startsWith("Fiado", ignoreCase = true) -> {
                pType = "Fiado"
            }
            part.startsWith("Pago Dividido", ignoreCase = true) || part.contains("Dividido", ignoreCase = true) -> {
                pType = "Pago Dividido"
            }
            part.startsWith("Pago Digital", ignoreCase = true) || (part.contains("Digital", ignoreCase = true) && !part.contains("Efectivo") && pType != "Fiado") -> {
                pType = "Digital"
            }
            part.startsWith("Pago Efectivo", ignoreCase = true) || (part.contains("Efectivo", ignoreCase = true) && !part.contains("Digital") && pType != "Fiado") -> {
                pType = "Efectivo"
            }
            part.startsWith("Abonó:", ignoreCase = true) || part.startsWith("Abono:", ignoreCase = true) -> {
                val abonoClean = part.substringAfter(":").trim()
                if (abonoClean.contains("(")) {
                    abono = abonoClean.substringBefore("(").trim()
                    val inside = abonoClean.substringAfter("(").substringBefore(")")
                    val subParts = inside.split(",").map { it.trim() }
                    for (sp in subParts) {
                        if (sp.startsWith("Efectivo:", ignoreCase = true)) cash = sp.substringAfter(":").trim()
                        if (sp.startsWith("Digital:", ignoreCase = true)) digital = sp.substringAfter(":").trim()
                    }
                } else {
                    abono = abonoClean
                }
            }
            part.startsWith("Debe:", ignoreCase = true) -> {
                debe = part.substringAfter("Debe:").trim()
            }
            part.startsWith("Efectivo:", ignoreCase = true) -> {
                cash = part.substringAfter("Efectivo:").trim()
            }
            part.startsWith("Digital:", ignoreCase = true) -> {
                digital = part.substringAfter("Digital:").trim()
            }
            part.startsWith("Vuelto:", ignoreCase = true) -> {
                vuelto = part.substringAfter("Vuelto:").trim()
            }
        }
    }

    val (badgeColor, badgeIcon) = when (pType) {
        "Fiado" -> Pair(Color(0xFFF57F17), "🗓️")
        "Pago Dividido" -> Pair(Color(0xFF7B1FA2), "💳")
        "Digital" -> Pair(Color(0xFF1976D2), "📱")
        "Efectivo" -> Pair(Color(0xFF388E3C), "💵")
        else -> Pair(Color(0xFF616161), "🛒")
    }

    return ParsedSaleDetails(
        customerName = cName.ifBlank { defaultName },
        paymentType = pType,
        badgeColor = badgeColor,
        badgeIcon = badgeIcon,
        abonoInfo = abono,
        cashPart = cash,
        digitalPart = digital,
        debeInfo = debe,
        vueltoInfo = vuelto
    )
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ComercioScreen(
    viewModel: FinanceViewModel,
    pedidos: List<ComercioPedido>,
    products: List<ComercioProduct>,
    movements: List<ComercioMovement>,
    country: String,
    bcvRate: Double,
    onOpenHistory: () -> Unit = {},
    showSearch: Boolean = true,
    onToggleSearch: () -> Unit = {},
    onEditFiador: (com.xxcamixx.contabilidad.model.Fiador) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    val imageCache = remember { androidx.compose.runtime.mutableStateMapOf<String, android.graphics.Bitmap>() }

    fun matchesDateQuery(timestamp: Long, query: String): Boolean {
        if (query.isBlank()) return false
        val q = query.trim().lowercase(Locale.getDefault())
        val formats = listOf("dd MMM yyyy", "dd MMMM yyyy", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy", "d MMM", "d MMMM")
        for (pattern in formats) {
            try {
                val formatted = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp)).lowercase(Locale.getDefault())
                if (formatted.contains(q)) return true
            } catch (_: Exception) {}
        }
        return false
    }
    var showAddPedidoDialog by remember { mutableStateOf(false) }
    var showInvestedDialog by remember { mutableStateOf(false) }
    var showProfitDialog by remember { mutableStateOf(false) }
    var showSoldDialog by remember { mutableStateOf(false) }
    var activePedidoForAdd by remember { mutableStateOf<ComercioPedido?>(null) }
    var viewMode by remember { mutableStateOf("INVENTARIO") }
    var expandedPedidos by remember { mutableStateOf(setOf<Int>()) }
    var expandedInvestedPedidos by remember { mutableStateOf(setOf<Int>()) }
    var expandedProfitProducts by remember { mutableStateOf(setOf<Int>()) }
    var expandedVentas by remember { mutableStateOf(setOf<String>()) }

    var expandedImageUri by remember { mutableStateOf<String?>(null) }

    // Edit/Delete States
    var pedidoOptions by remember { mutableStateOf<ComercioPedido?>(null) }
    var productOptions by remember { mutableStateOf<ComercioProduct?>(null) }
    var pedidoToEdit by remember { mutableStateOf<ComercioPedido?>(null) }
    var productToEdit by remember { mutableStateOf<ComercioProduct?>(null) }
    var productToAddStock by remember { mutableStateOf<ComercioProduct?>(null) }
    var productToDiscount by remember { mutableStateOf<ComercioProduct?>(null) }

    // Cart
    val cart = remember { mutableStateListOf<Triple<ComercioProduct, Double, Double>>() }
    var showCartDialog by remember { mutableStateOf(false) }
    var productToCart by remember { mutableStateOf<ComercioProduct?>(null) }

    val fiadores by viewModel.fiadores.collectAsState(initial = emptyList())

    fun getInCartQty(productId: Int): Double = cart.filter { it.first.id == productId }.sumOf { it.second }
    fun getAvailableStock(p: ComercioProduct): Double = maxOf(0.0, p.quantityInStock - getInCartQty(p.id))

    val existingProductIds = remember(products) { products.map { it.id }.toSet() }
    val validMovements = remember(movements, existingProductIds) { movements.filter { it.productId in existingProductIds } }

    val totalInvested = remember(products) { products.sumOf { it.totalPurchased * it.costPerUnit } }
    val totalSold = remember(validMovements) { validMovements.filter { it.type == "VENTA" }.sumOf { it.total } }
    val totalProfit = remember(products) { products.sumOf { it.totalSold * (it.salePricePerUnit - it.costPerUnit) } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Panel de Pedidos 📦", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Button(
                            onClick = { showAddPedidoDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB388FF),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Nuevo", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Pedido", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Invertido
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showInvestedDialog = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Invertido", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    formatMoneyMain(totalInvested, country),
                                    color = Color(0xFFE53935),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                val sec = formatMoneySec(totalInvested, country, bcvRate)
                                if (sec.isNotEmpty()) {
                                    Text(sec, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }

                        // Ganancias
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showProfitDialog = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Ganancias", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    formatMoneyMain(totalProfit, country),
                                    color = if (totalProfit >= 0) Color(0xFFFFC107) else Color.Red,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                val sec = formatMoneySec(totalProfit, country, bcvRate)
                                if (sec.isNotEmpty()) {
                                    Text(sec, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }

                        // Vendido
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showSoldDialog = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Vendido", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    formatMoneyMain(totalSold, country),
                                    color = Color(0xFF2196F3),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                val sec = formatMoneySec(totalSold, country, bcvRate)
                                if (sec.isNotEmpty()) {
                                    Text(sec, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // Selector elegante de Modos: Estantes / Ventas (Cada uno con el 50% de ancho)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    modifier = Modifier.weight(1f).height(44.dp),
                    onClick = { viewMode = "INVENTARIO" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewMode == "INVENTARIO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (viewMode == "INVENTARIO") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📦 Estantes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    modifier = Modifier.weight(1f).height(44.dp),
                    onClick = { viewMode = "VENTAS" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewMode == "VENTAS") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (viewMode == "VENTAS") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🛍️ Ventas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Barra de Búsqueda elegante con Lupa
            AnimatedVisibility(visible = showSearch) {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        placeholder = {
                            Text(
                                "Búsqueda",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Lupa de búsqueda",
                                tint = if (searchQuery.isNotEmpty()) Color(0xFFB388FF) else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Limpiar búsqueda", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFB388FF),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            if (viewMode == "INVENTARIO") {
                val activePedidos = remember(pedidos, products) {
                    pedidos.filter { pedido ->
                        val pedidoProducts = products.filter { it.pedidoId == pedido.id }
                        pedidoProducts.isEmpty() || pedidoProducts.any { it.quantityInStock > 0 }
                    }
                }

                val filteredPedidos = remember(activePedidos, products, searchQuery, country) {
                    val q = searchQuery.trim().lowercase(Locale.getDefault())
                    if (q.isBlank()) {
                        activePedidos
                    } else {
                        activePedidos.filter { pedido ->
                            val pedidoProducts = products.filter { it.pedidoId == pedido.id }
                            val matchesPedidoName = pedido.name.lowercase(Locale.getDefault()).contains(q)
                            val matchesPedidoDate = matchesDateQuery(pedido.timestamp, q)
                            val matchesAnyProduct = pedidoProducts.any { p ->
                                p.name.lowercase(Locale.getDefault()).contains(q) ||
                                p.unit.lowercase(Locale.getDefault()).contains(q) ||
                                p.costPerUnit.toString().contains(q) ||
                                p.salePricePerUnit.toString().contains(q) ||
                                formatQty(p.quantityInStock).contains(q) ||
                                formatMoneyMain(p.salePricePerUnit, country).lowercase(Locale.getDefault()).contains(q) ||
                                formatMoneyMain(p.costPerUnit, country).lowercase(Locale.getDefault()).contains(q)
                            }
                            matchesPedidoName || matchesPedidoDate || matchesAnyProduct
                        }
                    }
                }
                val completedCount = remember(pedidos, products) {
                    pedidos.count { pedido ->
                        val pedidoProducts = products.filter { it.pedidoId == pedido.id }
                        pedidoProducts.isNotEmpty() && pedidoProducts.all { it.quantityInStock <= 0 }
                    }
                }

                if (activePedidos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📦", fontSize = 52.sp)
                            Spacer(Modifier.height(10.dp))
                            if (completedCount > 0) {
                                Text("No hay pedidos activos", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Tienes $completedCount pedido(s) finalizado(s) en el Historial de pedidos porque todos sus productos llegaron a 0.",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = onOpenHistory,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Ver Historial de pedidos")
                                }
                            } else {
                                Text("Aún no tienes pedidos", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Toca '+ Pedido' en la parte superior para crear tu primer pedido.",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (filteredPedidos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 52.sp)
                            Spacer(Modifier.height(10.dp))
                            Text("Sin resultados para \"$searchQuery\"", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "No se encontraron pedidos ni productos que coincidan con tu búsqueda.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { searchQuery = "" }) {
                                Text("Limpiar búsqueda")
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredPedidos) { pedido ->
                        val pedidoProducts = products.filter { it.pedidoId == pedido.id }
                        val qLower = searchQuery.trim().lowercase(Locale.getDefault())
                        val hasMatchingProduct = qLower.isNotEmpty() && pedidoProducts.any { p ->
                            p.name.lowercase(Locale.getDefault()).contains(qLower) ||
                            p.unit.lowercase(Locale.getDefault()).contains(qLower) ||
                            p.costPerUnit.toString().contains(qLower) ||
                            p.salePricePerUnit.toString().contains(qLower) ||
                            formatQty(p.quantityInStock).contains(qLower) ||
                            formatMoneyMain(p.salePricePerUnit, country).lowercase(Locale.getDefault()).contains(qLower) ||
                            formatMoneyMain(p.costPerUnit, country).lowercase(Locale.getDefault()).contains(qLower)
                        }
                        val isExpanded = expandedPedidos.contains(pedido.id) || hasMatchingProduct

                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 1.dp)) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().combinedClickable(
                                        onClick = { expandedPedidos = if (isExpanded) expandedPedidos - pedido.id else expandedPedidos + pedido.id },
                                        onLongClick = { pedidoOptions = pedido }
                                    ).padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                         if (pedido.imageUri != null) {
                                             val cachedBitmap = imageCache[pedido.imageUri]
                                             if (cachedBitmap != null) {
                                                 Image(bitmap = cachedBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape).clickable { expandedImageUri = pedido.imageUri }, contentScale = ContentScale.Crop)
                                                 Spacer(modifier = Modifier.width(8.dp))
                                             } else {
                                                 var bitmap by remember(pedido.imageUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                                                 LaunchedEffect(pedido.imageUri) {
                                                     val b = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                         com.xxcamixx.contabilidad.util.loadBitmapFromUri(context, pedido.imageUri)
                                                     }
                                                     if (b != null) {
                                                         imageCache[pedido.imageUri!!] = b
                                                         bitmap = b
                                                     }
                                                 }
                                                 if (bitmap != null) {
                                                     Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape).clickable { expandedImageUri = pedido.imageUri }, contentScale = ContentScale.Crop)
                                                     Spacer(modifier = Modifier.width(8.dp))
                                                 }
                                             }
                                         } else {
                                             Text(getSmartEmoji(pedido.name, true), fontSize = 24.sp)
                                             Spacer(modifier = Modifier.width(8.dp))
                                         }
                                         Column {
                                             Text(pedido.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                             Text(formatDateOnly(pedido.timestamp), fontSize = 12.sp, color = Color.Gray)
                                         }
                                     }
                                     Row(verticalAlignment = Alignment.CenterVertically) {
                                         FilledTonalButton(
                                             onClick = { activePedidoForAdd = pedido },
                                             contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                             modifier = Modifier.height(34.dp)
                                         ) {
                                             Icon(Icons.Filled.Add, contentDescription = "Agregar producto", modifier = Modifier.size(16.dp))
                                             Spacer(Modifier.width(4.dp))
                                             Text("Producto", fontSize = 12.sp)
                                         }
                                         Spacer(Modifier.width(8.dp))
                                         Icon(if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
                                     }
                                 }

                                  if (isExpanded) {
                                      val displayedProducts = remember(pedidoProducts, qLower, country) {
                                          if (qLower.isBlank()) {
                                              pedidoProducts
                                          } else {
                                              val matching = pedidoProducts.filter { p ->
                                                  p.name.lowercase(Locale.getDefault()).contains(qLower) ||
                                                  p.unit.lowercase(Locale.getDefault()).contains(qLower) ||
                                                  p.costPerUnit.toString().contains(qLower) ||
                                                  p.salePricePerUnit.toString().contains(qLower) ||
                                                  formatQty(p.quantityInStock).contains(qLower) ||
                                                  formatMoneyMain(p.salePricePerUnit, country).lowercase(Locale.getDefault()).contains(qLower) ||
                                                  formatMoneyMain(p.costPerUnit, country).lowercase(Locale.getDefault()).contains(qLower)
                                              }
                                              if (matching.isNotEmpty()) {
                                                  matching
                                              } else if (pedido.name.lowercase(Locale.getDefault()).contains(qLower) || matchesDateQuery(pedido.timestamp, qLower)) {
                                                  pedidoProducts
                                              } else {
                                                  emptyList()
                                              }
                                          }
                                      }

                                      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                          Divider()
                                          displayedProducts.forEach { p ->
                                              val available = getAvailableStock(p)
                                              Row(
                                                  modifier = Modifier
                                                      .fillMaxWidth()
                                                      .combinedClickable(
                                                          onClick = {},
                                                          onLongClick = { productOptions = p }
                                                      )
                                                      .padding(vertical = 8.dp),
                                                  horizontalArrangement = Arrangement.SpaceBetween,
                                                  verticalAlignment = Alignment.Bottom
                                              ) {
                                                  // Lado izquierdo: Foto y detalles del producto
                                                  Row(
                                                      modifier = Modifier
                                                          .weight(1f)
                                                          .padding(end = 8.dp),
                                                      verticalAlignment = Alignment.Top
                                                  ) {
                                                      if (p.imageUri != null) {
                                                          val cachedBitmap = imageCache[p.imageUri]
                                                          if (cachedBitmap != null) {
                                                              Image(
                                                                  bitmap = cachedBitmap.asImageBitmap(),
                                                                  contentDescription = null,
                                                                  modifier = Modifier
                                                                      .size(46.dp)
                                                                      .clip(RoundedCornerShape(8.dp))
                                                                      .clickable { expandedImageUri = p.imageUri },
                                                                  contentScale = ContentScale.Crop
                                                              )
                                                              Spacer(modifier = Modifier.width(8.dp))
                                                          } else {
                                                              var pbitmap by remember(p.imageUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                                                              LaunchedEffect(p.imageUri) {
                                                                  val b = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                                      com.xxcamixx.contabilidad.util.loadBitmapFromUri(context, p.imageUri)
                                                                  }
                                                                  if (b != null) {
                                                                      imageCache[p.imageUri!!] = b
                                                                      pbitmap = b
                                                                  }
                                                              }
                                                              if (pbitmap != null) {
                                                                  Image(
                                                                      bitmap = pbitmap!!.asImageBitmap(),
                                                                      contentDescription = null,
                                                                      modifier = Modifier
                                                                          .size(46.dp)
                                                                          .clip(RoundedCornerShape(8.dp))
                                                                          .clickable { expandedImageUri = p.imageUri },
                                                                      contentScale = ContentScale.Crop
                                                                  )
                                                                  Spacer(modifier = Modifier.width(8.dp))
                                                              }
                                                          }
                                                      }
                                                      Column(modifier = Modifier.weight(1f)) {
                                                          Text(
                                                              text = p.name,
                                                              fontWeight = FontWeight.Bold,
                                                              fontSize = 15.sp,
                                                              maxLines = 2,
                                                              overflow = TextOverflow.Ellipsis
                                                          )
                                                          Spacer(modifier = Modifier.height(2.dp))
                                                          Row(verticalAlignment = Alignment.CenterVertically) {
                                                              Text("Disp: ", fontSize = 12.sp, color = Color.Gray)
                                                              Text(
                                                                  text = "${formatQty(available)} ${p.unit}${if (available <= 0) " (Agotado)" else ""}",
                                                                  fontSize = 12.sp,
                                                                  fontWeight = FontWeight.SemiBold,
                                                                  color = if (available <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                                              )
                                                          }

                                                          Spacer(modifier = Modifier.height(3.dp))
                                                          Row(verticalAlignment = Alignment.CenterVertically) {
                                                              Text("Costo: ", fontSize = 12.sp, color = Color.Gray)
                                                              Text(formatMoneyMain(p.costPerUnit, country), fontSize = 12.sp, color = Color.Gray)
                                                          }
                                                          val secCosto = formatMoneySec(p.costPerUnit, country, bcvRate)
                                                          if (secCosto.isNotEmpty()) {
                                                              Text(secCosto, fontSize = 10.sp, color = Color.Gray)
                                                          }

                                                          Spacer(modifier = Modifier.height(2.dp))
                                                          Row(verticalAlignment = Alignment.CenterVertically) {
                                                              Text("Precio Venta: ", fontSize = 12.sp, color = Color(0xFF2196F3))
                                                              Text(formatMoneyMain(p.salePricePerUnit, country), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                                                          }
                                                          val secVenta = formatMoneySec(p.salePricePerUnit, country, bcvRate)
                                                          if (secVenta.isNotEmpty()) {
                                                              Text(secVenta, fontSize = 10.sp, color = Color.Gray)
                                                          }
                                                      }
                                                  }

                                                  // Lado inferior derecho: [ - Cantidad + ] encima y [ 🛒 Añadir ] debajo
                                                  Column(
                                                      modifier = Modifier.width(IntrinsicSize.Min),
                                                      horizontalAlignment = Alignment.End,
                                                      verticalArrangement = Arrangement.spacedBy(6.dp)
                                                  ) {
                                                      // Control de ajuste de stock [-] Cantidad [+]
                                                      Row(
                                                          verticalAlignment = Alignment.CenterVertically,
                                                          horizontalArrangement = Arrangement.Center,
                                                          modifier = Modifier
                                                              .fillMaxWidth()
                                                              .clip(RoundedCornerShape(8.dp))
                                                              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                                              .padding(horizontal = 2.dp, vertical = 2.dp)
                                                      ) {
                                                          IconButton(
                                                              onClick = { productToDiscount = p },
                                                              enabled = available > 0,
                                                              modifier = Modifier.size(28.dp)
                                                          ) {
                                                              Icon(
                                                                  Icons.Filled.Remove,
                                                                  contentDescription = "Descontar cantidad",
                                                                  modifier = Modifier.size(15.dp),
                                                                  tint = if (available > 0) MaterialTheme.colorScheme.error else Color.Gray
                                                              )
                                                          }
                                                          Text(
                                                              text = "Cantidad",
                                                              fontWeight = FontWeight.SemiBold,
                                                              fontSize = 11.sp,
                                                              modifier = Modifier.padding(horizontal = 4.dp)
                                                          )
                                                          IconButton(
                                                              onClick = { productToAddStock = p },
                                                              modifier = Modifier.size(28.dp)
                                                          ) {
                                                              Icon(
                                                                  Icons.Filled.Add,
                                                                  contentDescription = "Agregar cantidad",
                                                                  modifier = Modifier.size(15.dp),
                                                                  tint = MaterialTheme.colorScheme.primary
                                                              )
                                                          }
                                                      }

                                                      // Botón de Carrito [🛒 Añadir]
                                                      Button(
                                                          onClick = { productToCart = p },
                                                          enabled = available > 0,
                                                          modifier = Modifier
                                                              .fillMaxWidth()
                                                              .height(34.dp),
                                                          shape = RoundedCornerShape(8.dp),
                                                          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                      ) {
                                                          Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(15.dp))
                                                          Spacer(Modifier.width(4.dp))
                                                          Text("Añadir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                      }
                                                  }
                                              }
                                              Divider(color = Color.LightGray.copy(alpha = 0.3f))
                                          }
                                    }
                                }
                            }
                        }
                    }
                }
                }
            } else {
                // Modo VENTAS: Muestra cantidad de ventas realizadas y ventas en proceso (deudas por cobrar)
                val allVentas = remember(validMovements) { validMovements.filter { it.type == "VENTA" } }
                val deudasEnProceso = remember(fiadores) { fiadores.filter { (it.originMode == "PEDIDOS" || (it.originMode.isEmpty() && it.isStore)) && (it.amount - it.paidAmount) > 0 } }
                val totalVentasCobrado = remember(allVentas) { allVentas.sumOf { it.total } }
                val totalDeudaPendiente = remember(deudasEnProceso) { deudasEnProceso.sumOf { it.amount - it.paidAmount } }

                var ventasSubFilter by remember { mutableStateOf("TODAS") }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Métricas Superiores de Ventas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tarjeta Ventas Realizadas
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { ventasSubFilter = "REALIZADAS" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (ventasSubFilter == "REALIZADAS") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("✅", fontSize = 12.sp)
                                    Text("Realizadas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${allVentas.size} ventas",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    formatMoneyMain(totalVentasCobrado, country),
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Tarjeta Ventas En Proceso (Deudas / Fiados)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { ventasSubFilter = "EN_PROCESO" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (ventasSubFilter == "EN_PROCESO") Color(0xFFFFECB3) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("⏳", fontSize = 12.sp)
                                    Text("En Proceso", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${deudasEnProceso.size} con deuda",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFE65100)
                                )
                                Text(
                                    "Por cobrar: ${formatMoneyMain(totalDeudaPendiente, country)}",
                                    fontSize = 11.sp,
                                    color = Color.Red,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Chips de filtrado rápido
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = ventasSubFilter == "TODAS",
                            onClick = { ventasSubFilter = "TODAS" },
                            label = { Text("Todas (${allVentas.size + deudasEnProceso.size})", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = ventasSubFilter == "REALIZADAS",
                            onClick = { ventasSubFilter = "REALIZADAS" },
                            label = { Text("✅ Realizadas (${allVentas.size})", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = ventasSubFilter == "EN_PROCESO",
                            onClick = { ventasSubFilter = "EN_PROCESO" },
                            label = { Text("⏳ En Proceso (${deudasEnProceso.size})", fontSize = 12.sp) }
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    val q = searchQuery.trim().lowercase(Locale.getDefault())

                    val filteredVentas = remember(allVentas, q, country) {
                        if (q.isBlank()) allVentas
                        else allVentas.filter { m ->
                            m.productName.lowercase(Locale.getDefault()).contains(q) ||
                            m.note.lowercase(Locale.getDefault()).contains(q) ||
                            formatDateOnly(m.timestamp).lowercase(Locale.getDefault()).contains(q) ||
                            matchesDateQuery(m.timestamp, q) ||
                            m.total.toString().contains(q) ||
                            formatMoneyMain(m.total, country).lowercase(Locale.getDefault()).contains(q)
                        }
                    }

                    val groupedVentas = remember(filteredVentas) {
                        filteredVentas.groupBy { it.transactionId ?: it.timestamp.toString() }
                    }

                    val filteredDeudas = remember(deudasEnProceso, q, country) {
                        if (q.isBlank()) deudasEnProceso
                        else deudasEnProceso.filter { f ->
                            f.name.lowercase(Locale.getDefault()).contains(q) ||
                            f.phone.lowercase(Locale.getDefault()).contains(q) ||
                            f.reason.lowercase(Locale.getDefault()).contains(q) ||
                            formatDateOnly(f.targetDateInMillis).lowercase(Locale.getDefault()).contains(q) ||
                            matchesDateQuery(f.targetDateInMillis, q) ||
                            (f.amount - f.paidAmount).toString().contains(q) ||
                            formatMoneyMain(f.amount - f.paidAmount, country).lowercase(Locale.getDefault()).contains(q)
                        }
                    }

                    val showVentas = ventasSubFilter == "TODAS" || ventasSubFilter == "REALIZADAS"
                    val showDeudas = ventasSubFilter == "TODAS" || ventasSubFilter == "EN_PROCESO"

                    val totalItemsCount = (if (showDeudas) filteredDeudas.size else 0) + (if (showVentas) filteredVentas.size else 0)

                    if (totalItemsCount == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🛍️", fontSize = 52.sp)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    if (searchQuery.isNotBlank()) "Sin resultados para \"$searchQuery\"" else "No hay ventas en esta sección",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    if (searchQuery.isNotBlank()) "Intenta con otro término o limpia la búsqueda." else "Las ventas realizadas o con fiados pendientes aparecerán aquí.",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                if (searchQuery.isNotBlank()) {
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedButton(onClick = { searchQuery = "" }) {
                                        Text("Limpiar búsqueda")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            // Primero las deudas en proceso (si corresponde)
                            if (showDeudas && filteredDeudas.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "⏳ VENTAS EN PROCESO (CON DEUDA)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                                items(filteredDeudas) { f ->
                                    val restante = f.amount - f.paidAmount
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { onEditFiador(f) },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFFFB300).copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("⏳", fontSize = 18.sp)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text(f.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Surface(
                                                            color = Color(0xFFFF9800),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                "En Proceso",
                                                                color = Color.White,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                    if (f.phone.isNotBlank()) {
                                                        Text("📞 ${f.phone}", fontSize = 11.sp, color = Color.Gray)
                                                    }
                                                    Text("Productos: ${f.reason}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Total: ${formatMoneyMain(f.amount, country)}", fontSize = 11.sp, color = Color.Gray)
                                                        if (f.paidAmount > 0) {
                                                            Text("Abonado: ${formatMoneyMain(f.paidAmount, country)}", fontSize = 11.sp, color = Color(0xFF4CAF50))
                                                        }
                                                    }
                                                    Text("Fecha límite de cobro: ${formatDateOnly(f.targetDateInMillis)}", fontSize = 10.sp, color = Color.Gray)
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Debe:", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    formatMoneyMain(restante, country),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Red,
                                                    fontSize = 14.sp
                                                )
                                                val secD = formatMoneySec(restante, country, bcvRate)
                                                if (secD.isNotEmpty()) Text(secD, fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }

                            // Luego las ventas realizadas
                            if (showVentas && groupedVentas.isNotEmpty()) {
                                if (showDeudas && filteredDeudas.isNotEmpty()) {
                                    item {
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "✅ VENTAS REALIZADAS",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                }
                                groupedVentas.entries.forEachIndexed { index, entry ->
                                    val groupId = entry.key
                                    val groupItems = entry.value
                                    val isExpanded = expandedVentas.contains(groupId)
                                    val groupTotal = groupItems.sumOf { it.total }
                                    val firstItem = groupItems.first()
                                    val dateStr = formatDateOnly(firstItem.timestamp)

                                    // Determinar nombre del cliente a partir de la nota (Cliente: nombre | ...)
                                    val noteRegex = Regex("Cliente: ([^|]+)")
                                    val match = noteRegex.find(firstItem.note)
                                    val clientName = match?.groups?.get(1)?.value?.trim()

                                    val headerTitle = if (clientName != null) "Venta: $clientName" else "Venta #${groupedVentas.size - index}"

                                    item {
                                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Column {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            expandedVentas = if (isExpanded) expandedVentas - groupId else expandedVentas + groupId
                                                        }
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(38.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFF2196F3).copy(alpha = 0.15f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                Icons.Filled.ShoppingCart,
                                                                contentDescription = null,
                                                                tint = Color(0xFF2196F3),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column {
                                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                Text(headerTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                                Surface(
                                                                    color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                                                    shape = RoundedCornerShape(4.dp)
                                                                ) {
                                                                    Text(
                                                                        "Realizada",
                                                                        color = Color(0xFF2E7D32),
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                    )
                                                                }
                                                            }
                                                            Text(dateStr, fontSize = 10.sp, color = Color.Gray)
                                                            if (groupItems.size > 1) {
                                                                Text("${groupItems.size} productos", fontSize = 11.sp, color = Color.Gray)
                                                            }
                                                        }
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(formatMoneyMain(groupTotal, country), fontWeight = FontWeight.Bold, color = Color(0xFF2196F3), fontSize = 14.sp)
                                                        val secT = formatMoneySec(groupTotal, country, bcvRate)
                                                        if (secT.isNotEmpty()) Text(secT, fontSize = 10.sp, color = Color.Gray)
                                                    }
                                                }

                                                AnimatedVisibility(visible = isExpanded) {
                                                    Column(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.05f)).padding(12.dp)) {
                                                        // Show the payment note from the first item
                                                        if (firstItem.note.isNotEmpty()) {
                                                            Text(firstItem.note, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
                                                        }

                                                        groupItems.forEach { m ->
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text("• ${m.productName} (${formatQty(m.quantity)})", fontSize = 13.sp)
                                                                Text(formatMoneyMain(m.total, country), fontSize = 13.sp, color = Color.Gray)
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
        }

        // Cart FAB
        if (cart.isNotEmpty() && viewMode == "INVENTARIO") {
            FloatingActionButton(
                onClick = { showCartDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = "Carrito", tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("${cart.size}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showInvestedDialog) {
        AlertDialog(
            onDismissRequest = { showInvestedDialog = false },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Desglose de Inversión", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    IconButton(onClick = { showInvestedDialog = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(pedidos) { pedido ->
                        val pedidoProducts = products.filter { it.pedidoId == pedido.id }
                        val pedidoCost = pedidoProducts.sumOf { it.totalPurchased * it.costPerUnit }

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(pedido.name, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(formatMoneyMain(pedidoCost, country), color = Color(0xFFE53935))
                                Text(formatMoneySec(pedidoCost, country, bcvRate), color = Color.Gray)
                            }
                            Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp)) {
                                pedidoProducts.forEach { p ->
                                    val pCost = p.costPerUnit * p.totalPurchased
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("• ${p.name} (${formatQty(p.totalPurchased)})", fontSize = 12.sp)
                                        Text(formatMoneyMain(pCost, country), fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInvestedDialog = false },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Cerrar") }
            },
            dismissButton = null
        )
    }

    if (showSoldDialog) {
        AlertDialog(
            onDismissRequest = { showSoldDialog = false },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Desglose de Ventas", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    IconButton(onClick = { showSoldDialog = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(pedidos) { pedido ->
                        val pedidoProducts = products.filter { it.pedidoId == pedido.id }
                        val pedidoSales = pedidoProducts.sumOf { it.totalSold * it.salePricePerUnit }

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(pedido.name, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(formatMoneyMain(pedidoSales, country), color = Color(0xFF2196F3))
                                Text(formatMoneySec(pedidoSales, country, bcvRate), color = Color.Gray)
                            }
                            Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp)) {
                                pedidoProducts.filter { it.totalSold > 0 }.forEach { p ->
                                    val pSales = p.salePricePerUnit * p.totalSold
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("• ${p.name} (${formatQty(p.totalSold)} vendidas)", fontSize = 12.sp)
                                        Text(formatMoneyMain(pSales, country), fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSoldDialog = false },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Cerrar") }
            },
            dismissButton = null
        )
    }

    if (showProfitDialog) {
        val salesMovements = validMovements.filter { it.type == "VENTA" }.sortedByDescending { it.timestamp }
        val saleGroups = remember(salesMovements) {
            val groups = mutableListOf<MutableList<ComercioMovement>>()
            for (m in salesMovements) {
                val lastGroup = groups.lastOrNull()
                if (lastGroup != null &&
                    Math.abs(lastGroup.first().timestamp - m.timestamp) < 5000L &&
                    lastGroup.first().note == m.note
                ) {
                    lastGroup.add(m)
                } else {
                    groups.add(mutableListOf(m))
                }
            }
            groups
        }

        AlertDialog(
            onDismissRequest = { showProfitDialog = false },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Relación de Ganancias", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    IconButton(onClick = { showProfitDialog = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                    if (saleGroups.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Aún no se han registrado ventas.\nCuando realices ventas desde el carrito, cada venta aparecerá aquí como un módulo unificado para ver sus productos y ganancias.",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(saleGroups.size) { idx ->
                                val group = saleGroups[idx]
                                val isExpanded = expandedProfitProducts.contains(idx)
                                val totalVenta = group.sumOf { it.total }
                                val totalCosto = group.sumOf { m ->
                                    (products.find { it.id == m.productId || it.name.equals(m.productName, ignoreCase = true) }?.costPerUnit ?: 0.0) * m.quantity
                                }
                                val totalGanancia = totalVenta - totalCosto
                                val clientLabel = if (group.first().note.isNotBlank()) group.first().note else "Venta #${saleGroups.size - idx}"
                                val dateStr = formatDate(group.first().timestamp)

                                val details = remember(group) { parseComercioSaleNote(group.first().note, "Venta #${saleGroups.size - idx}") }

                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // 1. Cabecera simétrica: Cliente y Ganancia
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    expandedProfitProducts = if (isExpanded) expandedProfitProducts - idx else expandedProfitProducts + idx
                                                },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(details.badgeColor.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(details.badgeIcon, fontSize = 17.sp)
                                                }
                                                Spacer(Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        details.customerName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(dateStr, fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    "+ ${formatMoneyMain(totalGanancia, country)}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (totalGanancia >= 0) Color(0xFF4CAF50) else Color.Red,
                                                    fontSize = 15.sp
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        "${group.size} ${if (group.size == 1) "producto" else "productos"}",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray
                                                    )
                                                    Spacer(Modifier.width(2.dp))
                                                    Icon(
                                                        if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = Color.Gray
                                                    )
                                                }
                                            }
                                        }

                                        // 2. Fila de Tipo de Pago y Total de la Venta
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = details.badgeColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "${details.badgeIcon} ${details.paymentType}",
                                                    color = details.badgeColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Text(
                                                "Total Venta: ${formatMoneyMain(totalVenta, country)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.Gray
                                            )
                                        }

                                        // 3. Desglose simétrico estructurado (si aplica: Fiado, Dividido o Vuelto)
                                        if (details.debeInfo != null || details.abonoInfo != null || (details.cashPart != null && details.digitalPart != null) || details.vueltoInfo != null) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    if (details.paymentType == "Fiado") {
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text("Abonado Inicial:", fontSize = 10.sp, color = Color.Gray)
                                                                Text(details.abonoInfo ?: "$0", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF388E3C))
                                                                if (details.cashPart != null || details.digitalPart != null) {
                                                                    val cashStr = details.cashPart?.let { "Ef: $it" } ?: ""
                                                                    val digStr = details.digitalPart?.let { "Dig: $it" } ?: ""
                                                                    val splitStr = listOf(cashStr, digStr).filter { it.isNotEmpty() }.joinToString(" · ")
                                                                    if (splitStr.isNotEmpty()) {
                                                                        Text(splitStr, fontSize = 10.sp, color = Color.Gray)
                                                                    }
                                                                }
                                                            }
                                                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                                                Text("Saldo Pendiente:", fontSize = 10.sp, color = Color.Gray)
                                                                Text(details.debeInfo ?: "$0", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Red)
                                                                Text("(Por cobrar)", fontSize = 10.sp, color = Color.Red.copy(alpha = 0.7f))
                                                            }
                                                        }
                                                    } else if (details.paymentType == "Pago Dividido" || (details.cashPart != null && details.digitalPart != null)) {
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text("Efectivo:", fontSize = 10.sp, color = Color.Gray)
                                                                Text(details.cashPart ?: "$0", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF388E3C))
                                                            }
                                                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text("Digital:", fontSize = 10.sp, color = Color.Gray)
                                                                Text(details.digitalPart ?: "$0", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1976D2))
                                                            }
                                                            if (details.vueltoInfo != null) {
                                                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                                                    Text("Vuelto:", fontSize = 10.sp, color = Color.Gray)
                                                                    Text(details.vueltoInfo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFE53935))
                                                                }
                                                            }
                                                        }
                                                    } else if (details.vueltoInfo != null) {
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text("Vuelto devuelto al cliente:", fontSize = 11.sp, color = Color.Gray)
                                                            Text(details.vueltoInfo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFE53935))
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Al desplegar: se muestran los productos de dicha venta
                                        if (isExpanded) {
                                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.3f))
                                            Text("Productos de esta venta:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)

                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                group.forEach { m ->
                                                    val prod = products.find { it.id == m.productId || it.name.equals(m.productName, ignoreCase = true) }
                                                    val pedidoPadre = pedidos.find { it.id == prod?.pedidoId }?.name ?: "Pedido general"
                                                    val costUnit = prod?.costPerUnit ?: 0.0
                                                    val costoTotalItem = costUnit * m.quantity
                                                    val ingresoTotalItem = m.total
                                                    val gananciaItem = ingresoTotalItem - costoTotalItem
                                                    val unit = prod?.unit ?: "Uds"

                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                                    ) {
                                                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                                Text(m.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                                Text(
                                                                    "+ ${formatMoneyMain(gananciaItem, country)}",
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (gananciaItem >= 0) Color(0xFF4CAF50) else Color.Red,
                                                                    fontSize = 13.sp
                                                                )
                                                            }

                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text("Pedido: ", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                                                Text(pedidoPadre, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                            }

                                                            Text("Vendidas: ${formatQty(m.quantity)}${unit}", fontSize = 11.sp, color = Color.Gray)

                                                            Divider(modifier = Modifier.padding(vertical = 2.dp), color = Color.LightGray.copy(alpha = 0.2f))

                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                                Column {
                                                                    Text("Compra (costo proveedor):", fontSize = 10.sp, color = Color.Gray)
                                                                    Text("${formatMoneyMain(costUnit, country)} c/u", fontSize = 10.sp)
                                                                    Text("Invertido: ${formatMoneyMain(costoTotalItem, country)}", fontSize = 10.sp, color = Color(0xFFE53935))
                                                                }
                                                                Column(horizontalAlignment = Alignment.End) {
                                                                    Text("Venta (cobrado cliente):", fontSize = 10.sp, color = Color.Gray)
                                                                    Text("${formatMoneyMain(m.pricePerUnit, country)} c/u", fontSize = 10.sp, color = Color(0xFF2196F3))
                                                                    Text("Ingreso: ${formatMoneyMain(ingresoTotalItem, country)}", fontSize = 10.sp, color = Color(0xFF2196F3))
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
            },
            confirmButton = {
                Button(
                    onClick = { showProfitDialog = false },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Cerrar") }
            },
            dismissButton = null
        )
    }

    // Modal Edit/Delete Pedido Options
    if (pedidoOptions != null) {
        val currentPedido = pedidoOptions!!
        AlertDialog(
            onDismissRequest = { pedidoOptions = null },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Opciones de Pedido", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { pedidoOptions = null }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("¿Qué deseas hacer con '${currentPedido.name}'?", fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = { pedidoToEdit = currentPedido; pedidoOptions = null },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Editar Pedido", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.deleteComercioPedido(currentPedido); pedidoOptions = null },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.Red.copy(alpha = 0.6f)))
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Red)
                        Spacer(Modifier.width(8.dp))
                        Text("Eliminar Pedido", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Red)
                    }
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = { pedidoOptions = null },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancelar")
                }
            },
            dismissButton = null
        )
    }

    // Modal Edit Pedido Form
    pedidoToEdit?.let { p ->
        var editName by remember { mutableStateOf(p.name) }
        AlertDialog(
            onDismissRequest = { pedidoToEdit = null },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Editar Pedido", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    IconButton(onClick = { pedidoToEdit = null }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }
            },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Nombre del Pedido") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { pedidoToEdit = null },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Cancelar") }
                    Button(
                        onClick = {
                            if (editName.isNotBlank()) {
                                viewModel.updateComercioPedido(p.copy(name = editName))
                                pedidoToEdit = null
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = editName.isNotBlank()
                    ) { Text("Guardar") }
                }
            },
            dismissButton = null
        )
    }

    // Modal Edit/Delete Product Options
    if (productOptions != null) {
        val currentProduct = productOptions!!
        AlertDialog(
            onDismissRequest = { productOptions = null },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Opciones de Producto", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { productOptions = null }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("¿Qué deseas hacer con '${currentProduct.name}'?", fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = { productToEdit = currentProduct; productOptions = null },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Editar Producto", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.deleteComercioProduct(currentProduct); productOptions = null },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.Red.copy(alpha = 0.6f)))
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Red)
                        Spacer(Modifier.width(8.dp))
                        Text("Eliminar Producto", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Red)
                    }
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = { productOptions = null },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancelar")
                }
            },
            dismissButton = null
        )
    }

    // Modal Edit Product Form (permite editar todo: foto, nombre, unidad, stock, costo y precio)
    productToEdit?.let { p ->
        EditComercioProductDialog(
            product = p,
            country = country,
            bcvRate = bcvRate,
            onDismiss = { productToEdit = null },
            onSave = { name, unit, stock, cost, salePrice, imageUri ->
                val diff = stock - p.quantityInStock
                viewModel.updateComercioProduct(p.copy(
                    name = name,
                    unit = unit,
                    quantityInStock = stock,
                    totalPurchased = maxOf(0.0, p.totalPurchased + diff),
                    costPerUnit = cost,
                    salePricePerUnit = salePrice,
                    imageUri = imageUri
                ))
                productToEdit = null
            }
        )
    }

    // Modal para Agregar Cantidad Disponible (+)
    productToAddStock?.let { p ->
        var addQtyStr by remember { mutableStateOf("") }
        val q = addQtyStr.toDoubleOrNull() ?: 0.0
        val newTotal = p.quantityInStock + q

        AlertDialog(
            onDismissRequest = { productToAddStock = null },
            title = { Text("Agregar Cantidad") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Producto: ${p.name}", fontWeight = FontWeight.Bold)
                    Text("Cantidad actual disponible: ${formatQty(p.quantityInStock)} ${p.unit}", fontSize = 13.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = addQtyStr,
                        onValueChange = { input ->
                            addQtyStr = if (p.unit == "Uds") {
                                cleanAmountInput(input)
                            } else {
                                com.xxcamixx.contabilidad.util.cleanDecimalWithPrecision(input, 2)
                            }
                        },
                        label = { Text("Cantidad a agregar (${p.unit})") },
                        placeholder = { Text(if (p.unit == "Uds") "Ej: 5" else "Ej: 1.46") },
                        keyboardOptions = KeyboardOptions(keyboardType = if (p.unit == "Uds") KeyboardType.Number else KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (q > 0) {
                        Text(
                            "Nueva cantidad disponible: ${formatQty(newTotal)} ${p.unit}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (q > 0) {
                            viewModel.adjustComercioStock(p, q)
                            productToAddStock = null
                        }
                    },
                    enabled = q > 0
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { productToAddStock = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal para Descontar Cantidad Disponible (-)
    productToDiscount?.let { p ->
        var discQtyStr by remember { mutableStateOf("") }
        val q = discQtyStr.toDoubleOrNull() ?: 0.0
        val available = getAvailableStock(p)
        val isExceeded = q > available
        val newTotal = maxOf(0.0, available - q)

        AlertDialog(
            onDismissRequest = { productToDiscount = null },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Descontar Cantidad", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    IconButton(onClick = { productToDiscount = null }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Producto: ${p.name}", fontWeight = FontWeight.Bold)
                    Text("Cantidad actual disponible: ${formatQty(available)} ${p.unit}", fontSize = 13.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = discQtyStr,
                        onValueChange = { input ->
                            discQtyStr = if (p.unit == "Uds") {
                                cleanAmountInput(input)
                            } else {
                                com.xxcamixx.contabilidad.util.cleanDecimalWithPrecision(input, 2)
                            }
                        },
                        label = { Text("Cantidad a descontar (${p.unit})") },
                        placeholder = { Text(if (p.unit == "Uds") "Ej: 2" else "Ej: 0.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = if (p.unit == "Uds") KeyboardType.Number else KeyboardType.Decimal),
                        singleLine = true,
                        isError = isExceeded,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isExceeded) {
                        Text(
                            "No puedes descontar más de las existencias disponibles (${formatQty(available)} ${p.unit}).",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    } else if (q > 0) {
                        Text(
                            "Nueva cantidad disponible: ${formatQty(newTotal)} ${p.unit}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (newTotal > 0) MaterialTheme.colorScheme.primary else Color.Red
                        )
                    }
                }
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { productToDiscount = null },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            if (q > 0 && !isExceeded) {
                                viewModel.adjustComercioStock(p, -q)
                                productToDiscount = null
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = q > 0 && !isExceeded,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Descontar")
                    }
                }
            },
            dismissButton = null
        )
    }

    if (showAddPedidoDialog) {
        var newName by remember { mutableStateOf("") }
        var imageUri by remember { mutableStateOf<String?>(null) }
        var showPedidoImageSourceDialog by remember { mutableStateOf(false) }

        fun onPedidoImagePicked(uri: Uri?) {
            if (uri != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    val savedUri = saveImageToInternalStorage(context, uri)
                    withContext(Dispatchers.Main) {
                        imageUri = savedUri
                    }
                }
            }
        }

        val galleryPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            onPedidoImagePicked(uri)
        }

        val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            onPedidoImagePicked(uri)
        }

        if (showPedidoImageSourceDialog) {
            ImageSourceDialog(
                onDismiss = { showPedidoImageSourceDialog = false },
                onSelectGallery = { galleryPickerLauncher.launch("image/*") },
                onSelectFileManager = { filePickerLauncher.launch(arrayOf("image/*")) }
            )
        }

        AlertDialog(
            onDismissRequest = { showAddPedidoDialog = false },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Nuevo Pedido", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    IconButton(onClick = { showAddPedidoDialog = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.2f))
                            .clickable { showPedidoImageSourceDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null) {
                            var bitmap by remember(imageUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                            LaunchedEffect(imageUri) {
                                val loadedBitmap = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                    loadBitmapFromUri(context, imageUri)
                                }
                                bitmap = loadedBitmap
                            }
                            if (bitmap != null) {
                                Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                IconButton(
                                    onClick = { imageUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Eliminar foto",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Image, contentDescription = "Añadir foto", modifier = Modifier.size(32.dp), tint = Color.Gray)
                                Text("Añadir foto al pedido (Opcional)", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nombre (Ej: Pedido Septiembre)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { showAddPedidoDialog = false },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                viewModel.addComercioPedido(newName, imageUri)
                                showAddPedidoDialog = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = newName.isNotBlank()
                    ) {
                        Text("Crear")
                    }
                }
            },
            dismissButton = null
        )
    }

    activePedidoForAdd?.let { pedido ->
        AddComercioProductDialog(
            country = country,
            bcvRate = bcvRate,
            onDismiss = { activePedidoForAdd = null },
            onSave = { name, unit, qty, cost, sp, imageUri ->
                viewModel.addComercioProduct(pedido.id, name, unit, qty, cost, sp, imageUri)
                expandedPedidos = expandedPedidos + pedido.id
            }
        )
    }

    productToCart?.let { p ->
        var qtyStr by remember { mutableStateOf("") }
        val q = qtyStr.toDoubleOrNull() ?: 0.0
        val available = getAvailableStock(p)
        val isExceeded = q > available
        val totalCosto = q * p.costPerUnit
        val totalVenta = q * p.salePricePerUnit
        val totalGanancia = totalVenta - totalCosto

        AlertDialog(
            onDismissRequest = { productToCart = null },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Añadir ${p.name}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    IconButton(onClick = { productToCart = null }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(min = 280.dp)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Disponible: ${formatQty(available)} ${p.unit}", fontSize = 12.sp, color = if (available <= 0) MaterialTheme.colorScheme.error else Color.Gray)
                        Text("Venta: ${formatMoneyMain(p.salePricePerUnit, country)} c/u", fontSize = 12.sp, color = Color(0xFF2196F3))
                    }

                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { input ->
                            qtyStr = if (p.unit == "Uds") {
                                cleanAmountInput(input)
                            } else {
                                com.xxcamixx.contabilidad.util.cleanDecimalWithPrecision(input, 2)
                            }
                        },
                        label = { Text("Cantidad a vender (${p.unit})") },
                        placeholder = { Text(if (p.unit == "Uds") "Ej: 1" else "Ej: 1.46") },
                        keyboardOptions = KeyboardOptions(keyboardType = if (p.unit == "Uds") KeyboardType.Number else KeyboardType.Decimal),
                        singleLine = true,
                        isError = isExceeded,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isExceeded) {
                        Text(
                            "La cantidad supera el stock disponible (${formatQty(available)} ${p.unit}).",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    // Tarjeta fija de cálculo: permanece siempre visible para que el modal mantenga su tamaño exacto
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Compra (Costo):", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    formatMoneyMain(totalCosto, country),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE53935)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Venta:", fontSize = 12.sp, color = Color.Gray)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        formatMoneyMain(totalVenta, country),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2196F3)
                                    )
                                    val secV = formatMoneySec(totalVenta, country, bcvRate)
                                    if (secV.isNotEmpty()) Text(secV, fontSize = 10.sp, color = Color.Gray)
                                }
                            }

                            Divider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 2.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ganancia Estimada:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        formatMoneyMain(totalGanancia, country),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalGanancia >= 0) Color(0xFF4CAF50) else Color.Red
                                    )
                                    val secG = formatMoneySec(totalGanancia, country, bcvRate)
                                    if (secG.isNotEmpty()) Text(secG, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { productToCart = null },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            if (q > 0 && !isExceeded) {
                                val existingIdx = cart.indexOfFirst { it.first.id == p.id }
                                if (existingIdx >= 0) {
                                    val old = cart[existingIdx]
                                    cart[existingIdx] = Triple(old.first, old.second + q, old.third)
                                } else {
                                    cart.add(Triple(p, q, p.salePricePerUnit))
                                }
                                productToCart = null
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = q > 0 && !isExceeded
                    ) {
                        Text("Añadir")
                    }
                }
            },
            dismissButton = null
        )
    }

    if (showCartDialog) {
        var step by remember { mutableStateOf(1) }
        var customerName by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var isDivided by remember { mutableStateOf(false) }
        var simpleMethod by remember { mutableStateOf("Efectivo") }
        var simpleReceivedRaw by remember { mutableStateOf("") }
        var cashRaw by remember { mutableStateOf("") }
        var digitalRaw by remember { mutableStateOf("") }
        var tempDueDateMillis by remember { mutableStateOf(System.currentTimeMillis() + 7 * 86400000L) }
        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }

        val cartTotal = cart.sumOf { it.second * it.third }

        var checkoutCurrency by remember { mutableStateOf("USD") }
        LaunchedEffect(Unit) { if (country == "Venezuela") checkoutCurrency = "BS" }
        val isBsInput = country == "Venezuela" && checkoutCurrency == "BS"
        val inputMultiplier = if (isBsInput && bcvRate > 0) 1 / bcvRate else 1.0
        val sym = if (isBsInput) "Bs" else "$"
        val visualTrans = if (country == "Venezuela") VisualTransformation.None else AmountVisualTransformation(prefix = "")

        val (receivedAmount, changeAmount) = if (!isDivided) {
            val rec = (simpleReceivedRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier
            Pair(rec, rec - cartTotal)
        } else {
            val cV = (cashRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier
            val dV = (digitalRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier
            val rec = cV + dV
            Pair(rec, rec - cartTotal)
        }

        if (showDatePicker) {
            CustomDatePickerDialog(
                initialDateMillis = tempDueDateMillis,
                onDismiss = { showDatePicker = false },
                onDateSelected = { selected ->
                    val prevCal = java.util.Calendar.getInstance().apply { timeInMillis = tempDueDateMillis }
                    val newCal = java.util.Calendar.getInstance().apply {
                        timeInMillis = selected
                        set(java.util.Calendar.HOUR_OF_DAY, prevCal.get(java.util.Calendar.HOUR_OF_DAY))
                        set(java.util.Calendar.MINUTE, prevCal.get(java.util.Calendar.MINUTE))
                        set(java.util.Calendar.SECOND, 0)
                    }
                    tempDueDateMillis = newCal.timeInMillis
                    showDatePicker = false
                    showTimePicker = true
                }
            )
        }

        if (showTimePicker) {
            CustomTimePickerDialog(
                initialTimeMillis = tempDueDateMillis,
                onDismiss = { showTimePicker = false },
                onTimeSelected = { hour24, minute ->
                    val cal = java.util.Calendar.getInstance().apply {
                        timeInMillis = tempDueDateMillis
                        set(java.util.Calendar.HOUR_OF_DAY, hour24)
                        set(java.util.Calendar.MINUTE, minute)
                        set(java.util.Calendar.SECOND, 0)
                    }
                    tempDueDateMillis = cal.timeInMillis
                    showTimePicker = false
                }
            )
        }

        AlertDialog(
            onDismissRequest = { showCartDialog = false },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (step) {
                            1 -> "Carrito de Ventas 🛒"
                            2 -> "Opciones de Pago 💳"
                            else -> "Registrar Deuda / Fiador 🗓️"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    IconButton(onClick = { showCartDialog = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (step == 1) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Nombre del cliente (Opcional)") },
                            placeholder = { Text("Ej: Humberto") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Productos a vender:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(max = 200.dp)
                        ) {
                            items(cart.toList()) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${item.first.name} (${formatQty(item.second)}${item.first.unit})",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                formatMoneyMain(item.second * item.third, country),
                                                color = Color(0xFF2196F3),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            val secT = formatMoneySec(item.second * item.third, country, bcvRate)
                                            if (secT.isNotEmpty()) {
                                                Text(secT, color = Color.Gray, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                    IconButton(onClick = { cart.remove(item) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Quitar", tint = Color.Red)
                                    }
                                }
                                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }

                        // Tarjeta con el total a pagar
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total a pagar:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        formatMoneyMain(cartTotal, country),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val secTotal = formatMoneySec(cartTotal, country, bcvRate)
                                    if (secTotal.isNotEmpty()) {
                                        Text(secTotal, fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    } else if (step == 2) {
                        // Tarjeta total de la compra
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total a cobrar:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        formatMoneyMain(cartTotal, country),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val secTotal = formatMoneySec(cartTotal, country, bcvRate)
                                    if (secTotal.isNotEmpty()) {
                                        Text(secTotal, fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        if (country == "Venezuela") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                FilterChip(selected = checkoutCurrency == "USD", onClick = { checkoutCurrency = "USD" }, label = { Text("Ingresar en $") })
                                FilterChip(selected = checkoutCurrency == "BS", onClick = { checkoutCurrency = "BS" }, label = { Text("Ingresar en Bs") })
                            }
                        }

                        // Selector Pago Único vs Pago Dividido
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDivided = !isDivided }
                                .padding(vertical = 4.dp)
                        ) {
                            Switch(
                                checked = isDivided,
                                onCheckedChange = { isDivided = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isDivided) "Pago Dividido (Por partes)" else "Pago Único", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        if (!isDivided) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                FilterChip(selected = simpleMethod == "Efectivo", onClick = { simpleMethod = "Efectivo" }, label = { Text("Efectivo") })
                                FilterChip(selected = simpleMethod == "Digital", onClick = { simpleMethod = "Digital" }, label = { Text("Digital") })
                            }

                            PaymentInputRow("Monto Recibido", simpleReceivedRaw, sym, visualTrans, country) { simpleReceivedRaw = it }

                            if (changeAmount > 0) {
                                Text(
                                    "Vuelto a devolver: ${formatMoneyMain(changeAmount, country)} ${formatMoneySec(changeAmount, country, bcvRate)}",
                                    color = Color(0xFFE53935),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            } else if (changeAmount < 0) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "Falta (Queda debiendo): ${formatMoneyMain(abs(changeAmount), country)} ${formatMoneySec(abs(changeAmount), country, bcvRate)}",
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { step = 3 },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D), contentColor = Color.Black)
                                    ) {
                                        Text(if (receivedAmount > 0) "Fiar el restante 🗓️" else "Fiar esta venta 🗓️", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else if (changeAmount == 0.0 && receivedAmount > 0) {
                                Text("Pago exacto ✅", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else {
                            PaymentInputRow("Efectivo Recibido", cashRaw, sym, visualTrans, country) { cashRaw = it }
                            PaymentInputRow("Digital Recibido", digitalRaw, sym, visualTrans, country) { digitalRaw = it }

                            if (changeAmount > 0) {
                                Text(
                                    "Vuelto a devolver: ${formatMoneyMain(changeAmount, country)} ${formatMoneySec(changeAmount, country, bcvRate)}",
                                    color = Color(0xFFE53935),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            } else if (changeAmount < 0) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "Falta dinero (Queda debiendo): ${formatMoneyMain(abs(changeAmount), country)} ${formatMoneySec(abs(changeAmount), country, bcvRate)}",
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { step = 3 },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D), contentColor = Color.Black)
                                    ) {
                                        Text(if (receivedAmount > 0) "Fiar el restante 🗓️" else "Fiar esta venta 🗓️", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else if (changeAmount == 0.0 && receivedAmount > 0) {
                                Text("Pago completo y exacto ✅", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    } else {
                        // Paso 3: Fiar Venta / Queda debiendo
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total de la venta:", fontSize = 12.sp, color = Color.Gray)
                                    Text(formatMoneyMain(cartTotal, country), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Abono inicial recibido:", fontSize = 12.sp, color = Color.Gray)
                                    Text(formatMoneyMain(receivedAmount, country), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF4CAF50))
                                }
                                Divider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 2.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Queda debiendo:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Red)
                                    Text(
                                        formatMoneyMain(abs(changeAmount), country),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.Red
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Nombre del cliente") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = cleanAmountInput(it) },
                            label = { Text("Teléfono (Opcional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Fecha y hora de cobro ⏰:", fontSize = 11.sp, color = Color.Gray)
                                Text(
                                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(tempDueDateMillis)),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.height(36.dp)) {
                                Text("Cambiar 📅⏰", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (step == 1) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { showCartDialog = false },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Cancelar", maxLines = 1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { step = 2 },
                            enabled = cart.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Siguiente", maxLines = 1, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (step == 2) {
                    val isEnabled = receivedAmount >= cartTotal
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { step = 1 },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Atrás", maxLines = 1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                val (netCash, netDigital) = if (!isDivided) {
                                    if (simpleMethod == "Efectivo") Pair(cartTotal, 0.0) else Pair(0.0, cartTotal)
                                } else {
                                    val cV = (cashRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier
                                    val dV = (digitalRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier
                                    Pair(cV, dV)
                                }
                                viewModel.checkoutComercioCartWithPayment(
                                    items = cart.toList(),
                                    customerName = customerName.trim(),
                                    paymentMethod = if (isDivided) "Dividido" else simpleMethod,
                                    cashReceived = netCash,
                                    digitalReceived = netDigital,
                                    changeAmount = if (changeAmount > 0) changeAmount else 0.0,
                                    isFiado = false,
                                    context = context
                                )
                                cart.clear()
                                showCartDialog = false
                            },
                            enabled = isEnabled,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Text("Confirmar Venta", maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { step = 2 },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Atrás", maxLines = 1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                val cV = if (!isDivided) {
                                    if (simpleMethod == "Efectivo") receivedAmount else 0.0
                                } else {
                                    (cashRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier
                                }
                                val dV = if (!isDivided) {
                                    if (simpleMethod == "Digital") receivedAmount else 0.0
                                } else {
                                    (digitalRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier
                                }
                                viewModel.checkoutComercioCartWithPayment(
                                    items = cart.toList(),
                                    customerName = customerName.trim(),
                                    paymentMethod = if (isDivided) "Dividido" else simpleMethod,
                                    cashReceived = cV,
                                    digitalReceived = dV,
                                    changeAmount = 0.0,
                                    isFiado = true,
                                    phone = phone.trim(),
                                    dueDateMillis = tempDueDateMillis,
                                    context = context,
                                    originMode = "PEDIDOS"
                                )
                                cart.clear()
                                showCartDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D), contentColor = Color.Black),
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Text("Guardar Fiado", maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = null
        )
    }

    if (expandedImageUri != null) {
        ExpandedImageDialog(imageUri = expandedImageUri!!, onDismiss = { expandedImageUri = null })
    }
}
