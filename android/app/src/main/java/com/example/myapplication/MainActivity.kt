package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.myapplication.storage.ConversationManager
import com.example.myapplication.ui.ConversationFragment
import com.example.myapplication.ui.ConversationListFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ConversationListFragment())
                .commit()
        }
    }

    fun openConversation(conversationId: String) {
        val conversation = ConversationManager(this).getConversations()
            .find { it.id == conversationId } ?: return

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ConversationFragment.newInstance(conversation))
            .addToBackStack(null)
            .commit()
    }
}