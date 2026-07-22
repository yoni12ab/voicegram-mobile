package com.voicegram.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.voicegram.app.R
import com.voicegram.app.service.AuthManager
import com.voicegram.app.service.SpeechToTextConverter
import com.voicegram.app.service.TextToSpeechConverter
import com.voicegram.app.service.TelegramService
import com.voicegram.app.service.VoiceRecorder

class CallActivity : AppCompatActivity() {
    
    private lateinit var authManager: AuthManager
    private lateinit var voiceRecorder: VoiceRecorder
    private lateinit var speechToTextConverter: SpeechToTextConverter
    private lateinit var textToSpeechConverter: TextToSpeechConverter
    private lateinit var telegramService: TelegramService
    
    private lateinit var statusTextView: TextView
    private lateinit var endCallButton: Button
    
    private val RECORD_AUDIO_PERMISSION = 1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        
        // Initialize services
        authManager = AuthManager(this)
        voiceRecorder = VoiceRecorder(this)
        speechToTextConverter = SpeechToTextConverter(this)
        textToSpeechConverter = TextToSpeechConverter(this)
        telegramService = TelegramService()
        
        // Setup UI
        setupUI()
        
        // Check authentication
        checkAuthentication()
    }
    
    private fun setupUI() {
        statusTextView = findViewById(R.id.statusTextView)
        endCallButton = findViewById(R.id.endCallButton)
        
        endCallButton.setOnClickListener {
            endCall()
        }
    }
    
    private fun checkAuthentication() {
        if (!authManager.isAuthenticated()) {
            Toast.makeText(this, "Please authenticate first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Setup Telegram credentials
        val botToken = authManager.getBotToken()
        val chatId = authManager.getChatId()
        if (botToken != null && chatId != null) {
            telegramService.setCredentials(botToken, chatId)
        }
        
        startCall()
    }
    
    private fun startCall() {
        statusTextView.text = "Connected to chat"
        
        // Check for audio recording permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION
            )
        } else {
            startVoiceRecording()
        }
    }
    
    private fun startVoiceRecording() {
        voiceRecorder.startRecording { audioFile ->
            statusTextView.text = "Recording..."
        }
    }
    
    private fun stopVoiceRecording() {
        voiceRecorder.stopRecording { audioFile ->
            if (audioFile != null) {
                statusTextView.text = "Processing audio..."
                processAudioFile(audioFile)
            }
        }
    }
    
    private fun processAudioFile(audioFile: java.io.File) {
        speechToTextConverter.convertAudioToText(audioFile, 
            { text ->
                statusTextView.text = "Transcribed: $text"
                sendToTelegram(text)
            },
            { error ->
                statusTextView.text = "Error: ${error.message}"
            }
        )
    }
    
    private fun sendToTelegram(text: String) {
        // Use coroutine to send message
        // For now, we'll show a placeholder
        statusTextView.text = "Sending to Telegram: $text"
        
        // In production, you would use:
        // lifecycleScope.launch {
        //     val success = telegramService.sendMessage(text)
        //     if (success) {
        //         statusTextView.text = "Message sent successfully"
        //     } else {
        //         statusTextView.text = "Failed to send message"
        //     }
        // }
    }
    
    private fun endCall() {
        if (voiceRecorder.isRecording()) {
            stopVoiceRecording()
        }
        
        statusTextView.text = "Call ended"
        
        // Cleanup
        // voiceRecorder.release() - Not implemented in current version
        // speechToTextConverter.release() - No resources to release
        // textToSpeechConverter.release()
        
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
        // voiceRecorder.release() - Not implemented in current version
        // speechToTextConverter.release() - No resources to release
        textToSpeechConverter.release()
    }
}