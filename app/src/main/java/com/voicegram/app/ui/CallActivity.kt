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
import com.voicegram.app.service.SpeechToTextConverter
import com.voicegram.app.service.TextToSpeechConverter
import com.voicegram.app.service.VoiceRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallActivity : AppCompatActivity(), CoroutineScope by CoroutineScope(Dispatchers.Main) {
    
    private lateinit var voiceRecorder: VoiceRecorder
    private lateinit var speechToTextConverter: SpeechToTextConverter
    private lateinit var textToSpeechConverter: TextToSpeechConverter
    
    private lateinit var statusTextView: TextView
    private lateinit var contactNameTextView: TextView
    private lateinit var endCallButton: Button
    
    private var botToken: String? = null
    private var botName: String? = null
    
    private val RECORD_AUDIO_PERMISSION = 1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        
        // Get bot information from intent
        botToken = intent.getStringExtra("bot_token")
        botName = intent.getStringExtra("bot_name")
        
        // Initialize services
        voiceRecorder = VoiceRecorder(this)
        speechToTextConverter = SpeechToTextConverter(this)
        textToSpeechConverter = TextToSpeechConverter(this)
        
        // Setup UI
        setupUI()
        
        // Start voice recording
        startVoiceRecording()
    }
    
    private fun setupUI() {
        statusTextView = findViewById(R.id.statusTextView)
        contactNameTextView = findViewById(R.id.contactNameTextView)
        endCallButton = findViewById(R.id.endCallButton)
        
        contactNameTextView.text = botName ?: "Bot"
        
        endCallButton.setOnClickListener {
            endCall()
        }
    }
    
    private fun startVoiceRecording() {
        statusTextView.text = "Connected to @$botName"
        
        // Check for audio recording permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION
            )
        } else {
            voiceRecorder.startRecording { audioFile ->
                statusTextView.text = "Recording..."
            }
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
                statusTextView.text = "Sending to bot..."
                sendToBot(text)
            },
            { error ->
                statusTextView.text = "Error: ${error.message}"
            }
        )
    }
    
    private fun sendToBot(text: String) {
        launch {
            // Placeholder for bot interaction
            // In production, this would use the Telegram Bot API to send messages
            statusTextView.text = "Message sent to @$botName"
            
            // Simulate a bot response
            val botResponse = "This is a simulated response from @$botName. Your message was: $text"
            speakBotResponse(botResponse)
        }
    }
    
    private fun speakBotResponse(text: String) {
        statusTextView.text = "Bot: $text"
        textToSpeechConverter.speak(text) {
            statusTextView.text = "Ready to record"
        }
    }
    
    private fun endCall() {
        if (voiceRecorder.isRecording()) {
            stopVoiceRecording()
        }
        
        statusTextView.text = "Call ended"
        
        // Cleanup
        // voiceRecorder.release() - Not implemented in current version
        // speechToTextConverter.release() - No resources to release
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
        // voiceRecorder.release() - Not implemented in current version
        // speechToTextConverter.release() - No resources to release
        textToSpeechConverter.release()
    }
}