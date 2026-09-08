package com.xxcamixx.contabilidad.ui.dialogs

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xxcamixx.contabilidad.model.*
import com.xxcamixx.contabilidad.ui.components.TransactionItem
import com.xxcamixx.contabilidad.util.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExpandedImageDialog(imageUri: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
            var bitmap by remember(imageUri) { mutableStateOf<android.graphics.Bitmap?>(null) }

            LaunchedEffect(imageUri) {
                val loadedBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        if (imageUri.startsWith("data:image") || imageUri.length > 1000) {
                            val b64 = imageUri.substringAfter(",")
                            val decoded = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                            android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                        } else {
                            val uri = Uri.parse(imageUri)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, uri))
                            } else {
                                @Suppress("DEPRECATION")
                                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                            }
                        }
                    } catch (e: Exception) { null }
                }
                bitmap = loadedBitmap
            }

            if (bitmap != null) {
                Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = "Imagen Expandida", modifier = Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Fit)
            } else {
                CircularProgressIndicator(color = Color.White)
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(32.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
    }
}

@Composable
fun CustomDatePickerDialog(initialDateMillis: Long?, onDismiss: () -> Unit, onDateSelected: (Long) -> Unit) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); if (initialDateMillis != null) { timeInMillis = initialDateMillis; set(Calendar.DAY_OF_MONTH, 1) } }) }
    var selectedDate by remember { mutableStateOf<Calendar?>(initialDateMillis?.let { Calendar.getInstance().apply { timeInMillis = it } }) }
    val formatMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false), modifier = Modifier.fillMaxWidth().padding(16.dp), containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Seleccionar Fecha 🗓️", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") }
            }
        },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { val newCal = currentMonth.clone() as Calendar; newCal.add(Calendar.MONTH, -1); newCal.set(Calendar.DAY_OF_MONTH, 1); currentMonth = newCal }) { Icon(Icons.Filled.ChevronLeft, "Anterior") }
                    Text(text = formatMonth.format(currentMonth.time).replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { val newCal = currentMonth.clone() as Calendar; newCal.add(Calendar.MONTH, 1); newCal.set(Calendar.DAY_OF_MONTH, 1); currentMonth = newCal }) { Icon(Icons.Filled.ChevronRight, "Siguiente") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb").forEach {
                        Text(text = it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                val tempCal = currentMonth.clone() as Calendar
                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1
                val totalCells = daysInMonth + firstDayOfWeek
                val rows = (totalCells + 6) / 7

                Column(modifier = Modifier.fillMaxWidth()) {
                    for (i in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (j in 0..6) {
                                val cellIndex = i * 7 + j
                                val dayNumber = cellIndex - firstDayOfWeek + 1
                                if (dayNumber in 1..daysInMonth) {
                                    val dayCal = currentMonth.clone() as Calendar
                                    dayCal.set(Calendar.DAY_OF_MONTH, dayNumber)
                                    val isDaySelected = selectedDate != null && isSameDay(selectedDate!!.timeInMillis, dayCal.timeInMillis)
                                    Box(
                                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(CircleShape).background(if (isDaySelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent).clickable { selectedDate = dayCal.clone() as Calendar },
                                        contentAlignment = Alignment.Center
                                    ) { Text(text = dayNumber.toString(), fontSize = 14.sp, fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Normal, color = if (isDaySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
                                } else { Box(modifier = Modifier.weight(1f).aspectRatio(1f)) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { if (selectedDate != null) onDateSelected(selectedDate!!.timeInMillis) }, enabled = selectedDate != null) { Text("Seleccionar") } },
        dismissButton = { }
    )
}

@Composable
fun CustomTimePickerDialog(
    initialTimeMillis: Long,
    onDismiss: () -> Unit,
    onTimeSelected: (hour24: Int, minute: Int) -> Unit
) {
    val cal = remember { java.util.Calendar.getInstance().apply { timeInMillis = initialTimeMillis } }
    val currentHourInt = cal.get(java.util.Calendar.HOUR).let { if (it == 0) 12 else it }
    val currentMinInt = cal.get(java.util.Calendar.MINUTE)
    val currentHourStr = currentHourInt.toString()
    val currentMinStr = currentMinInt.toString().padStart(2, '0')

    var customHour by remember { mutableStateOf(currentHourStr) }
    var customMinute by remember { mutableStateOf(currentMinStr) }
    var isPm by remember { mutableStateOf(cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.PM) }
    val minuteFocusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnClickOutside = false),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Hora de Notificación ⏰", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                val phoneTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                Text("Hora actual del teléfono: $phoneTime", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))
                Text("¿A qué hora deseas que suene la alarma para cobrar?", fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = customHour,
                        onValueChange = { input ->
                            if (input.isEmpty()) { customHour = input }
                            else if (input.length <= 2 && input.all { char -> char.isDigit() }) {
                                val h = input.toIntOrNull()
                                if (h != null) {
                                    if (input.length == 1 && h == 0) { customHour = input }
                                    else if (h in 1..12) {
                                        customHour = input
                                        if (input.length == 2) minuteFocusRequester.requestFocus()
                                    }
                                }
                            }
                        },
                        placeholder = { Text(currentHourStr, color = Color.Gray.copy(alpha=0.4f), fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        modifier = Modifier.width(76.dp),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 28.sp, fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Text(" : ", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    OutlinedTextField(
                        value = customMinute,
                        onValueChange = { input ->
                            if (input.isEmpty()) { customMinute = input }
                            else if (input.length <= 2 && input.all { it.isDigit() }) {
                                val m = input.toIntOrNull()
                                if (m != null && m in 0..59) { customMinute = input }
                            }
                        },
                        placeholder = { Text(currentMinStr, color = Color.Gray.copy(alpha=0.4f), fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        modifier = Modifier.width(76.dp).focusRequester(minuteFocusRequester),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 28.sp, fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(selected = !isPm, onClick = { isPm = false }, label = { Text("AM", fontWeight = FontWeight.Bold) })
                        FilterChip(selected = isPm, onClick = { isPm = true }, label = { Text("PM", fontWeight = FontWeight.Bold) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalH = customHour.toIntOrNull() ?: currentHourInt
                    val finalM = customMinute.toIntOrNull() ?: currentMinInt
                    var hour24 = finalH
                    if (isPm && hour24 < 12) hour24 += 12
                    if (!isPm && hour24 == 12) hour24 = 0
                    onTimeSelected(hour24, finalM)
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirmar Hora ⏰", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        dismissButton = { }
    )
}


@Composable
fun AddToCartDialog(product: Product, currentCartQty: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var qtyRaw by remember { mutableStateOf("") }
    val maxAvailable = product.stock - currentCartQty
    val focusRequester = remember { FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { delay(200); focusRequester.requestFocus(); keyboardController?.show() }

    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Añadir al Carrito 🛒", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Disponible para añadir: $maxAvailable ${product.unit}", color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = qtyRaw, onValueChange = { n -> val d = n.filter { it.isDigit() }; qtyRaw = d }, label = { Text("Cantidad") }, placeholder = { Text("0", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.Gray.copy(alpha = 0.5f)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.width(150.dp).padding(vertical = 8.dp).focusRequester(focusRequester), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                )
            }
        },
        confirmButton = { Button(onClick = { val q = qtyRaw.toIntOrNull() ?: 0; if (q > 0 && q <= maxAvailable) { onConfirm(q) } }) { Text("Añadir") } },
        dismissButton = { }
    )
}

@Composable
fun LimitDialog(currentLimit: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    val initial = if(currentLimit > 0) currentLimit.toLong().toString() else ""; var amountRaw by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Saldo Crítico 🔔", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
        containerColor = MaterialTheme.colorScheme.surface,
        text = { Column { Text("Te avisaremos si tu saldo baja de esta cantidad (Déjalo en 0 para apagar):", fontSize = 14.sp); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = amountRaw, onValueChange = { amountRaw = cleanAmountInput(it) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), leadingIcon = { Icon(Icons.Filled.AttachMoney, null) }, singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = AmountVisualTransformation()) } },
        confirmButton = { Button(onClick = { onConfirm(amountRaw.toDoubleOrNull() ?: 0.0) }) { Text("Guardar") } }, dismissButton = { }
    )
}

@Composable
fun SummaryDialog(totalIncome: Double, totalExpense: Double, balance: Double, transactionCount: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Resumen de Totales 📊", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
        containerColor = MaterialTheme.colorScheme.surface,
        text = { Column(modifier = Modifier.fillMaxWidth()) { Text("Aquí tienes el balance histórico general de todos tus movimientos personales:", fontSize = 14.sp, color = Color.Gray); Spacer(modifier = Modifier.height(16.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total Ingresos:", fontWeight = FontWeight.Bold); Column(horizontalAlignment = Alignment.End) { Text(formatCOP(totalIncome), color = Color(0xFF2196F3), fontWeight = FontWeight.Bold) } }; Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total Gastado:", fontWeight = FontWeight.Bold); Column(horizontalAlignment = Alignment.End) { Text(formatCOP(totalExpense), color = Color(0xFFF44336), fontWeight = FontWeight.Bold) } }; Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Balance Total:", fontWeight = FontWeight.Bold); Column(horizontalAlignment = Alignment.End) { Text(formatCOP(balance), color = if (balance >= 0) Color(0xFF2196F3) else Color(0xFFE53935), fontWeight = FontWeight.Bold) } }; Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Cant. Movimientos:", fontWeight = FontWeight.Bold); Text("$transactionCount", fontWeight = FontWeight.Bold) } } },
        confirmButton = { }, dismissButton = { }
    )
}

@Composable
fun DeleteQuantityDialog(product: Product, initialQty: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var qtyRaw by remember { mutableStateOf(initialQty) }; val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Eliminar Stock 🗑️", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
        containerColor = MaterialTheme.colorScheme.surface,
        text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { Text("¿Cuántas unidades de '${product.name}' deseas eliminar?", textAlign = TextAlign.Center, fontSize = 14.sp); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = qtyRaw, onValueChange = { n -> val d = n.filter { it.isDigit() }; qtyRaw = d }, label = { Text("Cantidad") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.width(150.dp), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.Bold)); Text("Stock actual: ${product.stock} ${product.unit}", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) } },
        confirmButton = { Button(onClick = { val q = qtyRaw.toIntOrNull() ?: -1; if (q > product.stock) { Toast.makeText(context, "Supera el stock actual.", Toast.LENGTH_SHORT).show() } else if (q <= 0) { Toast.makeText(context, "No se puede eliminar 0.", Toast.LENGTH_SHORT).show() } else { onConfirm(q) } }) { Text("Siguiente") } }, dismissButton = { }
    )
}

@Composable
fun RedWarningDialog(productName: String, qty: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false), containerColor = Color(0xFFD32F2F), titleContentColor = Color.White, textContentColor = Color.White,
        title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("¡Acción Irreversible! ⚠️", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar", tint = Color.White) } } },
        text = { Text("Estás a punto de eliminar $qty unidades de '$productName' de tu inventario. ¿Estás seguro?") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFD32F2F))) { Text("Sí, Eliminar", fontWeight = FontWeight.Bold) } }, dismissButton = { }
    )
}

@Composable
fun ProductosVendidosDialog(
    transactions: List<Transaction>,
    activeFiadores: List<Fiador>,
    onDismiss: () -> Unit,
    onDeleteVentas: (List<Transaction>) -> Unit,
    onRestoreFiador: (String, Double, Double) -> Unit // NUEVO
) {
    val context = LocalContext.current
    val ventas = remember(transactions) { transactions.filter { it.isIncome && it.description.startsWith("Venta") } }
    var searchQuery by remember { mutableStateOf("") }

    val processedItems = remember(ventas, searchQuery) {
        val filteredVentas = ventas.filter { sale -> sale.description.contains(searchQuery, ignoreCase = true) || sale.note.contains(searchQuery, ignoreCase = true) || formatDate(sale.timestamp).contains(searchQuery, ignoreCase = true) }

        val list = mutableListOf<Any>()
        val fiadorGroups = mutableMapOf<String, MutableList<Transaction>>()

        filteredVentas.forEach { t ->
            val name = when {
                t.description.startsWith("Venta: Abono inicial (") && t.description.endsWith(")") -> t.description.removePrefix("Venta: Abono inicial (").removeSuffix(")")
                t.description.startsWith("Venta a crédito (") && t.description.endsWith(")") -> t.description.removePrefix("Venta a crédito (").removeSuffix(")")
                t.description.startsWith("Venta: Abono de ") -> t.description.removePrefix("Venta: Abono de ")
                else -> null
            }

            if (name != null) {
                fiadorGroups.getOrPut(name) { mutableListOf() }.add(t)
            } else {
                list.add(t)
            }
        }

        fiadorGroups.forEach { (name, txs) ->
            list.add(Pair(name, txs.sortedByDescending { it.timestamp }))
        }

        list.sortedByDescending {
            if (it is Transaction) it.timestamp
            else {
                @Suppress("UNCHECKED_CAST")
                val pair = it as Pair<String, List<Transaction>>
                pair.second.firstOrNull()?.timestamp ?: 0L
            }
        }
    }

    val totalMonto = remember(ventas) { ventas.sumOf { it.amount } }
    val totalGanancia = remember(ventas) { ventas.sumOf { it.profit } }
    var showConfirmDelete by remember { mutableStateOf(false) }

    // NUEVO ESTADO PARA RESTAURAR FIADOR
    var fiadorToRestoreName by remember { mutableStateOf<String?>(null) }
    var fiadorToRestoreAbonado by remember { mutableStateOf(0.0) }

    if (showConfirmDelete) { AlertDialog(onDismissRequest = { showConfirmDelete = false }, title = { Text("Limpiar Historial ⚠️", fontWeight = FontWeight.Bold) }, text = { Text("¿Estás seguro de que deseas borrar este historial de ventas?\n\nEsta acción eliminará permanentemente todos los registros mostrados actualmente.") }, confirmButton = { Button(onClick = { onDeleteVentas(ventas); showConfirmDelete = false; onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))) { Text("Limpiar Todo", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { showConfirmDelete = false }) { Text("Cancelar") } }) }

    // NUEVO DIÁLOGO DE RESTAURACIÓN
    if (fiadorToRestoreName != null) {
        var restoreAmountRaw by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { fiadorToRestoreName = null },
            title = { Text("Retomar Deuda ♻️", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column {
                    Text("Ingresa la deuda TOTAL original de ${fiadorToRestoreName}. (Debe ser mayor a lo que ya abonó: ${formatCOP(fiadorToRestoreAbonado)})", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = restoreAmountRaw,
                        onValueChange = { restoreAmountRaw = cleanAmountInput(it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Monto TOTAL de la deuda") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val totalAmount = restoreAmountRaw.toDoubleOrNull() ?: 0.0
                    if (totalAmount > fiadorToRestoreAbonado) {
                        onRestoreFiador(fiadorToRestoreName!!, totalAmount, fiadorToRestoreAbonado)
                        fiadorToRestoreName = null
                    } else {
                        Toast.makeText(context, "Monto total inválido", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { fiadorToRestoreName = null }) { Text("Cancelar") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Productos Vendidos 🛍️", fontWeight = FontWeight.Bold, fontSize = 20.sp); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { if(ventas.isEmpty()) { Toast.makeText(context, "No hay ventas para exportar", Toast.LENGTH_SHORT).show(); return@OutlinedButton }; val reporte = buildString { appendLine("📊 REPORTE DE VENTAS"); appendLine("Fecha de Exportación: ${formatDate(System.currentTimeMillis())}"); appendLine("--------------------------------"); ventas.forEachIndexed { index, sale -> appendLine("Venta #${ventas.size - index} - ${formatDateOnly(sale.timestamp)}"); appendLine(sale.description); appendLine(sale.note); appendLine("Total: ${formatCOP(sale.amount)} | Ganancia: ${formatCOP(sale.profit)}"); appendLine("--------------------------------") }; appendLine("TOTAL VENTAS: ${formatCOP(totalMonto)}"); appendLine("TOTAL GANANCIA: ${formatCOP(totalGanancia)}") }; val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, reporte) }; context.startActivity(Intent.createChooser(intent, "Exportar Reporte")) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Filled.Share, contentDescription = "Exportar", modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Exportar", fontSize = 12.sp) }
                    OutlinedButton(onClick = { if(ventas.isNotEmpty()) showConfirmDelete = true else Toast.makeText(context, "No hay ventas para limpiar", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Filled.Delete, contentDescription = "Limpiar", modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Limpiar", fontSize = 12.sp) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Buscar...") }, leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") }, modifier = Modifier.fillMaxWidth().height(50.dp), singleLine = true, shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(12.dp))

                if (processedItems.isEmpty()) {
                    Text("No se encontraron ventas registradas.", color = Color.Gray, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(processedItems) { item ->
                            if (item is Transaction) {
                                val sale = item
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text(formatDate(sale.timestamp), fontSize = 11.sp, color = Color.Gray)
                                            Text(formatCOP(sale.amount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(sale.note, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                                        if (sale.profit > 0) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) { Text("Ganancia: ${formatCOP(sale.profit)}", fontSize = 11.sp, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                }
                            } else {
                                @Suppress("UNCHECKED_CAST")
                                val group = item as Pair<String, List<Transaction>>
                                val name = group.first
                                val txs = group.second
                                var expanded by remember { mutableStateOf(false) }
                                val activeFiador = activeFiadores.find { it.name == name && it.isStore }
                                val totalAbonado = txs.sumOf { it.amount }
                                val groupGanancia = txs.sumOf { it.profit }
                                val latestDate = txs.firstOrNull()?.timestamp ?: 0L

                                val isPaidComplete = activeFiador == null
                                val badgeColor = if (isPaidComplete) Color(0xFF2196F3) else Color.Red
                                val statusText = if (isPaidComplete) "Pago completo ✅" else "Falta por pagar: ${formatCOP(activeFiador!!.amount - activeFiador.paidAmount)}"

                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text(formatDate(latestDate), fontSize = 11.sp, color = Color.Gray)
                                            Text(formatCOP(totalAbonado), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Abonos de $name", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                Text(statusText, fontSize = 13.sp, color = badgeColor, fontWeight = FontWeight.Bold)
                                            }
                                            // NUEVO BOTÓN: Solo aparece si fue marcado como pagado/completado
                                            if (isPaidComplete) {
                                                IconButton(onClick = {
                                                    fiadorToRestoreName = name
                                                    fiadorToRestoreAbonado = totalAbonado
                                                }) {
                                                    Icon(Icons.Filled.ErrorOutline, contentDescription = "Retomar deuda", tint = Color(0xFFFBC02D))
                                                }
                                            }
                                            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Color.Gray)
                                        }

                                        if (groupGanancia > 0) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                color = Color.Transparent,
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2196F3))
                                            ) {
                                                Text("Ganancia Obtenida: ${formatCOP(groupGanancia)}", fontSize = 12.sp, color = Color(0xFF2196F3), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        AnimatedVisibility(visible = expanded) {
                                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                                Divider(color = Color.Gray.copy(alpha = 0.2f))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Detalle de movimientos:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                                txs.forEach { tx ->
                                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                            Text(formatDate(tx.timestamp), fontSize = 10.sp, color = Color.Gray)
                                                            Text(tx.description, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                            if (tx.note.isNotBlank()) Text(tx.note, fontSize = 12.sp)
                                                        }
                                                        Text(formatCOP(tx.amount), fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
        confirmButton = {}
    )
}

@Composable
fun ScheduledRemindersDialog(reminders: List<Reminder>, onDismiss: () -> Unit, onDelete: (Reminder) -> Unit, onEdit: (Reminder) -> Unit, onCreateNew: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("A quien le debo 📋", fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface, text = { if (reminders.isEmpty()) { Text("No tienes deudas activas registradas.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp)) } else { LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) { itemsIndexed(reminders) { index, reminder -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(text = "${index + 1}.", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.width(28.dp)); Column(modifier = Modifier.weight(1f)) { Text(reminder.title, fontWeight = FontWeight.Bold, fontSize = 16.sp); if (reminder.amount > 0) { Text("Deuda: ${formatCOP(reminder.amount)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary) }; Text(formatDate(reminder.targetDateInMillis), fontSize = 12.sp, color = Color.Gray) }; IconButton(onClick = { onEdit(reminder) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color.Blue.copy(alpha = 0.7f)) }; IconButton(onClick = { onDelete(reminder) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.7f)) } }; if (index < reminders.size - 1) { Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp) } } } } }, confirmButton = { Button(onClick = onCreateNew) { Text("Crear") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}

@Composable
fun ScheduledFiadoresDialog(fiadores: List<Fiador>, onDismiss: () -> Unit, onDelete: (Fiador) -> Unit, onEdit: (Fiador) -> Unit, onCreateNew: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Quien me debe 🤝", fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface, text = { if (fiadores.isEmpty()) { Text("No tienes personas que te deban dinero.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp)) } else { LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) { itemsIndexed(fiadores) { index, fiador -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(text = "${index + 1}.", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.width(28.dp)); Column(modifier = Modifier.weight(1f)) { val phoneStr = if(fiador.phone.isNotBlank()) " 📞 ${fiador.phone}" else ""; val remaining = fiador.amount - fiador.paidAmount; Text(fiador.name + phoneStr, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("Resta: ${formatCOP(remaining)} (Total: ${formatCOP(fiador.amount)}) - ${fiador.reason}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary); Text(formatDate(fiador.targetDateInMillis), fontSize = 12.sp, color = Color.Gray) }; IconButton(onClick = { onEdit(fiador) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color.Blue.copy(alpha = 0.7f)) }; IconButton(onClick = { onDelete(fiador) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.7f)) } }; if (index < fiadores.size - 1) { Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp) } } } } }, confirmButton = { Button(onClick = onCreateNew) { Text("Agregar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesDialog(categories: List<String>, onDismiss: () -> Unit, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    var newCat by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Categorías 🏷️", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Cerrar") }
            }
        },
        text = {
            Column {
                Text("Gestiona las categorías de tus gastos:", color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newCat, onValueChange = { newCat = it }, label = { Text("Nueva Categoría") }, modifier = Modifier.weight(1f), singleLine = true)
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (newCat.isNotBlank()) {
                            onAdd(newCat.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                            newCat = ""
                        }
                    }) { Text("Añadir") }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (categories.isEmpty()) {
                    Text("Aún no tienes categorías creadas.", color = Color.Gray, fontSize = 14.sp)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                        items(categories) { cat ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cat, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { onRemove(cat) }) { Icon(Icons.Filled.Delete, "Eliminar", tint = Color.Red) }
                            }
                            Divider(color = Color.Gray.copy(alpha=0.2f))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseBreakdownDialog(title: String, expenses: List<Transaction>, onDismiss: () -> Unit, onDelete: (Transaction) -> Unit, onImageClick: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredExpenses = remember(expenses, searchQuery) {
        val query = searchQuery.lowercase(Locale.getDefault())
        expenses.filter {
            it.description.lowercase(Locale.getDefault()).contains(query) ||
                    it.note.lowercase(Locale.getDefault()).contains(query) ||
                    (it.category?.lowercase(Locale.getDefault()) ?: "").contains(query) ||
                    it.amount.toLong().toString().contains(query)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Quita el límite estricto
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f), // Obliga a la ventana a ocupar el 95% de la pantalla
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Close, "Cerrar") }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por título, categoría, monto...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (filteredExpenses.isEmpty()) {
                    Text("No se encontraron gastos con esa búsqueda.", color = Color.Gray, modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                        items(filteredExpenses, key = { it.id }) { tx ->
                            TransactionItem(
                                transaction = tx,
                                onDelete = { onDelete(tx) },
                                onImageClick = onImageClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoSyncSetupDialog(initialFrequency: Int, initialHour: Int, initialMinute: Int, onDismiss: () -> Unit, onSave: (Int, Int, Int) -> Unit) {
    var isEnabled by remember { mutableStateOf(initialFrequency > 0) }
    val currentHourInt = if (initialHour == 0) 12 else if (initialHour > 12) initialHour - 12 else initialHour
    var customHour by remember { mutableStateOf(currentHourInt.toString()) }
    var customMinute by remember { mutableStateOf(initialMinute.toString().padStart(2, '0')) }
    var isPm by remember { mutableStateOf(initialHour >= 12) }
    val minuteFocusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        properties = DialogProperties(usePlatformDefaultWidth = false), // <-- EXPANDE EL MODAL
        modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp),
        title = { Text("Auto-Sincronización ⏰", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Configura una hora para guardar tu información en la nube automáticamente todos los días (Requiere Internet).", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isEnabled = !isEnabled }.padding(8.dp)) {
                    Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Activar copia de seguridad diaria", fontWeight = FontWeight.Bold)
                }

                AnimatedVisibility(visible = isEnabled) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("¿A qué hora se hará la copia?", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(value = customHour, onValueChange = { input -> if (input.isEmpty()) { customHour = input } else if (input.length <= 2 && input.all { char -> char.isDigit() }) { val h = input.toIntOrNull(); if (h != null) { if (input.length == 1 && h == 0) { customHour = input } else if (h in 1..12) { customHour = input; if (input.length == 2) minuteFocusRequester.requestFocus() } } } }, placeholder = { Text("12", color = Color.Gray.copy(alpha=0.4f), fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, modifier = Modifier.width(80.dp), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 28.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                            Text(" : ", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                            OutlinedTextField(value = customMinute, onValueChange = { input -> if (input.isEmpty()) { customMinute = input } else if (input.length <= 2 && input.all { it.isDigit() }) { val m = input.toIntOrNull(); if (m != null && m in 0..59) { customMinute = input } } }, placeholder = { Text("00", color = Color.Gray.copy(alpha=0.4f), fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, modifier = Modifier.width(80.dp).focusRequester(minuteFocusRequester), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 28.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                FilterChip(selected = !isPm, onClick = { isPm = false }, label = { Text("AM") })
                                FilterChip(selected = isPm, onClick = { isPm = true }, label = { Text("PM") })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (!isEnabled) {
                    onSave(0, 0, 0)
                } else {
                    val finalH = customHour.toIntOrNull() ?: currentHourInt
                    val finalM = customMinute.toIntOrNull() ?: initialMinute
                    var hour24 = finalH
                    if (isPm && hour24 < 12) hour24 += 12
                    if (!isPm && hour24 == 12) hour24 = 0
                    onSave(1, hour24, finalM)
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}