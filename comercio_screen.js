const fs = require('fs');

const code = `package com.xxcamixx.contabilidad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xxcamixx.contabilidad.model.ComercioPedido
import com.xxcamixx.contabilidad.model.ComercioProduct
import com.xxcamixx.contabilidad.model.ComercioMovement
import com.xxcamixx.contabilidad.util.formatCOP
import com.xxcamixx.contabilidad.util.formatDateOnly
import com.xxcamixx.contabilidad.util.cleanAmountInput
import com.xxcamixx.contabilidad.viewmodel.FinanceViewModel
import com.xxcamixx.contabilidad.ui.dialogs.AddComercioProductDialog

fun formatQty(qty: Double): String = if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComercioScreen(
    viewModel: FinanceViewModel,
    pedidos: List<ComercioPedido>,
    products: List<ComercioProduct>,
    movements: List<ComercioMovement>
) {
    var showAddPedidoDialog by remember { mutableStateOf(false) }
    var activePedidoForAdd by remember { mutableStateOf<ComercioPedido?>(null) }
    var viewMode by remember { mutableStateOf("INVENTARIO") }
    var expandedPedidos by remember { mutableStateOf(setOf<Int>()) }

    // Cart: Triple(Product, Quantity, SalePrice)
    val cart = remember { mutableStateListOf<Triple<ComercioProduct, Double, Double>>() }
    var showCartDialog by remember { mutableStateOf(false) }
    var productToCart by remember { mutableStateOf<ComercioProduct?>(null) }

    val totalInvested = movements.filter { it.type == "COMPRA" }.sumOf { it.total }
    val totalSold = movements.filter { it.type == "VENTA" }.sumOf { it.total }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Panel de Pedidos \uD83D\uDCE6", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total Invertido", fontSize = 12.sp)
                            Text(formatCOP(totalInvested), color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Total Vendido", fontSize = 12.sp)
                            Text(formatCOP(totalSold), color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilterChip(selected = viewMode == "INVENTARIO", onClick = { viewMode = "INVENTARIO" }, label = { Text("Estantes") })
                FilterChip(selected = viewMode == "HISTORIAL", onClick = { viewMode = "HISTORIAL" }, label = { Text("Movimientos") })
                Button(onClick = { showAddPedidoDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuevo")
                    Spacer(Modifier.width(4.dp))
                    Text("Pedido")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (viewMode == "INVENTARIO") {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(pedidos) { pedido ->
                        val pedidoProducts = products.filter { it.pedidoId == pedido.id }
                        val isExpanded = expandedPedidos.contains(pedido.id)
                        
                        val inv = pedidoProducts.sumOf { it.totalPurchased * it.costPerUnit }
                        val sold = pedidoProducts.sumOf { it.totalSold * it.costPerUnit } // Base cost of sold
                        val revenue = pedidoProducts.sumOf { it.totalSold * it.salePricePerUnit }
                        
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 1.dp)) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { 
                                        expandedPedidos = if (isExpanded) expandedPedidos - pedido.id else expandedPedidos + pedido.id
                                    }.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(pedido.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(formatDateOnly(pedido.timestamp), fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Icon(if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
                                }
                                
                                if (isExpanded) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                        Divider()
                                        pedidoProducts.forEach { p ->
                                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(p.name, fontWeight = FontWeight.Bold)
                                                    Text("Disp: \${formatQty(p.quantityInStock)} \${p.unit}", fontSize = 12.sp, color = if(p.quantityInStock<=0) Color.Red else Color.Unspecified)
                                                    Text("Costo: \${formatCOP(p.costPerUnit)}", fontSize = 12.sp, color = Color.Gray)
                                                }
                                                Button(
                                                    onClick = { productToCart = p },
                                                    enabled = p.quantityInStock > 0,
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Añadir", fontSize = 12.sp)
                                                }
                                            }
                                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                                        }
                                        OutlinedButton(
                                            onClick = { activePedidoForAdd = pedido },
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Agregar producto a este pedido")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(movements) { m ->
                        val isCompra = m.type == "COMPRA"
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("\${m.productName} (\${formatQty(m.quantity)})", fontWeight = FontWeight.Bold)
                                    Text(if (isCompra) "Inversión" else "Venta", fontSize = 12.sp, color = if (isCompra) Color(0xFFE53935) else Color(0xFF2196F3))
                                    Text(formatDateOnly(m.timestamp), fontSize = 10.sp, color = Color.Gray)
                                }
                                Text(formatCOP(m.total), fontWeight = FontWeight.Bold, color = if (isCompra) Color(0xFFE53935) else Color(0xFF2196F3))
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
                    Text("\${cart.size}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAddPedidoDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddPedidoDialog = false },
            title = { Text("Nuevo Pedido") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nombre (Ej: Pedido Septiembre)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.addComercioPedido(newName)
                        showAddPedidoDialog = false
                    }
                }) { Text("Crear") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddPedidoDialog = false }) { Text("Cancelar") }
            }
        )
    }

    activePedidoForAdd?.let { pedido ->
        AddComercioProductDialog(
            onDismiss = { activePedidoForAdd = null },
            onSave = { name, unit, qty, cost, sp ->
                viewModel.addComercioProduct(pedido.id, name, unit, qty, cost, sp)
                expandedPedidos = expandedPedidos + pedido.id
            }
        )
    }

    productToCart?.let { p ->
        var qtyStr by remember { mutableStateOf("") }
        var spStr by remember { mutableStateOf(if (p.salePricePerUnit > 0) p.salePricePerUnit.toInt().toString() else "") }
        
        AlertDialog(
            onDismissRequest = { productToCart = null },
            title = { Text("Añadir \${p.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Disponible: \${formatQty(p.quantityInStock)} \${p.unit}")
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = cleanAmountInput(it) },
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = spStr,
                        onValueChange = { spStr = cleanAmountInput(it) },
                        label = { Text("Precio de Venta (c/u)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val q = qtyStr.toDoubleOrNull() ?: 0.0
                    val sp = spStr.toDoubleOrNull() ?: 0.0
                    if (q > 0 && q <= p.quantityInStock && sp > 0) {
                        cart.add(Triple(p, q, sp))
                        productToCart = null
                    }
                }) { Text("Añadir") }
            },
            dismissButton = {
                OutlinedButton(onClick = { productToCart = null }) { Text("Cancelar") }
            }
        )
    }

    if (showCartDialog) {
        AlertDialog(
            onDismissRequest = { showCartDialog = false },
            title = { Text("Carrito de Ventas") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(cart.toList()) { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("\${item.first.name} (\${formatQty(item.second)})", fontWeight = FontWeight.Bold)
                                Text("\${formatCOP(item.second * item.third)}", color = Color(0xFF2196F3))
                            }
                            IconButton(onClick = { cart.remove(item) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Quitar", tint = Color.Red)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (cart.isNotEmpty()) {
                        viewModel.checkoutComercioCart(cart.toList())
                        cart.clear()
                        showCartDialog = false
                    }
                }) { Text("Confirmar Venta") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCartDialog = false }) { Text("Cerrar") }
            }
        )
    }
}
`
fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens/ComercioScreen.kt', code, 'utf-8');
console.log('ComercioScreen recreated.');
