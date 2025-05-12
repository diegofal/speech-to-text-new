package com.example.myapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.audio.AudioPlayer
import com.example.myapplication.model.Conversation
import com.example.myapplication.model.Message
import com.example.myapplication.speech.SpeechRecognitionListener
import com.example.myapplication.speech.SpeechRecognizer
import com.example.myapplication.storage.ConversationManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ConversationFragment : Fragment(), SpeechRecognitionListener {
    private lateinit var conversation: Conversation
    private lateinit var conversationManager: ConversationManager
    private lateinit var messagesContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var recognizerToggleGroup: MaterialButtonToggleGroup
    private lateinit var btnStartStop: MaterialButton
    
    private var currentRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var lastPartialResult: String = ""
    private var currentMessage: Message? = null
    private var audioPlayer: AudioPlayer? = null
    private var currentlyPlayingView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_conversation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        conversation = arguments?.getParcelable(ARG_CONVERSATION, Conversation::class.java) ?: return
        android.util.Log.d("ConversationFragment", "onViewCreated: conversation from args = $conversation")
        conversationManager = ConversationManager(requireContext())
        audioPlayer = AudioPlayer(requireContext())

        setupViews(view)
        loadMessages()

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    parentFragmentManager.popBackStack()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audioPlayer?.stop()
        audioPlayer = null
    }

    private fun setupViews(view: View) {
        view.findViewById<MaterialToolbar>(R.id.toolbar).apply {
            title = conversation.title
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }

        messagesContainer = view.findViewById(R.id.messagesContainer)
        scrollView = view.findViewById(R.id.scrollView)
        recognizerToggleGroup = view.findViewById(R.id.recognizerToggleGroup)
        btnStartStop = view.findViewById(R.id.btnStartStop)

        recognizerToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                try {
                    stopListening()
                    currentRecognizer?.destroy()
                    currentRecognizer = when (checkedId) {
                        R.id.btnVosk -> SpeechRecognizer.createVoskRecognizer(requireContext())
                        R.id.btnWhisper -> SpeechRecognizer.createWhisperRecognizer(requireContext())
                        else -> null
                    }
                    currentRecognizer?.setListener(this)
                } catch (e: Exception) {
                    e.printStackTrace()
                    addMessage("❌ Error: ${e.message}")
                }
            }
        }

        recognizerToggleGroup.check(R.id.btnVosk)

        btnStartStop.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                startListening()
            }
        }
    }

    private fun loadMessages() {
        // Always fetch the latest conversation from storage
        val updatedConversation = conversationManager.getConversations().find { it.id == conversation.id }
        if (updatedConversation != null) {
            conversation = updatedConversation
        }
        val messages = conversation.messages
        android.util.Log.d("ConversationFragment", "loadMessages: messages=$messages")
        if (messages.isEmpty()) {
            messagesContainer.removeAllViews()
            return
        }

        messagesContainer.removeAllViews()
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        messages.forEach { message ->
            val messageView = layoutInflater.inflate(R.layout.item_message, messagesContainer, false)
            val messageTextView = messageView.findViewById<TextView>(R.id.tvMessage)
            val playButton = messageView.findViewById<ImageButton>(R.id.btnPlay)
            
            val time = dateFormat.format(message.timestamp)
            messageTextView.text = "$time: ${message.text}"
            
            // Handle audio playback
            if (message.audioFilePath != null) {
                val audioFile = File(message.audioFilePath)
                if (audioFile.exists() && audioFile.canRead() && audioFile.length() > 0) {
                    playButton.visibility = View.VISIBLE
                    playButton.setOnClickListener {
                        if (currentlyPlayingView == messageView) {
                            audioPlayer?.stop()
                            currentlyPlayingView = null
                            playButton.setImageResource(R.drawable.ic_play)
                        } else {
                            // Stop any currently playing audio
                            currentlyPlayingView?.findViewById<ImageButton>(R.id.btnPlay)?.setImageResource(R.drawable.ic_play)
                            
                            audioPlayer?.play(audioFile) {
                                currentlyPlayingView = null
                                playButton.setImageResource(R.drawable.ic_play)
                            }
                            currentlyPlayingView = messageView
                            playButton.setImageResource(R.drawable.ic_pause)
                        }
                    }
                } else {
                    Log.e("ConversationFragment", "Audio file not accessible: ${message.audioFilePath}")
                    playButton.visibility = View.GONE
                }
            } else {
                playButton.visibility = View.GONE
            }
            
            messagesContainer.addView(messageView)
        }
        
        scrollToBottom()
    }

    private fun addMessage(text: String) {
        val message = Message(text = text)
        conversationManager.addMessage(conversation.id, message)
        loadMessages()
    }

    private fun startListening() {
        if (!isListening) {
            try {
                currentRecognizer?.startListening("en-US")
                isListening = true
                updateUI()
            } catch (e: Exception) {
                e.printStackTrace()
                isListening = false
                updateUI()
            }
        }
    }

    private fun stopListening() {
        if (isListening) {
            try {
                currentRecognizer?.stopListening()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isListening = false
                updateUI()
            }
        }
    }

    private fun updateUI() {
        btnStartStop.apply {
            text = if (isListening) "Stop Listening" else "Start Listening"
            setIconResource(if (isListening) R.drawable.ic_mic_off else R.drawable.ic_mic)
        }
        recognizerToggleGroup.isEnabled = !isListening
    }

    private fun scrollToBottom() {
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    // SpeechRecognitionListener implementation
    override fun onRecognitionStarted() {
        // Clear any previous partial results
        lastPartialResult = ""
        currentMessage = null
        addMessage("Starting recognition...")
        scrollToBottom()
    }

    override fun onPartialResult(text: String) {
        android.util.Log.d("ConversationFragment", "onPartialResult: $text, thread: ${Thread.currentThread().name}")
        if (text == lastPartialResult) return
        lastPartialResult = text
        
        // Update or create the current message
        if (currentMessage == null) {
            currentMessage = Message(text = text, isPartial = true)
            conversationManager.addMessage(conversation.id, currentMessage!!)
        } else {
            currentMessage = currentMessage!!.copy(text = text)
            conversationManager.updatePartialMessage(conversation.id, currentMessage!!)
        }
        
        requireActivity().runOnUiThread {
            loadMessages()
        }
    }

    override fun onResult(text: String, audioFilePath: String?) {
        android.util.Log.d("ConversationFragment", "onResult: $text, audioFilePath: $audioFilePath")
        requireActivity().runOnUiThread {
            if (currentMessage != null) {
                // Update the existing message
                currentMessage = currentMessage?.copy(
                    text = text,
                    isPartial = false,
                    audioFilePath = audioFilePath
                )
                currentMessage?.let { message ->
                    Log.d("ConversationFragment", "Updating message with audio file: ${message.audioFilePath}")
                    conversationManager.updatePartialMessage(conversation.id, message)
                }
            } else {
                // Create a new message
                currentMessage = Message(
                    text = text,
                    audioFilePath = audioFilePath
                )
                currentMessage?.let { message ->
                    Log.d("ConversationFragment", "Creating new message with audio file: ${message.audioFilePath}")
                    conversationManager.addMessage(conversation.id, message)
                }
            }
            loadMessages()
        }
    }

    override fun onError(error: String) {
        android.util.Log.d("ConversationFragment", "onError: $error, thread: "+Thread.currentThread().name)
        requireActivity().runOnUiThread {
            addMessage("❌ Error: $error")
            scrollToBottom()
        }
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("ConversationFragment", "onResume: Fragment is active")
        loadMessages() // Force UI refresh when fragment becomes visible
    }

    override fun onRecognitionEnded() {
        android.util.Log.d("ConversationFragment", "onRecognitionEnded")
        requireActivity().runOnUiThread {
            isListening = false
            updateUI()
            
            // Show audio file location if available
            Log.d("ConversationFragment", "Checking for audio file in current message: ${currentMessage?.audioFilePath}")
            currentMessage?.audioFilePath?.let { filePath ->
                Log.d("ConversationFragment", "Found audio file path, showing popup: $filePath")
                showAudioFileLocation(filePath)
            } ?: Log.d("ConversationFragment", "No audio file path found in current message")
        }
    }

    private fun showAudioFileLocation(filePath: String) {
        val file = File(filePath)
        Log.d("ConversationFragment", "Showing audio file location dialog for: $filePath")
        Log.d("ConversationFragment", "File exists: ${file.exists()}, Size: ${file.length()}")
        
        val message = if (file.exists()) {
            "Audio file saved at:\n$filePath\n\nFile size: ${file.length() / 1024} KB"
        } else {
            "Audio file not found at:\n$filePath"
        }
        
        try {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Audio File Location")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
            Log.d("ConversationFragment", "Successfully showed audio file location dialog")
        } catch (e: Exception) {
            Log.e("ConversationFragment", "Error showing audio file location dialog", e)
        }
    }

    companion object {
        private const val ARG_CONVERSATION = "conversation"

        fun newInstance(conversation: Conversation): ConversationFragment {
            return ConversationFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CONVERSATION, conversation)
                }
            }
        }
    }
} 