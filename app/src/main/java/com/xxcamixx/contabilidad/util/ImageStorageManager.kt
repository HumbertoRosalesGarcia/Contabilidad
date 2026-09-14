package com.xxcamixx.contabilidad.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import java.io.File

object ImageStorageManager {
    private const val PREFS_NAME = "ImageStoragePrefs"
    private const val KEY_CUSTOM_TREE_URI = "custom_folder_tree_uri"
    private const val KEY_CUSTOM_DISPLAY_PATH = "custom_folder_display_path"

    fun getCustomStorageUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriStr = prefs.getString(KEY_CUSTOM_TREE_URI, null) ?: return null
        return try {
            Uri.parse(uriStr)
        } catch (_: Exception) {
            null
        }
    }

    fun setCustomStorage(context: Context, treeUri: Uri?, displayName: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (treeUri == null) {
            prefs.edit()
                .remove(KEY_CUSTOM_TREE_URI)
                .remove(KEY_CUSTOM_DISPLAY_PATH)
                .apply()
        } else {
            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}

            prefs.edit()
                .putString(KEY_CUSTOM_TREE_URI, treeUri.toString())
                .putString(KEY_CUSTOM_DISPLAY_PATH, displayName ?: treeUri.lastPathSegment ?: "Carpeta seleccionada")
                .apply()
        }
    }

    fun getStorageLocationDescription(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customName = prefs.getString(KEY_CUSTOM_DISPLAY_PATH, null)
        if (!customName.isNullOrBlank()) {
            return customName
        }
        return "Almacenamiento interno / Contabilidad / Imágenes"
    }

    fun getSavedImagesCountAndSize(context: Context): Pair<Int, Long> {
        var count = 0
        var totalBytes = 0L

        // Archivos en la carpeta interna predeterminada
        val defaultDir = File(context.filesDir, "product_images")
        if (defaultDir.exists()) {
            defaultDir.listFiles()?.forEach { f ->
                if (f.isFile) {
                    count++
                    totalBytes += f.length()
                }
            }
        }

        // Archivos en la carpeta personalizada si existe
        val customUri = getCustomStorageUri(context)
        if (customUri != null) {
            try {
                val docDir = DocumentFile.fromTreeUri(context, customUri)
                if (docDir != null && docDir.exists() && docDir.isDirectory) {
                    docDir.listFiles().forEach { f ->
                        if (f.isFile && f.name?.startsWith("prod_") == true) {
                            count++
                            totalBytes += f.length()
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return Pair(count, totalBytes)
    }

    /**
     * Guarda la imagen localmente en el dispositivo.
     * Si el usuario eligió una carpeta personalizada en su teléfono, se almacena allí.
     * De lo contrario, se guarda en la carpeta interna aislada de la app.
     * NUNCA se sube al servidor.
     */
    fun saveImageLocally(context: Context, sourceUri: Uri): String {
        return try {
            try {
                context.contentResolver.takePersistableUriPermission(
                    sourceUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val customUri = getCustomStorageUri(context)
            if (customUri != null) {
                try {
                    val docDir = DocumentFile.fromTreeUri(context, customUri)
                    if (docDir != null && docDir.canWrite()) {
                        val newFile = docDir.createFile("image/jpeg", "prod_${System.currentTimeMillis()}.jpg")
                        if (newFile != null) {
                            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                                    input.copyTo(output)
                                }
                            }
                            return newFile.uri.toString()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Guardado en almacenamiento interno predeterminado
            val imagesDir = File(context.filesDir, "product_images").apply { if (!exists()) mkdirs() }
            val destFile = File(imagesDir, "prod_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            sourceUri.toString()
        }
    }

    /**
     * Genera una URI segura mediante FileProvider para que la cámara del sistema
     * capture la foto en alta resolución directamente en el caché local.
     */
    fun createCameraTempUri(context: Context): Pair<File, Uri> {
        val cacheDir = File(context.cacheDir, "camera_captures").apply { if (!exists()) mkdirs() }
        val tempFile = File(cacheDir, "temp_camera_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return Pair(tempFile, uri)
    }
}
