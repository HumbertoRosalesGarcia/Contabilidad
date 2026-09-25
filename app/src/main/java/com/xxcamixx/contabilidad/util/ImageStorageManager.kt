package com.xxcamixx.contabilidad.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import java.io.File

object ImageStorageManager {
    private const val PREFS_NAME = "ImageStoragePrefs"
    private const val KEY_CUSTOM_TREE_URI = "custom_folder_tree_uri"
    private const val KEY_CUSTOM_DISPLAY_PATH = "custom_folder_display_path"
    const val FOLDER_NAME = "MiNegocio"

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
                .putString(KEY_CUSTOM_DISPLAY_PATH, displayName ?: treeUri.lastPathSegment ?: FOLDER_NAME)
                .apply()
        }
    }

    fun resetToDefaultStorage(context: Context) {
        setCustomStorage(context, null, null)
        // Asegurar que la carpeta MiNegocio se cree en la raíz correspondiente
        getDefaultStorageDir(context)
    }

    /**
     * Comprueba si la aplicación puede crear y escribir archivos reales en el directorio dado.
     */
    private fun canWriteToDirectory(dir: File): Boolean {
        return try {
            if (!dir.exists()) {
                dir.mkdirs()
            }
            if (dir.exists() && dir.isDirectory) {
                val testFile = File(dir, ".test_write_${System.currentTimeMillis()}")
                if (testFile.createNewFile()) {
                    testFile.delete()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Intenta obtener la raíz de la tarjeta MicroSD extraíble si existe y es accesible.
     */
    private fun getMicroSdDirectory(context: Context): File? {
        try {
            val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
            for (dir in externalDirs) {
                if (dir != null) {
                    val isRemovable = try {
                        Environment.isExternalStorageRemovable(dir)
                    } catch (_: Exception) {
                        false
                    }
                    if (isRemovable) {
                        // Ej: /storage/1234-5678/Android/data/com.xxcamixx.contabilidad/files
                        val path = dir.absolutePath
                        val androidIndex = path.indexOf("/Android", ignoreCase = true)
                        val rootDir = if (androidIndex > 0) {
                            File(path.substring(0, androidIndex))
                        } else {
                            dir
                        }
                        val miNegocioSD = File(rootDir, FOLDER_NAME)
                        if (canWriteToDirectory(miNegocioSD)) {
                            return miNegocioSD
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Obtiene la carpeta MiNegocio en la raíz del almacenamiento principal del teléfono (/storage/emulated/0/MiNegocio).
     * Si el sistema tiene restricciones estrictas de Scoped Storage, utiliza fallbacks seguros.
     */
    private fun getPrimaryStorageDirectory(context: Context): File {
        // 1. Raíz directa del almacenamiento compartido del teléfono
        try {
            @Suppress("DEPRECATION")
            val primaryRoot = Environment.getExternalStorageDirectory()
            val primaryMiNegocio = File(primaryRoot, FOLDER_NAME)
            if (canWriteToDirectory(primaryMiNegocio)) {
                return primaryMiNegocio
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Carpeta pública de Imágenes / MiNegocio
        try {
            @Suppress("DEPRECATION")
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val picturesMiNegocio = File(picturesDir, FOLDER_NAME)
            if (canWriteToDirectory(picturesMiNegocio)) {
                return picturesMiNegocio
            }
        } catch (_: Exception) {}

        // 3. Fallback en almacenamiento externo de la app
        val appExt = File(context.getExternalFilesDir(null), FOLDER_NAME)
        if (canWriteToDirectory(appExt)) {
            return appExt
        }

        // 4. Fallback final interno
        return File(context.filesDir, FOLDER_NAME).apply { mkdirs() }
    }

    /**
     * Obtiene el directorio predeterminado "MiNegocio":
     * - Primero en la raíz de la MicroSD si está disponible y con permisos.
     * - De lo contrario, en la raíz del almacenamiento principal del teléfono.
     */
    fun getDefaultStorageDir(context: Context): File {
        val sdDir = getMicroSdDirectory(context)
        val chosenDir = sdDir ?: getPrimaryStorageDirectory(context)
        migrateOldImagesIfNeeded(context, chosenDir)
        return chosenDir
    }

    /**
     * Migra imágenes de la carpeta interna anterior "product_images" a "MiNegocio"
     * para que las fotos existentes no se pierdan.
     */
    private fun migrateOldImagesIfNeeded(context: Context, targetDir: File) {
        try {
            val oldDir = File(context.filesDir, "product_images")
            if (oldDir.exists() && oldDir.isDirectory) {
                oldDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.startsWith("prod_")) {
                        val dest = File(targetDir, file.name)
                        if (!dest.exists()) {
                            file.copyTo(dest, overwrite = true)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getStorageLocationDescription(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customName = prefs.getString(KEY_CUSTOM_DISPLAY_PATH, null)
        if (!customName.isNullOrBlank()) {
            return customName
        }

        val sdDir = getMicroSdDirectory(context)
        return if (sdDir != null) {
            "MicroSD / $FOLDER_NAME"
        } else {
            "Almacenamiento principal / $FOLDER_NAME"
        }
    }

    fun getSavedImagesCountAndSize(context: Context): Pair<Int, Long> {
        var count = 0
        var totalBytes = 0L

        // Archivos en la carpeta predeterminada "MiNegocio"
        val defaultDir = getDefaultStorageDir(context)
        if (defaultDir.exists() && defaultDir.isDirectory) {
            defaultDir.listFiles()?.forEach { f ->
                if (f.isFile && f.name.startsWith("prod_")) {
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

            // Guardado en almacenamiento predeterminado MiNegocio
            val targetDir = getDefaultStorageDir(context)
            val destFile = File(targetDir, "prod_${System.currentTimeMillis()}.jpg")
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
