package com.xxcamixx.contabilidad.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xxcamixx.contabilidad.model.ComercioProduct
import com.xxcamixx.contabilidad.util.AmountVisualTransformation
import com.xxcamixx.contabilidad.util.cleanAmountInput

@Composable
fun RestockComercioDialog(
    product: ComercioProduct,
    onDismiss: () -> Unit,
    onSave: (quantity: Double, cost: Double) -> Unit
) {
    var quantityStr by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reabastecer ${product.name}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Stock actual: ${product.quantityInStock} ${product.unit}")
                
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = cleanAmountInput(it) },
                    label = { Text("Cantidad nueva comprada") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = cleanAmountInput(it) },
                    label = { Text("Nuevo costo (por ${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = AmountVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val q = quantityStr.toDoubleOrNull() ?: 0.0
                val c = costStr.toDoubleOrNull() ?: 0.0
                if (q > 0 && c > 0) {
                    onSave(q, c)
                    onDismiss()
                }
            }) { Text("Guardar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
