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
                    // Botones para generar nuevos cierres
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = {
                            modeToClose = "PERSONAL"
                            nameForCierre = "Cierre Personal - " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                            showConfirmation = true
                        }) { Text("Personal") }
                        Button(onClick = {
                            modeToClose = "PEDIDOS"
                            nameForCierre = "Cierre Pedidos - " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                            showConfirmation = true
                        }) { Text("Pedidos") }
                        Button(onClick = {
                            modeToClose = "TIENDA"
                            nameForCierre = "Cierre Tienda - " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                            showConfirmation = true
                        }) { Text("Tienda") }
                    }

                    Divider()

                    // Lista de cierres previos
                    Text("Historial de Cierres", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                        items(cierres) { cierre ->
                            CierreItem(cierre = cierre, onDelete = { viewModel.deleteCierreSession(cierre) })
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
}

@Composable
fun CierreItem(cierre: CierreSession, onDelete: () -> Unit) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(cierre.name, fontWeight = FontWeight.Bold)
                TextButton(onClick = onDelete) {
                    Text("Eliminar", color = Color.Red)
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
