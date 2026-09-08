package com.xxcamixx.contabilidad.ui.dialogs

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.xxcamixx.contabilidad.util.formatDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ProfileDialog(
    currentName: String,
    currentRole: String,
    consumedSecs: Long,
    planDurationSecs: Long,
    userId: String,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onImageChange: (String) -> Unit,
    onViewImage: (String) -> Unit, // <--- ESTE ERA EL PARÁMETRO QUE FALTABA
    onUpgradeClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("GlobalAuthPrefs", Context.MODE_PRIVATE)

    var editingName by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(currentName) }
    var profileImageUri by remember { mutableStateOf(prefs.getString("profilePic_$userId", null)) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val scale = kotlin.math.min(400f / bitmap.width, 400f / bitmap.height)
                        val resized = android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                        val outputStream = java.io.ByteArrayOutputStream()
                        resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
                        val b64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.DEFAULT)
                        val uriStr = "data:image/jpeg;base64,$b64"

                        prefs.edit().putString("profilePic_$userId", uriStr).apply()
                        launch(Dispatchers.Main) {
                            profileImageUri = uriStr
                            onImageChange(uriStr)
                        }
                    }
                } catch (e: Exception) {}
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val outputStream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val b64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.DEFAULT)
                    val uriStr = "data:image/jpeg;base64,$b64"

                    prefs.edit().putString("profilePic_$userId", uriStr).apply()
                    launch(Dispatchers.Main) {
                        profileImageUri = uriStr
                        onImageChange(uriStr)
                    }
                } catch (e: Exception) {}
            }
        }
    }

    val crownEmoji = when (currentRole) { "INVITADO", "INVITADO_PRUEBA" -> "🪵"; "PRUEBA", "Invitado-Gold" -> "⏳"; "BÁSICO" -> "🥉"; "PREMIUM" -> "🥈"; "GOLD" -> "🥇"; "ADMIN" -> "👑"; else -> "🪵" }

    val startMillis = System.currentTimeMillis() - (consumedSecs * 1000L)
    val endMillis = startMillis + (planDurationSecs * 1000L)

    val remainingSecs = maxOf(0L, planDurationSecs - consumedSecs)
    val rDays = remainingSecs / 86400
    val rHours = (remainingSecs % 86400) / 3600
    val rMins = (remainingSecs % 3600) / 60
    val rSecs = remainingSecs % 60
    val timeRemainingStr = "${rDays}d ${rHours}h ${rMins}m ${rSecs}s"

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Foto de Perfil 📸", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (profileImageUri != null) {
                        OutlinedButton(onClick = { showImageSourceDialog = false; onViewImage(profileImageUri!!) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Visibility, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Ver Foto Grande")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedButton(onClick = { showImageSourceDialog = false; cameraLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Tomar con la Cámara")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { showImageSourceDialog = false; galleryLauncher.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Explorador de Archivos")
                    }
                }
            },
            confirmButton = {}, dismissButton = { TextButton(onClick = { showImageSourceDialog = false }) { Text("Cancelar") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, "Cerrar") }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { showImageSourceDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    var bitmap by remember(profileImageUri) { mutableStateOf<android.graphics.Bitmap?>(null) }

                    LaunchedEffect(profileImageUri) {
                        if (profileImageUri != null) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val uriStr = profileImageUri!!
                                    val b64 = if (uriStr.contains(",")) uriStr.split(",")[1] else uriStr
                                    val decoded = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                    bitmap = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                } catch (e: Exception) { null }
                            }
                        }
                    }

                    if (bitmap != null) {
                        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = "Foto de perfil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Filled.Add, "Añadir foto", modifier = Modifier.size(40.dp), tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (editingName) {
                    OutlinedTextField(
                        value = nameInput, onValueChange = { nameInput = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { editingName = false; if (nameInput.isNotBlank()) onNameChange(nameInput.trim()) }) { Icon(Icons.Filled.Check, "Guardar", tint = Color(0xFF2196F3)) }
                        }
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { editingName = true }) {
                        Text(currentName, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.Edit, "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = Color.Gray.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    Text("Membresía Actual", fontSize = 12.sp, color = Color.Gray)
                    Text("$currentRole $crownEmoji", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                            Text("Activada el:", fontSize = 12.sp, color = Color.Gray)
                            Text(formatDate(startMillis), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Finaliza el:", fontSize = 12.sp, color = Color.Gray)
                            Text(formatDate(endMillis), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Tiempo Restante:", fontSize = 12.sp, color = Color.Gray)
                    Text(timeRemainingStr, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color(0xFFE65100))

                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentRole != "GOLD" && currentRole != "ADMIN") {
                        Button(onClick = onUpgradeClick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)) {
                            Text("⭐ Mejorar Membresía", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
