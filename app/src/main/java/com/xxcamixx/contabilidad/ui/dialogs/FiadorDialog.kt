package com.xxcamixx.contabilidad.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xxcamixx.contabilidad.model.Fiador
import com.xxcamixx.contabilidad.model.Product
import com.xxcamixx.contabilidad.util.AmountVisualTransformation
import com.xxcamixx.contabilidad.util.cleanAmountInput
import com.xxcamixx.contabilidad.util.formatCOP
import com.xxcamixx.contabilidad.util.formatMoneyMain
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiadorDialog(
    initialFiador: Fiador? = null,
    initialName: String = "",
    initialCart: List<Pair<Product, Int>> = emptyList(),
    initialCash: Double = 0.0,
    initialDigital: Double = 0.0,
    products: List<Product>,
    selectedCountry: String, // NUEVO
    bcvRate: Double,         // NUEVO
    preselectedDate: Long? = null,
    isStore: Boolean,
    onDismiss: () -> Unit,
    onConfirmNew: (String, String, List<Pair<Product, Int>>, Double, Long, Double, Double) -> Unit,
    onConfirmEdit: (Fiador, Long) -> Unit,
    onConfirmAbono: (Fiador, Double, String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialFiador?.name ?: initialName) }
    var phone by remember { mutableStateOf(initialFiador?.phone ?: "") }
    var tempDateMillis by remember { mutableStateOf<Long?>(initialFiador?.targetDateInMillis ?: preselectedDate) }
    var activeScreen by remember { mutableStateOf(if (initialFiador == null && preselectedDate == null) "NEW_INFO" else if (initialFiador == null && preselectedDate != null) "NEW_TIME" else "EDIT_OPTIONS") }
    var isEditDateOnly by remember { mutableStateOf(false) }

    val cartItems = remember { mutableStateListOf<Pair<Product, Int>>().apply { if (initialFiador == null) addAll(initialCart) } }

    var personalDebtAmountRaw by remember { mutableStateOf("") }

    var expandedProduct by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var qtyRaw by remember { mutableStateOf("1") }
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = initialFiador?.targetDateInMillis ?: preselectedDate ?: System.currentTimeMillis() } }

    val initialPaidAmount = initialCash + initialDigital
    val initialMethod = when {
        initialCash > 0 && initialDigital == 0.0 -> "Efectivo"
        initialDigital > 0 && initialCash == 0.0 -> "Digital"
        else -> "Múltiple"
    }

    var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        CustomDatePickerDialog(
            initialDateMillis = tempDateMillis ?: System.currentTimeMillis(),
            onDismiss = { showDatePicker = false },
            onDateSelected = { selected ->
                val cal = Calendar.getInstance().apply { timeInMillis = tempDateMillis ?: System.currentTimeMillis() }
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val minute = cal.get(Calendar.MINUTE)
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = selected
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                tempDateMillis = newCal.timeInMillis
                showDatePicker = false
                if (isEditDateOnly) {
                    onConfirmEdit(initialFiador!!.copy(name = name, phone = phone), tempDateMillis!!)
                } else {
                    activeScreen = "NEW_TIME"
                }
            }
        )
    }

    if (activeScreen == "EDIT_OPTIONS") {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("¿Qué deseas hacer? ✏️", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { activeScreen = "ABONO" }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("\uD83D\uDCB0 Registrar Abono") }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { activeScreen = "EDIT_INFO" }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("\uD83D\uDCDD Información del Fiador") }
                    Button(onClick = { isEditDateOnly = true; showDatePicker = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("\uD83D\uDCC5 Fecha de Cobro") }
                    Button(onClick = { activeScreen = "EDIT_TIME" }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("⏰ Hora de Cobro") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
        )
    }

    if (activeScreen == "ABONO" && initialFiador != null) {
        var abonoRaw by remember { mutableStateOf("") }
        var abonoMethod by remember { mutableStateOf("Efectivo") }
        val remaining = initialFiador.amount - initialFiador.paidAmount

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Abonar a Deuda \uD83D\uDCB0", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Deuda Total: ${formatCOP(initialFiador.amount)}", fontWeight = FontWeight.Bold)
                    Text("Abonado hasta ahora: ${formatCOP(initialFiador.paidAmount)}", color = Color(0xFF2196F3))
                    Text("Resta por pagar: ${formatCOP(remaining)}", color = Color.Red, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (initialFiador.paymentHistory.isNotEmpty()) {
                        Text("Historial de Abonos:", fontSize = 12.sp, color = Color.Gray)
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))) {
                            Text(initialFiador.paymentHistory, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    OutlinedTextField(
                        value = abonoRaw,
                        onValueChange = { abonoRaw = cleanAmountInput(it) },
                        label = { Text("Monto a abonar") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = AmountVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Text("$", color = Color.Gray, modifier = Modifier.padding(start=8.dp)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        FilterChip(selected = abonoMethod == "Efectivo", onClick = { abonoMethod = "Efectivo" }, label = { Text("Efectivo") })
                        FilterChip(selected = abonoMethod == "Digital", onClick = { abonoMethod = "Digital" }, label = { Text("Digital") })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val abono = abonoRaw.toDoubleOrNull() ?: 0.0
                    if (abono > 0 && abono <= remaining) {
                        onConfirmAbono(initialFiador, abono, abonoMethod)
                    } else if (abono > remaining) {
                        Toast.makeText(context, "El abono supera la deuda restante", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Confirmar Abono") }
            },
            dismissButton = { TextButton(onClick = { activeScreen = "EDIT_OPTIONS" }) { Text("Atrás") } }
        )
    }

    if (activeScreen == "NEW_INFO" || activeScreen == "EDIT_INFO") {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (activeScreen == "EDIT_INFO") "Editar Deudor ✏️" else "Nuevo Deudor \uD83E\uDD1D", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = name, onValueChange = { input -> name = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }, label = { Text("Nombre de la persona") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono (Opcional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))

                    if (initialFiador == null) {
                        if (isStore) {
                            if (initialCart.isEmpty()) {
                                Text("Productos a fiar:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Box {
                                    Button(onClick = { expandedProduct = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors()) { Text(selectedProduct?.name ?: "🔽 Seleccionar Producto", color = MaterialTheme.colorScheme.onSurface) }
                                    DropdownMenu(expanded = expandedProduct, onDismissRequest = { expandedProduct = false }) {
                                        products.filter { it.stock > 0 }.forEach { p ->
                                            DropdownMenuItem(text = { Text("${p.name} (Disp: ${p.stock} ${p.unit}) - ${formatCOP(p.price)}") }, onClick = { selectedProduct = p; expandedProduct = false })
                                        }
                                        if(products.none { it.stock > 0 }) {
                                            DropdownMenuItem(text = { Text("No hay productos en stock") }, onClick = { expandedProduct = false })
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                    OutlinedTextField(value = qtyRaw, onValueChange = { n -> val d = n.filter { it.isDigit() }; qtyRaw = d }, label = { Text("Cant.") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = {
                                        val q = qtyRaw.toIntOrNull() ?: 0
                                        if (selectedProduct != null && q > 0 && q <= selectedProduct!!.stock) {
                                            val existing = cartItems.find { it.first.id == selectedProduct!!.id }
                                            if (existing != null) {
                                                val newQ = existing.second + q; if (newQ <= selectedProduct!!.stock) { val idx = cartItems.indexOf(existing); cartItems[idx] = existing.copy(second = newQ) } else { Toast.makeText(context, "Supera el stock disponible", Toast.LENGTH_SHORT).show() }
                                            } else {
                                                cartItems.add(Pair(selectedProduct!!, q))
                                            }
                                            qtyRaw = "1"; selectedProduct = null
                                        }
                                    }) { Text("Añadir") }
                                }
                            }

                            if (cartItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(if (initialCart.isEmpty()) 16.dp else 0.dp))
                                Text(if (initialCart.isEmpty()) "Lista de deuda:" else "Resumen de la deuda:", fontSize = 12.sp, color = Color.Gray)
                                cartItems.forEachIndexed { index, item ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("• ${item.second}${item.first.unit} ${item.first.name}", fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        // MODIFICADO: Agregada la función formatMoneyMain
                                        Text(formatMoneyMain(item.first.price * item.second, selectedCountry), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        if (initialCart.isEmpty()) {
                                            IconButton(onClick = { cartItems.removeAt(index) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, null, tint = Color.Red, modifier = Modifier.size(16.dp)) }
                                        }
                                    }
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                val total = cartItems.sumOf { it.first.price * it.second }
                                Text("Total Deuda: ${formatCOP(total)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                                if (initialPaidAmount > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Abono inicial: ${formatCOP(initialPaidAmount)} ($initialMethod)", fontSize = 14.sp, color = Color(0xFF2196F3))
                                    Text("Resta por pagar: ${formatCOP(total - initialPaidAmount)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Red)
                                }
                            }
                        } else {
                            Text("Monto de la deuda:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = personalDebtAmountRaw,
                                onValueChange = { personalDebtAmountRaw = cleanAmountInput(it) },
                                label = { Text("¿Cuánto dinero le prestaste?") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = AmountVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Text("$", color = Color.Gray, modifier = Modifier.padding(start=8.dp)) }
                            )

                            if (initialPaidAmount > 0) {
                                val totalPersonal = personalDebtAmountRaw.toDoubleOrNull() ?: 0.0
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Abono inicial: ${formatCOP(initialPaidAmount)} ($initialMethod)", fontSize = 14.sp, color = Color(0xFF2196F3))
                                Text("Resta por pagar: ${formatCOP(totalPersonal - initialPaidAmount)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Red)
                            }
                        }
                    } else {
                        Text("Detalle original:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(initialFiador.reason, fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Monto Deuda Total: ${formatCOP(initialFiador.amount)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Abonado: ${formatCOP(initialFiador.paidAmount)}", fontSize = 14.sp, color = Color(0xFF2196F3))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        val capName = name.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        val pAmount = personalDebtAmountRaw.toDoubleOrNull() ?: 0.0

                        if (initialFiador == null && isStore && cartItems.isEmpty()) {
                            Toast.makeText(context, "Agrega productos a la deuda", Toast.LENGTH_SHORT).show()
                        } else if (initialFiador == null && !isStore && pAmount <= 0) {
                            Toast.makeText(context, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                        } else {
                            if (activeScreen == "EDIT_INFO") {
                                onConfirmEdit(initialFiador!!.copy(name = capName, phone = phone), tempDateMillis!!)
                            } else {
                                name = capName; showDatePicker = true
                            }
                        }
                    } else {
                        Toast.makeText(context, "Escribe el nombre del deudor", Toast.LENGTH_SHORT).show()
                    }
                }) { Text(if (activeScreen == "EDIT_INFO") "Guardar" else "Siguiente") }
            },
            dismissButton = { TextButton(onClick = { if (activeScreen == "EDIT_INFO") activeScreen = "EDIT_OPTIONS" else onDismiss() }) { Text(if (activeScreen == "EDIT_INFO") "Atrás" else "Cancelar") } }
        )
    }

    if (activeScreen == "NEW_TIME" || activeScreen == "EDIT_TIME") {
        val currentHourInt = calendar.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val currentMinInt = calendar.get(Calendar.MINUTE)
        val currentHourStr = currentHourInt.toString()
        val currentMinStr = currentMinInt.toString().padStart(2, '0')
        var customHour by remember { mutableStateOf(if(initialFiador != null) currentHourInt.toString() else "") }
        var customMinute by remember { mutableStateOf(if(initialFiador != null) currentMinStr else "") }
        var isPm by remember { mutableStateOf(calendar.get(Calendar.AM_PM) == Calendar.PM) }
        val minuteFocusRequester = remember { FocusRequester() }

        AlertDialog(
            onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, title = { Text("Hora de la Alerta ⏰", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    val phoneTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    Text("Hora actual del teléfono: $phoneTime", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp).alpha(0.7f))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = customHour, onValueChange = { input -> if (input.isEmpty()) { customHour = input } else if (input.length <= 2 && input.all { char -> char.isDigit() }) { val h = input.toIntOrNull(); if (h != null) { if (input.length == 1 && h == 0) { customHour = input } else if (h in 1..12) { customHour = input; if (input.length == 2) minuteFocusRequester.requestFocus() } } } }, placeholder = { Text(currentHourStr, color = Color.Gray.copy(alpha=0.4f), fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, modifier = Modifier.width(80.dp), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 28.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true); Text(" : ", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp)); OutlinedTextField(value = customMinute, onValueChange = { input -> if (input.isEmpty()) { customMinute = input } else if (input.length <= 2 && input.all { it.isDigit() }) { val m = input.toIntOrNull(); if (m != null && m in 0..59) { customMinute = input } } }, placeholder = { Text(currentMinStr, color = Color.Gray.copy(alpha=0.4f), fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, modifier = Modifier.width(80.dp).focusRequester(minuteFocusRequester), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 28.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true); Spacer(modifier = Modifier.width(8.dp)); Column { FilterChip(selected = !isPm, onClick = { isPm = false }, label = { Text("AM") }); FilterChip(selected = isPm, onClick = { isPm = true }, label = { Text("PM") }) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val finalH = customHour.toIntOrNull() ?: currentHourInt
                    val finalM = customMinute.toIntOrNull() ?: currentMinInt
                    var hour24 = finalH
                    if (isPm && hour24 < 12) hour24 += 12
                    if (!isPm && hour24 == 12) hour24 = 0

                    if (tempDateMillis != null) {
                        val baseCal = Calendar.getInstance().apply { timeInMillis = tempDateMillis!! }
                        val localCal = Calendar.getInstance().apply { set(baseCal.get(Calendar.YEAR), baseCal.get(Calendar.MONTH), baseCal.get(Calendar.DAY_OF_MONTH), hour24, finalM, 0) }

                        if (initialFiador != null) {
                            onConfirmEdit(initialFiador.copy(name = name, phone = phone), localCal.timeInMillis)
                        } else {
                            val pAmount = personalDebtAmountRaw.toDoubleOrNull() ?: 0.0
                            onConfirmNew(name, phone, cartItems.toList(), pAmount, localCal.timeInMillis, initialCash, initialDigital)
                        }
                    }
                }) { Text(if (activeScreen == "EDIT_TIME") "Guardar" else "Aceptar") }
            },
            dismissButton = { TextButton(onClick = { if (activeScreen == "NEW_TIME") activeScreen = "NEW_INFO" else activeScreen = "EDIT_OPTIONS" }) { Text("Atrás") } }
        )
    }
}
