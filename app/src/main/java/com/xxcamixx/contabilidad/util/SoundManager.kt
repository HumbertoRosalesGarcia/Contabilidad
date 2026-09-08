package com.xxcamixx.contabilidad.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri

object AppSounds {
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var defaultToneGen: ToneGenerator? = null

    fun init() {
        if (defaultToneGen == null) {
            try {
                defaultToneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            } catch (e: Exception) {}
        }
    }

    fun play(context: Context, soundUri: String?) {
        try {
            init()
            if (soundUri.isNullOrEmpty()) {
                defaultToneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                return
            }
            mediaPlayer?.release()
            mediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(context, Uri.parse(soundUri))
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setOnPreparedListener { it.start() }
                setOnCompletionListener { it.release() }
                prepareAsync()
            }
        } catch (e: Exception) {
            defaultToneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        }
    }
}