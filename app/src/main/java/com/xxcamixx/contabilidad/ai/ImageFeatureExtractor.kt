package com.xxcamixx.contabilidad.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

object ImageFeatureExtractor {

    private const val MODEL_ASSET = "mobilenet_v1_quant.tflite"
    private const val INPUT_SIZE = 224
    private const val EMBEDDING_SIZE = 1001

    @Volatile
    private var interpreter: Interpreter? = null

    @Synchronized
    fun init(context: Context) {
        if (interpreter == null) {
            try {
                val assetFileDescriptor = context.assets.openFd(MODEL_ASSET)
                val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
                val fileChannel = inputStream.channel
                val startOffset = assetFileDescriptor.startOffset
                val declaredLength = assetFileDescriptor.declaredLength
                val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                interpreter = Interpreter(modelBuffer, options)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Extrae el vector de caracteristicas (embedding) a partir de un Bitmap.
     * Retorna un FloatArray normalizado de tamano 1001 o null si falla.
     */
    fun extractFeatures(context: Context, bitmap: Bitmap): FloatArray? {
        init(context)
        val tflite = interpreter ?: return null

        try {
            // Escalar y recortar centrado a 224x224
            val scaledBitmap = scaleAndCenterCrop(bitmap, INPUT_SIZE, INPUT_SIZE)

            // Buffer de entrada para modelo quantized uint8 [1, 224, 224, 3]
            val inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3).apply {
                order(ByteOrder.nativeOrder())
            }

            val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
            scaledBitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

            for (pixelValue in intValues) {
                val r = (pixelValue shr 16 and 0xFF).toByte()
                val g = (pixelValue shr 8 and 0xFF).toByte()
                val b = (pixelValue and 0xFF).toByte()
                inputBuffer.put(r)
                inputBuffer.put(g)
                inputBuffer.put(b)
            }

            val outputBuffer = Array(1) { ByteArray(EMBEDDING_SIZE) }

            synchronized(tflite) {
                tflite.run(inputBuffer, outputBuffer)
            }

            val rawOutput = outputBuffer[0]
            val floatVector = FloatArray(EMBEDDING_SIZE)
            var normSum = 0.0

            for (i in 0 until EMBEDDING_SIZE) {
                val unsignedVal = (rawOutput[i].toInt() and 0xFF).toFloat() / 255.0f
                floatVector[i] = unsignedVal
                normSum += unsignedVal * unsignedVal
            }

            val norm = sqrt(normSum).toFloat()
            if (norm > 0f) {
                for (i in 0 until EMBEDDING_SIZE) {
                    floatVector[i] /= norm
                }
            }

            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }

            return floatVector
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Extrae caracteristicas directamente desde la URI guardada de una foto del producto.
     */
    fun extractFeaturesFromUri(context: Context, imageUriStr: String): FloatArray? {
        return try {
            val uri = Uri.parse(imageUriStr)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap != null) {
                val vec = extractFeatures(context, bitmap)
                bitmap.recycle()
                vec
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Calcula la Similitud Coseno entre dos vectores matematicos normalizados.
     * Rango: 0.0f a 1.0f (1.0 = identicos).
     */
    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.size != v2.size || v1.isEmpty()) return 0f
        var dot = 0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
        }
        return dot.coerceIn(0f, 1f)
    }

    /**
     * Serializa un FloatArray en una cadena compacta separada por comas para Room y JSON.
     */
    fun vectorToString(vector: FloatArray): String {
        return buildString {
            for (i in vector.indices) {
                append(String.format(java.util.Locale.US, "%.5f", vector[i]))
                if (i < vector.size - 1) append(",")
            }
        }
    }

    /**
     * Deserializa una cadena separada por comas en un FloatArray.
     */
    fun stringToVector(str: String?): FloatArray? {
        if (str.isNullOrBlank()) return null
        return try {
            val parts = str.split(",")
            val floats = FloatArray(parts.size)
            for (i in parts.indices) {
                floats[i] = parts[i].trim().toFloat()
            }
            floats
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Escala y recorta centralmente el Bitmap para mantener la relacion de aspecto.
     */
    fun scaleAndCenterCrop(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height

        val xScale = targetWidth.toFloat() / sourceWidth
        val yScale = targetHeight.toFloat() / sourceHeight
        val scale = maxOf(xScale, yScale)

        val scaledWidth = scale * sourceWidth
        val scaledHeight = scale * sourceHeight

        val left = (targetWidth - scaledWidth) / 2
        val top = (targetHeight - scaledHeight) / 2

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(left, top)
        }

        val targetBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(targetBitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)

        return targetBitmap
    }
}
