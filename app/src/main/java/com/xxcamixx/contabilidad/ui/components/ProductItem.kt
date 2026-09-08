package com.xxcamixx.contabilidad.ui.components

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xxcamixx.contabilidad.model.Product
import com.xxcamixx.contabilidad.util.formatDateOnly
import com.xxcamixx.contabilidad.util.formatMoneyMain
import com.xxcamixx.contabilidad.util.formatMoneySec

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductItem(
    product: Product,
    selectedCountry: String,
    bcvRate: Double,
    onAddToCart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLongDelete: () -> Unit,
    onInfo: () -> Unit,
    onImageClick: () -> Unit
) {
    val context = LocalContext.current
    val isOutOfStock = product.stock <= 0
    val isLowStock = product.minStock > 0 && product.stock <= product.minStock && !isOutOfStock
    val isExpired = product.expirationDateInMillis != null && product.expirationDateInMillis < System.currentTimeMillis()
    val unitColor = when (product.unit) {
        "Kg" -> Color(0xFFFF9800)
        "L" -> Color(0xFF03A9F4)
        else -> Color(0xFF9C27B0)
    }
    val statusColor = when {
        isExpired -> Color(0xFFD32F2F)
        isOutOfStock -> Color(0xFFE53935)
        isLowStock -> Color(0xFFFF9800)
        else -> unitColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .combinedClickable(onClick = onInfo, onLongClick = onLongDelete),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Banda lateral de estado / unidad
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Foto del producto
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray.copy(alpha = 0.15f))
                        .clickable { if (product.imageUri != null) onImageClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (product.imageUri != null) {
                        val bitmap = remember(product.imageUri) {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    android.graphics.ImageDecoder.decodeBitmap(
                                        android.graphics.ImageDecoder.createSource(
                                            context.contentResolver,
                                            Uri.parse(product.imageUri)
                                        )
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    android.provider.MediaStore.Images.Media.getBitmap(
                                        context.contentResolver,
                                        Uri.parse(product.imageUri)
                                    )
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Imagen de ${product.name}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Filled.ImageNotSupported, contentDescription = null, tint = Color.Gray)
                        }
                    } else {
                        Icon(Icons.Filled.Inventory, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Información central del producto
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Badges de alerta si corresponde
                    if (isExpired || isOutOfStock || isLowStock) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isExpired) {
                                Surface(
                                    color = Color(0xFFD32F2F).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "⚠️ VENCIDO",
                                        color = Color(0xFFD32F2F),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (isOutOfStock) {
                                Surface(
                                    color = Color.Red.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "AGOTADO",
                                        color = Color.Red,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            } else if (isLowStock) {
                                Surface(
                                    color = Color(0xFFFF9800).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "⚠️ POCO STOCK",
                                        color = Color(0xFFE65100),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    val unitName = when (product.unit) {
                        "Kg" -> "Kilo"
                        "L" -> "Litro"
                        else -> "Unidad"
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Precio: ", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            formatMoneyMain(product.price, selectedCountry),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val sec = formatMoneySec(product.price, selectedCountry, bcvRate)
                        if (sec.isNotEmpty()) {
                            Text(" $sec", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Stock: ", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            "${product.stock} ${product.unit}",
                            color = if (isOutOfStock) Color.Red else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (product.expirationDateInMillis != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Vence: ", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                formatDateOnly(product.expirationDateInMillis),
                                color = if (isExpired) Color.Red else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = if (isExpired) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Columna derecha: [Añadir] y fila simétrica [Editar] [Eliminar]
                Column(
                    modifier = Modifier.width(IntrinsicSize.Min),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAddToCart,
                        enabled = !isOutOfStock,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                    ) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Añadir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .clickable { onEdit() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Editar",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Red.copy(alpha = 0.12f))
                                .clickable { if (product.stock <= 0) onLongDelete() else onDelete() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Borrar",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
