package com.xxcamixx.contabilidad.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.xxcamixx.contabilidad.model.Product
import com.xxcamixx.contabilidad.ui.components.PaymentInputRow
import com.xxcamixx.contabilidad.util.AmountVisualTransformation
import com.xxcamixx.contabilidad.util.cleanAmountInput
import com.xxcamixx.contabilidad.util.cleanDecimalInput
import com.xxcamixx.contabilidad.util.formatCOP
import com.xxcamixx.contabilidad.util.formatMoneyMain
import com.xxcamixx.contabilidad.util.formatMoneySec
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutDialog(cartItems: MutableList<Pair<Product, Int>>, products: List<Product>, totalStoreCash: Double, totalStoreDigital: Double, selectedCountry: String, bcvRate: Double, onDismiss: () -> Unit, onConfirmSale: (List<Pair<Product, Int>>, String, String, Double, Double, Double) -> Unit, onFiarVenta: (String, Double, Double) -> Unit) {
    var step by remember { mutableStateOf(1) }
    var buyerName by remember { mutableStateOf("") }
    var isDivided by remember { mutableStateOf(false) }
    var simpleMethod by remember { mutableStateOf("Efectivo") }
    var simpleReceivedRaw by remember { mutableStateOf("") }
    var cashRaw by remember { mutableStateOf("") }
    var digitalRaw by remember { mutableStateOf("") }
    var cashChangeRaw by remember { mutableStateOf("") }
    var digitalChangeRaw by remember { mutableStateOf("") }
    var itemToEdit by remember { mutableStateOf<Pair<Int, Pair<Product, Int>>?>(null) }
    var showProductSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var productToSelectQty by remember { mutableStateOf<Product?>(null) }
    var pocketChange by remember { mutableStateOf(false) }

    val totalCOP = cartItems.sumOf { it.first.price * it.second }

    var checkoutCurrency by remember { mutableStateOf("USD") }
    LaunchedEffect(Unit) { if (selectedCountry == "Venezuela") checkoutCurrency = "BS" }
    val isBsInput = selectedCountry == "Venezuela" && checkoutCurrency == "BS"
    val inputMultiplier = if (isBsInput && bcvRate > 0) 1 / bcvRate else 1.0
    val sym = if (isBsInput) "Bs" else "$"
    val visualTrans = if (selectedCountry == "Venezuela") VisualTransformation.None else AmountVisualTransformation()

    if (itemToEdit != null) {
        var editQtyRaw by remember { mutableStateOf(itemToEdit!!.second.second.toString()) }
        AlertDialog(
            onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
            title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Editar cantidad", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center); IconButton(onClick = { itemToEdit = null }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { Text(itemToEdit!!.second.first.name, fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = editQtyRaw, onValueChange = { editQtyRaw = it.filter { c -> c.isDigit() } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), label = { Text("Nueva Cantidad") }, singleLine = true, modifier = Modifier.width(150.dp), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.Bold)) } },
            confirmButton = { Button(onClick = { val newQty = editQtyRaw.toIntOrNull() ?: 0; if (newQty > 0 && newQty <= itemToEdit!!.second.first.stock) { cartItems[itemToEdit!!.first] = itemToEdit!!.second.first to newQty; itemToEdit = null } else if (newQty == 0) { cartItems.removeAt(itemToEdit!!.first); itemToEdit = null } }) { Text("Guardar") } },
            dismissButton = { }
        )
    }

    if (productToSelectQty != null) {
        var qtyRaw by remember { mutableStateOf("") }
        val p = productToSelectQty!!
        val currentInCart = cartItems.find { it.first.id == p.id }?.second ?: 0
        val maxAvailable = p.stock - currentInCart
        val focusRequester = remember { FocusRequester() }
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

        LaunchedEffect(Unit) { delay(200); focusRequester.requestFocus(); keyboardController?.show() }

        AlertDialog(
            onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
            title = { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Añadir ${p.name}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = { productToSelectQty = null }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") } } },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { Text("Disponible: $maxAvailable ${p.unit}", color = Color.Gray); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = qtyRaw, onValueChange = { qtyRaw = it.filter { c -> c.isDigit() } }, label = { Text("Cantidad") }, placeholder = { Text("0", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.Gray.copy(alpha = 0.5f)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.width(150.dp).padding(vertical = 8.dp).focusRequester(focusRequester), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.Bold)) } },
            confirmButton = { Button(onClick = { val q = qtyRaw.toIntOrNull() ?: 0; if (q > 0 && q <= maxAvailable) { val existing = cartItems.find { it.first.id == p.id }; if (existing != null) { val idx = cartItems.indexOf(existing); cartItems[idx] = existing.copy(second = existing.second + q) } else { cartItems.add(Pair(p, q)) }; productToSelectQty = null; showProductSearch = false; searchQuery = "" } }) { Text("Añadir") } },
            dismissButton = { }
        )
    }

    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (showProductSearch) "Buscar Producto \uD83D\uDD0D" else if (step == 1) "Resumen de Venta \uD83D\uDED2" else "Opciones de Pago \uD83D\uDCB3", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (showProductSearch) {
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Nombre del producto...") }, leadingIcon = { Icon(Icons.Filled.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    val filteredProducts = products.filter { it.name.contains(searchQuery, ignoreCase = true) && it.stock > 0 }
                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                        items(filteredProducts) { p ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { productToSelectQty = p }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text("Stock: ${p.stock} ${p.unit}", color = Color.Gray, fontSize = 12.sp) }; Text(formatCOP(p.price), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                            Divider(color = Color.Gray.copy(alpha = 0.2f))
                        }
                    }
                } else if (step == 1) {
                    if (cartItems.isEmpty()) { Text("El carrito está vacío.", modifier = Modifier.padding(16.dp)) } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                            itemsIndexed(cartItems) { index, item ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text("${item.second}x ${item.first.name}", modifier = Modifier.weight(1f), fontSize = 14.sp, maxLines=1, overflow = TextOverflow.Ellipsis)
                                    // MODIFICADO: Muestra siempre los decimales respetando el país
                                    Text(formatMoneyMain(item.first.price * item.second, selectedCountry), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    IconButton(onClick = { itemToEdit = Pair(index, item) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Edit, "Editar", tint = Color.Blue, modifier = Modifier.size(16.dp)) }
                                    IconButton(onClick = { cartItems.removeAt(index) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Delete, "Eliminar", tint = Color.Red, modifier = Modifier.size(16.dp)) }
                                }
                            }
                        }
                        TextButton(onClick = { showProductSearch = true }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp)) { Text("+ Agregar producto nuevo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Total a cobrar:", fontSize = 12.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatMoneyMain(totalCOP, selectedCountry), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                            if (selectedCountry == "Venezuela" && bcvRate > 0) {
                                Text(" ${formatMoneySec(totalCOP, selectedCountry, bcvRate)}", fontSize = 16.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(value = buyerName, onValueChange = { buyerName = it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() } }, label = { Text("Nombre del Cliente (Opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth().background(Color.DarkGray.copy(alpha=0.2f), RoundedCornerShape(8.dp)).padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Caja (Efectivo)", fontSize=10.sp, color=Color.Gray); Text(formatCOP(totalStoreCash), fontSize=13.sp, fontWeight=FontWeight.Bold) }; Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Banco (Digital)", fontSize=10.sp, color=Color.Gray); Text(formatCOP(totalStoreDigital), fontSize=13.sp, fontWeight=FontWeight.Bold) } }
                    Spacer(modifier = Modifier.height(12.dp)); Text("Total de la compra:", fontSize = 13.sp);
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatMoneyMain(totalCOP, selectedCountry), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        if (selectedCountry == "Venezuela" && bcvRate > 0) {
                            Text(" ${formatMoneySec(totalCOP, selectedCountry, bcvRate)}", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedCountry == "Venezuela") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            FilterChip(selected = checkoutCurrency == "USD", onClick = { checkoutCurrency = "USD" }, label = { Text("Ingresar en $") })
                            FilterChip(selected = checkoutCurrency == "BS", onClick = { checkoutCurrency = "BS" }, label = { Text("Ingresar en Bs") })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isDivided = !isDivided; pocketChange = false }) { Switch(checked = isDivided, onCheckedChange = { isDivided = it; pocketChange = false }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, uncheckedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary, uncheckedTrackColor = Color.Gray, uncheckedBorderColor = Color.Transparent)); Spacer(modifier = Modifier.width(8.dp)); Text(if (isDivided) "Pago Dividido Múltiple" else "Pago Único", fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!isDivided) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) { FilterChip(selected = simpleMethod == "Efectivo", onClick = { simpleMethod = "Efectivo" }, label = { Text("Efectivo") }); FilterChip(selected = simpleMethod == "Digital", onClick = { simpleMethod = "Digital" }, label = { Text("Digital") }) }
                        PaymentInputRow("Monto Recibido", simpleReceivedRaw, sym, visualTrans, selectedCountry) { simpleReceivedRaw = it }

                        val rec = (simpleReceivedRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier
                        val change = rec - totalCOP

                        if (change > 0) {
                            Text("Vuelto a devolver: ${formatMoneyMain(change, selectedCountry)} ${formatMoneySec(change, selectedCountry, bcvRate)}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { pocketChange = !pocketChange }) { Checkbox(checked = pocketChange, onCheckedChange = { pocketChange = it }); Text("Sacar vuelto de mi bolsillo", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                            if (pocketChange) { Text("El vuelto se dará de tu bolsillo personal. La ganancia ingresará completa a la tienda y la tienda te deberá el vuelto.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp)) } else { val hasEnoughFunds = if (simpleMethod == "Efectivo") change <= totalStoreCash else change <= totalStoreDigital; if (!hasEnoughFunds) { Text("⚠️ Fondos insuficientes en ${if(simpleMethod == "Efectivo") "Caja" else "Banco"}. La caja quedará en negativo.", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp) } else { Text("El vuelto saldrá de: $simpleMethod", fontWeight = FontWeight.Bold, color = Color(0xFF2196F3), fontSize = 12.sp) } }
                        } else if (change < 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { if (rec > 0) { Text("Falta: ${formatMoneyMain(abs(change), selectedCountry)} ${formatMoneySec(abs(change), selectedCountry, bcvRate)}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp) } else { Text("Cobro pendiente", color = Color.Gray, fontSize = 14.sp) }; Spacer(modifier = Modifier.height(12.dp)); Button(onClick = { val cash = if (simpleMethod == "Efectivo") rec else 0.0; val digital = if (simpleMethod == "Digital") rec else 0.0; onFiarVenta(buyerName, cash, digital) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D), contentColor = Color.Black)) { Text(if (rec > 0) "Fiar el restante \uD83D\uDDD3️" else "Fiar esta venta \uD83D\uDDD3️", fontWeight = FontWeight.Bold) } }
                        } else if (change == 0.0 && rec > 0) { Text("Pago exacto ✅", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold) }
                    } else {
                        PaymentInputRow("Efectivo Recibido", cashRaw, sym, visualTrans, selectedCountry) { cashRaw = it }
                        PaymentInputRow("Digital Recibido", digitalRaw, sym, visualTrans, selectedCountry) { digitalRaw = it }
                        val cV = (cashRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier
                        val qV = (digitalRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier
                        val receivedCOP = cV + qV
                        val changeCOP = receivedCOP - totalCOP

                        if (changeCOP > 0) {
                            Text("Vuelto a devolver: ${formatMoneyMain(changeCOP, selectedCountry)} ${formatMoneySec(changeCOP, selectedCountry, bcvRate)}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { pocketChange = !pocketChange }) { Checkbox(checked = pocketChange, onCheckedChange = { pocketChange = it }); Text("Sacar vuelto de mi bolsillo", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                            if (pocketChange) { Text("El vuelto se dará de tu bolsillo. La ganancia ingresará intacta y la tienda te deberá el vuelto.", fontSize = 12.sp, color = Color.Gray) } else { Text("¿De dónde darás el vuelto?", fontSize = 12.sp, color = Color.Gray); PaymentInputRow("Vuelto Efectivo", cashChangeRaw, sym, visualTrans, selectedCountry) { cashChangeRaw = it }; PaymentInputRow("Vuelto Digital", digitalChangeRaw, sym, visualTrans, selectedCountry) { digitalChangeRaw = it }; val cc = (cashChangeRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; val dc = (digitalChangeRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; if (cc + dc != changeCOP) { Text("La suma del vuelto no cuadra con ${formatMoneyMain(changeCOP, selectedCountry)}", color = Color.Red, fontSize = 11.sp) } else if (cc > totalStoreCash) { Text("⚠️ Caja quedará en negativo al dar el vuelto.", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold) } else if (dc > totalStoreDigital) { Text("⚠️ Banco quedará en negativo al dar el vuelto.", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold) } else { Text("Vuelto distribuido correctamente ✅", color = Color(0xFF2196F3), fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
                        } else if (changeCOP < 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { if (receivedCOP > 0) { Text("Falta dinero: ${formatMoneyMain(abs(changeCOP), selectedCountry)} ${formatMoneySec(abs(changeCOP), selectedCountry, bcvRate)}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp) } else { Text("Cobro pendiente", color = Color.Gray, fontSize = 14.sp) }; Spacer(modifier = Modifier.height(12.dp)); Button(onClick = { onFiarVenta(buyerName, cV, qV) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D), contentColor = Color.Black)) { Text(if (receivedCOP > 0) "Fiar el restante \uD83D\uDDD3️" else "Fiar esta venta \uD83D\uDDD3️", fontWeight = FontWeight.Bold) } }
                        } else if (changeCOP == 0.0 && receivedCOP > 0) { Text("Pago completo y exacto ✅", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                }
            }
        },
        confirmButton = {
            if (step == 1 && !showProductSearch) {
                Button(onClick = { step = 2 }, enabled = cartItems.isNotEmpty()) { Text("Siguiente") }
            } else if (step == 2) {
                var isEnabled = false
                if (!isDivided) { val rec = (simpleReceivedRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; if (rec >= totalCOP) { isEnabled = true } } else { val cV = (cashRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; val dV = (digitalRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; val changeCOP = (cV + dV) - totalCOP; if (changeCOP == 0.0) isEnabled = true; if (changeCOP > 0.0) { if (pocketChange) { isEnabled = true } else { val cc = (cashChangeRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; val dc = (digitalChangeRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; if (abs(cc + dc - changeCOP) < 0.01) isEnabled = true } } }
                Button(onClick = { var netC = 0.0; var netD = 0.0; var summary = ""; var pocketDebtAmount = 0.0; if (!isDivided) { val rec = (simpleReceivedRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; val change = rec - totalCOP; if (simpleMethod == "Efectivo") { netC = totalCOP; summary = "Pago Efectivo: ${formatMoneyMain(rec, selectedCountry)}" + if(change>0) " | Vuelto: ${formatMoneyMain(change, selectedCountry)}" + if(pocketChange) " (De bolsillo)" else "" else "" } else { netD = totalCOP; summary = "Pago Digital: ${formatMoneyMain(rec, selectedCountry)}" + if(change>0) " | Vuelto: ${formatMoneyMain(change, selectedCountry)}" + if(pocketChange) " (De bolsillo)" else "" else "" }; if (pocketChange && change > 0) { pocketDebtAmount = change } } else { val cV = (cashRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; val dV = (digitalRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; val changeCOP = (cV + dV) - totalCOP; if (pocketChange) { netC = cV; netD = dV; summary = "Efectivo recibido: ${formatMoneyMain(cV, selectedCountry)} | Digital recibido: ${formatMoneyMain(dV, selectedCountry)}"; if (changeCOP > 0) { summary += "\nVuelto: ${formatMoneyMain(changeCOP, selectedCountry)} (De bolsillo)"; pocketDebtAmount = changeCOP } } else { val cc = (cashChangeRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; val dc = (digitalChangeRaw.toDoubleOrNull() ?: 0.0) * inputMultiplier; netC = cV - cc; netD = dV - dc; summary = "Efectivo recibido: ${formatMoneyMain(cV, selectedCountry)} | Digital recibido: ${formatMoneyMain(dV, selectedCountry)}"; if (cc > 0 || dc > 0) { summary += "\nVuelto Efectivo: ${formatMoneyMain(cc, selectedCountry)} | Vuelto Digital: ${formatMoneyMain(dc, selectedCountry)}" } } }; onConfirmSale(cartItems.toList(), buyerName.trim(), summary, netC, netD, pocketDebtAmount) }, enabled = isEnabled) { Text("Confirmar Venta") }
            }
        },
        dismissButton = {
            if (showProductSearch) { TextButton(onClick = { showProductSearch = false }) { Text("Volver al carrito") } } else if (step == 2) { TextButton(onClick = { step = 1 }) { Text("Atrás") } }
        }
    )
}
