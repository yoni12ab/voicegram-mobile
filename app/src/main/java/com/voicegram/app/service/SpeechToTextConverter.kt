package com.voicegram.app.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.io.File

class SpeechToTextConverter(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    
    fun convertAudioToText(audioFile: File, onResult: (String) -> Unit, onError: (Exception) -> Unit) {
        // For audio files, we'll use live speech recognition instead
        // Android SpeechRecognizer works best with live audio
        startLiveSpeechRecognition(onResult, onError)
    }
    
    fun startLiveSpeechRecognition(onResult: (String) -> Unit, onError: (Exception) -> Unit) {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.US)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    // Ready to start listening
                }
                
                override fun onBeginningOfSpeech() {
                    // User started speaking
                }
                
                override fun onRmsChanged(rmsdB: Float) {
                    // Audio level changed
                }
                
                override fun onBufferReceived(buffer: ByteArray?) {
                    // Audio buffer received
                }
                
                override fun onEndOfSpeech() {
                    // User stopped speaking
                }
                
                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown error"
                    }
                    onError(Exception(errorMessage))
                }
                
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        onResult(matches[0])
                    }
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    // Partial results - can be used for real-time feedback
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Other events
                }
            })
            
            speechRecognizer?.startListening(intent)
            
        } catch (e: Exception) {
            onError(e)
        }
    }
    
    fun stopListening() {
        speechRecognizer?.stopListening()
    }
    
    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}