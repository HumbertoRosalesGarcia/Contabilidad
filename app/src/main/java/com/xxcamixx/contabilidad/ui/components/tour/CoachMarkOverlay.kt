package com.xxcamixx.contabilidad.ui.components.tour

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun CoachMarkOverlay(
    visible: Boolean,
    steps: List<TourStep>,
    currentStepIndex: Int,
    targetsBounds: Map<TourTarget, Rect>,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    if (!visible || steps.isEmpty() || currentStepIndex !in steps.indices) return

    val currentStep = steps[currentStepIndex]
    val density = LocalDensity.current

    // Posición del overlay en la ventana para calcular coordenadas relativas exactas
    var overlayWindowOffset by remember { mutableStateOf(Offset.Zero) }
    var overlaySize by remember { mutableStateOf(Size.Zero) }

    val rawTargetRect = targetsBounds[currentStep.target]
    val relativeTargetRect = remember(rawTargetRect, overlayWindowOffset) {
        rawTargetRect?.translate(-overlayWindowOffset.x, -overlayWindowOffset.y)
    }

    // Animación suave de transición de posición y tamaño del recorte
    val animLeft by animateFloatAsState(
        targetValue = relativeTargetRect?.left ?: (overlaySize.width / 2f - 40f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animLeft"
    )
    val animTop by animateFloatAsState(
        targetValue = relativeTargetRect?.top ?: (overlaySize.height / 2f - 40f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animTop"
    )
    val animRight by animateFloatAsState(
        targetValue = relativeTargetRect?.right ?: (overlaySize.width / 2f + 40f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animRight"
    )
    val animBottom by animateFloatAsState(
        targetValue = relativeTargetRect?.bottom ?: (overlaySize.height / 2f + 40f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animBottom"
    )

    // Animación de pulso / halo brillante alrededor del recorte
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                overlayWindowOffset = coordinates.positionInWindow()
                overlaySize = Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Previene clics a través de la capa oscura
            }
    ) {
        // Lienzo para oscurecer y recortar
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f) // Necesario para BlendMode.Clear en hardware acceleration
        ) {
            // Fondo oscuro atenuado
            drawRect(color = Color.Black.copy(alpha = 0.78f))

            if (relativeTargetRect != null && overlaySize.width > 0 && overlaySize.height > 0) {
                val paddingPx = with(density) { 8.dp.toPx() }
                val cornerRadiusPx = with(density) { 14.dp.toPx() }

                val cutoutRect = Rect(
                    left = animLeft - paddingPx,
                    top = animTop - paddingPx,
                    right = animRight + paddingPx,
                    bottom = animBottom + paddingPx
                )

                // Perforar agujero transparente en el fondo atenuado
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(cutoutRect.left, cutoutRect.top),
                    size = Size(cutoutRect.width, cutoutRect.height),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    blendMode = BlendMode.Clear
                )

                // Borde pulsante iluminado
                val borderPadding = (cutoutRect.width * (pulseScale - 1f)) / 2f
                drawRoundRect(
                    color = primaryColor.copy(alpha = pulseAlpha),
                    topLeft = Offset(cutoutRect.left - borderPadding, cutoutRect.top - borderPadding),
                    size = Size(cutoutRect.width + borderPadding * 2, cutoutRect.height + borderPadding * 2),
                    cornerRadius = CornerRadius(cornerRadiusPx + borderPadding, cornerRadiusPx + borderPadding),
                    style = Stroke(width = with(density) { 2.5.dp.toPx() })
                )
            }
        }

        // Tarjeta explicativa con Tooltip inteligente
        if (overlaySize.height > 0) {
            val targetCenterY = (animTop + animBottom) / 2f
            val isTargetInTopHalf = targetCenterY < overlaySize.height * 0.52f

            val tooltipAlignment = when (currentStep.position) {
                TooltipPosition.ABOVE -> Alignment.TopCenter
                TooltipPosition.BELOW -> Alignment.BottomCenter
                TooltipPosition.CENTER -> Alignment.Center
                TooltipPosition.AUTO -> if (isTargetInTopHalf) Alignment.BottomCenter else Alignment.TopCenter
            }

            val verticalPadding = when (tooltipAlignment) {
                Alignment.BottomCenter -> PaddingValues(bottom = 60.dp, start = 18.dp, end = 18.dp)
                Alignment.TopCenter -> PaddingValues(top = 75.dp, start = 18.dp, end = 18.dp)
                else -> PaddingValues(horizontal = 18.dp)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(verticalPadding),
                contentAlignment = tooltipAlignment
            ) {
                TooltipCard(
                    step = currentStep,
                    stepNumber = currentStepIndex + 1,
                    totalSteps = steps.size,
                    hasPrev = currentStepIndex > 0,
                    hasNext = currentStepIndex < steps.size - 1,
                    onNext = onNext,
                    onPrev = onPrev,
                    onSkip = onSkip,
                    onFinish = onFinish
                )
            }
        }
    }
}

@Composable
fun TooltipCard(
    step: TourStep,
    stepNumber: Int,
    totalSteps: Int,
    hasPrev: Boolean,
    hasNext: Boolean,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(18.dp), spotColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Cabecera: Icono, Contador de pasos y Botón de cerrar/saltar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Paso $stepNumber de $totalSteps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = step.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onSkip,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar tour",
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Descripción de la función
            Text(
                text = step.description,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = Color(0xFFD1D1D6)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Indicador de puntos (Pill dots)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..totalSteps) {
                    val isCurrent = i == stepNumber
                    val isCompleted = i < stepNumber
                    val dotWidth = if (isCurrent) 22.dp else 6.dp
                    val dotColor = if (isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else if (isCompleted) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        Color(0xFF383840)
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Anterior
                if (hasPrev) {
                    OutlinedButton(
                        onClick = onPrev,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF383840)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCCCCCC)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Anterior", fontSize = 12.sp)
                    }
                } else {
                    TextButton(
                        onClick = onSkip,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF8E8E93))
                    ) {
                        Text("Saltar tour", fontSize = 12.sp)
                    }
                }

                // Botón Siguiente o Finalizar
                if (hasNext) {
                    Button(
                        onClick = onNext,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text("Siguiente", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                } else {
                    Button(
                        onClick = onFinish,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("¡Comenzar!", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
