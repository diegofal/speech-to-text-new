package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.model.Conversation
import com.example.myapplication.model.Message
import com.example.myapplication.speech.SpeechRecognitionListener
import com.example.myapplication.speech.SpeechRecognizer
import com.example.myapplication.storage.ConversationManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import java.text.SimpleDateFormat
import java.util.Locale

class ConversationFragment : Fragment(), SpeechRecognitionListener {
    private lateinit var conversation: Conversation
    private lateinit var conversationManager: ConversationManager
    private lateinit var messagesTextView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var recognizerToggleGroup: MaterialButtonToggleGroup
    private lateinit var btnStartStop: MaterialButton
    
    private var currentRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var lastPartialResult: String = ""

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
        conversationManager = ConversationManager(requireContext())

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

    private fun setupViews(view: View) {
        view.findViewById<MaterialToolbar>(R.id.toolbar).apply {
            title = conversation.title
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }

        messagesTextView = view.findViewById(R.id.tvMessages)
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
        val messages = conversation.messages
        if (messages.isEmpty()) {
            messagesTextView.text = ""
            return
        }

        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedMessages = messages.joinToString("\n\n") { message ->
            val time = dateFormat.format(message.timestamp)
            if (message.isPartial) {
                "_$time: ${message.text}"
            } else {
                "$time: ${message.text}"
            }
        }

        messagesTextView.text = formattedMessages
        scrollToBottom()
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
        // Do nothing
    }

    override fun onPartialResult(text: String) {
        if (text == lastPartialResult) return
        
        lastPartialResult = text
        val message = Message(text = text, isPartial = true)
        conversationManager.updatePartialMessage(conversation.id, message)
        loadMessages()
    }

    override fun onResult(text: String) {
        if (text.isNotEmpty()) {
            val message = Message(text = text)
            conversationManager.addMessage(conversation.id, message)
            loadMessages()
        }
    }

    override fun onError(error: String) {
        // Show error in UI
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