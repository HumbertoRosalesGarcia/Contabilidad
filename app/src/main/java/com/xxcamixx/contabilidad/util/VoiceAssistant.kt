package com.xxcamixx.contabilidad.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object AppVoice {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    // Función limpiadora para que la voz no diga "Punto" o "Guión"
    private fun cleanSpeechText(input: String): String {
        return input
            .replace("...", ",")
            .replace(Regex("\\.(?=\\s|$)"), ",")
            .replace("/", " ")
            .replace("-", " ")
            .replace("_", " ")
    }

    fun speak(context: Context, text: String) {
        val sanitizedText = cleanSpeechText(text)

        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("es", "ES")
                    isInitialized = true
                    tts?.speak(pendingText ?: sanitizedText, TextToSpeech.QUEUE_FLUSH, null, "NotifID")
                    pendingText = null
                }
            }
            pendingText = sanitizedText
        } else if (isInitialized) {
            tts?.speak(sanitizedText, TextToSpeech.QUEUE_FLUSH, null, "NotifID")
        }
    }
}