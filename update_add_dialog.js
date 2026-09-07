const fs = require('fs');

const code = `package com.xxcamixx.contabilidad.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.xxcamixx.contabilidad.util.cleanAmountInput
import com.xxcamixx.contabilidad.util.formatMoneyMain
import com.xxcamixx.contabilidad.util.formatMoneySec

@Composable
fun AddComercioProductDialog(
    country: String,
    bcvRate: Double,
    onDismiss: () -> Unit,
    onSave: (name: String, unit: String, quantity: Double, cost: Double, salePrice: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }
    var quantityStr by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var salePriceStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Producto en Pedido") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Producto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("kg", "lb", "Uds", "paq").forEach { u ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(selected = unit == u, onClick = { unit = u })
                            Text(u, fontSize = 14.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = cleanAmountInput(it) },
                    label = { Text("Cantidad comprada") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = cleanAmountInput(it) },
                    label = { Text("Costo por unidad (precio al que se compra)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = salePriceStr,
                    onValueChange = { salePriceStr = cleanAmountInput(it) },
                    label = { Text("Precio de Venta Sugerido") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                val q = quantityStr.toDoubleOrNull() ?: 0.0
                val c = costStr.toDoubleOrNull() ?: 0.0
                if (q > 0 && c > 0) {
                    val total = q * c
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Costo Total de Inversión:", fontSize = 12.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(formatMoneyMain(total, country), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(formatMoneySec(total, country, bcvRate), color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val q = quantityStr.toDoubleOrNull() ?: 0.0
                val c = costStr.toDoubleOrNull() ?: 0.0
                val sp = salePriceStr.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && q > 0 && c > 0 && sp > 0) {
                    onSave(name, unit, q, c, sp)
                    onDismiss()
                }
            }) { Text("Guardar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
`

fs.writeFileSync('d:/Proyectos/Contabilidad/app/src/main/java/com/xxcamixx/contabilidad/ui/dialogs/AddComercioProductDialog.kt', code, 'utf-8');
console.log('AddComercioProductDialog updated');
