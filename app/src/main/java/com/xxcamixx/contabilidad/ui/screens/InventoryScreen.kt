package com.xxcamixx.contabilidad.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xxcamixx.contabilidad.model.Product
import com.xxcamixx.contabilidad.ui.components.ProductItem
import com.xxcamixx.contabilidad.ui.dialogs.ExpandedImageDialog
import com.xxcamixx.contabilidad.util.formatMoneyMain
import com.xxcamixx.contabilidad.util.formatMoneySec
import java.util.Locale

@Composable
fun InventoryScreen(
    products: List<Product>,
    shoppingCart: List<Pair<Product, Int>>,
    selectedCountry: String,
    bcvRate: Double,
    categories: List<String>,
    onBack: () -> Unit,
    onAddProductClick: () -> Unit,
    onAddToCartClick: (Product) -> Unit,
    onOpenCheckout: () -> Unit,
    onEditClick: (Product) -> Unit,
    onDeleteClick: (Product) -> Unit,
    onLongDeleteClick: (Product) -> Unit,
    onInfoClick: (Product) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("A-Z") }
    var expandedImageUri by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focusManager.clearFocus() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { focusManager.clearFocus(); onBack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás", tint = MaterialTheme.colorScheme.onPrimary) }
                Text("Inventario \uD83D\uDCE6", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Buscar en el inventario...") }, leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            ScrollableTabRow(selectedTabIndex = listOf("A-Z", "Poco Stock", "Vencimiento", "Precio", "Recientes").indexOf(sortBy), modifier = Modifier.fillMaxWidth(), edgePadding = 16.dp, containerColor = Color.Transparent, divider = {}, indicator = {}) {
                listOf("A-Z", "Poco Stock", "Vencimiento", "Precio", "Recientes").forEach { tab ->
                    FilterChip(selected = sortBy == tab, onClick = { sortBy = tab; focusManager.clearFocus() }, label = { Text(tab) }, modifier = Modifier.padding(end = 8.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(visible = shoppingCart.isNotEmpty()) {
                val totalCart = shoppingCart.sumOf { it.first.price * it.second }; val totalItems = shoppingCart.sumOf { it.second }
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp).clickable { focusManager.clearFocus(); onOpenCheckout() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2)), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Carrito activo ($totalItems artículos)", fontWeight = FontWeight.Bold, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Total: ${formatMoneyMain(totalCart, selectedCountry)}", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                                if (selectedCountry == "Venezuela" && bcvRate > 0) {
                                    Text(" ${formatMoneySec(totalCart, selectedCountry, bcvRate).replace("=", "-")}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Cobrar", tint = Color.White)
                    }
                }
            }

            if (categories.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("Todas") }
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            val sortedProducts = remember(products, searchQuery, sortBy, selectedCategory) {
                val filtered = products.filter { it.name.contains(searchQuery, ignoreCase = true) && (selectedCategory == null || it.category == selectedCategory) }
                when (sortBy) {
                    "A-Z" -> filtered.sortedBy { it.name.lowercase(Locale.getDefault()) }
                    "Precio" -> filtered.sortedByDescending { it.price }
                    "Vencimiento" -> filtered.sortedWith(compareBy<Product> { it.expirationDateInMillis == null }.thenBy { it.expirationDateInMillis })
                    "Recientes" -> filtered.sortedByDescending { it.entryDateInMillis }
                    "Poco Stock" -> filtered.filter { it.minStock > 0 && it.stock <= it.minStock }.sortedBy { it.stock }
                    else -> filtered
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                if (sortedProducts.isEmpty()) { item { Text("No se encontraron productos.", color = Color.Gray, modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center) } }
                items(sortedProducts, key = { it.id }) { product ->
                    ProductItem(
                        product = product,
                        selectedCountry = selectedCountry,
                        bcvRate = bcvRate,
                        onAddToCart = { focusManager.clearFocus(); onAddToCartClick(product) },
                        onEdit = { focusManager.clearFocus(); onEditClick(product) },
                        onDelete = { focusManager.clearFocus(); onDeleteClick(product) },
                        onLongDelete = { focusManager.clearFocus(); onLongDeleteClick(product) },
                        onInfo = { focusManager.clearFocus(); onInfoClick(product) },
                        onImageClick = { focusManager.clearFocus(); expandedImageUri = product.imageUri }
                    )
                }
            }
        }
        FloatingActionButton(onClick = { focusManager.clearFocus(); onAddProductClick() }, containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 16.dp)) { Icon(Icons.Filled.Add, contentDescription = "Agregar Producto") }

        if (expandedImageUri != null) { ExpandedImageDialog(imageUri = expandedImageUri!!, onDismiss = { expandedImageUri = null }) }
    }
}
