package com.voicegram.app.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.io.File
import java.util.Locale

class SpeechToTextConverter(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    
    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    fun convertAudioToText(@Suppress("UNUSED_PARAMETER") audioFile: File, onResult: (String) -> Unit, onError: (Exception) -> Unit) {
        // For audio files, we'll use live speech recognition instead
        // Android SpeechRecognizer works best with live audio
        startLiveSpeechRecognition(onResult, onError)
    }
    
    fun startLiveSpeechRecognition(onResult: (String) -> Unit, onError: (Exception) -> Unit) {
        try {
            // Check if speech recognition is available
            if (!isSpeechRecognitionAvailable()) {
                onError(Exception("Speech recognition not available on this device"))
                return
            }
            
            // Check microphone permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
                onError(Exception("Microphone permission not granted"))
                return
            }
            
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            
            // Create intent with optimized settings for Xiaomi devices
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // Use device default language for better compatibility
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                // Add extra parameters for better Xiaomi compatibility
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000)
            }
            
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    // Ready to start listening - speech recognition is working
                }
                
                override fun onBeginningOfSpeech() {
                    // User started speaking
                }
                
                override fun onRmsChanged(rmsdB: Float) {
                    // Audio level changed - indicates microphone is working
                }
                
                override fun onBufferReceived(buffer: ByteArray?) {
                    // Audio buffer received
                }
                
                override fun onEndOfSpeech() {
                    // User stopped speaking
                }
                
                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Microphone error - check if microphone is working"
                        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied - please grant in settings"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error - check internet connection"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout - please try again"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected - please speak clearly"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition busy - please wait"
                        SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input - please speak within 5 seconds"
                        else -> "Unknown speech recognition error: $error"
                    }
                    onError(Exception(errorMessage))
                }
                
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        onResult(matches[0])
                    } else {
                        onError(Exception("No speech results received"))
                    }
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    // Partial results - can be used for real-time feedback
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        // Use partial results as fallback
                        onResult(matches[0])
                    }
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Other events
                }
            })
            
            speechRecognizer?.startListening(intent)
            
        } catch (e: SecurityException) {
            onError(Exception("Security error: ${e.message} - check app permissions"))
        } catch (e: Exception) {
            onError(Exception("Speech recognition error: ${e.message}"))
        }
    }
    
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore stop errors
        }
    }
    
    fun release() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore destroy errors
        } finally {
            speechRecognizer = null
        }
    }
}