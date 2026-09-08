package com.xxcamixx.contabilidad.ui.dialogs

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.window.DialogProperties
import com.xxcamixx.contabilidad.model.ComercioProduct
import com.xxcamixx.contabilidad.util.AmountVisualTransformation
import com.xxcamixx.contabilidad.util.cleanAmountInput
import com.xxcamixx.contabilidad.util.formatQty
import com.xxcamixx.contabilidad.util.loadBitmapFromUri
import com.xxcamixx.contabilidad.util.saveImageToInternalStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditComercioProductDialog(
    product: ComercioProduct,
    country: String,
    bcvRate: Double,
    onDismiss: () -> Unit,
    onSave: (name: String, unit: String, stock: Double, cost: Double, salePrice: Double, imageUri: String?) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var unit by remember { mutableStateOf(product.unit) }
    var stockStr by remember { mutableStateOf(formatQty(product.quantityInStock)) }
    var costStr by remember {
        mutableStateOf(
            if (product.costPerUnit > 0) {
                if (product.costPerUnit % 1.0 == 0.0) product.costPerUnit.toInt().toString() else product.costPerUnit.toString()
            } else ""
        )
    }
    var salePriceStr by remember {
        mutableStateOf(
            if (product.salePricePerUnit > 0) {
                if (product.salePricePerUnit % 1.0 == 0.0) product.salePricePerUnit.toInt().toString() else product.salePricePerUnit.toString()
            } else ""
        )
    }
    var imageUri by remember { mutableStateOf(product.imageUri) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var showImageSourceDialog by remember { mutableStateOf(false) }

    fun onImagePicked(uri: Uri?) {
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val savedUri = saveImageToInternalStorage(context, uri)
                withContext(Dispatchers.Main) {
                    imageUri = savedUri
                }
            }
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        onImagePicked(uri)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        onImagePicked(uri)
    }

    if (showImageSourceDialog) {
        ImageSourceDialog(
            onDismiss = { showImageSourceDialog = false },
            onSelectGallery = { galleryPickerLauncher.launch("image/*") },
            onSelectFileManager = { filePickerLauncher.launch(arrayOf("image/*")) }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Editar Producto", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(scrollState)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Zona de Foto
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(alpha = 0.2f))
                        .clickable { showImageSourceDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        var bitmap by remember(imageUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                        LaunchedEffect(imageUri) {
                            val loaded = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                loadBitmapFromUri(context, imageUri)
                            }
                            bitmap = loaded
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap!!.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }

                        // Botón flotante para quitar foto
                        IconButton(
                            onClick = { imageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Quitar foto", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Image, contentDescription = "Añadir foto", modifier = Modifier.size(32.dp), tint = Color.Gray)
                            Text("Toca para añadir o cambiar foto", color = Color.Gray, fontSize = 12.sp)
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

                // Selector de Unidades por Categoría: Peso / Volumen / Uds
                var showPesoMenu by remember { mutableStateOf(false) }
                var showVolumenMenu by remember { mutableStateOf(false) }
                val pesoUnits = listOf("kg", "lb", "g", "oz")
                val volumenUnits = listOf("L", "ml", "gal", "paquete")

                val isPesoSelected = unit in pesoUnits
                val isVolumenSelected = unit in volumenUnits
                val isUdsSelected = unit == "Uds"

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Unidad de Medida:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Categoría Peso
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showPesoMenu = true },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isPesoSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (isPesoSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                                    )
                                )
                            ) {
                                Text(
                                    text = if (isPesoSelected) "Peso ($unit)" else "Peso ▾",
                                    fontSize = 12.sp,
                                    fontWeight = if (isPesoSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isPesoSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showPesoMenu,
                                onDismissRequest = { showPesoMenu = false }
                            ) {
                                DropdownMenuItem(text = { Text("Kilogramo (kg)") }, onClick = { unit = "kg"; showPesoMenu = false })
                                DropdownMenuItem(text = { Text("Libra (lb)") }, onClick = { unit = "lb"; showPesoMenu = false })
                                DropdownMenuItem(text = { Text("Gramo (g)") }, onClick = { unit = "g"; showPesoMenu = false })
                                DropdownMenuItem(text = { Text("Onza (oz)") }, onClick = { unit = "oz"; showPesoMenu = false })
                            }
                        }

                        // Categoría Volumen
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showVolumenMenu = true },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isVolumenSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (isVolumenSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                                    )
                                )
                            ) {
                                Text(
                                    text = if (isVolumenSelected) "Volumen ($unit)" else "Volumen ▾",
                                    fontSize = 12.sp,
                                    fontWeight = if (isVolumenSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isVolumenSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showVolumenMenu,
                                onDismissRequest = { showVolumenMenu = false }
                            ) {
                                DropdownMenuItem(text = { Text("Litro (L)") }, onClick = { unit = "L"; showVolumenMenu = false })
                                DropdownMenuItem(text = { Text("Mililitro (ml)") }, onClick = { unit = "ml"; showVolumenMenu = false })
                                DropdownMenuItem(text = { Text("Galón (gal)") }, onClick = { unit = "gal"; showVolumenMenu = false })
                                DropdownMenuItem(text = { Text("Paquete (pq)") }, onClick = { unit = "paquete"; showVolumenMenu = false })
                            }
                        }

                        // Categoría Uds
                        OutlinedButton(
                            onClick = {
                                unit = "Uds"
                                if (stockStr.contains(".")) stockStr = stockStr.substringBefore(".")
                            },
                            modifier = Modifier.weight(0.9f).height(40.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isUdsSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isUdsSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                                )
                            )
                        ) {
                            Text(
                                text = "Uds",
                                fontSize = 12.sp,
                                fontWeight = if (isUdsSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isUdsSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { input ->
                        stockStr = if (unit == "Uds") {
                            com.xxcamixx.contabilidad.util.cleanAmountInput(input)
                        } else {
                            com.xxcamixx.contabilidad.util.cleanDecimalWithPrecision(input, 2)
                        }
                    },
                    label = { Text("Cantidad disponible ($unit)") },
                    placeholder = { Text(if (unit == "Uds") "Ej: 10" else "Ej: 1.46") },
                    keyboardOptions = KeyboardOptions(keyboardType = if (unit == "Uds") KeyboardType.Number else KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = cleanAmountInput(it) },
                    label = { Text("Costo por $unit (compra al proveedor)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = AmountVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = salePriceStr,
                    onValueChange = { salePriceStr = cleanAmountInput(it) },
                    label = { Text("Precio de venta al cliente") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = AmountVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        val stock = stockStr.toDoubleOrNull() ?: product.quantityInStock
                        val cost = costStr.toDoubleOrNull() ?: product.costPerUnit
                        val sp = salePriceStr.toDoubleOrNull() ?: product.salePricePerUnit
                        if (name.isNotBlank()) {
                            onSave(name.trim(), unit, stock, cost, sp, imageUri)
                        }
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    enabled = name.isNotBlank()
                ) {
                    Text("Guardar Cambios")
                }
            }
        },
        dismissButton = null
    )
}
