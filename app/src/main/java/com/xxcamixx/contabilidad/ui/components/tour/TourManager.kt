package com.xxcamixx.contabilidad.ui.components.tour

import android.content.Context
import android.content.SharedPreferences

object TourManager {
    private const val PREFS_PREFIX = "TourPrefs_"
    private const val KEY_ZONE_PREFIX = "tour_seen_zone_"

    private fun getPrefs(context: Context, userId: String): SharedPreferences {
        val safeUserId = if (userId.isBlank()) "default" else userId
        return context.getSharedPreferences("${PREFS_PREFIX}$safeUserId", Context.MODE_PRIVATE)
    }

    /**
     * Verifica si el usuario ya vio el tour de una zona específica.
     */
    fun isZoneSeen(context: Context, userId: String, zone: TourZone): Boolean {
        val prefs = getPrefs(context, userId)
        return prefs.getBoolean("${KEY_ZONE_PREFIX}${zone.prefKey}", false)
    }

    /**
     * Marca una zona como vista para que no vuelva a desplegarse automáticamente.
     */
    fun markZoneSeen(context: Context, userId: String, zone: TourZone) {
        val prefs = getPrefs(context, userId)
        prefs.edit().putBoolean("${KEY_ZONE_PREFIX}${zone.prefKey}", true).apply()
    }

    /**
     * Reinicia una zona específica para que vuelva a mostrarse.
     */
    fun resetZone(context: Context, userId: String, zone: TourZone) {
        val prefs = getPrefs(context, userId)
        prefs.edit().remove("${KEY_ZONE_PREFIX}${zone.prefKey}").apply()
    }

    /**
     * Reinicia todas las zonas del tour para el usuario, permitiendo volver a ver el tutorial completo.
     */
    fun resetAllZones(context: Context, userId: String) {
        val prefs = getPrefs(context, userId)
        val editor = prefs.edit()
        for (zone in TourZone.values()) {
            editor.remove("${KEY_ZONE_PREFIX}${zone.prefKey}")
        }
        editor.remove("tour_completed")
        editor.apply()
    }
}
