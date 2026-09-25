package com.xxcamixx.contabilidad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xxcamixx.contabilidad.model.CierreSession
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.geometry.Rect
import com.xxcamixx.contabilidad.ui.components.tour.CoachMarkOverlay
import com.xxcamixx.contabilidad.ui.components.tour.TourCatalog
import com.xxcamixx.contabilidad.ui.components.tour.TourManager
import com.xxcamixx.contabilidad.ui.components.tour.TourTarget
import com.xxcamixx.contabilidad.ui.components.tour.TourZone
import kotlinx.coroutines.delay

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

    val fiadores by viewModel.fiadores.collectAsState(initial = emptyList())
    var currentTab by remember { mutableStateOf(0) } // 0 = Cierres, 1 = Deudas Pendientes

    var selectedCierre by remember { mutableStateOf<CierreSession?>(null) }

    val context = LocalContext.current
    var isAutoCierreExpanded by remember { mutableStateOf(false) }
    var isAutoCierreEnabled by remember(viewModel.autoCierreEnabled) { mutableStateOf(viewModel.autoCierreEnabled) }
    var selectedFrequency by remember(viewModel.autoCierreFrequency) { mutableStateOf(viewModel.autoCierreFrequency) }
    var selectedAutoCierreMode by remember(viewModel.autoCierreMode) { mutableStateOf(viewModel.autoCierreMode) }

    val initialHour24 = viewModel.autoCierreHour
    val initialMinute = viewModel.autoCierreMinute
    var selectedHour12 by remember(initialHour24) {
        mutableStateOf(if (initialHour24 == 0) 12 else if (initialHour24 > 12) initialHour24 - 12 else initialHour24)
    }
    var isPm by remember(initialHour24) { mutableStateOf(initialHour24 >= 12) }
    var selectedMinute by remember(initialMinute) { mutableStateOf(initialMinute) }

    val cierresTourSteps = remember { TourCatalog.getStepsForZone(TourZone.CIERRES) }
    var currentCierresTourStepIndex by remember { mutableStateOf(0) }
    var isCierresTourActive by remember { mutableStateOf(false) }
    val cierresTourTargetsBounds = remember { mutableStateMapOf<TourTarget, Rect>() }

    LaunchedEffect(Unit) {
        delay(600L)
        if (viewModel.userId.isNotEmpty() && !TourManager.isZoneSeen(context, viewModel.userId, TourZone.CIERRES)) {
            currentCierresTourStepIndex = 0
            isCierresTourActive = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, Color(0xFF2C2C32))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                        val arrowRotation by animateFloatAsState(
                            targetValue = if (isAutoCierreExpanded) 180f else 0f,
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                            label = "arrowRotation"
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                        ) {
                            item {
                                // Sección Desplegable de Cierre Automático
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .onGloballyPositioned { cierresTourTargetsBounds[TourTarget.CIERRES_AUTO_SCHEDULE] = it.boundsInWindow() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                                    border = BorderStroke(1.dp, Color(0xFF33333A))
                                ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    // Cabecera Desplegable
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isAutoCierreExpanded = !isAutoCierreExpanded },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = "Cierre Automático",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Cierre Automático",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            val statusText = if (viewModel.autoCierreEnabled) {
                                                val h12 = if (viewModel.autoCierreHour == 0) 12 else if (viewModel.autoCierreHour > 12) viewModel.autoCierreHour - 12 else viewModel.autoCierreHour
                                                val amPm = if (viewModel.autoCierreHour >= 12) "PM" else "AM"
                                                val freqStr = when (viewModel.autoCierreFrequency) {
                                                    "SEMANAL" -> "Semanal"
                                                    "MENSUAL" -> "Mensual"
                                                    else -> "Diario"
                                                }
                                                val modeStr = when (viewModel.autoCierreMode) {
                                                    "PERSONAL" -> "Personal"
                                                    "PEDIDOS" -> "Pedidos"
                                                    "TIENDA" -> "Tienda"
                                                    else -> "Todos"
                                                }
                                                "● Activo: $freqStr ${String.format("%02d:%02d %s", h12, viewModel.autoCierreMinute, amPm)} ($modeStr)"
                                            } else {
                                                "Desactivado (Toca para configurar)"
                                            }
                                            Text(
                                                statusText,
                                                color = if (viewModel.autoCierreEnabled) MaterialTheme.colorScheme.primary else Color(0xFF9E9E9E),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isAutoCierreExpanded) "Colapsar" else "Desplegar",
                                            modifier = Modifier.rotate(arrowRotation)
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isAutoCierreExpanded,
                                        enter = expandVertically(
                                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                                        ) + fadeIn(animationSpec = tween(durationMillis = 150)),
                                        exit = shrinkVertically(
                                            animationSpec = tween(durationMillis = 160, easing = FastOutLinearInEasing)
                                        ) + fadeOut(animationSpec = tween(durationMillis = 100))
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                                            HorizontalDivider(
                                                color = Color(0xFF33333A),
                                                modifier = Modifier.padding(bottom = 10.dp)
                                            )

                                            // Switch de activación
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Activar Cierres Automáticos", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                Switch(
                                                    checked = isAutoCierreEnabled,
                                                    onCheckedChange = { isAutoCierreEnabled = it },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                                        uncheckedThumbColor = Color(0xFF888888),
                                                        uncheckedTrackColor = Color(0xFF2C2C30)
                                                    )
                                                )
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .alpha(if (isAutoCierreEnabled) 1f else 0.38f)
                                            ) {
                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Frecuencia
                                                Text("Frecuencia de Cierre:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE0E0E0))
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    val frequencies = listOf("Diario" to "DIARIO", "Semanal" to "SEMANAL", "Mensual" to "MENSUAL")
                                                    frequencies.forEach { (label, value) ->
                                                        val selected = selectedFrequency == value
                                                        FilterChip(
                                                            selected = selected,
                                                            onClick = { if (isAutoCierreEnabled) selectedFrequency = value },
                                                            label = { Text(label, fontSize = 12.sp) },
                                                            modifier = Modifier.weight(1f),
                                                            colors = FilterChipDefaults.filterChipColors(
                                                                containerColor = Color(0xFF161618),
                                                                labelColor = Color(0xFFB0B0B5),
                                                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                                                selectedLabelColor = MaterialTheme.colorScheme.primary
                                                            ),
                                                            border = FilterChipDefaults.filterChipBorder(
                                                                enabled = true,
                                                                selected = selected,
                                                                borderColor = Color(0xFF35353C),
                                                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                            )
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                // Hora de Cierre
                                                Text("Hora del Cierre:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE0E0E0))
                                                Spacer(modifier = Modifier.height(6.dp))

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    // Horas (1-12)
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        IconButton(
                                                            onClick = {
                                                                selectedHour12 = if (selectedHour12 >= 12) 1 else selectedHour12 + 1
                                                            },
                                                            enabled = isAutoCierreEnabled,
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.KeyboardArrowUp,
                                                                contentDescription = "Subir hora",
                                                                tint = if (isAutoCierreEnabled) MaterialTheme.colorScheme.primary else Color(0xFF666666)
                                                            )
                                                        }
                                                        Surface(
                                                            color = Color(0xFF141416),
                                                            shape = RoundedCornerShape(8.dp),
                                                            border = BorderStroke(1.dp, Color(0xFF3A3A42)),
                                                            modifier = Modifier.width(46.dp).height(38.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Text(
                                                                    text = String.format("%02d", selectedHour12),
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 17.sp,
                                                                    color = Color.White
                                                                )
                                                            }
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                selectedHour12 = if (selectedHour12 <= 1) 12 else selectedHour12 - 1
                                                            },
                                                            enabled = isAutoCierreEnabled,
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.KeyboardArrowDown,
                                                                contentDescription = "Bajar hora",
                                                                tint = if (isAutoCierreEnabled) MaterialTheme.colorScheme.primary else Color(0xFF666666)
                                                            )
                                                        }
                                                    }

                                                    Text(" : ", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))

                                                    // Minutos (0-59)
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        IconButton(
                                                            onClick = {
                                                                selectedMinute = (selectedMinute + 5) % 60
                                                            },
                                                            enabled = isAutoCierreEnabled,
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.KeyboardArrowUp,
                                                                contentDescription = "Subir minuto",
                                                                tint = if (isAutoCierreEnabled) MaterialTheme.colorScheme.primary else Color(0xFF666666)
                                                            )
                                                        }
                                                        Surface(
                                                            color = Color(0xFF141416),
                                                            shape = RoundedCornerShape(8.dp),
                                                            border = BorderStroke(1.dp, Color(0xFF3A3A42)),
                                                            modifier = Modifier.width(46.dp).height(38.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Text(
                                                                    text = String.format("%02d", selectedMinute),
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 17.sp,
                                                                    color = Color.White
                                                                )
                                                            }
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                selectedMinute = if (selectedMinute - 5 < 0) 55 else selectedMinute - 5
                                                            },
                                                            enabled = isAutoCierreEnabled,
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.KeyboardArrowDown,
                                                                contentDescription = "Bajar minuto",
                                                                tint = if (isAutoCierreEnabled) MaterialTheme.colorScheme.primary else Color(0xFF666666)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(10.dp))

                                                    // AM / PM
                                                    Row {
                                                        FilterChip(
                                                            selected = !isPm,
                                                            onClick = { if (isAutoCierreEnabled) isPm = false },
                                                            label = { Text("AM") },
                                                            colors = FilterChipDefaults.filterChipColors(
                                                                containerColor = Color(0xFF161618),
                                                                labelColor = Color(0xFFB0B0B5),
                                                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                                                selectedLabelColor = MaterialTheme.colorScheme.primary
                                                            ),
                                                            border = FilterChipDefaults.filterChipBorder(
                                                                enabled = true,
                                                                selected = !isPm,
                                                                borderColor = Color(0xFF35353C),
                                                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                            )
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        FilterChip(
                                                            selected = isPm,
                                                            onClick = { if (isAutoCierreEnabled) isPm = true },
                                                            label = { Text("PM") },
                                                            colors = FilterChipDefaults.filterChipColors(
                                                                containerColor = Color(0xFF161618),
                                                                labelColor = Color(0xFFB0B0B5),
                                                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                                                selectedLabelColor = MaterialTheme.colorScheme.primary
                                                            ),
                                                            border = FilterChipDefaults.filterChipBorder(
                                                                enabled = true,
                                                                selected = isPm,
                                                                borderColor = Color(0xFF35353C),
                                                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                            )
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                // Modo a Cerrar
                                                Text("Modo a Cerrar:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE0E0E0))
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    val modes = listOf("Todos" to "TODOS", "Personal" to "PERSONAL", "Pedidos" to "PEDIDOS", "Tienda" to "TIENDA")
                                                    modes.forEach { (label, value) ->
                                                        val selected = selectedAutoCierreMode == value
                                                        FilterChip(
                                                            selected = selected,
                                                            onClick = { if (isAutoCierreEnabled) selectedAutoCierreMode = value },
                                                            label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                                                            modifier = Modifier.weight(1f),
                                                            colors = FilterChipDefaults.filterChipColors(
                                                                containerColor = Color(0xFF161618),
                                                                labelColor = Color(0xFFB0B0B5),
                                                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                                                selectedLabelColor = MaterialTheme.colorScheme.primary
                                                            ),
                                                            border = FilterChipDefaults.filterChipBorder(
                                                                enabled = true,
                                                                selected = selected,
                                                                borderColor = Color(0xFF35353C),
                                                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            // Botón Guardar
                                            Button(
                                                onClick = {
                                                    val finalHour24 = if (isPm) {
                                                        if (selectedHour12 == 12) 12 else selectedHour12 + 12
                                                    } else {
                                                        if (selectedHour12 == 12) 0 else selectedHour12
                                                    }
                                                    viewModel.updateAutoCierreSchedule(
                                                        enabled = isAutoCierreEnabled,
                                                        frequency = selectedFrequency,
                                                        hour = finalHour24,
                                                        minute = selectedMinute,
                                                        mode = selectedAutoCierreMode
                                                    )
                                                    Toast.makeText(context, "Configuración guardada", Toast.LENGTH_SHORT).show()
                                                    isAutoCierreExpanded = false
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = Color.Black
                                                )
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Guardar Configuración", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Botones para generar nuevos cierres manuales
                            Text(
                                "Cierre Manual Inmediato",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .onGloballyPositioned { cierresTourTargetsBounds[TourTarget.CIERRES_MANUAL_BTN] = it.boundsInWindow() },
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val manualModes = listOf(
                                    "Personal" to "PERSONAL",
                                    "Pedidos" to "PEDIDOS",
                                    "Tienda" to "TIENDA",
                                    "Todos" to "TODOS"
                                )
                                manualModes.forEach { (label, mode) ->
                                    Button(
                                        onClick = {
                                            modeToClose = mode
                                            nameForCierre = if (mode == "TODOS") {
                                                "Cierre General - " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                            } else {
                                                "Cierre $label - " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                            }
                                            showConfirmation = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF25252A),
                                            contentColor = Color.White
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF3A3A42))
                                    ) {
                                        Text(
                                            text = label,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = Color(0xFF33333A),
                                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                            )

                            // Lista de cierres previos
                            Text(
                                "Historial de Cierres",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .onGloballyPositioned { cierresTourTargetsBounds[TourTarget.CIERRES_HISTORY_LIST] = it.boundsInWindow() }
                            )
                        }

                        items(cierres) { cierre ->
                            CierreItem(cierre = cierre, onDelete = { viewModel.deleteCierreSession(cierre) }, onClick = { selectedCierre = cierre })
                        }
                    }
                } else {
                    // Vista de deudas pendientes
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp)) {
                        items(fiadores) { fiador ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                                border = BorderStroke(1.dp, Color(0xFF2C2C32))
                            ) {
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

            CoachMarkOverlay(
                visible = isCierresTourActive && cierresTourSteps.isNotEmpty(),
                steps = cierresTourSteps,
                currentStepIndex = currentCierresTourStepIndex,
                targetsBounds = cierresTourTargetsBounds,
                onNext = { if (currentCierresTourStepIndex < cierresTourSteps.size - 1) currentCierresTourStepIndex++ },
                onPrev = { if (currentCierresTourStepIndex > 0) currentCierresTourStepIndex-- },
                onSkip = {
                    if (viewModel.userId.isNotEmpty()) TourManager.markZoneSeen(context, viewModel.userId, TourZone.CIERRES)
                    isCierresTourActive = false
                },
                onFinish = {
                    if (viewModel.userId.isNotEmpty()) TourManager.markZoneSeen(context, viewModel.userId, TourZone.CIERRES)
                    isCierresTourActive = false
                }
            )
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
                Button(
                    onClick = {
                        viewModel.createCierreSession(modeToClose, nameForCierre, 0.0, 0.0)
                        showConfirmation = false
                    }
                ) {
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
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, Color(0xFF2C2C32))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Detalles del Cierre", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar") }
                }
                Text("Nombre: ${cierre.name}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Text("Modo: ${cierre.mode}")
            Text("Fecha: ${sdf.format(Date(cierre.timestamp))}", color = Color(0xFF9E9E9E))

                HorizontalDivider(
                    color = Color(0xFF33333A),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (cierre.mode == "PERSONAL" || cierre.mode == "TIENDA") {
                    val transactions by viewModel.getTransactionsForCierre(cierre.id).collectAsState(initial = emptyList())
                    val incomes = transactions.filter { it.isIncome }.sumOf { it.amount }
                    val expenses = transactions.filter { !it.isIncome }.sumOf { it.amount }

                    Text("Total Ingresos: ${formatter.format(incomes)}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    Text("Total Egresos: ${formatter.format(expenses)}", fontWeight = FontWeight.Bold, color = Color.Red)

                    HorizontalDivider(
                        color = Color(0xFF33333A),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text("Transacciones:", fontWeight = FontWeight.Bold)
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(transactions) { t ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                                border = BorderStroke(1.dp, Color(0xFF2C2C32))
                            ) {
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
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                                border = BorderStroke(1.dp, Color(0xFF2C2C32))
                            ) {
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
        border = BorderStroke(1.dp, Color(0xFF2C2C32))
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
            Text("Fecha: ${sdf.format(Date(cierre.timestamp))}", color = Color(0xFF9E9E9E))
        }
    }
}
