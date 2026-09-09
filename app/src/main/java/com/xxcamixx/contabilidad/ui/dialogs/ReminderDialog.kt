package com.xxcamixx.contabilidad.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.xxcamixx.contabilidad.model.Reminder
import com.xxcamixx.contabilidad.util.AmountVisualTransformation
import com.xxcamixx.contabilidad.util.cleanAmountInput
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(initialReminder: Reminder? = null, preselectedDate: Long? = null, onDismiss: () -> Unit, onConfirm: (String, Double, Long) -> Unit) {
    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var amountRaw by remember { mutableStateOf(if(initialReminder != null && initialReminder.amount > 0) initialReminder.amount.toLong().toString() else "") }
    var tempDateMillis by remember { mutableStateOf<Long?>(initialReminder?.targetDateInMillis ?: preselectedDate) }
    var activeScreen by remember { mutableStateOf(if (initialReminder == null && preselectedDate == null) "NEW_INFO" else if (initialReminder == null && preselectedDate != null) "NEW_TIME" else "EDIT_OPTIONS") }
    var isEditDateOnly by remember { mutableStateOf(false) }
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = initialReminder?.targetDateInMillis ?: preselectedDate ?: System.currentTimeMillis() } }
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showDatePicker) {
        CustomDatePickerDialog(initialDateMillis = tempDateMillis ?: System.currentTimeMillis(), onDismiss = { showDatePicker = false }, onDateSelected = { selected -> val cal = Calendar.getInstance().apply { timeInMillis = tempDateMillis ?: System.currentTimeMillis() }; val hour = cal.get(Calendar.HOUR_OF_DAY); val minute = cal.get(Calendar.MINUTE); val newCal = Calendar.getInstance().apply { timeInMillis = selected; set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0) }; tempDateMillis = newCal.timeInMillis; showDatePicker = false; if (isEditDateOnly) { onConfirm(title, amountRaw.toDoubleOrNull() ?: 0.0, tempDateMillis!!) } else { activeScreen = "NEW_TIME" } })
    }

    if (activeScreen == "EDIT_OPTIONS") { AlertDialog(onDismissRequest = onDismiss, title = { Text("¿Qué deseas editar? ✏️", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, containerColor = MaterialTheme.colorScheme.surface, text = { Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Button(onClick = { activeScreen = "EDIT_INFO" }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("\uD83D\uDCDD Información de la Deuda") }; Button(onClick = { isEditDateOnly = true; showDatePicker = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("\uD83D\uDCC5 Fecha de Cobro") }; Button(onClick = { activeScreen = "EDIT_TIME" }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("⏰ Hora de Cobro") } } }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }) }

    if (activeScreen == "NEW_INFO" || activeScreen == "EDIT_INFO") {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (activeScreen == "EDIT_INFO") "Editar Deuda ✏️" else "Nueva Deuda \uD83D\uDCC5", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { input -> title = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } },
                        label = { Text("¿Qué debes pagar? (ej. Luz)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = amountRaw,
                        onValueChange = { amountRaw = cleanAmountInput(it) },
                        label = { Text("Monto de la deuda") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = AmountVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Text("$", color = Color.Gray, modifier = Modifier.padding(start=8.dp)) }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsedAmount = amountRaw.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && parsedAmount > 0) {
                        val capTitle = title.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        if (activeScreen == "EDIT_INFO") {
                            onConfirm(capTitle, parsedAmount, tempDateMillis!!)
                        } else {
                            title = capTitle; showDatePicker = true
                        }
                    } else {
                        Toast.makeText(context, "Ingresa un nombre y monto válido", Toast.LENGTH_SHORT).show()
                    }
                }) { Text(if (activeScreen == "EDIT_INFO") "Guardar" else "Siguiente") }
            },
            dismissButton = { TextButton(onClick = { if (activeScreen == "EDIT_INFO") activeScreen = "EDIT_OPTIONS" else onDismiss() }) { Text(if (activeScreen == "EDIT_INFO") "Atrás" else "Cancelar") } }
        )
    }

    if (activeScreen == "NEW_TIME" || activeScreen == "EDIT_TIME") {
        val currentHourInt = calendar.get(Calendar.HOUR).let { if (it == 0) 12 else it }; val currentMinInt = calendar.get(Calendar.MINUTE); val currentHourStr = currentHourInt.toString(); val currentMinStr = currentMinInt.toString().padStart(2, '0'); var customHour by remember { mutableStateOf(if(initialReminder != null) currentHourInt.toString() else "") }; var customMinute by remember { mutableStateOf(if(initialReminder != null) currentMinStr else "") }; var isPm by remember { mutableStateOf(calendar.get(Calendar.AM_PM) == Calendar.PM) }; val minuteFocusRequester = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, title = { Text("Ingresar Hora ⏰", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { val phoneTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()); Text("Hora actual del teléfono: $phoneTime", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp).alpha(0.7f)); Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) { OutlinedTextField(value = customHour, onValueChange = { input -> if (input.isEmpty()) { customHour = input } else if (input.length <= 2 && input.all { char -> char.isDigit() }) { val h = input.toIntOrNull(); if (h != null) { if (input.length == 1 && h == 0) { customHour = input } else if (h in 1..12) { customHour = input; if (input.length == 2) minuteFocusRequester.requestFocus() } } } }, placeholder = { Text(currentHourStr, color = Color.Gray.copy(alpha=0.4f), fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, modifier = Modifier.width(80.dp), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 28.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true); Text(" : ", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp)); OutlinedTextField(value = customMinute, onValueChange = { input -> if (input.isEmpty()) { customMinute = input } else if (input.length <= 2 && input.all { it.isDigit() }) { val m = input.toIntOrNull(); if (m != null && m in 0..59) { customMinute = input } } }, placeholder = { Text(currentMinStr, color = Color.Gray.copy(alpha=0.4f), fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, modifier = Modifier.width(80.dp).focusRequester(minuteFocusRequester), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 28.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true); Spacer(modifier = Modifier.width(8.dp)); Column { FilterChip(selected = !isPm, onClick = { isPm = false }, label = { Text("AM") }); FilterChip(selected = isPm, onClick = { isPm = true }, label = { Text("PM") }) } } } },
            confirmButton = { TextButton(onClick = { val finalH = customHour.toIntOrNull() ?: currentHourInt; val finalM = customMinute.toIntOrNull() ?: currentMinInt; var hour24 = finalH; if (isPm && hour24 < 12) hour24 += 12; if (!isPm && hour24 == 12) hour24 = 0; if (tempDateMillis != null) { val baseCal = Calendar.getInstance().apply { timeInMillis = tempDateMillis!! }; val localCal = Calendar.getInstance().apply { set(baseCal.get(Calendar.YEAR), baseCal.get(Calendar.MONTH), baseCal.get(Calendar.DAY_OF_MONTH), hour24, finalM, 0) }; onConfirm(title, amountRaw.toDoubleOrNull() ?: 0.0, localCal.timeInMillis) } }) { Text(if (activeScreen == "EDIT_TIME") "Guardar" else "Aceptar") } },
            dismissButton = { TextButton(onClick = { if (activeScreen == "NEW_TIME") activeScreen = "NEW_INFO" else activeScreen = "EDIT_OPTIONS" }) { Text("Atrás") } }
        )
    }
}
