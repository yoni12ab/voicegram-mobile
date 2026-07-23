package com.voicegram.app.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.voicegram.app.R
import com.voicegram.app.service.BotValidator
import com.voicegram.app.service.TelegramService
import com.voicegram.app.service.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class BotItem(val id: String, val name: String, val token: String, val chatId: String? = null)

class BotListActivity : AppCompatActivity(), CoroutineScope by CoroutineScope(Dispatchers.Main) {
    
    private lateinit var botListView: ListView
    private lateinit var addBotButton: Button
    private val botList = ArrayList<BotItem>()
    private lateinit var adapter: BotAdapter
    private val botValidator = BotValidator(this)
    private val telegramService = TelegramService()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bot_list)
        
        setupUI()
        loadBots()
    }
    
    private fun setupUI() {
        botListView = findViewById(R.id.botListView)
        addBotButton = findViewById(R.id.addBotButton)
        
        adapter = BotAdapter(this, botList)
        botListView.adapter = adapter
        
        botListView.setOnItemClickListener { _, _, position, _ ->
            val selectedBot = botList[position]
            openBotCall(selectedBot)
        }
        
        botListView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedBot = botList[position]
            showDeleteBotDialog(selectedBot, position)
            true
        }
        
        addBotButton.setOnClickListener {
            showAddBotDialog()
        }
        
        // Add test chat ID button
        val testChatIdButton = Button(this)
        testChatIdButton.text = "Test Chat IDs"
        testChatIdButton.setBackgroundColor(android.graphics.Color.parseColor("#FF5722"))
        testChatIdButton.setTextColor(android.graphics.Color.WHITE)
        testChatIdButton.setOnClickListener {
            showChatIdTestDialog()
        }
        
        // Add button to the layout
        val parentLayout = addBotButton.parent as android.widget.LinearLayout
        val index = parentLayout.indexOfChild(addBotButton)
        parentLayout.addView(testChatIdButton, index + 1)
    }
    
    private fun loadBots() {
        val prefs = getSharedPreferences("VoiceGramBots", MODE_PRIVATE)
        val botJson = prefs.getString("bot_list", "")
        
        if (botJson.isNullOrEmpty()) {
            // Add some example bots for demonstration
            botList.add(BotItem("1", "@BotFather", "PLACEHOLDER_TOKEN"))
            botList.add(BotItem("2", "@VoicegramBot", "PLACEHOLDER_TOKEN"))
        } else {
            // Load saved bots (simplified for now)
            botList.add(BotItem("1", "@BotFather", "PLACEHOLDER_TOKEN"))
            botList.add(BotItem("2", "@VoicegramBot", "PLACEHOLDER_TOKEN"))
        }
        
        adapter.notifyDataSetChanged()
    }
    
    private fun showAddBotDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_bot, null)
        val botNameEditText = dialogView.findViewById<EditText>(R.id.botNameEditText)
        val botTokenEditText = dialogView.findViewById<EditText>(R.id.botTokenEditText)
        val chatIdEditText = dialogView.findViewById<EditText>(R.id.chatIdEditText)
        
        AlertDialog.Builder(this)
            .setTitle("Add Bot")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val botName = botNameEditText.text.toString().trim()
                val botToken = botTokenEditText.text.toString().trim()
                val chatId = chatIdEditText.text.toString().trim()
                
                if (botName.isNotEmpty() && botToken.isNotEmpty()) {
                    addBot(botName, botToken, if (chatId.isNotEmpty()) chatId else null)
                } else {
                    Toast.makeText(this, "Please fill in bot name and token", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addBot(name: String, token: String, chatId: String? = null) {
        Toast.makeText(this, "Validating bot token...", Toast.LENGTH_SHORT).show()
        
        launch {
            val validationResult = botValidator.validateBotToken(token)
            
            if (validationResult.isValid) {
                val newBot = BotItem(
                    id = (botList.size + 1).toString(),
                    name = validationResult.botName ?: name,
                    token = token,
                    chatId = chatId
                )
                botList.add(newBot)
                adapter.notifyDataSetChanged()
                saveBots()
                val botUsername = validationResult.botName ?: name
                val message = if (chatId != null) {
                    "Bot added: @$botUsername with chat ID. Ready to use!"
                } else {
                    "Bot added: @$botUsername. Tip: Send /start to the bot in Telegram first!"
                }
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(applicationContext, "Invalid bot token: " + validationResult.error, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showDeleteBotDialog(bot: BotItem, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Remove Bot")
            .setMessage("Are you sure you want to remove ${bot.name}?")
            .setPositiveButton("Remove") { _, _ ->
                removeBot(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun removeBot(position: Int) {
        botList.removeAt(position)
        adapter.notifyDataSetChanged()
        saveBots()
        Toast.makeText(this, "Bot removed", Toast.LENGTH_SHORT).show()
    }
    
    private fun saveBots() {
        val prefs = getSharedPreferences("VoiceGramBots", MODE_PRIVATE)
        val editor = prefs.edit()
        
        // Simplified saving - in production, use proper JSON serialization
        editor.putString("bot_count", botList.size.toString())
        botList.forEachIndexed { index, bot ->
            editor.putString("bot_${index}_name", bot.name)
            editor.putString("bot_${index}_token", bot.token)
            editor.putString("bot_${index}_chatId", bot.chatId ?: "")
        }
        editor.apply()
    }
    
    private fun openBotCall(bot: BotItem) {
        val intent = Intent(this, CallActivity::class.java)
        intent.putExtra("bot_id", bot.id)
        intent.putExtra("bot_name", bot.name)
        intent.putExtra("bot_token", bot.token)
        intent.putExtra("bot_chat_id", bot.chatId)
        startActivity(intent)
    }
    
    private fun showChatIdTestDialog() {
        val builder = AlertDialog.Builder(this)
        
        val botTokenEditText = EditText(this)
        botTokenEditText.hint = "Bot Token"
        
        val chatIdEditText = EditText(this)
        chatIdEditText.hint = "Chat ID to test (number or @username)"
        
        val testInstructions = TextView(this)
        testInstructions.text = "Test different chat IDs to find which one works:\n\n1. Try your user ID from @userinfobot\n2. Try the bot's own ID\n3. Try @username format\n4. Check logs for detailed error info"
        testInstructions.setPadding(20, 20, 20, 20)
        testInstructions.textSize = 12f
        
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)
        layout.addView(testInstructions)
        layout.addView(botTokenEditText)
        layout.addView(chatIdEditText)
        
        builder.setTitle("Test Chat ID")
            .setMessage("Send a test message to identify the working chat ID")
            .setView(layout)
            .setPositiveButton("Test") { _, _ ->
                val botToken = botTokenEditText.text.toString().trim()
                val chatId = chatIdEditText.text.toString().trim()
                
                if (botToken.isNotEmpty() && chatId.isNotEmpty()) {
                    testChatId(botToken, chatId)
                } else {
                    Toast.makeText(this, "Please enter both Bot Token and Chat ID", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun testChatId(botToken: String, chatId: String) {
        launch {
            try {
                Toast.makeText(applicationContext, "Testing chat ID: $chatId...", Toast.LENGTH_SHORT).show()
                DebugLogger.log("Testing chat ID: $chatId with bot token: ${botToken.take(10)}...", DebugLogger.LogLevel.INFO)
                
                val result = telegramService.sendMessage(botToken, chatId, "Test message from VoiceGram", null, null)
                
                if (result.success) {
                    Toast.makeText(applicationContext, "✓ SUCCESS! Chat ID '$chatId' works!", Toast.LENGTH_LONG).show()
                    DebugLogger.log("Chat ID test SUCCESS: $chatId", DebugLogger.LogLevel.INFO)
                    
                    AlertDialog.Builder(this@BotListActivity)
                        .setTitle("Chat ID Works!")
                        .setMessage("Chat ID '$chatId' is working!\n\nUse this chat ID in your bot configuration.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    val error = result.error ?: "Unknown error"
                    Toast.makeText(applicationContext, "✗ FAILED: $error", Toast.LENGTH_LONG).show()
                    DebugLogger.logError("Chat ID test FAILED for '$chatId': $error", null)
                    
                    AlertDialog.Builder(this@BotListActivity)
                        .setTitle("Chat ID Failed")
                        .setMessage("Chat ID '$chatId' failed with error:\n\n$error\n\nTry a different chat ID.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                DebugLogger.logError("Chat ID test exception", e)
            }
        }
    }
}