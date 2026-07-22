package com.voicegram.app.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.util.concurrent.TimeUnit
import android.content.Context

class BotValidator(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    suspend fun validateBotToken(token: String): ValidationResult = withContext(Dispatchers.IO) {
        // Check network connectivity first
        if (!NetworkUtils.isNetworkAvailable(context)) {
            return@withContext ValidationResult(
                isValid = false,
                botName = null,
                botId = null,
                error = "No internet connection"
            )
        }
        
        try {
            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/getMe")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val ok = json.get("ok").asBoolean
                
                if (ok) {
                    val result = json.getAsJsonObject("result")
                    val botName = result.get("username").asString
                    val botId = result.get("id").asLong
                    
                    ValidationResult(
                        isValid = true,
                        botName = botName,
                        botId = botId,
                        error = null
                    )
                } else {
                    val errorDescription = json.get("description")?.asString ?: "Unknown error"
                    ValidationResult(
                        isValid = false,
                        botName = null,
                        botId = null,
                        error = errorDescription
                    )
                }
            } else {
                ValidationResult(
                    isValid = false,
                    botName = null,
                    botId = null,
                    error = "HTTP error: ${response.code}"
                )
            }
        } catch (e: Exception) {
            ValidationResult(
                isValid = false,
                botName = null,
                botId = null,
                error = e.message ?: "Network error"
            )
        }
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val botName: String?,
        val botId: Long?,
        val error: String?
    )
}