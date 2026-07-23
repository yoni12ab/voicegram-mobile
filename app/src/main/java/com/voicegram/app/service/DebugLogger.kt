package com.voicegram.app.service

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    private const val TAG = "VoiceGram"
    private const val LOG_FILE = "voicegram_debug.log"
    private val logEntries = mutableListOf<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    fun log(message: String, level: LogLevel = LogLevel.INFO) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] [${level.name}] $message"
        
        // Add to memory
        logEntries.add(logEntry)
        
        // Keep only last 1000 entries in memory
        if (logEntries.size > 1000) {
            logEntries.removeAt(0)
        }
        
        // Log to Android logcat
        when (level) {
            LogLevel.ERROR -> Log.e(TAG, message)
            LogLevel.WARN -> Log.w(TAG, message)
            LogLevel.INFO -> Log.i(TAG, message)
            LogLevel.DEBUG -> Log.d(TAG, message)
        }
    }
    
    fun logApiCall(endpoint: String, method: String, requestBody: String? = null) {
        log("API Call: $method $endpoint", LogLevel.DEBUG)
        if (requestBody != null) {
            log("Request Body: $requestBody", LogLevel.DEBUG)
        }
    }
    
    fun logApiResponse(endpoint: String, responseCode: Int, responseBody: String? = null) {
        log("API Response: $endpoint - Code: $responseCode", LogLevel.DEBUG)
        if (responseBody != null) {
            log("Response Body: $responseBody", LogLevel.DEBUG)
        }
    }
    
    fun logError(message: String, error: Throwable? = null) {
        log("ERROR: $message", LogLevel.ERROR)
        error?.let {
            log("Stack trace: ${Log.getStackTraceString(it)}", LogLevel.ERROR)
        }
    }
    
    fun exportLogs(context: Context): String {
        val logFile = File(context.getExternalFilesDir(null), LOG_FILE)
        val writer = FileWriter(logFile)
        
        writer.write("VoiceGram Debug Log\n")
        writer.write("Generated: ${dateFormat.format(Date())}\n")
        writer.write("Total entries: ${logEntries.size}\n")
        writer.write("=====================================\n\n")
        
        logEntries.forEach { entry ->
            writer.write("$entry\n")
        }
        
        writer.close()
        return logFile.absolutePath
    }
    
    fun getRecentLogs(count: Int = 50): List<String> {
        return logEntries.takeLast(count)
    }
    
    fun clearLogs() {
        logEntries.clear()
        log("Logs cleared", LogLevel.INFO)
    }
    
    enum class LogLevel {
        ERROR, WARN, INFO, DEBUG
    }
}