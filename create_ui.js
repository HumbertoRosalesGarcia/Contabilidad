const fs = require('fs');
const path = require('path');

const dirDialogs = 'd:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/dialogs';
const dirScreens = 'd:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/screens';

// AddComercioProductDialog.kt
fs.writeFileSync(path.join(dirDialogs, 'AddComercioProductDialog.kt'), `package com.xxcamixx.contabilidad.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xxcamixx.contabilidad.util.cleanAmountInput

@Composable
fun AddComercioProductDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, unit: String, quantity: Double, cost: Double, salePrice: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }
    var quantityStr by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var salePriceStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Pedido/Producto") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Producto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("kg", "lb", "Uds", "paq").forEach { u ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(selected = unit == u, onClick = { unit = u })
                            Text(u, fontSize = 14.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = cleanAmountInput(it) },
                    label = { Text("Cantidad comprada") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = cleanAmountInput(it) },
                    label = { Text("Costo Total o por Unidad (Tu Inversión)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = salePriceStr,
                    onValueChange = { salePriceStr = cleanAmountInput(it) },
                    label = { Text("Precio de Venta Sugerido (Opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val q = quantityStr.toDoubleOrNull() ?: 0.0
                val c = costStr.toDoubleOrNull() ?: 0.0
                val sp = salePriceStr.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && q > 0 && c > 0) {
                    onSave(name, unit, q, c, sp)
                    onDismiss()
                }
            }) { Text("Guardar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
`, 'utf-8');

// SellComercioDialog.kt
fs.writeFileSync(path.join(dirDialogs, 'SellComercioDialog.kt'), `package com.xxcamixx.contabilidad.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xxcamixx.contabilidad.model.ComercioProduct
import com.xxcamixx.contabilidad.util.cleanAmountInput
import com.xxcamixx.contabilidad.util.formatCOP

@Composable
fun SellComercioDialog(
    product: ComercioProduct,
    onDismiss: () -> Unit,
    onSave: (quantity: Double, salePrice: Double) -> Unit
) {
    var quantityStr by remember { mutableStateOf("") }
    var salePriceStr by remember { mutableStateOf(if(product.salePricePerUnit > 0) product.salePricePerUnit.toInt().toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vender \${product.name}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Stock disponible: \${product.quantityInStock} \${product.unit}")
                Text("Costo promedio: \${formatCOP(product.costPerUnit)}")
                
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = cleanAmountInput(it) },
                    label = { Text("Cantidad a vender") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = salePriceStr,
                    onValueChange = { salePriceStr = cleanAmountInput(it) },
                    label = { Text("Precio de Venta (por \${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                val q = quantityStr.toDoubleOrNull() ?: 0.0
                val p = salePriceStr.toDoubleOrNull() ?: 0.0
                if (q > 0 && p > 0) {
                    val total = q * p
                    val profit = (p - product.costPerUnit) * q
                    Text("Total Venta: \${formatCOP(total)}", color = MaterialTheme.colorScheme.primary)
                    Text("Ganancia Neta: \${formatCOP(profit)}", color = if(profit>=0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val q = quantityStr.toDoubleOrNull() ?: 0.0
                val p = salePriceStr.toDoubleOrNull() ?: 0.0
                if (q > 0 && q <= product.quantityInStock && p > 0) {
                    onSave(q, p)
                    onDismiss()
                }
            }) { Text("Confirmar Venta") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
`, 'utf-8');

// RestockComercioDialog.kt
fs.writeFileSync(path.join(dirDialogs, 'RestockComercioDialog.kt'), `package com.xxcamixx.contabilidad.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xxcamixx.contabilidad.model.ComercioProduct
import com.xxcamixx.contabilidad.util.cleanAmountInput

@Composable
fun RestockComercioDialog(
    product: ComercioProduct,
    onDismiss: () -> Unit,
    onSave: (quantity: Double, cost: Double) -> Unit
) {
    var quantityStr by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reabastecer \${product.name}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Stock actual: \${product.quantityInStock} \${product.unit}")
                
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = cleanAmountInput(it) },
                    label = { Text("Cantidad nueva comprada") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = cleanAmountInput(it) },
                    label = { Text("Nuevo costo (por \${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val q = quantityStr.toDoubleOrNull() ?: 0.0
                val c = costStr.toDoubleOrNull() ?: 0.0
                if (q > 0 && c > 0) {
                    onSave(q, c)
                    onDismiss()
                }
            }) { Text("Guardar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
`, 'utf-8');

// ComercioScreen.kt
fs.writeFileSync(path.join(dirScreens, 'ComercioScreen.kt'), `package com.xxcamixx.contabilidad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xxcamixx.contabilidad.model.ComercioProduct
import com.xxcamixx.contabilidad.model.ComercioMovement
import com.xxcamixx.contabilidad.util.formatCOP
import com.xxcamixx.contabilidad.viewmodel.FinanceViewModel
import com.xxcamixx.contabilidad.ui.dialogs.AddComercioProductDialog
import com.xxcamixx.contabilidad.ui.dialogs.SellComercioDialog
import com.xxcamixx.contabilidad.ui.dialogs.RestockComercioDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComercioScreen(
    viewModel: FinanceViewModel,
    products: List<ComercioProduct>,
    movements: List<ComercioMovement>
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var sellProduct by remember { mutableStateOf<ComercioProduct?>(null) }
    var restockProduct by remember { mutableStateOf<ComercioProduct?>(null) }
    var viewMode by remember { mutableStateOf("INVENTARIO") }

    val totalInvested = movements.filter { it.type == "COMPRA" }.sumOf { it.total }
    val totalSold = movements.filter { it.type == "VENTA" }.sumOf { it.total }
    val totalVentasNeto = movements.filter { it.type == "VENTA" }.sumOf { it.total }
    
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Panel de Comercio ??", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Ingresos", fontSize = 12.sp)
                        Text(formatCOP(totalVentasNeto), color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Productos Activos", fontSize = 12.sp)
                        Text("\${products.count { it.quantityInStock > 0 }}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            FilterChip(selected = viewMode == "INVENTARIO", onClick = { viewMode = "INVENTARIO" }, label = { Text("Inventario") })
            FilterChip(selected = viewMode == "HISTORIAL", onClick = { viewMode = "HISTORIAL" }, label = { Text("Historial") })
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo")
                Spacer(Modifier.width(4.dp))
                Text("Pedido")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (viewMode == "INVENTARIO") {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(products) { p ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Stock: \${p.quantityInStock} \${p.unit}", color = if(p.quantityInStock <= 0) Color.Red else MaterialTheme.colorScheme.onSurface)
                            }
                            Text("Costo promed: \${formatCOP(p.costPerUnit)}", fontSize = 12.sp, color = Color.Gray)
                            Text("Precio Venta sugerido: \${formatCOP(p.salePricePerUnit)}", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                OutlinedButton(onClick = { restockProduct = p }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Surtir", fontSize = 12.sp)
                                }
                                Button(onClick = { sellProduct = p }, enabled = p.quantityInStock > 0) {
                                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Vender", fontSize = 12.sp)
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
                                Text("\${m.productName} (\${m.quantity})", fontWeight = FontWeight.Bold)
                                Text(if (isCompra) "Comprado" else "Vendido", fontSize = 12.sp, color = if (isCompra) Color(0xFFE53935) else Color(0xFF2196F3))
                            }
                            Text(formatCOP(m.total), fontWeight = FontWeight.Bold, color = if (isCompra) Color(0xFFE53935) else Color(0xFF2196F3))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddComercioProductDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, unit, qty, cost, sp -> viewModel.addComercioProduct(name, unit, qty, cost, sp) }
        )
    }
    
    sellProduct?.let { p ->
        SellComercioDialog(
            product = p,
            onDismiss = { sellProduct = null },
            onSave = { qty, sp -> viewModel.sellComercioProduct(p, qty, sp) }
        )
    }
    
    restockProduct?.let { p ->
        RestockComercioDialog(
            product = p,
            onDismiss = { restockProduct = null },
            onSave = { qty, cost -> viewModel.restockComercioProduct(p, qty, cost) }
        )
    }
}
`, 'utf-8');

console.log('UI files created');
