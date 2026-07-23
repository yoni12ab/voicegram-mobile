package com.voicegram.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.voicegram.app.service.DebugLogger
import com.voicegram.app.service.TelegramAuthService
import java.io.File

class MainActivity : AppCompatActivity() {
    
    private lateinit var botListButton: Button
    private lateinit var debugLogsButton: Button
    private lateinit var authButton: Button
    private lateinit var versionText: TextView
    private lateinit var authStatusText: TextView
    
    private lateinit var telegramAuthService: TelegramAuthService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize services
        telegramAuthService = TelegramAuthService(this)
        DebugLogger.log("App started - MainActivity onCreate", DebugLogger.LogLevel.INFO)
        
        setupUI()
        checkAuthenticationStatus()
    }
    
    private fun setupUI() {
        botListButton = findViewById(R.id.botListButton)
        debugLogsButton = findViewById(R.id.debugLogsButton)
        authButton = findViewById(R.id.authButton)
        versionText = findViewById(R.id.versionText)
        authStatusText = findViewById(R.id.authStatusText)
        
        // Set version info
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }
            versionText.text = "Version $versionName (Build $versionCode)"
            DebugLogger.log("App version: $versionName (Build $versionCode)", DebugLogger.LogLevel.INFO)
        } catch (e: Exception) {
            versionText.text = "Version 1.0.28"
            DebugLogger.logError("Error getting version info", e)
        }
        
        botListButton.setOnClickListener {
            if (telegramAuthService.isAuthenticated()) {
                openBotList()
            } else {
                Toast.makeText(this, "Please authenticate with Telegram first", Toast.LENGTH_SHORT).show()
            }
        }
        
        authButton.setOnClickListener {
            authenticateWithTelegram()
        }
        
        debugLogsButton.setOnClickListener {
            exportDebugLogs()
        }
        
        // Show auth button initially
        showAuthButton()
    }
    
    private fun checkAuthenticationStatus() {
        if (telegramAuthService.isAuthenticated()) {
            val senderInfo = telegramAuthService.getSenderInfo()
            authStatusText.text = "Authenticated as: ${senderInfo.username ?: senderInfo.phoneNumber}"
            authStatusText.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            showBotListButton()
        } else {
            authStatusText.text = "Not authenticated with Telegram"
            authStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            showAuthButton()
        }
    }
    
    private fun authenticateWithTelegram() {
        DebugLogger.log("Starting Telegram authentication process", DebugLogger.LogLevel.INFO)
        
        // Open BotFather for authentication
        telegramAuthService.authenticateWithBotFather()
        
        Toast.makeText(this, "Opening Telegram... Complete the authentication there", Toast.LENGTH_LONG).show()
    }
    
    private fun showAuthButton() {
        authButton.visibility = Button.VISIBLE
        botListButton.visibility = Button.GONE
    }
    
    private fun showBotListButton() {
        authButton.visibility = Button.GONE
        botListButton.visibility = Button.VISIBLE
    }
    
    private fun openBotList() {
        val intent = Intent(this, com.voicegram.app.ui.BotListActivity::class.java)
        // Pass sender info to BotListActivity
        val senderInfo = telegramAuthService.getSenderInfo()
        intent.putExtra("sender_phone_number", senderInfo.phoneNumber)
        intent.putExtra("sender_user_id", senderInfo.userId)
        intent.putExtra("sender_username", senderInfo.username)
        startActivity(intent)
    }
    
    private fun exportDebugLogs() {
        try {
            DebugLogger.log("Exporting debug logs...", DebugLogger.LogLevel.INFO)
            val logFilePath = DebugLogger.exportLogs(this)
            val logFile = File(logFilePath)
            
            if (logFile.exists()) {
                val uri = Uri.fromFile(logFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "VoiceGram Debug Logs")
                    putExtra(Intent.EXTRA_TEXT, "Debug logs from VoiceGram app")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Share Debug Logs"))
                Toast.makeText(this, "Debug logs exported successfully!", Toast.LENGTH_SHORT).show()
                DebugLogger.log("Debug logs exported to: $logFilePath", DebugLogger.LogLevel.INFO)
            } else {
                Toast.makeText(this, "Failed to export debug logs", Toast.LENGTH_SHORT).show()
                DebugLogger.logError("Failed to export debug logs - file not created", null)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error exporting logs: ${e.message}", Toast.LENGTH_SHORT).show()
            DebugLogger.logError("Error exporting debug logs", e)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check authentication status when returning to the app
        checkAuthenticationStatus()
    }
}