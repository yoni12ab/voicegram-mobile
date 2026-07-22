package com.voicegram.app.service

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

class AuthManager(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "voicegram_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    private val secureRandom = SecureRandom()
    
    companion object {
        private const val KEY_AUTH_METHOD = "auth_method"
        private const val KEY_BOT_TOKEN = "bot_token"
        private const val KEY_CHAT_ID = "chat_id"
        private const val KEY_PHONE_NUMBER = "phone_number"
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"
        private const val KEY_APP_PIN = "app_pin"
        private const val KEY_PIN_SET = "pin_set"
        
        const val AUTH_METHOD_BOT = "bot"
        const val AUTH_METHOD_PHONE = "phone"
    }
    
    fun setAppPin(pin: String): Boolean {
        return try {
            sharedPreferences.edit()
                .putString(KEY_APP_PIN, hashPin(pin))
                .putBoolean(KEY_PIN_SET, true)
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun verifyAppPin(pin: String): Boolean {
        val storedHash = sharedPreferences.getString(KEY_APP_PIN, null)
        return storedHash != null && storedHash == hashPin(pin)
    }
    
    fun isPinSet(): Boolean {
        return sharedPreferences.getBoolean(KEY_PIN_SET, false)
    }
    
    fun authenticateWithBotToken(botToken: String, chatId: String): Boolean {
        return try {
            sharedPreferences.edit()
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
            sharedPreferences.edit()
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
    
    fun getBotToken(): String? = sharedPreferences.getString(KEY_BOT_TOKEN, null)
    
    fun getChatId(): String? = sharedPreferences.getString(KEY_CHAT_ID, null)
    
    fun getPhoneNumber(): String? = sharedPreferences.getString(KEY_PHONE_NUMBER, null)
    
    fun getAuthMethod(): String? = sharedPreferences.getString(KEY_AUTH_METHOD, null)
    
    fun isAuthenticated(): Boolean = sharedPreferences.getBoolean(KEY_IS_AUTHENTICATED, false)
    
    fun logout() {
        sharedPreferences.edit().clear().apply()
    }
    
    private fun hashPin(pin: String): String {
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)
        
        // Simple hash - in production, use proper password hashing like bcrypt
        val combined = pin + salt.contentToString()
        return combined.hashCode().toString() + ":" + salt.contentToString()
    }
}