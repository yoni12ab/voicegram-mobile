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
    
    suspend fun sendMessage(botToken: String, chatId: String, text: String, senderPhoneNumber: String? = null): MessageResult = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.telegram.org/bot$botToken/sendMessage"
            
            // Add sender phone number to message for controller verification
            val messageWithSender = if (senderPhoneNumber != null) {
                "From: $senderPhoneNumber\n\n$text"
            } else {
                text
            }
            
            val json = """
                {
                    "chat_id": "$chatId",
                    "text": "$messageWithSender",
                    "parse_mode": "HTML"
                }
            """.trimIndent()
            
            // Log the API call
            DebugLogger.logApiCall(url, "POST", json)
            DebugLogger.log("Attempting to send message to chat_id: $chatId from sender: $senderPhoneNumber", DebugLogger.LogLevel.INFO)
            
            val requestBody = json.toRequestBody(JSON)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            // Log the response
            DebugLogger.logApiResponse(url, response.code, responseBody)
            
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
                val ok = jsonResponse.get("ok").asBoolean
                
                if (ok) {
                    val result = jsonResponse.getAsJsonObject("result")
                    val messageId = result.get("message_id").asInt
                    
                    DebugLogger.log("Message sent successfully! Message ID: $messageId", DebugLogger.LogLevel.INFO)
                    
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
                        403 -> """
                            403 Forbidden: Bot cannot send messages to this chat.
                            
                            SOLUTIONS:
                            1. Send /start to your bot in Telegram first
                            2. Use @userinfobot to get your correct chat ID
                            3. Add your chat ID manually in bot settings
                            
                            Error: $errorDescription
                        """.trimIndent()
                        400 -> """
                            400 Bad Request: Invalid chat ID or message format.
                            
                            SOLUTIONS:
                            1. Check your chat ID format (should be a number like 123456789)
                            2. Get your correct chat ID from @userinfobot
                            3. Make sure you've started a conversation with your bot
                            
                            Error: $errorDescription
                        """.trimIndent()
                        401 -> "401 Unauthorized: Invalid bot token. Please check your bot token."
                        429 -> "429 Too Many Requests: Rate limited by Telegram. Please wait a moment."
                        else -> "Telegram API error ($errorCode): $errorDescription"
                    }
                    
                    DebugLogger.logError("API Error $errorCode: $errorDescription", null)
                    
                    MessageResult(
                        success = false,
                        messageId = null,
                        error = detailedError
                    )
                }
            } else {
                // Handle HTTP errors with more detail
                val errorDetails = when (response.code) {
                    403 -> """
                        403 Forbidden: Bot doesn't have permission to send to this chat.
                        
                        SOLUTIONS:
                        1. Send /start to your bot in Telegram
                        2. Get your chat ID from @userinfobot
                        3. Add your chat ID in bot settings manually
                    """.trimIndent()
                    401 -> "401 Unauthorized: Check your bot token - it might be invalid."
                    404 -> "404 Not Found: Bot token or API endpoint incorrect. Please verify your bot token."
                    429 -> "429 Too Many Requests: Telegram rate limit. Please wait a moment."
                    500 -> "500 Server Error: Telegram API is temporarily down. Please try again later."
                    else -> "HTTP error ${response.code}: ${response.message}"
                }
                
                DebugLogger.logError("HTTP Error ${response.code}: ${response.message}", null)
                
                MessageResult(
                    success = false,
                    messageId = null,
                    error = errorDetails
                )
            }
        } catch (e: Exception) {
            DebugLogger.logError("Network/Exception error: ${e.message}", e)
            
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
            
            // Log the polling call
            DebugLogger.logApiCall(url, "GET", null)
            DebugLogger.log("Polling for bot updates with offset: $offset", DebugLogger.LogLevel.DEBUG)
            
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            // Log the polling response
            DebugLogger.logApiResponse(url, response.code, responseBody)
            
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
                val ok = jsonResponse.get("ok").asBoolean
                
                if (ok && jsonResponse.has("result")) {
                    val result = jsonResponse.getAsJsonArray("result")
                    val messages = mutableListOf<BotMessage>()
                    
                    DebugLogger.log("Received ${result.size()} updates from Telegram", DebugLogger.LogLevel.DEBUG)
                    
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
                            
                            DebugLogger.log("Parsed message from $fromName (ID: $fromId) in chat $chatId: $text", DebugLogger.LogLevel.DEBUG)
                        }
                    }
                    
                    messages
                } else {
                    DebugLogger.log("No updates received from Telegram or API error", DebugLogger.LogLevel.DEBUG)
                    emptyList()
                }
            } else {
                DebugLogger.log("Failed to get updates: HTTP ${response.code}", DebugLogger.LogLevel.WARN)
                emptyList()
            }
        } catch (e: Exception) {
            DebugLogger.logError("Error getting bot updates: ${e.message}", e)
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