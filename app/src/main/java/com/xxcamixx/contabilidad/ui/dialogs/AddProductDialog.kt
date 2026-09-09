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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.xxcamixx.contabilidad.util.AmountVisualTransformation
import com.xxcamixx.contabilidad.util.cleanAmountInput
import com.xxcamixx.contabilidad.util.cleanDecimalInput
import com.xxcamixx.contabilidad.util.formatBs
import com.xxcamixx.contabilidad.util.formatDateOnly
import com.xxcamixx.contabilidad.util.formatUSD
import com.xxcamixx.contabilidad.viewmodel.ProductDraftState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(isEditMode: Boolean, draftState: ProductDraftState, selectedCountry: String, bcvRate: Double, categories: List<String>, onDismiss: () -> Unit, onConfirm: (Double, Double) -> Unit) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var inputCurrency by remember { mutableStateOf("USD") }
    val isBsInput = selectedCountry == "Venezuela" && inputCurrency == "BS"

    // NUEVAS VARIABLES: Mantienen la independencia y precisión de ambos valores
    var bsPurchase by remember { mutableStateOf("") }
    var bsPrice by remember { mutableStateOf("") }
    var usdPurchase by remember { mutableStateOf("") }
    var usdPrice by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> if (uri != null) { try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) { e.printStackTrace() }; draftState.imageUri = uri.toString() } }
    if (showDatePicker) { CustomDatePickerDialog(initialDateMillis = draftState.expiryDateMillis ?: System.currentTimeMillis(), onDismiss = { showDatePicker = false }, onDateSelected = { selected -> draftState.expiryDateMillis = selected; showDatePicker = false }) }

    // MODIFICADO: Lógica de carga sin pérdida por redondeo visual
    LaunchedEffect(Unit) {
        if (selectedCountry == "Venezuela") {
            val rawP = draftState.purchasePriceRaw.toDoubleOrNull() ?: 0.0
            val rawV = draftState.priceRaw.toDoubleOrNull() ?: 0.0
            if (bcvRate > 0) {
                // Al redondear la vista a 2 decimales, un 1699.999 se restaura visualmente a 1700
                val dfBs = java.text.DecimalFormat("#.##", java.text.DecimalFormatSymbols(Locale.US))
                val dfUsd = java.text.DecimalFormat("#.##", java.text.DecimalFormatSymbols(Locale.US))

                bsPurchase = if (rawP > 0) dfBs.format(rawP * bcvRate) else ""
                bsPrice = if (rawV > 0) dfBs.format(rawV * bcvRate) else ""

                usdPurchase = if (rawP > 0) dfUsd.format(rawP) else ""
                usdPrice = if (rawV > 0) dfUsd.format(rawV) else ""
            }
            inputCurrency = "BS"
            draftState.purchasePriceRaw = bsPurchase
            draftState.priceRaw = bsPrice
        }
    }

    fun switchCurrency(toBs: Boolean) {
        if (bcvRate <= 0) return
        if (toBs) {
            inputCurrency = "BS"
            draftState.purchasePriceRaw = bsPurchase
            draftState.priceRaw = bsPrice
        } else {
            inputCurrency = "USD"
            draftState.purchasePriceRaw = usdPurchase
            draftState.priceRaw = usdPrice
        }
    }

    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(if (isEditMode) "Editar Producto ✏️" else "Nuevo Producto \uD83C\uDFF7️", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).clickable { imagePickerLauncher.launch(arrayOf("image/*")) }, contentAlignment = Alignment.Center) {
                    var bitmap by remember(draftState.imageUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                    LaunchedEffect(draftState.imageUri) { if (draftState.imageUri != null) { val loadedBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, Uri.parse(draftState.imageUri!!))) } else { @Suppress("DEPRECATION") android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.parse(draftState.imageUri!!)) } } catch (e: Exception) { null } }; bitmap = loadedBitmap } else { bitmap = null } }
                    if (draftState.imageUri != null) { if (bitmap != null) { Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = "Imagen del producto", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop) } else { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) } } else { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Image, contentDescription = "Añadir foto", modifier = Modifier.size(48.dp), tint = Color.Gray); Text("Añadir foto del producto", color = Color.Gray, fontSize = 12.sp) } }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = draftState.name, onValueChange = { input -> draftState.name = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }, label = { Text("Nombre del Producto") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences))
                Spacer(modifier = Modifier.height(16.dp))

                if (categories.isNotEmpty()) {
                    var expandedCat by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedCat,
                        onExpandedChange = { expandedCat = !expandedCat }
                    ) {
                        OutlinedTextField(
                            value = draftState.category ?: "Sin Categoría",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCat,
                            onDismissRequest = { expandedCat = false }
                        ) {
                            DropdownMenuItem(text = { Text("Sin Categoría") }, onClick = { draftState.category = null; expandedCat = false })
                            categories.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = { draftState.category = cat; expandedCat = false })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (selectedCountry == "Venezuela") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        FilterChip(selected = inputCurrency == "USD", onClick = { if (inputCurrency == "BS") switchCurrency(false) }, label = { Text("Dólares ($)") })
                        FilterChip(selected = inputCurrency == "BS", onClick = { if (inputCurrency == "USD") switchCurrency(true) }, label = { Text("Bolívares (Bs)") })
                    }
                    Text("Al cambiar de pestaña, el valor que hayas escrito se convertirá automáticamente.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp, top = 4.dp), textAlign = TextAlign.Center)
                }

                val visualTrans = if (selectedCountry == "Venezuela") VisualTransformation.None else AmountVisualTransformation()
                val cleaner = { input: String -> if (selectedCountry == "Venezuela") cleanDecimalInput(input) else cleanAmountInput(input) }
                val symbolText = if (selectedCountry == "Venezuela") if (isBsInput) "(Bs)" else "($)" else ""
                val leadingSym = if (selectedCountry == "Venezuela") if (isBsInput) "Bs" else "$" else "$"

                OutlinedTextField(
                    value = draftState.purchasePriceRaw,
                    onValueChange = { input ->
                        val cleaned = cleaner(input)
                        draftState.purchasePriceRaw = cleaned
                        if (selectedCountry == "Venezuela" && bcvRate > 0) {
                            val num = cleaned.toDoubleOrNull() ?: 0.0
                            val df = java.text.DecimalFormat("#.##", java.text.DecimalFormatSymbols(Locale.US))
                            if (isBsInput) {
                                bsPurchase = cleaned
                                usdPurchase = if (num > 0) df.format(num / bcvRate) else ""
                            } else {
                                usdPurchase = cleaned
                                bsPurchase = if (num > 0) df.format(num * bcvRate) else ""
                            }
                        }
                    },
                    label = { Text("Precio de Compra (Costo) $symbolText") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = visualTrans,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text(leadingSym, color = Color.Gray, modifier = Modifier.padding(start=8.dp)) }
                )
                if (selectedCountry == "Venezuela" && draftState.purchasePriceRaw.isNotEmpty()) {
                    val entered = draftState.purchasePriceRaw.toDoubleOrNull() ?: 0.0
                    val converted = if (isBsInput) (if (bcvRate > 0) "= ${formatUSD(entered / bcvRate)}" else "= $0.00") else "= ${formatBs(entered * bcvRate)}"
                    Text(converted, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = draftState.priceRaw,
                    onValueChange = { input ->
                        val cleaned = cleaner(input)
                        draftState.priceRaw = cleaned
                        if (selectedCountry == "Venezuela" && bcvRate > 0) {
                            val num = cleaned.toDoubleOrNull() ?: 0.0
                            val df = java.text.DecimalFormat("#.##", java.text.DecimalFormatSymbols(Locale.US))
                            if (isBsInput) {
                                bsPrice = cleaned
                                usdPrice = if (num > 0) df.format(num / bcvRate) else ""
                            } else {
                                usdPrice = cleaned
                                bsPrice = if (num > 0) df.format(num * bcvRate) else ""
                            }
                        }
                    },
                    label = { Text("Precio de Venta al Público $symbolText") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = visualTrans,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text(leadingSym, color = Color.Gray, modifier = Modifier.padding(start=8.dp)) }
                )
                if (selectedCountry == "Venezuela" && draftState.priceRaw.isNotEmpty()) {
                    val entered = draftState.priceRaw.toDoubleOrNull() ?: 0.0
                    val converted = if (isBsInput) (if (bcvRate > 0) "= ${formatUSD(entered / bcvRate)}" else "= $0.00") else "= ${formatBs(entered * bcvRate)}"
                    Text(converted, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                }

                Spacer(modifier = Modifier.height(12.dp)); Text("Unidad de Medida", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) { FilterChip(selected = draftState.selectedUnit == "Uds", onClick = { draftState.selectedUnit = "Uds" }, label = { Text("Unidad") }); FilterChip(selected = draftState.selectedUnit == "Kg", onClick = { draftState.selectedUnit = "Kg" }, label = { Text("Kilos") }); FilterChip(selected = draftState.selectedUnit == "L", onClick = { draftState.selectedUnit = "L" }, label = { Text("Litros") }) }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = draftState.stockRaw, onValueChange = { n -> val d = n.filter { it.isDigit() }; draftState.stockRaw = d }, label = { Text("Cantidad Inicial en Stock") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = draftState.minStockRaw, onValueChange = { n -> val d = n.filter { it.isDigit() }; draftState.minStockRaw = d }, label = { Text("Alerta de cantidad baja") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { draftState.hasExpiry = !draftState.hasExpiry }) { Checkbox(checked = draftState.hasExpiry, onCheckedChange = { draftState.hasExpiry = it }); Text("Tiene fecha de vencimiento", fontSize = 14.sp) }
                if (draftState.hasExpiry) { Spacer(modifier = Modifier.height(4.dp)); OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(if (draftState.expiryDateMillis == null) "Seleccionar Fecha \uD83D\uDCC5" else "Vence: ${formatDateOnly(draftState.expiryDateMillis!!)}") } }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalPurchase: Double
                val finalPrice: Double

                if (selectedCountry == "Venezuela" && bcvRate > 0) {
                    // Siempre calcula el resultado final que se guarda en la DB priorizando el Bolívar,
                    // sin importar en qué pestaña estés. Garantiza que el redondeo visual no afecte los datos reales.
                    val pBs = bsPurchase.toDoubleOrNull() ?: 0.0
                    val vBs = bsPrice.toDoubleOrNull() ?: 0.0
                    finalPurchase = pBs / bcvRate
                    finalPrice = vBs / bcvRate
                } else {
                    finalPurchase = draftState.purchasePriceRaw.toDoubleOrNull() ?: 0.0
                    finalPrice = draftState.priceRaw.toDoubleOrNull() ?: 0.0
                }

                onConfirm(finalPurchase, finalPrice)
            }) { Text("Guardar ✔️") }
        },
        dismissButton = { }
    )
}
