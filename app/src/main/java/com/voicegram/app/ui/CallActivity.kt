package com.voicegram.app.ui

import android.Manifest
import android.app.AlertDialog
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
    private var manualChatId: String? = null // User-provided chat ID
    private var chatId: Long? = null
    private var senderPhoneNumber: String? = null // Sender's phone number for authentication
    private var senderUserId: String? = null // Sender's user ID for authentication
    
    private val RECORD_AUDIO_PERMISSION = 1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        
        // Get bot information from intent
        botToken = intent.getStringExtra("bot_token")
        botName = intent.getStringExtra("bot_name")
        manualChatId = intent.getStringExtra("bot_chat_id")
        senderPhoneNumber = intent.getStringExtra("sender_phone_number") // Get sender phone number
        senderUserId = intent.getStringExtra("sender_user_id") // Get sender user ID
        
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
            
            // Use manual chat ID if provided, otherwise try to detect automatically
            val targetChatId = if (manualChatId != null && manualChatId!!.isNotEmpty()) {
                showLoading("Using manual chat ID...")
                manualChatId!!
            } else {
                showLoading("Getting chat ID...")
                val detectedChatId = telegramService.getRecentChatId(botToken ?: "")
                if (detectedChatId == null) {
                    showStatus("Setup required: Send /start to @$botName in Telegram")
                    Toast.makeText(applicationContext, "Open Telegram, search for @$botName, and send /start to activate the bot", Toast.LENGTH_LONG).show()
                    return@launch
                }
                detectedChatId.toString()
            }
            
            showLoading("Sending to @$botName (${NetworkUtils.getNetworkType(this@CallActivity)})...")
            
            // Use the chat ID for sending messages with sender phone number and user ID for authentication
            val result = telegramService.sendMessage(botToken ?: "", targetChatId, text, senderPhoneNumber, senderUserId)
            
            if (result.success) {
                showStatus("Message sent to @$botName with sender verification")
                Toast.makeText(applicationContext, "Message sent successfully with your ID for verification", Toast.LENGTH_SHORT).show()
                // Don't poll for responses - just send and be done
            } else {
                val errorMessage = result.error ?: "Unknown error"
                showStatus("Error: $errorMessage")
                Toast.makeText(applicationContext, "Failed to send message: $errorMessage", Toast.LENGTH_LONG).show()
                
                // Provide specific guidance for 403 errors
                if (errorMessage.contains("403", ignoreCase = true)) {
                    val guidanceMessage = if (manualChatId != null && manualChatId!!.isNotEmpty()) {
                        "The manual chat ID you provided doesn't work with this bot.\n\nPlease:\n1. Get your correct chat ID from @userinfobot in Telegram\n2. Edit the bot and update the chat ID\n3. Try again"
                    } else {
                        "To fix this 403 error:\n\nOption 1: Send /start to @$botName in Telegram\n\nOption 2: Add your chat ID manually:\n1. Get your chat ID from @userinfobot in Telegram\n2. Edit the bot in VoiceGram and add the chat ID\n3. This prevents 403 errors"
                    }
                    
                    AlertDialog.Builder(this@CallActivity)
                        .setTitle("Chat ID Error")
                        .setMessage(guidanceMessage)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }
    
    private fun endCall() {
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