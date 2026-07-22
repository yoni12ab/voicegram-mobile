package com.voicegram.app.service

import android.content.Context
import android.content.SharedPreferences

class AuthManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("VoiceGramAuth", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_AUTH_METHOD = "auth_method"
        private const val KEY_BOT_TOKEN = "bot_token"
        private const val KEY_CHAT_ID = "chat_id"
        private const val KEY_PHONE_NUMBER = "phone_number"
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"
        
        const val AUTH_METHOD_BOT = "bot"
        const val AUTH_METHOD_PHONE = "phone"
    }
    
    fun authenticateWithBotToken(botToken: String, chatId: String): Boolean {
        return try {
            prefs.edit()
                .putString(KEY_AUTH_METHOD, AUTH_METHOD_BOT)
                .putString(KEY_BOT_TOKEN, botToken)
                .putString(KEY_CHAT_ID, chatId)
                .putBoolean(KEY_IS_AUTHENTICATED, true)
                .apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun authenticateWithPhone(phoneNumber: String): Boolean {
        // Phone authentication would require Telegram's login API
        // This is a placeholder for the phone authentication flow
        return try {
            prefs.edit()
                .putString(KEY_AUTH_METHOD, AUTH_METHOD_PHONE)
                .putString(KEY_PHONE_NUMBER, phoneNumber)
                .putBoolean(KEY_IS_AUTHENTICATED, true)
                .apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun getBotToken(): String? = prefs.getString(KEY_BOT_TOKEN, null)
    
    fun getChatId(): String? = prefs.getString(KEY_CHAT_ID, null)
    
    fun getPhoneNumber(): String? = prefs.getString(KEY_PHONE_NUMBER, null)
    
    fun getAuthMethod(): String? = prefs.getString(KEY_AUTH_METHOD, null)
    
    fun isAuthenticated(): Boolean = prefs.getBoolean(KEY_IS_AUTHENTICATED, false)
    
    fun logout() {
        prefs.edit().clear().apply()
    }
}