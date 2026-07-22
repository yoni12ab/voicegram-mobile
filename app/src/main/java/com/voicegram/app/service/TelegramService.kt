package com.voicegram.app.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import com.google.gson.JsonParser
import java.util.concurrent.TimeUnit

class TelegramService {
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val JSON = "application/json; charset=utf-8".toMediaType()
    
    suspend fun sendMessage(botToken: String, chatId: String, text: String): MessageResult = withContext(Dispatchers.IO) {
        try {
            // For now, we'll send to the bot itself (most bots can receive messages)
            // In a real implementation, you'd need the actual user's chat ID
            val url = "https://api.telegram.org/bot$botToken/sendMessage"
            
            val json = """
                {
                    "chat_id": "$chatId",
                    "text": "$text",
                    "parse_mode": "HTML"
                }
            """.trimIndent()
            
            val requestBody = json.toRequestBody(JSON)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
                val ok = jsonResponse.get("ok").asBoolean
                
                if (ok) {
                    val result = jsonResponse.getAsJsonObject("result")
                    val messageId = result.get("message_id").asInt
                    
                    MessageResult(
                        success = true,
                        messageId = messageId,
                        error = null
                    )
                } else {
                    val errorDescription = jsonResponse.get("description")?.asString ?: "Unknown error"
                    MessageResult(
                        success = false,
                        messageId = null,
                        error = errorDescription
                    )
                }
            } else {
                MessageResult(
                    success = false,
                    messageId = null,
                    error = "HTTP error: ${response.code}"
                )
            }
        } catch (e: Exception) {
            MessageResult(
                success = false,
                messageId = null,
                error = e.message ?: "Network error"
            )
        }
    }
    
    suspend fun getBotUpdates(botToken: String, offset: Long = 0): List<BotMessage> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.telegram.org/bot$botToken/getUpdates?offset=$offset&timeout=10"
            
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
                val ok = jsonResponse.get("ok").asBoolean
                
                if (ok && jsonResponse.has("result")) {
                    val result = jsonResponse.getAsJsonArray("result")
                    val messages = mutableListOf<BotMessage>()
                    
                    for (item in result) {
                        val messageObj = item.asJsonObject
                        val updateId = messageObj.get("update_id").asLong
                        
                        if (messageObj.has("message")) {
                            val message = messageObj.getAsJsonObject("message")
                            val text = message.get("text")?.asString ?: ""
                            val from = message.getAsJsonObject("from")
                            val fromId = from.get("id").asLong
                            val fromName = from.get("first_name").asString
                            val chat = message.getAsJsonObject("chat")
                            val chatId = chat.get("id").asLong
                            
                            messages.add(BotMessage(
                                updateId = updateId,
                                messageId = message.get("message_id").asInt,
                                text = text,
                                fromId = fromId,
                                fromName = fromName,
                                chatId = chatId,
                                timestamp = message.get("date").asLong
                            ))
                        }
                    }
                    
                    messages
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getRecentChatId(botToken: String): Long? = withContext(Dispatchers.IO) {
        try {
            // Get recent updates to find a valid chat ID
            val updates = getBotUpdates(botToken, 0)
            if (updates.isNotEmpty()) {
                // Return the most recent chat ID
                return@withContext updates.last().chatId
            }
            
            // If no updates, try to get bot info and use bot ID as fallback
            val request = Request.Builder()
                .url("https://api.telegram.org/bot$botToken/getMe")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                if (json.get("ok").asBoolean) {
                    val result = json.getAsJsonObject("result")
                    return@withContext result.get("id").asLong
                }
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }
    
    data class MessageResult(
        val success: Boolean,
        val messageId: Int?,
        val error: String?
    )
    
    data class BotMessage(
        val updateId: Long,
        val messageId: Int,
        val text: String,
        val fromId: Long,
        val fromName: String,
        val chatId: Long,
        val timestamp: Long
    )
}