package com.voicegram.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.voicegram.app.R
import com.voicegram.app.service.AuthManager

data class ChatItem(val id: String, val name: String, val lastMessage: String)

class ChatListActivity : AppCompatActivity() {
    
    private lateinit var authManager: AuthManager
    private lateinit var chatListView: ListView
    private val chatList = ArrayList<ChatItem>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)
        
        authManager = AuthManager(this)
        
        setupUI()
        loadChats()
    }
    
    private fun setupUI() {
        chatListView = findViewById(R.id.chatListView)
        
        chatListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedChat = chatList[position]
            openCallActivity(selectedChat)
        }
    }
    
    private fun loadChats() {
        // Placeholder chat data - in production, this would come from Telegram API
        chatList.add(ChatItem("1", "General Chat", "Last message..."))
        chatList.add(ChatItem("2", "Voice Notes", "Voice message..."))
        chatList.add(ChatItem("3", "Work Group", "Meeting at 3pm..."))
        chatList.add(ChatItem("4", "Family", "Call when you can..."))
        
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            chatList.map { "${it.name}: ${it.lastMessage}" }
        )
        
        chatListView.adapter = adapter
    }
    
    private fun openCallActivity(chat: ChatItem) {
        val intent = Intent(this, CallActivity::class.java)
        intent.putExtra("chat_id", chat.id)
        intent.putExtra("chat_name", chat.name)
        startActivity(intent)
    }
    
    override fun onResume() {
        super.onResume()
        if (!authManager.isAuthenticated()) {
            Toast.makeText(this, "Please authenticate first", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}