package com.xxcamixx.contabilidad.ui.dialogs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.xxcamixx.contabilidad.ai.ImageFeatureExtractor
import com.xxcamixx.contabilidad.model.Product
import com.xxcamixx.contabilidad.util.formatMoneyMain
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun VisualProductScannerDialog(
    products: List<Product>,
    shoppingCart: List<Pair<Product, Int>>,
    selectedCountry: String,
    onAddToCart: (Product, Int) -> Unit,
    onOpenCheckout: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Precalcular vectores de productos para comparaciones ultra-rápidas en memoria
    val productVectors = remember(products) {
        products.mapNotNull { p ->
            val vec = ImageFeatureExtractor.stringToVector(p.featureVector)
            if (vec != null) Pair(p, vec) else null
        }
    }

    // Estado del escaneo en vivo
    var detectedProduct by remember { mutableStateOf<Product?>(null) }
    var matchConfidence by remember { mutableStateOf(0f) }
    var isCooldownActive by remember { mutableStateOf(false) }
    var cooldownProgress by remember { mutableStateOf(0f) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var stockAlert by remember { mutableStateOf<String?>(null) }

    // Estados de Zoom (Alejado por defecto + selector 1x, 2x, 3x)
    var currentZoomRatio by remember { mutableStateOf(1f) }
    var minZoomRatio by remember { mutableStateOf(1f) }
    var maxZoomRatio by remember { mutableStateOf(3f) }

    // Diálogo de confirmación de cantidad tras reconocimiento
    var pendingProductForQty by remember { mutableStateOf<Product?>(null) }
    var pendingConfidence by remember { mutableStateOf(0f) }
    var inputQtyStr by remember { mutableStateOf("1") }
    var lastAddedQty by remember { mutableStateOf(1) }

    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                // Vista de Cámara CameraX
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FIT_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val cameraExecutor = Executors.newSingleThreadExecutor()

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder()
                                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                                .build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (!isCooldownActive && pendingProductForQty == null && productVectors.isNotEmpty()) {
                                    try {
                                        val bitmap = imageProxy.toBitmap()
                                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                                        // Rotar bitmap según orientación
                                        val rotatedBitmap = if (rotationDegrees != 0) {
                                            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                        } else {
                                            bitmap
                                        }

                                        // Extraer vector del frame
                                        val liveVector = ImageFeatureExtractor.extractFeatures(ctx, rotatedBitmap)
                                        if (liveVector != null) {
                                            var bestMatch: Product? = null
                                            var highestSim = 0f

                                            for ((prod, targetVec) in productVectors) {
                                                val sim = ImageFeatureExtractor.cosineSimilarity(liveVector, targetVec)
                                                if (sim > highestSim) {
                                                    highestSim = sim
                                                    bestMatch = prod
                                                }
                                            }

                                            // Umbral de confianza: 78% (0.78f)
                                            if (bestMatch != null && highestSim >= 0.78f) {
                                                val inCart = shoppingCart.filter { it.first.id == bestMatch.id }.sumOf { it.second }
                                                if (inCart >= bestMatch.stock) {
                                                    stockAlert = "Stock agotado para ${bestMatch.name}"
                                                    coroutineScope.launch {
                                                        delay(2000L)
                                                        stockAlert = null
                                                    }
                                                } else {
                                                    // Haptic y pausar para solicitar cantidad al usuario
                                                    try {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                                                        } else {
                                                            @Suppress("DEPRECATION")
                                                            vibrator?.vibrate(100)
                                                        }
                                                    } catch (_: Exception) {}

                                                    pendingProductForQty = bestMatch
                                                    pendingConfidence = highestSim
                                                    inputQtyStr = "1"
                                                }
                                            }
                                        }

                                        if (rotatedBitmap != bitmap) {
                                            rotatedBitmap.recycle()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                imageProxy.close()
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                val cam = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                                cameraControl = cam.cameraControl

                                cam.cameraInfo.zoomState.observe(lifecycleOwner) { zState ->
                                    minZoomRatio = zState.minZoomRatio
                                    maxZoomRatio = zState.maxZoomRatio
                                    currentZoomRatio = zState.zoomRatio
                                }

                                // Iniciar en el plano más amplio y alejado disponible (sin zoom excesivo)
                                val defaultZoom = cam.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
                                cam.cameraControl.setZoomRatio(defaultZoom)
                                currentZoomRatio = defaultZoom
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )

                // Capa de HUD y Visor de Enfoque
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val boxSize = canvasWidth * 0.70f
                        val left = (canvasWidth - boxSize) / 2f
                        val top = (canvasHeight - boxSize) / 2f - 10.dp.toPx()

                        // Oscurecer bordes fuera del visor
                        drawRect(
                            color = Color.Black.copy(alpha = 0.5f),
                            size = size
                        )

                        // Área transparente central (recorte del visor)
                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = Offset(left, top),
                            size = Size(boxSize, boxSize),
                            cornerRadius = CornerRadius(20.dp.toPx()),
                            blendMode = BlendMode.Clear
                        )

                        // Borde de la mira
                        drawRoundRect(
                            color = if (isCooldownActive) Color(0xFF4CAF50) else Color(0xFFFFD700),
                            topLeft = Offset(left, top),
                            size = Size(boxSize, boxSize),
                            cornerRadius = CornerRadius(20.dp.toPx()),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Encabezado Superior (Botón Salir + Linterna + Indicador)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚡ Reconocimiento Visual IA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                            }
                        }

                        IconButton(
                            onClick = {
                                isFlashEnabled = !isFlashEnabled
                                cameraControl?.enableTorch(isFlashEnabled)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                if (isFlashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                contentDescription = "Flash",
                                tint = if (isFlashEnabled) Color(0xFFFFD700) else Color.White
                            )
                        }
                    }

                    // Banner de Instrucción (Claramente posicionado sobre el visor, sin superponerse)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = BorderStroke(
                            1.dp,
                            if (isCooldownActive) Color(0xFF4CAF50).copy(alpha = 0.8f) else Color(0xFFFFD700).copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 72.dp)
                            .padding(horizontal = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isCooldownActive) "¡PRODUCTO AGREGADO! 🎉" else "Apunta la cámara al producto",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCooldownActive) Color(0xFF81C784) else Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${productVectors.size} productos con foto aprendida",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }

                    // Tarjeta de Detección Flotante (Ubicada limpiamente sobre la barra inferior del carrito)
                    AnimatedVisibility(
                        visible = detectedProduct != null,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 124.dp)
                            .padding(horizontal = 20.dp)
                    ) {
                        if (detectedProduct != null) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24).copy(alpha = 0.95f)),
                                border = BorderStroke(1.5.dp, Color(0xFF4CAF50)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("✅", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = detectedProduct!!.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "+$lastAddedQty al Carrito",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF81C784)
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "${(matchConfidence * 100).toInt()}% match",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF81C784),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Barra de Cooldown de 2s para no duplicar
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Pausa para siguiente escaneo", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                                        LinearProgressIndicator(
                                            progress = { cooldownProgress },
                                            modifier = Modifier
                                                .width(100.dp)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = Color(0xFFFFD700),
                                            trackColor = Color(0xFF33333E)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Controles de Zoom en vivo (Alejado por defecto, con botones 1x, 2x, 3x)
                    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                    val boxSizeDp = screenWidth * 0.70f

                    val zoomOptions = remember(minZoomRatio, maxZoomRatio) {
                        val list = mutableListOf<Float>()
                        if (minZoomRatio < 0.95f) {
                            list.add(minZoomRatio)
                        }
                        list.add(1.0f)
                        // Distancias alejadas un 25%:
                        // 2x alejado un 25% -> 1.5x
                        if (maxZoomRatio >= 1.5f) list.add(1.5f)
                        // 3x alejado un 25% -> 2.2x
                        if (maxZoomRatio >= 2.2f) {
                            list.add(2.2f)
                        } else if (maxZoomRatio >= 2.0f) {
                            list.add(2.0f)
                        }
                        list.distinct()
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = boxSizeDp / 2f + 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        zoomOptions.forEach { z ->
                            val isSelected = kotlin.math.abs(currentZoomRatio - z) < 0.15f
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color(0xFFFFD700) else Color.Black.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        currentZoomRatio = z
                                        cameraControl?.setZoomRatio(z)
                                    }
                            ) {
                                Text(
                                    text = if (z < 1f || z % 1f != 0f) String.format(java.util.Locale.US, "%.1fx", z) else "${z.toInt()}x",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Barra Inferior del Carrito
                    val totalCart = shoppingCart.sumOf { it.first.price * it.second }
                    val totalItems = shoppingCart.sumOf { it.second }

                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141418).copy(alpha = 0.95f)),
                        border = BorderStroke(1.dp, Color(0xFF33333E))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Carrito: $totalItems uds", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                }

                                Text(
                                    text = formatMoneyMain(totalCart, selectedCountry),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color(0xFFFFD700)
                                )
                            }

                            if (shoppingCart.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onOpenCheckout()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Ir a Cobrar Carrito 🛒▸", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // Pantalla de Solicitud de Permiso
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Permiso de Cámara Requerido",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "La aplicación necesita acceso a la cámara para escanear y agregar productos automáticamente al carrito con inteligencia artificial local.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                    ) {
                        Text("Conceder Permiso", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            }
        }

        // Modal interactivo de cantidad al reconocer un producto
        if (pendingProductForQty != null) {
            val prod = pendingProductForQty!!
            val inCart = shoppingCart.filter { it.first.id == prod.id }.sumOf { it.second }
            val maxAvailable = maxOf(0, prod.stock - inCart)
            val currentQty = inputQtyStr.toIntOrNull() ?: 0

            AlertDialog(
                onDismissRequest = {
                    pendingProductForQty = null
                    isCooldownActive = true
                    coroutineScope.launch {
                        delay(1500L)
                        isCooldownActive = false
                    }
                },
                properties = DialogProperties(dismissOnClickOutside = false),
                containerColor = Color(0xFF1E1E24),
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("¡Producto Detectado! 🎉", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.25f)
                        ) {
                            Text(
                                "${(pendingConfidence * 100).toInt()}% match",
                                color = Color(0xFF81C784),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = prod.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFFFFD700),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Precio: ${formatMoneyMain(prod.price, selectedCountry)}",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "Stock disponible: $maxAvailable uds",
                            fontSize = 12.sp,
                            color = if (maxAvailable <= 2) Color(0xFFFF5252) else Color.Gray
                        )

                        Spacer(Modifier.height(14.dp))
                        Text("¿Cuántas unidades deseas agregar?", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                        Spacer(Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    val current = inputQtyStr.toIntOrNull() ?: 1
                                    if (current > 1) {
                                        inputQtyStr = (current - 1).toString()
                                    }
                                },
                                enabled = (inputQtyStr.toIntOrNull() ?: 1) > 1,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            ) {
                                Text("−", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(Modifier.width(16.dp))

                            OutlinedTextField(
                                value = inputQtyStr,
                                onValueChange = { str ->
                                    val clean = str.filter { it.isDigit() }
                                    inputQtyStr = clean
                                },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFFFFD700)
                                ),
                                modifier = Modifier.width(90.dp)
                            )

                            Spacer(Modifier.width(16.dp))

                            IconButton(
                                onClick = {
                                    val current = inputQtyStr.toIntOrNull() ?: 1
                                    if (current < maxAvailable) {
                                        inputQtyStr = (current + 1).toString()
                                    }
                                },
                                enabled = (inputQtyStr.toIntOrNull() ?: 1) < maxAvailable,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            ) {
                                Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(1, 5, 10).forEach { addVal ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        val current = inputQtyStr.toIntOrNull() ?: 0
                                        val next = minOf(maxAvailable, current + addVal)
                                        inputQtyStr = maxOf(1, next).toString()
                                    },
                                    label = { Text("+$addVal", fontSize = 11.sp) }
                                )
                            }
                            FilterChip(
                                selected = false,
                                onClick = {
                                    inputQtyStr = maxOf(1, maxAvailable).toString()
                                },
                                label = { Text("Máx ($maxAvailable)", fontSize = 11.sp) }
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        val subtotal = currentQty * prod.price
                        Text(
                            text = "Subtotal: ${formatMoneyMain(subtotal, selectedCountry)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF81C784)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (currentQty in 1..maxAvailable) {
                                onAddToCart(prod, currentQty)
                                lastAddedQty = currentQty
                                detectedProduct = prod
                                matchConfidence = pendingConfidence
                                pendingProductForQty = null
                                isCooldownActive = true
                                coroutineScope.launch {
                                    val totalSteps = 20
                                    for (step in 1..totalSteps) {
                                        delay(100)
                                        cooldownProgress = step.toFloat() / totalSteps.toFloat()
                                    }
                                    isCooldownActive = false
                                    cooldownProgress = 0f
                                    detectedProduct = null
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        enabled = currentQty in 1..maxAvailable
                    ) {
                        Icon(Icons.Filled.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Aceptar y Agregar al Carrito", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            pendingProductForQty = null
                            isCooldownActive = true
                            coroutineScope.launch {
                                delay(1500L)
                                isCooldownActive = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
    }
}
