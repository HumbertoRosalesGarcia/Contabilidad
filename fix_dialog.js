const fs = require('fs');

const code = `package com.xxcamixx.contabilidad.ui.dialogs

import android.net.Uri
import android.os.Build
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xxcamixx.contabilidad.util.cleanAmountInput
import com.xxcamixx.contabilidad.util.formatMoneyMain
import com.xxcamixx.contabilidad.util.formatMoneySec

@Composable
fun AddComercioProductDialog(
    country: String,
    bcvRate: Double,
    onDismiss: () -> Unit,
    onSave: (name: String, unit: String, quantity: Double, cost: Double, salePrice: Double, imageUri: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }
    var quantityStr by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var salePriceStr by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { e.printStackTrace() }
            imageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Producto en Pedido") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Photo Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(alpha = 0.2f))
                        .clickable { imagePickerLauncher.launch(arrayOf("image/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        var bitmap by remember(imageUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                        LaunchedEffect(imageUri) {
                            val loadedBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, Uri.parse(imageUri!!)))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.parse(imageUri!!))
                                    }
                                } catch (e: Exception) { null }
                            }
                            bitmap = loadedBitmap
                        }
                        if (bitmap != null) {
                            Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Image, contentDescription = "A\\u00F1adir foto", modifier = Modifier.size(32.dp), tint = Color.Gray)
                            Text("Toca para a\\u00F1adir foto", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

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
                            Text(u, fontSize = 14.sp, maxLines = 1, softWrap = false)
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
                    label = { Text("Costo por unidad (compra)") },
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
                        Text("Costo Total de Inversi\\u00F3n:", fontSize = 12.sp, color = Color.Gray)
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
                    onSave(name, unit, q, c, sp, imageUri)
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
console.log('AddComercioProductDialog updated for photos and encoding');
