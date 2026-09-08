package com.xxcamixx.contabilidad.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xxcamixx.contabilidad.model.Transaction

@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onConfirm: (isCash: Boolean, isDigital: Boolean, note: String) -> Unit
) {
    var note by remember { mutableStateOf(transaction.note) }
    var isCash by remember { mutableStateOf(transaction.cashAmount > 0) }
    var isDigital by remember { mutableStateOf(transaction.digitalAmount > 0) }

    // Si era mixto (ambos > 0) no se permite cambiar a un solo tipo fácilmente usando checkbox aquí, pero 
    // asumiremos que el usuario usará radio buttons para pasarlo todo a Efectivo o todo a Digital, 
    // y si era mixto inicialmente, lo dejamos mixto si no toca los radio buttons.
    // Para simplificar, usemos un estado: "Efectivo", "Digital", "Mixto".
    var paymentType by remember { 
        mutableStateOf(
            when {
                transaction.cashAmount > 0 && transaction.digitalAmount > 0 -> "Mixto"
                transaction.digitalAmount > 0 -> "Digital"
                else -> "Efectivo"
            }
        ) 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Movimiento") },
        text = {
            Column {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Método de Pago:")
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = paymentType == "Efectivo", onClick = { paymentType = "Efectivo" })
                    Text("Efectivo")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = paymentType == "Digital", onClick = { paymentType = "Digital" })
                    Text("Digital")
                }
                if (paymentType == "Mixto") {
                    Text("(Actualmente Mixto)", color = androidx.compose.ui.graphics.Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    val newCash = paymentType == "Efectivo" || paymentType == "Mixto"
                    val newDigital = paymentType == "Digital" || paymentType == "Mixto"
                    onConfirm(newCash, newDigital, note) 
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
