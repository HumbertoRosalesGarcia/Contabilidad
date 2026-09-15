package com.xxcamixx.contabilidad.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun TechSplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) { delay(2200L); onTimeout() }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val infiniteTransition = rememberInfiniteTransition(label = "spin")
        val rotation1 by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing)), label = "spin1")
        val rotation2 by infiniteTransition.animateFloat(initialValue = 360f, targetValue = 0f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing)), label = "spin2")

        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawArc(color = Color(0xFF2196F3), startAngle = rotation1, sweepAngle = 270f, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)); drawArc(color = Color(0xFF81C784), startAngle = rotation2, sweepAngle = 200f, useCenter = false, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round), size = Size(size.width * 0.75f, size.height * 0.75f), topLeft = Offset(size.width * 0.125f, size.height * 0.125f)) }
            Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(60.dp))
        }
        Text("Accediendo a Billetera...", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp))
    }
}
