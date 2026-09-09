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
import com.xxcamixx.contabilidad.util.formatCOP

@Composable
fun SellComercioDialog(
    product: ComercioProduct,
    onDismiss: () -> Unit,
    onSave: (quantity: Double, salePrice: Double) -> Unit
) {
    var quantityStr by remember { mutableStateOf("") }
    var salePriceStr by remember { mutableStateOf(if(product.salePricePerUnit > 0) product.salePricePerUnit.toInt().toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vender ${product.name}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Stock disponible: ${product.quantityInStock} ${product.unit}")
                Text("Costo promedio: ${formatCOP(product.costPerUnit)}")

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = cleanAmountInput(it) },
                    label = { Text("Cantidad a vender") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = salePriceStr,
                    onValueChange = { salePriceStr = cleanAmountInput(it) },
                    label = { Text("Precio de Venta (por ${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = AmountVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val q = quantityStr.toDoubleOrNull() ?: 0.0
                val p = salePriceStr.toDoubleOrNull() ?: 0.0
                if (q > 0 && p > 0) {
                    val total = q * p
                    val profit = (p - product.costPerUnit) * q
                    Text("Total Venta: ${formatCOP(total)}", color = MaterialTheme.colorScheme.primary)
                    Text("Ganancia Neta: ${formatCOP(profit)}", color = if(profit>=0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val q = quantityStr.toDoubleOrNull() ?: 0.0
                val p = salePriceStr.toDoubleOrNull() ?: 0.0
                if (q > 0 && q <= product.quantityInStock && p > 0) {
                    onSave(q, p)
                    onDismiss()
                }
            }) { Text("Confirmar Venta") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
