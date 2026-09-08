package com.xxcamixx.contabilidad.ui.components

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xxcamixx.contabilidad.model.Transaction
import com.xxcamixx.contabilidad.util.formatCOP
import com.xxcamixx.contabilidad.util.formatDate
import com.xxcamixx.contabilidad.util.getSmartEmoji

@Composable
fun TransactionItem(transaction: Transaction, onDelete: () -> Unit, onImageClick: (String) -> Unit) {
    val isIncome = transaction.isIncome
    val color = if (isIncome) Color(0xFF2196F3) else Color(0xFFF44336)
    val emoji = getSmartEmoji(transaction.description, isIncome)
    val context = LocalContext.current

    val methodText = when {
        transaction.cashAmount > 0 && transaction.digitalAmount == 0.0 -> "Efectivo"
        transaction.digitalAmount > 0 && transaction.cashAmount == 0.0 -> "Digital"
        transaction.cashAmount > 0 && transaction.digitalAmount > 0 -> "Mixto"
        else -> ""
    }

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {

            // --- THUMBNAIL O EMOJI ---
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)).clickable { if (transaction.imageUri != null) onImageClick(transaction.imageUri) }, contentAlignment = Alignment.Center) {
                if (transaction.imageUri != null) {
                    var bitmap by remember(transaction.imageUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                    LaunchedEffect(transaction.imageUri) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val uriStr = transaction.imageUri
                                if (uriStr.startsWith("data:image") || uriStr.length > 1000) {
                                    val b64 = uriStr.substringAfter(",")
                                    val decoded = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                    bitmap = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                } else {
                                    val uri = Uri.parse(uriStr)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        bitmap = android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, uri))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                                    }
                                }
                            } catch (e: Exception) {}
                        }
                    }
                    if (bitmap != null) {
                        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = "Recibo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text(text = "🖼️", fontSize = 20.sp)
                    }
                } else {
                    Text(text = emoji, fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // --- COLUMNA DE TEXTOS (Sin límites para no recortar información) ---
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {

                Text(text = transaction.description, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)

                if (transaction.category != null) {
                    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha=0.15f), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                        Text(transaction.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                    }
                }

                val noteAndMethod = buildString {
                    if (transaction.note.isNotBlank()) append(transaction.note)
                    if (methodText.isNotEmpty()) {
                        if (isNotEmpty()) append(" | ")
                        append(methodText)
                    }
                }
                if (noteAndMethod.isNotEmpty()) {
                    Text(text = noteAndMethod, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = formatDate(transaction.timestamp), color = Color.Gray.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    if (transaction.imageUri != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.Image, "Ver Recibo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp).clickable { onImageClick(transaction.imageUri) })
                    }
                }
            }

            // --- COLUMNA DEL MONTO Y BOTÓN ELIMINAR ---
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${if (isIncome) "+" else "-"}${formatCOP(transaction.amount)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).padding(top = 8.dp)) {
                    Icon(Icons.Filled.Delete, "Eliminar", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
