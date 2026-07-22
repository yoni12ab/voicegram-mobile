package com.voicegram.app.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TextToSpeechConverter(private val context: Context) {
    
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var onSpeakComplete: (() -> Unit)? = null
    
    private val initListener = TextToSpeech.OnInitListener { status ->
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.US
            isInitialized = true
        }
    }
    
    init {
        textToSpeech = TextToSpeech(context, initListener)
    }
    
    fun speak(text: String, onComplete: () -> Unit) {
        if (!isInitialized) {
            onComplete()
            return
        }
        
        onSpeakComplete = onComplete
        
        // Simplified version without listener for now
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE")
        
        // Simulate completion after a delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            onComplete()
        }, 1000)
    }
    
    fun stop() {
        textToSpeech?.stop()
    }
    
    fun release() {
        textToSpeech?.shutdown()
        textToSpeech = null
    }
    
    fun isReady(): Boolean = isInitialized
}