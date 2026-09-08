package com.xxcamixx.contabilidad.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.xxcamixx.contabilidad.model.Fiador
import com.xxcamixx.contabilidad.model.Product
import com.xxcamixx.contabilidad.model.Reminder
import com.xxcamixx.contabilidad.model.Transaction
import com.xxcamixx.contabilidad.ui.dialogs.ProductosVendidosDialog
import com.xxcamixx.contabilidad.util.formatMoneyMain
import com.xxcamixx.contabilidad.util.formatMoneySec

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
            title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Inversión \uD83D\uDCE6", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = { showInversionDialog = false }) { Icon(Icons.Filled.Close, "Cerrar") } } },
            text = { Text("El dinero total que invertiste en los productos actuales (calculado por su precio de compra original) es:\n\n${formatMoneyMain(totalInversion, selectedCountry)}", fontSize = 16.sp) },
            confirmButton = { }, dismissButton = { }, containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showGananciaFuturaDialog) {
        AlertDialog(
            onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
            title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Ganancias \uD83D\uDE80", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = { showGananciaFuturaDialog = false }) { Icon(Icons.Filled.Close, "Cerrar") } } },
            text = {
                Column {
                    Text("Ganancia Obtenida (Ventas Realizadas):", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                    Text(formatMoneyMain(totalProfit, selectedCountry), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Desglose Actual:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Caja (Efectivo):", fontSize = 13.sp); Text(formatMoneyMain(totalStoreCash, selectedCountry), fontWeight = FontWeight.Bold, color = Color(0xFF2196F3)) }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Banco (Digital):", fontSize = 13.sp); Text(formatMoneyMain(totalStoreDigital, selectedCountry), fontWeight = FontWeight.Bold, color = Color(0xFF2196F3)) }
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
                    Column(modifier = Modifier.weight(1f)) { Text("\uD83D\uDCC5 Pagar: ${reminder.title}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp); if (reminder.amount > 0) { Text("Monto: ${formatMoneyMain(reminder.amount, selectedCountry)}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp) } }
                    IconButton(onClick = { onSettleReminder(reminder) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Check, contentDescription = "Hecho", tint = Color.White) }
                }
            }
        }

        activeFiadores.forEachIndexed { index, fiador ->
            val remaining = fiador.amount - fiador.paidAmount
            AnimatedVisibility(visible = true) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).padding(top = if(index == 0 && activeReminders.isEmpty()) 16.dp else 0.dp).background(Color(0xFFFBC02D), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).clickable { onEditFiador(fiador) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) { val phoneStr = if(fiador.phone.isNotBlank()) " \uD83D\uDCDE ${fiador.phone}" else ""; Text("\uD83D\uDCB0 Cobrar a ${fiador.name}$phoneStr", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text("Resta: ${formatMoneyMain(remaining, selectedCountry)} de ${formatMoneyMain(fiador.amount, selectedCountry)} - ${fiador.reason}", color = Color.Black.copy(alpha=0.8f), fontSize = 12.sp) }
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
                        Text(text = formatMoneyMain(totalProfit, selectedCountry), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2196F3), modifier = Modifier.clickable { if (isLockedStore) showPremiumToast() else showGananciaFuturaDialog = true })
                        if (selectedCountry == "Venezuela") {
                            Text(text = formatMoneySec(totalProfit, selectedCountry, bcvRate), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (isLockedStore) showPremiumToast() else showVendidosDialog = true }, modifier = Modifier.weight(1f).height(50.dp).alpha(if(isLockedStore) 0.5f else 1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)) { Text(if(isLockedStore) "👑 Ventas" else "Ventas", fontWeight = FontWeight.Bold) }
            Button(onClick = { if (isLockedStore) showPremiumToast() else onOpenInventory() }, modifier = Modifier.weight(1f).height(50.dp).alpha(if(isLockedStore) 0.5f else 1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) { Text(if(isLockedStore) "👑 Inventario \uD83D\uDCE6" else "Inventario \uD83D\uDCE6", fontWeight = FontWeight.Bold) }
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
