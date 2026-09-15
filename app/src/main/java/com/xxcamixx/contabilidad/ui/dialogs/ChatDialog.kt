package com.xxcamixx.contabilidad.ui.dialogs

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xxcamixx.contabilidad.model.ChatMessage
import com.xxcamixx.contabilidad.model.ChatSendRequest
import com.xxcamixx.contabilidad.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminChatListDialog(onDismiss: () -> Unit, onSelectClient: (String) -> Unit) {
    var chats by remember { mutableStateOf<Map<String, List<ChatMessage>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try { chats = RetrofitInstance.api.getAllChats() } catch (_: Exception) {}
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Mensajes de Clientes \uD83D\uDCAC", fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface,
        text = {
            if (isLoading) { CircularProgressIndicator() }
            else if (chats.isEmpty()) { Text("No hay mensajes de clientes por ahora.", color = Color.Gray) }
            else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(chats.keys.toList()) { clientEmail ->
                        val lastMsg = chats[clientEmail]?.lastOrNull()
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelectClient(clientEmail) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(clientEmail, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (lastMsg != null) {
                                    Text("${if(lastMsg.sender=="zonacami77777@gmail.com") "Tú:" else "Cliente:"} ${lastMsg.text}", maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Gray, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }, properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDialog(currentUserEmail: String, targetClientEmail: String, isAdmin: Boolean, onDismiss: () -> Unit) {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    var showClearConfirm by remember { mutableStateOf(false) }

    // Estados para las imágenes del chat
    var imageUriToSend by remember { mutableStateOf<String?>(null) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var expandedImageUri by remember { mutableStateOf<String?>(null) }
    var showAttachmentOptions by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            isProcessingImage = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val scale = kotlin.math.min(800f / bitmap.width, 800f / bitmap.height)
                        val resized = android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                        val outputStream = java.io.ByteArrayOutputStream()
                        resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
                        val b64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.DEFAULT)
                        launch(Dispatchers.Main) { imageUriToSend = "data:image/jpeg;base64,$b64" }
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
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val b64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.DEFAULT)
                    launch(Dispatchers.Main) { imageUriToSend = "data:image/jpeg;base64,$b64" }
                } catch (e: Exception) {}
                launch(Dispatchers.Main) { isProcessingImage = false }
            }
        }
    }

    LaunchedEffect(targetClientEmail) {
        while (true) {
            try {
                val newMsgs = RetrofitInstance.api.getChat(targetClientEmail)
                if (newMsgs != messages) {
                    messages = newMsgs
                    if (messages.isNotEmpty()) { listState.animateScrollToItem(messages.size - 1) }
                }
            } catch (_: Exception) {}
            delay(3000L)
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false }, title = { Text("Borrar Chat \uD83D\uDDD1️", fontWeight = FontWeight.Bold) }, containerColor = MaterialTheme.colorScheme.surface,
            text = { Text("¿Estás seguro de que deseas borrar este historial de chat? Esta acción es irreversible para ambas partes.") },
            confirmButton = { Button(onClick = { coroutineScope.launch(Dispatchers.IO) { try { RetrofitInstance.api.clearChat(targetClientEmail); launch(Dispatchers.Main) { messages = emptyList(); showClearConfirm = false } } catch(_: Exception){} } }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Cancelar") } }
        )
    }

    if (showAttachmentOptions) {
        AlertDialog(
            onDismissRequest = { showAttachmentOptions = false },
            title = { Text("Adjuntar Imagen \uD83D\uDCF8", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showAttachmentOptions = false; cameraLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Cámara")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { showAttachmentOptions = false; galleryLauncher.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Image, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Galería")
                    }
                }
            },
            confirmButton = {}, dismissButton = { TextButton(onClick = { showAttachmentOptions = false }) { Text("Cancelar") } }
        )
    }

    if (expandedImageUri != null) {
        ExpandedImageDialog(imageUri = expandedImageUri!!, onDismiss = { expandedImageUri = null })
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) }
                    Text(if (isAdmin) "Chat: ${targetClientEmail.substringBefore("@")}" else "Soporte Técnico \uD83D\uDCAC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { showClearConfirm = true }) { Icon(Icons.Filled.Delete, "Borrar Chat", tint = Color.White) }
                }

                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), state = listState) {
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    if (messages.isEmpty()) { item { Text("No hay mensajes todavía. ¡Escribe algo para empezar!", color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) } }
                    items(messages) { msg ->
                        val isMe = msg.sender == currentUserEmail
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start) {
                            Box(modifier = Modifier.background(if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isMe) 16.dp else 4.dp, bottomEnd = if (isMe) 4.dp else 16.dp)).padding(12.dp).widthIn(max = 280.dp)) {
                                Column {
                                    if (msg.imageUrl != null) {
                                        var bitmap by remember(msg.imageUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
                                        LaunchedEffect(msg.imageUrl) {
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                try {
                                                    val b64 = if (msg.imageUrl.contains(",")) msg.imageUrl.split(",")[1] else msg.imageUrl
                                                    val decoded = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                                    bitmap = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                                } catch (e: Exception) {}
                                            }
                                        }
                                        if (bitmap != null) {
                                            Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.height(150.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { expandedImageUri = msg.imageUrl }.padding(bottom = 8.dp), contentScale = ContentScale.Crop)
                                        }
                                    }
                                    if (msg.text.isNotBlank()) {
                                        Text(msg.text, color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                                    }
                                    Text(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp)), fontSize = 10.sp, color = (if (isMe) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.6f), modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                    if (isProcessingImage) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    if (imageUriToSend != null) {
                        Box(modifier = Modifier.padding(start = 16.dp, top = 8.dp).size(80.dp).clip(RoundedCornerShape(8.dp))) {
                            var previewBitmap by remember(imageUriToSend) { mutableStateOf<android.graphics.Bitmap?>(null) }
                            LaunchedEffect(imageUriToSend) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val b64 = imageUriToSend!!.substringAfter(",")
                                        val decoded = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                        previewBitmap = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                    } catch (e: Exception) {}
                                }
                            }
                            if (previewBitmap != null) {
                                Image(bitmap = previewBitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            IconButton(onClick = { imageUriToSend = null }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(0.5f), CircleShape)) {
                                Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showAttachmentOptions = true }) { Icon(Icons.Filled.AttachFile, null, tint = MaterialTheme.colorScheme.primary) }
                        OutlinedTextField(
                            value = inputText, onValueChange = { inputText = it }, modifier = Modifier.weight(1f),
                            placeholder = { Text("Escribe un mensaje...") }, shape = RoundedCornerShape(24.dp), maxLines = 4
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(onClick = {
                            if (inputText.isNotBlank() || imageUriToSend != null) {
                                focusManager.clearFocus()
                                val textToSend = inputText; inputText = ""
                                val imgToSend = imageUriToSend; imageUriToSend = null
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val receiver = if (isAdmin) targetClientEmail else "zonacami77777@gmail.com"
                                        RetrofitInstance.api.sendMessage(ChatSendRequest(currentUserEmail, receiver, textToSend, imgToSend))
                                        val updated = RetrofitInstance.api.getChat(targetClientEmail)
                                        launch(Dispatchers.Main) { messages = updated; if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }
                                    } catch (_: Exception) {}
                                }
                            }
                        }, containerColor = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) { Icon(Icons.Filled.Send, null, tint = Color.White) }
                    }
                }
            }
        }
    }
}
