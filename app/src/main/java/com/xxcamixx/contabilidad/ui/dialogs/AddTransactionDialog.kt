package com.xxcamixx.contabilidad.ui.dialogs

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.xxcamixx.contabilidad.util.AmountVisualTransformation
import com.xxcamixx.contabilidad.util.cleanAmountInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(categories: List<String>, onDismiss: () -> Unit, onConfirm: (String, Double, Boolean, String, String, String?, String?) -> Unit) {
    var isIncome by remember { mutableStateOf(true) }
    var descIncome by remember { mutableStateOf("") }
    var amountIncome by remember { mutableStateOf("") }
    var descExpense by remember { mutableStateOf("") }
    var amountExpense by remember { mutableStateOf("") }
    var noteExpense by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Efectivo") }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var expandedCategory by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var isProcessingImage by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            isProcessingImage = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val scale = kotlin.math.min(1200f / bitmap.width, 1200f / bitmap.height)
                        val resized = android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                        val outputStream = java.io.ByteArrayOutputStream()
                        resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
                        val b64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.DEFAULT)
                        launch(Dispatchers.Main) { imageUri = "data:image/jpeg;base64,$b64" }
                    }
                } catch (e: Exception) {}
                launch(Dispatchers.Main) { isProcessingImage = false }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            isProcessingImage = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val outputStream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, outputStream)
                    val b64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.DEFAULT)
                    launch(Dispatchers.Main) { imageUri = "data:image/jpeg;base64,$b64" }
                } catch (e: Exception) {}
                launch(Dispatchers.Main) { isProcessingImage = false }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Nuevo Movimiento ✍️", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(4.dp)) {
                    Button(onClick = { isIncome = true }, colors = ButtonDefaults.buttonColors(containerColor = if (isIncome) Color(0xFF2196F3) else Color.Transparent, contentColor = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurface), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f), elevation = null) { Text("Ingresos \uD83D\uDC5D", fontWeight = FontWeight.Bold) }
                    Button(onClick = { isIncome = false }, colors = ButtonDefaults.buttonColors(containerColor = if (!isIncome) Color(0xFFF44336) else Color.Transparent, contentColor = if (!isIncome) Color.White else MaterialTheme.colorScheme.onSurface), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f), elevation = null) { Text("Gastos \uD83D\uDC5B", fontWeight = FontWeight.Bold) }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("Método de Pago", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    FilterChip(selected = method == "Efectivo", onClick = { method = "Efectivo" }, label = { Text("Efectivo") })
                    FilterChip(selected = method == "Digital", onClick = { method = "Digital" }, label = { Text("Digital") })
                }
                Spacer(modifier = Modifier.height(12.dp))

                Crossfade(targetState = isIncome, label = "") { showIncome ->
                    Column {
                        if (showIncome) {
                            OutlinedTextField(value = descIncome, onValueChange = { input -> descIncome = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = amountIncome, onValueChange = { amountIncome = cleanAmountInput(it) }, label = { Text("Monto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = AmountVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                        } else {
                            OutlinedTextField(value = descExpense, onValueChange = { input -> descExpense = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = amountExpense, onValueChange = { amountExpense = cleanAmountInput(it) }, label = { Text("Monto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = AmountVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(12.dp))

                            Box {
                                OutlinedButton(onClick = { expandedCategory = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(selectedCategory ?: "🔽 Categoría (Opcional)", color = MaterialTheme.colorScheme.onSurface)
                                }
                                DropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(text = { Text(cat) }, onClick = { selectedCategory = cat; expandedCategory = false })
                                    }
                                    if(categories.isEmpty()) {
                                        DropdownMenuItem(text = { Text("Sin categorías creadas", color = Color.Gray) }, onClick = { expandedCategory = false })
                                    }
                                    Divider()
                                    DropdownMenuItem(text = { Text("❌ Ninguna") }, onClick = { selectedCategory = null; expandedCategory = false })
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(value = noteExpense, onValueChange = { input -> noteExpense = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }, label = { Text("Nota (Opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences))
                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Adjuntar Recibo (Opcional)", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))

                            if (isProcessingImage) {
                                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                            } else if (imageUri != null) {
                                Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha=0.1f))) {
                                    var bitmap by remember(imageUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                                    LaunchedEffect(imageUri) {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            try {
                                                val b64 = if (imageUri!!.contains(",")) imageUri!!.split(",")[1] else imageUri!!
                                                val decoded = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                                bitmap = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                            } catch (e: Exception) {}
                                        }
                                    }

                                    if (bitmap != null) {
                                        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = "Recibo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
                                    }

                                    IconButton(onClick = { imageUri = null }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha=0.6f), CircleShape).size(24.dp)) {
                                        Icon(Icons.Filled.Close, "Quitar", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    OutlinedButton(onClick = { cameraLauncher.launch(null) }) { Icon(Icons.Filled.CameraAlt, "Cámara"); Spacer(Modifier.width(4.dp)); Text("Cámara") }
                                    OutlinedButton(onClick = { galleryLauncher.launch(arrayOf("image/*")) }) { Icon(Icons.Filled.Image, "Galería"); Spacer(Modifier.width(4.dp)); Text("Galería") }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (isIncome) {
                    val a = amountIncome.toDoubleOrNull();
                    if (descIncome.isNotBlank() && a != null) {
                        onConfirm(descIncome.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, a, true, "", method, null, null)
                    }
                } else {
                    val a = amountExpense.toDoubleOrNull();
                    if (descExpense.isNotBlank() && a != null) {
                        onConfirm(descExpense.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, a, false, noteExpense.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, method, selectedCategory, imageUri)
                    }
                }
            }) { Text("Guardar ✔️") }
        },
        dismissButton = { }
    )
}
