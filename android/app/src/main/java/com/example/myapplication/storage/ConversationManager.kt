package com.example.myapplication.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.model.Conversation
import com.example.myapplication.model.Message
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.util.Date

class ConversationManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .create()

    fun saveConversation(conversation: Conversation) {
        val conversations = getConversations().toMutableList()
        val index = conversations.indexOfFirst { it.id == conversation.id }
        if (index != -1) {
            conversations[index] = conversation
        } else {
            conversations.add(conversation)
        }
        saveConversations(conversations)
    }

    fun getConversations(): List<Conversation> {
        val json = sharedPreferences.getString(KEY_CONVERSATIONS, "[]")
        val type = object : TypeToken<List<Conversation>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun deleteConversation(conversationId: String) {
        val conversations = getConversations().toMutableList()
        conversations.removeAll { it.id == conversationId }
        saveConversations(conversations)
    }

    fun addMessage(conversationId: String, message: Message) {
        val conversations = getConversations().toMutableList()
        val conversation = conversations.find { it.id == conversationId }
        if (conversation != null) {
            conversation.messages.add(message)
            conversation.lastUpdated = Date()
            saveConversations(conversations)
        }
    }

    fun updatePartialMessage(conversationId: String, message: Message) {
        val conversations = getConversations().toMutableList()
        val conversation = conversations.find { it.id == conversationId }
        if (conversation != null) {
            // Remove the last message if it was partial
            if (conversation.messages.isNotEmpty() && conversation.messages.last().isPartial) {
                conversation.messages.removeLast()
            }
            conversation.messages.add(message)
            conversation.lastUpdated = Date()
            saveConversations(conversations)
        }
    }

    private fun saveConversations(conversations: List<Conversation>) {
        val json = gson.toJson(conversations)
        sharedPreferences.edit().putString(KEY_CONVERSATIONS, json).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "conversations"
        private const val KEY_CONVERSATIONS = "conversations_list"
    }
} 