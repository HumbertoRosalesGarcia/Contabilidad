package com.xxcamixx.contabilidad.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlanCardInfo(
    title: String,
    subtitle: String,
    features: List<String>,
    restrictions: List<String>,
    isGold: Boolean = false // <-- NUEVO: Parámetro para saber si es el plan GOLD
) {
    Card(
        modifier = Modifier.width(280.dp).fillMaxHeight(),
        // MODIFICADO: Agrega el borde dorado si es el plan GOLD
        border = if (isGold) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD700)) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight()) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))

            // Características Permitidas
            Column(modifier = Modifier.weight(1f)) {
                features.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(Icons.Filled.Check, contentDescription = "Permitido", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(feature, fontSize = 13.sp)
                    }
                }
                if (restrictions.isNotEmpty()) { Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha=0.2f)) }

                // Restricciones
                restrictions.forEach { restriction ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp).alpha(0.5f)) {
                        Icon(Icons.Filled.Close, contentDescription = "No Permitido", tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(restriction, fontSize = 13.sp, textDecoration = TextDecoration.LineThrough)
                    }
                }
            }

            // NUEVO: Botón de Compra Atractivo
            Button(
                onClick = { /* Acción para contactar al admin/comprar */ },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)
            ) {
                Text("¡Adquiérelo Ahora!", fontWeight = FontWeight.Bold)
            }
        }
    }
}
