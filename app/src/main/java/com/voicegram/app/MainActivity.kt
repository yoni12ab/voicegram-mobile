package com.voicegram.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.voicegram.app.service.AuthManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var authManager: AuthManager
    private lateinit var authMethodGroup: RadioGroup
    private lateinit var botTokenEditText: EditText
    private lateinit var chatIdEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var authenticateButton: Button
    private lateinit var chatListButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        authManager = AuthManager(this)
        
        setupUI()
        
        // Check if already authenticated
        if (authManager.isAuthenticated()) {
            showChatListButton()
        }
    }
    
    private fun setupUI() {
        authMethodGroup = findViewById(R.id.authMethodGroup)
        botTokenEditText = findViewById(R.id.botTokenEditText)
        chatIdEditText = findViewById(R.id.chatIdEditText)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        authenticateButton = findViewById(R.id.authenticateButton)
        chatListButton = findViewById(R.id.chatListButton)
        
        authMethodGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.botAuthRadioButton -> {
                    botTokenEditText.isEnabled = true
                    chatIdEditText.isEnabled = true
                    phoneNumberEditText.isEnabled = false
                }
                R.id.phoneAuthRadioButton -> {
                    botTokenEditText.isEnabled = false
                    chatIdEditText.isEnabled = false
                    phoneNumberEditText.isEnabled = true
                }
            }
        }
        
        authenticateButton.setOnClickListener {
            authenticate()
        }
        
        chatListButton.setOnClickListener {
            openChatList()
        }
        
        // Initialize with bot authentication selected
        botTokenEditText.isEnabled = true
        chatIdEditText.isEnabled = true
        phoneNumberEditText.isEnabled = false
    }
    
    private fun authenticate() {
        val selectedMethod = authMethodGroup.checkedRadioButtonId
        
        when (selectedMethod) {
            R.id.botAuthRadioButton -> {
                val botToken = botTokenEditText.text.toString().trim()
                val chatId = chatIdEditText.text.toString().trim()
                
                if (botToken.isEmpty() || chatId.isEmpty()) {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return
                }
                
                if (authManager.authenticateWithBotToken(botToken, chatId)) {
                    Toast.makeText(this, "Authentication successful!", Toast.LENGTH_SHORT).show()
                    showChatListButton()
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
            
            R.id.phoneAuthRadioButton -> {
                val phoneNumber = phoneNumberEditText.text.toString().trim()
                
                if (phoneNumber.isEmpty()) {
                    Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show()
                    return
                }
                
                if (authManager.authenticateWithPhone(phoneNumber)) {
                    Toast.makeText(this, "Phone authentication initiated", Toast.LENGTH_SHORT).show()
                    showChatListButton()
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showChatListButton() {
        chatListButton.visibility = Button.VISIBLE
        authenticateButton.visibility = Button.GONE
        authMethodGroup.visibility = RadioGroup.GONE
        botTokenEditText.isEnabled = false
        chatIdEditText.isEnabled = false
        phoneNumberEditText.isEnabled = false
    }
    
    private fun openChatList() {
        val intent = Intent(this, com.voicegram.app.ui.ChatListActivity::class.java)
        startActivity(intent)
    }
}