package com.xxcamixx.contabilidad.ui.dialogs

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.xxcamixx.contabilidad.model.ComercioMovement
import com.xxcamixx.contabilidad.model.ComercioPedido
import com.xxcamixx.contabilidad.model.ComercioProduct
import com.xxcamixx.contabilidad.ui.screens.formatQty
import com.xxcamixx.contabilidad.util.formatDateOnly
import com.xxcamixx.contabilidad.util.formatMoneyMain
import com.xxcamixx.contabilidad.util.formatMoneySec
import com.xxcamixx.contabilidad.util.getSmartEmoji
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComercioPedidosHistoryDialog(
    pedidos: List<ComercioPedido>,
    products: List<ComercioProduct>,
    country: String,
    bcvRate: Double,
    onDismiss: () -> Unit,
    onRestockProduct: (ComercioProduct, Double) -> Unit,
    onDeletePedido: (ComercioPedido) -> Unit
) {
    val context = LocalContext.current
    var expandedPedidos by remember { mutableStateOf(setOf<Int>()) }
    var pedidoToDelete by remember { mutableStateOf<ComercioPedido?>(null) }
    var expandedImageUri by remember { mutableStateOf<String?>(null) }
    var productToRestock by remember { mutableStateOf<ComercioProduct?>(null) }
    var restockQtyStr by remember { mutableStateOf("") }

    // Un pedido completado es aquel que tiene productos y TODOS tienen cantidad disponible <= 0
    val completedPedidos = remember(pedidos, products) {
        pedidos.filter { pedido ->
            val pList = products.filter { it.pedidoId == pedido.id }
            pList.isNotEmpty() && pList.all { it.quantityInStock <= 0 }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .fillMaxHeight(0.90f)
            .padding(8.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📦 Historial de Pedidos", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Pedidos finalizados cuyas existencias han llegado a 0 en su totalidad.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (completedPedidos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📦", fontSize = 52.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No hay pedidos en el historial",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Cuando todos los productos de un pedido lleguen a 0 unidades, dicho pedido se trasladará automáticamente aquí.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Resumen general del historial
                    val totalHistorialInvertido = completedPedidos.sumOf { ped ->
                        val pList = products.filter { it.pedidoId == ped.id }
                        pList.sumOf { it.totalPurchased * it.costPerUnit }
                    }
                    val totalHistorialVendido = completedPedidos.sumOf { ped ->
                        val pList = products.filter { it.pedidoId == ped.id }
                        pList.sumOf { it.totalSold * it.salePricePerUnit }
                    }
                    val totalHistorialGanancia = completedPedidos.sumOf { ped ->
                        val pList = products.filter { it.pedidoId == ped.id }
                        pList.sumOf { it.totalSold * (it.salePricePerUnit - it.costPerUnit) }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Completados", fontSize = 10.sp, color = Color.Gray)
                                Text("${completedPedidos.size}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Vendido", fontSize = 10.sp, color = Color.Gray)
                                Text(formatMoneyMain(totalHistorialVendido, country), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2196F3))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Ganancia", fontSize = 10.sp, color = Color.Gray)
                                Text(formatMoneyMain(totalHistorialGanancia, country), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFFFEB3B))
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(completedPedidos) { pedido ->
                            val pedidoProducts = products.filter { it.pedidoId == pedido.id }
                            val isExpanded = expandedPedidos.contains(pedido.id)

                            val pedidoInvertido = pedidoProducts.sumOf { it.totalPurchased * it.costPerUnit }
                            val pedidoVendido = pedidoProducts.sumOf { it.totalSold * it.salePricePerUnit }
                            val pedidoGanancia = pedidoProducts.sumOf { it.totalSold * (it.salePricePerUnit - it.costPerUnit) }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 1.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Encabezado del Pedido
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedPedidos = if (isExpanded) expandedPedidos - pedido.id else expandedPedidos + pedido.id
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            if (pedido.imageUri != null) {
                                                var bitmap by remember(pedido.imageUri) { mutableStateOf<Bitmap?>(null) }
                                                LaunchedEffect(pedido.imageUri) {
                                                    val b = withContext(Dispatchers.IO) {
                                                        com.xxcamixx.contabilidad.util.loadBitmapFromUri(context, pedido.imageUri)
                                                    }
                                                    bitmap = b
                                                }
                                                if (bitmap != null) {
                                                    Image(
                                                        bitmap = bitmap!!.asImageBitmap(),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                            .clickable { expandedImageUri = pedido.imageUri },
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }
                                            } else {
                                                Text(getSmartEmoji(pedido.name, true), fontSize = 24.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(pedido.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    Spacer(Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "Agotado",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF4CAF50),
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(formatDateOnly(pedido.timestamp), fontSize = 11.sp, color = Color.Gray)
                                                Text(
                                                    text = "Ganancia: ${formatMoneyMain(pedidoGanancia, country)}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFFFEB3B)
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { pedidoToDelete = pedido },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = "Eliminar pedido definitivamente",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                                contentDescription = null
                                            )
                                        }
                                    }

                                    // Contenido expandido: Productos de este pedido
                                    if (isExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Divider(color = Color.LightGray.copy(alpha = 0.4f))

                                            // Resumen de números del pedido
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Invertido: ${formatMoneyMain(pedidoInvertido, country)}", fontSize = 11.sp, color = Color(0xFFE53935))
                                                Text("Vendido: ${formatMoneyMain(pedidoVendido, country)}", fontSize = 11.sp, color = Color(0xFF2196F3))
                                            }

                                            Divider(color = Color.LightGray.copy(alpha = 0.2f))

                                            pedidoProducts.forEach { prod ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (prod.imageUri != null) {
                                                            var pbitmap by remember(prod.imageUri) { mutableStateOf<Bitmap?>(null) }
                                                            LaunchedEffect(prod.imageUri) {
                                                                val b = withContext(Dispatchers.IO) {
                                                                    com.xxcamixx.contabilidad.util.loadBitmapFromUri(context, prod.imageUri)
                                                                }
                                                                pbitmap = b
                                                            }
                                                            if (pbitmap != null) {
                                                                Image(
                                                                    bitmap = pbitmap!!.asImageBitmap(),
                                                                    contentDescription = null,
                                                                    modifier = Modifier
                                                                        .size(40.dp)
                                                                        .clip(RoundedCornerShape(6.dp))
                                                                        .clickable { expandedImageUri = prod.imageUri },
                                                                    contentScale = ContentScale.Crop
                                                                )
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                            }
                                                        }
                                                        Column {
                                                            Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                            Text(
                                                                text = "Comprados: ${formatQty(prod.totalPurchased)} ${prod.unit} | Vendidos: ${formatQty(prod.totalSold)} ${prod.unit}",
                                                                fontSize = 11.sp,
                                                                color = Color.Gray
                                                            )
                                                            Text(
                                                                text = "Disp: 0 ${prod.unit} (Agotado)",
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.error,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                            val prodProfit = prod.totalSold * (prod.salePricePerUnit - prod.costPerUnit)
                                                            Text(
                                                                text = "Ganancia generada: ${formatMoneyMain(prodProfit, country)}",
                                                                fontSize = 11.sp,
                                                                color = Color(0xFFFFEB3B),
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }

                                                    // Botón para reabastecer si se desea reactivar el pedido
                                                    FilledTonalButton(
                                                        onClick = {
                                                            productToRestock = prod
                                                            restockQtyStr = ""
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(34.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Reabastecer", fontSize = 11.sp)
                                                    }
                                                }
                                                Divider(color = Color.LightGray.copy(alpha = 0.2f))
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
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )

    // Modal para ingresar la cantidad al reabastecer producto y reactivar pedido
    if (productToRestock != null) {
        val prod = productToRestock!!
        AlertDialog(
            onDismissRequest = { productToRestock = null },
            title = { Text("Reabastecer Producto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Producto: ${prod.name}", fontWeight = FontWeight.Bold)
                    Text("Al agregar existencias, este pedido se reactivará automáticamente y volverá a aparecer en Estantes.", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = restockQtyStr,
                        onValueChange = { input ->
                            restockQtyStr = if (prod.unit == "Uds") {
                                com.xxcamixx.contabilidad.util.cleanAmountInput(input)
                            } else {
                                com.xxcamixx.contabilidad.util.cleanDecimalInput(input)
                            }
                        },
                        label = { Text("Cantidad a agregar (${prod.unit})") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = if (prod.unit == "Uds") androidx.compose.ui.text.input.KeyboardType.Number else androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                val q = restockQtyStr.toDoubleOrNull() ?: 0.0
                Button(
                    onClick = {
                        if (q > 0.0) {
                            onRestockProduct(prod, q)
                            productToRestock = null
                            restockQtyStr = ""
                        }
                    },
                    enabled = (restockQtyStr.toDoubleOrNull() ?: 0.0) > 0.0
                ) {
                    Text("Reactivar con Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToRestock = null; restockQtyStr = "" }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Confirmación para eliminar pedido definitivamente del historial
    if (pedidoToDelete != null) {
        AlertDialog(
            onDismissRequest = { pedidoToDelete = null },
            title = { Text("¿Eliminar pedido definitivamente?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Se eliminará '${pedidoToDelete?.name}' y sus registros asociados de la base de datos de pedidos.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pedidoToDelete?.let { onDeletePedido(it) }
                        pedidoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar definitivamente")
                }
            },
            dismissButton = {
                TextButton(onClick = { pedidoToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (expandedImageUri != null) {
        ExpandedImageDialog(imageUri = expandedImageUri!!) { expandedImageUri = null }
    }
}
