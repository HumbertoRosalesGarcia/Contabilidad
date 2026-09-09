package com.xxcamixx.contabilidad

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xxcamixx.contabilidad.model.CierreSession
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierresDialog(
    onDismiss: () -> Unit,
    viewModel: com.xxcamixx.contabilidad.viewmodel.FinanceViewModel
) {
    val cierres by viewModel.cierreSessions.collectAsState(initial = emptyList())
    var showConfirmation by remember { mutableStateOf(false) }
    var modeToClose by remember { mutableStateOf("") }
    var calculatedIncome by remember { mutableStateOf(0.0) }
    var calculatedExpense by remember { mutableStateOf(0.0) }
    var nameForCierre by remember { mutableStateOf("") }

    // Get unclosed fiadores (debts) directly from viewmodel flow (or similar data)
    // to show in an alternate section.
    val fiadores by viewModel.fiadores.collectAsState(initial = emptyList())
    var currentTab by remember { mutableStateOf(0) } // 0 = Cierres, 1 = Deudas Pendientes

    var selectedCierre by remember { mutableStateOf<CierreSession?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cierres de Sesión", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar") }
                }

                TabRow(selectedTabIndex = currentTab) {
                    Tab(selected = currentTab == 0, onClick = { currentTab = 0 }, text = { Text("Sesiones") })
                    Tab(selected = currentTab == 1, onClick = { currentTab = 1 }, text = { Text("Deudas Pendientes") })
                }

                if (currentTab == 0) {
                    // Pestañas para generar nuevos cierres
                    var modeTab by remember { mutableStateOf(-1) }

                    TabRow(selectedTabIndex = if (modeTab == -1) 0 else modeTab, modifier = Modifier.padding(top = 16.dp)) {
                        Tab(selected = modeTab == 0, onClick = {
                            modeTab = 0
                            modeToClose = "PERSONAL"
                            nameForCierre = "Cierre Personal - " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                            showConfirmation = true
                        }, text = { Text("Personal", maxLines = 1, overflow = TextOverflow.Ellipsis) })

                        Tab(selected = modeTab == 1, onClick = {
                            modeTab = 1
                            modeToClose = "PEDIDOS"
                            nameForCierre = "Cierre Pedidos - " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                            showConfirmation = true
                        }, text = { Text("Pedidos", maxLines = 1, overflow = TextOverflow.Ellipsis) })

                        Tab(selected = modeTab == 2, onClick = {
                            modeTab = 2
                            modeToClose = "TIENDA"
                            nameForCierre = "Cierre Tienda - " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                            showConfirmation = true
                        }, text = { Text("Tienda", maxLines = 1, overflow = TextOverflow.Ellipsis) })
                    }

                    Divider()

                    // Lista de cierres previos
                    Text("Historial de Cierres", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                        items(cierres) { cierre ->
                            CierreItem(cierre = cierre, onDelete = { viewModel.deleteCierreSession(cierre) }, onClick = { selectedCierre = cierre })
                        }
                    }
                } else {
                    // Vista de deudas pendientes
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp)) {
                        items(fiadores) { fiador ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Cobrar a: ${fiador.name}", fontWeight = FontWeight.Bold)
                                    Text("Resta: ${fiador.amount - fiador.paidAmount} (${fiador.originMode})")
                                    Text("Motivo: ${fiador.reason}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Confirmar Cierre: $modeToClose") },
            text = {
                Column {
                    Text("¿Estás seguro de que deseas cerrar todas las transacciones pendientes para $modeToClose?")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameForCierre,
                        onValueChange = { nameForCierre = it },
                        label = { Text("Nombre del Cierre") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createCierreSession(modeToClose, nameForCierre, 0.0, 0.0)
                    showConfirmation = false
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (selectedCierre != null) {
        CierreDetailsDialog(
            cierre = selectedCierre!!,
            onDismiss = { selectedCierre = null },
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreDetailsDialog(
    cierre: CierreSession,
    onDismiss: () -> Unit,
    viewModel: com.xxcamixx.contabilidad.viewmodel.FinanceViewModel
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Detalles del Cierre", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar") }
                }
                Text("Nombre: ${cierre.name}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Text("Modo: ${cierre.mode}")
                Text("Fecha: ${sdf.format(Date(cierre.timestamp))}")

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                if (cierre.mode == "PERSONAL" || cierre.mode == "TIENDA") {
                    val transactions by viewModel.getTransactionsForCierre(cierre.id).collectAsState(initial = emptyList())
                    val incomes = transactions.filter { it.isIncome }.sumOf { it.amount }
                    val expenses = transactions.filter { !it.isIncome }.sumOf { it.amount }

                    Text("Total Ingresos: ${formatter.format(incomes)}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    Text("Total Egresos: ${formatter.format(expenses)}", fontWeight = FontWeight.Bold, color = Color.Red)

                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Transacciones:", fontWeight = FontWeight.Bold)
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(transactions) { t ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(t.description, fontWeight = FontWeight.Bold)
                                    Text("Monto: ${formatter.format(t.amount)}", color = if (t.isIncome) Color(0xFF4CAF50) else Color.Red)
                                    if (t.note.isNotBlank()) Text("Nota: ${t.note}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                } else if (cierre.mode == "PEDIDOS") {
                    val movements by viewModel.getComercioMovementsForCierre(cierre.id).collectAsState(initial = emptyList())
                    Text("Movimientos de Pedidos:", fontWeight = FontWeight.Bold)
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(movements) { m ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(if (m.type == "IN") "Entrada: ${m.quantity}" else "Salida: ${m.quantity}", fontWeight = FontWeight.Bold, color = if (m.type == "IN") Color(0xFF4CAF50) else Color.Red)
                                    if (m.note.isNotBlank()) Text("Nota: ${m.note}", style = MaterialTheme.typography.bodySmall)
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
fun CierreItem(cierre: CierreSession, onDelete: () -> Unit, onClick: () -> Unit = {}) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(cierre.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onDelete) {
                    Text("Eliminar", color = Color.Red, maxLines = 1, softWrap = false)
                }
            }
            Text("Modo: ${cierre.mode}")
            Text("Fecha: ${sdf.format(Date(cierre.timestamp))}")
            // Uncomment if tracking totals strictly per mode
            // Text("Ingresos: ${formatter.format(cierre.totalIncomes)}")
            // Text("Gastos: ${formatter.format(cierre.totalExpenses)}")
        }
    }
}
