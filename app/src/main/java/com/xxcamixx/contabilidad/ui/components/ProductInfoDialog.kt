package com.xxcamixx.contabilidad.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.xxcamixx.contabilidad.model.Product
import com.xxcamixx.contabilidad.util.formatMoneyMain
import com.xxcamixx.contabilidad.util.formatMoneySec

@Composable
fun ProductInfoDialog(product: Product, selectedCountry: String, bcvRate: Double, onDismiss: () -> Unit, onDeleteCompletely: () -> Unit) {
    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Detalle del Producto ℹ️", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(product.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // PRECIO UNITARIO
                val unitName = when(product.unit) { "Kg" -> "Kilo"; "L" -> "Litro"; else -> "Unidad" }
                Text("Precio por $unitName:", fontSize = 14.sp, color = Color.Gray)
                Text(formatMoneyMain(product.price, selectedCountry), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (selectedCountry == "Venezuela" && bcvRate > 0) {
                    // Muestra el equivalente en Bs abajo
                    Text(formatMoneySec(product.price, selectedCountry, bcvRate).replace("= ", ""), fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Stock Disponible: ${product.stock} ${product.unit}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.Gray.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                // VALOR TOTAL EN INVENTARIO
                val totalValue = product.price * product.stock
                Text("Valor Total en Inventario:", fontSize = 14.sp, color = Color.Gray)
                Text(formatMoneyMain(totalValue, selectedCountry), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                if (selectedCountry == "Venezuela" && bcvRate > 0) {
                    // Muestra el equivalente en Bs abajo
                    Text(formatMoneySec(totalValue, selectedCountry, bcvRate).replace("= ", ""), fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = { }, dismissButton = { TextButton(onClick = onDeleteCompletely, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Text("Eliminar del Inventario", fontWeight = FontWeight.Bold) } }
    )
}
