package com.voicegram.app.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.voicegram.app.R
import com.voicegram.app.service.BotValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class BotItem(val id: String, val name: String, val token: String)

class BotListActivity : AppCompatActivity(), CoroutineScope by CoroutineScope(Dispatchers.Main) {
    
    private lateinit var botListView: ListView
    private lateinit var addBotButton: Button
    private val botList = ArrayList<BotItem>()
    private lateinit var adapter: BotAdapter
    private val botValidator = BotValidator(this)
    
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
        
        AlertDialog.Builder(this)
            .setTitle("Add Bot")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val botName = botNameEditText.text.toString().trim()
                val botToken = botTokenEditText.text.toString().trim()
                
                if (botName.isNotEmpty() && botToken.isNotEmpty()) {
                    addBot(botName, botToken)
                } else {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addBot(name: String, token: String) {
        Toast.makeText(this, "Validating bot token...", Toast.LENGTH_SHORT).show()
        
        launch {
            val validationResult = botValidator.validateBotToken(token)
            
            if (validationResult.isValid) {
                val newBot = BotItem(
                    id = (botList.size + 1).toString(),
                    name = validationResult.botName ?: name,
                    token = token
                )
                botList.add(newBot)
                adapter.notifyDataSetChanged()
                saveBots()
                val botUsername = validationResult.botName ?: name
                Toast.makeText(applicationContext, "Bot added: @$botUsername. Tip: Send /start to the bot in Telegram first!", Toast.LENGTH_LONG).show()
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
        }
        editor.apply()
    }
    
    private fun openBotCall(bot: BotItem) {
        val intent = Intent(this, CallActivity::class.java)
        intent.putExtra("bot_id", bot.id)
        intent.putExtra("bot_name", bot.name)
        intent.putExtra("bot_token", bot.token)
        startActivity(intent)
    }
}