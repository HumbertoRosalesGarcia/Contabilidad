package com.xxcamixx.contabilidad.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.xxcamixx.contabilidad.model.Fiador
import com.xxcamixx.contabilidad.model.Product
import com.xxcamixx.contabilidad.model.Reminder
import com.xxcamixx.contabilidad.util.isSameDay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarDialog(currentTab: Int, reminders: List<Reminder>, fiadores: List<Fiador>, products: List<Product>, onDismiss: () -> Unit, onDayClick: (Long, Boolean) -> Unit, onViewReminders: () -> Unit, onViewFiadores: () -> Unit) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    val formatMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val titleText = when (currentTab) {
        0 -> "Agenda Personal 🗓️"
        1 -> "Agenda de Pedidos 📦 🗓️"
        else -> "Agenda de Tienda 🏪 🗓️"
    }

    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false), modifier = Modifier.fillMaxWidth().padding(16.dp), containerColor = MaterialTheme.colorScheme.surface,
        title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(titleText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { val newCal = currentMonth.clone() as Calendar; newCal.add(Calendar.MONTH, -1); newCal.set(Calendar.DAY_OF_MONTH, 1); currentMonth = newCal }) { Icon(Icons.Filled.ChevronLeft, "Anterior") }; Text(text = formatMonth.format(currentMonth.time).replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, fontSize = 16.sp); IconButton(onClick = { val newCal = currentMonth.clone() as Calendar; newCal.add(Calendar.MONTH, 1); newCal.set(Calendar.DAY_OF_MONTH, 1); currentMonth = newCal }) { Icon(Icons.Filled.ChevronRight, "Siguiente") } }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) { listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb").forEach { Text(text = it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) } }
                Spacer(modifier = Modifier.height(8.dp))

                val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH); val tempCal = currentMonth.clone() as Calendar; tempCal.set(Calendar.DAY_OF_MONTH, 1); val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1; val totalCells = daysInMonth + firstDayOfWeek; val rows = (totalCells + 6) / 7
                Column(modifier = Modifier.fillMaxWidth()) {
                    for (i in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (j in 0..6) {
                                val cellIndex = i * 7 + j; val dayNumber = cellIndex - firstDayOfWeek + 1
                                if (dayNumber in 1..daysInMonth) {
                                    val dayCal = currentMonth.clone() as Calendar; dayCal.set(Calendar.DAY_OF_MONTH, dayNumber)
                                    val hasReminder = reminders.any { isSameDay(it.targetDateInMillis, dayCal.timeInMillis) }; val hasFiador = fiadores.any { isSameDay(it.targetDateInMillis, dayCal.timeInMillis) }; val hasProduct = if (currentTab == 2) products.any { it.expirationDateInMillis != null && isSameDay(it.expirationDateInMillis, dayCal.timeInMillis) } else false; val hasEvents = hasReminder || hasFiador || hasProduct
                                    val bgColor = when { hasProduct -> Color(0xFFD32F2F); hasReminder -> Color(0xFF1976D2); hasFiador -> Color(0xFFFBC02D); else -> Color.Transparent }; val textColor = if (bgColor == Color.Transparent) MaterialTheme.colorScheme.onSurface else Color.White
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(CircleShape).background(bgColor).clickable { onDayClick(dayCal.timeInMillis, hasEvents) }, contentAlignment = Alignment.Center) { Text(text = dayNumber.toString(), fontSize = 14.sp, fontWeight = if (hasEvents) FontWeight.Bold else FontWeight.Normal, color = textColor) }
                                } else { Box(modifier = Modifier.weight(1f).aspectRatio(1f)) }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp)); Divider(color = Color.Gray.copy(alpha = 0.2f))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onViewReminders, modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2), contentColor = Color.White)) { Text("\uD83D\uDCB8 Mis Deudas", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center) }
                    Button(onClick = onViewFiadores, modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D), contentColor = Color.Black)) { Text("\uD83D\uDCCB Mis Deudores", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center) }
                }
            }
        },
        confirmButton = {}, dismissButton = { }
    )
}
