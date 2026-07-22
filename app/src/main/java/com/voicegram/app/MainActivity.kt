package com.voicegram.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var botListButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupUI()
    }
    
    private fun setupUI() {
        botListButton = findViewById(R.id.botListButton)
        
        botListButton.setOnClickListener {
            openBotList()
        }
        
        // For now, auto-show bot list button
        showBotListButton()
    }
    
    private fun showBotListButton() {
        botListButton.visibility = Button.VISIBLE
    }
    
    private fun openBotList() {
        val intent = Intent(this, com.voicegram.app.ui.BotListActivity::class.java)
        startActivity(intent)
    }
}