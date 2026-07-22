package com.voicegram.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.voicegram.app.R
import com.voicegram.app.service.SpeechToTextConverter
import com.voicegram.app.service.TextToSpeechConverter
import com.voicegram.app.service.TelegramService
import com.voicegram.app.service.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallActivity : AppCompatActivity(), CoroutineScope by CoroutineScope(Dispatchers.Main) {
    
    private lateinit var speechToTextConverter: SpeechToTextConverter
    private lateinit var textToSpeechConverter: TextToSpeechConverter
    private lateinit var telegramService: TelegramService
    
    private lateinit var statusTextView: TextView
    private lateinit var contactNameTextView: TextView
    private lateinit var endCallButton: Button
    private lateinit var progressBar: ProgressBar
    
    private var botToken: String? = null
    private var botName: String? = null
    private var chatId: Long? = null
    private var lastUpdateId: Long = 0
    private var isPolling = false
    
    private val RECORD_AUDIO_PERMISSION = 1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        
        // Get bot information from intent
        botToken = intent.getStringExtra("bot_token")
        botName = intent.getStringExtra("bot_name")
        
        // Initialize services
        speechToTextConverter = SpeechToTextConverter(this)
        textToSpeechConverter = TextToSpeechConverter(this)
        telegramService = TelegramService()
        
        // Setup UI
        setupUI()
        
        // Start voice recording
        startVoiceRecording()
    }
    
    private fun setupUI() {
        statusTextView = findViewById(R.id.statusTextView)
        contactNameTextView = findViewById(R.id.contactNameTextView)
        endCallButton = findViewById(R.id.endCallButton)
        progressBar = findViewById(R.id.progressBar)
        
        contactNameTextView.text = botName ?: "Bot"
        
        endCallButton.setOnClickListener {
            endCall()
        }
    }
    
    private fun showLoading(message: String) {
        statusTextView.text = message
        progressBar.visibility = ProgressBar.VISIBLE
    }
    
    private fun hideLoading() {
        progressBar.visibility = ProgressBar.GONE
    }
    
    private fun showStatus(message: String) {
        statusTextView.text = message
        hideLoading()
    }
    
    private fun startVoiceRecording() {
        showStatus("Connected to @$botName")
        
        // Check if speech recognition is available on this device
        if (!speechToTextConverter.isSpeechRecognitionAvailable()) {
            showStatus("Speech recognition not available")
            Toast.makeText(applicationContext, "Speech recognition is not available on this device. Please check if Google speech services are installed.", Toast.LENGTH_LONG).show()
            return
        }
        
        // Check for audio recording permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION
            )
        } else {
            // Start speech recognition directly
            showLoading("Listening... Speak now")
            speechToTextConverter.startLiveSpeechRecognition(
                onResult = { text ->
                    showStatus("Recognized: $text")
                    sendToBot(text)
                },
                onError = { error ->
                    val errorMessage = error.message ?: "Unknown error"
                    showStatus("Error: $errorMessage")
                    Toast.makeText(applicationContext, "Speech recognition failed: $errorMessage", Toast.LENGTH_LONG).show()
                    
                    // Provide specific guidance based on error type
                    when {
                        errorMessage.contains("permission", ignoreCase = true) -> {
                            Toast.makeText(applicationContext, "Please grant microphone permission in Settings > Apps > VoiceGram > Permissions", Toast.LENGTH_LONG).show()
                        }
                        errorMessage.contains("available", ignoreCase = true) -> {
                            Toast.makeText(applicationContext, "Please install Google speech services or check device compatibility", Toast.LENGTH_LONG).show()
                        }
                        errorMessage.contains("microphone", ignoreCase = true) -> {
                            Toast.makeText(applicationContext, "Please check if your microphone is working and not blocked by another app", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
    }
    
    private fun stopVoiceRecording() {
        speechToTextConverter.stopListening()
    }
    
    private fun processAudioFile(@Suppress("UNUSED_PARAMETER") audioFile: java.io.File) {
        // No longer needed with live speech recognition
        // This method is kept for compatibility
    }
    
    private fun sendToBot(text: String) {
        launch {
            // Check network connectivity
            if (!NetworkUtils.isNetworkAvailable(this@CallActivity)) {
                showStatus("No internet connection")
                Toast.makeText(applicationContext, "Please check your internet connection", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            showLoading("Getting chat ID...")
            
            // Get a valid chat ID first
            val validChatId = telegramService.getRecentChatId(botToken ?: "")
            
            if (validChatId == null) {
                showStatus("Setup required: Send /start to @$botName in Telegram")
                Toast.makeText(applicationContext, "Open Telegram, search for @$botName, and send /start to activate the bot", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            chatId = validChatId
            showLoading("Sending to @$botName (${NetworkUtils.getNetworkType(this@CallActivity)})...")
            
            // Use the valid chat ID for sending messages
            val result = telegramService.sendMessage(botToken ?: "", validChatId.toString(), text)
            
            if (result.success) {
                showStatus("Message sent to @$botName")
                
                // Start polling for bot responses
                startBotResponsePolling()
            } else {
                showStatus("Error: " + (result.error ?: "Unknown error"))
                Toast.makeText(applicationContext, "Failed to send message: " + (result.error ?: "Unknown error"), Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun startBotResponsePolling() {
        if (isPolling) return
        isPolling = true
        
        launch {
            showLoading("Waiting for bot response...")
            
            // Poll for responses with timeout
            val maxPolls = 10 // Maximum number of polling attempts
            var pollCount = 0
            
            while (isPolling && pollCount < maxPolls) {
                try {
                    val updates = telegramService.getBotUpdates(botToken ?: "", lastUpdateId + 1)
                    
                    if (updates.isNotEmpty()) {
                        val lastMessage = updates.last()
                        lastUpdateId = lastMessage.updateId
                        
                        if (lastMessage.text.isNotEmpty()) {
                            isPolling = false
                            showStatus("Response received")
                            speakBotResponse(lastMessage.text)
                            return@launch
                        }
                    }
                    
                    pollCount++
                    kotlinx.coroutines.delay(1000) // Wait 1 second between polls
                    
                } catch (e: Exception) {
                    // Continue polling on error
                    pollCount++
                    kotlinx.coroutines.delay(1000)
                }
            }
            
            // Timeout reached
            isPolling = false
            showStatus("No response received from @$botName")
            Toast.makeText(applicationContext, "Bot didn't respond within timeout", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun speakBotResponse(text: String) {
        showStatus("Bot: $text")
        textToSpeechConverter.speak(text) {
            showStatus("Ready to speak")
        }
    }
    
    private fun endCall() {
        isPolling = false // Stop polling
        speechToTextConverter.stopListening()
        showStatus("Call ended")
        
        // Cleanup
        speechToTextConverter.release()
        textToSpeechConverter.release()
        
        finish()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecording()
            } else {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        speechToTextConverter.release()
        textToSpeechConverter.release()
    }
}