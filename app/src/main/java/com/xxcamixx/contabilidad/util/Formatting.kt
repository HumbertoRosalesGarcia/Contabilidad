package com.xxcamixx.contabilidad.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatBs(amount: Double): String {
    val format = DecimalFormat("#,##0.00").apply { decimalFormatSymbols = decimalFormatSymbols.apply { groupingSeparator = '.'; decimalSeparator = ',' } }
    return "Bs ${format.format(amount)}"
}

fun formatUSD(amount: Double): String {
    val format = DecimalFormat("#,##0.00").apply { decimalFormatSymbols = decimalFormatSymbols.apply { groupingSeparator = ','; decimalSeparator = '.' } }
    return "$${format.format(amount)}"
}

fun formatMoneyMain(amount: Double, country: String): String {
    return if (country == "Venezuela") formatUSD(amount) else formatCOP(amount)
}

fun formatMoneySec(amount: Double, country: String, bcvRate: Double): String {
    return if (country == "Venezuela" && bcvRate > 0) "= ${formatBs(amount * bcvRate)}" else ""
}

fun cleanDecimalInput(input: String): String {
    var dotCount = 0
    return input.replace(',', '.').filter {
        if (it == '.') {
            dotCount++
            dotCount <= 1
        } else {
            it.isDigit()
        }
    }
}

fun cleanDecimalWithPrecision(input: String, maxDecimals: Int = 2): String {
    val cleaned = cleanDecimalInput(input)
    if (!cleaned.contains('.')) return cleaned
    val parts = cleaned.split('.')
    val decimals = if (parts.size > 1) parts[1].take(maxDecimals) else ""
    return "${parts[0]}.$decimals"
}

fun cleanAmountInput(input: String): String { return input.filter { it.isDigit() } }

class AmountVisualTransformation(val prefix: String = "$ ") : VisualTransformation { 
    override fun filter(text: AnnotatedString): TransformedText { 
        val inputText = text.text
        val formattedInt = if (inputText.isNotEmpty()) { 
            var result = ""
            val reversed = inputText.reversed()
            for (i in reversed.indices) { 
                result += reversed[i]
                if ((i + 1) % 3 == 0 && i != reversed.lastIndex) { result += "." } 
            }
            prefix + result.reversed() 
        } else ""

        val offsetMapping = object : OffsetMapping { 
            override fun originalToTransformed(offset: Int): Int { 
                if (inputText.isEmpty()) return 0
                var transformedCursor = prefix.length
                var originalCursor = 0
                while (originalCursor < offset && originalCursor < inputText.length) { 
                    transformedCursor++
                    originalCursor++
                    val remaining = inputText.length - originalCursor
                    if (remaining > 0 && remaining % 3 == 0) transformedCursor++ 
                }
                return transformedCursor.coerceIn(0, formattedInt.length) 
            }

            override fun transformedToOriginal(offset: Int): Int { 
                if (inputText.isEmpty() || offset <= prefix.length) return 0
                var originalOffset = 0
                var transformedIndex = prefix.length
                while (transformedIndex < offset && originalOffset < inputText.length) { 
                    if (formattedInt[transformedIndex] == '.') { 
                        transformedIndex++ 
                    } else { 
                        originalOffset++
                        transformedIndex++ 
                    } 
                }
                return originalOffset.coerceIn(0, inputText.length) 
            } 
        }
        return TransformedText(AnnotatedString(formattedInt), offsetMapping) 
    } 
}

fun formatCOP(amount: Double): String { 
    val format = DecimalFormat("#,###").apply { decimalFormatSymbols = decimalFormatSymbols.apply { groupingSeparator = '.' } }
    return "$${format.format(amount)}" 
}

fun formatDate(timestamp: Long): String { 
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp)) 
}

fun formatDateOnly(timestamp: Long): String { 
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp)) 
}

fun isSameDay(time1: Long, time2: Long): Boolean { 
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) 
}

fun getSmartEmoji(description: String, isIncome: Boolean = true): String {
    val d = description.lowercase(Locale.getDefault())
    return when {
        // --- ZAPATOS Y CALZADO ---
        listOf("zapato", "zapatos", "calzado", "tenis", "zapatilla", "sneaker", "deportivo").any { d.contains(it) } -> {
            when {
                d.contains("tacon") || d.contains("tacón") || d.contains("tacos") -> "👠"
                d.contains("bota") || d.contains("botin") || d.contains("botín") -> "👢"
                d.contains("sandalia") || d.contains("chancleta") || d.contains("crocs") -> "👡"
                d.contains("formal") || d.contains("cuero") || d.contains("elegante") || d.contains("mocasines") -> "👞"
                else -> "👟" // Tenis / Zapatos por defecto
            }
        }
        
        // --- ROPA Y VESTIMENTA ---
        listOf("camisa", "camiseta", "franela", "playera", "remera", "polo").any { d.contains(it) } -> "👕"
        listOf("pantalon", "pantalón", "jeans", "jean", "short", "bermuda").any { d.contains(it) } -> "👖"
        listOf("vestido", "falda", "traje", "bata").any { d.contains(it) } -> "👗"
        listOf("chaqueta", "abrigo", "saco", "sueter", "suéter", "buzo").any { d.contains(it) } -> "🧥"
        listOf("media", "medias", "calcetin", "calcetines").any { d.contains(it) } -> "🧦"
        listOf("gorra", "sombrero", "gorro", "cachucha").any { d.contains(it) } -> "🧢"
        listOf("ropa", "prendas", "textil").any { d.contains(it) } -> "👔"

        // --- ALIMENTOS Y VÍVERES ---
        listOf("papa", "papas", "patata").any { d.contains(it) } -> "🥔"
        listOf("pollo", "pechuga", "alitas", "muslos").any { d.contains(it) } -> "🍗"
        listOf("carne", "res", "lomo", "bistec", "chuleta", "costilla").any { d.contains(it) } -> "🥩"
        listOf("pescado", "pescado", "camaron", "camarón", "mariscos").any { d.contains(it) } -> "🍤"
        listOf("platano", "plátano", "banano", "guineo").any { d.contains(it) } -> "🍌"
        listOf("yuca", "verdura", "legumbre", "hortaliza", "ensalada").any { d.contains(it) } -> "🥗"
        listOf("salchicha", "chorizo", "perro", "salchipapa", "embutido").any { d.contains(it) } -> "🌭"
        listOf("hamburguesa", "kfc", "comida rapida", "comida rápida").any { d.contains(it) } -> "🍔"
        listOf("pizza").any { d.contains(it) } -> "🍕"
        listOf("arroz").any { d.contains(it) } -> "🍚"
        listOf("pan", "harina", "arepa", "panaderia").any { d.contains(it) } -> "🍞"
        listOf("huevo", "huevos").any { d.contains(it) } -> "🥚"
        listOf("queso", "lacteo", "lácteo").any { d.contains(it) } -> "🧀"
        listOf("leche").any { d.contains(it) } -> "🥛"
        listOf("cafe", "café", "tinto").any { d.contains(it) } -> "☕"
        listOf("gaseosa", "refresco", "jugo", "bebida", "soda").any { d.contains(it) } -> "🥤"
        listOf("cerveza", "licor", "trago").any { d.contains(it) } -> "🍺"
        listOf("comida", "almuerzo", "cena", "desayuno", "restaurante").any { d.contains(it) } -> "🍽️"

        // --- MERCADO, COMERCIO Y FINANZAS ---
        listOf("mercado", "supermercado", "tienda", "viveres", "víveres", "abarrotes").any { d.contains(it) } -> "🛒"
        listOf("transporte", "pasaje", "bus", "taxi", "uber", "gasolina").any { d.contains(it) } -> "🚕"
        listOf("servicios", "luz", "agua", "internet", "factura").any { d.contains(it) } -> "💡"
        listOf("casa", "arriendo", "hogar", "alquiler").any { d.contains(it) } -> "🏠"
        listOf("regalo", "cumpleaños", "cumpleanos", "fiesta").any { d.contains(it) } -> "🎁"
        listOf("medico", "médico", "salud", "farmacia", "pastillas", "droga").any { d.contains(it) } -> "💊"
        listOf("salario", "sueldo", "pago", "nomina", "nómina").any { d.contains(it) } -> "💵"
        listOf("negocio", "venta", "cliente", "producto").any { d.contains(it) } -> "📦"
        listOf("ahorro", "banco", "intereses", "nequi", "bancolombia", "daviplata").any { d.contains(it) } -> "🏦"
        
        // --- TECNOLOGÍA ---
        listOf("celular", "telefono", "teléfono", "iphone", "samsung", "xiaomi").any { d.contains(it) } -> "📱"
        listOf("computador", "laptop", "pc").any { d.contains(it) } -> "💻"

        else -> if (isIncome) "📦" else "💸"
    }
}

fun formatQty(qty: Double): String = if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()

fun uriToBase64(context: android.content.Context, uriString: String?): String? {
    if (uriString.isNullOrBlank()) return null
    if (uriString.startsWith("data:image")) return uriString
    return try {
        val uri = android.net.Uri.parse(uriString)
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream) ?: return null
        val maxDim = 800f
        val scale = kotlin.math.min(1f, kotlin.math.min(maxDim / bitmap.width, maxDim / bitmap.height))
        val resized = if (scale < 1f) {
            android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else bitmap
        val outputStream = java.io.ByteArrayOutputStream()
        resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        val b64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
        "data:image/jpeg;base64,$b64"
    } catch (e: Exception) {
        null
    }
}

fun loadBitmapFromUri(context: android.content.Context, uriString: String?): android.graphics.Bitmap? {
    if (uriString.isNullOrBlank()) return null
    return try {
        if (uriString.startsWith("data:image")) {
            val b64 = uriString.substringAfter(",")
            val decoded = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
        } else {
            val uri = android.net.Uri.parse(uriString)
            if (uri.scheme == "file") {
                android.graphics.BitmapFactory.decodeFile(uri.path)
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        }
    } catch (e: Exception) {
        null
    }
}

fun saveImageToInternalStorage(context: android.content.Context, uri: android.net.Uri): String {
    return try {
        try {
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {}

        val imagesDir = java.io.File(context.filesDir, "product_images").apply { if (!exists()) mkdirs() }
        val destFile = java.io.File(imagesDir, "prod_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        android.net.Uri.fromFile(destFile).toString()
    } catch (e: Exception) {
        uri.toString()
    }
}
