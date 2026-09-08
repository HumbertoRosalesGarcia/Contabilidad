package com.xxcamixx.contabilidad.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xxcamixx.contabilidad.model.Product
import java.util.Locale

class ProductDraftState {
    var name by mutableStateOf("")
    var purchasePriceRaw by mutableStateOf("")
    var priceRaw by mutableStateOf("")
    var stockRaw by mutableStateOf("")
    var minStockRaw by mutableStateOf("")
    var selectedUnit by mutableStateOf("Uds")
    var hasExpiry by mutableStateOf(false)
    var expiryDateMillis by mutableStateOf<Long?>(null)
    var imageUri by mutableStateOf<String?>(null)

    fun clear() {
        name = ""
        purchasePriceRaw = ""
        priceRaw = ""
        stockRaw = ""
        minStockRaw = ""
        selectedUnit = "Uds"
        hasExpiry = false
        expiryDateMillis = null
        imageUri = null
    }

    fun loadFrom(product: Product) {
        name = product.name
        val pCost = product.purchasePrice
        purchasePriceRaw = if (pCost > 0) {
            if (pCost % 1.0 == 0.0) pCost.toLong().toString() else {
                val df = java.text.DecimalFormat("#.######", java.text.DecimalFormatSymbols(Locale.US))
                df.format(pCost)
            }
        } else ""

        val pPrice = product.price
        priceRaw = if (pPrice > 0) {
            if (pPrice % 1.0 == 0.0) pPrice.toLong().toString() else {
                val df = java.text.DecimalFormat("#.######", java.text.DecimalFormatSymbols(Locale.US))
                df.format(pPrice)
            }
        } else ""

        stockRaw = product.stock.toString()
        minStockRaw = if (product.minStock > 0) product.minStock.toString() else ""
        selectedUnit = product.unit
        hasExpiry = product.expirationDateInMillis != null
        expiryDateMillis = product.expirationDateInMillis
        imageUri = product.imageUri
    }
}
