package com.voicegram.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class BotAdapter(context: Context, private val bots: ArrayList<BotItem>) :
    ArrayAdapter<BotItem>(context, android.R.layout.simple_list_item_1, bots) {
    
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        
        val bot = bots[position]
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = "${bot.name} - Tap to call"
        
        return view
    }
}