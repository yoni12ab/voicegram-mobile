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
                    val errorCode = jsonResponse.get("error_code")?.asInt ?: 0
                    
                    // Provide more detailed error information
                    val detailedError = when (errorCode) {
                        403 -> "403 Forbidden: Bot cannot send messages to this chat. Please send /start to the bot in Telegram first."
                        400 -> "400 Bad Request: Invalid chat ID or message format. Error: $errorDescription"
                        401 -> "401 Unauthorized: Invalid bot token"
                        429 -> "429 Too Many Requests: Rate limited by Telegram"
                        else -> "Telegram API error ($errorCode): $errorDescription"
                    }
                    
                    MessageResult(
                        success = false,
                        messageId = null,
                        error = detailedError
                    )
                }
            } else {
                // Handle HTTP errors with more detail
                val errorDetails = when (response.code) {
                    403 -> "403 Forbidden: Bot doesn't have permission to send to this chat. Send /start to your bot in Telegram first."
                    401 -> "401 Unauthorized: Check your bot token"
                    404 -> "404 Not Found: Bot token or API endpoint incorrect"
                    429 -> "429 Too Many Requests: Telegram rate limit"
                    500 -> "500 Server Error: Telegram API is down"
                    else -> "HTTP error ${response.code}: ${response.message}"
                }
                
                MessageResult(
                    success = false,
                    messageId = null,
                    error = errorDetails
                )
            }
        } catch (e: Exception) {
            MessageResult(
                success = false,
                messageId = null,
                error = "Network error: ${e.message}"
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
            // First, try to get bot info to get the bot's own ID
            val botRequest = Request.Builder()
                .url("https://api.telegram.org/bot$botToken/getMe")
                .build()
            
            val botResponse = client.newCall(botRequest).execute()
            val botResponseBody = botResponse.body?.string()
            
            if (botResponse.isSuccessful && botResponseBody != null) {
                val json = JsonParser.parseString(botResponseBody).asJsonObject
                if (json.get("ok").asBoolean) {
                    val result = json.getAsJsonObject("result")
                    val botId = result.get("id").asLong
                    
                    // Try to get recent updates to find a valid chat ID
                    val updates = getBotUpdates(botToken, 0)
                    if (updates.isNotEmpty()) {
                        // Return the most recent chat ID from actual user messages
                        val userMessages = updates.filter { it.fromId != botId }
                        if (userMessages.isNotEmpty()) {
                            return@withContext userMessages.last().chatId
                        }
                        // If no user messages, use the most recent chat ID
                        return@withContext updates.last().chatId
                    }
                    
                    // Fallback: try using the bot's own ID (some bots can receive messages this way)
                    return@withContext botId
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