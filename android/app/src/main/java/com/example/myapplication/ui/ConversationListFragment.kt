package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.model.Conversation
import com.example.myapplication.storage.ConversationManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationListFragment : Fragment() {
    private lateinit var conversationManager: ConversationManager
    private lateinit var adapter: ConversationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_conversation_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        conversationManager = ConversationManager(requireContext())
        setupRecyclerView(view)
        setupFab(view)
    }

    override fun onResume() {
        super.onResume()
        loadConversations()
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvConversations)
        adapter = ConversationAdapter(
            onItemClick = { conversation ->
                (activity as? MainActivity)?.openConversation(conversation.id)
            },
            onDeleteClick = { conversation ->
                showDeleteConfirmationDialog(conversation)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        loadConversations()
    }

    private fun setupFab(view: View) {
        view.findViewById<FloatingActionButton>(R.id.fabNewConversation).setOnClickListener {
            createNewConversation()
        }
    }

    private fun loadConversations() {
        adapter.updateConversations(conversationManager.getConversations())
    }

    private fun createNewConversation() {
        val conversation = Conversation(
            title = "Conversation ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())}"
        )
        conversationManager.saveConversation(conversation)
        loadConversations()
        (activity as? MainActivity)?.openConversation(conversation.id)
    }

    private fun showDeleteConfirmationDialog(conversation: Conversation) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Conversation")
            .setMessage("Are you sure you want to delete this conversation?")
            .setPositiveButton("Delete") { _, _ ->
                conversationManager.deleteConversation(conversation.id)
                loadConversations()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

class ConversationAdapter(
    private val onItemClick: (Conversation) -> Unit,
    private val onDeleteClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

    private var conversations: List<Conversation> = emptyList()

    fun updateConversations(newConversations: List<Conversation>) {
        conversations = newConversations.sortedByDescending { it.lastUpdated }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = conversations[position]
        holder.bind(conversation)
    }

    override fun getItemCount(): Int = conversations.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.tvTitle)
        private val lastMessageTextView: TextView = itemView.findViewById(R.id.tvLastMessage)
        private val timestampTextView: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(conversation: Conversation) {
            titleTextView.text = conversation.title
            
            // Create a summary of the conversation
            val messages = conversation.messages.filter { !it.isPartial }
            val summary = when {
                messages.isEmpty() -> "No messages"
                messages.size == 1 -> messages.first().text
                else -> {
                    val firstMessage = messages.first().text
                    val lastMessage = messages.last().text
                    if (firstMessage == lastMessage) {
                        firstMessage
                    } else {
                        "${firstMessage.take(30)}...${lastMessage.takeLast(30)}"
                    }
                }
            }
            lastMessageTextView.text = summary
            
            val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            timestampTextView.text = dateFormat.format(conversation.lastUpdated)

            itemView.setOnClickListener { onItemClick(conversation) }
            itemView.setOnLongClickListener {
                onDeleteClick(conversation)
                true
            }
        }
    }
} 