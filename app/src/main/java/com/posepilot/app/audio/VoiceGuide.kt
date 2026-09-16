package com.posepilot.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** On-device text-to-speech. Throttling is decided by SpeechScheduler; this class only speaks. */
class VoiceGuide(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready = false
    @Volatile var enabled: Boolean = true
        set(value) {
            field = value
            if (!value) stop()
        }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val result = tts.setLanguage(Locale.getDefault())
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.US)
        }
        tts.setSpeechRate(1.05f)
        ready = true
    }

    /** Interrupts whatever is being said: the newest instruction is always the relevant one. */
    fun speak(text: String) {
        if (!enabled || !ready) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "posepilot-${System.nanoTime()}")
    }

    fun stop() { if (ready) tts.stop() }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
