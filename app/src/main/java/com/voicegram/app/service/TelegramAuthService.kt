package com.voicegram.app.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.voicegram.app.service.DebugLogger

class TelegramAuthService(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "telegram_auth",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    companion object {
        private const val KEY_USER_ID = "telegram_user_id"
        private const val KEY_PHONE_NUMBER = "telegram_phone_number"
        private const val KEY_USERNAME = "telegram_username"
        private const val KEY_AUTH_TOKEN = "telegram_auth_token"
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"
    }
    
    fun isAuthenticated(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_AUTHENTICATED, false)
    }
    
    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }
    
    fun getPhoneNumber(): String? {
        return sharedPreferences.getString(KEY_PHONE_NUMBER, null)
    }
    
    fun getUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }
    
    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }
    
    fun openTelegramDeepLink(deepLink: String) {
        try {
            DebugLogger.log("Opening Telegram deep link: $deepLink", DebugLogger.LogLevel.INFO)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            DebugLogger.logError("Error opening Telegram deep link", e)
            Toast.makeText(context, "Error opening Telegram: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun openTelegramBot(botUsername: String, startParam: String? = null) {
        val deepLink = if (startParam != null) {
            "tg://resolve?domain=$botUsername&start=$startParam"
        } else {
            "tg://resolve?domain=$botUsername"
        }
        openTelegramDeepLink(deepLink)
    }
    
    fun authenticateWithBotFather() {
        // Open BotFather with a simpler deep link format
        DebugLogger.log("Starting authentication with BotFather", DebugLogger.LogLevel.INFO)
        // Use a simpler deep link that just opens BotFather
        val deepLink = "tg://resolve?domain=BotFather"
        openTelegramDeepLink(deepLink)
        Toast.makeText(context, "Opening BotFather in Telegram... Please get your bot info there", Toast.LENGTH_LONG).show()
    }
    
    fun authenticateWithCustomBot(botUsername: String) {
        DebugLogger.log("Starting authentication with custom bot: $botUsername", DebugLogger.LogLevel.INFO)
        val deepLink = "tg://resolve?domain=$botUsername"
        openTelegramDeepLink(deepLink)
    }
    
    fun openUserInfoBot() {
        // Open @userinfobot to get user ID and phone number
        DebugLogger.log("Opening @userinfobot to get user info", DebugLogger.LogLevel.INFO)
        val deepLink = "tg://resolve?domain=userinfobot"
        openTelegramDeepLink(deepLink)
        Toast.makeText(context, "Opening @userinfobot... Send /start to get your user ID", Toast.LENGTH_LONG).show()
    }
    
    fun saveAuthentication(userId: String, phoneNumber: String, username: String?, authToken: String?) {
        try {
            sharedPreferences.edit().apply {
                putString(KEY_USER_ID, userId)
                putString(KEY_PHONE_NUMBER, phoneNumber)
                putString(KEY_USERNAME, username)
                putString(KEY_AUTH_TOKEN, authToken)
                putBoolean(KEY_IS_AUTHENTICATED, true)
                apply()
            }
            DebugLogger.log("Authentication saved successfully for user: $username ($phoneNumber)", DebugLogger.LogLevel.INFO)
            Toast.makeText(context, "Authentication successful!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            DebugLogger.logError("Error saving authentication", e)
            Toast.makeText(context, "Error saving authentication: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun manualAuthenticate(userId: String, phoneNumber: String, username: String = "") {
        saveAuthentication(userId, phoneNumber, username.ifEmpty { null }, null)
    }
    
    fun clearAuthentication() {
        try {
            sharedPreferences.edit().clear().apply()
            DebugLogger.log("Authentication cleared", DebugLogger.LogLevel.INFO)
            Toast.makeText(context, "Authentication cleared", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            DebugLogger.logError("Error clearing authentication", e)
        }
    }
    
    fun getSenderInfo(): SenderInfo {
        return SenderInfo(
            userId = getUserId(),
            phoneNumber = getPhoneNumber(),
            username = getUsername(),
            isAuthenticated = isAuthenticated()
        )
    }
    
    data class SenderInfo(
        val userId: String?,
        val phoneNumber: String?,
        val username: String?,
        val isAuthenticated: Boolean
    )
}